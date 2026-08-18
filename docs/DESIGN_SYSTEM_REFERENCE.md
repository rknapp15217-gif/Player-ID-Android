# Spotr / Player-ID Design System Reference

## Overview

Player-ID uses **Material 3** with a custom Spotr theme tailored for youth sports applications. This document is the canonical reference for all design tokens, typography scales, and reusable components.

---

## Color Palette (`ui/theme/Color.kt`)

| Token | Hex | Usage |
|-------|-----|-------|
| `SpotrPrimaryBlue` | `#2563EB` | Primary buttons, links, active nav items |
| `SpotrSuccessGreen` | `#22C55E` | Success states, live detection indicators |
| `SpotrHighlightOrange` | `#F97316` | Warnings, highlights, jersey badges |
| `SpotrDarkSurface` | `#111827` | Dark-mode background |
| `SpotrLightBackground` | `#F5F6FA` | Light-mode background |
| `SpotrText` | `#1F2937` | Primary text on light backgrounds |
| `SpotrSurfaceLight` | `#FFFFFF` | Card / surface on light theme |
| `SpotrSurfaceDark` | `#0F172A` | Card / surface on dark theme |
| `SpotrOutline` | `#E5E7EB` | Borders / dividers on light theme |
| `SpotrOutlineDark` | `#273047` | Borders / dividers on dark theme |
| `ErrorRed` | `#EF4444` | Errors, delete actions |

### Team Colors (dynamic assignment)

```kotlin
val TeamColors = listOf(
    Color(0xFFEF4444), // Red
    Color(0xFF3B82F6), // Blue
    Color(0xFF22C55E), // Green
    Color(0xFFF59E0B), // Yellow
    Color(0xFF8B5CF6), // Purple
    Color(0xFFF97316), // Orange
    Color(0xFF14B8A6), // Teal
    Color(0xFFEC4899)  // Pink
)
```

---

## Typography (`ui/theme/Type.kt`)

Fonts: **Oswald** (headings) · **Inter** (body / labels)

| Style | Font | Weight | Size / Line Height |
|-------|------|--------|--------------------|
| `displayLarge` | Oswald | Bold | 40sp / 46sp |
| `headlineLarge` | Oswald | Bold | 32sp / 38sp |
| `headlineMedium` | Oswald | Bold | 28sp / 34sp |
| `titleLarge` | Inter | SemiBold | 22sp / 28sp |
| `titleMedium` | Inter | SemiBold | 18sp / 24sp |
| `bodyLarge` | Inter | Regular | 16sp / 24sp |
| `bodyMedium` | Inter | Regular | 14sp / 20sp |
| `labelLarge` | Inter | Medium | 13sp / 18sp |
| `labelSmall` | Inter | Medium | 11sp / 16sp |

---

## Reusable Components

### `SpotrCards` (`ui/components/SpotrCards.kt`)
- `PlayerCard` – displays player name, jersey number, position
- `TeamCard` – displays team name and color indicator

### `SpotrHeaders` (`ui/components/SpotrHeaders.kt`)
- `SpotrTopBar` – screen header with optional back button and actions

### `SpotrBottomNav` (`ui/components/SpotrBottomNav.kt`)
- 4-tab bottom navigation: Camera · Validate · My Team · Settings

---

## Design Rules

1. **Use theme tokens** — always reference `MaterialTheme.colorScheme.*` and `MaterialTheme.typography.*`, never hardcode hex values.
2. **Corner radii** — large cards: `16.dp`; dialogs: `28.dp`; chips/tags: `8.dp`.
3. **Prefer existing components** — before creating a new card or header, check if a Spotr component already exists.
4. **Padding** — screen edges: `16.dp`; card internal padding: `12–16.dp`.
5. **Scaffold** — all full screens use `Scaffold` with `TopAppBar`.
6. **Dark + Light** — all screens must work in both themes.
7. **State** — use `ViewModel` + `StateFlow` / `collectAsState()`.

---

## Navigation Routes

| Route | Screen |
|-------|--------|
| `camera` | CameraScreen |
| `validate` | JerseyValidationScreen |
| `team` | TeamScreen |
| `settings` | SettingsScreen |
| `design_explorer` | DesignSystemExplorerScreen |
| `video_library/{teamName}` | VideoLibraryScreen |
| `video_editor?videoUri={uri}` | VideoEditorScreen |
| `video_playback/{videoUri}` | VideoPlaybackScreen |
| `post_recording?videoUri={uri}` | PostRecordingScreen |
| `app_roster_import/{teamName}` | AppRosterImportScreen |
| `web_roster_import/{teamName}` | WebRosterImportDialog |
| `crowd_sourced_teams` | CrowdSourcedTeamsScreen |
