import SwiftUI

private let testBlue = Color(hex: 0x2196F3)
private let testBlueDark = Color(hex: 0x1976D2)
private let correctGreen = Color(hex: 0x4CAF50)
private let incorrectRed = Color(hex: 0xF44336)

/// Test Mode screen matching Android's TestModeScreen.kt.
/// Three phases: scope selection, quiz in progress, results.
struct TestModeView: View {
    @EnvironmentObject var container: AppContainer
    var onBack: () -> Void

    var body: some View {
        TestModeContent(container: container, onBack: onBack)
    }
}

private struct TestModeContent: View {
    @StateObject private var viewModel: TestModeViewModel
    let onBack: () -> Void

    init(container: AppContainer, onBack: @escaping () -> Void) {
        _viewModel = StateObject(wrappedValue: TestModeViewModel(container: container))
        self.onBack = onBack
    }

    var body: some View {
        let state = viewModel.uiState

        VStack(spacing: 0) {
            switch state.phase {
            case .scopeSelection:
                scopeSelectionContent(isLoading: state.isLoading)
            case .inProgress:
                quizContent(state: state)
            case .results:
                resultsContent(state: state)
            }
        }
        .background(KanjiQuestTheme.background)
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: {
                    if state.phase == .scopeSelection {
                        onBack()
                    } else {
                        viewModel.resetToScopeSelection()
                    }
                }) {
                    Image(systemName: "chevron.left")
                        .foregroundColor(.white)
                }
            }
            ToolbarItem(placement: .principal) {
                Text("Test Mode")
                    .font(KanjiQuestTheme.titleSmall)
                    .foregroundColor(.white)
            }
        }
        .toolbarBackground(testBlue, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
    }

    // MARK: - Scope Selection

    @ViewBuilder
    private func scopeSelectionContent(isLoading: Bool) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Text("Choose what to test")
                    .font(KanjiQuestTheme.titleLarge)
                    .fontWeight(.bold)
                Text("Pure assessment - no XP or level changes")
                    .font(KanjiQuestTheme.bodyMedium)
                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)

                Spacer().frame(height: 20)

                ForEach(TestScope.groupedByCategory, id: \.0) { category, scopes in
                    Text(category)
                        .font(KanjiQuestTheme.titleMedium)
                        .fontWeight(.semibold)
                        .foregroundColor(testBlue)

                    Spacer().frame(height: 8)

                    ForEach(scopes, id: \.displayName) { scope in
                        Button(action: { if !isLoading { viewModel.selectScope(scope) } }) {
                            HStack {
                                Text(scope.displayName)
                                    .font(KanjiQuestTheme.bodyLarge)
                                    .fontWeight(.medium)
                                    .foregroundColor(KanjiQuestTheme.onSurface)
                                Spacer()
                                Text("10 Q")
                                    .font(KanjiQuestTheme.bodySmall)
                                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                            }
                            .padding(16)
                            .background(KanjiQuestTheme.surfaceVariant)
                            .cornerRadius(12)
                        }
                        .padding(.bottom, 8)
                    }

                    Spacer().frame(height: 12)
                }
            }
            .padding(16)
        }
    }

    // MARK: - Quiz Content

    @ViewBuilder
    private func quizContent(state: TestModeUiState) -> some View {
        if let question = state.currentQuestion {
            VStack(spacing: 0) {
            // Progress bar
            ProgressView(value: Double(state.currentIndex + 1) / Double(state.totalQuestions))
                .tint(testBlue)
                .padding(.horizontal, 16)
                .padding(.top, 8)

            // Counter and timer
            HStack {
                Text("\(state.currentIndex + 1) / \(state.totalQuestions)")
                    .font(KanjiQuestTheme.bodySmall)
                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                Spacer()
                Text(formatTime(state.elapsedSeconds))
                    .font(KanjiQuestTheme.bodySmall)
                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
            }
            .padding(.horizontal, 16)
            .padding(.top, 4)

            Spacer().frame(height: 32)

            // Character display
            ZStack {
                Circle()
                    .fill(testBlue.opacity(0.1))
                    .frame(width: 140, height: 140)
                KanjiText(text: question.displayCharacter, font: .system(size: 64, weight: .bold))
                    .foregroundColor(testBlue)
            }

            Spacer().frame(height: 16)

            Text(question.prompt)
                .font(KanjiQuestTheme.titleMedium)
                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)

            Spacer().frame(height: 24)

            // Answer options
            VStack(spacing: 8) {
                ForEach(Array(question.options.enumerated()), id: \.offset) { index, option in
                    let answered = state.selectedAnswer != nil
                    let isSelected = state.selectedAnswer == index
                    let isCorrectOption = index == question.correctIndex

                    let bgColor: Color = {
                        if !answered { return KanjiQuestTheme.surface }
                        if isCorrectOption { return correctGreen.opacity(0.15) }
                        if isSelected { return incorrectRed.opacity(0.15) }
                        return KanjiQuestTheme.surface
                    }()

                    let borderColor: Color = {
                        if !answered { return testBlue.opacity(0.3) }
                        if isCorrectOption { return correctGreen }
                        if isSelected { return incorrectRed }
                        return Color.clear
                    }()

                    let textColor: Color = {
                        if answered && isCorrectOption { return correctGreen }
                        if answered && isSelected { return incorrectRed }
                        return KanjiQuestTheme.onSurface
                    }()

                    Button(action: { if !answered { viewModel.selectAnswer(index) } }) {
                        Text(option)
                            .font(KanjiQuestTheme.bodyLarge)
                            .foregroundColor(textColor)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(bgColor)
                            .cornerRadius(12)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(borderColor, lineWidth: answered && (isCorrectOption || isSelected) ? 2 : 1)
                            )
                    }
                }
            }
            .padding(.horizontal, 16)

            Spacer()

            // Next button
            if state.selectedAnswer != nil {
                Button(action: { viewModel.nextQuestion() }) {
                    Text(state.currentIndex + 1 >= state.totalQuestions ? "See Results" : "Next")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(testBlue)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
            }
            } // VStack
        } // if let question
    }

    // MARK: - Results Content

    @ViewBuilder
    private func resultsContent(state: TestModeUiState) -> some View {
        let accuracyPct = Int(state.accuracy * 100)
        let resultColor: Color = accuracyPct >= 80 ? correctGreen : accuracyPct >= 50 ? Color(hex: 0xFFA726) : incorrectRed

        ScrollView {
            VStack(spacing: 0) {
                Spacer().frame(height: 24)

                Text(state.selectedScope?.displayName ?? "Test")
                    .font(KanjiQuestTheme.titleLarge)
                    .fontWeight(.bold)

                Spacer().frame(height: 8)

                Text("Results")
                    .font(KanjiQuestTheme.bodyMedium)
                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)

                Spacer().frame(height: 32)

                // Score circle
                ZStack {
                    Circle()
                        .fill(resultColor.opacity(0.1))
                        .frame(width: 160, height: 160)
                    VStack {
                        Text("\(state.correctCount)/\(state.totalQuestions)")
                            .font(.system(size: 36, weight: .bold))
                            .foregroundColor(resultColor)
                        Text("\(accuracyPct)%")
                            .font(.system(size: 18))
                            .foregroundColor(resultColor)
                    }
                }

                Spacer().frame(height: 24)

                // Stats card
                HStack {
                    statItem(label: "Correct", value: "\(state.correctCount)")
                    Spacer()
                    statItem(label: "Wrong", value: "\(state.totalQuestions - state.correctCount)")
                    Spacer()
                    statItem(label: "Time", value: formatTime(state.elapsedSeconds))
                }
                .padding(20)
                .background(KanjiQuestTheme.surfaceVariant)
                .cornerRadius(16)
                .padding(.horizontal, 16)

                Spacer().frame(height: 16)

                Text("No XP or level changes were applied")
                    .font(KanjiQuestTheme.bodySmall)
                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                    .multilineTextAlignment(.center)

                Spacer().frame(height: 32)

                // Action buttons
                Button(action: { viewModel.retakeTest() }) {
                    Text("Test Again")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(testBlue)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 16)

                Spacer().frame(height: 12)

                Button(action: { onBack() }) {
                    Text("Done")
                        .font(.system(size: 16))
                        .foregroundColor(KanjiQuestTheme.onSurface)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(KanjiQuestTheme.onSurfaceVariant.opacity(0.3), lineWidth: 1)
                        )
                }
                .padding(.horizontal, 16)
            }
        }
    }

    private func statItem(label: String, value: String) -> some View {
        VStack {
            Text(value)
                .font(KanjiQuestTheme.titleLarge)
                .fontWeight(.bold)
            Text(label)
                .font(KanjiQuestTheme.bodySmall)
                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
        }
    }

    private func formatTime(_ seconds: Int) -> String {
        let mins = seconds / 60
        let secs = seconds % 60
        return String(format: "%d:%02d", mins, secs)
    }
}
