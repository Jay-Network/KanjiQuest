import SwiftUI

/// Floating action button for feedback. Mirrors Android's FeedbackFAB.kt.
struct FeedbackFAB: View {
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "envelope.fill")
                .font(.system(size: 20))
                .foregroundColor(KanjiQuestTheme.onPrimaryContainer)
                .frame(width: 56, height: 56)
                .background(KanjiQuestTheme.primaryContainer)
                .clipShape(Circle())
                .shadow(color: .black.opacity(0.15), radius: 4, y: 2)
        }
    }
}
