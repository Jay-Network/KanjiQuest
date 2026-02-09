package com.jworks.kanjiquest.core.domain.usecase

import com.jworks.kanjiquest.core.domain.repository.LearningSyncRepository
import com.jworks.kanjiquest.core.domain.repository.SrsRepository
import com.jworks.kanjiquest.core.domain.repository.UserRepository

class DataRestorationUseCase(
    private val learningSyncRepository: LearningSyncRepository,
    private val userRepository: UserRepository,
    private val srsRepository: SrsRepository
) {
    suspend fun execute(userId: String) {
        try {
            val profile = userRepository.getProfile()
            val hasLocalData = profile.totalXp > 0 || srsRepository.getNonNewCardCount() > 0

            if (!hasLocalData) {
                // Empty local data: pull from cloud
                val cloudData = learningSyncRepository.pullCloudData(userId)
                if (cloudData != null) {
                    learningSyncRepository.applyCloudData(cloudData)
                }
            } else {
                // Has local data: push to cloud (server resolves conflicts)
                learningSyncRepository.syncPendingEvents()
            }
        } catch (_: Exception) {
            // Data restoration is best-effort
        }
    }
}
