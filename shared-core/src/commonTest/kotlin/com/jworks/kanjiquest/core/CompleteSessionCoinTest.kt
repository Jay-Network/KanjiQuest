package com.jworks.kanjiquest.core

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.jworks.kanjiquest.core.data.JCoinRepositoryImpl
import com.jworks.kanjiquest.core.data.SessionRepositoryImpl
import com.jworks.kanjiquest.core.data.UserRepositoryImpl
import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.model.LOCAL_USER_ID
import com.jworks.kanjiquest.core.domain.usecase.CompleteSessionUseCase
import com.jworks.kanjiquest.core.engine.SessionStats
import com.jworks.kanjiquest.core.scoring.ScoringEngine
import com.jworks.kanjiquest.db.KanjiQuestDatabase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompleteSessionCoinTest {

    private fun createTestDb(): KanjiQuestDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KanjiQuestDatabase.Schema.create(driver)
        return KanjiQuestDatabase(driver)
    }

    private fun createUseCase(db: KanjiQuestDatabase = createTestDb()): Pair<CompleteSessionUseCase, JCoinRepositoryImpl> {
        val fixedClock = object : Clock {
            override fun now() = Instant.fromEpochSeconds(1700000000L)
        }
        val userRepo = UserRepositoryImpl(db)
        val sessionRepo = SessionRepositoryImpl(db)
        val jCoinRepo = JCoinRepositoryImpl(db, fixedClock)
        val useCase = CompleteSessionUseCase(userRepo, sessionRepo, ScoringEngine(), jCoinRepo)
        return useCase to jCoinRepo
    }

    @Test
    fun session20Cards_earns10Coins() = runTest {
        val (useCase, jCoinRepo) = createUseCase()
        val stats = SessionStats(
            gameMode = GameMode.RECOGNITION,
            cardsStudied = 20,
            correctCount = 15,
            comboMax = 5,
            xpEarned = 100,
            durationSec = 300
        )

        val result = useCase.execute(stats)
        assertEquals(10, result.coinsEarned)

        val balance = jCoinRepo.getBalance(LOCAL_USER_ID)
        assertEquals(10L, balance.localBalance)
    }

    @Test
    fun session10Cards_earns5Coins() = runTest {
        val (useCase, jCoinRepo) = createUseCase()
        val stats = SessionStats(
            gameMode = GameMode.RECOGNITION,
            cardsStudied = 10,
            correctCount = 7,
            comboMax = 3,
            xpEarned = 50,
            durationSec = 120
        )

        val result = useCase.execute(stats)
        assertEquals(5, result.coinsEarned)
    }

    @Test
    fun sessionUnder10Cards_earns0Coins() = runTest {
        val (useCase, _) = createUseCase()
        val stats = SessionStats(
            gameMode = GameMode.RECOGNITION,
            cardsStudied = 5,
            correctCount = 5,
            comboMax = 5,
            xpEarned = 30,
            durationSec = 60
        )

        val result = useCase.execute(stats)
        assertEquals(0, result.coinsEarned)
    }

    @Test
    fun perfectScore10Cards_earns30Coins() = runTest {
        val (useCase, _) = createUseCase()
        val stats = SessionStats(
            gameMode = GameMode.RECOGNITION,
            cardsStudied = 10,
            correctCount = 10,
            comboMax = 10,
            xpEarned = 80,
            durationSec = 120
        )

        // 5 (session 10+) + 25 (perfect) = 30
        val result = useCase.execute(stats)
        assertEquals(30, result.coinsEarned)
    }

    @Test
    fun perfectScore20Cards_earns35Coins() = runTest {
        val (useCase, _) = createUseCase()
        val stats = SessionStats(
            gameMode = GameMode.RECOGNITION,
            cardsStudied = 20,
            correctCount = 20,
            comboMax = 20,
            xpEarned = 200,
            durationSec = 300
        )

        // 10 (session 20+) + 25 (perfect) = 35
        val result = useCase.execute(stats)
        assertEquals(35, result.coinsEarned)
    }

    @Test
    fun noJCoinRepo_earns0Coins() = runTest {
        val db = createTestDb()
        val useCase = CompleteSessionUseCase(
            UserRepositoryImpl(db),
            SessionRepositoryImpl(db),
            ScoringEngine(),
            null // No JCoin repo
        )
        val stats = SessionStats(
            gameMode = GameMode.RECOGNITION,
            cardsStudied = 20,
            correctCount = 20,
            comboMax = 20,
            xpEarned = 200,
            durationSec = 300
        )

        val result = useCase.execute(stats)
        assertEquals(0, result.coinsEarned)
    }

    @Test
    fun multipleSessionsAccumulate() = runTest {
        val db = createTestDb()
        val (useCase, jCoinRepo) = createUseCase(db)
        val stats = SessionStats(
            gameMode = GameMode.RECOGNITION,
            cardsStudied = 20,
            correctCount = 15,
            comboMax = 5,
            xpEarned = 100,
            durationSec = 300
        )

        useCase.execute(stats)
        useCase.execute(stats)
        useCase.execute(stats)

        val balance = jCoinRepo.getBalance(LOCAL_USER_ID)
        assertEquals(30L, balance.localBalance) // 10 * 3
        assertTrue(balance.needsSync)
    }
}
