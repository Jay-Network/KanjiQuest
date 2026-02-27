import Foundation
import SharedCore
import BackgroundTasks
import UIKit

/// Wraps KMP LearningSyncRepository for iOS lifecycle sync.
/// Triggers: scenePhase → .active (pull), session complete (push via shared-core), BGAppRefreshTask.
final class SyncService: ObservableObject {

    static let backgroundTaskIdentifier = "com.jworks.kanjiquest.sync"

    private let learningSyncRepository: LearningSyncRepositoryImpl
    private let userSessionProvider: UserSessionProviderImpl

    init(
        learningSyncRepository: LearningSyncRepositoryImpl,
        userSessionProvider: UserSessionProviderImpl
    ) {
        self.learningSyncRepository = learningSyncRepository
        self.userSessionProvider = userSessionProvider
    }

    // MARK: - App Foreground Sync

    func syncOnAppOpen() {
        Task {
            guard let userId = getUserId() else { return }
            do {
                let result = try await learningSyncRepository.syncAll(
                    userId: userId,
                    trigger: .appOpen
                )
                if let success = result as? SyncResultSuccess {
                    NSLog("KanjiQuest [Sync]: App open sync complete — pushed=\(success.pushed), pulled=\(success.pulled)")
                }
            } catch {
                NSLog("KanjiQuest [Sync]: App open sync failed: \(error.localizedDescription)")
            }
        }
    }

    // MARK: - Background App Refresh

    func registerBackgroundTask() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: Self.backgroundTaskIdentifier,
            using: nil
        ) { [weak self] task in
            self?.handleBackgroundSync(task: task as! BGAppRefreshTask)
        }
    }

    func scheduleBackgroundSync() {
        let request = BGAppRefreshTaskRequest(identifier: Self.backgroundTaskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 30 * 60) // 30 minutes
        do {
            try BGTaskScheduler.shared.submit(request)
            NSLog("KanjiQuest [Sync]: Background sync scheduled")
        } catch {
            NSLog("KanjiQuest [Sync]: Failed to schedule background sync: \(error.localizedDescription)")
        }
    }

    private func handleBackgroundSync(task: BGAppRefreshTask) {
        // Schedule next refresh
        scheduleBackgroundSync()

        let syncTask = Task {
            guard let userId = getUserId() else {
                task.setTaskCompleted(success: true)
                return
            }
            do {
                let result = try await learningSyncRepository.syncAll(
                    userId: userId,
                    trigger: .backgroundPeriodic
                )
                let success = result is SyncResultSuccess
                task.setTaskCompleted(success: success)
                NSLog("KanjiQuest [Sync]: Background sync \(success ? "succeeded" : "failed")")
            } catch {
                task.setTaskCompleted(success: false)
                NSLog("KanjiQuest [Sync]: Background sync error: \(error.localizedDescription)")
            }
        }

        task.expirationHandler = {
            syncTask.cancel()
        }
    }

    // MARK: - Device Registration

    func registerDeviceIfNeeded() {
        Task {
            guard let userId = getUserId() else { return }
            let deviceInfo = DeviceInfoProvider.deviceInfo()
            do {
                let deviceId = try await learningSyncRepository.registerDevice(
                    userId: userId,
                    deviceInfo: deviceInfo
                )
                if let deviceId = deviceId as? String {
                    NSLog("KanjiQuest [Sync]: Device registered: \(deviceId)")
                }
            } catch {
                NSLog("KanjiQuest [Sync]: Device registration failed: \(error.localizedDescription)")
            }
        }
    }

    // MARK: - Helpers

    private func getUserId() -> String? {
        let userId = userSessionProvider.getUserId()
        if userId == nil || userId == LOCAL_USER_ID {
            return nil
        }
        return userId
    }
}
