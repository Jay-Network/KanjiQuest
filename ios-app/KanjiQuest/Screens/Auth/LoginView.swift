import SwiftUI

/// Login screen. Mirrors Android's LoginScreen.kt.
/// Hero image, sign-in form card, TutoringJay pitch, guest mode.
struct LoginView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = AuthViewModel()
    let onLoginSuccess: () -> Void
    var onContinueWithoutAccount: (() -> Void)? = nil

    @State private var email = ""
    @State private var password = ""

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Spacer(minLength: 16)

                // Hero illustration
                AssetImage(filename: "login-hero.png", contentDescription: "KanjiQuest login")
                    .frame(maxWidth: .infinity)
                    .frame(height: 180)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .padding(.horizontal, 24)

                Spacer(minLength: 16)

                // App branding
                Text("KanjiQuest")
                    .font(KanjiQuestTheme.headlineMedium)
                    .fontWeight(.bold)
                    .foregroundColor(KanjiQuestTheme.primary)

                Spacer(minLength: 4)

                Text("Master kanji through play")
                    .font(KanjiQuestTheme.bodyLarge)
                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)

                Spacer(minLength: 24)

                // Sign in form card
                VStack(spacing: 0) {
                    Text(viewModel.isSignUpMode ? "Create Account" : "Welcome Back")
                        .font(KanjiQuestTheme.titleLarge)
                        .fontWeight(.bold)

                    Spacer(minLength: 4)

                    Text(viewModel.isSignUpMode
                         ? "Sign up to sync progress and unlock premium"
                         : "Sign in with your TutoringJay account")
                        .font(KanjiQuestTheme.bodySmall)
                        .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                        .multilineTextAlignment(.center)

                    Spacer(minLength: 20)

                    TextField("Email", text: $email)
                        .textFieldStyle(.roundedBorder)
                        .textContentType(.emailAddress)
                        .autocapitalization(.none)
                        .keyboardType(.emailAddress)
                        .disabled(viewModel.isLoading)

                    Spacer(minLength: 12)

                    SecureField("Password", text: $password)
                        .textFieldStyle(.roundedBorder)
                        .textContentType(.password)
                        .disabled(viewModel.isLoading)

                    if let error = viewModel.errorMessage {
                        Spacer(minLength: 8)
                        Text(error)
                            .font(KanjiQuestTheme.bodySmall)
                            .foregroundColor(KanjiQuestTheme.error)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity)
                    }

                    Spacer(minLength: 20)

                    Button {
                        if viewModel.isSignUpMode {
                            viewModel.email = email
                            viewModel.password = password
                            viewModel.signUp(authRepository: container.authRepository)
                        } else {
                            viewModel.email = email
                            viewModel.password = password
                            viewModel.signIn(authRepository: container.authRepository)
                        }
                    } label: {
                        Group {
                            if viewModel.isLoading {
                                ProgressView().tint(.white)
                            } else {
                                Text(viewModel.isSignUpMode ? "Create Account" : "Sign In")
                                    .fontWeight(.bold)
                                    .font(.system(size: 16))
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(KanjiQuestTheme.primary)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    }
                    .disabled(email.isEmpty || password.isEmpty || viewModel.isLoading)

                    Spacer(minLength: 8)

                    Button {
                        viewModel.toggleSignUpMode()
                    } label: {
                        Text(viewModel.isSignUpMode
                             ? "Already have an account? Sign In"
                             : "New here? Create an account")
                            .font(.system(size: 13))
                            .foregroundColor(KanjiQuestTheme.primary)
                    }
                }
                .padding(20)
                .background(KanjiQuestTheme.surface)
                .cornerRadius(16)
                .padding(.horizontal, 24)

                Spacer(minLength: 16)

                // TutoringJay membership pitch
                VStack(alignment: .leading, spacing: 0) {
                    Text("TutoringJay Members get more")
                        .font(KanjiQuestTheme.titleSmall)
                        .fontWeight(.bold)
                        .foregroundColor(Color(hex: 0x1565C0))

                    Spacer(minLength: 8)

                    memberPerk("Premium app access included with tutoring")
                    memberPerk("Earn J Coins for real rewards")
                    memberPerk("Personalized learning with a STEM tutor")
                    memberPerk("Track progress across all subjects")

                    Spacer(minLength: 8)

                    Text("Learn more at tutoringjay.com")
                        .font(KanjiQuestTheme.labelSmall)
                        .foregroundColor(Color(hex: 0x1565C0).opacity(0.7))
                }
                .padding(16)
                .background(Color(hex: 0x1E88E5).opacity(0.08))
                .cornerRadius(12)
                .padding(.horizontal, 24)

                Spacer(minLength: 20)

                // Divider
                HStack {
                    Rectangle().fill(Color.gray.opacity(0.3)).frame(height: 1)
                    Text("or")
                        .font(KanjiQuestTheme.bodySmall)
                        .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                        .padding(.horizontal, 8)
                    Rectangle().fill(Color.gray.opacity(0.3)).frame(height: 1)
                }
                .padding(.horizontal, 24)

                Spacer(minLength: 16)

                // Guest mode
                if let guestAction = onContinueWithoutAccount {
                    Button(action: guestAction) {
                        Text("Try as Guest")
                            .font(.system(size: 14))
                            .fontWeight(.medium)
                            .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                            )
                    }
                    .padding(.horizontal, 24)

                    Spacer(minLength: 6)

                    Text("Recognition mode free. Preview other modes daily.")
                        .font(KanjiQuestTheme.labelSmall)
                        .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                }

                Spacer(minLength: 32)
            }
        }
        .background(KanjiQuestTheme.background)
        .onChange(of: viewModel.isLoggedIn) { loggedIn in
            if loggedIn { onLoginSuccess() }
        }
    }

    private func memberPerk(_ text: String) -> some View {
        HStack(alignment: .top, spacing: 4) {
            Text("\u{2713} ")
                .foregroundColor(Color(hex: 0x1565C0))
                .fontWeight(.bold)
                .font(.system(size: 13))
            Text(text)
                .font(KanjiQuestTheme.bodySmall)
                .foregroundColor(Color(hex: 0x1565C0).opacity(0.85))
        }
        .padding(.vertical, 2)
    }
}
