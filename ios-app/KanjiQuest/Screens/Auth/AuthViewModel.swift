import Foundation
import SharedCore

/// Auth view model. Mirrors Android's AuthViewModel.kt.
@MainActor
final class AuthViewModel: ObservableObject {
    @Published var email = ""
    @Published var password = ""
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var isLoggedIn = false
    @Published var isSignUpMode = false

    func signIn(authRepository: AuthRepository) {
        guard validate() else { return }
        isLoading = true
        errorMessage = nil

        Task {
            do {
                try await authRepository.signIn(email: email, password: password)
                isLoading = false
                isLoggedIn = true
            } catch {
                isLoading = false
                errorMessage = sanitizeError(error, fallback: "Sign in failed")
            }
        }
    }

    func signUp(authRepository: AuthRepository) {
        guard validate() else { return }
        isLoading = true
        errorMessage = nil

        Task {
            do {
                try await authRepository.signUp(email: email, password: password)
                isLoading = false
                isLoggedIn = true
            } catch {
                isLoading = false
                errorMessage = sanitizeError(error, fallback: "Sign up failed")
            }
        }
    }

    func toggleSignUpMode() {
        isSignUpMode.toggle()
        errorMessage = nil
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

    private func sanitizeError(_ error: Error, fallback: String) -> String {
        let msg = error.localizedDescription
        if msg.contains("Invalid login credentials") { return "Invalid email or password" }
        if msg.contains("Email not confirmed") { return "Please confirm your email first" }
        if msg.contains("User already registered") { return "An account with this email already exists" }
        if msg.contains("Auth not configured") { return "Authentication is not available" }
        if msg.lowercased().contains("rate limit") { return "Too many attempts. Please try again later." }
        return fallback
    }
}
