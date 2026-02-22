import Foundation

/// Reports AI handwriting feedback analytics.
/// Mirrors Android's AiFeedbackReporter.
class AiFeedbackReporter {
    func reportFeedback(kanjiLiteral: String, isCorrect: Bool, confidence: Double, feedback: String) {
        #if DEBUG
        print("[AiFeedback] \(kanjiLiteral): correct=\(isCorrect), confidence=\(confidence), feedback=\(feedback)")
        #endif
        // TODO: Send report to backend analytics
    }

    func submitReport(_ kanji: String, _ feedback: HandwritingFeedback) {
        reportFeedback(
            kanjiLiteral: kanji,
            isCorrect: feedback.isAvailable,
            confidence: Double(feedback.qualityRating) / 5.0,
            feedback: feedback.overallComment
        )
    }
}
