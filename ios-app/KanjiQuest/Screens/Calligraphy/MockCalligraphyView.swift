import SwiftUI

#if IPAD_TARGET

/// Standalone calligraphy practice — works WITHOUT KMP/AppContainer.
/// Uses the full brush engine canvas + Gemini AI feedback.
/// Hardcoded kanji set for practice until KMP init works.
struct MockCalligraphyView: View {
    @Environment(\.dismiss) private var dismiss

    // Hardcoded practice kanji with SVG stroke paths
    private static let practiceKanji: [(literal: String, meaning: String, strokePaths: [String])] = [
        ("一", "One", ["M 15,54 H 94"]),
        ("二", "Two", ["M 22,35 H 87", "M 14,72 H 95"]),
        ("三", "Three", ["M 27,25 H 82", "M 20,52 H 89", "M 14,79 H 95"]),
        ("十", "Ten", ["M 54,10 V 99", "M 10,54 H 99"]),
        ("大", "Big", ["M 54,10 V 55", "M 15,40 L 54,55 L 93,40", "M 54,55 L 20,95", "M 54,55 L 88,95"]),
        ("山", "Mountain", ["M 54,15 V 90", "M 20,45 V 90", "M 88,45 V 90", "M 10,90 H 98"]),
        ("川", "River", ["M 25,15 V 90", "M 54,20 V 85", "M 83,15 V 90"]),
        ("日", "Day/Sun", ["M 25,15 V 90", "M 25,15 H 83", "M 83,15 V 90", "M 25,52 H 83", "M 25,90 H 83"]),
    ]

    @State private var currentIndex = 0
    @State private var strokes: [[CalligraphyPointData]] = []
    @State private var activeStroke: [CalligraphyPointData] = []
    @State private var canvasVersion = 0
    @State private var showResult = false
    @State private var isAILoading = false
    @State private var feedback: CalligraphyFeedbackService.CalligraphyFeedback?
    @State private var canvasSize: CGSize = CGSize(width: 400, height: 400)

    private let teal = Color(red: 0.05, green: 0.58, blue: 0.53)
    private let paperBackground = Color(red: 1.0, green: 0.973, blue: 0.941)

    private var currentKanji: (literal: String, meaning: String, strokePaths: [String]) {
        Self.practiceKanji[currentIndex % Self.practiceKanji.count]
    }

    private var geminiApiKey: String {
        Bundle.main.infoDictionary?["GEMINI_API_KEY"] as? String ?? ""
    }

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text(currentKanji.literal)
                    .font(.system(size: 48, weight: .bold))
                    .foregroundColor(teal)

                VStack(alignment: .leading) {
                    Text(currentKanji.meaning)
                        .font(.headline)
                    Text("Strokes: \(currentKanji.strokePaths.count) | Drawn: \(strokes.count)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                if isAILoading {
                    ProgressView()
                        .progressViewStyle(.circular)
                }

                Text("\(currentIndex + 1)/\(Self.practiceKanji.count)")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color(.systemGray5))
                    .cornerRadius(12)
            }
            .padding()

            // Canvas
            CalligraphyCanvasView(
                strokes: $strokes,
                activeStroke: $activeStroke,
                referenceStrokePaths: currentKanji.strokePaths,
                canvasVersion: canvasVersion,
                onStrokeComplete: { _ in }
            )
            .aspectRatio(1.0, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(Color.gray.opacity(0.3), lineWidth: 1)
            )
            .background(
                GeometryReader { geo in
                    Color.clear.onAppear { canvasSize = geo.size }
                }
            )
            .padding(.horizontal)

            // Controls
            HStack(spacing: 16) {
                Button("Clear") {
                    strokes = []
                    activeStroke = []
                    canvasVersion += 1
                    showResult = false
                    feedback = nil
                }
                .buttonStyle(.bordered)

                Button("Undo") {
                    if !strokes.isEmpty {
                        strokes.removeLast()
                        showResult = false
                        feedback = nil
                    }
                }
                .buttonStyle(.bordered)
                .disabled(strokes.isEmpty)

                Spacer()

                Button("Submit") {
                    submitDrawing()
                }
                .buttonStyle(.borderedProminent)
                .tint(teal)
                .disabled(strokes.isEmpty)
            }
            .padding()

            // Result / AI Feedback
            if showResult {
                ScrollView {
                    VStack(alignment: .leading, spacing: 8) {
                        if let feedback = feedback {
                            Text("AI 書道 Feedback")
                                .font(.headline)

                            if !feedback.overallComment.isEmpty {
                                Text(feedback.overallComment)
                                    .font(.body)
                            }

                            // Technique scores
                            HStack(spacing: 12) {
                                if let balance = feedback.balanceScore {
                                    techniqueChip("均 Balance", score: balance)
                                }
                                if let tome = feedback.tomeScore {
                                    techniqueChip("止 Tome", score: tome)
                                }
                                if let hane = feedback.haneScore {
                                    techniqueChip("撥 Hane", score: hane)
                                }
                                if let harai = feedback.haraiScore {
                                    techniqueChip("払 Harai", score: harai)
                                }
                            }

                            if !feedback.pressureFeedback.isEmpty {
                                Text("筆圧: \(feedback.pressureFeedback)")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }
                        } else if isAILoading {
                            HStack {
                                ProgressView()
                                Text("Analyzing your calligraphy...")
                                    .foregroundColor(.secondary)
                            }
                        } else {
                            Text("Stroke count: \(strokes.count)/\(currentKanji.strokePaths.count)")
                                .foregroundColor(.secondary)
                        }

                        // Next button
                        Button("Next Kanji →") {
                            nextKanji()
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(teal)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 8)
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(16)
                    .padding(.horizontal)
                }
            }
        }
        .background(paperBackground)
        .navigationTitle("書道 Practice")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("Done") { dismiss() }
            }
        }
    }

    private func submitDrawing() {
        showResult = true
        guard !geminiApiKey.isEmpty else { return }

        isAILoading = true
        let currentStrokes = strokes
        let kanji = currentKanji.literal
        let strokeCount = currentKanji.strokePaths.count
        let size = canvasSize

        Task {
            let service = CalligraphyFeedbackService(apiKey: geminiApiKey)
            let result = await service.evaluate(
                strokes: currentStrokes,
                targetKanji: kanji,
                strokeCount: strokeCount,
                canvasSize: size
            )
            await MainActor.run {
                isAILoading = false
                if result.isAvailable {
                    feedback = result
                }
            }
        }
    }

    private func nextKanji() {
        currentIndex = (currentIndex + 1) % Self.practiceKanji.count
        strokes = []
        activeStroke = []
        canvasVersion += 1
        showResult = false
        feedback = nil
    }

    private func techniqueChip(_ label: String, score: Int) -> some View {
        VStack(spacing: 2) {
            Text(label)
                .font(.system(size: 11, weight: .medium))
            HStack(spacing: 1) {
                ForEach(1...5, id: \.self) { i in
                    Circle()
                        .fill(i <= score ? scoreColor(score) : Color.gray.opacity(0.2))
                        .frame(width: 6, height: 6)
                }
            }
        }
        .padding(.vertical, 6)
        .padding(.horizontal, 10)
        .background(scoreColor(score).opacity(0.08))
        .cornerRadius(8)
    }

    private func scoreColor(_ score: Int) -> Color {
        switch score {
        case 5: return .green
        case 4: return Color(red: 0.2, green: 0.7, blue: 0.3)
        case 3: return .orange
        case 2: return Color(red: 0.9, green: 0.5, blue: 0.2)
        default: return .red
        }
    }
}

#endif
