import SwiftUI

/// Animated splash screen. Mirrors Android's SplashScreen.kt.
/// Logo fade-in + scale, shimmer sweep, title/subtitle, then fade out.
struct SplashView: View {
    var onSplashComplete: () -> Void

    @State private var logoAlpha: Double = 0
    @State private var logoScale: CGFloat = 0.85
    @State private var shimmerOffset: CGFloat = -1
    @State private var titleAlpha: Double = 0
    @State private var subtitleAlpha: Double = 0
    @State private var fadeOut: Double = 1

    private let tealAccent = Color(hex: 0x0D9488)

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            VStack(spacing: 0) {
                // Logo with shimmer
                ZStack {
                    Image("JWorksLogo")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 240, height: 240)
                        .accessibilityLabel("JWorks Logo")
                    .opacity(logoAlpha)
                    .scaleEffect(logoScale)

                    // Shimmer overlay
                    if shimmerOffset > -1 {
                        GeometryReader { geo in
                            let shimmerWidth = geo.size.width * 0.5
                            let x = geo.size.width * shimmerOffset
                            LinearGradient(
                                colors: [.clear, .white.opacity(0.3), .clear],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                            .frame(width: shimmerWidth * 2)
                            .offset(x: x - shimmerWidth)
                        }
                        .frame(width: 240, height: 240)
                        .clipped()
                        .allowsHitTesting(false)
                    }
                }

                Spacer().frame(height: 24)

                Text("KanjiQuest")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(tealAccent)
                    .tracking(1)
                    .opacity(titleAlpha)

                Spacer().frame(height: 8)

                Text("by JWorks")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(.white)
                    .tracking(0.5)
                    .opacity(subtitleAlpha)
            }
        }
        .opacity(fadeOut)
        .task {
            // Phase 1: Logo fade in + scale (0-800ms)
            withAnimation(.easeOut(duration: 0.8)) {
                logoAlpha = 1
                logoScale = 1
            }
            try? await Task.sleep(nanoseconds: 800_000_000)

            // Phase 2: Shimmer sweep (800-1400ms)
            withAnimation(.linear(duration: 0.6)) {
                shimmerOffset = 2
            }
            try? await Task.sleep(nanoseconds: 600_000_000)

            // Phase 3: Title fade in (1400-1900ms)
            withAnimation(.easeOut(duration: 0.5)) {
                titleAlpha = 1
            }
            try? await Task.sleep(nanoseconds: 500_000_000)

            // Phase 4: Subtitle fade in (1900-2300ms)
            withAnimation(.easeOut(duration: 0.4)) {
                subtitleAlpha = 1
            }
            try? await Task.sleep(nanoseconds: 400_000_000)

            // Phase 5: Hold (600ms)
            try? await Task.sleep(nanoseconds: 600_000_000)

            // Phase 6: Fade out (700ms)
            withAnimation(.easeOut(duration: 0.7)) {
                fadeOut = 0
            }
            try? await Task.sleep(nanoseconds: 700_000_000)

            onSplashComplete()
        }
    }
}
