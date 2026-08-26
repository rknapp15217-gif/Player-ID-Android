# PlayerID Shared

This is the Kotlin Multiplatform module for behavior that must remain identical on Android and iOS.

## Source Sets

- `commonMain`: product logic, models, state, and platform service contracts.
- `commonTest`: tests that define behavior for every platform.
- `androidMain`: Android implementations when shared interfaces begin replacing direct framework calls.
- `iosMain`: Apple implementations, compiled on macOS and by GitHub Actions.

`commonMain` also owns the first Compose Multiplatform UI assets: the product color tokens and reusable moment-tag and Goat vector icons. Their packages are unchanged, so Android consumes them directly from this module and iOS receives the same definitions in `PlayerIDShared.framework`.

The iOS targets are declared only when Gradle runs on macOS. This keeps Windows Android development operational while the GitHub `KMP Shared` workflow validates the Apple simulator framework.

## Rules

- Preserve existing packages when moving code so Android callers require minimal changes.
- Do not import Android or Apple frameworks from `commonMain`.
- Do not expose native URI, image, camera, player, or database objects through common APIs.
- Translate native results to shared models at the platform boundary.
- Add or move tests to `commonTest` before changing behavior.
- Keep Compose UI free of Android resources and `androidx.compose.ui.platform` APIs.

## Local Validation

```powershell
.\gradlew.bat :shared:testDebugUnitTest :shared:checkCommonMainBoundaries
.\gradlew.bat :app:compileDebugKotlin
```

## macOS Validation

```bash
sh gradlew :shared:iosSimulatorArm64Test
sh gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

See `../KMP_MIGRATION.md` for the migration sequence and iOS activation checklist.
