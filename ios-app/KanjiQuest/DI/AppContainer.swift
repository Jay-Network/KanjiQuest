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

    // MARK: - Preview Trial Manager (UserDefaults-based, like Android SharedPreferences)
    let previewTrialManager: PreviewTrialManager

    // MARK: - Gemini AI
    let geminiClient: GeminiClient
    let handwritingChecker: HandwritingChecker

    init() {
        NSLog("KanjiQuest [AppContainer]: init() START")

        configuration = Configuration()
        NSLog("KanjiQuest [AppContainer]: Configuration OK")

        // Stage the pre-built DB from the app bundle to Documents/ BEFORE
        // Kotlin/Native's DatabaseDriverFactory touches it.
        Self.stageBundleDatabase()
        NSLog("KanjiQuest [AppContainer]: stageBundleDatabase() OK")

        // Database
        NSLog("KanjiQuest [AppContainer]: Creating DatabaseDriverFactory...")
        databaseDriverFactory = DatabaseDriverFactory()
        NSLog("KanjiQuest [AppContainer]: DatabaseDriverFactory OK, creating driver...")
        let driver = databaseDriverFactory.createDriver()
        NSLog("KanjiQuest [AppContainer]: Driver OK, creating database...")
        database = databaseDriverFactory.createDatabase(driver: driver)
        NSLog("KanjiQuest [AppContainer]: Database OK")

        // Core Repositories
        NSLog("KanjiQuest [AppContainer]: Creating repositories...")
        kanjiRepository = KanjiRepositoryImpl(db: database)
        srsRepository = SrsRepositoryImpl(db: database)
        userRepository = UserRepositoryImpl(db: database)
        sessionRepository = SessionRepositoryImpl(db: database)
        achievementRepository = AchievementRepositoryImpl(database: database)
        vocabSrsRepository = VocabSrsRepositoryImpl(db: database)
        authRepository = AuthRepositoryImpl()
        jCoinRepository = JCoinRepositoryImpl(database: database)
        flashcardRepository = FlashcardRepositoryImpl(db: database)
        kanaRepository = KanaRepositoryImpl(db: database)
        kanaSrsRepository = KanaSrsRepositoryImpl(db: database)
        radicalRepository = RadicalRepositoryImpl(db: database)
        radicalSrsRepository = RadicalSrsRepositoryImpl(db: database)
        collectionRepository = CollectionRepositoryImpl(db: database)
        devChatRepository = DevChatRepositoryImpl()
        feedbackRepository = FeedbackRepositoryImpl()
        fieldJournalRepository = FieldJournalRepositoryImpl(db: database)
        learningSyncRepository = LearningSyncRepositoryImpl(database: database)
        NSLog("KanjiQuest [AppContainer]: All repositories OK")

        // Algorithms
        NSLog("KanjiQuest [AppContainer]: Creating algorithms...")
        srsAlgorithm = Sm2Algorithm()
        scoringEngine = ScoringEngine()
        NSLog("KanjiQuest [AppContainer]: Sm2Algorithm + ScoringEngine OK, accessing StrokeMatcher.shared...")
        strokeMatcher = StrokeMatcher.shared
        NSLog("KanjiQuest [AppContainer]: StrokeMatcher OK")

        // Collection Engines
        NSLog("KanjiQuest [AppContainer]: Creating EncounterEngine...")
        encounterEngine = EncounterEngine(
            collectionRepository: collectionRepository,
            kanjiRepository: kanjiRepository
        )
        NSLog("KanjiQuest [AppContainer]: Creating ItemLevelEngine...")
        itemLevelEngine = ItemLevelEngine(
            collectionRepository: collectionRepository
        )
        NSLog("KanjiQuest [AppContainer]: Collection engines OK")

        // User Session
        NSLog("KanjiQuest [AppContainer]: Creating UserSessionProviderImpl...")
        userSessionProvider = UserSessionProviderImpl(authRepository: authRepository)
        NSLog("KanjiQuest [AppContainer]: UserSessionProvider OK")

        // Use Cases
        NSLog("KanjiQuest [AppContainer]: Creating use cases...")
        wordOfTheDayUseCase = WordOfTheDayUseCase(kanjiRepository: kanjiRepository)
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
        NSLog("KanjiQuest [AppContainer]: Use cases OK")

        // Preview Trial Manager
        previewTrialManager = PreviewTrialManager()

        // Gemini AI
        geminiClient = GeminiClient(apiKey: configuration.geminiApiKey)
        handwritingChecker = HandwritingChecker(geminiClient: geminiClient)

        NSLog("KanjiQuest [AppContainer]: init() COMPLETE - all dependencies initialized")
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
