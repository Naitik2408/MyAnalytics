# Life Analytics — Design Implementation Notes (v1.2 Redesign)

This document summarizes the visual-layer implementation of [docs/03_UI_UX_GUIDELINES.md](file:///home/antigravity/Naitik2408/MyAnalytics/docs/03_UI_UX_GUIDELINES.md) (v1.2).

---

## 1. Token & Theme Architecture

| File | Responsibility |
| :--- | :--- |
| [Color.kt](file:///home/antigravity/Naitik2408/MyAnalytics/app/src/main/java/com/lifeanalytics/app/core/designsystem/Color.kt) | Implements `LaColorScheme`, `LaTints`, `TintSteps(strong, base, soft)` for `lavender`, `mint`, `blue`, `rose`, and `butter` in both Light (`LightLaColors`) and Dark (`DarkLaColors`) themes, plus `ModuleTint` mapping (`TASKS`, `FOCUS`, `HABITS`, `FINANCE`, `REVIEW`, `INSIGHTS`). |
| [Theme.kt](file:///home/antigravity/Naitik2408/MyAnalytics/app/src/main/java/com/lifeanalytics/app/core/designsystem/Theme.kt) | Bundles the static **Urbanist** typeface (`400`, `500`, `600`, `700`) with system sans-serif fallback, defines `LaTypeScale` (including `tnum` tabular numerals on `metricValue`, `timerValue`, and `chartMicroLabel`, plus `metricEmptyValue` for text-state tile values), `LaSpacing` (4 dp base scale + `dockBottomClearance`), `LaRadii` (`16 / 24 / 32 / 40 dp` + `concentricInner`), and sets **Light theme (`AppThemeOption.LIGHT`)** as the app default. |
| [app/OFL_Urbanist.txt](file:///home/antigravity/Naitik2408/MyAnalytics/app/OFL_Urbanist.txt) | Official SIL Open Font License 1.1 for the bundled Urbanist font files in `app/src/main/res/font/`. |

---

## 2. Core Reusable Component Inventory

Implemented in [Components.kt](file:///home/antigravity/Naitik2408/MyAnalytics/app/src/main/java/com/lifeanalytics/app/core/designsystem/Components.kt):

- **Buttons & Utility Affordances:**
  - `LaPrimaryButton`: Full-pill primary action button (`LaTheme.colors.ink` fill, `LaTheme.colors.onInk` label/icon, `54.dp` min height, single-line overflow protection, `0.98f` press scale).
  - `LaSecondaryButton`: Full-pill tonal secondary action button (`surfaceVariant` or `tint.soft` fill, `52.dp` min height, single-line label).
  - `LaTextButton`: Quiet pill text action (`48.dp` min touch target).
  - `LaCircularIconButton`: Circular `46.dp` utility button (`surfaceVariant` fill) used for Settings, Back, Date steppers, and row actions (never bare floating icons).
  - `LaArrowButton`: Circular `38.dp` `↗` affordance (`Icons.Outlined.NorthEast`) on interactive hero/bento tiles.
  - `LaIconChip`: Circular `38.dp` icon container (`85%` white on tinted tiles in light mode, `surface.copy(alpha = 0.72f)` in dark mode).
- **Panels & Bento Tiles (Borderless Tonal Separation):**
  - `LaHeroPanel`: `32.dp` corner radius, `20.dp` padding, strong module tint fill, header row with `LaIconChip`, title, subtitle, and optional `LaArrowButton`.
  - `LaContainerPanel`: `24.dp` corner radius, `16.dp` padding, `surface` fill, grouping list rows or charts as a single unified sheet instead of separate bordered cards.
  - `LaStatTile`: `24.dp` corner radius, `16.dp` padding, automatic detection of numeric vs text-state values (`metricValue` `30.sp` tabular vs `metricEmptyValue` `16.sp` medium so text states like `"No habits scheduled"` never wrap into giant bold text), plus optional `microVisual` slot.
  - `LaTintedTile`: Flexible borderless pastel surface container.
- **Navigation, Filters & Inputs:**
  - `LaFloatingDock`: Floating pill navigation bar (`68.dp` height, `20.dp` horizontal inset, `14.dp` bottom margin + navigation bar inset, soft ink shadow, `color.ink` active pill).
  - `LaSegmentedControl`: Pill track with sliding/animated `color.ink` active segment (`Monthly`/`Daily`, `Daily`/`Weekly`/`Monthly`, and theme selector).
  - `LaDateRangePill`: Compact pill date/month/range navigator with circular prev/next buttons, optional calendar picker trigger, and quick `"Today"` reset pill.
  - `LaFilterChip`: Pill filter chip (`40.dp` min height, `tint.lavender.strong` selected fill).
  - `LaAnimatedCheckbox`: Custom `24.dp` rounded-square (`8.dp` radius) checkbox inside a `48.dp` touch target with spring scale pulse.
  - `LaRowDivider`: Subtle `1.dp` horizontal divider inside unified `LaContainerPanel` lists.
  - `LaTextField`: Borderless filled input (`16.dp` radius, `56.dp` min height, `accent` focus ring).
  - `LaScreenHeader`: Sub-screen / primary header with circular back button, `screenTitle` (`21.sp`), subtitle, and trailing action slot.
  - `EmptyStateCard`: Soft tinted block with 5 ghost capsule bars (`10%` ink opacity), neutral title/explanation, and optional tonal button.

---

## 3. Custom Canvas Chart Components

Implemented in [Charts.kt](file:///home/antigravity/Naitik2408/MyAnalytics/app/src/main/java/com/lifeanalytics/app/core/designsystem/Charts.kt):

1. `LaPillBarChart`:
   - Full-radius vertical capsules (`width = 66%` of slot clamped to `22..52.dp`, `cornerRadius = width / 2`).
   - **Zero-baseline rule:** `0` values draw **no filled bar** (only the soft capsule track is visible). Small non-zero values clamp to at least one bar-width high so they remain true capsules.
   - **45° diagonal hatch pattern (`drawHatchedCapsule`):** Clipped to the capsule path (`1.5.dp` stroke, `6.dp` spacing, `16%` ink opacity) for non-selected bars or comparison series.
   - **Selected / Highlighted Bar:** Solid `LaTheme.colors.ink` with a direct value pill above the bar.
   - **Ghost Empty State:** 7 ghost capsules at `10%` ink opacity with centered neutral message when all values are zero.
2. `LaHorizontalTrackBar`:
   - Horizontal "fill inside a pill" (`14.dp` height, `tint.soft` track, `tint.strong` or `ink` fill) for habit consistency and planned-vs-completed progress.
3. `LaRingChart`:
   - Thick rounded-cap donut (`28.dp` stroke) with `color.ink` primary slice + module tint strong slices, **white circular rank badges (`1`, `2`, `3`, `4`)** overlaid on segments, side legend with amounts and percentages, and a ghost ring empty state.
4. `LaMiniHeatCalendar`:
   - 7-column grid of `11.dp`-radius cells with 5 intensity steps (`surfaceVariant` -> `tint.soft` -> `tint.base` -> `tint.strong` -> `ink`) and a `Less .. More` legend.
5. `LaSessionTimeline`:
   - 24-hour horizontal capsule ruler (`00:00` to `24:00`) plotting completed focus intervals as `tint.mint.strong` / `ink` pills with paused gaps excluded, plus a ghost empty state.
6. `LaGoalGauge`:
   - 270° rounded-cap arc gauge with end knob dot and centered tabular percentage/ratio.

---

## 4. Screen Rebuilds & Window Inset Architecture

- **Single-Source Window Insets ([LifeAnalyticsApp.kt](file:///home/antigravity/Naitik2408/MyAnalytics/app/src/main/java/com/lifeanalytics/app/app/LifeAnalyticsApp.kt)):**
  - Removed nested `Scaffold(topBar = TopAppBar(...))` from individual screens. `statusBarsPadding()` is applied once at the root container in `LifeAnalyticsApp`, and every screen `LazyColumn` applies `LaTheme.spacing.dockBottomClearance` (`104.dp`) so content scrolls cleanly above `LaFloatingDock`.
- **Home ([HomeDashboardScreen.kt](file:///home/antigravity/Naitik2408/MyAnalytics/app/src/main/java/com/lifeanalytics/app/feature/home/HomeDashboardScreen.kt)):**
  - White header sheet (`32.dp` bottom corners) with greeting + date, circular review & settings buttons, two-line editorial headline (`"Your daily\nrhythm, clearly."`), and 7-day Week Strip with activity check badges.
  - Lavender Hero Panel with weekly focus `LaPillBarChart`, asymmetric Bento Grid (tall Tasks tile with `LaGoalGauge` on left, stacked Focus + Expense tiles on right, full-width Habits tile with `LaHorizontalTrackBar`), single-line Quick Actions (`Start focus`, `Add task`, `Expense`), and unified `Today's Tasks` container panel.
- **Finance ([FinanceScreen.kt](file:///home/antigravity/Naitik2408/MyAnalytics/app/src/main/java/com/lifeanalytics/app/feature/finance/FinanceScreen.kt)):**
  - Compact header with single primary `"Add entry"` ink button, `Monthly`/`Daily` segmented control + month/day pill control, `tint.blue.strong` Net Hero Panel + paired Income/Expense tiles, Spending Trend `LaPillBarChart`, Category `LaRingChart`, and unified Transactions `LaContainerPanel` with `"Transactions (N)"` on its own row above horizontal `LaFilterChip`s.
- **Activity ([ActivityHubScreen.kt](file:///home/antigravity/Naitik2408/MyAnalytics/app/src/main/java/com/lifeanalytics/app/feature/activity/ActivityHubScreen.kt)):**
  - Composed module-tinted Bento Grid (`Focus Timer` largest tile in `tint.mint.base`, 2-column `Daily Planning` in `tint.lavender.base` + `Daily Habits` in `tint.butter.base`, full-width `Daily Review` in `tint.rose.base`), neutral status dots for non-completed states (checkmarks reserved strictly for completed states), and `Today's Focus Timeline` (`LaSessionTimeline`).
- **Insights ([InsightsScreen.kt](file:///home/antigravity/Naitik2408/MyAnalytics/app/src/main/java/com/lifeanalytics/app/feature/insights/InsightsScreen.kt)):**
  - Compact header combining `Daily`/`Weekly`/`Monthly` `LaSegmentedControl` + `LaDateRangePill` with `"Today"`, `"This week"`, and `"This month"` badges, Lavender Hero Trend `LaPillBarChart`, supporting Bento tiles (`LaGoalGauge` on Tasks), and `LaContainerPanel`s with `LaPillBarChart`, `LaMiniHeatCalendar`, `LaHorizontalTrackBar`, and `LaRingChart`.
- **Sub-screens ([DailyPlanningScreen.kt](file:///home/antigravity/Naitik2408/MyAnalytics/app/src/main/java/com/lifeanalytics/app/feature/planning/DailyPlanningScreen.kt), [FocusScreen.kt](file:///home/antigravity/Naitik2408/MyAnalytics/app/src/main/java/com/lifeanalytics/app/feature/focus/FocusScreen.kt), [DailyHabitsScreen.kt](file:///home/antigravity/Naitik2408/MyAnalytics/app/src/main/java/com/lifeanalytics/app/feature/habits/DailyHabitsScreen.kt), [DailyReviewScreen.kt](file:///home/antigravity/Naitik2408/MyAnalytics/app/src/main/java/com/lifeanalytics/app/feature/review/DailyReviewScreen.kt), [SettingsScreen.kt](file:///home/antigravity/Naitik2408/MyAnalytics/app/src/main/java/com/lifeanalytics/app/feature/settings/SettingsScreen.kt)):**
  - Rebuilt with `LaScreenHeader`, module-tinted hero/bento summaries, unified `LaContainerPanel` lists, and themed dialogs (`LaTextField`, `LaFilterChip`, `LaPrimaryButton`, `LaSecondaryButton`).

---

## 5. Decisions Made Where Guidelines Were Silent & Items Deferred

- **Font Licensing File Location:** Android's AAPT2 resource compiler only allows font files (`.ttf`, `.otf`, `.ttc`, `.xml`) inside `res/font/`. The OFL license text was therefore placed at [app/OFL_Urbanist.txt](file:///home/antigravity/Naitik2408/MyAnalytics/app/OFL_Urbanist.txt) while the four static `.ttf` weights reside in `app/src/main/res/font/`.
- **DataStore Theme Migration Key:** To ensure upgrades from earlier builds default to Light mode (`AppThemeOption.LIGHT`) per Section 3.1 & Section 4 while still persisting explicit user choices, the preference key was versioned to `theme_option_v12` in [PreferencesRepository.kt](file:///home/antigravity/Naitik2408/MyAnalytics/app/src/main/java/com/lifeanalytics/app/data/repository/PreferencesRepository.kt).
- **Deferred Items:** None. All tokens, components, custom canvas charts, empty/ghost states, and screen layouts specified in v1.2 are implemented.
