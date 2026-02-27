import Foundation

extension ObjCExceptionCatcher {
    /// Catch Objective-C NSExceptions as Swift errors.
    /// KMP/Kotlin-Native can throw NSExceptions that Swift `do/catch` can't handle.
    ///
    /// The ObjC method `+catchException:error:` is bridged by Swift as a throwing
    /// function: `catchException(_ block:) throws -> Any?` — the `error:` parameter
    /// becomes the thrown NSError automatically.
    static func `catch`<T: AnyObject>(_ block: () -> T) throws -> T {
        let result = try catchException { block() }
        guard let typedResult = result as? T else {
            throw NSError(domain: "com.jworks.kanjiquest", code: 2,
                          userInfo: [NSLocalizedDescriptionKey: "ObjC block returned nil"])
        }
        return typedResult
    }

    /// Void overload — catch NSExceptions from void calls (e.g. KMPBridge.initialize()).
    static func catchVoid(_ block: () -> Void) throws {
        _ = try catchException {
            block()
            return NSNull()
        }
    }
}
