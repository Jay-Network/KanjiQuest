import SwiftUI

struct BrushTestView: View {
    @State private var strokes: [[CalligraphyPointData]] = []
    @State private var activeStroke: [CalligraphyPointData] = []
    @State private var canvasVersion = 0
    @State private var strokeCount = 0
    @State private var brushSize: BrushSize = .medium
    @State private var inkConcentration: CGFloat = 1.0
    @State private var isSoundMuted: Bool = false

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
