import Foundation

/// HTTP client for Gemini API calls (writing feedback, AI grading).
/// Mirrors Android's GeminiClient.
class GeminiClient {
    let apiKey: String

    init(apiKey: String) {
        self.apiKey = apiKey
    }

    func generateContent(prompt: String, imageData: Data? = nil) async throws -> String {
        let url = URL(string: "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=\(apiKey)")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        var parts: [[String: Any]] = [["text": prompt]]

        if let imageData = imageData {
            let base64 = imageData.base64EncodedString()
            parts.append([
                "inline_data": [
                    "mime_type": "image/png",
                    "data": base64
                ]
            ])
        }

        let body: [String: Any] = [
            "contents": [["parts": parts]]
        ]

        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, _) = try await URLSession.shared.data(for: request)
        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        let candidates = json?["candidates"] as? [[String: Any]]
        let content = candidates?.first?["content"] as? [String: Any]
        let responseParts = content?["parts"] as? [[String: Any]]
        return responseParts?.first?["text"] as? String ?? ""
    }
}
