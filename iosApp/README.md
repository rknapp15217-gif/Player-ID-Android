# PlayerID iOS Host

This directory is the thin native host for the shared `PlayerIDShared` framework. Product rules and portable Compose UI belong in `shared`; Swift should only own Apple lifecycle and native service adapters.

## Run in the simulator

1. On macOS, install the current Xcode command-line tools and JDK 17.
2. Open `iosApp/iosApp.xcodeproj` in Xcode.
3. Select the `PlayerID` scheme and an iPhone simulator.
4. Run the app. The Xcode build phase invokes `:shared:embedAndSignAppleFrameworkForXcode` automatically.

Command-line validation does not require signing:

```bash
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme PlayerID \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO \
  build
```

## Run on a device

Set the app target's Team and a unique bundle identifier in Xcode. Signing identities and provisioning profiles stay in local Xcode settings and must not be committed.

Before App Store distribution, add the required usage descriptions and entitlements as each native adapter is implemented:

- Camera and microphone: AVFoundation
- Photos library import/export: PhotosUI and PhotoKit
- Speech recognition: Speech
- Contacts: ContactsUI
- Notifications and background processing: UserNotifications and BackgroundTasks
- Subscription purchase and restore: StoreKit

The current host proves framework embedding and shared Compose rendering. Native service adapters should implement the narrow contracts under `shared/src/commonMain/kotlin/com/playerid/app/platform`; do not duplicate shared reducers, parsers, policy, or Compose screens in SwiftUI.

General key-value persistence is available through `NSUserDefaultsPreferencesStore`. Referral persistence uses `NSUserDefaultsReferralStorage`, which can be passed directly to `ReferralService`.