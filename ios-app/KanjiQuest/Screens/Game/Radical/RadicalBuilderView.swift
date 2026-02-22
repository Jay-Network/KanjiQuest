import SwiftUI
import SharedCore

private let radicalColor = Color(hex: 0x795548)

/// Radical builder game screen. Mirrors Android's RadicalBuilderScreen.kt.
/// Shows radical components and asks which kanji contains them. 2x2 kanji choice grid.
struct RadicalBuilderView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = RadicalBuilderViewModel()
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
        }
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left"); Text("Back")
                }.foregroundColor(.white)
            }
            ToolbarItem(placement: .principal) {
                Text("Radical Builder").font(.headline).foregroundColor(.white)
            }
        }
        .toolbarBackground(radicalColor, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .task {
            if viewModel.currentQuestion == nil && !viewModel.sessionComplete {
                viewModel.start(container: container)
            }
        }
    }

    // MARK: - Loading

    private var loadingContent: some View {
        VStack(spacing: KanjiQuestTheme.spacingM) {
            Text("Preparing radical questions...").font(KanjiQuestTheme.titleMedium)
            ProgressView().tint(radicalColor)
        }
    }

    // MARK: - Question

    private func questionView(_ question: Question) -> some View {
        ScrollView {
            VStack(spacing: 0) {
                progressRow
                    .padding(.horizontal, KanjiQuestTheme.spacingM)
                    .padding(.top, KanjiQuestTheme.spacingS)

                ProgressView(value: Float(viewModel.questionNumber), total: Float(viewModel.totalQuestions))
                    .tint(radicalColor)
                    .padding(.horizontal, KanjiQuestTheme.spacingM)
                    .padding(.vertical, 8)

                Spacer().frame(height: KanjiQuestTheme.spacingM)

                Text("Which kanji contains these radicals?")
                    .font(KanjiQuestTheme.titleMedium)
                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)

                Spacer().frame(height: KanjiQuestTheme.spacingM)

                // Radical composition prompt card
                Text(question.questionText)
                    .font(.system(size: 48))
                    .tracking(8)
                    .foregroundColor(radicalColor)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .frame(height: 120)
                    .background(KanjiQuestTheme.surface)
                    .cornerRadius(KanjiQuestTheme.radiusM)
                    .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
                    .padding(.horizontal, KanjiQuestTheme.spacingM)

                Spacer().frame(height: KanjiQuestTheme.spacingL)

                // Result feedback
                if let correct = viewModel.isCorrect {
                    VStack(spacing: 4) {
                        Text(correct ? "+\(viewModel.xpGained) XP" : "Incorrect")
                            .font(KanjiQuestTheme.titleLarge).fontWeight(.bold)
                            .foregroundColor(correct ? radicalColor : KanjiQuestTheme.error)

                        if !correct, let correctKanji = question.correctAnswer {
                            Text("Answer: \(correctKanji)")
                                .font(KanjiQuestTheme.bodyLarge)
                                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                        }

                        if let breakdown = question.kanjiBreakdown, !breakdown.isEmpty {
                            ForEach(Array(breakdown.enumerated()), id: \.offset) { _, line in
                                Text(line)
                                    .font(KanjiQuestTheme.bodySmall)
                                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer().frame(height: KanjiQuestTheme.spacingM)
                }

                // 2x2 kanji choice grid (larger buttons for kanji)
                answerGrid(question)
                    .padding(.horizontal, KanjiQuestTheme.spacingM)

                if viewModel.showResult {
                    Spacer().frame(height: KanjiQuestTheme.spacingL)
                    Button(action: viewModel.next) {
                        Text("Next").font(KanjiQuestTheme.labelLarge).fontWeight(.bold)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity).frame(height: 48)
                            .background(radicalColor)
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
                    .foregroundColor(radicalColor)
            }
            Spacer()
            Text("\(viewModel.sessionXp) XP")
                .font(KanjiQuestTheme.bodyMedium).foregroundColor(radicalColor)
        }
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
            if !showing { return radicalColor }
            if isAnswer { return Color(hex: 0x4CAF50) }
            if isSelected { return KanjiQuestTheme.error }
            return KanjiQuestTheme.surfaceVariant
        }()

        return Button {
            if !viewModel.showResult { viewModel.submitAnswer(choice) }
        } label: {
            Text(choice).font(.system(size: 32)).foregroundColor(.white)
                .frame(maxWidth: .infinity).frame(height: 72)
                .background(bgColor.opacity(showing && !isAnswer && !isSelected ? 0.6 : 1.0))
                .cornerRadius(KanjiQuestTheme.radiusM)
        }
        .disabled(viewModel.showResult)
    }

    // MARK: - Session Complete

    private var sessionCompleteView: some View {
        VStack(spacing: KanjiQuestTheme.spacingL) {
            Spacer()
            Text("Session Complete!").font(KanjiQuestTheme.headlineMedium).fontWeight(.bold)

            VStack(spacing: 12) {
                statRow("Cards Studied", "\(viewModel.questionNumber)")
                statRow("Correct", "\(viewModel.correctCount) / \(viewModel.questionNumber)")
                statRow("Accuracy", "\(viewModel.questionNumber > 0 ? viewModel.correctCount * 100 / viewModel.questionNumber : 0)%")
                statRow("Best Combo", "\(viewModel.comboMax)x")
                statRow("XP Earned", "+\(viewModel.sessionXp)")
            }
            .padding(20)
            .background(KanjiQuestTheme.surface)
            .cornerRadius(KanjiQuestTheme.radiusM)
            .padding(.horizontal, KanjiQuestTheme.spacingM)

            Spacer()
            Button {
                viewModel.reset(); onBack()
            } label: {
                Text("Done").font(KanjiQuestTheme.labelLarge).fontWeight(.bold)
                    .foregroundColor(.white).frame(maxWidth: .infinity).frame(height: 56)
                    .background(radicalColor).cornerRadius(KanjiQuestTheme.radiusM)
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
}
