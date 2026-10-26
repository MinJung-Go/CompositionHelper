#!/usr/bin/env python3
"""Gemini plan + hosted SAM 3 masks + deterministic local photo grading.
Credentials come from environment variables or a hidden JSON prompt, never files.
Dependencies: Pillow, numpy, requests. Originals are never modified.
"""
import argparse
import base64
import concurrent.futures
import getpass
import io
import json
import os
from pathlib import Path
import time
import threading
from urllib.parse import urlparse

import numpy as np
import requests
from PIL import Image, ImageCms, ImageDraw, ImageFilter, ImageFont, ImageOps

LIMITS = {'exposure':(-.5,.5), 'contrast':(-.2,.2), 'shadows':(-.2,.25),
          'highlights':(-.25,.15), 'temperature':(-.05,.05), 'tint':(-.04,.04), 'saturation':(-.25,.3)}
REGIONS = {'sky':'sky', 'water':'lake water', 'vegetation':'trees, grass, reeds and plants',
           'stone':'stone monument', 'person':'person'}
COLORS = {'sky':(75,140,255), 'water':(0,220,240), 'vegetation':(80,240,110),
          'stone':(255,165,55), 'person':(245,90,180)}
KEYS = {}
FAL_BLOCKED = threading.Event()


def safe_error(exc):
    msg = str(exc)
    for secret in KEYS.values():
        if secret: msg = msg.replace(secret, '[REDACTED]')
    return msg[:1500]


def response_json(response):
    if not response.ok:
        # Never dump request headers or base64 image payloads.
        try:
            body = response.json()
            detail = body.get('error', body.get('detail', body.get('message', 'request failed')))
        except Exception:
            detail = 'non-JSON response'
        raise RuntimeError(f'HTTP {response.status_code}: {str(detail)[:1000]}')
    return response.json()


def open_photo(path):
    with Image.open(path) as source:
        icc = source.info.get('icc_profile')
        oriented = ImageOps.exif_transpose(source).convert('RGB')
        if icc:
            oriented = ImageCms.profileToProfile(oriented, ImageCms.ImageCmsProfile(io.BytesIO(icc)),
                                                ImageCms.createProfile('sRGB'), outputMode='RGB')
    return oriented


def jpeg_bytes(image, limit=1600):
    preview = image.copy()
    preview.thumbnail((limit, limit))
    buf = io.BytesIO()
    preview.save(buf, 'JPEG', quality=91)
    return preview, buf.getvalue()


def checked_parameters(value):
    if not isinstance(value, dict): raise ValueError('Missing parameter object')
    result = {}
    for key, (low, high) in LIMITS.items():
        v = float(value.get(key, 0))
        if not np.isfinite(v): raise ValueError('Non-finite adjustment')
        result[key] = min(high, max(low, v))
    return result


def make_plan(data, model):
    prompt = '''Act as a conservative professional landscape photo colorist. Analyze the actual photograph.
We will use SAM 3 semantic masks and a deterministic local engine. Produce a NATURAL, visibly improved,
coherent daylight edit. Preserve lettering, faces, texture, geometry, and the scene's real water color.
Do not make a green lake artificially blue. Protect the bright sky. Lift a dark stone/foreground gently.
Use global correction sparingly; select local corrections based on observed material and lighting.
The available region IDs are sky, water, vegetation, stone, person. Include all present regions;
include person with all-zero parameters if only protecting it. Do not invent absent regions.
Output JSON with scene (Chinese), intent (Chinese), global_reason (Chinese), global (parameters),
regions (array of objects with id, reason in Chinese, params). No markdown.
Parameters are: exposure (stops, -0.5..0.5), contrast (-0.2..0.2), shadows (-0.2..0.25),
highlights (-0.25..0.15), temperature (-0.05..0.05, positive warms), tint (-0.04..0.04,
positive magenta), saturation (-0.25..0.3). Omitted parameters default to zero.
Exact renderer on gamma-encoded sRGB values in 0..1:
y=0.2126*r+0.7152*g+0.0722*b; tonal=shadows*(1-y)^2+highlights*y^2;
R=((r*2^exposure+tonal+temperature+tint/2)-0.5)*(1+contrast)+0.5;
G=((g*2^exposure+tonal-tint)-0.5)*(1+contrast)+0.5;
B=((b*2^exposure+tonal-temperature+tint/2)-0.5)*(1+contrast)+0.5;
then luminance-preserving saturation multiplier (1+saturation), clamp to [0,1].
Local regional correction runs after global. Avoid combinations that crush shadows or clip highlights.
A small positive shadows value can have a large effect. Keep exposure of the sky conservative.
'''
    payload = {'contents':[{'parts':[{'text':prompt}, {'inline_data':{'mime_type':'image/jpeg',
                      'data':base64.b64encode(data).decode()}}]}],
               'generationConfig':{'temperature':.15, 'responseMimeType':'application/json',
                                   'maxOutputTokens':8192}}
    started = time.monotonic()
    raw = None
    for candidate in dict.fromkeys([model, 'gemini-3.8-flash', 'gemini-3.5-flash']):
        response = requests.post(
            f'https://generativelanguage.googleapis.com/v1beta/models/{candidate}:generateContent',
            headers={'x-goog-api-key':KEYS['gemini']}, json=payload, timeout=(15,120))
        if response.status_code in [404, 503]:
            print(f'Gemini {candidate}: HTTP {response.status_code}; trying available Flash alternative',flush=True)
            continue
        raw = response_json(response)
        model = candidate
        break
    if raw is None: raise RuntimeError('All selected Gemini Flash models unavailable')
    text = ''.join(p.get('text','') for p in raw.get('candidates',[{}])[0].get('content',{}).get('parts',[]))
    plan = json.loads(text)
    plan['global'] = checked_parameters(plan['global'])
    seen = set()
    regions = []
    for r in plan.get('regions',[]):
        if r.get('id') not in REGIONS or r['id'] in seen: continue
        seen.add(r['id'])
        regions.append({'id':r['id'], 'reason':str(r.get('reason','')), 'params':checked_parameters(r['params'])})
    plan['regions'] = regions
    plan['model'] = model
    plan['analysis_seconds'] = round(time.monotonic()-started,2)
    plan['usage'] = raw.get('usageMetadata',{})
    return plan


def queue_get(url):
    if urlparse(url).scheme != 'https' or urlparse(url).hostname != 'queue.fal.run':
        raise ValueError('Unexpected FAL queue URL')
    return response_json(requests.get(url, headers={'Authorization':'Key '+KEYS['fal']}, timeout=(15,45)))


def mask_bytes(url):
    if url.startswith('data:image/'):
        return base64.b64decode(url.split(',',1)[1])
    parsed = urlparse(url)
    if parsed.scheme != 'https' or not any(parsed.hostname == host or parsed.hostname.endswith('.'+host)
                                          for host in ['fal.media','fal.run']):
        raise ValueError('Unexpected mask download host')
    r = requests.get(url, timeout=(15,60))
    r.raise_for_status()
    return r.content


def segment(data, region, size, folder):
    if FAL_BLOCKED.is_set(): raise RuntimeError('FAL authentication/balance blocked; no new request submitted')
    target = folder / f'mask_{region}.png'
    meta_path = folder / f'mask_{region}.json'
    if target.exists() and meta_path.exists():
        return region, Image.open(target).convert('L'), json.loads(meta_path.read_text())
    submission_path = folder / f'queue_{region}.json'
    started = time.monotonic()
    if submission_path.exists():
        queue = json.loads(submission_path.read_text())
    else:
        submitted = requests.post('https://queue.fal.run/fal-ai/sam-3/image',
            headers={'Authorization':'Key '+KEYS['fal']},
            json={'image_url':'data:image/jpeg;base64,'+base64.b64encode(data).decode(),
                  'prompt':REGIONS[region], 'apply_mask':False, 'return_multiple_masks':True,
                  'max_masks':8, 'include_scores':True, 'output_format':'png'}, timeout=(15,60))
        if submitted.status_code in (401,403): FAL_BLOCKED.set()
        queue = response_json(submitted)
        # Request IDs enable safe resume without submitting another billable job.
        submission_path.write_text(json.dumps({k:queue[k] for k in ['request_id','status_url','response_url']},indent=2))
    print(f'SAM3 queued {folder.name}/{region}', flush=True)
    deadline = time.monotonic()+300
    while time.monotonic() < deadline:
        status = queue_get(queue['status_url'])
        if status.get('status') == 'COMPLETED': break
        if status.get('status') not in ['IN_QUEUE','IN_PROGRESS']:
            raise RuntimeError('Unexpected SAM3 job status: '+str(status.get('status')))
        time.sleep(2)
    else: raise TimeoutError('SAM3 queue timed out; request ID saved for resume')
    result = queue_get(queue['response_url'])
    union = np.zeros((size[1],size[0]), dtype=np.uint8)
    count = 0
    scores = result.get('scores') or []
    for i,item in enumerate(result.get('masks',[])):
        score = scores[i] if i < len(scores) else None
        if score is not None and score < .35: continue
        m = Image.open(io.BytesIO(mask_bytes(item['url'])))
        if m.mode == 'RGBA' and m.getchannel('A').getextrema() != (255,255):
            m = m.getchannel('A')
        else: m = m.convert('L')
        m = m.resize(size, Image.Resampling.BILINEAR)
        union = np.maximum(union,np.asarray(m))
        count += 1
    image = Image.fromarray(union)
    image.save(target)
    meta = {'provider':'fal.ai','model':'fal-ai/sam-3/image','prompt':REGIONS[region],
            'request_id':queue['request_id'],'scores':scores,'masks_used':count,
            'coverage':round(float((union>127).mean()),5),'seconds':round(time.monotonic()-started,2)}
    meta_path.write_text(json.dumps(meta,indent=2))
    print(f'SAM3 ready {folder.name}/{region}: {meta["coverage"]:.1%} coverage',flush=True)
    return region,image,meta


def adjust(rgb, p):
    r,g,b = rgb[...,0],rgb[...,1],rgb[...,2]
    y=.2126*r+.7152*g+.0722*b
    tonal=p['shadows']*(1-y)**2+p['highlights']*y*y
    balances=np.array([p['temperature']+p['tint']/2,-p['tint'],-p['temperature']+p['tint']/2],np.float32)
    out=(rgb*2**p['exposure']+tonal[...,None]+balances-.5)*(1+p['contrast'])+.5
    grey=(out*np.array([.2126,.7152,.0722],np.float32)).sum(axis=2,keepdims=True)
    return np.clip(grey+(out-grey)*(1+p['saturation']),0,1)


def render(source, plan, masks, strength):
    width,height=source.size
    # Mutually exclusive masks: person/stone protect against broad vegetation masks.
    priority=['person','stone','water','sky','vegetation']
    used=Image.new('L',source.size,0)
    weights={}
    for region in priority:
        if region not in masks: continue
        raw=masks[region].resize(source.size,Image.Resampling.BILINEAR)
        a=np.asarray(raw,dtype=np.float32)/255
        u=np.asarray(used,dtype=np.float32)/255
        a=a*(1-u)
        used=Image.fromarray(np.uint8(np.clip(u+a,0,1)*255))
        weights[region]=Image.fromarray(np.uint8(a*255)).filter(ImageFilter.GaussianBlur(max(1,width/1600)))
    output=Image.new('RGB',source.size)
    params={r['id']:r['params'] for r in plan['regions']}
    for top in range(0,height,192):
        box=(0,top,width,min(height,top+192))
        original=np.asarray(source.crop(box),dtype=np.float32)/255
        base=adjust(original,plan['global'])
        delta=np.zeros_like(base)
        total=np.zeros(base.shape[:2],np.float32)
        for region,mask in weights.items():
            w=np.asarray(mask.crop(box),dtype=np.float32)/255
            target = adjust(original if region == 'person' else base, params[region])
            delta+=(target-base)*w[...,None]
            total+=w
        delta/=np.maximum(total,1)[...,None]
        graded=np.clip(base+delta,0,1)
        final=np.clip(original+(graded-original)*strength,0,1)
        output.paste(Image.fromarray(np.uint8(np.round(final*255))),box)
    return output


def comparisons(source, result, masks, folder):
    a=source.copy();a.thumbnail((1100,850))
    b=result.resize(a.size,Image.Resampling.LANCZOS)
    font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',24)
    sheet=Image.new('RGB',(a.width*2,a.height+52),(22,25,30))
    sheet.paste(a,(0,52));sheet.paste(b,(a.width,52))
    d=ImageDraw.Draw(sheet);d.text((18,12),'ORIGINAL',fill='white',font=font)
    d.text((a.width+18,12),'GEMINI + SAM 3 / NATURAL',fill='white',font=font)
    sheet.save(folder/'comparison.jpg',quality=94)
    thumb=source.copy();thumb.thumbnail((1200,1000))
    overlay=np.asarray(thumb,dtype=np.float32)
    for region,mask in masks.items():
        alpha=np.asarray(mask.resize(thumb.size),dtype=np.float32)/255*.38
        overlay=overlay*(1-alpha[...,None])+np.array(COLORS[region])*alpha[...,None]
    mask_image=Image.fromarray(np.uint8(overlay))
    legend=Image.new('RGB',(thumb.width,thumb.height+90),(22,25,30));legend.paste(mask_image,(0,90))
    d=ImageDraw.Draw(legend);d.text((12,8),'SAM 3 REGION MASKS',font=font,fill='white')
    x=12
    for region in masks:
        d.text((x,46),region,font=font,fill=COLORS[region]);x+=max(110,len(region)*17+20)
    legend.save(folder/'regions.jpg',quality=94)


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('photos',nargs='+',type=Path)
    parser.add_argument('--output',type=Path,default=Path('artifacts/ai-color-preview'))
    parser.add_argument('--model',default='gemini-3.6-flash')
    args=parser.parse_args()
    KEYS.update(gemini=os.environ.get('GEMINI_API_KEY',''),fal=os.environ.get('FAL_KEY',''))
    if not all(KEYS.values()): KEYS.update(json.loads(getpass.getpass('API credentials JSON (hidden): ')))
    args.output.mkdir(parents=True,exist_ok=True)
    jobs=[]
    for path in args.photos:
        folder=args.output/path.stem;folder.mkdir(exist_ok=True)
        source=open_photo(path);preview,data=jpeg_bytes(source)
        preview.save(folder/'original_preview.jpg',quality=93)
        jobs.append((path,folder,source,preview,data))
    # The two supplied photos have these five visibly present regions. Segmenting
    # them is independent of Gemini's choice of correction parameters.
    plan_results={}; mask_results={}; error_results={}
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as planners, concurrent.futures.ThreadPoolExecutor(max_workers=3) as segmenters:
        plan_futures={}
        mask_futures={}
        for path,folder,source,preview,data in jobs:
            plan_path=folder/'plan.json'
            if plan_path.exists(): plan_results[path.name]=json.loads(plan_path.read_text())
            else:
                print(f'Gemini analyzing {path.name} ({source.width}x{source.height})',flush=True)
                plan_futures[planners.submit(make_plan,data,args.model)]=(path,folder)
            mask_results[path.name]={};error_results[path.name]={}
            for region in REGIONS:
                mask_futures[segmenters.submit(segment,data,region,preview.size,folder)]=(path,region)
        for future in concurrent.futures.as_completed(mask_futures):
            path,region=mask_futures[future]
            try:
                _,mask,meta=future.result()
                mask_results[path.name][region]=(mask,meta)
            except Exception as e:
                error_results[path.name][region]=safe_error(e)
                print(f'SAM3 failed {path.name}/{region}: {safe_error(e)}',flush=True)
        for future in concurrent.futures.as_completed(plan_futures):
            path,folder=plan_futures[future]
            try:
                plan=future.result();plan_results[path.name]=plan
                (folder/'plan.json').write_text(json.dumps(plan,ensure_ascii=False,indent=2))
                print(f'Gemini plan {path.name}: {plan.get("intent","")}',flush=True)
            except Exception as e:
                error_results[path.name]['gemini']=safe_error(e)
                print(f'Gemini failed {path.name}: {safe_error(e)}',flush=True)
    blocked=False
    for path,folder,source,preview,data in jobs:
        errors=error_results[path.name]
        plan=plan_results.get(path.name)
        if plan is None:
            blocked=True
            (folder/'errors.json').write_text(json.dumps(errors,ensure_ascii=False,indent=2))
            continue
        wanted={r['id'] for r in plan['regions']}
        metadata={k:v[1] for k,v in mask_results[path.name].items()}
        masks={k:v[0] for k,v in mask_results[path.name].items() if k in wanted and v[1]['coverage']>.0001}
        if errors:
            (folder/'errors.json').write_text(json.dumps(errors,ensure_ascii=False,indent=2))
        if not masks:
            blocked=True
            print(f'BLOCKED {path.name}: Gemini plan saved, no SAM3 masks; no graded output generated',flush=True)
            continue
        result=render(source,plan,masks,1.0)
        icc=ImageCms.ImageCmsProfile(ImageCms.createProfile('sRGB')).tobytes()
        result.save(folder/'natural.jpg',quality=96,subsampling=0,icc_profile=icc)
        gentle=render(source,plan,masks,.65)
        gentle.save(folder/'gentle.jpg',quality=96,subsampling=0,icc_profile=icc)
        comparisons(source,result,masks,folder)
        small=result.copy();small.thumbnail(preview.size)
        orig=np.asarray(preview,dtype=np.float32)/255
        new=np.asarray(small,dtype=np.float32)/255
        metrics={'image':path.name,'size':source.size,'model':plan['model'],
                 'regions':metadata,'failures':errors,
                 'mean_absolute_rgb_change':float(np.abs(new-orig).mean()),
                 'original_high_clip_fraction':float((orig.max(axis=2)>=.995).mean()),
                 'output_high_clip_fraction':float((new.max(axis=2)>=.995).mean()),
                 'renderer':'CPU sRGB parameter engine, equations aligned with Android ColorAdjustmentEngine plus regional masks',
                 'variants':{'natural':1.0,'gentle':.65}}
        (folder/'result.json').write_text(json.dumps(metrics,ensure_ascii=False,indent=2))
        print(f'DONE {path.name}: {folder}/comparison.jpg',flush=True)
    print('FINISHED: inspect result.json / errors.json for each photo',flush=True)
    if blocked: raise SystemExit(2)

if __name__=='__main__':
    try: main()
    except Exception as exc:
        print('FAILED: '+safe_error(exc),flush=True)
        raise SystemExit(1)
