import SwiftUI
import SharedCore

/// Vocabulary game screen. Mirrors Android's VocabularyScreen.kt.
/// Shows vocabulary questions with different types (meaning, reading, kanji_fill, sentence).
struct VocabularyView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = VocabularyViewModel()
    var targetKanjiId: Int32? = nil
    var onBack: () -> Void = {}

    var body: some View {
        ZStack {
            KanjiQuestTheme.background.ignoresSafeArea()

            if let error = viewModel.errorMessage {
                errorContent(error)
            } else if viewModel.sessionComplete {
                sessionCompleteView
            } else if let question = viewModel.currentQuestion {
                questionView(question)
            } else {
                loadingContent
            }

            if viewModel.showDiscovery, let item = viewModel.discoveredItem {
                DiscoveryOverlay(
                    discoveredItem: item,
                    kanjiLiteral: viewModel.currentQuestion?.kanjiLiteral,
                    kanjiMeaning: nil,
                    onDismiss: { viewModel.showDiscovery = false }
                )
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left"); Text("Back")
                }.foregroundColor(.white)
            }
            ToolbarItem(placement: .principal) {
                Text("Vocabulary").font(.headline).foregroundColor(.white)
            }
        }
        .toolbarBackground(KanjiQuestTheme.primary, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .task {
            if viewModel.currentQuestion == nil && !viewModel.sessionComplete {
                viewModel.start(container: container, targetKanjiId: targetKanjiId)
            }
        }
    }

    // MARK: - Loading

    private var loadingContent: some View {
        VStack(spacing: KanjiQuestTheme.spacingM) {
            Text("Preparing vocabulary...").font(KanjiQuestTheme.titleMedium)
            ProgressView()
        }
    }

    // MARK: - Question

    private func questionView(_ question: Question) -> some View {
        ScrollView {
            VStack(spacing: 0) {
                // Progress
                progressRow.padding(.horizontal, KanjiQuestTheme.spacingM).padding(.top, KanjiQuestTheme.spacingS)

                ProgressView(value: Float(viewModel.questionNumber), total: Float(viewModel.totalQuestions))
                    .tint(KanjiQuestTheme.primary)
                    .padding(.horizontal, KanjiQuestTheme.spacingM)
                    .padding(.vertical, 8)

                Spacer().frame(height: KanjiQuestTheme.spacingM)

                // Question type text
                Text(question.questionText)
                    .font(KanjiQuestTheme.titleMedium)
                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)

                Spacer().frame(height: 12)

                // Main display card
                vocabDisplayCard(question)
                    .padding(.horizontal, KanjiQuestTheme.spacingM)

                Spacer().frame(height: 20)

                // XP popup
                if let correct = viewModel.isCorrect {
                    Text(correct ? "+\(viewModel.xpGained) XP" : "Incorrect")
                        .font(KanjiQuestTheme.titleLarge).fontWeight(.bold)
                        .foregroundColor(correct ? KanjiQuestTheme.tertiary : KanjiQuestTheme.error)
                        .transition(.scale.combined(with: .opacity))
                    Spacer().frame(height: 12)
                }

                // 2x2 answer grid
                answerGrid(question)
                    .padding(.horizontal, KanjiQuestTheme.spacingM)

                // Post-answer details
                if viewModel.showResult {
                    Spacer().frame(height: KanjiQuestTheme.spacingM)

                    // Kanji decomposition
                    if let breakdown = question.kanjiBreakdown, !breakdown.isEmpty {
                        decompositionCard(breakdown)
                            .padding(.horizontal, KanjiQuestTheme.spacingM)
                    }

                    // Example sentence
                    if let sentenceJa = question.exampleSentenceJa {
                        exampleSentenceCard(ja: sentenceJa, en: question.exampleSentenceEn)
                            .padding(.horizontal, KanjiQuestTheme.spacingM)
                            .padding(.top, 8)
                    }

                    Spacer().frame(height: 20)
                    Button(action: viewModel.next) {
                        Text("Next").font(KanjiQuestTheme.labelLarge).fontWeight(.bold)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity).frame(height: 48)
                            .background(KanjiQuestTheme.primary)
                            .cornerRadius(KanjiQuestTheme.radiusM)
                    }
                    .padding(.horizontal, KanjiQuestTheme.spacingM)
                }

                Spacer().frame(height: KanjiQuestTheme.spacingL)
            }
        }
    }

    private var progressRow: some View {
        HStack {
            Text("\(viewModel.questionNumber) / \(viewModel.totalQuestions)")
                .font(KanjiQuestTheme.bodyMedium)
            Spacer()
            if viewModel.currentCombo > 1 {
                Text("\(viewModel.currentCombo)x combo")
                    .font(KanjiQuestTheme.bodyMedium).fontWeight(.bold)
                    .foregroundColor(KanjiQuestTheme.tertiary)
            }
            Spacer()
            Text("\(viewModel.sessionXp) XP")
                .font(KanjiQuestTheme.bodyMedium)
                .foregroundColor(KanjiQuestTheme.primary)
        }
    }

    @ViewBuilder
    private func vocabDisplayCard(_ question: Question) -> some View {
        VStack(spacing: 4) {
            let questionType = question.vocabQuestionType ?? "meaning"
            switch questionType {
            case "meaning":
                Text(question.kanjiLiteral).font(.system(size: 48)).multilineTextAlignment(.center)
                if let reading = question.vocabReading {
                    Text(reading).font(KanjiQuestTheme.titleMedium).foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                }
            case "reading":
                Text(question.kanjiLiteral).font(.system(size: 48)).multilineTextAlignment(.center)
            case "kanji_fill", "sentence":
                Text(question.kanjiLiteral).font(.system(size: 24)).multilineTextAlignment(.center).lineSpacing(8)
            default:
                Text(question.kanjiLiteral).font(.system(size: 48)).multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(20)
        .background(KanjiQuestTheme.surface)
        .cornerRadius(KanjiQuestTheme.radiusM)
        .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
    }

    private func answerGrid(_ question: Question) -> some View {
        let choices = question.choices
        return VStack(spacing: 12) {
            ForEach(0..<(choices.count + 1) / 2, id: \.self) { rowIndex in
                HStack(spacing: 12) {
                    ForEach(0..<2, id: \.self) { colIndex in
                        let index = rowIndex * 2 + colIndex
                        if index < choices.count {
                            answerButton(choices[index], question: question)
                        }
                    }
                }
            }
        }
    }

    private func answerButton(_ choice: String, question: Question) -> some View {
        let isSelected = viewModel.selectedAnswer == choice
        let isAnswer = choice == question.correctAnswer
        let showing = viewModel.showResult

        let bgColor: Color = {
            if !showing { return KanjiQuestTheme.primary }
            if isAnswer { return Color(hex: 0x4CAF50) }
            if isSelected { return KanjiQuestTheme.error }
            return KanjiQuestTheme.surfaceVariant
        }()

        return Button {
            if !viewModel.showResult { viewModel.submitAnswer(choice) }
        } label: {
            Text(choice)
                .font(.system(size: choice.count > 8 ? 14 : 18))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .frame(maxWidth: .infinity).frame(height: 56)
                .background(bgColor.opacity(showing && !isAnswer && !isSelected ? 0.6 : 1.0))
                .cornerRadius(KanjiQuestTheme.radiusM)
        }
        .disabled(viewModel.showResult)
    }

    // MARK: - Decomposition Card

    private func decompositionCard(_ breakdown: [String]) -> some View {
        let components = breakdown.map { entry -> (kanji: String, meaning: String) in
            let parts = entry.components(separatedBy: "=").map { $0.trimmingCharacters(in: .whitespaces) }
            return (kanji: parts.first ?? "?", meaning: parts.count > 1 ? parts[1] : "")
        }

        return VStack(spacing: 8) {
            Text("Composition")
                .font(KanjiQuestTheme.labelMedium)
                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)

            HStack(spacing: 0) {
                ForEach(Array(components.enumerated()), id: \.offset) { index, component in
                    VStack(spacing: 2) {
                        KanjiText(text: component.kanji, font: .system(size: 32, weight: .bold, design: .serif), color: KanjiQuestTheme.primary)
                        Text(component.meaning)
                            .font(KanjiQuestTheme.bodySmall)
                            .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                            .lineLimit(1)
                    }
                    .frame(width: 80, height: 90)
                    .background(KanjiQuestTheme.surface)
                    .cornerRadius(8)
                    .shadow(color: .black.opacity(0.05), radius: 2)
                    .overlay(
                        Circle()
                            .fill(Color(hex: 0x4CAF50))
                            .frame(width: 20, height: 20)
                            .overlay(Text("\u{2713}").font(.system(size: 12, weight: .bold)).foregroundColor(.white))
                            .offset(x: 4, y: -4),
                        alignment: .topTrailing
                    )

                    if index < components.count - 1 {
                        Text("+")
                            .font(KanjiQuestTheme.titleLarge).fontWeight(.bold)
                            .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                            .padding(.horizontal, 8)
                    }
                }
            }
        }
        .padding(12)
        .background(KanjiQuestTheme.surfaceVariant.opacity(0.5))
        .cornerRadius(KanjiQuestTheme.radiusM)
    }

    private func exampleSentenceCard(ja: String, en: String?) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Example")
                .font(KanjiQuestTheme.labelMedium)
                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
            Text(ja).font(KanjiQuestTheme.bodyMedium)
            if let en = en {
                Text(en).font(KanjiQuestTheme.bodySmall).foregroundColor(KanjiQuestTheme.onSurfaceVariant)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(KanjiQuestTheme.surfaceVariant.opacity(0.5))
        .cornerRadius(KanjiQuestTheme.radiusM)
    }

    // MARK: - Session Complete

    private var sessionCompleteView: some View {
        VStack(spacing: KanjiQuestTheme.spacingL) {
            Spacer()
            Text("Session Complete!").font(KanjiQuestTheme.headlineMedium).fontWeight(.bold)

            VStack(spacing: 12) {
                statRow("Words Studied", "\(viewModel.questionNumber)")
                statRow("Correct", "\(viewModel.correctCount) / \(viewModel.questionNumber)")
                statRow("Accuracy", "\(viewModel.questionNumber > 0 ? viewModel.correctCount * 100 / viewModel.questionNumber : 0)%")
                statRow("Best Combo", "\(viewModel.comboMax)x")
                statRow("XP Earned", "+\(viewModel.sessionXp)")
                statRow("Duration", formatDuration(viewModel.durationSec))
            }
            .padding(20)
            .background(KanjiQuestTheme.surface)
            .cornerRadius(KanjiQuestTheme.radiusM)
            .padding(.horizontal, KanjiQuestTheme.spacingM)

            Spacer()
            Button {
                viewModel.reset()
                onBack()
            } label: {
                Text("Done").font(KanjiQuestTheme.labelLarge).fontWeight(.bold)
                    .foregroundColor(.white).frame(maxWidth: .infinity).frame(height: 56)
                    .background(KanjiQuestTheme.primary).cornerRadius(KanjiQuestTheme.radiusM)
            }
            .padding(.horizontal, KanjiQuestTheme.spacingM)
            Spacer()
        }
    }

    // MARK: - Helpers

    private func errorContent(_ message: String) -> some View {
        VStack(spacing: KanjiQuestTheme.spacingM) {
            Spacer()
            Text(message).font(KanjiQuestTheme.bodyLarge).multilineTextAlignment(.center)
            Button("Go Back", action: onBack).buttonStyle(.bordered)
            Spacer()
        }.padding(KanjiQuestTheme.spacingL)
    }

    private func statRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label).font(KanjiQuestTheme.bodyLarge).foregroundColor(KanjiQuestTheme.onSurfaceVariant)
            Spacer()
            Text(value).font(KanjiQuestTheme.bodyLarge).fontWeight(.bold)
        }
    }

    private func formatDuration(_ seconds: Int) -> String {
        let m = seconds / 60; let s = seconds % 60
        return m > 0 ? "\(m)m \(s)s" : "\(s)s"
    }
}
