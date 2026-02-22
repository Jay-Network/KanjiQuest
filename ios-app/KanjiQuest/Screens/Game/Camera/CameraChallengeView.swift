import SwiftUI
import SharedCore
import AVFoundation

/// Camera challenge screen. Mirrors Android's CameraChallengeScreen.kt.
/// Uses AVFoundation camera + ML Kit OCR to find kanji in the real world.
struct CameraChallengeView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = CameraChallengeViewModel()
    var targetKanjiId: Int32? = nil
    var onBack: () -> Void = {}
    var onJournal: (() -> Void)? = nil

    @State private var cameraPermissionGranted = false

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if !cameraPermissionGranted {
                permissionDenied
            } else {
                switch viewModel.state {
                case .loading:
                    loadingContent
                case .showTarget(let targetKanji, let challengeNumber, let totalChallenges, _, let sessionXp):
                    cameraWithTarget(targetKanji, challenge: challengeNumber, total: totalChallenges, xp: sessionXp)
                case .success(let targetKanji, let challengeNumber, let totalChallenges, _, _, let xpGained):
                    successOverlay(targetKanji, challenge: challengeNumber, total: totalChallenges, xpGained: xpGained)
                case .sessionComplete(let totalChallenges, let successCount, let accuracy, let totalXp):
                    sessionComplete(total: totalChallenges, success: successCount, accuracy: accuracy, xp: totalXp)
                case .error(let message):
                    errorContent(message)
                }
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left"); Text("Back")
                }.foregroundColor(.white)
            }
            ToolbarItem(placement: .principal) {
                Text("Camera Challenge").font(.headline).foregroundColor(.white)
            }
            if let onJournal = onJournal {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: onJournal) {
                        Text("\u{1F4D3}").font(.system(size: 20))
                    }.foregroundColor(.white)
                }
            }
        }
        .toolbarBackground(KanjiQuestTheme.primary, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .task {
            viewModel.load(container: container)
            await checkCameraPermission()
        }
    }

    // MARK: - Camera Permission

    private func checkCameraPermission() async {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            cameraPermissionGranted = true
            viewModel.startSession(targetKanjiId: targetKanjiId)
        case .notDetermined:
            let granted = await AVCaptureDevice.requestAccess(for: .video)
            cameraPermissionGranted = granted
            if granted { viewModel.startSession(targetKanjiId: targetKanjiId) }
        default:
            cameraPermissionGranted = false
        }
    }

    private var permissionDenied: some View {
        VStack(spacing: 16) {
            Text("Camera permission is required for this mode")
                .font(KanjiQuestTheme.bodyLarge)
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
            Button("Open Settings") {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
            .fontWeight(.bold).foregroundColor(.white)
            .padding(.horizontal, 16).padding(.vertical, 8)
            .background(KanjiQuestTheme.primary).cornerRadius(8)
        }
        .padding(24)
    }

    // MARK: - Loading

    private var loadingContent: some View {
        VStack(spacing: 16) {
            ProgressView().tint(.white)
            Text("Preparing camera challenges...")
                .font(KanjiQuestTheme.titleMedium)
                .foregroundColor(.white)
        }
    }

    // MARK: - Camera with Target Overlay

    private func cameraWithTarget(_ kanji: Kanji, challenge: Int, total: Int, xp: Int) -> some View {
        ZStack {
            // Camera preview placeholder
            // TODO: Replace with actual AVCaptureSession + Vision OCR
            CameraPreviewPlaceholder(onTextRecognized: { text in
                viewModel.onTextRecognized(text)
            })

            // Target overlay
            VStack {
                // Progress bar
                HStack {
                    Text("\(challenge) / \(total)")
                        .font(KanjiQuestTheme.titleMedium)
                        .foregroundColor(.white)
                        .padding(.horizontal, 12).padding(.vertical, 6)
                        .background(Color.black.opacity(0.6))
                        .cornerRadius(8)

                    Spacer()

                    Text("\(xp) XP")
                        .font(KanjiQuestTheme.titleMedium)
                        .fontWeight(.bold)
                        .foregroundColor(KanjiQuestTheme.xpGold)
                        .padding(.horizontal, 12).padding(.vertical, 6)
                        .background(Color.black.opacity(0.6))
                        .cornerRadius(8)
                }

                Spacer().frame(height: 24)

                // Target kanji card
                VStack(spacing: 16) {
                    Text("Find and scan this kanji:")
                        .font(KanjiQuestTheme.titleMedium)
                        .foregroundColor(KanjiQuestTheme.onSurface)

                    KanjiText(text: kanji.literal)
                        .font(.system(size: 96, weight: .bold))
                        .foregroundColor(KanjiQuestTheme.primary)

                    Text(kanji.primaryMeaning)
                        .font(KanjiQuestTheme.bodyLarge)
                        .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                }
                .padding(24)
                .background(KanjiQuestTheme.surface)
                .cornerRadius(16)
                .shadow(radius: 8)

                Spacer()

                // Scanning indicator
                if viewModel.isScanning {
                    Text("Scanning...")
                        .font(KanjiQuestTheme.titleMedium)
                        .foregroundColor(.white)
                        .padding(.horizontal, 16).padding(.vertical, 8)
                        .background(Color.black.opacity(0.6))
                        .cornerRadius(8)
                        .padding(.bottom, 32)
                }
            }
            .padding(16)
        }
    }

    // MARK: - Success Overlay

    private func successOverlay(_ kanji: Kanji, challenge: Int, total: Int, xpGained: Int) -> some View {
        ZStack {
            Color(hex: 0x4CAF50).opacity(0.92).ignoresSafeArea()

            VStack(spacing: 16) {
                Text("\u{2713}")
                    .font(.system(size: 120))
                    .foregroundColor(.white)

                Text("Success!")
                    .font(KanjiQuestTheme.headlineLarge)
                    .fontWeight(.bold)
                    .foregroundColor(.white)

                Text("+\(xpGained) XP")
                    .font(KanjiQuestTheme.titleLarge)
                    .fontWeight(.bold)
                    .foregroundColor(KanjiQuestTheme.xpGold)

                Spacer().frame(height: 16)

                KanjiText(text: kanji.literal)
                    .font(.system(size: 80))
                    .foregroundColor(.white)

                Text(kanji.primaryMeaning)
                    .font(KanjiQuestTheme.titleMedium)
                    .foregroundColor(.white.opacity(0.9))

                Spacer().frame(height: 32)

                Button {
                    viewModel.nextChallenge()
                } label: {
                    Text(challenge < total ? "Next Challenge" : "Finish")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(Color(hex: 0x4CAF50))
                        .frame(width: 220, height: 56)
                        .background(.white)
                        .cornerRadius(12)
                }
            }
            .padding(24)
        }
    }

    // MARK: - Session Complete

    private func sessionComplete(total: Int, success: Int, accuracy: Int, xp: Int) -> some View {
        VStack(spacing: 24) {
            Spacer()

            Text("Challenge Complete!")
                .font(KanjiQuestTheme.headlineMedium)
                .fontWeight(.bold)

            VStack(spacing: 12) {
                statRow("Total Challenges", "\(total)")
                statRow("Successfully Scanned", "\(success)")
                statRow("Accuracy", "\(accuracy)%")
                statRow("XP Earned", "+\(xp)")
            }
            .padding(20)
            .background(KanjiQuestTheme.surface)
            .cornerRadius(KanjiQuestTheme.radiusM)

            Spacer()

            Button {
                viewModel.reset()
                onBack()
            } label: {
                Text("Done")
                    .font(.system(size: 18)).fontWeight(.bold)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity).frame(height: 56)
                    .background(KanjiQuestTheme.primary).cornerRadius(12)
            }

            if let onJournal = onJournal {
                Button(action: onJournal) {
                    Text("View Field Journal")
                        .font(.system(size: 18))
                        .foregroundColor(KanjiQuestTheme.primary)
                        .frame(maxWidth: .infinity).frame(height: 56)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(KanjiQuestTheme.primary, lineWidth: 1))
                }
            }
        }
        .padding(24)
    }

    // MARK: - Error

    private func errorContent(_ message: String) -> some View {
        VStack(spacing: 16) {
            Text(message)
                .font(KanjiQuestTheme.bodyLarge)
                .multilineTextAlignment(.center)
            Button("Go Back", action: onBack)
                .foregroundColor(KanjiQuestTheme.primary)
        }
        .padding(24)
    }

    // MARK: - Helpers

    private func statRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
                .font(KanjiQuestTheme.bodyLarge)
                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
            Spacer()
            Text(value)
                .font(KanjiQuestTheme.bodyLarge)
                .fontWeight(.bold)
        }
    }
}

// MARK: - Camera Preview Placeholder
// TODO: Implement actual AVCaptureSession with Vision framework OCR

struct CameraPreviewPlaceholder: UIViewRepresentable {
    var onTextRecognized: (String) -> Void

    func makeUIView(context: Context) -> UIView {
        let view = UIView()
        view.backgroundColor = .black

        let label = UILabel()
        label.text = "Camera Preview\n(AVFoundation + Vision OCR)"
        label.textColor = .white
        label.textAlignment = .center
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(label)
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: view.centerYAnchor)
        ])

        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {}
}
