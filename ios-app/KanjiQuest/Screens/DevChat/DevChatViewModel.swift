import Foundation
import SharedCore

@MainActor
final class DevChatViewModel: ObservableObject {
    @Published var messages: [DevChatMessage] = []
    @Published var isLoading = true
    @Published var isSending = false
    @Published var error: String? = nil
    @Published var selectedCategory: MessageCategory? = nil

    private var devChatRepository: DevChatRepository?
    private var cachedEmail: String?
    private var pollTask: Task<Void, Never>?

    func load(container: AppContainer) {
        devChatRepository = container.devChatRepository

        Task {
            cachedEmail = try? await container.userSessionProvider.getUserEmail()
            await loadMessages()
            startPolling()
        }
    }

    private func loadMessages() async {
        guard let email = cachedEmail else { return }
        isLoading = true
        error = nil
        do {
            let msgs = try await devChatRepository?.getMessageHistory(email: email) ?? []
            messages = msgs.reversed()
            isLoading = false
        } catch {
            self.error = error.localizedDescription
            isLoading = false
        }
    }

    func sendMessage(_ text: String) {
        guard let email = cachedEmail, !text.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        let category = selectedCategory
        let trimmed = text.trimmingCharacters(in: .whitespaces)

        // Optimistic add
        let tempId = Int64(-Date().timeIntervalSince1970 * 1000)
        let optimistic = DevChatMessage(
            id: tempId,
            messageText: trimmed,
            direction: .toAgent,
            category: category,
            sentAt: "",
            readAt: nil
        )

        messages.append(optimistic)
        isSending = true
        error = nil
        selectedCategory = nil

        Task {
            guard let result = try? await devChatRepository?.sendMessage(email: email, text: trimmed, category: category) else {
                messages.removeAll { $0.id == tempId }
                isSending = false
                error = "Failed to send message"
                return
            }

            switch result {
            case .success(let messageId, let sentAt):
                if let idx = messages.firstIndex(where: { $0.id == tempId }) {
                    messages[idx] = DevChatMessage(
                        id: messageId,
                        messageText: trimmed,
                        direction: .toAgent,
                        category: category,
                        sentAt: sentAt,
                        readAt: nil
                    )
                }
                isSending = false
            case .error(let message):
                messages.removeAll { $0.id == tempId }
                isSending = false
                error = message
            case .notDeveloper:
                messages.removeAll { $0.id == tempId }
                isSending = false
                error = "Not registered as a developer"
            }
        }
    }

    func selectCategory(_ category: MessageCategory?) {
        selectedCategory = category
    }

    func clearError() {
        error = nil
    }

    private func startPolling() {
        pollTask = Task {
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 10_000_000_000) // 10 seconds
                guard let email = cachedEmail else { continue }
                if let msgs = try? await devChatRepository?.getMessageHistory(email: email) {
                    messages = msgs.reversed()
                }
            }
        }
    }

    deinit {
        pollTask?.cancel()
    }
}
