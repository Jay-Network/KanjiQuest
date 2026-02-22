import SwiftUI

struct BrushTestView: View {
    @State private var strokes: [[CalligraphyPointData]] = []
    @State private var activeStroke: [CalligraphyPointData] = []
    @State private var canvasVersion = 0
    @State private var strokeCount = 0
    @State private var brushSize: BrushSize = .medium
    @State private var inkConcentration: CGFloat = 1.0
    @State private var isSoundMuted: Bool = false
    @State private var showHelp: Bool = false

    // Zen-minimal warm tones
    private let bgColor = Color(red: 0.12, green: 0.11, blue: 0.10)
    private let textPrimary = Color(red: 0.85, green: 0.82, blue: 0.78)
    private let textSecondary = Color(red: 0.6, green: 0.57, blue: 0.53)
    private let textTertiary = Color(red: 0.5, green: 0.47, blue: 0.43)
    private let toolbarColor = Color(red: 0.7, green: 0.67, blue: 0.63)
    private let clearColor = Color(red: 0.75, green: 0.35, blue: 0.30)

    var body: some View {
        ZStack {
            bgColor.ignoresSafeArea()

            VStack(spacing: 0) {
                // Title bar
                HStack {
                    Text("書道")
                        .font(.system(size: 28, weight: .thin, design: .serif))
                        .foregroundColor(textPrimary)
                    Text("Brush Test")
                        .font(.system(size: 16, weight: .light, design: .default))
                        .foregroundColor(textSecondary)
                    Spacer()
                    Text("\(strokeCount) strokes")
                        .font(.system(size: 14, weight: .light, design: .monospaced))
                        .foregroundColor(textTertiary)

                    Button(action: { showHelp = true }) {
                        Image(systemName: "questionmark.circle")
                            .font(.system(size: 18))
                            .foregroundColor(textTertiary)
                    }
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 12)

                // Canvas
                GeometryReader { geo in
                    let side = min(geo.size.width - 48, geo.size.height - 24)
                    HStack {
                        Spacer()
                        CalligraphyCanvasView(
                            strokes: $strokes,
                            activeStroke: $activeStroke,
                            referenceStrokePaths: [],
                            canvasVersion: canvasVersion,
                            brushSize: brushSize,
                            inkConcentration: inkConcentration,
                            isSoundMuted: isSoundMuted,
                            onStrokeComplete: { _ in
                                strokeCount += 1
                            }
                        )
                        .frame(width: side, height: side)
                        .clipShape(RoundedRectangle(cornerRadius: 4))
                        .shadow(color: .black.opacity(0.5), radius: 12, y: 4)
                        Spacer()
                    }
                }

                // Toolbar
                HStack(spacing: 24) {
                    // Brush size picker (3 dots)
                    brushSizePicker

                    // Ink concentration slider (濃淡)
                    inkConcentrationSlider

                    Spacer()

                    // Sound mute toggle
                    Button(action: { isSoundMuted.toggle() }) {
                        Image(systemName: isSoundMuted ? "speaker.slash.fill" : "speaker.wave.2.fill")
                            .font(.system(size: 18))
                            .foregroundColor(toolbarColor)
                    }

                    // Undo
                    Button(action: {
                        if !strokes.isEmpty {
                            strokes.removeLast()
                            strokeCount = max(0, strokeCount - 1)
                            canvasVersion += 1
                        }
                    }) {
                        Image(systemName: "arrow.uturn.backward")
                            .font(.system(size: 18))
                            .foregroundColor(toolbarColor)
                    }

                    // Clear
                    Button(action: {
                        canvasVersion += 1
                        strokes = []
                        activeStroke = []
                        strokeCount = 0
                    }) {
                        Image(systemName: "trash")
                            .font(.system(size: 18))
                            .foregroundColor(clearColor)
                    }
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 16)
            }
        }
        .sheet(isPresented: $showHelp) {
            HelpSheet()
        }
    }

    // MARK: - Brush Size Picker

    /// Three circles of increasing size — no labels (zen-minimal).
    private var brushSizePicker: some View {
        HStack(spacing: 12) {
            ForEach(BrushSize.allCases) { size in
                Circle()
                    .fill(brushSize == size ? Color.white : toolbarColor)
                    .frame(width: size.dotSize, height: size.dotSize)
                    .onTapGesture {
                        brushSize = size
                    }
            }
        }
    }

    // MARK: - Ink Concentration Slider

    /// Small slider with gray→black gradient indicator for 濃淡 (ink dilution).
    private var inkConcentrationSlider: some View {
        VStack(spacing: 4) {
            Text("濃淡")
                .font(.system(size: 10, weight: .light))
                .foregroundColor(textTertiary)
            HStack(spacing: 8) {
                Circle()
                    .fill(Color(white: 0.6))
                    .frame(width: 6, height: 6)

                Slider(value: $inkConcentration, in: 0.2...1.0)
                    .frame(width: 80)
                    .tint(Color(white: Double(inkConcentration) * 0.5))

                Circle()
                    .fill(Color(white: 0.1))
                    .frame(width: 6, height: 6)
            }
        }
    }
}

// MARK: - Help Sheet

private struct HelpSheet: View {
    @Environment(\.dismiss) private var dismiss

    private let bg = Color(red: 0.14, green: 0.13, blue: 0.12)
    private let heading = Color(red: 0.85, green: 0.82, blue: 0.78)
    private let body_ = Color(red: 0.65, green: 0.62, blue: 0.58)
    private let accent = Color(red: 0.5, green: 0.47, blue: 0.43)

    var body: some View {
        ZStack(alignment: .topTrailing) {
            bg.ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    // Header
                    VStack(alignment: .leading, spacing: 4) {
                        Text("書道 Brush Test")
                            .font(.system(size: 24, weight: .thin, design: .serif))
                            .foregroundColor(heading)
                        Text("Realistic fude brush calligraphy engine")
                            .font(.system(size: 14, weight: .light))
                            .foregroundColor(accent)
                    }

                    Divider().background(accent.opacity(0.3))

                    // Apple Pencil
                    helpSection(title: "Apple Pencil") {
                        helpRow("Pressure", "Controls stroke width and ink darkness")
                        helpRow("Tilt", "Flat angle creates elongated, drier strokes")
                        helpRow("Speed", "Fast strokes create 飛白 (kasure) dry brush gaps")
                        helpRow("Long strokes", "Ink naturally fades — like real brush running dry")
                    }

                    // Brush Size
                    helpSection(title: "Brush Size  ●  ●  ●") {
                        helpRow("Small (細筆)", "Fine lines — signatures, detail work")
                        helpRow("Medium (中筆)", "Standard — most kanji writing")
                        helpRow("Large (太筆)", "Bold strokes — expressive, large characters")
                    }

                    // Ink Concentration
                    helpSection(title: "濃淡  Ink Concentration") {
                        helpRow("Right (濃墨)", "Dense black ink — bold, formal writing")
                        helpRow("Left (淡墨)", "Diluted ink — warm gray-brown, softer feel")
                    }

                    // Toolbar
                    helpSection(title: "Toolbar") {
                        helpRow("🔊 / 🔇", "Toggle brush-on-paper sound")
                        helpRow("↩", "Undo last stroke")
                        helpRow("🗑", "Clear canvas")
                    }

                    // Tips
                    helpSection(title: "Tips") {
                        Text("• Start strokes slowly for ink pooling at the entry point")
                            .font(.system(size: 13, weight: .light))
                            .foregroundColor(body_)
                        Text("• Vary pressure mid-stroke for thick→thin transitions (太い→細い)")
                            .font(.system(size: 13, weight: .light))
                            .foregroundColor(body_)
                        Text("• Tilt the Pencil flat and drag sideways for side-brush (側筆) effects")
                            .font(.system(size: 13, weight: .light))
                            .foregroundColor(body_)
                        Text("• Quick flicks at stroke ends create natural 払い (harai) tails")
                            .font(.system(size: 13, weight: .light))
                            .foregroundColor(body_)
                    }

                    Spacer(minLength: 40)
                }
                .padding(32)
            }

            // Close button
            Button(action: { dismiss() }) {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 28))
                    .foregroundColor(accent)
            }
            .padding(20)
        }
    }

    private func helpSection<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(heading)
            content()
        }
    }

    private func helpRow(_ label: String, _ description: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Text(label)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(heading)
                .frame(width: 120, alignment: .leading)
            Text(description)
                .font(.system(size: 13, weight: .light))
                .foregroundColor(body_)
        }
    }
}
