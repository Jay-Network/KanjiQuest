import SwiftUI
import SharedCore

/// Full navigation host matching Android KanjiQuestNavHost.
/// Handles all 23+ routes, feedback FAB, and post-login routing.
struct AppNavigation: View {
    @EnvironmentObject var container: AppContainer
    @State private var path = NavigationPath()
    @State private var showSplash = true
    @State private var isAuthenticated = false

    // Feedback
    @StateObject private var feedbackViewModel = FeedbackViewModel()
    @State private var showFeedbackDialog = false

    var body: some View {
        ZStack {
            NavigationStack(path: $path) {
                Group {
                    if showSplash {
                        SplashView(onSplashComplete: {
                            showSplash = false
                        })
                    } else if isAuthenticated {
                        MainTabView(navigateTo: navigate)
                    } else {
                        LoginView(
                            onLoginSuccess: { navigateAfterLogin() },
                            onContinueWithoutAccount: { navigateAfterLogin() }
                        )
                    }
                }
                .navigationDestination(for: NavRoute.self) { route in
                    destinationView(for: route)
                }
            }

            // Feedback FAB — shown on all screens except splash/login
            if !showSplash && isAuthenticated {
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        FeedbackFAB(action: { showFeedbackDialog = true })
                            .padding(.trailing, 16)
                            .padding(.bottom, 16)
                    }
                }
            }
        }
        .sheet(isPresented: $showFeedbackDialog) {
            FeedbackDialogView(
                viewModel: feedbackViewModel,
                onDismiss: { showFeedbackDialog = false }
            )
        }
        .task {
            feedbackViewModel.configure(container: container)
            isAuthenticated = (try? await container.authRepository.getCurrentUserId()) != nil
        }
        .environmentObject(container)
    }

    // MARK: - Post-login routing (placement test check)
    private func navigateAfterLogin() {
        let assessmentDone = UserDefaults.standard.bool(forKey: "assessment_completed")
        isAuthenticated = true
        if !assessmentDone {
            path.append(NavRoute.placementTest)
        }
    }

    // MARK: - Navigation helper
    func navigate(to route: NavRoute) {
        path.append(route)
    }

    func navigateBack() {
        if !path.isEmpty {
            path.removeLast()
        }
    }

    func navigateToRoot() {
        path = NavigationPath()
    }

    // MARK: - Route → View mapping
    @ViewBuilder
    private func destinationView(for route: NavRoute) -> some View {
        switch route {
        case .splash:
            SplashView(onSplashComplete: { showSplash = false })

        case .login:
            LoginView(
                onLoginSuccess: {
                    isAuthenticated = true
                    path = NavigationPath()
                },
                onContinueWithoutAccount: {
                    isAuthenticated = true
                    path = NavigationPath()
                }
            )

        case .home:
            MainTabView(navigateTo: navigate)

        case .placementTest:
            PlacementTestView(
                onBack: { navigateBack() },
                onComplete: {
                    UserDefaults.standard.set(true, forKey: "assessment_completed")
                    path = NavigationPath()
                }
            )

        // MARK: - Kanji Detail
        case .kanjiDetail(let kanjiId):
            KanjiDetailView(
                kanjiId: kanjiId,
                onBack: { navigateBack() },
                onPracticeWriting: { id in navigate(to: .writingTargeted(kanjiId: id)) },
                onPracticeCamera: { id in navigate(to: .cameraTargeted(kanjiId: id)) }
            )

        // MARK: - Game Modes
        case .recognition:
            RecognitionView(onBack: { navigateBack() })

        case .recognitionTargeted(let kanjiId):
            RecognitionView(targetKanjiId: kanjiId, onBack: { navigateBack() })

        case .writing:
            WritingView(onBack: { navigateBack() })

        case .writingTargeted(let kanjiId):
            WritingView(targetKanjiId: kanjiId, onBack: { navigateBack() })

        case .vocabulary:
            VocabularyView(onBack: { navigateBack() })

        case .vocabularyTargeted(let kanjiId):
            VocabularyView(targetKanjiId: kanjiId, onBack: { navigateBack() })

        case .camera:
            CameraChallengeView(
                onBack: { navigateBack() },
                onJournal: { navigate(to: .fieldJournal) }
            )

        case .cameraTargeted(let kanjiId):
            CameraChallengeView(
                targetKanjiId: kanjiId,
                onBack: { navigateBack() },
                onJournal: { navigate(to: .fieldJournal) }
            )

        // MARK: - Kana Modes
        case .kanaRecognition(let kanaType):
            KanaRecognitionView(kanaType: kanaType == "hiragana" ? .hiragana : .katakana, onBack: { navigateBack() })

        case .kanaWriting(let kanaType):
            // iPad: uses calligraphy canvas. iPhone: not available (excluded at build time)
            KanaRecognitionView(kanaType: kanaType == "hiragana" ? .hiragana : .katakana, onBack: { navigateBack() })

        // MARK: - Radical Modes
        case .radicalRecognition:
            RadicalRecognitionView(onBack: { navigateBack() })

        case .radicalBuilder:
            RadicalBuilderView(onBack: { navigateBack() })

        case .radicalDetail(let radicalId):
            RadicalDetailView(
                radicalId: radicalId,
                onBack: { navigateBack() },
                onKanjiClick: { kanjiId in navigate(to: .kanjiDetail(kanjiId: kanjiId)) }
            )

        // MARK: - Collection & Flashcards
        case .collection:
            CollectionView(
                onBack: { navigateBack() },
                onKanjiClick: { kanjiId in navigate(to: .kanjiDetail(kanjiId: kanjiId)) }
            )

        case .flashcards:
            FlashcardView(
                onBack: { navigateBack() },
                onStudy: { deckId in navigate(to: .flashcardStudy(deckId: deckId)) },
                onKanjiClick: { kanjiId in navigate(to: .kanjiDetail(kanjiId: kanjiId)) }
            )

        case .flashcardStudy(let deckId):
            FlashcardStudyView(deckId: deckId, onBack: { navigateBack() })

        // MARK: - Progress & Achievements
        case .progress:
            KQProgressView(onBack: { navigateBack() })

        case .achievements:
            AchievementsView(onBack: { navigateBack() })

        // MARK: - Shop & Subscription
        case .shop:
            ShopView(onBack: { navigateBack() })

        case .subscription:
            SubscriptionView(onBack: { navigateBack() })

        // MARK: - Settings
        case .settings:
            SettingsView(
                onBack: { navigateBack() },
                onDevChat: { navigate(to: .devChat) },
                onRetakeAssessment: { navigate(to: .placementTest) }
            )

        // MARK: - Word Detail
        case .wordDetail(let wordId):
            WordDetailView(
                wordId: wordId,
                onBack: { navigateBack() },
                onKanjiClick: { kanjiId in navigate(to: .kanjiDetail(kanjiId: kanjiId)) }
            )

        // MARK: - Test Mode
        case .testMode:
            TestModeView(onBack: { navigateBack() })

        // MARK: - Dev & Feedback
        case .devChat:
            DevChatView(onBack: { navigateBack() })

        case .fieldJournal:
            FieldJournalView(onBack: { navigateBack() })

        // MARK: - iPad Calligraphy
        #if IPAD_TARGET
        case .calligraphySession(let kanji, let paths):
            CalligraphySessionView(
                kanjiLiteral: kanji,
                strokePaths: paths
            )
        #endif
        }
    }
}
