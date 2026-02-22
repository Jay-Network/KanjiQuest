import SwiftUI

struct BrushTestView: View {
    @State private var strokes: [[CalligraphyPointData]] = []
    @State private var activeStroke: [CalligraphyPointData] = []
    @State private var canvasVersion = 0
    @State private var strokeCount = 0

    var body: some View {
        ZStack {
            // Warm dark background
            Color(red: 0.12, green: 0.11, blue: 0.10)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // Title bar
                HStack {
                    Text("書道")
                        .font(.system(size: 28, weight: .thin, design: .serif))
                        .foregroundColor(Color(red: 0.85, green: 0.82, blue: 0.78))
                    Text("Brush Test")
                        .font(.system(size: 16, weight: .light, design: .default))
                        .foregroundColor(Color(red: 0.6, green: 0.57, blue: 0.53))
                    Spacer()
                    Text("\(strokeCount) strokes")
                        .font(.system(size: 14, weight: .light, design: .monospaced))
                        .foregroundColor(Color(red: 0.5, green: 0.47, blue: 0.43))
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 12)

                // Canvas — the star of the show
                GeometryReader { geo in
                    let side = min(geo.size.width - 48, geo.size.height - 24)
                    HStack {
                        Spacer()
                        CalligraphyCanvasView(
                            strokes: $strokes,
                            activeStroke: $activeStroke,
                            referenceStrokePaths: [],
                            canvasVersion: canvasVersion,
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
                HStack(spacing: 32) {
                    Button(action: {
                        canvasVersion += 1
                        strokes = []
                        activeStroke = []
                        strokeCount = 0
                    }) {
                        VStack(spacing: 4) {
                            Image(systemName: "trash")
                                .font(.system(size: 20))
                            Text("Clear")
                                .font(.system(size: 11, weight: .light))
                        }
                        .foregroundColor(Color(red: 0.75, green: 0.35, blue: 0.30))
                    }

                    Button(action: {
                        if !strokes.isEmpty {
                            strokes.removeLast()
                            strokeCount = max(0, strokeCount - 1)
                            canvasVersion += 1
                        }
                    }) {
                        VStack(spacing: 4) {
                            Image(systemName: "arrow.uturn.backward")
                                .font(.system(size: 20))
                            Text("Undo")
                                .font(.system(size: 11, weight: .light))
                        }
                        .foregroundColor(Color(red: 0.7, green: 0.67, blue: 0.63))
                    }
                }
                .padding(.vertical, 16)
            }
        }
    }
}
