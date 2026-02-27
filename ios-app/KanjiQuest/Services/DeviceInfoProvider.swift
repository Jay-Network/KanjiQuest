import Foundation
import UIKit
import SharedCore

enum DeviceInfoProvider {
    static func deviceInfo() -> DeviceInfo {
        let deviceName = UIDevice.current.name
        let platform: String
        #if IPAD_TARGET
        platform = "ios_ipad"
        #else
        platform = "ios_iphone"
        #endif
        let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.0.0"
        return DeviceInfo(deviceName: deviceName, platform: platform, appVersion: appVersion)
    }
}
