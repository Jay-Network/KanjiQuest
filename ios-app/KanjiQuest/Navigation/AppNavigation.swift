import SwiftUI

enum Route: Hashable {
    case home
    case calligraphySession(kanjiLiteral: String, strokePaths: [String])
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
                case .calligraphySession(let kanji, let paths):
                    CalligraphySessionView(
                        kanjiLiteral: kanji,
                        strokePaths: paths
                    )
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
            isAuthenticated = await container.authRepository.hasActiveSession()
        }
    }

    private func navigate(to route: Route) {
        path.append(route)
    }
}
