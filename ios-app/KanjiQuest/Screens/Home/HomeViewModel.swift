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
                // Show diagnostics so we can see exactly what went wrong
                let diag = Self.dbDiagnostics(factory: container.databaseDriverFactory)
                kanjiLoadError = "No kanji (grade=1) found.\n\n\(diag)"
            }
        } catch {
            let diag = Self.dbDiagnostics(factory: container.databaseDriverFactory)
            kanjiLoadError = "Failed to load kanji: \(error.localizedDescription)\n\n\(diag)"
        }
        isLoadingKanji = false
    }

    private static func dbDiagnostics(factory: DatabaseDriverFactory) -> String {
        let fm = FileManager.default
        var lines: [String] = []

        // 1. Where Kotlin opens the DB
        let kotlinPath = factory.resolvedDbPath
        lines.append("KN path: \(kotlinPath)")

        // 2. Does that file exist? How big?
        if fm.fileExists(atPath: kotlinPath) {
            let size = (try? fm.attributesOfItem(atPath: kotlinPath))?[.size] as? UInt64 ?? 0
            lines.append("KN file: \(size) bytes")
        } else {
            lines.append("KN file: MISSING")
        }

        // 3. Bundle resource check
        if let bundleURL = Bundle.main.url(forResource: "kanjiquest", withExtension: "db") {
            let bSize = (try? fm.attributesOfItem(atPath: bundleURL.path))?[.size] as? UInt64 ?? 0
            lines.append("Bundle DB: \(bSize) bytes at \(bundleURL.lastPathComponent)")
        } else {
            lines.append("Bundle DB: NOT FOUND")
        }

        // 4. Documents dir check
        if let docsURL = fm.urls(for: .documentDirectory, in: .userDomainMask).first {
            let docsPath = docsURL.appendingPathComponent("kanjiquest.db").path
            if fm.fileExists(atPath: docsPath) {
                let dSize = (try? fm.attributesOfItem(atPath: docsPath))?[.size] as? UInt64 ?? 0
                lines.append("Docs DB: \(dSize) bytes")
            } else {
                lines.append("Docs DB: MISSING")
            }
            lines.append("Docs dir: \(docsURL.path)")
        }

        // 5. Swift staging version
        let ver = UserDefaults.standard.integer(forKey: "kanjiquest_swift_db_version")
        lines.append("Swift DB ver: \(ver)")

        return lines.joined(separator: "\n")
    }
}
