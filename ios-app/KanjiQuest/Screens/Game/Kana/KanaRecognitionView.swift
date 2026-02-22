import SwiftUI
import SharedCore

/// Kana recognition game screen. Mirrors Android's KanaRecognitionScreen.kt.
/// Accent color changes based on kana type: pink for hiragana, cyan for katakana.
struct KanaRecognitionView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = KanaRecognitionViewModel()
    let kanaType: KanaType
    var onBack: () -> Void = {}

    private var accentColor: Color {
        kanaType == .hiragana ? Color(hex: 0xE91E63) : Color(hex: 0x00BCD4)
    }

    private var title: String {
        kanaType == .hiragana ? "Hiragana" : "Katakana"
    }

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
                Text("\(title) Recognition").font(.headline).foregroundColor(.white)
            }
        }
        .toolbarBackground(accentColor, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .task {
            if viewModel.currentQuestion == nil && !viewModel.sessionComplete {
                viewModel.start(container: container, kanaType: kanaType)
            }
        }
    }

    // MARK: - Loading

    private var loadingContent: some View {
        VStack(spacing: KanjiQuestTheme.spacingM) {
            Text("Preparing questions...").font(KanjiQuestTheme.titleMedium)
            ProgressView().tint(accentColor)
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
                    .tint(accentColor)
                    .padding(.horizontal, KanjiQuestTheme.spacingM)
                    .padding(.vertical, 8)

                Spacer().frame(height: KanjiQuestTheme.spacingL)

                Text("What is the reading?")
                    .font(KanjiQuestTheme.titleMedium)
                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)

                Spacer().frame(height: KanjiQuestTheme.spacingM)

                // Large kana card
                RoundedRectangle(cornerRadius: KanjiQuestTheme.radiusM)
                    .fill(KanjiQuestTheme.surface)
                    .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
                    .frame(width: 200, height: 200)
                    .overlay(
                        Text(question.kanjiLiteral)
                            .font(.system(size: 96))
                            .multilineTextAlignment(.center)
                    )

                Spacer().frame(height: 32)

                // XP popup
                if let correct = viewModel.isCorrect {
                    Text(correct ? "+\(viewModel.xpGained) XP" : "Incorrect")
                        .font(KanjiQuestTheme.titleLarge).fontWeight(.bold)
                        .foregroundColor(correct ? accentColor : KanjiQuestTheme.error)
                        .transition(.scale.combined(with: .opacity))
                    Spacer().frame(height: KanjiQuestTheme.spacingM)
                }

                // 2x2 answer grid
                answerGrid(question)
                    .padding(.horizontal, KanjiQuestTheme.spacingM)

                if viewModel.showResult {
                    Spacer().frame(height: KanjiQuestTheme.spacingL)
                    Button(action: viewModel.next) {
                        Text("Next").font(KanjiQuestTheme.labelLarge).fontWeight(.bold)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity).frame(height: 48)
                            .background(accentColor)
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
                    .foregroundColor(accentColor)
            }
            Spacer()
            Text("\(viewModel.sessionXp) XP")
                .font(KanjiQuestTheme.bodyMedium).foregroundColor(accentColor)
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
            if !showing { return accentColor }
            if isAnswer { return Color(hex: 0x4CAF50) }
            if isSelected { return KanjiQuestTheme.error }
            return KanjiQuestTheme.surfaceVariant
        }()

        return Button {
            if !viewModel.showResult { viewModel.submitAnswer(choice) }
        } label: {
            Text(choice).font(.system(size: 20)).foregroundColor(.white)
                .frame(maxWidth: .infinity).frame(height: 56)
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
                viewModel.reset()
                onBack()
            } label: {
                Text("Done").font(KanjiQuestTheme.labelLarge).fontWeight(.bold)
                    .foregroundColor(.white).frame(maxWidth: .infinity).frame(height: 56)
                    .background(accentColor).cornerRadius(KanjiQuestTheme.radiusM)
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
