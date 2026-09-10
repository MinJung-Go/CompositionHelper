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
func streamEvent(_ text: String, finish: String? = nil, thought: Bool = false) throws -> String {
    var candidate: [String: Any] = ["index": 0, "content": ["parts": [["text": text, "thought": thought]]]]
    if let finish = finish { candidate["finishReason"] = finish }
    return String(data: try JSONSerialization.data(withJSONObject: ["candidates": [candidate]]), encoding: .utf8)!
}
func send(_ decoder: inout ColorStreamDecoder, _ text: String, finish: String? = nil, thought: Bool = false) throws -> ColorPlan? {
    _ = try decoder.line("data: " + streamEvent(text, finish: finish, thought: thought))
    return try decoder.line("")
}
func streamRejects(_ operation: () throws -> Void) -> Bool { do { try operation(); return false } catch { return true } }
var stream = ColorStreamDecoder()
let incomplete = try send(&stream, #"{"basic":{"exposure":0."#)
check(incomplete == nil, "no incomplete number preview")
let partial = try send(&stream, #"2},"curve_y":[0,32"#)
check(partial?.basic["exposure"] == 0.2, "complete basic arrives before curve")
check(partial?.curve == ColorPlan.curveX, "unfinished curve not used")
_ = try send(&stream, ",64,96,128,160,192,224,255]}", finish: "STOP")
let finalStream = try stream.finish()
check(finalStream.basic["exposure"] == 0.2, "final stream recipe")
var unicodeStream = ColorStreamDecoder()
_ = try send(&unicodeStream, "ignore thought", thought: true)
let unicodeJSON = #"{"basic":{},"intent":"湖泊，含\"引号\"和}逗号,","hsl":[]}"#
for c in unicodeJSON { _ = try send(&unicodeStream, String(c)) }
_ = try send(&unicodeStream, "", finish: "STOP")
let unicodePlan = try unicodeStream.finish()
check(unicodePlan.intent == "湖泊，含\"引号\"和}逗号,", "escaped and Unicode chunks")
check(streamRejects { var d = ColorStreamDecoder(); _ = try send(&d, #"{"basic":{}}"#); _ = try d.finish() }, "missing STOP")
check(streamRejects { var d = ColorStreamDecoder(); _ = try send(&d, #"{"basic":{},"curve_y":[0"#, finish: "STOP"); _ = try d.finish() }, "truncated JSON with STOP")
check(streamRejects { var d = ColorStreamDecoder(); _ = try send(&d, "", finish: "MAX_TOKENS") }, "token limit")
check(streamRejects { var d = ColorStreamDecoder(); _ = try d.line(#"data: {"promptFeedback":{"blockReason":"SAFETY"}}"#); _ = try d.line("") }, "blocked prompt")
check(streamRejects { var d = ColorStreamDecoder(); _ = try send(&d, #"{"basic":{},"#); _ = try send(&d, #""rgb_correction":{"global":[99,0,0]}}"#, finish: "STOP"); _ = try d.finish() }, "invalid later group")
check(streamRejects { var d = ColorStreamDecoder(); _ = try d.line("data: " + String(repeating: "x", count: 256001)) }, "bounded payload")
check(streamRejects { var d = ColorStreamDecoder(); _ = try d.line("data: [DONE]"); _ = try d.line(""); _ = try d.finish() }, "DONE is not STOP")
check(streamRejects { var d = ColorStreamDecoder(); _ = try d.line("data: " + streamEvent(#"{"basic":{}}"#, finish: "STOP")); _ = try d.finish() }, "unterminated event")
var multi = ColorStreamDecoder()
_ = try multi.line(": ping"); _ = try multi.line("")
let multiPayload = try streamEvent(#"{"basic":{}}"#, finish: "STOP")
_ = try multi.line("data: {"); _ = try multi.line("data: " + multiPayload.dropFirst()); _ = try multi.line("")
_ = try multi.line(#"data: {"usageMetadata":{}}"#); _ = try multi.line("")
let multiResult = try multi.finish()
check(multiResult == identity, "multiline SSE and trailing usage metadata")
print("PASS: \(checks) checks (recipe validation, RGB semantics, precision, cross-platform pixels)")
