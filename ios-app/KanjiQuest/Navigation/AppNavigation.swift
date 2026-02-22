import SwiftUI

enum Route: Hashable {
    case home
    case recognition(targetKanjiId: Int32? = nil)
    #if IPAD_TARGET
    case calligraphySession(kanjiLiteral: String, strokePaths: [String])
    #endif
    case login
    case progress
    case settings
    case subscription
}

struct AppNavigation: View {
    @EnvironmentObject var container: AppContainer
    @State private var path = NavigationPath()
    @State private var isAuthenticated = false

    var body: some View {
        NavigationStack(path: $path) {
            Group {
                if isAuthenticated {
                    HomeView(navigateTo: navigate)
                } else {
                    LoginView(onLoginSuccess: {
                        isAuthenticated = true
                    })
                }
            }
            .navigationDestination(for: Route.self) { route in
                switch route {
                case .home:
                    HomeView(navigateTo: navigate)
                case .recognition(let targetKanjiId):
                    RecognitionView(targetKanjiId: targetKanjiId)
                #if IPAD_TARGET
                case .calligraphySession(let kanji, let paths):
                    CalligraphySessionView(
                        kanjiLiteral: kanji,
                        strokePaths: paths
                    )
                #endif
                case .login:
                    LoginView(onLoginSuccess: {
                        isAuthenticated = true
                        path = NavigationPath()
                    })
                case .progress:
                    ProgressView()
                case .settings:
                    SettingsView()
                case .subscription:
                    SubscriptionView()
                }
            }
        }
        .task {
            // Check for existing session
            isAuthenticated = (try? await container.authRepository.getCurrentUserId()) != nil
        }
    }

    private func navigate(to route: Route) {
        path.append(route)
    }
}
