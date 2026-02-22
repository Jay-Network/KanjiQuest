import Foundation
import SharedCore

/// Subscription view model. Mirrors Android's SubscriptionViewModel.kt.
@MainActor
class SubscriptionViewModel: ObservableObject {
    @Published var isLoggedIn = false
    @Published var isPremium = false
    @Published var isLoading = true

    func load(container: AppContainer) {
        Task {
            let userId = try? await container.authRepository.getCurrentUserId()
            let subscription = try? await container.authRepository.getSubscription()
            isLoggedIn = userId != nil
            isPremium = subscription?.plan == .premium
            isLoading = false
        }
    }

    func refresh(container: AppContainer) {
        isLoading = true
        load(container: container)
    }
}
