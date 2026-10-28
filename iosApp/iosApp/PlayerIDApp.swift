import PlayerIDShared
import SwiftUI
import UIKit

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        IosOrientationLock.shared.onChange = { AppDelegate.applyOrientationLock() }
        return true
    }

    // Shared UI decides which screens may rotate (only Camera); everything else stays portrait.
    func application(
        _ application: UIApplication,
        supportedInterfaceOrientationsFor window: UIWindow?
    ) -> UIInterfaceOrientationMask {
        IosOrientationLock.shared.rotationAllowed ? .allButUpsideDown : .portrait
    }

    private static func applyOrientationLock() {
        let mask: UIInterfaceOrientationMask = IosOrientationLock.shared.rotationAllowed ? .allButUpsideDown : .portrait
        for case let scene as UIWindowScene in UIApplication.shared.connectedScenes {
            if #available(iOS 16.0, *) {
                scene.windows.forEach { $0.rootViewController?.setNeedsUpdateOfSupportedInterfaceOrientations() }
                scene.requestGeometryUpdate(.iOS(interfaceOrientations: mask)) { _ in }
            } else if !IosOrientationLock.shared.rotationAllowed {
                UIDevice.current.setValue(UIInterfaceOrientation.portrait.rawValue, forKey: "orientation")
                UIViewController.attemptRotationToDeviceOrientation()
            } else {
                UIViewController.attemptRotationToDeviceOrientation()
            }
        }
    }
}

@main
struct PlayerIDApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
