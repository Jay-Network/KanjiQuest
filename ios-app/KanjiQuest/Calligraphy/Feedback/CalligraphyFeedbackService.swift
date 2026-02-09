import Foundation

/// AI calligraphy feedback using Gemini 2.5 Flash.
/// Extends the Android HandwritingChecker with 2 additional evaluation aspects:
/// 筆圧 (brush pressure) and 運筆 (brush movement/rhythm).
final class CalligraphyFeedbackService {

    private let apiKey: String
    private let endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    struct CalligraphyFeedback {
        let overallComment: String
        let strokeFeedback: [String]
        let qualityRating: Int           // 1-5
        let pressureFeedback: String     // 筆圧 evaluation
        let movementFeedback: String     // 運筆 evaluation
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

        // Generate pressure metadata
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
        You are a Japanese 書道 (calligraphy) master evaluating a student's work written with Apple Pencil on iPad. The kanji is「\(kanji)」with \(strokeCount) stroke(s). The image shows pressure-aware rendering where thicker lines = more pressure.

        PRESSURE & MOVEMENT DATA (from Apple Pencil sensors):
        \(pressureMetadata)

        Evaluate these 5 aspects IN ORDER OF IMPORTANCE:

        1. OVERALL BALANCE (バランス): Proportions, centering, spacing of components.

        2. STROKE ORDER (筆順): Standard Japanese conventions (top→bottom, left→right, horizontal before vertical).

        3. STROKE ENDINGS (止め・はね・はらい):
           - 止め (tome): Firm stop — pressure should remain steady at end
           - はね (hane): Upward flick — pressure should decrease then spike briefly
           - はらい (harai): Gradual taper — pressure should decrease smoothly to near-zero

        4. BRUSH PRESSURE (筆圧): Is pressure variation natural? Does it follow 書道 conventions?
           - Strokes should generally start with moderate pressure
           - Horizontal strokes often have slight pressure increase in middle
           - Ending pressure pattern should match the stroke ending type
           - Look for unnatural constant pressure (robotic) vs natural variation

        5. BRUSH MOVEMENT (運筆): Flow, rhythm, and speed variation.
           - Even spacing between points = steady movement (good for 横画)
           - Acceleration at ends = confident はらい
           - Duration proportional to stroke length = good rhythm
           - Very fast strokes may indicate carelessness

        IMPORTANT: This is iPad + Apple Pencil, so pressure data is REAL and meaningful. Unlike smartphone evaluation, you CAN and SHOULD evaluate brush pressure and movement.

        Be encouraging. Praise what is done well before noting issues. Only discuss visible strokes.

        Respond with ONLY this JSON (no other text):
        {"rating":3,"overall":"summary","strokes":["stroke-specific feedback"],"pressure":"筆圧 evaluation","movement":"運筆 evaluation"}

        rating: 1=major errors, 2=significant issues, 3=acceptable, 4=good technique, 5=excellent 書道 form.
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
