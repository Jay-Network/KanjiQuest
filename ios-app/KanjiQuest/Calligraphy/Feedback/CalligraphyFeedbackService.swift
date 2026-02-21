import Foundation

/// AI calligraphy feedback using Gemini 2.5 Flash.
/// Evaluates 書道 technique including stroke endings (止め/はね/はらい),
/// brush pressure, tilt usage, and movement rhythm.
final class CalligraphyFeedbackService {

    private let apiKey: String
    private let endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    struct CalligraphyFeedback {
        let overallComment: String
        let strokeFeedback: [String]
        let qualityRating: Int           // 1-5
        let pressureFeedback: String     // 筆圧 evaluation
        let movementFeedback: String     // 運筆 evaluation
        let tomeScore: Int?              // 1-5 for 止め technique
        let haneScore: Int?              // 1-5 for はね technique
        let haraiScore: Int?             // 1-5 for はらい technique
        let balanceScore: Int?           // 1-5 for overall balance
        let isAvailable: Bool
    }

    init(apiKey: String) {
        self.apiKey = apiKey
    }

    func evaluate(
        strokes: [[CalligraphyPointData]],
        targetKanji: String,
        strokeCount: Int,
        canvasSize: CGSize
    ) async -> CalligraphyFeedback {
        guard !strokes.isEmpty, !apiKey.isEmpty else {
            return unavailable()
        }

        // Render pressure-aware image
        guard let imageBase64 = CalligraphyStrokeRenderer.renderToBase64(
            strokes: strokes, canvasSize: canvasSize
        ) else {
            return unavailable()
        }

        // Generate enhanced metadata with tilt, velocity, and stroke endings
        let analysis = PressureAnalyzer.analyze(strokes: strokes)
        let metadata = PressureAnalyzer.generateTextMetadata(from: analysis)

        let prompt = buildCalligraphyPrompt(
            kanji: targetKanji,
            strokeCount: strokeCount,
            pressureMetadata: metadata
        )

        do {
            let response = try await callGemini(prompt: prompt, imageBase64: imageBase64)
            return parseResponse(response)
        } catch {
            return unavailable()
        }
    }

    // MARK: - Prompt

    private func buildCalligraphyPrompt(
        kanji: String,
        strokeCount: Int,
        pressureMetadata: String
    ) -> String {
        """
        You are a Japanese 書道 (calligraphy) master evaluating a student's work written with Apple Pencil on iPad. The kanji is「\(kanji)」with \(strokeCount) stroke(s). The image shows pressure-and-tilt-aware rendering where thicker lines = more pressure, and elliptical brush shapes show pencil tilt angle.

        SENSOR DATA (from Apple Pencil — pressure, tilt altitude/azimuth, velocity, stroke endings):
        \(pressureMetadata)

        IMPORTANT: The stroke ending detector has already analyzed each stroke's ending pattern from raw sensor data. The detected types (tome/hane/harai) and confidence scores are included above. Use these as INPUT data alongside the visual image.

        Evaluate these aspects with STRICT 書道 criteria:

        1. OVERALL BALANCE (バランス) [score 1-5]:
           Proportions, centering, spacing of components, stroke length ratios.
           5: Master — perfect proportions, centered, harmonious spacing
           4: Good — mostly balanced, minor proportion issues
           3: Acceptable — recognizable but noticeably off-balance
           2: Needs work — significant proportion/spacing errors
           1: Beginner — major balance issues

        2. STROKE ORDER (筆順):
           Standard Japanese conventions (top→bottom, left→right, horizontal before vertical).

        3. STROKE ENDINGS — evaluate EACH applicable type:

           止め (tome) [score 1-5]: Firm stop technique
           5: Clean stop with steady pressure, no wobble, confident halt
           4: Good stop, minor pressure instability
           3: Recognizable stop but pressure fades or wobbles
           2: Weak stop, inconsistent pressure at end
           1: No discernible stop technique

           はね (hane) [score 1-5]: Upward flick technique
           5: Crisp flick with clear pressure dip→spike, correct direction
           4: Good flick, slightly weak or off-angle
           3: Attempt visible but pressure pattern unclear
           2: Weak or absent flick
           1: No discernible flick

           はらい (harai) [score 1-5]: Gradual taper technique
           5: Smooth monotonic pressure decrease, natural taper to nothing
           4: Good taper with minor pressure bumps
           3: Taper present but irregular or too abrupt
           2: Pressure drops suddenly instead of tapering
           1: No discernible taper

           Only score ending types that appear in this kanji. Set others to null.

        4. BRUSH PRESSURE (筆圧):
           - Natural pressure variation (not robotic constant pressure)
           - Appropriate pressure curves for each stroke type
           - Starting pressure: moderate entry
           - Horizontal strokes: slight pressure swell in middle
           - Ending matches the stroke ending type

        5. BRUSH MOVEMENT (運筆):
           - Flow and rhythm between strokes
           - Velocity consistency within strokes
           - Appropriate speed: not rushed, not labored
           - Duration proportional to stroke complexity

        6. TILT USAGE (筆角):
           - Consistent tilt angle (altitude variance should be low)
           - Appropriate tilt for stroke types
           - Higher altitude (perpendicular) for thin strokes, lower for broad strokes

        Be encouraging but honest. Praise what is done well. Give specific, actionable feedback.

        Respond with ONLY this JSON (no markdown, no other text):
        {"rating":3,"overall":"summary","strokes":["per-stroke feedback"],"pressure":"筆圧 eval","movement":"運筆 eval","tome_score":null,"hane_score":null,"harai_score":null,"balance_score":3}

        rating: 1-5 overall. tome_score/hane_score/harai_score: 1-5 or null if not applicable. balance_score: 1-5.
        """
    }

    // MARK: - Gemini API

    private func callGemini(prompt: String, imageBase64: String) async throws -> String {
        guard let url = URL(string: "\(endpoint)?key=\(apiKey)") else {
            throw CalligraphyError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 30

        let body: [String: Any] = [
            "contents": [[
                "parts": [
                    ["text": prompt],
                    ["inline_data": [
                        "mime_type": "image/png",
                        "data": imageBase64
                    ]]
                ]
            ]]
        ]

        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse,
              (200...299).contains(httpResponse.statusCode) else {
            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? 0
            throw CalligraphyError.httpError(statusCode)
        }

        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let candidates = json["candidates"] as? [[String: Any]],
              let content = candidates.first?["content"] as? [String: Any],
              let parts = content["parts"] as? [[String: Any]],
              let text = parts.first?["text"] as? String else {
            throw CalligraphyError.parseError
        }

        return text
    }

    // MARK: - Response Parsing

    private func parseResponse(_ response: String) -> CalligraphyFeedback {
        guard let jsonStr = extractJson(response),
              let data = jsonStr.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return fallbackParse(response)
        }

        let rating = (json["rating"] as? Int ?? 3).clamped(to: 1...5)
        let overall = json["overall"] as? String ?? ""
        let pressure = json["pressure"] as? String ?? ""
        let movement = json["movement"] as? String ?? ""

        let tomeScore = (json["tome_score"] as? Int)?.clamped(to: 1...5)
        let haneScore = (json["hane_score"] as? Int)?.clamped(to: 1...5)
        let haraiScore = (json["harai_score"] as? Int)?.clamped(to: 1...5)
        let balanceScore = (json["balance_score"] as? Int)?.clamped(to: 1...5)

        var strokeFeedback: [String] = []
        if let strokes = json["strokes"] as? [String] {
            strokeFeedback = strokes.filter { !$0.isEmpty }
        }

        return CalligraphyFeedback(
            overallComment: overall,
            strokeFeedback: strokeFeedback,
            qualityRating: rating,
            pressureFeedback: pressure,
            movementFeedback: movement,
            tomeScore: tomeScore,
            haneScore: haneScore,
            haraiScore: haraiScore,
            balanceScore: balanceScore,
            isAvailable: true
        )
    }

    private func extractJson(_ text: String) -> String? {
        guard let start = text.firstIndex(of: "{"),
              let end = text.lastIndex(of: "}"),
              start < end else { return nil }
        return String(text[start...end])
    }

    private func fallbackParse(_ response: String) -> CalligraphyFeedback {
        let trimmed = response.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return unavailable() }
        return CalligraphyFeedback(
            overallComment: String(trimmed.prefix(200)),
            strokeFeedback: [],
            qualityRating: 3,
            pressureFeedback: "",
            movementFeedback: "",
            tomeScore: nil,
            haneScore: nil,
            haraiScore: nil,
            balanceScore: nil,
            isAvailable: true
        )
    }

    private func unavailable() -> CalligraphyFeedback {
        CalligraphyFeedback(
            overallComment: "",
            strokeFeedback: [],
            qualityRating: 0,
            pressureFeedback: "",
            movementFeedback: "",
            tomeScore: nil,
            haneScore: nil,
            haraiScore: nil,
            balanceScore: nil,
            isAvailable: false
        )
    }
}

enum CalligraphyError: Error {
    case invalidURL
    case httpError(Int)
    case parseError
}

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}

private extension Optional where Wrapped: Comparable {
    func clamped(to range: ClosedRange<Wrapped>) -> Wrapped? {
        guard let value = self else { return nil }
        return value.clamped(to: range)
    }
}
