import Foundation
import SharedCore

@MainActor
final class AuthViewModel: ObservableObject {
    @Published var email = ""
    @Published var password = ""
    @Published var isLoading = false
    @Published var errorMessage: String?

    func login(authRepository: AuthRepository) async -> Bool {
        guard validate() else { return false }

        isLoading = true
        errorMessage = nil

        do {
            try await authRepository.signIn(email: email, password: password)
            isLoading = false
            return true
        } catch {
            isLoading = false
            errorMessage = "Login failed. Check your credentials."
            return false
        }
    }

    func signUp(authRepository: AuthRepository) async -> Bool {
        guard validate() else { return false }

        isLoading = true
        errorMessage = nil

        do {
            try await authRepository.signUp(email: email, password: password)
            isLoading = false
            return true
        } catch {
            isLoading = false
            errorMessage = "Sign up failed. Try a different email."
            return false
        }
    }

    private func validate() -> Bool {
        guard !email.isEmpty else {
            errorMessage = "Email is required."
            return false
        }
        guard password.count >= 6 else {
            errorMessage = "Password must be at least 6 characters."
            return false
        }
        return true
    }
}
