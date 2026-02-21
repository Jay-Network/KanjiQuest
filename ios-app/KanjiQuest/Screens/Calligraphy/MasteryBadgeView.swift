import SwiftUI
import SharedCore

/// Displays the appropriate calligraphy mastery grade badge image
/// based on the shared-core MasteryLevel enum.
struct MasteryBadgeView: View {
    let level: MasteryLevel
    let size: CGFloat

    init(level: MasteryLevel, size: CGFloat = 64) {
        self.level = level
        self.size = size
    }

    private var imageName: String {
        switch level {
        case .beginning:  return "Calligraphy/GradeBeginning"
        case .developing: return "Calligraphy/GradeDeveloping"
        case .proficient: return "Calligraphy/GradeProficient"
        case .advanced:   return "Calligraphy/GradeAdvanced"
        default:          return "Calligraphy/GradeBeginning"
        }
    }

    var body: some View {
        VStack(spacing: 4) {
            Image(imageName)
                .resizable()
                .scaledToFit()
                .frame(width: size, height: size)
                .clipShape(RoundedRectangle(cornerRadius: size * 0.1))

            Text(level.labelJp)
                .font(.system(size: size * 0.18, weight: .medium))
                .foregroundColor(.secondary)
        }
    }
}
