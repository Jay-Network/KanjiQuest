import Foundation
import UIKit
import SharedCore

@MainActor
final class FeedbackViewModel: ObservableObject {
    @Published var feedbackList: [FeedbackWithHistory] = []
    @Published var isDialogOpen = false
    @Published var isSubmitting = false
    @Published var selectedCategory: FeedbackCategory = .other
    @Published var feedbackText = ""
    @Published var error: String? = nil
    @Published var successMessage: String? = nil
    @Published var isLoadingHistory = false

    private static let appId = "kanjiquests"
    private static let pollInterval: UInt64 = 15_000_000_000 // 15 seconds

    private var feedbackRepository: FeedbackRepository?
    private var cachedEmail: String?
    private var lastFeedbackId: Int64? = nil
    private var pollTask: Task<Void, Never>?

    func configure(container: AppContainer) {
        feedbackRepository = container.feedbackRepository
        Task {
            cachedEmail = try? await container.userSessionProvider.getUserEmail()
        }
    }

    func openDialog() {
        feedbackText = ""
        selectedCategory = .other
        error = nil
        successMessage = nil
        isDialogOpen = true

        Task {
            loadFeedbackHistory()
        }
        startPolling()
    }

    func closeDialog() {
        isDialogOpen = false
        stopPolling()
    }

    func selectCategory(_ category: FeedbackCategory) {
        selectedCategory = category
    }

    func updateFeedbackText(_ text: String) {
        feedbackText = text
    }

    func submitFeedback() {
        guard let email = cachedEmail else {
            error = "User not authenticated"
            return
        }

        let text = feedbackText.trimmingCharacters(in: .whitespacesAndNewlines)
        if text.count < 10 {
            error = "Please provide at least 10 characters"
            return
        }
        if text.count > 1000 {
            error = "Maximum 1000 characters allowed"
            return
        }

        isSubmitting = true
        error = nil

        Task {
            let deviceInfo: [String: String] = [
                "os": "iOS",
                "osVersion": UIDevice.current.systemVersion,
                "device": UIDevice.current.model
            ]

            let result = try? await feedbackRepository?.submitFeedback(
                email: email,
                appId: Self.appId,
                category: selectedCategory,
                feedbackText: text,
                deviceInfo: deviceInfo
            )

            if result is SubmitFeedbackResult.Success {
                isSubmitting = false
                successMessage = "Thank you for your feedback! We'll keep you updated on progress."
                feedbackText = ""
                selectedCategory = .other
                loadFeedbackHistory()
            } else if let rateLimited = result as? SubmitFeedbackResult.RateLimited {
                isSubmitting = false
                error = rateLimited.message
            } else if let feedbackError = result as? SubmitFeedbackResult.Error {
                isSubmitting = false
                error = feedbackError.message
            } else {
                isSubmitting = false
                error = "Failed to submit feedback"
            }
        }
    }

    func clearMessages() {
        error = nil
        successMessage = nil
    }

    private func loadFeedbackHistory() {
        guard let email = cachedEmail else { return }
        isLoadingHistory = true

        Task {
            do {
                let feedback = try await feedbackRepository?.getFeedbackUpdates(
                    email: email,
                    appId: Self.appId,
                    sinceId: nil
                ) ?? []

                lastFeedbackId = feedback.map { $0.feedback.id }.max()
                feedbackList = feedback.sorted { $0.feedback.createdAt > $1.feedback.createdAt }
                isLoadingHistory = false
            } catch {
                isLoadingHistory = false
                self.error = "Failed to load feedback history: \(error.localizedDescription)"
            }
        }
    }

    private func startPolling() {
        stopPolling()
        pollTask = Task {
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: Self.pollInterval)
                guard let email = cachedEmail else { continue }
                do {
                    let sinceIdParam: KotlinLong? = lastFeedbackId != nil ? KotlinLong(value: lastFeedbackId!) : nil
                    let newFeedback = try await feedbackRepository?.getFeedbackUpdates(
                        email: email,
                        appId: Self.appId,
                        sinceId: sinceIdParam
                    ) ?? []

                    if !newFeedback.isEmpty {
                        let merged = (feedbackList + newFeedback)
                            .reduce(into: [Int64: FeedbackWithHistory]()) { dict, item in
                                dict[item.feedback.id] = item
                            }
                            .values
                            .sorted { $0.feedback.createdAt > $1.feedback.createdAt }

                        lastFeedbackId = merged.map { $0.feedback.id }.max()
                        feedbackList = Array(merged)
                    }
                } catch {
                    // Silently ignore poll failures
                }
            }
        }
    }

    private func stopPolling() {
        pollTask?.cancel()
        pollTask = nil
    }

    deinit {
        pollTask?.cancel()
    }
}
