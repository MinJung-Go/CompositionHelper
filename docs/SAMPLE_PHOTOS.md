# 内置参考照片

2026-09-08 从 Pexels 照片页面选择并下载以下图片，用于 App 中的离线调色示例。摄影作品不是本项目创作，也不是 AI 生成。

| 文件 | 作者 | 来源 |
|---|---|---|
| sample_lake.jpg | Kata | https://www.pexels.com/photo/mountain-landscape-with-lake-14958494/ |
| sample_forest.jpg | Aysegul Aytoren | https://www.pexels.com/photo/a-forest-with-mossy-rocks-14755971/ |
| sample_coffee.jpg | Negative Space | https://www.pexels.com/photo/caffeine-coffee-cup-mug-134577/ |
| sample_city.jpg | Mo Eid | https://www.pexels.com/photo/drone-shot-of-city-with-skyscrapers-17910086/ |

许可：https://www.pexels.com/license/ 。该许可允许在应用中使用、修改照片，禁止将照片作为图库或壁纸平台重新分发销售等用途。咖啡照片页面另标记为 CC0。App 显示摄影师、原始页面及许可链接。

本地文件位于 `Sources/Resources/`。从图片 CDN 获取 sRGB JPEG，保留比例，最长边缩至 1600 像素，JPEG 质量 87，去除 EXIF；共约 1.7 MiB。仅做尺寸和编码处理，未额外调色。示例原片可能已经由摄影师调色，不代表未经处理的相机原片。

入口：相机页或相册页点击“智能调色”，编辑器顶部选择内置照片。资源随应用安装，无需相册权限。浏览和本地调色无需网络，AI 分析仍需点击并联网。
