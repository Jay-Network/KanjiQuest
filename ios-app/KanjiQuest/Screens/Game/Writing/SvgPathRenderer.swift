import SwiftUI

/// Renders SVG paths as a stroke order reference image.
struct SvgPathRenderer: View {
    let paths: [String]

    var body: some View {
        HStack(spacing: 4) {
            ForEach(Array(paths.enumerated()), id: \.offset) { index, _ in
                StrokeRenderer(strokePaths: paths, highlightIndex: index)
                    .frame(width: 40, height: 40)
                    .background(Color.white)
                    .cornerRadius(4)
                    .overlay(
                        RoundedRectangle(cornerRadius: 4)
                            .stroke(Color.gray.opacity(0.2), lineWidth: 1)
                    )
                    .overlay(
                        Text("\(index + 1)")
                            .font(.system(size: 8))
                            .foregroundColor(.secondary)
                            .padding(2),
                        alignment: .bottomTrailing
                    )
            }
        }
    }
}
