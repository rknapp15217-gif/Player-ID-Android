# PlayerID Kotlin Multiplatform Migration

## Goal

Keep product behavior in one codebase. Android and iOS hosts should contain only operating-system adapters and application startup code.

## Current Foundation

- `:app` remains the production Android application.
- `:shared` is a Kotlin Multiplatform library consumed by `:app`.
- `commonMain` contains platform-neutral production code.
- `commonTest` validates behavior once for every platform.
- iOS targets are enabled automatically on macOS; Windows continues to build the Android target without requiring Xcode.
- `.github/workflows/kmp-shared.yml` compiles and tests the iOS simulator framework on a GitHub macOS runner, so Apple-target regressions can be caught before local Mac hardware is available.
- Shared production code now includes team matching, roster text parsing, tracking math, native service contracts, brand colors, and reusable vector icons.
- Shared team-domain profiles and repository contracts now sit in front of Android Room adapters. `TeamSubscriptionService` owns the onboarding rule that replaces all existing subscriptions with exactly one normalized team.
- Create Team form values, option lists, reducer events, duplicate matching, normalized submission, and Compose rendering now live in `commonMain`; Android preserves only its existing callback adapter.
- Roster list search and favorite-toggle reduction now operate on `PlayerProfile` in `commonMain`; Android retains saveable UI state, Room entities, photo URIs, and player mutation callbacks during the transition.
- `TeamRosterService` now owns team-scoped roster loading plus add, update, hard-delete, and OCR merge coordination. `PlayerViewModel` remains the Android facade and supplies UUIDs, timestamps, lifecycle scope, and Room-backed repository adapters.
- Add/Edit Player field state, academic-year options, reducer events, validation, profile initialization, and trimmed submissions now live in `commonMain`; Android dialogs preserve their existing `Player` callback APIs.
- Add/Edit Player dialog rendering now uses shared Compose Material3. Android wrappers retain only `Player` entity mapping, UUID/contributor assignment, and existing public callback signatures.
- Roster player row layout now renders from `PlayerProfile` in shared Compose. Android injects photo-picker and favorite-icon slots, keeping URI permissions and platform media access outside `commonMain`.
- The full roster page container now renders in shared Compose, including header, search, count, list, empty state, and add/import commands. Android injects icons and media-aware row slots through a private adapter.
- Create Team dialog flow and rendering now live in shared Compose, including duplicate advisories, sport selection, color-target state, RGB/HSV conversion, swatches, presets, and custom Canvas/pointer color selection. Edit Team reuses the shared color picker.
- Team detail page selection and overview/roster/schedule transitions now live in a tested shared reducer. Android retains saveable Compose state and navigation callbacks.
- Team overview rendering now lives in shared Compose. Android maps the assigned `Player` to `PlayerProfile` and injects Material icons, the assigned-player picker, and navigation/dialog callbacks.
- Team schedule search, upcoming/past partitioning, ordering, portable date/time label formatting, and Compose rendering now live in shared code. Android maps `GameSchedule`, supplies locale symbols and each instant's UTC offset, and injects icons/import callbacks.
- Schedule import entries plus CSV and free-form website/OCR parsing now live in shared code with portable timezone handling. Android maps shared entries into Room-backed schedules.
- Schedule import hierarchy/game construction and persistence orchestration now live in a tested shared service. Android injects team sport, local calendar year, UUIDs, and timestamps.
- Child, season, and game-schedule hierarchy reads now pass through shared profiles/repository contracts backed by an Android Room adapter. TeamViewModel and MemoryBrowsingViewModel preserve their existing entity APIs while the hierarchy storage boundary becomes implementable by an iOS host.
- Memory browsing reads now expose portable metadata and `MediaReference` through the same shared repository boundary. Android maps opaque identifiers back to Room entities and retains MediaStore decoding and permissions.
- Memory browsing hierarchy selection, descendant clearing, and flow collection now live in a shared state holder that cancels stale child/season/game collectors. The Android ViewModel preserves its existing Room-entity flows for the current screen.
- Memory browsing navigation chrome, transitions, hierarchy lists, cards, empty states, and media grid now render in shared Compose. Android injects date labels, the back icon, and native `ContentResolver` thumbnail decoding.
- Memory ingestion hierarchy, schedule, deduplication, and media mutations now use the shared storage repository. Accept/skip review behavior is shared and tested; Android retains MediaStore discovery and its native scan checkpoint.
- Memory-to-game matching now uses a tested shared scorer with portable schedule profiles, preserving the existing time window, midpoint decay, distance bands, and score weighting.
- Memory ingestion reduction now shares deduplication, matching thresholds, portable memory construction, persistence, prompt grouping, and scan-watermark calculation. Android retains MediaStore queries, permission handling, localized prompt labels, and checkpoint storage.
- Camera opponent restoration TTL and current-game selection now use portable shared rules with explicit timezone IDs. Android retains SharedPreferences, current-clock injection, and camera UI effects.
- Voice capture/team-switch/jersey/fuzzy-name command decisions now live in a tested shared processor over `PlayerProfile`. Android retains speech recognition, Room snapshots, TextToSpeech, recording actions, and lifecycle state.
- Referral codes, progress, milestones, links/messages, and reward mutations now live in a tested shared service behind `ReferralStorage`. Android preserves the existing `ReferralManager` API through a SharedPreferences adapter; an iOS host can supply its own storage implementation.
- Subscription access rules, trial countdown/status transitions, and referral/trial messages now live in tested shared policy functions. Android retains paywall state, lifecycle wiring, and the future Google Play Billing integration; iOS should retain StoreKit behind the same policy boundary.
- Roster and schedule import-source options now render through one shared dialog and portable source enum. Android supplies localized labels/icons and retains image picking plus app/website navigation effects.
- Team management now uses one tested shared dialog coordinator for add/edit player, settings, leave, invite, roster import, and schedule import transitions. Android maps edit-player IDs to Room entities and retains each dialog's ViewModel, picker, and navigation effects.
- Join Team subscription exclusion, search reduction, and dialog rendering now live in shared code. Android maps ViewModel flows to display items, supplies localized labels/icons, and retains the subscription side effect.
- Invite Team capability decisions, options/QR/NFC transitions, and dialog rendering now live in shared code. Android retains invite-link encoding, SMS/settings/share intents, QR bitmap generation, NFC foreground dispatch, and icon/content slots.
- Team Selection deep links, selected/created team state, dialog transitions, roster-opening rules, and page rendering now live in shared code. Android maps Room-backed teams to display items, serializes state for restoration, supplies localized icons/labels, and retains ViewModel, subscription, and TeamSnap side effects.
- Compose Multiplatform `1.5.10` runs on the conservative Kotlin `1.9.20` baseline; Android uses Compose compiler `1.5.4`.

## Dependency Rule

Dependencies point inward:

```text
androidApp -----> shared common code <----- iosApp
     |                                      |
CameraX, Media3                    AVFoundation, Vision
```

Code in `commonMain` must not import Android or Apple framework types. In particular, do not expose `Context`, `android.net.Uri`, `Bitmap`, Room entities, CameraX, Media3, ML Kit, `NSURL`, or `UIImage` through shared APIs.

Use `MediaReference` for media identity and capability flags for workflows that differ by platform. Prefer injected interfaces over broad `expect`/`actual` declarations so common behavior can be tested with fakes.

## Intended Modules

```text
shared/src/commonMain
  data/model          Plain shared models
  data/repository     Repository interfaces
  domain              Use cases, matching, parsing, validation
  presentation        StateFlow state holders and UI events
  platform            Narrow native service contracts
  ui                  Compose Multiplatform screens and components

shared/src/androidMain
  Android implementations backed by Room, CameraX, Media3, ML Kit

shared/src/iosMain
  iOS implementations backed by SQLite, AVFoundation, AVKit, Vision

app
  Android manifest, Application, Activity, services, and native adapters

iosApp
  Xcode project, signing, entitlements, Info.plist, and Compose root host
```

## Migration Order

1. Move pure utilities and parsers, preserving their packages and APIs.
2. Separate Room annotations and Parcelable types from domain models.
3. Introduce repository interfaces in `commonMain`; initially wrap existing Room DAOs.
4. Move networking from Retrofit/Gson to Ktor and Kotlinx Serialization.
5. Move screen state from `AndroidViewModel` to common state holders using coroutines and `StateFlow`.
6. Adopt a single cross-platform database schema. Evaluate SQLDelight before changing the existing Room database.
7. Move platform-neutral Compose screens and components to shared UI.
8. Wrap camera, playback, export, OCR, speech, contacts, notifications, and media-library access behind platform interfaces.
9. On a Mac, add the minimal Xcode `iosApp` host and implement iOS adapters.

Move Camera and video processing last. They have the largest platform surface and should consume stable shared workflows rather than define them.

## Next Safe Extractions

- Audit remaining Android ViewModels for platform-neutral reducers and repository orchestration that can move without changing native media/camera boundaries.
- Screen state after its Android services are replaced by the existing shared service contracts.
- Define the referral reward lifecycle before changing the legacy `earned`/`used` claim semantics; the current extraction intentionally preserves existing behavior.

Do not move `Player`, `Team`, or `GameSchedule` directly yet: current definitions combine domain data with Room and Parcelable annotations.

## Validation

On Windows or macOS:

```powershell
.\gradlew.bat :shared:testDebugUnitTest
.\gradlew.bat :shared:checkCommonMainBoundaries
.\gradlew.bat :app:compileDebugKotlin
```

On macOS, after Xcode is installed:

```bash
sh gradlew :shared:iosSimulatorArm64Test
sh gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

Until a local Mac is available, push the branch and confirm both jobs in the `KMP Shared` GitHub Actions workflow. The iOS job publishes the generated debug simulator framework as a workflow artifact.

Every extraction should pass shared tests and the Android compile before the next file is moved.

## iOS Activation Checklist

- Use a Mac with the current Xcode command-line tools.
- Open the repository on macOS so Gradle configures `iosX64`, `iosArm64`, and `iosSimulatorArm64`.
- Create `iosApp` as a thin Xcode host; do not recreate shared screens in SwiftUI.
- Embed the generated `PlayerIDShared` framework or configure direct Gradle/Xcode integration.
- Add bundle ID, signing team, permission descriptions, entitlements, icons, and App Store metadata.
- Implement AVFoundation camera, AVKit playback, Vision OCR, Photos media selection, speech, contacts, and background-task adapters.
- Provide screenshot/photo import on iOS where Android currently uses unrestricted MediaProjection capture.

## Change Policy During Migration

- New business rules go into `commonMain`.
- Platform modules translate native input/output only.
- Shared UI consumes immutable state and emits events; it does not call native APIs directly.
- Database and API schema changes must have one source of truth.
- Do not maintain separate Android and Swift implementations of the same screen.
