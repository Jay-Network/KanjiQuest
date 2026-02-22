import SwiftUI
import SharedCore

/// Kanji recognition game screen. Mirrors Android's RecognitionScreen.kt.
/// Shows a kanji and 4 answer choices in a 2x2 grid.
struct RecognitionView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = RecognitionViewModel()
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

            // Discovery overlay
            if viewModel.showDiscovery, let item = viewModel.discoveredItem {
                DiscoveryOverlay(
                    discoveredItem: item,
                    kanjiLiteral: viewModel.discoveredKanjiLiteral,
                    kanjiMeaning: viewModel.currentQuestion?.kanjiMeaning,
                    onDismiss: { viewModel.showDiscovery = false }
                )
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                    Text("Back")
                }.foregroundColor(.white)
            }
            ToolbarItem(placement: .principal) {
                Text("Recognition").font(.headline).foregroundColor(.white)
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
            Text("Preparing questions...")
                .font(KanjiQuestTheme.titleMedium)
            ProgressView()
        }
    }

    // MARK: - Question

    private func questionView(_ question: Question) -> some View {
        ScrollView {
            VStack(spacing: 0) {
                // Progress bar + stats row
                progressRow
                    .padding(.horizontal, KanjiQuestTheme.spacingM)
                    .padding(.top, KanjiQuestTheme.spacingS)

                ProgressView(value: Float(viewModel.questionNumber), total: Float(viewModel.totalQuestions))
                    .tint(KanjiQuestTheme.primary)
                    .padding(.horizontal, KanjiQuestTheme.spacingM)
                    .padding(.vertical, 8)

                Spacer().frame(height: KanjiQuestTheme.spacingL)

                // Prompt
                Text("What is the reading?")
                    .font(KanjiQuestTheme.titleMedium)
                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)

                Spacer().frame(height: KanjiQuestTheme.spacingM)

                // Large kanji card with NEW badge
                kanjiCard(question)

                Spacer().frame(height: KanjiQuestTheme.spacingM)

                // XP popup + readings (shown after answer)
                if viewModel.showResult {
                    resultFeedback(question)
                }

                // 2x2 answer grid
                answerGrid(question)
                    .padding(.horizontal, KanjiQuestTheme.spacingM)

                // Next button
                if viewModel.showResult {
                    Spacer().frame(height: KanjiQuestTheme.spacingL)
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

    private func kanjiCard(_ question: Question) -> some View {
        ZStack(alignment: .topTrailing) {
            RoundedRectangle(cornerRadius: KanjiQuestTheme.radiusM)
                .fill(KanjiQuestTheme.surface)
                .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
                .frame(width: 200, height: 200)
                .overlay(
                    KanjiText(text: question.kanjiLiteral, font: .system(size: 96, weight: .regular, design: .serif))
                )

            if question.isNewCard {
                Text("NEW")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(Color(hex: 0x00BFA5))
                    .cornerRadius(4)
                    .padding(4)
            }
        }
    }

    private func resultFeedback(_ question: Question) -> some View {
        VStack(spacing: 4) {
            if let correct = viewModel.isCorrect {
                Text(correct ? "+\(viewModel.xpGained) XP" : "Incorrect")
                    .font(KanjiQuestTheme.titleLarge).fontWeight(.bold)
                    .foregroundColor(correct ? KanjiQuestTheme.tertiary : KanjiQuestTheme.error)
                    .transition(.scale.combined(with: .opacity))
            }

            if let meaning = question.kanjiMeaning, !meaning.isEmpty {
                Text(meaning)
                    .font(KanjiQuestTheme.titleMedium)
                    .foregroundColor(KanjiQuestTheme.onSurface)
            }

            if let kunReadings = question.kunReadings, !kunReadings.isEmpty {
                Text("訓: \(kunReadings.joined(separator: "、"))")
                    .font(KanjiQuestTheme.bodyLarge).fontWeight(.bold)
                    .foregroundColor(KanjiQuestTheme.primary)
            }

            if let onReadings = question.onReadings, !onReadings.isEmpty {
                Text("音: \(onReadings.joined(separator: "、"))")
                    .font(KanjiQuestTheme.bodyMedium)
                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
            }

            if let words = question.exampleWords, !words.isEmpty {
                ForEach(Array(words.prefix(3).enumerated()), id: \.offset) { _, vocab in
                    Text("\(vocab.kanjiForm) (\(vocab.reading)) \(vocab.primaryMeaning)")
                        .font(KanjiQuestTheme.bodySmall)
                        .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                }
            }

            Spacer().frame(height: 8)
        }
        .padding(.horizontal, KanjiQuestTheme.spacingM)
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
            if !viewModel.showResult {
                viewModel.submitAnswer(choice)
            }
        } label: {
            Text(choice)
                .font(.system(size: 20))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(bgColor.opacity(showing && !isAnswer && !isSelected ? 0.6 : 1.0))
                .cornerRadius(KanjiQuestTheme.radiusM)
        }
        .disabled(viewModel.showResult)
    }

    // MARK: - Session Complete

    private var sessionCompleteView: some View {
        ScrollView {
            VStack(spacing: KanjiQuestTheme.spacingL) {
                Spacer().frame(height: KanjiQuestTheme.spacingXL)

                Text("Session Complete!")
                    .font(KanjiQuestTheme.headlineMedium).fontWeight(.bold)

                // Stats card
                VStack(spacing: 12) {
                    statRow("Cards Studied", "\(viewModel.questionNumber)")
                    statRow("Correct", "\(viewModel.correctCount) / \(viewModel.questionNumber)")
                    statRow("Accuracy", "\(viewModel.questionNumber > 0 ? viewModel.correctCount * 100 / viewModel.questionNumber : 0)%")
                    statRow("Best Combo", "\(viewModel.comboMax)x")
                    statRow("XP Earned", "+\(viewModel.sessionXp)")
                    statRow("Duration", formatDuration(viewModel.durationSec))
                }
                .padding(20)
                .background(KanjiQuestTheme.surface)
                .cornerRadius(KanjiQuestTheme.radiusM)
                .shadow(color: .black.opacity(0.05), radius: 2)
                .padding(.horizontal, KanjiQuestTheme.spacingM)

                // New discoveries
                if !viewModel.newDiscoveries.isEmpty {
                    VStack(spacing: 8) {
                        Text("New Discoveries")
                            .font(KanjiQuestTheme.titleMedium).fontWeight(.bold)
                            .foregroundColor(Color(hex: 0x00BFA5))

                        HStack(spacing: 12) {
                            ForEach(Array(viewModel.newDiscoveries.enumerated()), id: \.offset) { _, discovery in
                                VStack {
                                    KanjiText(text: discovery.literal, font: .system(size: 36, design: .serif))
                                    Text(discovery.meaning)
                                        .font(KanjiQuestTheme.bodySmall)
                                        .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                                        .lineLimit(1)
                                }
                            }
                        }
                    }
                    .padding(16)
                    .background(Color(hex: 0x00BFA5).opacity(0.1))
                    .cornerRadius(KanjiQuestTheme.radiusM)
                    .padding(.horizontal, KanjiQuestTheme.spacingM)
                }

                Button {
                    viewModel.reset()
                    onBack()
                } label: {
                    Text("Done").font(KanjiQuestTheme.labelLarge).fontWeight(.bold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity).frame(height: 56)
                        .background(KanjiQuestTheme.primary)
                        .cornerRadius(KanjiQuestTheme.radiusM)
                }
                .padding(.horizontal, KanjiQuestTheme.spacingM)

                Spacer()
            }
        }
    }

    // MARK: - Error

    private func errorContent(_ message: String) -> some View {
        VStack(spacing: KanjiQuestTheme.spacingM) {
            Spacer()
            Text(message)
                .font(KanjiQuestTheme.bodyLarge)
                .multilineTextAlignment(.center)
            Button("Go Back", action: onBack)
                .buttonStyle(.bordered)
            Spacer()
        }
        .padding(KanjiQuestTheme.spacingL)
    }

    // MARK: - Helpers

    private func statRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
                .font(KanjiQuestTheme.bodyLarge)
                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
            Spacer()
            Text(value)
                .font(KanjiQuestTheme.bodyLarge).fontWeight(.bold)
        }
    }

    private func formatDuration(_ seconds: Int) -> String {
        let m = seconds / 60
        let s = seconds % 60
        return m > 0 ? "\(m)m \(s)s" : "\(s)s"
    }
}
