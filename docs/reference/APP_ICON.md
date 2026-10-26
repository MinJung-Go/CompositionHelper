# 应用图标

2026-09-08：使用内置 imagegen 工具生成并修订图标。炭黑背景、暖金色取景框及螺旋，延续应用深色界面。

- 原始成品：`design/app-icon/master.png`
- 裁切预览：`artifacts/app-icon/preview.jpg`
- Android 26+：自适应图标，背景加前景图层，适配启动器圆形和圆角遮罩。
- Android 24–25：五档密度普通及圆形 PNG。
- 资源缩放及裁切预览使用 Pillow，未调用外部图片 API。

## 生成提示词

Use case: logo-brand. Asset: production Android launcher icon for a refined photography composition and color editing app. Generate ONE flat square full-bleed 1024x1024 PNG, not a presentation mockup. Matte near-black charcoal background (#101113). Center one distinctive warm champagne-gold (#E3CAA1) geometric mark: four bold short softly rounded viewfinder corner brackets framing a restrained smooth golden spiral that suggests photographic composition, balanced and optically centered. Very clean premium editorial identity, flat two-color graphic, no text or letters, no camera clip art, no multicolor gradient, no 3D, no shadows, no border around image, no rounded app tile drawn inside canvas. Keep ALL gold artwork within the central 48 percent of canvas width and height, strong generously weighted strokes that remain legible at 48px. The outer canvas must be uninterrupted charcoal so Android can safely crop to circle or squircle. Deliver only the icon artwork.

## 最终修订提示词

Edit this app icon. Preserve the four viewfinder corners and the spiral shape and their centered placement. Replace EVERY pixel outside the champagne gold symbol with one completely uniform opaque charcoal color #101113. Remove ALL red, yellow, pink, white, gray speckles, painted texture and artifacts between the spiral and brackets. Flatten the gold symbol itself to a single solid warm gold #E3CAA1 with clean anti-aliased edges, no shading, no gradients, no outlines. Exactly two flat colors, smooth clean professional geometric logo. Full square image, no rounded tile, no text. Output PNG.
