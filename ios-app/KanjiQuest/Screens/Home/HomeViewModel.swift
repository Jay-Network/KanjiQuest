import Foundation
import SharedCore

@MainActor
final class HomeViewModel: ObservableObject {

    struct UserProfileData {
        let level: Int
        let xp: Int
        let xpToNext: Int
        let xpProgress: CGFloat  // 0.0-1.0
    }

    struct KanjiData {
        let literal: String
        let strokePaths: [String]
    }

    @Published var userProfile: UserProfileData?
    @Published var nextKanji: KanjiData?
    @Published var isLoadingKanji = true
    @Published var kanjiLoadError: String?

    func load(container: AppContainer) async {
        // Load user profile from shared-core
        do {
            let profile = try await container.userRepository.getProfile()
            let level = Int(profile.level)
            let xp = Int(profile.totalXp)
            let threshold = level * level * 50  // level²×50
            let prevThreshold = max(0, (level - 1) * (level - 1) * 50)
            let xpInLevel = xp - prevThreshold
            let xpNeeded = threshold - prevThreshold

            userProfile = UserProfileData(
                level: level,
                xp: xp,
                xpToNext: max(0, threshold - xp),
                xpProgress: xpNeeded > 0 ? CGFloat(xpInLevel) / CGFloat(xpNeeded) : 0
            )
        } catch {
            userProfile = UserProfileData(level: 1, xp: 0, xpToNext: 50, xpProgress: 0)
        }

        // Load a kanji for the practice button preview
        isLoadingKanji = true
        do {
            let kanjiList = try await container.kanjiRepository.getKanjiByGrade(grade: 1)
            if let first = kanjiList.first {
                nextKanji = KanjiData(
                    literal: first.literal,
                    strokePaths: first.strokeSvg?.components(separatedBy: "|||") ?? []
                )
            } else {
                kanjiLoadError = "No kanji data in database. The database may not be bundled correctly."
            }
        } catch {
            kanjiLoadError = "Failed to load kanji: \(error.localizedDescription)"
        }
        isLoadingKanji = false
    }
}
