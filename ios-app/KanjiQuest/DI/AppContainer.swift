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
    let authRepository: AuthRepository

    init() {
        configuration = Configuration()

        // Database
        databaseDriverFactory = DatabaseDriverFactory()
        let driver = databaseDriverFactory.createDriver()
        database = KanjiQuestDatabase(driver: driver)

        // Repositories
        kanjiRepository = KanjiRepositoryImpl(database: database)
        srsRepository = SrsRepositoryImpl(database: database)
        userRepository = UserRepositoryImpl(database: database)
        sessionRepository = SessionRepositoryImpl(database: database)
        achievementRepository = AchievementRepositoryImpl(database: database)
        vocabSrsRepository = VocabSrsRepositoryImpl(database: database)

        // Auth
        authRepository = AuthRepository(
            supabaseUrl: configuration.supabaseUrl,
            supabaseAnonKey: configuration.supabaseAnonKey
        )

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
            gradeMasteryProvider: { [weak self] grade in
                guard let self else { return GradeMastery.companion.empty() }
                return self.kanjiRepository.getGradeMastery(grade: grade)
            }
        )
        return GameEngine(
            questionGenerator: questionGenerator,
            srsAlgorithm: srsAlgorithm,
            srsRepository: srsRepository,
            scoringEngine: scoringEngine,
            vocabSrsRepository: vocabSrsRepository,
            userRepository: userRepository,
            userSessionProvider: authRepository
        )
    }

    func makeCalligraphyFeedbackService() -> CalligraphyFeedbackService {
        CalligraphyFeedbackService(apiKey: configuration.geminiApiKey)
    }
}
