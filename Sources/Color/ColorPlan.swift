import Foundation

struct ColorFailure: LocalizedError {
    let message: String
    var errorDescription: String? { message }
    init(_ message: String) { self.message = message }
}

/// The same gamma-encoded sRGB recipe used by the Android renderer.
struct ColorPlan: Equatable {
    static let curveX: [Double] = [0,32,64,96,128,160,192,224,255]
    static let bands: [String: Double] = ["red":0,"orange":30,"yellow":60,"green":110,"aqua":175,"blue":225,"purple":275,"magenta":315]
    static let limits: [String: ClosedRange<Double>] = ["exposure": -1...1, "contrast": -0.5...0.5,
        "shadows": -0.4...0.4, "highlights": -0.4...0.4, "temperature": -0.2...0.2,
        "tint": -0.2...0.2, "saturation": -0.5...0.5]
    struct Band: Equatable {
        var name: String
        var width: Double = 40
        var hue: Double = 0
        var saturation: Double = 0
        var lightness: Double = 0
    }
    var basic: [String: Double] = [:]
    var curve = curveX
    var hsl: [Band] = []
    var balance = Array(repeating: Array(repeating: 0.0, count: 3), count: 3)
    var rgb = Array(repeating: Array(repeating: 0.0, count: 3), count: 4)
    var complement = false
    var scene = ""
    var intent = ""

    static func decode(_ data: Data) throws -> ColorPlan {
        guard data.count <= 64000,
              let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let basic = root["basic"] as? [String: Any] else { throw ColorFailure("缺少有效调色参数") }
        func number(_ value: Any?, _ range: ClosedRange<Double>, _ fallback: Double = 0) throws -> Double {
            guard let value = value else { return fallback }
            guard let n = value as? NSNumber, String(cString: n.objCType) != "c",
                  n.doubleValue.isFinite, range.contains(n.doubleValue) else { throw ColorFailure("调色参数类型或范围无效") }
            return n.doubleValue
        }
        func object(_ key: String) throws -> [String: Any] {
            guard let value = root[key] else { return [:] }
            guard let result = value as? [String: Any] else { throw ColorFailure("\(key) 格式无效") }
            return result
        }
        func vector(_ value: Any?, count: Int, range: ClosedRange<Double>) throws -> [Double] {
            guard let value = value else { return Array(repeating: 0, count: count) }
            guard let array = value as? [Any], array.count == count else { throw ColorFailure("通道数量无效") }
            return try array.map { try number($0, range) }
        }
        var plan = ColorPlan()
        for (key, range) in limits { plan.basic[key] = try number(basic[key], range) }
        if let curve = root["curve_y"] {
            plan.curve = try vector(curve, count: 9, range: 0...255)
            guard plan.curve.first == 0, plan.curve.last == 255,
                  zip(plan.curve, curveX).allSatisfy({ abs($0 - $1) <= 22 }),
                  zip(plan.curve, plan.curve.dropFirst()).allSatisfy({ $0 <= $1 }) else {
                throw ColorFailure("曲线必须单调且幅度合理")
            }
        }
        if let raw = root["hsl"] {
            guard let items = raw as? [[String: Any]], items.count <= 8 else { throw ColorFailure("HSL 格式无效") }
            for item in items {
                let name = (item["band"] as? String)?.lowercased() ?? bands.first(where: { $0.value == (item["band"] as? NSNumber)?.doubleValue })?.key ?? ""
                guard bands[name] != nil, !plan.hsl.contains(where: { $0.name == name }) else { throw ColorFailure("HSL 色域无效或重复") }
                plan.hsl.append(Band(name: name, width: try number(item["half_width"], 20...70, 40),
                    hue: try number(item["hue_shift"], -10...10), saturation: try number(item["saturation_percent"], -25...25),
                    lightness: try number(item["lightness_points"], -7...7)))
            }
        }
        let balance = try object("color_balance")
        for (i, key) in ["shadows","midtones","highlights"].enumerated() {
            plan.balance[i] = try vector(balance[key], count: 3, range: -4...4)
        }
        let rgb = try object("rgb_correction")
        let mode = rgb["mode"] as? String ?? "direct"
        guard mode == "direct" || mode == "complement" else { throw ColorFailure("RGB 模式无效") }
        plan.complement = mode == "complement"
        for (i, key) in ["global","shadows","midtones","highlights"].enumerated() {
            plan.rgb[i] = try vector(rgb[key], count: 3, range: -0.08...0.08)
        }
        plan.scene = String((root["scene"] as? String ?? "").prefix(1200))
        plan.intent = String((root["intent"] as? String ?? "").prefix(1200))
        return plan
    }

    var summary: String {
        var rows = basic.sorted { $0.key < $1.key }.filter { $0.value != 0 }.map { "\($0.key): \(String(format: "%+.4f", $0.value))" }
        if curve != Self.curveX { rows.append("曲线: " + curve.map { String(format: "%.1f", $0) }.joined(separator: ", ")) }
        for band in hsl where band.hue != 0 || band.saturation != 0 || band.lightness != 0 {
            rows.append(String(format: "HSL %@: 色相 %+.2f° / 饱和 %+.2f%% / 明度 %+.2f", band.name, band.hue, band.saturation, band.lightness))
        }
        for (i, values) in balance.enumerated() where values.contains(where: { $0 != 0 }) {
            rows.append("色彩平衡 \(["阴影","中间调","高光"][i]): " + values.map { String(format: "%+.3f", $0) }.joined(separator: ", "))
        }
        for (i, values) in rgb.enumerated() where values.contains(where: { $0 != 0 }) {
            rows.append("RGB \(["整体","阴影","中间调","高光"][i]) \(complement ? "互补" : "直接"): " + values.map { String(format: "%+.4f", $0) }.joined(separator: ", "))
        }
        return rows.isEmpty ? "当前方案没有调整参数，保留原图" : rows.joined(separator: "\n")
    }
}

struct ColorPixelEngine {
    let plan: ColorPlan
    let strength: Double
    private func clamp(_ v: Double) -> Double { min(1, max(0, v)) }
    func transform(_ input: [Double]) -> [Double] {
        if strength == 0 { return input }
        let a = plan.basic
        let y = 0.2126 * input[0] + 0.7152 * input[1] + 0.0722 * input[2]
        let tonal = (a["shadows"] ?? 0) * pow(1-y,2) + (a["highlights"] ?? 0) * y*y
        let temperature = a["temperature"] ?? 0, tint = a["tint"] ?? 0
        let offsets = [temperature+tint/2, -tint, -temperature+tint/2]
        let gain = pow(2.0, a["exposure"] ?? 0)
        let contrast = 1.0 + (a["contrast"] ?? 0)
        var c = [Double](repeating: 0, count: 3)
        for i in 0..<3 { c[i] = (input[i]*gain + tonal + offsets[i] - 0.5)*contrast + 0.5 }
        let grey = 0.2126*c[0] + 0.7152*c[1] + 0.0722*c[2]
        c = c.map { value in
            let x = clamp(grey + (value-grey)*(1+(a["saturation"] ?? 0))) * 255
            let i = min(7, Int(x/32)), xs = ColorPlan.curveX
            let t = (x-xs[i])/(xs[i+1]-xs[i])
            return (plan.curve[i]+t*(plan.curve[i+1]-plan.curve[i]))/255
        }
        if !plan.hsl.isEmpty {
            let hi = c.max()!, lo = c.min()!, delta = hi-lo
            var l = (hi+lo)/2
            var s = 1-abs(2*l-1) > 0.000001 ? delta/(1-abs(2*l-1)) : 0
            var h = 0.0
            if delta > 0.000001 {
                if hi == c[0] { h = ((c[1]-c[2])/delta*60+360).truncatingRemainder(dividingBy:360) }
                else if hi == c[1] { h = ((c[2]-c[0])/delta+2)*60 }
                else { h = ((c[0]-c[1])/delta+4)*60 }
            }
            let neutral = clamp((s-0.05)/0.2)
            var total = 0.0, dh = 0.0, ds = 0.0, dl = 0.0
            for band in plan.hsl {
                let distance = abs((h-ColorPlan.bands[band.name]!+540).truncatingRemainder(dividingBy:360)-180)
                let w = distance < band.width ? (0.5+0.5*cos(.pi*distance/band.width))*neutral*neutral*(3-2*neutral) : 0
                total += w; dh += w*band.hue; ds += w*band.saturation/100; dl += w*band.lightness/100
            }
            h = (h+dh/max(1,total)+360).truncatingRemainder(dividingBy:360)
            s = clamp(s*(1+ds/max(1,total))); l = clamp(l+dl/max(1,total))
            let chroma = (1-abs(2*l-1))*s, hp = h/60
            let x = chroma*(1-abs(hp.truncatingRemainder(dividingBy:2)-1)), m = l-chroma/2
            switch Int(hp) {
            case 0: c = [chroma,x,0]; case 1: c = [x,chroma,0]; case 2: c = [0,chroma,x]
            case 3: c = [0,x,chroma]; case 4: c = [x,0,chroma]; default: c = [chroma,0,x]
            }
            c = c.map { $0+m }
        }
        let lum = 0.2126*c[0]+0.7152*c[1]+0.0722*c[2]
        let shadowWeight = pow(1-lum,2), midWeight = 4*lum*(1-lum), highWeight = lum*lum
        for i in 0..<3 {
            let shift = shadowWeight*plan.balance[0][i] + midWeight*plan.balance[1][i] + highWeight*plan.balance[2][i]
            c[i] = clamp(c[i] + shift/255)
        }
        let yr = 0.2126*c[0]+0.7152*c[1]+0.0722*c[2]
        let sw = pow(1-yr,2), mw = 2*yr*(1-yr), hw = yr*yr
        var axes = [Double](repeating: 0, count: 3)
        for i in 0..<3 {
            axes[i] = plan.rgb[0][i] + sw*plan.rgb[1][i] + mw*plan.rgb[2][i] + hw*plan.rgb[3][i]
        }
        for i in 0..<3 {
            let offset = plan.complement ? -axes[(i+1)%3]-axes[(i+2)%3] : axes[i]
            c[i] = input[i] + (clamp(c[i]+offset)-input[i])*clamp(strength)
        }
        return c
    }
}
