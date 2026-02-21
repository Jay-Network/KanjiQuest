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

        // Database
        databaseDriverFactory = DatabaseDriverFactory()
        let driver = databaseDriverFactory.createDriver()
        database = KanjiQuestDatabase(driver: driver)

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
            userSessionProvider: sessionProvider
        )
    }

    func makeCalligraphyFeedbackService() -> CalligraphyFeedbackService {
        CalligraphyFeedbackService(apiKey: configuration.geminiApiKey)
    }
}
