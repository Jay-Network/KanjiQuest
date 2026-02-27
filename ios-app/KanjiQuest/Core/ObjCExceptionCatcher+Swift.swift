import Foundation

extension ObjCExceptionCatcher {
    /// Catch Objective-C NSExceptions as Swift errors.
    /// KMP/Kotlin-Native can throw NSExceptions that Swift `do/catch` can't handle.
    static func `catch`<T: AnyObject>(_ block: () -> T) throws -> T {
        var error: NSError?
        let result = catchException({ block() }, error: &error)
        if let error = error {
            throw error
        }
        guard let typedResult = result as? T else {
            throw NSError(domain: "com.jworks.kanjiquest", code: 2,
                          userInfo: [NSLocalizedDescriptionKey: "ObjC block returned nil"])
        }
        return typedResult
    }

    /// Void overload — catch NSExceptions from void calls (e.g. KMPBridge.initialize()).
    static func catchVoid(_ block: () -> Void) throws {
        var error: NSError?
        _ = catchException({
            block()
            return NSNull()
        }, error: &error)
        if let error = error {
            throw error
        }
    }
}
