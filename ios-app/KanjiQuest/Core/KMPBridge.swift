import Foundation
import SharedCore

/// Thin initialization wrapper for shared-core KMP framework.
/// Ensures the Kotlin/Native runtime is properly bootstrapped before use.
enum KMPBridge {

    /// Call once at app launch to initialize Kotlin/Native.
    /// SharedCore's static framework auto-initializes, but this serves
    /// as the explicit entry point for any future init requirements.
    static func initialize() {
        // NativeSqliteDriver and Ktor Darwin engine initialize on first use.
        // This method exists as a hook for future pre-warm or diagnostics.
        #if DEBUG
        print("[KMPBridge] SharedCore framework loaded")
        #endif
    }
}
