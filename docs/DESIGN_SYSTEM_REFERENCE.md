# Player-ID Design System Reference

## Spotr Components

### SpotrPlayerCard
- Purpose: player identity, team, position, academic year
- Usage: roster and team management lists

### SpotrActionCard
- Purpose: gradient action callouts
- Usage: feature entry points and quick actions

### SpotrStatsCard
- Purpose: compact metric display
- Usage: dashboards and summary rows

### SpotrScreenHeader
- Purpose: branded section/screen headers
- Usage: top-of-screen identity and context

### SpotrBottomNavigationBar
- Purpose: primary app navigation
- Usage: route switching between main areas

## Material 3 + Spotr Color Tokens

From `ui/theme/Color.kt` and `ui/theme/Theme.kt`:
- `SpotrPrimaryBlue`
- `SpotrSuccessGreen`
- `SpotrHighlightOrange`
- `SpotrDarkSurface`
- `SpotrLightBackground`
- `SpotrText`
- `SpotrSurfaceLight`
- `SpotrSurfaceDark`
- `SpotrOutline`
- `SpotrOutlineDark`
- `ErrorRed`

## Typography Guidelines

From `ui/theme/Type.kt`:
- **Inter**: body, labels, most readable text
- **Oswald**: display/headline emphasis

Recommended usage:
- Headlines: `headlineLarge`, `headlineMedium`
- Section titles: `titleLarge`, `titleMedium`
- Content: `bodyLarge`, `bodyMedium`
- Labels/chips: `labelLarge`, `labelSmall`

## Design Pattern Catalog

- Prefer `MaterialTheme.colorScheme` before hardcoded colors.
- Use Spotr components where existing behavior matches your UI intent.
- Keep composables small and focused; route/business logic stays outside UI blocks.
- Add accessibility labels (`contentDescription`) to actionable icons.
- Validate generated UI code with `DesignValidator` before implementation.
