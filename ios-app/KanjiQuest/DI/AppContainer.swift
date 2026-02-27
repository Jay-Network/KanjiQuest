import Foundation
import SharedCore

/// Manual dependency injection container wrapping shared-core KMP repositories and engines.
/// Replaces Hilt/Dagger from Android — SwiftUI uses @EnvironmentObject propagation.
/// Mirrors Android AppModule.kt with all repositories and engines.
final class AppContainer: ObservableObject {

    // MARK: - Initialization status
    @Published var initError: String? = nil

    // MARK: - Configuration
    let configuration: Configuration

    // MARK: - Database
    let databaseDriverFactory: DatabaseDriverFactory
    let database: KanjiQuestDatabase

    // MARK: - Core Repositories (from Android AppModule)
    let kanjiRepository: KanjiRepositoryImpl
    let srsRepository: SrsRepositoryImpl
    let userRepository: UserRepositoryImpl
    let sessionRepository: SessionRepositoryImpl
    let achievementRepository: AchievementRepositoryImpl
    let vocabSrsRepository: VocabSrsRepositoryImpl
    let authRepository: AuthRepositoryImpl
    let jCoinRepository: JCoinRepositoryImpl
    let flashcardRepository: FlashcardRepositoryImpl
    let kanaRepository: KanaRepositoryImpl
    let kanaSrsRepository: KanaSrsRepositoryImpl
    let radicalRepository: RadicalRepositoryImpl
    let radicalSrsRepository: RadicalSrsRepositoryImpl
    let collectionRepository: CollectionRepositoryImpl
    let devChatRepository: DevChatRepositoryImpl
    let feedbackRepository: FeedbackRepositoryImpl
    let fieldJournalRepository: FieldJournalRepositoryImpl
    let learningSyncRepository: LearningSyncRepositoryImpl

    // MARK: - Algorithms & Engines
    let srsAlgorithm: Sm2Algorithm
    let scoringEngine: ScoringEngine
    let strokeMatcher: StrokeMatcher

    // MARK: - Collection Engines
    let encounterEngine: EncounterEngine
    let itemLevelEngine: ItemLevelEngine

    // MARK: - User Session
    let userSessionProvider: UserSessionProviderImpl

    // MARK: - Use Cases
    let wordOfTheDayUseCase: WordOfTheDayUseCase
    let completeSessionUseCase: CompleteSessionUseCase

    // MARK: - Sync Service
    let syncService: SyncService

    // MARK: - Preview Trial Manager (UserDefaults-based, like Android SharedPreferences)
    let previewTrialManager: PreviewTrialManager

    // MARK: - Gemini AI
    let geminiClient: GeminiClient
    let handwritingChecker: HandwritingChecker

    init() {
        CrashDiagnostic.step("AppContainer.init() START")

        configuration = Configuration()
        CrashDiagnostic.step("Configuration OK")

        // Stage the pre-built DB from the app bundle to Documents/ BEFORE
        // Kotlin/Native's DatabaseDriverFactory touches it.
        Self.stageBundleDatabase()
        CrashDiagnostic.step("stageBundleDatabase() OK")

        // Database
        CrashDiagnostic.step("Creating DatabaseDriverFactory...")
        databaseDriverFactory = DatabaseDriverFactory()
        CrashDiagnostic.step("DatabaseDriverFactory() OK, creating driver...")
        let driver = databaseDriverFactory.createDriver()
        CrashDiagnostic.step("createDriver() OK, creating database...")
        database = databaseDriverFactory.createDatabase(driver: driver)
        CrashDiagnostic.step("createDatabase() OK")

        // Core Repositories — one by one for crash isolation
        CrashDiagnostic.step("Creating KanjiRepositoryImpl...")
        kanjiRepository = KanjiRepositoryImpl(db: database)
        CrashDiagnostic.step("Creating SrsRepositoryImpl...")
        srsRepository = SrsRepositoryImpl(db: database)
        CrashDiagnostic.step("Creating UserRepositoryImpl...")
        userRepository = UserRepositoryImpl(db: database)
        CrashDiagnostic.step("Creating SessionRepositoryImpl...")
        sessionRepository = SessionRepositoryImpl(db: database)
        CrashDiagnostic.step("Creating AchievementRepositoryImpl...")
        achievementRepository = AchievementRepositoryImpl(database: database)
        CrashDiagnostic.step("Creating VocabSrsRepositoryImpl...")
        vocabSrsRepository = VocabSrsRepositoryImpl(db: database)
        CrashDiagnostic.step("Creating AuthRepositoryImpl...")
        authRepository = AuthRepositoryImpl()
        CrashDiagnostic.step("Creating JCoinRepositoryImpl...")
        jCoinRepository = JCoinRepositoryImpl(database: database)
        CrashDiagnostic.step("Creating FlashcardRepositoryImpl...")
        flashcardRepository = FlashcardRepositoryImpl(db: database)
        CrashDiagnostic.step("Creating KanaRepositoryImpl...")
        kanaRepository = KanaRepositoryImpl(db: database)
        CrashDiagnostic.step("Creating KanaSrsRepositoryImpl...")
        kanaSrsRepository = KanaSrsRepositoryImpl(db: database)
        CrashDiagnostic.step("Creating RadicalRepositoryImpl...")
        radicalRepository = RadicalRepositoryImpl(db: database)
        CrashDiagnostic.step("Creating RadicalSrsRepositoryImpl...")
        radicalSrsRepository = RadicalSrsRepositoryImpl(db: database)
        CrashDiagnostic.step("Creating CollectionRepositoryImpl...")
        collectionRepository = CollectionRepositoryImpl(db: database)
        CrashDiagnostic.step("Creating DevChatRepositoryImpl...")
        devChatRepository = DevChatRepositoryImpl()
        CrashDiagnostic.step("Creating FeedbackRepositoryImpl...")
        feedbackRepository = FeedbackRepositoryImpl()
        CrashDiagnostic.step("Creating FieldJournalRepositoryImpl...")
        fieldJournalRepository = FieldJournalRepositoryImpl(db: database)
        CrashDiagnostic.step("Creating LearningSyncRepositoryImpl...")
        learningSyncRepository = LearningSyncRepositoryImpl(database: database)
        CrashDiagnostic.step("All repositories OK")

        // Algorithms
        CrashDiagnostic.step("Creating Sm2Algorithm...")
        srsAlgorithm = Sm2Algorithm()
        CrashDiagnostic.step("Creating ScoringEngine...")
        scoringEngine = ScoringEngine()
        CrashDiagnostic.step("Accessing StrokeMatcher.shared...")
        strokeMatcher = StrokeMatcher.shared
        CrashDiagnostic.step("StrokeMatcher OK")

        // Collection Engines
        CrashDiagnostic.step("Creating EncounterEngine...")
        encounterEngine = EncounterEngine(
            collectionRepository: collectionRepository,
            kanjiRepository: kanjiRepository
        )
        CrashDiagnostic.step("Creating ItemLevelEngine...")
        itemLevelEngine = ItemLevelEngine(
            collectionRepository: collectionRepository
        )
        CrashDiagnostic.step("Collection engines OK")

        // User Session
        CrashDiagnostic.step("Creating UserSessionProviderImpl...")
        userSessionProvider = UserSessionProviderImpl(authRepository: authRepository)
        CrashDiagnostic.step("UserSessionProvider OK")

        // Use Cases
        CrashDiagnostic.step("Creating WordOfTheDayUseCase...")
        wordOfTheDayUseCase = WordOfTheDayUseCase(kanjiRepository: kanjiRepository)
        CrashDiagnostic.step("Creating CompleteSessionUseCase...")
        completeSessionUseCase = CompleteSessionUseCase(
            userRepository: userRepository,
            sessionRepository: sessionRepository,
            scoringEngine: scoringEngine,
            jCoinRepository: jCoinRepository,
            userSessionProvider: userSessionProvider,
            learningSyncRepository: learningSyncRepository,
            achievementRepository: achievementRepository,
            srsRepository: srsRepository,
            kanjiRepository: kanjiRepository
        )
        CrashDiagnostic.step("Use cases OK")

        // Sync Service
        CrashDiagnostic.step("Creating SyncService...")
        syncService = SyncService(
            learningSyncRepository: learningSyncRepository,
            userSessionProvider: userSessionProvider
        )
        // NOTE: BGTaskScheduler.register must be called during app launch (didFinishLaunching).
        // Deferring to avoid crashes when AppContainer is created after launch.
        // registerBackgroundTask() is called from KanjiQuestApp.init or .task.
        CrashDiagnostic.step("SyncService OK (background task deferred)")

        // Preview Trial Manager
        CrashDiagnostic.step("Creating PreviewTrialManager...")
        previewTrialManager = PreviewTrialManager()

        // Gemini AI
        CrashDiagnostic.step("Creating GeminiClient...")
        geminiClient = GeminiClient(apiKey: configuration.geminiApiKey)
        CrashDiagnostic.step("Creating HandwritingChecker...")
        handwritingChecker = HandwritingChecker(geminiClient: geminiClient)

        CrashDiagnostic.step("AppContainer.init() COMPLETE")
        CrashDiagnostic.complete()
    }

    // MARK: - Factory methods for scoped dependencies

    func makeQuestionGenerator() -> QuestionGenerator {
        return QuestionGenerator(
            kanjiRepository: kanjiRepository,
            srsRepository: srsRepository,
            vocabSrsRepository: vocabSrsRepository,
            gradeMasteryProvider: nil
        )
    }

    func makeKanaQuestionGenerator() -> KanaQuestionGenerator {
        return KanaQuestionGenerator(
            kanaRepository: kanaRepository,
            kanaSrsRepository: kanaSrsRepository
        )
    }

    func makeRadicalQuestionGenerator() -> RadicalQuestionGenerator {
        return RadicalQuestionGenerator(
            radicalRepository: radicalRepository,
            radicalSrsRepository: radicalSrsRepository,
            kanjiRepository: kanjiRepository
        )
    }

    func makeGameEngine() -> GameEngine {
        let questionGenerator = makeQuestionGenerator()
        return GameEngine(
            questionGenerator: questionGenerator,
            srsAlgorithm: srsAlgorithm,
            srsRepository: srsRepository,
            scoringEngine: scoringEngine,
            vocabSrsRepository: vocabSrsRepository,
            userRepository: userRepository,
            wordOfTheDayVocabId: nil,
            userSessionProvider: userSessionProvider,
            kanaQuestionGenerator: makeKanaQuestionGenerator(),
            kanaSrsRepository: kanaSrsRepository,
            radicalQuestionGenerator: makeRadicalQuestionGenerator(),
            radicalSrsRepository: radicalSrsRepository,
            collectionRepository: collectionRepository,
            encounterEngine: encounterEngine,
            itemLevelEngine: itemLevelEngine,
            timeProvider: { KotlinLong(value: Int64(Date().timeIntervalSince1970)) }
        )
    }

    func makeCompleteSessionUseCase() -> CompleteSessionUseCase {
        completeSessionUseCase
    }

    func makeHandwritingChecker() -> HandwritingChecker {
        handwritingChecker
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

    private static func stageBundleDatabase() {
        let fm = FileManager.default
        guard let docsURL = fm.urls(for: .documentDirectory, in: .userDomainMask).first else {
            NSLog("KanjiQuest [Swift]: Cannot resolve Documents directory")
            return
        }
        let dest = docsURL.appendingPathComponent(dbName)
        let installedVersion = UserDefaults.standard.integer(forKey: dbVersionKey)

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
            if let resourcePath = Bundle.main.resourcePath {
                let contents = (try? fm.contentsOfDirectory(atPath: resourcePath)) ?? []
                NSLog("KanjiQuest [Swift]: Bundle contains %d items: %@",
                      contents.count,
                      contents.filter { $0.hasSuffix(".db") || $0 == "kanjiquest.db" }.joined(separator: ", "))
            }
            return
        }

        NSLog("KanjiQuest [Swift]: Found bundle DB at %@", bundleURL.path)

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
