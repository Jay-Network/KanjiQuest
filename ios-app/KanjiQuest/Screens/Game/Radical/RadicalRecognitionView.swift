import SwiftUI
import SharedCore

private let radicalColor = Color(hex: 0x795548)

/// Radical recognition game screen. Mirrors Android's RadicalRecognitionScreen.kt.
/// Shows radical images and 4 answer choices.
struct RadicalRecognitionView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = RadicalRecognitionViewModel()
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
                Text("Radical Recognition").font(.headline).foregroundColor(.white)
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
            Text("Preparing questions...").font(KanjiQuestTheme.titleMedium)
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

                Spacer().frame(height: KanjiQuestTheme.spacingL)

                Text(question.questionText)
                    .font(KanjiQuestTheme.titleMedium)
                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)

                Spacer().frame(height: KanjiQuestTheme.spacingM)

                // Radical image card
                RoundedRectangle(cornerRadius: KanjiQuestTheme.radiusM)
                    .fill(KanjiQuestTheme.surface)
                    .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
                    .frame(width: 200, height: 200)
                    .overlay(
                        RadicalImage(
                            radicalId: question.kanjiId,
                            contentDescription: question.kanjiLiteral,
                            size: 160
                        )
                    )

                Spacer().frame(height: 32)

                // Result feedback
                if let correct = viewModel.isCorrect {
                    VStack(spacing: 4) {
                        Text(correct ? "+\(viewModel.xpGained) XP" : "Incorrect")
                            .font(KanjiQuestTheme.titleLarge).fontWeight(.bold)
                            .foregroundColor(correct ? radicalColor : KanjiQuestTheme.error)
                            .transition(.scale.combined(with: .opacity))

                        if let nameJp = question.radicalNameJp, !nameJp.isEmpty {
                            Text("\(question.kanjiLiteral)  \(nameJp)")
                                .font(KanjiQuestTheme.bodyLarge)
                                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                        }

                        if !(correct) {
                            Text("Answer: \(question.correctAnswer)")
                                .font(KanjiQuestTheme.bodyMedium)
                                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                        }
                    }
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
            Text(choice).font(.system(size: 16)).foregroundColor(.white)
                .multilineTextAlignment(.center).lineLimit(2)
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
