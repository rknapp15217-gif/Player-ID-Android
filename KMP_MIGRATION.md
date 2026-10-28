# PlayerID Kotlin Multiplatform Migration

## Goal

Keep product behavior in one codebase. Android and iOS hosts should contain only operating-system adapters and application startup code.

## Active Development Direction

- Ship Android and iPhone applications. macOS is the development environment, not an additional desktop target.
- Keep Kotlin Multiplatform and shared Compose UI; do not rewrite shared Kotlin screens or product rules in Swift.
- The production Android app is the visual and workflow source of truth. Preserve its branding, typography, icons, layout, navigation, and camera-first flow during migration; do not design a separate iPhone product. Discuss unavoidable platform-visible differences before implementing them.
- Use VS Code for editing and targeted Gradle checks. Use Xcode for physical-iPhone signing, installation, and native debugging.
- Preserve the working Android application and its storage while implementing iOS adapters against existing shared contracts.
- Bring up team selection, persistent team/roster data, and schedule workflows on iPhone before camera and video processing. Reuse the existing shared screens rather than building a separate SwiftUI product interface.
- The iOS entry point now opens the shared camera-first shell and team workspace instead of the framework placeholder. The user confirmed the app structure, native camera capture, and corrected capture-gallery playback work on the physical iPhone. New increments still need separate device acceptance; a linked framework is not full iOS feature parity.

## Current Foundation

- Playloom account foundation (October 23, 2026): both hosts offer optional **More > Account** email-code authentication against one Supabase project. Shared Kotlin owns the Auth REST flow and account state; Android Keystore/AES-GCM and iPhone Keychain adapters persist credentials. Existing local identities, databases, contacts and media are not migrated or uploaded. Shared team sync, membership/family RLS policies and Photos-original migration are separate upcoming milestones. See [account setup and device checks](iosApp/README.md#playloom-account-testing-android-and-iphone), including required email-code templates and SMTP configuration.
- `:app` remains the production Android application.
- `:shared` is a Kotlin Multiplatform library consumed by `:app`.
- `commonMain` contains platform-neutral production code.
- `commonTest` validates behavior once for every platform.
- The first iPhone camera increment connects AVFoundation preview, JPEG capture, and audio/video recording to the shared `CameraService`. The Android mode-button/shutter visuals are shared without replacing Android's CameraX actions. Shared capture state owns readiness, mode locking, errors, durable-save acknowledgement, native recording completion, and save-before-close behavior. Captures remain private local media files; automatic current-game matching, player tagging, Rolling, detection, and speech remain pending. The user confirmed the camera increment works on the physical iPhone.
- The iPhone Memories tab lists these private captures and opens photos or native AVPlayer video playback. It also supports manual team/optional player/season/game assignments and team/season/opponent/player/unassigned filters, using the original Android filter-label visuals. Organization is shared Kotlin logic persisted atomically in the existing version-1 snapshot with defaulted additive fields; filename keys survive iOS container-path changes. Existing captures retain their assignments, and media files are untouched. The user confirmed the organization/filter increment works on the physical iPhone on October 18, 2026. It is not full Android Clips parity: imported-video review, player overlays, tags, editing/trimming, reels, batch operations, teammate review, sharing/export, and Android/cloud metadata synchronization remain pending. See grouped device checks in `iosApp/README.md`.
- The next camera increment shares Android's team/opponent/player setup pill, including pulse, jersey vector paths and home/away/special-team color options. iPhone Game setup follows the original sheet treatment and supports a persisted team, jersey selection, optional player, free-text opponent and explicit scheduled game. New captures inherit frozen start-time details, including during tab/background/native finalization; organization-write errors preserve the media and give recovery instructions. No team leaves subsequent captures unassigned. This increment requires device layout/interaction acceptance and does not claim exact screen parity; post-capture roster tagging, detection, Rolling, speech and standby remain pending. Android keeps existing Room/selection/capture behavior.
- The following camera slice adds the roster drawer's dark peek/full panel, swipe handle, search, shared Android player rows/details, and animated preview/control offsets. iPhone uses active players from the camera team and an explicit Use for new captures action, persisted atomically without replacing matchup/jersey settings or earlier memories. Browsing alone changes nothing; selection is locked while recording/saving. Close/expand controls replace the unavailable microphone, and invite badges are not fabricated. Android keeps its existing voice/invite/browse callbacks. This slice also needs physical-iPhone visual/touch acceptance; it is not full roster tagging or exact camera parity.
- iPhone preview pinch zoom now follows Android's multiplicative gesture behavior using tested shared bounds logic and AVFoundation device zoom under a configuration lock. Hardware-reported limits apply, including fixed zoom ranges. Zoom is permitted while ready/recording, not during setup or capture finalization; configuration errors are visible without stopping recording. No toolbar or Swift product logic is added, and Android zoom behavior is unchanged. Gesture routing, saved framing and recording zoom still require physical-iPhone acceptance.
- Device testing found the initial Compose-wrapper pinch handler unresponsive. It is replaced with a native preview `UIPinchGestureRecognizer` and enabled multitouch; shared UI still owns readiness/setup guards and visible errors. The user confirmed corrected pinch zoom on the physical iPhone on October 19, 2026.
- Foreground iPhone video recording now temporarily disables idle auto-lock and restores the prior setting on recording completion/failure, screen disposal or app inactivity. This is deliberately narrower than Android's camera-wide keep-screen-on behavior: idle iPhone preview retains normal auto-lock. No brightness change or new standby action is added; Android's current standby overlay has no active entry action. The user confirmed recording auto-lock protection on the physical iPhone on October 19, 2026.
- iPhone Game setup now offers a current-game suggestion using Android's existing shared nearest-start/local-date/time-window matcher, scoped to the team's active seasons and refreshed each minute. Use scheduled matchup fills the draft opponent/season/game; Done persists through existing setup storage. Unlike Android's automatic opponent replacement, iPhone requires explicit acceptance to preserve its saved/manual context. Cancel changes nothing, and old captures are untouched. This suggestion increment still requires device acceptance; automatic reassignment remains out of scope.
- The user confirmed current-game suggestions on the physical iPhone on October 19, 2026.
- The next Memories visual increment removes the interim blue collection header and square three-column grid. It uses shared Android collection-tab styling, the existing filter controls, surface-colored chrome, separator and two/three-column responsive 124dp cards. Team/season/opponent grouping uses the current assignment data with Android-style accented headings and preserves unassigned captures. Working Edit details remains accessible from each card; unavailable Reels is disclosed, and import/search/batch/share/editor actions are not simulated. Viewer/native media rendering stays unchanged. Physical-iPhone visual/touch acceptance is pending.
- The user confirmed the Memories collection-layout increment on the physical iPhone.
- The next collection increment adds an Android-styled metadata search bar with shared case-insensitive, all-term matching across team/player/jersey/season/opponent/game/type and Unassigned. Search intersects filters and survives tab changes; clear and no-result reset paths preserve media. It does not implement Android natural-language reel/tag queries or speech. The viewer header also keeps title/back in one row to preserve playback space. The user confirmed search, keyboard behavior and playback on the physical iPhone on October 19, 2026.
- Memories now offers one-at-a-time Apple Photos import through native PHPicker without requesting full-library permission. The OS adapter copies only the selected item into private durable storage, converts still images to JPEG and preserves supported MOV/MP4/M4V videos. Shared filename validation/listing supports imported video extensions; shared collection UI owns progress/errors, reload and existing organization/search/filter behavior. Imports are unassigned, do not inherit Camera setup, and never modify Photos originals. Batch import, deduplication, original-date indexing, editing and synchronization remain pending. Native picker/iCloud/playback acceptance requires physical-iPhone testing; see `iosApp/README.md`.
- The user confirmed the Apple Photos import checklist on the physical iPhone on October 19, 2026.
- The viewer-shell increment extracts Android's primary-blue, empty-title single-memory toolbar into shared Compose while retaining Android editing callbacks and system insets. iPhone uses the working Edit details action and its existing host safe area, removes the interim metadata row/footer, and expands the black media canvas without changing native playback or saved data. Physical-iPhone layout/interaction acceptance is pending. This is not full viewer parity: Android's metadata ticket, custom playback controls, zoom, tags/overlays, trimming and sharing remain pending.
- The user confirmed the viewer-shell increment on the physical iPhone on October 19, 2026.
- Viewer pinch zoom now shares Android's 1x-4x multiplicative bounds in common Kotlin. iPhone photos use a clipped Compose display transform; video uses native pinch routing and a centered Core Image playback composition, keeping AVPlayer controls unscaled and requesting paused-frame refresh without seeking. Zoom resets on reopening and does not modify files or metadata. Android retains its rendering/gesture behavior using the same bounds helper. Device gesture, paused/playing video, rotation, performance and HDR appearance acceptance are pending; this adds no panning or persistent edits.
- The user confirmed viewer pinch zoom on the physical iPhone on October 19, 2026.
- The next viewer increment extracts Android's translucent metadata ticket into shared Compose, preserving its tag/opponent/date content and constraining long opponent text. iPhone supplies its stored scheduled/manual opponent only; missing original capture dates and moment tags are not invented. Photo tickets remain outside zoom; native video hosts the shared ticket through a transparent noninteractive child Compose controller so it appears above AVPlayer without blocking controls. Organization updates remain reactive and media is unchanged. Native overlay placement/transparency/touch acceptance is pending.
- Device testing reported a blank white video screen after the nested Compose metadata overlay. The video adapter replaces that full-screen controller with a small noninteractive UIKit label/container matching the ticket styling; photo/Android tickets remain shared Compose, and opponent selection remains shared logic. This avoids a second Compose rendering surface over AVPlayer while retaining playback/zoom. Physical-iPhone playback and overlay revalidation are pending.
- The user confirmed the revised native metadata overlay and video white-screen fix on the physical iPhone on October 19, 2026.
- The next viewer increment replaces iPhone AVPlayer control chrome with native-rendered Android-style bottom seek and play/pause controls, avoiding nested Compose rendering. Shared Kotlin owns scrub pause/resume, seek clamping, end replay and lifecycle invalidation of pending seeks; native AVPlayer supplies decoding and seek completion. Touch/VoiceOver seeking, controls, zoom/ticket interaction and foreground-loss behavior require device acceptance. Android ExoPlayer behavior is unchanged. Continuous scrub-preview frames, playlists, native fullscreen/speed/AirPlay menus, editing and sharing are not implemented by this increment.
- The user confirmed Android-style playback controls on the physical iPhone on October 19, 2026.
- Live iPhone scrub previews now use asynchronous AVAssetImageGenerator requests, orientation-correct frames capped at 480×270, roughly 120ms request throttling and cancellation/generation guards. A timestamped native tile displays above the seek controls; it hides on seek completion/cancel, interruption or viewer disposal. It doesn't intercept player controls, modify media or resume audio independently. Android's existing frame-preview pipeline is unchanged. Device latency, correctness, responsiveness and foreground cancellation acceptance are pending.
- The user requested main-video tracking instead of a separate preview box. The preview views/generator are removed. Shared playback controls now seek the main AVPlayer continuously during dragging, allowing one in-flight seek and coalescing pending requests to the latest forward/backward position. The final queued seek completes before prior playback resumes; paused clips stay paused, and interruption discards queued work and invalidates callbacks. Audio stays paused during dragging. Android is unchanged; exact smoothness/latency on physical iPhone remains pending.
- Slow-drag device testing reported jerky video. Intermediate iPhone seeks now allow 33ms tolerance; release always queues an exact final seek before resuming. Core Image composition is deferred until zoom is needed and removed at 1x, avoiding zoom-processing overhead during ordinary playback/scrubbing. Tests cover tolerant live/exact final seeks and lifecycle interruption; actual smoothness requires physical-iPhone revalidation.
- Stutter persisted even on short PlayerID recordings. Seek completions now hand off directly through the main dispatch queue, replacing the main operation queue in the seek chain. Debug-only per-gesture timing summaries separate native seek latency from main-thread delivery delay; they contain counts/timings only. Smoothness remains unverified and requires device timing evidence if the scheduling change does not resolve it.
- Scrub smoothness is deferred and unresolved at the user's request on October 20, 2026; continue other product work without claiming it fixed.
- Memories sharing now exposes a direct share icon on every thumbnail for exactly one original file and Select in the top Memories header for multiple checked visible memories. Shared Kotlin owns selection, deduplication and outgoing list construction; iOS validates readable local URLs and presents the native activity sheet with explicit completion/cancel/error handling. Hidden selections are removed when search/filters change; no media, saved organization or Android sharing workflow is changed. A Photos add-only usage description supports system Save Image/Video where available. No teammate-recipient logic, edited exports, metadata burn-in, merge or automatic upload is added. Physical-iPhone share-sheet and received-file acceptance are pending.
- The shared iPhone post-recording "What happened?" page now offers Android's five standard moment tags, reusable custom tags (rename/remove button), Share Clip, confirmed Delete and Skip. Review becomes available after successful video/details saving, including native/background finalization; dismissal is tracked in the session's shared capture state. Tags are durable defaulted version-1 snapshot metadata keyed by filename, shown in thumbnails/photo/native-video tickets and included in search. Native code only presents the system share sheet and deletes validated private files; shared Kotlin owns the flow, tag storage and metadata cleanup. System sharing does not yet port Android's player/guardian recipient picker or detection suggestions, and review prompts are not a durable cross-launch queue. Android behavior is unchanged. Physical-iPhone acceptance is pending; see the README checklist.
- iOS targets are enabled automatically on macOS; Windows continues to build the Android target without requiring Xcode.
- On October 20, 2026, the user confirmed the multi-selection Photos import workflow works on the physical iPhone. Individual iCloud/storage failure, large-batch and interruption scenarios remain unconfirmed. Scrub smoothness remains deferred.
- The next user-selected increment extends Apple Photos import to multiple selections without a fixed count cap. Shared Kotlin processes items sequentially with completed/total progress and explicit per-item failure reports; successful copies are retained and later items continue after a failure. Native PHPicker supplies only chosen items and callback-lifetime-safe private copies, with no unrestricted Photos permission. Imports remain Unassigned and preserve Photos originals/existing metadata. Cancel before submission imports nothing; submitted batches have no rollback/mid-batch cancel or cross-launch job resumption. Mixed-media/iCloud/large-batch and lifecycle behavior need physical-iPhone acceptance.
- On October 20, 2026, the user confirmed the post-trim Keep original / Delete original workflow works on the physical iPhone. This supersedes retention-choice acceptance status below; broader codec/output/interruption checks remain unconfirmed, and scrub smoothness remains deferred.
- On October 20, 2026, the user confirmed all three iPhone sharing entry points work: direct thumbnail Share, top-header Select for multiple memories, and post-recording Share Clip. This supersedes the sharing-entry-point acceptance status above, not the unconfirmed tagging/deletion checks or deferred scrub smoothness.
- The next user-selected increment adds a shared guardian/player recipient step across those entry points. iPhone Contacts picks an explicit phone/email endpoint, shared Kotlin persists team-scoped guardian/active-player links, and native Messages/Mail composers prepare addressed attachments for user-reviewed sending. General share remains available without roster data. Every selected file is retained for mixed-team batches; contact-team selection does not change media organization or tag featured players. Cancellation, saved drafts, unavailable composer capabilities and attachment failures are explicit; no automatic sending or delivery guarantee is implied. Contacts are local-only and not synchronized with Android/Apple Contacts. Native presentation, contact selection and composer delivery need physical-iPhone acceptance.
- The user confirmed adding guardians through the recipient/Contacts flow on October 20, 2026. Individual Messages/Mail send, draft, attachment and delivery checks remain unconfirmed; scrub smoothness remains deferred.
- The next user-selected slice adds video trimming and saved-copy export. The shared viewer's scissors action opens an Android-styled two-handle range editor with exact timestamp labels and selected-range native preview. AVFoundation exports MP4/MOV into a new private memory; the original is not overwritten. Shared Kotlin atomically copies team/player/season/game/opponent and moment tag; metadata failure explicitly retains the saved copy and offers retry without duplicate encoding. After successful file/detail saving, the user chooses Keep original or Delete original; deletion removes only the source and its details, with explicit file/cleanup errors and retry. No choice or deletion occurs after export/detail-copy failure. The new copy uses existing guardian/general sharing. This does not implement overlays/name bubbles, reels or arbitrary sub-frame/color/codec fidelity. Physical-iPhone range/output/audio/orientation/cancellation and retention-choice acceptance is pending. Scrub smoothness remains deferred.
- The following user-selected increment adds named video reels: shared Compose source selection, reorder/remove controls, durable filename-keyed names/source order, enabled Reels collection, source-aware search/filtering, and existing playback/guardian/general sharing. Native AVFoundation joins at least two videos with orientation-aware aspect-fit into 720x1280 at 24 fps, preserving each source's first audio track and silence/gaps. The user approved no intro, deliberately differing from Android's current silent/title-intro export; Android behavior is unchanged. Sources are never deleted by reel creation. A shared native export/save helper now serves both trimming and reels; metadata-only retry avoids duplicate encoding after a metadata failure. Older snapshots default to no reels. Drafts/export jobs are not durable, output is re-encoded, and no photo slides, music or overlays are added. Physical-iPhone audio/order/orientation/dimensions/cancellation and UI acceptance remain pending; see the README checklist. Scrub smoothness remains deferred.
- On October 20, 2026, the user confirmed the core reel workflow on physical iPhone, then requested Android-aligned layout/styling for reel creation/editing. The shared editor now follows Android's surface header, bold title field, rounded 158dp first-memory preview, tinted thumbnail rows with stacked reorder arrows, and separate searchable Add Memories page. Native playback is reused for the explicitly labeled first-memory preview, not a whole-reel render. iPhone retains 48dp touch targets instead of Android's compact 28dp controls; unavailable duration/fullscreen decorations are not fabricated. Export/storage/source preservation are unchanged. Layout/touch and preview lifecycle acceptance remain pending; broader reel edge cases are not individually confirmed and scrub smoothness remains deferred.
- The next requested pass continues Android-style layout/formatting alignment across Memories/Reels collection chrome, trim controls, post-recording review actions, and Team/Roster page headers/summary rows. This pass is shared-UI styling only: no changes to native adapters, storage schemas, sharing/export behavior, or previously accepted flows. Physical-iPhone visual/touch confirmation across those screens remains pending.
- The subsequent focused pixel-level pass targets Memories against Android `ClipsScreenRefactored`: removes the extra subtitle, places Photos import as a labeled Add icon beside collection tabs, aligns the 48dp search/filter/grid spacing, and matches Android's tag-colored thumbnail badge while removing its duplicate lower tag. It preserves top-header Select, import batch behavior, collection tabs, direct share, edit-details, selection and playback controls. iPhone VoiceOver now identifies the compact import icon. Physical-iPhone visual/touch acceptance is pending; subsequent screen-by-screen comparison should proceed only after this check.
- The user redirected alignment to Camera after reporting mode overflow and menu/setup color differences. At the user's choice, iPhone Camera and its navigation now use the shared light palette independently of system appearance; other destinations and Android retain their theme behavior. Camera mode controls use equal-width compact buttons instead of three scrolling 136dp minimum widths. Rolling remains disabled and explicitly Unavailable; its implementation is deferred by agreement. Game setup uses Android's team/jersey/matchup hierarchy, sport-grouped team menu and team-accented opponent styling, with iPhone's existing player/season/game controls preserved in an expandable section. Capture/storage/setup-save semantics are unchanged. Physical-iPhone fit, color, keyboard and interaction acceptance remain pending.
- `.github/workflows/kmp-shared.yml` compiles and tests the iOS simulator framework on a GitHub macOS runner, so Apple-target regressions can be caught before local Mac hardware is available.
- `iosApp` is a thin SwiftUI lifecycle host around a shared `ComposeUIViewController`. Its Xcode build phase embeds `PlayerIDShared`, and CI compiles the unsigned simulator application.
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
- iOS key-value persistence is implemented in `iosMain` with `NSUserDefaultsPreferencesStore` and `NSUserDefaultsReferralStorage`, keeping Apple framework types outside shared contracts and product policy.
- Roster and schedule import-source options now render through one shared dialog and portable source enum. Android supplies localized labels/icons and retains image picking plus app/website navigation effects.
- Team management now uses one tested shared dialog coordinator for add/edit player, settings, leave, invite, roster import, and schedule import transitions. Android maps edit-player IDs to Room entities and retains each dialog's ViewModel, picker, and navigation effects.
- Join Team subscription exclusion, search reduction, and dialog rendering now live in shared code. Android maps ViewModel flows to display items, supplies localized labels/icons, and retains the subscription side effect.
- Invite Team capability decisions, options/QR/NFC transitions, and dialog rendering now live in shared code. Android retains invite-link encoding, SMS/settings/share intents, QR bitmap generation, NFC foreground dispatch, and icon/content slots.
- Team Selection deep links, selected/created team state, dialog transitions, roster-opening rules, and page rendering now live in shared code. Android maps Room-backed teams to display items, serializes state for restoration, supplies localized icons/labels, and retains ViewModel, subscription, and TeamSnap side effects.
- Compose Multiplatform `1.5.10` runs on the conservative Kotlin `1.9.20` baseline; Android uses Compose compiler `1.5.4`.
- The iPhone host now connects shared team selection, team creation/settings, roster add/edit/delete/search, favorites, assigned-player selection, and manual/CSV/text schedule workflows. Roster text import and schedule import show recognized previews before applying changes.
- `TeamWorkspace` composes the existing roster and schedule services in `commonMain`. `LocalTeamRepository` implements the existing contracts with a versioned Kotlinx Serialization snapshot; iOS stores that snapshot atomically in Application Support through `IosTeamSnapshotStorage`. Multi-step imports commit once, failed writes do not publish changes, and unreadable or incompatible data is not silently replaced.
- This first iPhone workspace is explicitly local-only: teams are not downloaded from Android or other phones, leaving a team only hides it, and rejoining restores local access. Team names are fixed after creation; sport and jersey colors remain editable. Android Room storage is unchanged. A shared database and cloud synchronization remain separate migrations.
- Unimplemented TeamSnap/sharing actions are hidden in the iPhone workspace. Basic camera capture is connected in the next increment; photo import/OCR, video playback/export, speech, notifications, and purchases remain pending.
- Android's exact light/dark color schemes and typography now live in shared code. Android retains only system-bar and optional dynamic-color adapters; iPhone consumes the same brand theme. Team, roster, and schedule icon slots now use the Android Material artwork instead of interim text symbols. This corrects branding, not full screen/navigation parity.

## Restoring Android Experience on iPhone

The existing Android `PlayerIDApp` starts on Camera after onboarding. Its bottom navigation routes are Clips / Camera / More (the Clips tab's current user-facing label is **Memories**), with Teams and Settings reached through More.

### App-structure milestone

- Both hosts now consume `PlayerIDAppShell`, the original `SpotrBottomNavigationBar`, and shared More destination rows. Android keeps its existing NavController transitions, localized labels, CameraX screen, and all destination effects.
- `OnboardingFlow` reuses the original Android welcome, choice, team search/preview, player selection, ready screen, progress dots, wording, spacing, and button layout. `OnboardingState` shares the transitions. Android retains its Room/ViewModel effects, URI route encoding, system Back handling, and existing completion preference.
- The shared welcome logo reproduces the Android adaptive launcher's foreground paths and gradient, not the outdated raster mipmap icon. Both welcome screens use the same Compose artwork.
- iPhone onboarding completion is persisted atomically in the existing workspace snapshot. Older snapshots without the new optional completion field still load. Existing workspaces with teams skip onboarding; completing onboarding without a team also persists. No uninstall/reset is needed.
- iPhone now starts on Camera after onboarding, with the Android tab order and More > Teams/Settings structure. Team detail/search state survives tab switches. Camera connects basic native capture with shared Android controls, but not the complete game/player workflow. Memories exposes the locally captured photos/videos but does not yet match the complete Android Clips experience. Settings still displays an explicit integration-pending message.
- Fresh iPhone onboarding only searches locally saved teams until cross-device discovery is implemented. Subscription replacement and player assignment use the existing shared rules in a single durable transaction.

Continue in this order:

1. Shared shell and onboarding: reported working on the physical phone. Keep comparing corresponding screens with Android; do not delete existing app data to force onboarding.
2. Extract the Camera screen's existing visual controls and workflow without replacing them with a different camera UI. Separate CameraX/MediaStore/speech effects from portable UI before implementing AVFoundation/Photos adapters.
3. Connect iPhone preview, photo/video capture, permission handling, and lifecycle to that shared camera flow; do not add success-shaped capture controls or pretend a preview-only implementation has recording/detection parity.
4. Convert the full Android Clips library and its existing review/playback workflows, using native media adapters and preserving filters, tags, editing, reels, sharing, and export.
5. Validate corresponding Android/iPhone screens side by side and exercise the full user journey on the physical phone. Framework compilation alone does not establish visual or workflow parity.

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
9. Implement iOS adapters in the existing minimal Xcode `iosApp` host.

Move Camera and video processing last. They have the largest platform surface and should consume stable shared workflows rather than define them.

## Next Safe Extractions

- Audit remaining Android ViewModels for platform-neutral reducers and repository orchestration that can move without changing native media/camera boundaries.
- Screen state after its Android services are replaced by the existing shared service contracts.
- Persist a referral free-year expiration date when native billing is implemented; the current policy tracks earned and claimed/in-use states but does not yet expire the entitlement.

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
sh gradlew :shared:checkCommonMainBoundaries
sh gradlew :shared:linkDebugFrameworkIosArm64
```

The second command validates the framework for a physical iPhone without signing or building the full host. Run the host on a paired iPhone through Xcode using the steps in `iosApp/README.md`.

Simulator checks remain useful for CI and are optional for local phone-first development:

```bash
sh gradlew :shared:iosSimulatorArm64Test
sh gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

Confirm both jobs in the `KMP Shared` GitHub Actions workflow when changes are pushed. The iOS job publishes the generated debug simulator framework as a workflow artifact.

Every extraction should pass shared tests and the Android compile before the next file is moved.

## iOS Activation Checklist

- Use a Mac with the current Xcode command-line tools.
- Open the repository on macOS so Gradle configures `iosX64`, `iosArm64`, and `iosSimulatorArm64`.
- Open the existing `iosApp/iosApp.xcodeproj`; do not recreate shared screens in SwiftUI.
- Keep the existing Gradle/Xcode framework build phase that embeds `PlayerIDShared`.
- Add bundle ID, signing team, permission descriptions, entitlements, icons, and App Store metadata.
- Implement AVFoundation camera, AVKit playback, Vision OCR, Photos media selection, speech, contacts, and background-task adapters.
- Provide screenshot/photo import on iOS where Android currently uses unrestricted MediaProjection capture.

## Change Policy During Migration

- New business rules go into `commonMain`.
- Platform modules translate native input/output only.
- Shared UI consumes immutable state and emits events; it does not call native APIs directly.
- Database and API schema changes must have one source of truth.
- Do not maintain separate Android and Swift implementations of the same screen.
