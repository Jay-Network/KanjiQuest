import SwiftUI

struct LoginView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = AuthViewModel()
    let onLoginSuccess: () -> Void

    var body: some View {
        VStack(spacing: KanjiQuestTheme.spacingL) {
            Spacer()

            // Logo area
            VStack(spacing: KanjiQuestTheme.spacingS) {
                Text("漢")
                    .font(KanjiQuestTheme.kanjiDisplay)
                    .foregroundColor(KanjiQuestTheme.primary)

                Text("KanjiQuest")
                    .font(KanjiQuestTheme.titleLarge)

                Text("書道 Calligraphy Edition")
                    .font(KanjiQuestTheme.bodyLarge)
                    .foregroundColor(.secondary)
            }

            Spacer()

            // Login form
            VStack(spacing: KanjiQuestTheme.spacingM) {
                TextField("Email", text: $viewModel.email)
                    .textFieldStyle(.roundedBorder)
                    .textContentType(.emailAddress)
                    .autocapitalization(.none)
                    .keyboardType(.emailAddress)

                SecureField("Password", text: $viewModel.password)
                    .textFieldStyle(.roundedBorder)
                    .textContentType(.password)

                if let error = viewModel.errorMessage {
                    Text(error)
                        .font(KanjiQuestTheme.labelSmall)
                        .foregroundColor(KanjiQuestTheme.error)
                }

                Button {
                    Task {
                        let success = await viewModel.login(authRepository: container.authRepository)
                        if success { onLoginSuccess() }
                    }
                } label: {
                    if viewModel.isLoading {
                        SwiftUI.ProgressView()
                            .progressViewStyle(.circular)
                            .tint(.white)
                    } else {
                        Text("Sign In")
                    }
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(KanjiQuestTheme.primary)
                .foregroundColor(.white)
                .cornerRadius(KanjiQuestTheme.radiusM)
                .disabled(viewModel.isLoading)

                Button("Create Account") {
                    Task {
                        let success = await viewModel.signUp(authRepository: container.authRepository)
                        if success { onLoginSuccess() }
                    }
                }
                .font(KanjiQuestTheme.labelLarge)
                .foregroundColor(KanjiQuestTheme.primary)
            }
            .padding(.horizontal, KanjiQuestTheme.spacingXL)

            Spacer()
        }
        .background(KanjiQuestTheme.background)
    }
}
