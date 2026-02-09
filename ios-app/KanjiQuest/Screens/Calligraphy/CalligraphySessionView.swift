import SwiftUI
import SharedCore

struct CalligraphySessionView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = CalligraphySessionViewModel()

    let kanjiLiteral: String
    let strokePaths: [String]

    @State private var strokes: [[CalligraphyPointData]] = []
    @State private var activeStroke: [CalligraphyPointData] = []

    var body: some View {
        VStack(spacing: 0) {
            // Header: kanji + stroke count
            headerSection

            // Canvas
            canvasSection

            // Controls
            controlsSection

            // Result / Feedback
            if viewModel.showResult {
                resultSection
            }
        }
        .overlay {
            if viewModel.sessionComplete, let stats = viewModel.sessionStats {
                sessionCompleteOverlay(stats: stats)
            }
        }
        .background(KanjiQuestTheme.background)
        .navigationTitle("書道")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            viewModel.setup(container: container, kanji: kanjiLiteral, strokePaths: strokePaths)
            await viewModel.startSession()
        }
    }

    // MARK: - Sections

    private var headerSection: some View {
        HStack {
            Text(kanjiLiteral)
                .font(KanjiQuestTheme.kanjiMedium)
                .foregroundColor(KanjiQuestTheme.primary)

            VStack(alignment: .leading) {
                Text("Strokes: \(strokePaths.count)")
                    .font(KanjiQuestTheme.labelLarge)
                Text("Drawn: \(strokes.count)")
                    .font(KanjiQuestTheme.labelSmall)
                    .foregroundColor(.secondary)
            }

            Spacer()

            if viewModel.isAILoading {
                SwiftUI.ProgressView()
                    .progressViewStyle(.circular)
            }
        }
        .padding()
    }

    private var canvasSection: some View {
        CalligraphyCanvasView(
            strokes: $strokes,
            activeStroke: $activeStroke,
            referenceStrokePaths: strokePaths,
            onStrokeComplete: { _ in
                // Auto-submit when stroke count matches
                if strokes.count == strokePaths.count {
                    Task {
                        await viewModel.submitDrawing(strokes: strokes)
                    }
                }
            }
        )
        .aspectRatio(1.0, contentMode: .fit)
        .border(Color.gray.opacity(0.3), width: 1)
        .padding(.horizontal)
    }

    private var controlsSection: some View {
        HStack(spacing: KanjiQuestTheme.spacingM) {
            Button("Clear") {
                strokes = []
                activeStroke = []
                viewModel.reset()
            }
            .buttonStyle(.bordered)

            Button("Undo") {
                if !strokes.isEmpty {
                    strokes.removeLast()
                    viewModel.reset()
                }
            }
            .buttonStyle(.bordered)
            .disabled(strokes.isEmpty)

            Spacer()

            Button("Submit") {
                Task {
                    await viewModel.submitDrawing(strokes: strokes)
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(KanjiQuestTheme.primary)
            .disabled(strokes.isEmpty)
        }
        .padding()
    }

    private var resultSection: some View {
        VStack(alignment: .leading, spacing: KanjiQuestTheme.spacingS) {
            // Score
            HStack {
                Text("Score")
                    .font(KanjiQuestTheme.labelLarge)
                Spacer()
                Text(viewModel.isCorrect ? "Correct" : "Try Again")
                    .font(KanjiQuestTheme.labelLarge)
                    .foregroundColor(viewModel.isCorrect ? KanjiQuestTheme.success : KanjiQuestTheme.error)

                Text("Quality: \(viewModel.quality)/5")
                    .font(KanjiQuestTheme.labelSmall)
                    .foregroundColor(.secondary)
            }

            if let xp = viewModel.xpGained {
                Text("+\(xp) XP")
                    .font(KanjiQuestTheme.labelLarge)
                    .foregroundColor(KanjiQuestTheme.xpGold)
            }

            // AI Calligraphy Feedback
            if let feedback = viewModel.calligraphyFeedback {
                Divider()

                Text("AI 書道 Feedback")
                    .font(KanjiQuestTheme.labelLarge)

                if !feedback.overallComment.isEmpty {
                    Text(feedback.overallComment)
                        .font(KanjiQuestTheme.bodyMedium)
                }

                if !feedback.pressureFeedback.isEmpty {
                    HStack(alignment: .top) {
                        Text("筆圧:")
                            .font(KanjiQuestTheme.labelSmall)
                            .foregroundColor(KanjiQuestTheme.primary)
                        Text(feedback.pressureFeedback)
                            .font(KanjiQuestTheme.bodyMedium)
                    }
                }

                if !feedback.movementFeedback.isEmpty {
                    HStack(alignment: .top) {
                        Text("運筆:")
                            .font(KanjiQuestTheme.labelSmall)
                            .foregroundColor(KanjiQuestTheme.primary)
                        Text(feedback.movementFeedback)
                            .font(KanjiQuestTheme.bodyMedium)
                    }
                }

                ForEach(Array(feedback.strokeFeedback.enumerated()), id: \.offset) { _, tip in
                    Text("• \(tip)")
                        .font(KanjiQuestTheme.bodyMedium)
                        .foregroundColor(.secondary)
                }
            }

            // Next button
            Button("Next Kanji") {
                strokes = []
                activeStroke = []
                Task {
                    await viewModel.nextKanji()
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(KanjiQuestTheme.primary)
            .frame(maxWidth: .infinity)
            .padding(.top, KanjiQuestTheme.spacingS)
        }
        .padding()
        .background(KanjiQuestTheme.surfaceVariant)
        .cornerRadius(KanjiQuestTheme.radiusM)
        .padding(.horizontal)
    }

    // MARK: - Session Complete Overlay

    private func sessionCompleteOverlay(stats: SessionStats) -> some View {
        ZStack {
            Color.black.opacity(0.5)
                .ignoresSafeArea()

            VStack(spacing: KanjiQuestTheme.spacingL) {
                Text("Session Complete!")
                    .font(KanjiQuestTheme.titleLarge)
                    .foregroundColor(.white)

                VStack(spacing: KanjiQuestTheme.spacingS) {
                    statRow(label: "Kanji Studied", value: "\(stats.cardsStudied)")
                    statRow(label: "Correct", value: "\(stats.correctCount)/\(stats.cardsStudied)")
                    statRow(label: "Best Combo", value: "\(stats.comboMax)")
                    statRow(label: "XP Earned", value: "+\(stats.xpEarned)")
                    statRow(label: "Duration", value: "\(stats.durationSec)s")
                }
                .padding()
                .background(.ultraThinMaterial)
                .cornerRadius(KanjiQuestTheme.radiusL)

                Button("Done") {
                    // Navigate back to home (handled by NavigationStack pop)
                }
                .buttonStyle(.borderedProminent)
                .tint(KanjiQuestTheme.primary)
            }
            .padding(KanjiQuestTheme.spacingXL)
        }
    }

    private func statRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(KanjiQuestTheme.bodyMedium)
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .font(KanjiQuestTheme.labelLarge)
                .foregroundColor(.primary)
        }
    }
}
