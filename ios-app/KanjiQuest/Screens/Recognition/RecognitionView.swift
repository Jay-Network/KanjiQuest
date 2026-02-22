import SwiftUI
import SharedCore

struct RecognitionView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = RecognitionViewModel()
    @Environment(\.dismiss) private var dismiss

    let targetKanjiId: Int32?

    init(targetKanjiId: Int32? = nil) {
        self.targetKanjiId = targetKanjiId
    }

    var body: some View {
        ZStack {
            KanjiQuestTheme.background.ignoresSafeArea()

            switch viewModel.gameState {
            case is GameState.Idle, is GameState.Preparing:
                LoadingContent()

            case let awaiting as GameState.AwaitingAnswer:
                QuestionContent(
                    question: awaiting.question,
                    questionNumber: Int(awaiting.questionNumber),
                    totalQuestions: Int(awaiting.totalQuestions),
                    currentCombo: Int(awaiting.currentCombo),
                    sessionXp: Int(awaiting.sessionXp),
                    selectedAnswer: nil,
                    onAnswerSelected: { answer in
                        Task { await viewModel.submitAnswer(answer) }
                    }
                )

            case let showing as GameState.ShowingResult:
                QuestionContent(
                    question: showing.question,
                    questionNumber: Int(showing.questionNumber),
                    totalQuestions: Int(showing.totalQuestions),
                    currentCombo: Int(showing.currentCombo),
                    sessionXp: Int(showing.sessionXp),
                    selectedAnswer: showing.selectedAnswer,
                    isCorrect: showing.isCorrect,
                    xpGained: Int(showing.xpGained),
                    onAnswerSelected: { _ in },
                    onNext: {
                        Task { await viewModel.nextQuestion() }
                    }
                )

            case let complete as GameState.SessionComplete:
                SessionCompleteContent(
                    stats: complete.stats,
                    onDone: {
                        viewModel.reset()
                        dismiss()
                    }
                )

            case let error as GameState.Error:
                ErrorContent(message: error.message) {
                    dismiss()
                }

            default:
                LoadingContent()
            }
        }
        .navigationTitle("Recognition")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "xmark")
                }
            }
        }
        .task {
            viewModel.setup(container: container)
            if viewModel.gameState is GameState.Idle {
                await viewModel.startGame(targetKanjiId: targetKanjiId)
            }
        }
    }
}

// MARK: - Loading Content

private struct LoadingContent: View {
    var body: some View {
        VStack(spacing: KanjiQuestTheme.spacingM) {
            ProgressView()
                .scaleEffect(1.5)

            Text("Preparing questions...")
                .font(KanjiQuestTheme.titleMedium)
                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
        }
    }
}

// MARK: - Question Content

private struct QuestionContent: View {
    let question: Question
    let questionNumber: Int
    let totalQuestions: Int
    let currentCombo: Int
    let sessionXp: Int
    let selectedAnswer: String?
    var isCorrect: Bool? = nil
    var xpGained: Int = 0
    let onAnswerSelected: (String) -> Void
    var onNext: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: KanjiQuestTheme.spacingL) {
            // Progress header
            HStack {
                Text("\(questionNumber) / \(totalQuestions)")
                    .font(KanjiQuestTheme.bodyMedium)

                Spacer()

                if currentCombo > 1 {
                    Text("\(currentCombo)x combo")
                        .font(KanjiQuestTheme.bodyMedium)
                        .fontWeight(.bold)
                        .foregroundColor(KanjiQuestTheme.tertiary)
                }

                Spacer()

                Text("\(sessionXp) XP")
                    .font(KanjiQuestTheme.bodyMedium)
                    .foregroundColor(KanjiQuestTheme.primary)
            }

            // Progress bar
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(KanjiQuestTheme.surfaceVariant)

                    RoundedRectangle(cornerRadius: 4)
                        .fill(KanjiQuestTheme.primary)
                        .frame(width: geo.size.width * CGFloat(questionNumber) / CGFloat(totalQuestions))
                }
            }
            .frame(height: 8)

            Spacer()

            // Question prompt
            Text("What is the reading?")
                .font(KanjiQuestTheme.titleMedium)
                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)

            // Large kanji display — responsive sizing for iPad/iPhone
            ZStack {
                RoundedRectangle(cornerRadius: KanjiQuestTheme.radiusL)
                    .fill(KanjiQuestTheme.surface)
                    .shadow(color: .black.opacity(0.1), radius: 8, y: 4)

                Text(question.kanjiLiteral)
                    .font(KanjiQuestTheme.kanjiDisplay)
                    .minimumScaleFactor(0.6)
                    .foregroundColor(KanjiQuestTheme.onSurface)
            }
            .frame(maxWidth: 300)
            .aspectRatio(1.0, contentMode: .fit)

            // XP feedback
            if let correct = isCorrect {
                Text(correct ? "+\(xpGained) XP" : "Incorrect")
                    .font(KanjiQuestTheme.titleLarge)
                    .fontWeight(.bold)
                    .foregroundColor(correct ? KanjiQuestTheme.tertiary : KanjiQuestTheme.error)
                    .transition(.scale.combined(with: .opacity))
            }

            Spacer()

            // Answer choices (2x2 grid)
            VStack(spacing: KanjiQuestTheme.spacingM) {
                ForEach(Array(question.choices.chunked(into: 2).enumerated()), id: \.offset) { _, row in
                    HStack(spacing: KanjiQuestTheme.spacingM) {
                        ForEach(row, id: \.self) { choice in
                            AnswerButton(
                                choice: choice,
                                selectedAnswer: selectedAnswer,
                                correctAnswer: question.correctAnswer,
                                isDisabled: selectedAnswer != nil,
                                onTap: { onAnswerSelected(choice) }
                            )
                        }
                    }
                }
            }

            // Next button (after answer)
            if onNext != nil {
                Button {
                    onNext?()
                } label: {
                    Text("Next")
                        .font(KanjiQuestTheme.labelLarge)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(KanjiQuestTheme.primary)
                        .foregroundColor(.white)
                        .cornerRadius(KanjiQuestTheme.radiusM)
                }
            }
        }
        .padding()
        .animation(.easeInOut(duration: 0.3), value: isCorrect)
    }
}

// MARK: - Answer Button

private struct AnswerButton: View {
    let choice: String
    let selectedAnswer: String?
    let correctAnswer: String
    let isDisabled: Bool
    let onTap: () -> Void

    private var backgroundColor: Color {
        guard let selected = selectedAnswer else {
            return KanjiQuestTheme.primary
        }

        if choice == correctAnswer {
            return Color.green
        } else if choice == selected {
            return KanjiQuestTheme.error
        } else {
            return KanjiQuestTheme.surfaceVariant
        }
    }

    var body: some View {
        Button(action: onTap) {
            Text(choice)
                .font(.system(size: 20))
                .fontWeight(.medium)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(backgroundColor)
                .cornerRadius(KanjiQuestTheme.radiusM)
        }
        .disabled(isDisabled)
        .opacity(isDisabled && choice != correctAnswer && choice != selectedAnswer ? 0.6 : 1.0)
    }
}

// MARK: - Session Complete Content

private struct SessionCompleteContent: View {
    let stats: SessionStats
    let onDone: () -> Void

    var body: some View {
        VStack(spacing: KanjiQuestTheme.spacingXL) {
            Spacer()

            Text("Session Complete!")
                .font(KanjiQuestTheme.headlineMedium)
                .fontWeight(.bold)

            // Stats card
            VStack(spacing: KanjiQuestTheme.spacingM) {
                StatRow(label: "Cards Studied", value: "\(stats.cardsStudied)")
                StatRow(label: "Correct", value: "\(stats.correctCount) / \(stats.cardsStudied)")
                StatRow(label: "Accuracy", value: "\(Int(Double(stats.correctCount) / Double(max(stats.cardsStudied, 1)) * 100))%")
                StatRow(label: "Best Combo", value: "\(stats.comboMax)x")
                StatRow(label: "XP Earned", value: "+\(stats.xpEarned)")
                StatRow(label: "Duration", value: formatDuration(Int(stats.durationSec)))
            }
            .padding()
            .background(KanjiQuestTheme.surface)
            .cornerRadius(KanjiQuestTheme.radiusL)
            .shadow(color: .black.opacity(0.05), radius: 4, y: 2)

            Spacer()

            Button(action: onDone) {
                Text("Done")
                    .font(KanjiQuestTheme.labelLarge)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(KanjiQuestTheme.primary)
                    .foregroundColor(.white)
                    .cornerRadius(KanjiQuestTheme.radiusM)
            }
        }
        .padding()
    }

    private func formatDuration(_ seconds: Int) -> String {
        let minutes = seconds / 60
        let secs = seconds % 60
        return minutes > 0 ? "\(minutes)m \(secs)s" : "\(secs)s"
    }
}

// MARK: - Stat Row

private struct StatRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .font(KanjiQuestTheme.bodyLarge)
                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)

            Spacer()

            Text(value)
                .font(KanjiQuestTheme.bodyLarge)
                .fontWeight(.bold)
        }
    }
}

// MARK: - Error Content

private struct ErrorContent: View {
    let message: String
    let onBack: () -> Void

    var body: some View {
        VStack(spacing: KanjiQuestTheme.spacingL) {
            Spacer()

            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 48))
                .foregroundColor(KanjiQuestTheme.error)

            Text(message)
                .font(KanjiQuestTheme.bodyLarge)
                .multilineTextAlignment(.center)
                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)

            Spacer()

            Button(action: onBack) {
                Text("Go Back")
                    .font(KanjiQuestTheme.labelLarge)
                    .padding(.horizontal, 32)
                    .padding(.vertical, 12)
                    .overlay(
                        RoundedRectangle(cornerRadius: KanjiQuestTheme.radiusM)
                            .stroke(KanjiQuestTheme.primary, lineWidth: 2)
                    )
            }
        }
        .padding()
    }
}

// MARK: - Array Extension

private extension Array {
    func chunked(into size: Int) -> [[Element]] {
        stride(from: 0, to: count, by: size).map {
            Array(self[$0..<Swift.min($0 + size, count)])
        }
    }
}
