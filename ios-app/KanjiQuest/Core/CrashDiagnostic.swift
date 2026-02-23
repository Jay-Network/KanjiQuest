import Foundation

/// Writes initialization breadcrumbs to a file so crash point is visible on relaunch.
/// Works even without Xcode console access.
enum CrashDiagnostic {
    private static let fileName = "crash_breadcrumb.txt"
    private static let completedMarker = "===INIT_COMPLETE==="

    private static var fileURL: URL? {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first?
            .appendingPathComponent(fileName)
    }

    /// Call at the very start of app launch to begin tracking.
    static func begin() {
        guard let url = fileURL else { return }
        try? "LAUNCH_STARTED\n".write(to: url, atomically: true, encoding: .utf8)
        installSignalHandlers()
    }

    /// Record a breadcrumb step. If the app crashes, the last step written is the one that succeeded.
    static func step(_ description: String) {
        guard let url = fileURL else { return }
        let line = "STEP: \(description)\n"
        if let handle = try? FileHandle(forWritingTo: url) {
            handle.seekToEndOfFile()
            handle.write(line.data(using: .utf8) ?? Data())
            handle.closeFile()
        } else {
            try? line.write(to: url, atomically: true, encoding: .utf8)
        }
        NSLog("KanjiQuest [Diag]: %@", description)
    }

    /// Mark initialization as complete. No crash occurred.
    static func complete() {
        guard let url = fileURL else { return }
        if let handle = try? FileHandle(forWritingTo: url) {
            handle.seekToEndOfFile()
            handle.write("\(completedMarker)\n".data(using: .utf8) ?? Data())
            handle.closeFile()
        }
        NSLog("KanjiQuest [Diag]: Init complete")
    }

    /// Check if the previous launch crashed. Returns crash info or nil.
    static func checkPreviousCrash() -> String? {
        guard let url = fileURL else { return nil }
        guard let content = try? String(contentsOf: url, encoding: .utf8) else { return nil }

        // If the file contains our completed marker, no crash
        if content.contains(completedMarker) {
            // Clean up — no crash
            try? FileManager.default.removeItem(at: url)
            return nil
        }

        // File exists but no completed marker = previous launch crashed
        let lines = content.components(separatedBy: "\n").filter { !$0.isEmpty }
        let lastStep = lines.last(where: { $0.hasPrefix("STEP:") })
            ?? lines.last
            ?? "UNKNOWN"

        // Also check for signal handler info
        let signalInfo = lines.first(where: { $0.hasPrefix("SIGNAL:") })

        var crashInfo = "Previous launch crashed.\n\nLast successful step:\n\(lastStep)"
        if let sig = signalInfo {
            crashInfo += "\n\n\(sig)"
        }
        crashInfo += "\n\nFull breadcrumb trail:\n" + lines.joined(separator: "\n")

        return crashInfo
    }

    /// Clear previous crash data (call after user has seen the crash info).
    static func clearCrashData() {
        guard let url = fileURL else { return }
        try? FileManager.default.removeItem(at: url)
    }

    // MARK: - Signal Handler

    private static func installSignalHandlers() {
        // Catch fatal signals and write to breadcrumb file
        let signals: [Int32] = [SIGSEGV, SIGBUS, SIGABRT, SIGTRAP, SIGILL, SIGFPE]
        for sig in signals {
            signal(sig) { signalNumber in
                let name: String
                switch signalNumber {
                case SIGSEGV: name = "SIGSEGV (Segmentation Fault)"
                case SIGBUS: name = "SIGBUS (Bus Error)"
                case SIGABRT: name = "SIGABRT (Abort)"
                case SIGTRAP: name = "SIGTRAP (Trace Trap)"
                case SIGILL: name = "SIGILL (Illegal Instruction)"
                case SIGFPE: name = "SIGFPE (Floating Point)"
                default: name = "Signal \(signalNumber)"
                }

                // Write signal info to breadcrumb file (async-signal-safe: use write() syscall)
                if let docsPath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).first {
                    let path = docsPath + "/crash_breadcrumb.txt"
                    let msg = "\nSIGNAL: \(name)\n"
                    if let fd = fopen(path, "a") {
                        fputs(msg, fd)
                        fclose(fd)
                    }
                }

                // Re-raise to get default crash behavior
                signal(signalNumber, SIG_DFL)
                raise(signalNumber)
            }
        }
    }
}
