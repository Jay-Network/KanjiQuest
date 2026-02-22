import Foundation
import SharedCore

/// Manual dependency injection container wrapping shared-core KMP repositories and engines.
/// Replaces Hilt/Dagger from Android — SwiftUI uses @EnvironmentObject propagation.
final class AppContainer: ObservableObject {

    // MARK: - Configuration
    let configuration: Configuration

    // MARK: - Database
    let databaseDriverFactory: DatabaseDriverFactory
    let database: KanjiQuestDatabase

    // MARK: - Repositories
    let kanjiRepository: KanjiRepositoryImpl
    let srsRepository: SrsRepositoryImpl
    let userRepository: UserRepositoryImpl
    let sessionRepository: SessionRepositoryImpl
    let achievementRepository: AchievementRepositoryImpl
    let vocabSrsRepository: VocabSrsRepositoryImpl

    // MARK: - Engine
    let srsAlgorithm: Sm2Algorithm
    let scoringEngine: ScoringEngine
    let strokeMatcher: StrokeMatcher

    // MARK: - Auth
    let authRepository: AuthRepositoryImpl

    init() {
        configuration = Configuration()

        // Stage the pre-built DB from the app bundle to Documents/ BEFORE
        // Kotlin/Native's DatabaseDriverFactory touches it.
        // NSBundle.mainBundle from inside an XCFramework may not resolve the
        // app bundle correctly, so we do this from Swift where Bundle.main is reliable.
        Self.stageBundleDatabase()

        // Database
        databaseDriverFactory = DatabaseDriverFactory()
        let driver = databaseDriverFactory.createDriver()
        database = databaseDriverFactory.createDatabase(driver: driver)

        // Repositories (param is `db:` except AchievementRepositoryImpl which uses `database:`)
        kanjiRepository = KanjiRepositoryImpl(db: database)
        srsRepository = SrsRepositoryImpl(db: database)
        userRepository = UserRepositoryImpl(db: database)
        sessionRepository = SessionRepositoryImpl(db: database)
        achievementRepository = AchievementRepositoryImpl(database: database)
        vocabSrsRepository = VocabSrsRepositoryImpl(db: database)

        // Auth (no constructor params — uses internal AuthSupabaseClientFactory)
        authRepository = AuthRepositoryImpl()

        // Algorithms
        srsAlgorithm = Sm2Algorithm()
        scoringEngine = ScoringEngine()
        strokeMatcher = StrokeMatcher.shared
    }

    // MARK: - Factory methods for scoped dependencies

    func makeGameEngine() -> GameEngine {
        let questionGenerator = QuestionGenerator(
            kanjiRepository: kanjiRepository,
            srsRepository: srsRepository,
            vocabSrsRepository: vocabSrsRepository,
            gradeMasteryProvider: nil
        )
        let sessionProvider = UserSessionProviderImpl(authRepository: authRepository)
        return GameEngine(
            questionGenerator: questionGenerator,
            srsAlgorithm: srsAlgorithm,
            srsRepository: srsRepository,
            scoringEngine: scoringEngine,
            vocabSrsRepository: vocabSrsRepository,
            userRepository: userRepository,
            wordOfTheDayVocabId: nil,
            userSessionProvider: sessionProvider,
            kanaQuestionGenerator: nil,
            kanaSrsRepository: nil,
            radicalQuestionGenerator: nil,
            radicalSrsRepository: nil,
            collectionRepository: nil,
            encounterEngine: nil,
            itemLevelEngine: nil,
            timeProvider: { KotlinLong(value: Int64(Date().timeIntervalSince1970)) }
        )
    }

    #if IPAD_TARGET
    func makeCalligraphyFeedbackService() -> CalligraphyFeedbackService {
        CalligraphyFeedbackService(apiKey: configuration.geminiApiKey)
    }
    #endif

    // MARK: - Bundle Database Staging

    private static let dbName = "kanjiquest.db"
    private static let dbVersionKey = "kanjiquest_swift_db_version"
    private static let dbVersion = 2

    /// Copy the pre-built kanji database from the app bundle into Documents/
    /// so that NativeSqliteDriver (which opens Documents/<name>) finds it.
    private static func stageBundleDatabase() {
        let fm = FileManager.default
        guard let docsURL = fm.urls(for: .documentDirectory, in: .userDomainMask).first else {
            NSLog("KanjiQuest [Swift]: Cannot resolve Documents directory")
            return
        }
        let dest = docsURL.appendingPathComponent(dbName)
        let installedVersion = UserDefaults.standard.integer(forKey: dbVersionKey)

        // Skip if correct version AND file is large enough (pre-built DB is ~24 MB)
        if fm.fileExists(atPath: dest.path) && installedVersion >= dbVersion {
            if let attrs = try? fm.attributesOfItem(atPath: dest.path),
               let size = attrs[.size] as? UInt64, size > 1_000_000 {
                NSLog("KanjiQuest [Swift]: DB already staged (v%d, %llu bytes)", installedVersion, size)
                return
            }
            NSLog("KanjiQuest [Swift]: DB file too small (%@), forcing re-copy", dest.path)
        }

        guard let bundleURL = Bundle.main.url(forResource: "kanjiquest", withExtension: "db") else {
            NSLog("KanjiQuest [Swift]: DB NOT FOUND in app bundle!")
            // Log what IS in the bundle for debugging
            if let resourcePath = Bundle.main.resourcePath {
                let contents = (try? fm.contentsOfDirectory(atPath: resourcePath)) ?? []
                NSLog("KanjiQuest [Swift]: Bundle contains %d items: %@",
                      contents.count,
                      contents.filter { $0.hasSuffix(".db") || $0 == "kanjiquest.db" }.joined(separator: ", "))
            }
            return
        }

        NSLog("KanjiQuest [Swift]: Found bundle DB at %@", bundleURL.path)

        // Remove stale file
        if fm.fileExists(atPath: dest.path) {
            do {
                try fm.removeItem(at: dest)
            } catch {
                NSLog("KanjiQuest [Swift]: Failed to remove stale DB: %@", error.localizedDescription)
            }
        }

        do {
            try fm.copyItem(at: bundleURL, to: dest)
            UserDefaults.standard.set(dbVersion, forKey: dbVersionKey)
            let size = (try? fm.attributesOfItem(atPath: dest.path))?[.size] as? UInt64 ?? 0
            NSLog("KanjiQuest [Swift]: DB copied successfully (%llu bytes)", size)
        } catch {
            NSLog("KanjiQuest [Swift]: Failed to copy DB: %@", error.localizedDescription)
        }
    }
}
