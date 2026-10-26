#!/usr/bin/env python3
"""Real Gemini-authored PS-style adjustments; no segmentation or image synthesis.
Install Pillow, numpy, requests. GEMINI_API_KEY may be supplied via hidden input.
"""
import argparse
import base64
import getpass
import html
import json
import os
from pathlib import Path
import time

import numpy as np
import requests
from PIL import Image, ImageCms, ImageDraw, ImageFont
from preview_ai_color import open_photo, jpeg_bytes, checked_parameters, adjust, KEYS, response_json, safe_error

CURVE_X = [0,32,64,96,128,160,192,224,255]
BANDS = {'red':0, 'orange':30, 'yellow':60, 'green':110, 'aqua':175, 'blue':225, 'purple':275, 'magenta':315}


def number(value, low, high):
    value=float(value)
    if not np.isfinite(value): raise ValueError('Non-finite parameter')
    return min(high,max(low,value))


def validate(plan):
    out={'file':plan['file'],'scene':str(plan.get('scene','')),'intent':str(plan.get('intent','')),
         'steps':plan.get('steps',[]),'cautions':plan.get('cautions',[])}
    out['basic']=checked_parameters(plan.get('basic',{}))
    ys=plan.get('curve_y',CURVE_X)
    if len(ys)!=len(CURVE_X): raise ValueError('Curve must have nine knots')
    ys=[number(y,max(0,x-22),min(255,x+22)) for x,y in zip(CURVE_X,ys)]
    ys[0]=0;ys[-1]=255
    if any(a>b for a,b in zip(ys,ys[1:])): raise ValueError('Curve must be monotonic')
    out['curve_y']=ys
    out['hsl']=[]
    seen=set()
    for item in plan.get('hsl',[]):
        band=item.get('band')
        if isinstance(band,(int,float)):
            band=next((name for name,angle in BANDS.items() if angle==band),None)
        elif isinstance(band,str):
            band=band.lower()
        if band not in BANDS:raise ValueError('Unknown HSL color band')
        if band in seen:raise ValueError('Duplicate HSL color band')
        seen.add(band)
        out['hsl'].append({'band':band,'half_width':number(item.get('half_width',40),20,70),
                          'hue_shift':number(item.get('hue_shift',0),-10,10),
                          'saturation_percent':number(item.get('saturation_percent',0),-25,25),
                          'lightness_points':number(item.get('lightness_points',0),-7,7)})
    balance=plan.get('color_balance',{})
    out['color_balance']={}
    for band in ['shadows','midtones','highlights']:
        values=balance.get(band,[0,0,0])
        if len(values)!=3:raise ValueError('Color balance must have three channels')
        out['color_balance'][band]=[number(v,-4,4) for v in values]
    return out


def analyze(photos,folder,model):
    prompt='''You are a professional landscape photographer editing two provided photos of the same scene.
Act like a Photoshop colorist operating non-destructive adjustment layers. NO object masks, NO segmentation,
NO spatial selections. The engine can use ONLY global exposure/shadows/highlights, tone curve, feathered
HSL color ranges, and luminance-based RGB color balance. Look carefully at each actual photo.
Produce a coherent NATURAL, clean and visibly improved edit that retains natural lake-water hues, text,
faces, realistic sky gradients and texture. Do not exaggerate cyan/teal water or neon foliage. Keep warm
reeds and skin natural. Shadows/foreground and the dark monument can be lifted via tone ranges, but these
operations necessarily affect other pixels with similar tones. Avoid excessive brightening or HDR halos.
All edits are computed deterministically; do not describe object-exclusive changes which this engine cannot do.
Return a JSON object with plans: an array of one object per named input image. Each object contains:
file, scene (Chinese), intent (Chinese), steps (Chinese string array explaining actual operations),
cautions (Chinese string array explaining shared-color limitations), basic, curve_y, hsl, color_balance.
Exact processing order:
1. basic parameters with all defaults zero: exposure stops [-0.5,0.5], contrast [-0.2,0.2],
shadows [-0.2,0.25], highlights [-0.25,0.15], temperature [-0.05,0.05] positive warms,
tint [-0.04,0.04] positive magenta, saturation [-0.25,0.3]. Gamma-encoded sRGB in 0..1:
y=.2126r+.7152g+.0722b; tonal=shadows*(1-y)^2+highlights*y^2;
rgb=((rgb*2^exposure+tonal+[temperature+tint/2,-tint,-temperature+tint/2])-.5)*(1+contrast)+.5;
then saturation multiplier 1+saturation about luminance, clamp. A shadows value of 0.1 is significant;
be conservative when also lifting the curve. Keep basic exposure close to zero if the sky is already bright.
2. curve_y: 9 output values in 0..255 at fixed x=[0,32,64,96,128,160,192,224,255].
Each output can deviate at most 22 levels, must be monotonic; endpoints 0 and 255. Applied to RGB channels.
3. hsl: list of {band,half_width,hue_shift,saturation_percent,lightness_points}.
Bands centered at red=0,orange=30,yellow=60,green=110,aqua=175,blue=225,purple=275,magenta=315 degrees.
half_width 20..70 defines a cosine-feathered circular hue range. Hue shift -10..10 degrees;
saturation relative -25..25%; lightness additive -7..7 points in HSL 0..100.
Low-saturation neutral pixels are smoothly excluded. Similar-colored sky and water may both be affected.
4. color_balance: {shadows:[r,g,b],midtones:[r,g,b],highlights:[r,g,b]} channel offsets in byte levels,
-4..4 per channel, with smooth luminance weights (1-y)^2,4*y*(1-y),y^2. Keep near zero when unnecessary.
Use these tools selectively; zero is valid. Avoid strong saturation and crushed blacks. Photos must look
natural, not painted or replaced. Return JSON only, no markdown.
'''
    parts=[{'text':prompt}]
    for path,source,preview,data in photos:
        parts.extend([{'text':'Input photo filename: '+path.name},
                      {'inline_data':{'mime_type':'image/jpeg','data':base64.b64encode(data).decode()}}])
    body={'contents':[{'parts':parts}], 'generationConfig':{'temperature':.15,'responseMimeType':'application/json','maxOutputTokens':8192}}
    started=time.monotonic()
    for candidate in dict.fromkeys([model,'gemini-3.6-flash','gemini-3.8-flash']):
        print('Gemini requesting two-photo PS-style plan: '+candidate,flush=True)
        response=requests.post('https://generativelanguage.googleapis.com/v1beta/models/'+candidate+':generateContent',
            headers={'x-goog-api-key':KEYS['gemini']},json=body,timeout=(15,150))
        if response.status_code in [404,503]:
            print('Gemini unavailable: HTTP '+str(response.status_code),flush=True);continue
        raw=response_json(response)
        text=''.join(x.get('text','') for x in raw.get('candidates',[{}])[0].get('content',{}).get('parts',[]) if not x.get('thought'))
        (folder/'gemini_raw.json').write_text(json.dumps(raw,ensure_ascii=False,indent=2))
        parsed=json.loads(text)
        items=parsed if isinstance(parsed,list) else parsed.get('plans',[])
        requested={p.name for p,_,_,_ in photos}
        plans={item['file']:validate(item) for item in items if isinstance(item,dict) and item.get('file') in requested}
        if set(plans)!=requested:raise ValueError('Missing photo plan')
        record={'provider':'Gemini API','requested_model':candidate,'returned_model':raw.get('modelVersion'),
                'seconds':round(time.monotonic()-started,2),'usage':raw.get('usageMetadata',{}),
                'segmentation':False,'plans':list(plans.values())}
        (folder/'gemini_response.json').write_text(json.dumps(record,ensure_ascii=False,indent=2))
        (folder/'prompt.txt').write_text(prompt)
        return plans
    raise RuntimeError('Gemini Flash models temporarily unavailable')


def rgb_to_hsl(rgb):
    maximum=rgb.max(axis=-1);minimum=rgb.min(axis=-1);delta=maximum-minimum
    light=(maximum+minimum)/2
    sat=np.divide(delta,1-np.abs(2*light-1),out=np.zeros_like(light),where=(1-np.abs(2*light-1))>1e-6)
    dr=np.where(delta>1e-6,delta,1)
    idx=np.argmax(rgb,axis=-1)
    hue=np.select([idx==0,idx==1],[(rgb[...,1]-rgb[...,2])/dr,(rgb[...,2]-rgb[...,0])/dr+2],default=(rgb[...,0]-rgb[...,1])/dr+4)
    hue=np.where(delta>1e-6,(hue*60)%360,0)
    return hue,sat,light


def hsl_to_rgb(h,s,l):
    c=(1-np.abs(2*l-1))*s
    hp=(h%360)/60
    x=c*(1-np.abs(hp%2-1))
    z=np.zeros_like(c)
    sector=np.floor(hp).astype(int)
    r=np.select([sector==0,sector==1,sector==2,sector==3,sector==4],[c,x,z,z,x],default=c)
    g=np.select([sector==0,sector==1,sector==2,sector==3,sector==4],[x,c,c,x,z],default=z)
    b=np.select([sector==0,sector==1,sector==2,sector==3,sector==4],[z,z,x,c,c],default=x)
    return np.stack([r,g,b],axis=-1)+(l-c/2)[...,None]


def apply_plan(original,plan):
    base=adjust(original,plan['basic'])
    base=(np.interp(base*255,CURVE_X,plan['curve_y'])/255).astype(np.float32)
    if plan['hsl']:
        h,s,l=rgb_to_hsl(base)
        neutral=np.clip((s-.05)/.2,0,1);neutral=neutral*neutral*(3-2*neutral)
        dh=np.zeros_like(h);ds=np.zeros_like(h);dl=np.zeros_like(h);total=np.zeros_like(h)
        for item in plan['hsl']:
            dist=np.abs((h-BANDS[item['band']]+180)%360-180)
            w=np.where(dist<item['half_width'],.5+.5*np.cos(np.pi*np.minimum(dist/item['half_width'],1)),0)*neutral
            total+=w;dh+=w*item['hue_shift'];ds+=w*item['saturation_percent']/100;dl+=w*item['lightness_points']/100
        normal=np.maximum(total,1)
        base=hsl_to_rgb((h+dh/normal)%360,np.clip(s*(1+ds/normal),0,1),np.clip(l+dl/normal,0,1))
    y=(base*np.array([.2126,.7152,.0722],np.float32)).sum(axis=-1)
    for key,weight in [('shadows',(1-y)**2),('midtones',4*y*(1-y)),('highlights',y*y)]:
        base=base+weight[...,None]*np.array(plan['color_balance'][key],np.float32)/255
    return np.clip(base,0,1)


def render(source,plan):
    natural=Image.new('RGB',source.size);gentle=Image.new('RGB',source.size)
    for top in range(0,source.height,192):
        box=(0,top,source.width,min(top+192,source.height))
        original=np.asarray(source.crop(box),dtype=np.float32)/255
        out=apply_plan(original,plan)
        natural.paste(Image.fromarray(np.uint8(np.round(out*255))),box)
        gentle.paste(Image.fromarray(np.uint8(np.round((original+(out-original)*.65)*255))),box)
    return natural,gentle


def make_comparison(original,natural,gentle,folder):
    original=original.copy();original.thumbnail((900,700))
    font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',23)
    w,h=original.size
    sheet=Image.new('RGB',(w*3,h+52),(23,26,31));draw=ImageDraw.Draw(sheet)
    for i,(label,img) in enumerate([('ORIGINAL',original),('GENTLE / 65%',gentle),('NATURAL / 100%',natural)]):
        sheet.paste(img.resize((w,h),Image.Resampling.LANCZOS),(i*w,52));draw.text((i*w+16,13),label,font=font,fill='white')
    sheet.save(folder/'comparison.jpg',quality=94)
    for name,img in [('original_preview',original),('natural_preview',natural),('gentle_preview',gentle)]:
        copy=img.copy();copy.thumbnail((1600,1200));copy.save(folder/(name+'.jpg'),quality=94)


def write_html(root,photos,plans):
    cards=[]
    for path,*_ in photos:
        p=plans[path.name];name=path.stem
        steps=''.join('<li>'+html.escape(str(x))+'</li>' for x in p['steps'])
        cards.append(f'''<section><h2>{html.escape(path.name)}</h2><p>{html.escape(p['intent'])}</p>
<div class="compare"><img src="{name}/original_preview.jpg" alt="原图"><div class="after"><img src="{name}/natural_preview.jpg" alt="调色后"></div><span class="before-label">原图</span><span class="after-label">调色后</span></div>
<label>拖动比较原图与调色效果<input type="range" min="0" max="100" value="50" oninput="this.closest('section').querySelector('.after').style.clipPath='inset(0 '+(100-this.value)+'% 0 0)'" /></label>
<p><a href="{name}/natural.jpg" download>下载自然版原尺寸</a> · <a href="{name}/gentle.jpg" download>下载较轻版原尺寸</a> · <a href="{name}/comparison.jpg">三栏对比图</a></p><ul>{steps}</ul></section>''')
    doc='''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>AI 调色对比</title><style>
body{margin:0;background:#15191e;color:#e8edf3;font:16px/1.65 system-ui,sans-serif}main{max-width:1100px;margin:auto;padding:28px 20px}h1{margin:0}p{color:#b7c5d2}section{margin:32px 0;padding:20px;background:#20262e;border-radius:14px}a{color:#84caff} .compare{position:relative;overflow:hidden;line-height:0}.compare img{width:100%;display:block}.after{position:absolute;inset:0;clip-path:inset(0 50% 0 0)}.before-label,.after-label{position:absolute;top:12px;padding:8px;background:#0009;line-height:1.2}.before-label{right:12px}.after-label{left:12px}input{display:block;width:100%;margin:16px 0}label{display:block;margin-top:16px}
</style><main><h1>Gemini · PS 风格调色预览</h1><p>真实 LLM 生成参数，本地执行曲线、HSL 与色彩平衡。未调用分割或图像生成模型，原图保留。相近颜色可能同时受到调整。</p>'''+''.join(cards)+'</main></html>'
    (root/'index.html').write_text(doc)


def main():
    parser=argparse.ArgumentParser();parser.add_argument('photos',nargs='+',type=Path)
    parser.add_argument('--output',type=Path,default=Path('artifacts/llm-color-preview'))
    parser.add_argument('--model',default='gemini-3.5-flash');args=parser.parse_args()
    args.output.mkdir(parents=True,exist_ok=True)
    photos=[]
    for path in args.photos:
        source=open_photo(path);preview,data=jpeg_bytes(source);photos.append((path,source,preview,data))
    cached=args.output/'gemini_response.json'
    if cached.exists():plans={p['file']:validate(p) for p in json.loads(cached.read_text())['plans']}
    else:
        KEYS['gemini']=os.environ.get('GEMINI_API_KEY') or getpass.getpass('Gemini API key (hidden): ')
        plans=analyze(photos,args.output,args.model)
    icc=ImageCms.ImageCmsProfile(ImageCms.createProfile('sRGB')).tobytes()
    for path,source,preview,data in photos:
        plan=plans[path.name];folder=args.output/path.stem;folder.mkdir(exist_ok=True)
        (folder/'plan.json').write_text(json.dumps(plan,ensure_ascii=False,indent=2))
        print('Rendering '+path.name+': '+plan['intent'],flush=True)
        natural,gentle=render(source,plan)
        natural.save(folder/'natural.jpg',quality=96,subsampling=0,icc_profile=icc)
        gentle.save(folder/'gentle.jpg',quality=96,subsampling=0,icc_profile=icc)
        make_comparison(source,natural,gentle,folder)
        inp=np.asarray(preview,dtype=np.float32)/255
        out=np.asarray(natural.resize(preview.size,Image.Resampling.LANCZOS),dtype=np.float32)/255
        metrics={'source_dimensions':source.size,'output_dimensions':natural.size,'segmentation':False,
                 'mean_rgb_change':float(np.abs(out-inp).mean()),
                 'original_high_clip':float((inp.max(axis=-1)>=.995).mean()),
                 'output_high_clip':float((out.max(axis=-1)>=.995).mean())}
        (folder/'metrics.json').write_text(json.dumps(metrics,indent=2))
        print('DONE '+str(folder/'comparison.jpg'),flush=True)
    write_html(args.output,photos,plans)
    print('COMPLETE '+str(args.output/'index.html'),flush=True)

if __name__=='__main__':
    try:main()
    except Exception as e:
        print('FAILED: '+safe_error(e),flush=True);raise SystemExit(1)
