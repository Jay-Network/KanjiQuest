import Foundation

struct HandwritingFeedback {
    let overallComment: String
    let strokeFeedback: [String]
    let qualityRating: Int
    let isAvailable: Bool
    let analyzedImageBase64: String?

    static func unavailable(imageBase64: String? = nil) -> HandwritingFeedback {
        HandwritingFeedback(
            overallComment: "",
            strokeFeedback: [],
            qualityRating: 0,
            isAvailable: false,
            analyzedImageBase64: imageBase64
        )
    }
}

class HandwritingChecker {
    private let geminiClient: GeminiClient

    init(geminiClient: GeminiClient) {
        self.geminiClient = geminiClient
    }

    func evaluate(
        drawnStrokes: [[CGPoint]],
        targetKanji: String,
        strokeCount: Int,
        canvasSize: CGFloat,
        language: String = "en"
    ) async -> HandwritingFeedback {
        guard !drawnStrokes.isEmpty else {
            return .unavailable()
        }

        let imageBase64 = WritingStrokeRenderer.renderToBase64(drawnStrokes: drawnStrokes, canvasSize: canvasSize)

        let colorLegend = drawnStrokes.indices.map { i in
            "stroke \(i + 1) = \(WritingStrokeRenderer.getColorName(i))"
        }.joined(separator: ", ")

        let prompt = buildPrompt(
            kanji: targetKanji,
            strokeCount: strokeCount,
            language: language,
            drawnStrokeCount: drawnStrokes.count,
            colorLegend: colorLegend
        )

        do {
            let imageData = Data(base64Encoded: imageBase64)
            let response = try await geminiClient.generateContent(prompt: prompt, imageData: imageData)
            return parseResponse(response, imageBase64: imageBase64)
        } catch {
            return .unavailable(imageBase64: imageBase64)
        }
    }

    private func buildPrompt(
        kanji: String,
        strokeCount: Int,
        language: String,
        drawnStrokeCount: Int,
        colorLegend: String
    ) -> String {
        let langInstruction = language == "ja"
            ? "IMPORTANT: Respond entirely in Japanese (日本語で回答してください)."
            : "IMPORTANT: Respond entirely in English. Do NOT use Japanese except for technical terms (止め, はね, はらい)."

        let strokeInfo = drawnStrokeCount != strokeCount
            ? "The student drew \(drawnStrokeCount) stroke(s), but the correct kanji has \(strokeCount) stroke(s)."
            : "The student drew \(drawnStrokeCount) stroke(s), matching the expected stroke count."

        let legendInfo = colorLegend.isEmpty ? "" : "\nColor legend: \(colorLegend)"

        return """
        You are a Japanese calligraphy teacher evaluating a student's handwritten kanji on a smartphone touchscreen. The kanji is「\(kanji)」with \(strokeCount) stroke(s). Each stroke is color-coded and numbered at its starting point.

        \(strokeInfo)\(legendInfo)

        \(langInstruction)

        Focus on these 3 aspects IN ORDER OF IMPORTANCE:
        1. OVERALL BALANCE (バランス): Are the components proportioned correctly?
        2. STROKE ORDER (筆順): Check if stroke order follows standard conventions.
        3. STROKE ENDINGS (止め・はね・はらい): Check if strokes end correctly.

        DO NOT criticize wobbly lines or minor thickness variations (touchscreen writing).

        Rules:
        - ONLY discuss strokes visible in the image (at most \(drawnStrokeCount)).
        - Reference strokes by number and color.
        - If writing looks good, "strokes" array should be empty.
        - Be encouraging.

        Respond with ONLY this JSON:
        {"rating":3,"overall":"one sentence summary","strokes":["specific feedback for stroke N"]}

        rating: 1=wrong kanji, 2=significant issues, 3=acceptable, 4=good, 5=excellent.
        """
    }

    private func parseResponse(_ response: String, imageBase64: String?) -> HandwritingFeedback {
        guard let jsonStr = extractJson(response),
              let data = jsonStr.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return fallbackParse(response, imageBase64: imageBase64)
        }

        let rating = min(max(json["rating"] as? Int ?? 3, 1), 5)
        let overall = json["overall"] as? String ?? ""
        let strokes = (json["strokes"] as? [String])?.filter { !$0.isEmpty } ?? []

        return HandwritingFeedback(
            overallComment: overall,
            strokeFeedback: strokes,
            qualityRating: rating,
            isAvailable: true,
            analyzedImageBase64: imageBase64
        )
    }

    private func extractJson(_ text: String) -> String? {
        guard let start = text.firstIndex(of: "{"),
              let end = text.lastIndex(of: "}"),
              start < end else { return nil }
        return String(text[start...end])
    }

    private func fallbackParse(_ response: String, imageBase64: String?) -> HandwritingFeedback {
        let trimmed = response.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return .unavailable(imageBase64: imageBase64) }
        return HandwritingFeedback(
            overallComment: String(trimmed.prefix(200)),
            strokeFeedback: [],
            qualityRating: 3,
            isAvailable: true,
            analyzedImageBase64: imageBase64
        )
    }
}
