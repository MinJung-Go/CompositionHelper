import Foundation
import UIKit
import ImageIO
import Photos

/// Runs decoding and pixel work outside the main actor.
actor ColorPhotoService {
    struct Rendered {
        let image: UIImage
        let jpeg: Data
        let changedPercent: Double
        let meanDifference: Double
    }
    func decode(_ data: Data, preview: Bool, previewLimit: Int = 1400) throws -> CGImage {
        guard let source = CGImageSourceCreateWithData(data as CFData, [kCGImageSourceShouldCache:false] as CFDictionary),
              let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
              let width = properties[kCGImagePropertyPixelWidth] as? Int,
              let height = properties[kCGImagePropertyPixelHeight] as? Int,
              width > 0, height > 0 else { throw ColorFailure("照片无法读取，请重新选择") }
        // Avoid terminating the process on a full-resolution allocation spike.
        guard preview || Double(width)*Double(height) <= 32_000_000 else {
            throw ColorFailure("原图超过 3200 万像素，暂不支持导出；不会降低分辨率保存")
        }
        let options: [CFString: Any] = [kCGImageSourceCreateThumbnailFromImageAlways:true,
            kCGImageSourceCreateThumbnailWithTransform:true,
            kCGImageSourceThumbnailMaxPixelSize:preview ? previewLimit : max(width,height)]
        guard let image = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary) else { throw ColorFailure("图片解码失败") }
        return image
    }
    func render(_ data: Data, plan: ColorPlan, strength: Double, preview: Bool = true, previewLimit: Int = 1400) throws -> Rendered {
        try Task.checkCancellation()
        let image = try decode(data, preview: preview, previewLimit: previewLimit)
        let w = image.width, h = image.height
        var pixels = [UInt8](repeating: 255, count: w*h*4)
        let space = CGColorSpace(name: CGColorSpace.sRGB)!
        let engine = ColorPixelEngine(plan: plan, strength: strength)
        var changed = 0, difference = 0.0
        let result: CGImage = try pixels.withUnsafeMutableBytes { buffer in
            guard let context = CGContext(data: buffer.baseAddress, width: w, height: h, bitsPerComponent: 8,
                bytesPerRow: w*4, space: space, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue) else {
                throw ColorFailure("无法分配图片内存")
            }
            context.setFillColor(CGColor(gray: 1, alpha: 1)); context.fill(CGRect(x:0,y:0,width:w,height:h))
            context.draw(image, in: CGRect(x:0,y:0,width:w,height:h))
            let bytes = buffer.bindMemory(to: UInt8.self)
            for y in 0..<h {
                try Task.checkCancellation()
                for x in 0..<w {
                    let i = (y*w+x)*4
                    let original = (0..<3).map { Double(bytes[i+$0])/255 }
                    let adjusted = engine.transform(original)
                    var pixelDifference = 0
                    for channel in 0..<3 {
                        let value = UInt8(min(255,max(0,(adjusted[channel]*255).rounded())))
                        pixelDifference += abs(Int(value)-Int(bytes[i+channel]))
                        bytes[i+channel] = value
                    }
                    if pixelDifference > 0 { changed += 1 }
                    difference += Double(pixelDifference)
                }
            }
            guard let output = context.makeImage() else { throw ColorFailure("生成预览失败") }
            return output
        }
        let uiImage = UIImage(cgImage: result)
        guard let jpeg = uiImage.jpegData(compressionQuality: preview ? 0.85 : 0.95) else { throw ColorFailure("图片编码失败") }
        return Rendered(image: uiImage, jpeg: jpeg, changedPercent: Double(changed)*100/Double(w*h), meanDifference:difference/Double(w*h*3))
    }
    func save(_ data: Data, plan: ColorPlan, strength: Double) async throws {
        let permission = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
        guard permission == .authorized || permission == .limited else { throw ColorFailure("请在系统设置中允许添加照片") }
        let output = try render(data, plan: plan, strength: strength, preview: false)
        try Task.checkCancellation()
        try await PHPhotoLibrary.shared().performChanges {
            PHAssetCreationRequest.forAsset().addResource(with: .photo, data: output.jpeg, options: nil)
        }
    }
}

private final class ColorNetworkDelegate: NSObject, URLSessionTaskDelegate {
    func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse,
                    newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) {
        completionHandler(nil)
    }
}

struct GeminiColorService {
    func analyze(jpeg: Data, key: String, model: String, onPreview: (ColorPlan) async throws -> Void = { _ in }) async throws -> ColorPlan {
        guard !key.isEmpty, key.utf8.allSatisfy({ $0 >= 33 && $0 <= 126 }),
              model.range(of: "^gemini-[A-Za-z0-9._-]{1,80}$", options:.regularExpression) != nil else { throw ColorFailure("请填写有效密钥和模型名称") }
        let config = URLSessionConfiguration.ephemeral
        config.timeoutIntervalForRequest = 150; config.timeoutIntervalForResource = 180
        let session = URLSession(configuration: config, delegate: ColorNetworkDelegate(), delegateQueue: nil)
        defer { session.invalidateAndCancel() }
        var request = URLRequest(url: URL(string:"https://generativelanguage.googleapis.com/v1beta/models/\(model):streamGenerateContent?alt=sse")!)
        request.httpMethod = "POST"
        request.setValue(key, forHTTPHeaderField:"x-goog-api-key")
        request.setValue("application/json", forHTTPHeaderField:"Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: [
            "contents":[["parts":[["text": Self.prompt], ["inline_data":["mime_type":"image/jpeg","data":jpeg.base64EncodedString()]]]]],
            "generationConfig":["temperature":0.15,"responseMimeType":"application/json","maxOutputTokens":8192]
        ])
        let (bytes, response) = try await session.bytes(for: request)
        guard let http = response as? HTTPURLResponse, http.statusCode == 200 else { throw ColorFailure("AI 请求失败（HTTP \((response as? HTTPURLResponse)?.statusCode ?? 0)），请检查密钥、模型或额度") }
        var decoder = ColorStreamDecoder()
        var line = Data()
        var total = 0
        for try await byte in bytes {
            try Task.checkCancellation()
            total += 1
            guard total <= 256000 else { throw ColorFailure("模型响应过长") }
            if byte == 10 {
                if line.last == 13 { line.removeLast() }
                guard let value = String(data: line, encoding: .utf8) else { throw ColorFailure("AI 响应编码无效") }
                if let partial = try decoder.line(value), !decoder.isComplete { try await onPreview(partial) }
                line.removeAll(keepingCapacity: true)
            } else { line.append(byte) }
        }
        guard line.isEmpty else { throw ColorFailure("AI 响应中断，请重试") }
        return try decoder.finish()
    }

    static let prompt = """

        Analyze this photograph as a professional Photoshop colorist. Produce a natural, restrained edit
        appropriate to its actual scene, subjects and lighting. Preserve skin, text and realistic water/foliage.
        No masks or spatial selections are available. Similar-colored objects are affected together.
        Return ONE JSON object in this order: basic, curve_y, hsl, color_balance, rgb_correction,
        scene and intent (short Chinese descriptions). Emit basic first so a preview can render early. Describe only adjustments the engine can actually perform. No markdown.
        Processing order, on gamma-encoded sRGB [0,1]:
        1) basic: exposure [-0.5,0.5] stops; contrast [-0.2,0.2]; shadows [-0.2,0.25];
        highlights [-0.25,0.15]; temperature [-0.05,0.05]; tint [-0.04,0.04]; saturation [-0.25,0.3].
        All default zero. y=.2126*r+.7152*g+.0722*b.
        tonal=shadows*(1-y)^2+highlights*y^2;
        rgb=(rgb*2^exposure+tonal+[temperature+tint/2,-tint,-temperature+tint/2]-.5)*(1+contrast)+.5;
        saturation multiplier 1+saturation about luminance, then clamp.
        2) curve_y: nine RGB output levels at x=[0,32,64,96,128,160,192,224,255], linear interpolation.
        Monotonic, endpoints 0 and 255, each point deviates at most 22. Identity curve is valid.
        3) hsl: array of {band,half_width,hue_shift,saturation_percent,lightness_points}.
        band MUST be a name: red,orange,yellow,green,aqua,blue,purple,magenta (centers 0,30,60,110,175,225,275,315).
        half_width 20..70 degrees; hue_shift -10..10 degrees; saturation_percent -25..25 relative percent;
        lightness_points -7..7 additive HSL lightness points. Cosine feather weights over circular hue distance,
        neutral protection smoothstep(.05,.25,saturation); normalize overlapping total weights by max(total,1).
        4) color_balance: {shadows:[r,g,b],midtones:[r,g,b],highlights:[r,g,b]} byte offsets -4..4.
        Weights (1-y)^2,4*y*(1-y),y^2 based on luminance after HSL. Zero offsets valid.
        5) rgb_correction: {mode:"direct" or "complement",global:[r,g,b],shadows:[r,g,b],
        midtones:[r,g,b],highlights:[r,g,b]}. All entries default zero, each [-0.08,0.08] in sRGB units.
        On clamped output of step 4, compute y=.2126*r+.7152*g+.0722*b once;
        axis_i=global_i+(1-y)^2*shadows_i+2*y*(1-y)*midtones_i+y^2*highlights_i.
        direct: add [axis_r,axis_g,axis_b] to RGB.
        complement: add [-axis_g-axis_b,-axis_r-axis_b,-axis_r-axis_g] to RGB. Then clamp.
        Thus positive red in direct mode raises R; positive red in complement mode lowers G and B.
        These methods change brightness differently and are NOT equivalent. Keep offsets subtle, usually <=0.02.
        Judge casts from plausible neutral objects and scene lighting. A lake or forest having little red is
        NOT evidence of a cast. Do not equalize RGB histograms or neutralize intentional warm/cool lighting.
        Use this only for a visible cast or justified stylistic intent; preserve natural water and skin.
        Avoid stacking it with white balance and color_balance to correct the same cast multiple times.
        Explain the chosen direction and tonal range briefly in Chinese intent. Zero is valid.
        Be conservative: avoid cumulative shadow/curve brightening, HDR look, cyan water and neon greens.
        For daylight landscapes start exposure near zero, shadows around .05.. .10 only when needed,
        and reduce blue saturation if already strong. Choose parameters per photo, not a fixed preset.

    """
}
