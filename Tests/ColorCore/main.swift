import Foundation

var checks = 0
func check(_ condition: @autoclosure () -> Bool, _ name: String) {
    checks += 1
    if !condition() { fatalError("FAIL: \(name)") }
}
func decode(_ text: String) throws -> ColorPlan { try ColorPlan.decode(Data(text.utf8)) }
func rejects(_ text: String) -> Bool { do { _ = try decode(text); return false } catch { return true } }
let identity = try decode("{\"basic\":{}}")
let pixel = [0.25,0.5,0.75]
let unchanged = ColorPixelEngine(plan:identity,strength:1).transform(pixel)
check(zip(unchanged,pixel).allSatisfy { abs($0-$1) < 1e-12 }, "identity")
var red = identity; red.rgb[0][0] = 0.02
let direct = ColorPixelEngine(plan:red,strength:1).transform(pixel)
check(abs(direct[0]-0.27) < 1e-12 && direct[1] == pixel[1], "direct red")
red.complement = true
let complement = ColorPixelEngine(plan:red,strength:1).transform(pixel)
check(abs(complement[1]-0.48) < 1e-12 && complement[0] == pixel[0], "complement red")
check(ColorPixelEngine(plan:red,strength:0).transform(pixel) == pixel,"zero strength")
var exposure = identity; exposure.basic["exposure"] = 0.2
check(ColorPixelEngine(plan:exposure,strength:1).transform(pixel) != pixel,"RGB zero does not imply no edit")
check(exposure.summary.contains("exposure"),"summary includes basic")
red.rgb[0][0] = 0.004
check(red.summary.contains("0.0040"),"small RGB not rounded to zero")
check(rejects("{\"basic\":{\"exposure\":true}}"),"reject boolean number")
check(rejects("{\"basic\":{\"contrast\":999}}"),"reject out of range")
check(rejects("{\"basic\":{},\"curve_y\":[0,32]}"),"reject short curve")
check(rejects("{\"basic\":{},\"hsl\":[{\"band\":\"water\"}]}"),"reject bad hue band")
check(rejects("{\"basic\":{},\"rgb_correction\":{\"global\":[1,0,0]}}"),"reject RGB range")
let fixture = URL(fileURLWithPath:CommandLine.arguments[1])
let cases = try JSONSerialization.jsonObject(with:Data(contentsOf:fixture)) as! [[String:Any]]
for (index,item) in cases.enumerated() {
    let plan = try ColorPlan.decode(JSONSerialization.data(withJSONObject:item["plan"]!))
    let engine = ColorPixelEngine(plan:plan,strength:1)
    let inputs = item["input"] as! [[Double]]
    let expected = item["expected"] as! [[Double]]
    for (i,input) in inputs.enumerated() {
        let actual = engine.transform(input.map { $0/255 }).map { ($0*255).rounded() }
        check(zip(actual,expected[i]).allSatisfy { abs($0-$1) <= 1 },"Python parity \(index)/\(i)")
    }
}
print("PASS: \(checks) checks (recipe validation, RGB semantics, precision, cross-platform pixels)")
