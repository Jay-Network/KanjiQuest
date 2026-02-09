package com.jworks.kanjiquest.android.di

import android.content.Context
import com.jworks.kanjiquest.android.BuildConfig
import com.jworks.kanjiquest.android.data.PreviewTrialManager
import com.jworks.kanjiquest.android.network.GeminiClient
import com.jworks.kanjiquest.android.ui.game.writing.HandwritingChecker
import com.jworks.kanjiquest.core.data.AchievementRepositoryImpl
import com.jworks.kanjiquest.core.data.AuthRepositoryImpl
import com.jworks.kanjiquest.core.data.DatabaseDriverFactory
import com.jworks.kanjiquest.core.data.JCoinRepositoryImpl
import com.jworks.kanjiquest.core.data.KanjiRepositoryImpl
import com.jworks.kanjiquest.core.data.LearningSyncRepositoryImpl
import com.jworks.kanjiquest.core.data.SessionRepositoryImpl
import com.jworks.kanjiquest.core.data.SrsRepositoryImpl
import com.jworks.kanjiquest.core.data.UserRepositoryImpl
import com.jworks.kanjiquest.core.data.VocabSrsRepositoryImpl
import com.jworks.kanjiquest.core.domain.UserSessionProvider
import com.jworks.kanjiquest.core.domain.UserSessionProviderImpl
import com.jworks.kanjiquest.core.domain.repository.AchievementRepository
import com.jworks.kanjiquest.core.domain.repository.AuthRepository
import com.jworks.kanjiquest.core.domain.repository.JCoinRepository
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import com.jworks.kanjiquest.core.domain.repository.LearningSyncRepository
import com.jworks.kanjiquest.core.domain.repository.SessionRepository
import com.jworks.kanjiquest.core.domain.repository.SrsRepository
import com.jworks.kanjiquest.core.domain.repository.UserRepository
import com.jworks.kanjiquest.core.domain.repository.VocabSrsRepository
import com.jworks.kanjiquest.core.domain.usecase.CompleteSessionUseCase
import com.jworks.kanjiquest.core.domain.usecase.DataRestorationUseCase
import com.jworks.kanjiquest.core.domain.usecase.MigrateLocalDataUseCase
import com.jworks.kanjiquest.core.domain.usecase.WordOfTheDayUseCase
import com.jworks.kanjiquest.core.engine.GameEngine
import com.jworks.kanjiquest.core.engine.GradeMasteryProvider
import com.jworks.kanjiquest.core.engine.QuestionGenerator
import com.jworks.kanjiquest.core.scoring.ScoringEngine
import com.jworks.kanjiquest.core.srs.Sm2Algorithm
import com.jworks.kanjiquest.core.srs.SrsAlgorithm
import com.jworks.kanjiquest.db.KanjiQuestDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KanjiQuestDatabase {
        val driver = DatabaseDriverFactory(context).createDriver()
        return KanjiQuestDatabase(driver)
    }

    @Provides
    @Singleton
    fun provideKanjiRepository(db: KanjiQuestDatabase): KanjiRepository {
        return KanjiRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun provideSrsRepository(db: KanjiQuestDatabase): SrsRepository {
        return SrsRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun provideUserRepository(db: KanjiQuestDatabase): UserRepository {
        return UserRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(db: KanjiQuestDatabase): SessionRepository {
        return SessionRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun provideJCoinRepository(db: KanjiQuestDatabase): JCoinRepository {
        return JCoinRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun provideSrsAlgorithm(): SrsAlgorithm {
        return Sm2Algorithm()
    }

    @Provides
    @Singleton
    fun provideScoringEngine(): ScoringEngine {
        return ScoringEngine()
    }

    @Provides
    @Singleton
    fun provideVocabSrsRepository(db: KanjiQuestDatabase): VocabSrsRepository {
        return VocabSrsRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun provideAchievementRepository(db: KanjiQuestDatabase): AchievementRepository {
        return AchievementRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository {
        return AuthRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideLearningSyncRepository(db: KanjiQuestDatabase): LearningSyncRepository {
        return LearningSyncRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun provideUserSessionProvider(authRepository: AuthRepository): UserSessionProvider {
        return UserSessionProviderImpl(authRepository)
    }

    @Provides
    @Singleton
    fun provideWordOfTheDayUseCase(kanjiRepository: KanjiRepository): WordOfTheDayUseCase {
        return WordOfTheDayUseCase(kanjiRepository)
    }

    @Provides
    fun provideQuestionGenerator(
        kanjiRepository: KanjiRepository,
        srsRepository: SrsRepository,
        vocabSrsRepository: VocabSrsRepository
    ): QuestionGenerator {
        val masteryProvider = GradeMasteryProvider { grade ->
            val total = kanjiRepository.getKanjiCountByGrade(grade)
            srsRepository.getGradeMastery(grade, total)
        }
        return QuestionGenerator(kanjiRepository, srsRepository, vocabSrsRepository, masteryProvider)
    }

    @Provides
    fun provideGameEngine(
        questionGenerator: QuestionGenerator,
        srsAlgorithm: SrsAlgorithm,
        srsRepository: SrsRepository,
        scoringEngine: ScoringEngine,
        vocabSrsRepository: VocabSrsRepository,
        userRepository: UserRepository,
        userSessionProvider: UserSessionProvider
    ): GameEngine {
        return GameEngine(
            questionGenerator, srsAlgorithm, srsRepository, scoringEngine,
            vocabSrsRepository, userRepository,
            userSessionProvider = userSessionProvider
        )
    }

    @Provides
    fun provideCompleteSessionUseCase(
        userRepository: UserRepository,
        sessionRepository: SessionRepository,
        scoringEngine: ScoringEngine,
        jCoinRepository: JCoinRepository,
        userSessionProvider: UserSessionProvider,
        learningSyncRepository: LearningSyncRepository,
        achievementRepository: AchievementRepository
    ): CompleteSessionUseCase {
        return CompleteSessionUseCase(
            userRepository, sessionRepository, scoringEngine,
            jCoinRepository, userSessionProvider,
            learningSyncRepository, achievementRepository
        )
    }

    @Provides
    fun provideMigrateLocalDataUseCase(db: KanjiQuestDatabase): MigrateLocalDataUseCase {
        return MigrateLocalDataUseCase(db)
    }

    @Provides
    fun provideDataRestorationUseCase(
        learningSyncRepository: LearningSyncRepository,
        userRepository: UserRepository,
        srsRepository: SrsRepository
    ): DataRestorationUseCase {
        return DataRestorationUseCase(learningSyncRepository, userRepository, srsRepository)
    }

    @Provides
    @Singleton
    fun providePreviewTrialManager(@ApplicationContext context: Context): PreviewTrialManager {
        return PreviewTrialManager(context)
    }

    @Provides
    @Singleton
    fun provideGeminiClient(): GeminiClient {
        return GeminiClient(BuildConfig.GEMINI_API_KEY)
    }

    @Provides
    @Singleton
    fun provideHandwritingChecker(geminiClient: GeminiClient): HandwritingChecker {
        return HandwritingChecker(geminiClient)
    }
}
