# Life Analytics — UI/UX Guidelines

**Document:** `03_UI_UX_GUIDELINES.md`
**Product:** Life Analytics
**Version:** 1.2 (V1)
**Status:** Visual direction made specific and measurable — approved reference language
**Platform:** Android
**Design direction:** Premium light-mode-first habit-and-health-tracker visual language adapted for Life Analytics: soft lavender-led pastel surfaces, ink-black emphasis, pill-shaped data visualisation, large soft corners, a bento-style layout, and a floating navigation dock.

---

## 0. How to use this document (humans and AI coding tools)

This document is the single source of truth for how the app looks. If you are building UI from it, follow these rules first:

1. **Never hard-code colours, sizes, radii, or fonts inside a screen.** Everything comes from the tokens in sections 4–6.
2. **Do not ship default component-library styling** (default Material cards, default bar charts, default bottom bar, default switches). Every component must be themed to this document. Default styling is the fastest way to make an app look generated.
3. **If this document does not specify something, extend the existing pattern**; do not invent a new look. If a new pattern is truly needed, add it here first.
4. **Use the Signature Details (section 3.4) on every screen.** They are what make the app look designed rather than assembled.
5. **Check the Anti-patterns (section 3.5) before finishing a screen.**

---

## 1. Purpose

This document defines the visual language and interaction principles for Life Analytics V1. It is the shared design reference for all screens and reusable components.

The UI must help the user record daily activity quickly and understand personal patterns without making the app feel complicated or judgmental.

This document defines design rules, not final pixel-perfect screen layouts. Individual screen documents may specify exact content and screen-specific behaviour while following these guidelines.

## 2. Product design principles

1. **Minimum input, maximum meaningful output:** avoid asking the user to enter information that the app can calculate.
2. **Clarity before decoration:** visual hierarchy and readable information are more important than ornamental elements.
3. **Consistent interactions:** similar actions must look and behave similarly across modules.
4. **Fast daily use:** common actions such as adding a task, recording an expense, checking a habit, and starting focus must be easy to reach.
5. **Honest analytics:** charts and summaries must represent actual saved records and clearly communicate missing data.
6. **Calm, non-judgmental tone:** the app should help the user observe behaviour, not shame them for missed tasks or habits.
7. **Accessible by default:** text, controls, contrast, and touch targets must work for a broad range of users.
8. **Privacy-aware design:** show only the information needed for the current task and explain permission requests.

## 3. Visual direction

### 3.1 Approved visual reference and product adaptation

The approved visual direction is the shared **Health & Habit Tracker** reference. It is a source of **visual treatment only**. Do not copy its content, features, copywriting, photography, illustrations, logo, or branding. Do not blend in earlier dark-dashboard, finance-app, editorial, or unrelated concepts.

The reference's character, in one sentence: **a calm, airy, pastel interface where large soft shapes carry the data, and a single ink-black accent tells you what matters.**

The eight ideas to carry over:

1. **Soft canvas, white sheets, pastel tiles.** A very light lavender-grey canvas; white surfaces for content; tinted pastel surfaces (lavender, mint, blue, rose, butter) for distinct groups.
2. **Ink-black as the action and emphasis colour.** The primary button, the selected chart element, and the active navigation item are near-black. Lavender is the *surface* identity, not the button colour.
3. **Large, soft corners.** Cards and panels are visibly rounder than a standard Material card (see section 6).
4. **Pill-shaped data.** Bars are full-radius capsules, not rectangles with rounded tops. Charts feel like objects, not graphs.
5. **Hatched vs solid.** Non-selected data uses a fine diagonal hatch or soft tint; the selected item is solid ink. This is the signature chart treatment.
6. **Bento composition.** One hero panel, then a two-column grid of tiles of different heights, each with its own mini-visualisation.
7. **Quiet labels, loud numbers.** Labels are small and light; the value is the biggest thing in the tile.
8. **A floating navigation dock** instead of an edge-to-edge bottom bar.

### 3.2 Information density and composition

- Organise every screen as: header → (optional editorial headline) → primary summary → important actions → supporting tiles.
- Use a consistent grid: 2 columns, 12 dp gutter, 20 dp screen inset (section 6).
- Prefer **one hero panel plus a bento grid** over a long stack of identical full-width cards.
- Tiles in a grid should differ in height where content differs (a tall chart tile beside two short stat tiles). Identical equal-sized tiles in a row of four look templated.
- Lists (tasks, transactions, sessions) live **inside one container**; individual rows are not separate cards.
- Use whitespace between sections, not excessive padding inside components.
- Do not force a calendar, illustration, chart, or card onto a screen unless it supports the actual feature and real data.
- Keep the four primary destinations: Home, Finance, Activity, and Insights.

### 3.3 Avoid

- Mixing unrelated design directions or introducing a new visual identity on individual screens.
- Random pastel colours (each tint must have a meaning, see section 4.4), excessive gradients, glows, heavy shadows, or decorative backgrounds.
- Inconsistent radii and padding between tiles.
- Awkward text wrapping, clipped labels, and isolated single-word line breaks. (A deliberate two-line editorial headline is fine.)
- Charts that dominate the page without adding insight, or charts that use fabricated data.
- Generic placeholder illustrations repeated across unrelated modules.
- Redesigning product workflows, navigation, data models, calculations, or feature scope as part of visual work.

### 3.4 Signature details (required on every screen)

These small, repeatable details are what separate a designed app from a generated one. Every screen must use the ones that apply.

| # | Detail | Rule |
|---|---|---|
| 1 | **Ink moment** | Ink (`color.ink`) is used for the primary button, the selected data element, and the active nav item. No more than three ink elements are visible on one screen. |
| 2 | **Pill data** | All bars are full-radius capsules (section 11). |
| 3 | **Hatch for secondary data** | Non-selected or planned/secondary data uses the hatch pattern or a soft tint; selected or actual data is solid. |
| 4 | **Icon chips** | Icons sit inside 36–40 dp circular chips (white on tinted tiles, `surfaceVariant` on white cards), never floating bare beside text. |
| 5 | **Circular utility buttons** | Header actions and back buttons are 44–48 dp circles on `surfaceVariant`. |
| 6 | **Arrow affordance** | Tiles that open a detail screen carry a small circular `↗` button at top-right. |
| 7 | **Concentric radii** | Inner radius = outer radius − padding between them. A rounded element inside a rounded card must follow this or it looks off. |
| 8 | **Layered sheets** | On Home, the header and week strip sit on a white sheet with large bottom corners over the lavender-grey canvas. |
| 9 | **Floating dock** | Bottom navigation floats above content as a rounded dock (section 8). |
| 10 | **Big number, small label** | Tile pattern: icon chip → small label → large value (section 9.2). |
| 11 | **Number badges** | Ranked or counted items on a ring chart use small round white badges on the segment. |
| 12 | **Soft tinted shadow** | The only shadows are the dock and bottom sheets, tinted with ink at low opacity, never grey-black. |

### 3.5 Anti-patterns (what makes an app look AI-generated)

Do not ship any of the following:

- Default Material cards: 12 dp radius, 1 px grey border, 16 dp padding, repeated identically everywhere.
- A saturated purple filled primary button (for example `#7563D8` as a button fill). Use ink.
- Inter + a single purple + white cards as the entire identity. Use the specified typeface and tinted surfaces.
- Rectangle bars with small top radii, or default chart-library styling and gridlines.
- A purple-to-pink gradient hero, glows, or glassmorphism in the app screens.
- Emoji used as icons. (A mood emoji as *user-recorded data* is allowed.)
- Everything centred, every tile the same size, every section a card.
- A drop shadow on every card.
- Labels below 11 sp, or all text the same weight.
- Fake or lorem data in real analytics (also a product rule, see section 14).
- Using every pastel on one screen.

## 4. Colour system

Implement the palette as **semantic design tokens**. Never hard-code hex values in screens.

### 4.1 Light theme — core tokens

| Token | Value | Usage |
|---|---|---|
| `color.canvas` | `#F4F3F9` | App background behind sheets and tiles |
| `color.surface` | `#FFFFFF` | Sheets, white tiles, dialogs |
| `color.surfaceVariant` | `#EFEDF6` | Circular utility buttons, inputs, inactive chips, tracks |
| `color.ink` | `#111A1C` | Primary button fill, selected chart element, active nav, headline text |
| `color.onInk` | `#FFFFFF` | Text and icons on ink |
| `color.textPrimary` | `#111A1C` | Main text (same hue as ink) |
| `color.textSecondary` | `#777386` | Supporting text **on white or canvas only** |
| `color.onTintSecondary` | `#4F4B63` | Supporting text **on tinted surfaces** (secondary grey fails contrast on pastel) |
| `color.accent` | `#7563D8` | Focus rings, links, selected-chip text, small highlights. **Not** a button fill. |
| `color.outline` | `#E2DEED` | Dividers and input focus borders (used sparingly, see 9.2) |
| `color.success` | `#23865A` | Completed/success states |
| `color.warning` | `#B7791F` | Caution and pending states |
| `color.error` | `#C44752` | Errors and destructive actions |
| `color.income` | `#23865A` | Finance income |
| `color.expense` | `#C44752` | Finance expense |

### 4.2 Light theme — tint tokens (pastel surfaces)

Each tint has three steps: **strong** (hero panels, chart fills), **base** (tiles), **soft** (tracks, empty bars, backgrounds inside a tile).

| Tint | Strong | Base | Soft |
|---|---|---|---|
| `tint.lavender` | `#D3C1F3` | `#DDD0F7` | `#EFEAFC` |
| `tint.mint` | `#C4E4D2` | `#D5EBDF` | `#EAF5EF` |
| `tint.blue` | `#C9DEF4` | `#D9E9F8` | `#ECF3FB` |
| `tint.rose` | `#EBCFE0` | `#F0DDE8` | `#F8EEF3` |
| `tint.butter` | `#FFE6A8` | `#FFEFC4` | `#FFF8E3` |

Values are starting points; tune on device, but keep the soft, low-chroma character.

### 4.3 Dark theme

| Token | Value | Usage |
|---|---|---|
| `color.canvas` | `#12131B` | App background |
| `color.surface` | `#1C1E29` | Sheets, tiles, dialogs |
| `color.surfaceVariant` | `#272A38` | Utility buttons, inputs, tracks |
| `color.ink` | `#EDE7FF` | **Inverted:** primary button fill, selected chart element, active nav |
| `color.onInk` | `#1A1630` | Text and icons on the inverted ink |
| `color.textPrimary` | `#F2F2FA` | Main text |
| `color.textSecondary` | `#B1B3C2` | Supporting text |
| `color.onTintSecondary` | `#C9C5DD` | Supporting text on dark tints |
| `color.accent` | `#A69BFF` | Focus rings, links, small highlights |
| `color.outline` | `#3B3E4D` | Dividers |
| `color.success` | `#63C995` | Completed/success |
| `color.warning` | `#E5B45E` | Caution |
| `color.error` | `#F07A84` | Errors |
| `color.income` | `#63C995` | Finance income |
| `color.expense` | `#F07A84` | Finance expense |

Dark tints (deep, low-glare versions; text on them uses `textPrimary`):

| Tint | Strong | Base | Soft |
|---|---|---|---|
| `tint.lavender` | `#4A3F7E` | `#3A3260` | `#2B2548` |
| `tint.mint` | `#2F5A4A` | `#24453A` | `#1B332B` |
| `tint.blue` | `#2F4C70` | `#243B57` | `#1B2D43` |
| `tint.rose` | `#69405A` | `#503046` | `#3B2434` |
| `tint.butter` | `#6A5726` | `#51431E` | `#3B3116` |

Do not assume matching hex values across themes produce equivalent contrast. Verify every text/surface pair in both themes.

### 4.4 Tint meaning map (so colour is never random)

| Module / data | Tint |
|---|---|
| Tasks and Daily Planning | `tint.lavender` (the identity colour, also used for hero panels) |
| Focus and Active Work | `tint.mint` |
| Daily Habits | `tint.butter` |
| Finance | `tint.blue` |
| Daily Review and reflection | `tint.rose` |
| Insights | Uses each module's own tint for its data; hero panel is lavender |

Rules:

- A screen uses **one dominant tint plus at most two supporting tints**.
- Finance keeps semantic income/expense colours for amounts and chart series that mean income or expense, always with a text or icon label as well.
- Use success and error colours only when their meaning is clear.
- Do not use red merely to indicate an ordinary missed task or incomplete habit.
- Completed states must include a checkmark, label, or other non-colour cue.
- Charts must use a consistent series palette and a visible legend when multiple series exist.

### 4.5 Colour usage rules

- Lavender is the signature **surface** colour. Ink is the **action and emphasis** colour. Do not fill every component with colour.
- Keep the canvas and most surfaces light and neutral; reserve tinted surfaces for the hero panel and distinct data groups.
- Text on tinted surfaces uses `textPrimary` or `onTintSecondary`, never `textSecondary`.

## 5. Typography

**Typeface:** Urbanist (open font licence), bundled, weights 400, 500, 600, 700. Its geometric, slightly rounded forms match the soft shapes. Outfit or Plus Jakarta Sans are acceptable alternatives; choose one and use it everywhere. Fall back to the system sans-serif if the font fails to load. Avoid defaulting to Inter, since it reads as a generic default.

Confirm the chosen font supports **tabular figures**. If it does not, use a tabular-figure font for timers and financial amounts.

| Style | Size / line height | Weight | Usage |
|---|---|---|---|
| Headline (Home only, optional) | 34–38 sp / 1.05–1.1 | Bold (700), tracking −1% | Editorial two-line headline on Home |
| Screen title | 20–22 sp | Semibold (600) | Title on sub-screens, next to the back button |
| Metric value | 28–32 sp | Semibold/Bold | The big number in a tile |
| Timer value | 40–56 sp | Semibold | Active focus timer, tabular figures |
| Section title | 16–18 sp | Semibold | Section headings ("Today", "This week") |
| Body | 14–16 sp | Regular | Main content and descriptions |
| Label | 13–14 sp | Regular/Medium | Tile labels, row metadata (secondary colour) |
| Chart micro-label | 11–12 sp | Medium | Chart day labels and value labels |

Typography rules:

- **Hierarchy comes from contrast:** big, bold numbers; small, light labels. Do not make every text the same weight.
- Use `sp` so Android font scaling is respected; test at 130% and 150% scale.
- 11 sp is the floor. Never go smaller.
- Use tabular numerals for timers, durations, and amounts.
- **Number and currency formatting (Rule 7):** One formatter used everywhere. Rupee symbol `₹` only. Indian digit grouping (`₹12,34,567`). No `$` anywhere. Negative amounts use a minus sign and the expense colour plus a text/icon cue. Tabular numerals for all amounts and timers.
- Do not communicate hierarchy using font size alone; also use weight and colour.
- **Header-headline rule (Rule 9):** The editorial headline is a Home-only moment (one per app) and must be 1–2 lines maximum. Combined with the greeting and week strip, the header sheet must never push the first interactive card below 300 dp from the top of the screen. Other screens use the screen title style.

## 6. Spacing, sizing, and shape

### 6.1 Spacing (4 dp base)

| Token | Value |
|---|---|
| `space.1` | 4 dp |
| `space.2` | 8 dp |
| `space.3` | 12 dp |
| `space.4` | 16 dp |
| `space.5` | 20 dp |
| `space.6` | 24 dp |
| `space.7` | 28 dp |
| `space.8` | 32 dp |
| `space.10` | 40 dp |

- Screen horizontal inset: **20 dp** (16 dp on narrow phones).
- Gutter between tiles (horizontal and vertical): **12 dp**.
- Padding inside tiles: **16 dp**; inside hero panels: **20 dp**.
- Between major sections: **24–28 dp**.
- Between related controls: 8–12 dp.

### 6.2 Corner radius (larger than a standard Material card, on purpose)

| Token | Value | Usage |
|---|---|---|
| `radius.sm` | 16 dp | Inputs, small chips' containers, heat-map cells (10–12 dp) |
| `radius.md` | 24 dp | Standard tiles and list containers |
| `radius.lg` | 32 dp | Hero panels, the white header sheet's bottom corners |
| `radius.xl` | 40 dp | Top corners of bottom sheets |
| `radius.full` | pill | Buttons, chips, bars, icon chips, progress tracks, the dock |

- **Concentric rule:** inner radius = outer radius − padding. A 16 dp-padded tile of 24 dp radius holds inner shapes of about 8 dp, or full-pill shapes.
- If the framework lacks continuous (squircle) corners, use plain rounded corners with these values; do not fake smoothing with hacks.
- Keep card heights content-driven; avoid fixed heights that clip larger text.

### 6.3 Elevation

- Use **tonal separation** first: white tiles on `canvas`, tinted tiles on `canvas` or white. No borders on tiles.
- The only shadows: the navigation dock and bottom sheets. Style: offset y 8 dp, blur 24 dp, colour `ink` at about 10–12% opacity.
- Dividers only when spacing or tonal difference is insufficient, and only inside lists.

## 7. Layout and screen structure

### 7.1 General screen structure

1. **Header:** on Home, a greeting row (avatar or neutral person icon + greeting + date) with two circular utility buttons on the right. On sub-screens, a circular back button plus the screen title.
2. **Scrollable content area** with the bento composition.
3. **Floating navigation dock**, with enough bottom padding in the content (dock height + margin + system inset) so nothing is hidden behind it.
4. Safe-area-aware layout: no overlap with system bars, keyboard, or gesture navigation.

### 7.2 Home screen anatomy and composition rule (Rules 1, 2, 6, 8, 9)

- **Home composition rule (Rule 1):** On a 360 × 800 dp screen, the first viewport must show: header sheet (≤ 230 dp including the week strip), hero panel (≤ 200 dp), and the primary quick-action row. "Start focus" must be reachable without scrolling.
- **Bento rule (Rule 2):** Summary metrics are a **2 × 2 grid** of tiles (≈ 140–160 dp tall), not full-width stacks. Do not make a full-width tile for a single small number.

1. White **header sheet** with large bottom corners (`radius.lg`, ≤ 230 dp total height): greeting row, compact 1–2 line editorial headline, and the **week strip** so the first interactive card starts well above 300 dp from top.
2. **Week strip:** a row of day tiles (about 46–52 × 60–64 dp, `radius.md`). Each tile shows the day name and date; today is filled with a tint and others show a soft tint. Each tile may carry a small state mark for that day (a check, a dot, or a user-recorded mood) when real data exists. Day tiles with no data stay soft and unmarked.
3. **Hero panel** (`tint.lavender` strong, `radius.lg`, ≤ 200 dp height): title with icon and arrow button, and one pill-bar chart of real saved data for the week, with the selected day in ink.
4. **Quick actions** ("Start focus" primary ink button, plus "Add task" and "Add expense" equal-width tonal buttons) immediately reachable in the first viewport without scrolling.
5. **Bento 2 × 2 grid** of the four summary metric tiles (Tasks, Focus, Expenses, Habits, ≈ 140–160 dp tall each).
6. Compact previews of today's tasks and scheduled habits inside list containers.

### 7.3 Responsive behaviour

- Design for Android phone widths first.
- Support small and large phones without horizontal scrolling for ordinary screens.
- Tiles may switch from a 2-column grid to a stacked layout on narrow widths or large font scales.
- Respect display cutouts, system insets, and keyboard visibility.
- Do not rely on a single fixed screen size.

## 8. Navigation guidelines

Primary destinations:

| Destination | Purpose |
|---|---|
| Home | Daily overview and quick actions |
| Finance | Income, expenses, and finance summaries |
| Activity | Daily Planning, Active Work, and session history |
| Insights | Daily, weekly, and monthly analytics |

### 8.1 Floating dock specification and Dock rule (Rule 3)

- **Dock rule (Rule 3):** The dock must be visibly separated from content: soft ink-tinted shadow (y 8 dp, blur 24 dp, ≈ 10–12% opacity), plus a **scrim**: a 32 dp vertical fade from `canvas` (0%) to `canvas` (≈ 90%) directly above the dock, so cards never show through its edges. Every scrollable screen has bottom padding = dock height + margin + system inset, so the last element, and any CTA, is fully reachable and never hidden.
- A **floating pill** (`radius.full`), `color.surface`, horizontally inset 20 dp from the screen edges, floating 12–16 dp above the bottom system inset, with the soft tinted shadow from 6.3.
- Height about 68 dp. Four items, evenly spaced, each at least 48 dp in both dimensions.
- **Active item:** an ink pill (or rounded rectangle, radius about 22 dp) behind the icon and label, with `onInk` icon and label.
- **Inactive items:** outline icon in `textPrimary` at about 70% opacity with a 11–12 sp label in `textSecondary`.
- Every item keeps **both icon and label** for clarity and accessibility.
- Selected-state change animates over 200 ms (section 13).

### 8.2 Rules

- Clearly indicate the selected destination (ink + label).
- Preserve a user's place in a section when practical.
- Do not add every module as a bottom navigation item.
- Daily Habits is reachable from Home and its dedicated route.
- Daily Review and Settings have clear, discoverable entry points.
- Use Android back behaviour consistently; avoid trapping the user in nested screens.
- Hide or compress the dock when the keyboard is open or during an active focus session if it competes with the timer controls.

## 9. Reusable component guidelines

Create reusable components so the app stays consistent. Names are conceptual.

### 9.1 Buttons

- **Primary:** `color.ink` fill, `onInk` label, `radius.full`, height 52–56 dp, 15–16 sp Semibold label. Full width for the main action of a screen or sheet. One dominant primary action per screen where practical.
- **Secondary:** tonal (`surfaceVariant` or a tint base) fill with `textPrimary` label, same shape. Or outlined with `color.outline`.
- **Text button:** low-emphasis actions such as Cancel, View all, Edit; `color.accent` or `textPrimary`.
- **Icon button:** 44–48 dp circle on `surfaceVariant`, 20–24 dp outline icon. Must have an accessible description.
- **Arrow button (tile affordance):** 36–40 dp circle with a `↗` icon, translucent white on tinted tiles or `surfaceVariant` on white.
- States: enabled, pressed (about 8% darker/lighter plus a short scale to 0.98), disabled, loading. Prevent duplicate submissions while saving.

### 9.2 Tiles and panels (Rules 6 and 8)

Use panels to group related information, not to wrap every text element.

- **Stat tile pattern:** top row = icon chip (left) and an optional secondary action such as `+` or `↗` (right); below it a small label (13–14 sp, secondary); then a large value (28–32 sp). Optional micro-visual at the bottom.
- **Empty-value style (Rule 6):** Tiles with no data use a smaller medium-weight value style (colour `onTintSecondary`) so tile heights and baselines stay equal. Counts, durations and amounts that are truly zero show `0`, `0m`, `₹0`. A ratio with zero denominator ("0 / 0") is not shown; show `0` plus "No tasks planned" (or "No habits scheduled"). When the denominator is above zero, show the ratio (`2 / 5`) with a micro progress bar.
- **Micro-visual rule (Rule 8):** When data exists, each summary tile carries one quiet micro-visual at its bottom:
  - Tasks → thin progress bar.
  - Focus → 5–7 mini pills.
  - Expenses → mini pills or thin bar.
  - Habits → row of dots.
- **Hero panel:** `tint.lavender` strong, `radius.lg`, 20 dp padding. Title row (icon + label, arrow button), then one chart, then day/period labels.
- **Container panel:** white, `radius.md`, holds lists and charts that need a title; title 16–18 sp Semibold at top-left with an optional action at top-right.
- No borders on tiles; use tonal separation. No nested cards deeper than one level.
- Keep titles and actions in predictable positions across modules.

### 9.3 Inputs

- Every input has a persistent label; placeholder text is not a replacement.
- Style: `surfaceVariant` fill, `radius.sm`, height at least 56 dp, no border until focused; focus shows a 2 dp `color.accent` border.
- Show required/optional status where it is not obvious.
- Use the correct Android keyboard type for numbers, amounts, dates, and text.
- Show validation feedback close to the field; preserve entered values on failure.
- Provide clear focus, error, disabled, and read-only states.
- Do not clear a form unexpectedly when the user navigates back.

### 9.4 Lists and rows

- Consistent row structure for tasks, habits, transactions, and focus sessions: leading icon chip or checkbox, primary label (strongest), secondary metadata line, trailing value or action.
- Rows live inside one container panel, separated by spacing or a subtle divider, not individual cards.
- Make the whole row tappable only when that behaviour is clear.
- Keep checkboxes and row actions separated enough to avoid accidental activation.
- Provide useful empty states.

### 9.5 Chips and filters

- Filter chips: `radius.full`, height 40 dp. Selected = `tint.lavender` strong fill with `accent`-dark or ink text; unselected = white with `color.outline` border. Dropdown chips show a chevron.
- Chips scroll horizontally if they overflow; a partly cut-off last chip signals scrollability.

### 9.6 Dialogs and bottom sheets

Use a dialog for a short, important decision. Use a bottom sheet (`radius.xl` top corners, drag handle) for compact creation or selection flows.

- State the action and consequence clearly.
- Provide explicit Cancel and confirm actions for destructive operations.
- Do not use a dialog for routine information that can be shown inline.
- Ensure the keyboard does not obscure the active field or primary action.

## 10. Iconography

- One icon family throughout. Recommended: a **thin outline family with rounded caps** (for example Phosphor Regular or Lucide), 1.5 dp stroke, drawn on a 24 dp grid.
- Icons appear inside circular chips (signature detail 4) or as bare 24 dp icons in the dock and toolbars.
- Use icons with labels for primary navigation and important actions.
- Do not use an icon as the only explanation for an unfamiliar action.
- Maintain consistent visual weight; use the same icon meaning across modules.

## 11. Charts and data visualisation

Charts exist to help the user understand real behaviour. They are also where this app earns its designed look, so they follow a strict visual style.

### 11.1 Global rules

- Every chart has a clear title, date range, unit, and a legend when needed.
- Use the right chart type: bars for comparisons, line or ribbon for trends, a ring only for a small number of parts of a whole.
- No 3D, no default gridlines, no axis frames. Label important values directly.
- Do not imply precision beyond the data.
- Show an honest empty state instead of a misleading chart (11.4).
- Keep semantic colours consistent: income, expense, completed, and focus metrics keep their meaning.
- Make chart information available as readable text where practical.

### 11.2 Chart components

**Pill bar chart (primary)**
- Bars are full-radius capsules; width 40–56 dp depending on count (5–7 bars across a hero panel). Spacing 8–12 dp.
- Height is proportional to the value on a zero baseline. A zero value shows no fill (only the soft track or a thin ghost outline), never a fake minimum bar. A small non-zero value may be drawn at a minimum of one bar-width high so it stays a clean capsule; the value label states the true number.
- **Selected bar:** solid `color.ink`, with an `onInk` value label. **Other bars:** hatched pattern (11.3) on the tint, or a solid soft tint.
- Day/period labels sit below each bar, 11–12 sp Medium.
- Tapping a bar selects it, moves the ink, and updates the related summary.

**Track bars ("fill inside a pill")**
- A full-height pill in `tint.soft` with a pill fill in `tint.strong` rising from the bottom; the percentage or value is written inside near the bottom of the fill. Use for comparing progress to a target.

**Ring (donut) chart**
- Thick ring (stroke about 28–32 dp), rounded segment caps, 4 dp gaps between segments. The largest or selected segment is ink; others use tint strong values.
- Rank or count badges: 28 dp white circles with a soft shadow and a 12 sp number, placed on the segment.
- A legend with the value for each part sits beside the ring: colour dot + label + value.
- Use only for a small number (about 2–5) of meaningful parts.

**Mini calendar / heat tiles**
- Rounded squares (radius 10–12 dp, 28–32 dp size) in 3–4 lavender intensity steps. Days with no data are unfilled (or ghost opacity without date numbers shouting). A legend or label explains the scale.

**Session timeline (for focus and activity history)**
- Horizontal pill segments on a time ruler (tick marks every hour, labels at 11–12 sp), showing real start/end times. Paused time is not included in a session segment.

**Goal gauge**
- A 270° arc with rounded caps and a dot knob at the end, centre value in 20–24 sp. Alternative: a thin pill progress bar on a `tint.butter` soft track.

### 11.3 Hatch pattern specification

- 45° diagonal lines, 1.25–1.5 dp stroke, 3–4 dp gap, colour `ink` at about 70% opacity (dark theme: `ink` at 55%), clipped to the capsule shape.
- If it hurts scrolling performance on low-end devices, fall back to a solid `tint.strong` fill with a 1.5 dp ink-at-60% outline. Do not remove the selected-vs-other distinction.
- Hatching also encodes difference without relying on colour alone, which helps accessibility.

### 11.4 Empty and insufficient data — Ghost chart rule (Rule 5)

- **Ghost chart rule (Rule 5):** Ghost charts are **uniform, flat, equal-height** pills at low opacity (≈ 40%). Never random heights that imply data. Caption text goes **above or below** the chart, never overlapping it. If a chart is empty and its container has a caption, do not draw a second message pill.
- No fake data, ever.

### 11.5 Contrast exception for pastel fills (conscious trade-off)

Pastel fills on white can be below 3:1 against the background. This is allowed **only if** every bar has a directly visible value label that meets 4.5:1, selected or emphasised items use ink, and no meaning is carried by fill colour alone. Otherwise, use a darker tint or an outline.

## 12. Light and Dark theme behaviour

- Both themes must be supported across every V1 screen and reusable component.
- Dark theme keeps the same shapes, radii, spacing, and chart styles. Only tokens change; **ink inverts** to a light lavender-white with dark text on it.
- Theme selection applies to backgrounds, tiles, text, borders, icons, charts, dialogs, and system bars.
- Avoid hard-coded light-only or dark-only colours in feature screens.
- Hatch and ring colours come from tokens and stay legible in both themes.
- The selected theme persists between launches. If system-following behaviour is added later, it is an explicit setting and does not remove manual Light/Dark selection.

## 13. Interaction and motion

- Use motion to communicate state changes, navigation, or completion. Keep transitions short and purposeful.
- **Chart entrance:** bars grow from the baseline, 300–400 ms, ease-out, 30 ms stagger between bars. Run once per screen visit.
- **Bar selection:** colour/pattern transition 200 ms; the summary text cross-fades 150 ms.
- **Dock selection:** active pill slides or fades to the new item, 200 ms.
- **Press feedback:** tiles and buttons scale to 0.98 with a tint change, 100 ms.
- **Habit/task completion:** the checkbox fills with a check mark and a brief scale pulse, under 250 ms, providing immediate feedback.
- Subtle feedback when starting, pausing, resuming, or finishing a focus session.
- Never delay a frequent action with animation.
- Respect Android reduced-motion settings (replace with a simple fade or none).
- Never rely on animation alone to communicate success or failure.

## 14. Empty, loading, error, and success states

Every data-driven screen defines the relevant states.

### 14.1 Empty-state system — 3 levels (Rule 4)

Replace ad-hoc empty cards with a strict 3-level system:

- **Level A — first-run:** a single "Get started" card on Home when all modules are empty (one checklist of 3 steps: Add your first task, Pick one daily habit, Start a 25-min focus session), disappearing once any item is created.
- **Level B — module empty:** a compact block (one icon, one title line, one sentence, one button) inside the container it belongs to. **One empty block per container.** Never put a tinted box inside a white card inside a section. Never repeat the same glyph for different modules: tasks, habits, focus, and finance each have their own icon.
- **Level C — period empty (Insights/Finance):** when the *whole* period has no data, show **one** consolidated empty card with a relevant action and hide the per-section ghost cards. When only some sections lack data, collapse each empty section to a **one-line row** ("Habit consistency — no data yet"), not a full ghost card.

**Loading state**
- Use skeleton shapes that match the final layout (soft tint pills and tiles) when loading is genuinely needed; avoid indefinite loading without explanation or recovery.

**Error state**
- Explain what failed in plain language, preserve user-entered data, and offer retry or an alternative. Never expose stack traces or raw errors.

**Success state**
- Confirm briefly and non-disruptively; update lists and summaries promptly.

## 15. Accessibility and usability

- Meet WCAG-informed contrast: 4.5:1 for normal text, 3:1 for large text and essential graphical controls (with the pastel-fill exception in 11.5).
- Do not rely on colour alone to communicate completion, transaction type, or errors. Hatch vs solid and text labels provide redundant cues.
- Provide accessible labels for icon-only controls, including circular utility buttons and arrow buttons.
- Touch targets at least 48 dp in each dimension, including dock items and day tiles.
- Support Android font scaling; layouts must reflow (tiles stack) and not clip at 150%.
- Maintain logical screen-reader reading order. Charts expose a text summary (for example "Focus time this week: Monday 1 h 20 min, Tuesday 2 h 5 min…").
- Use clear, simple language and consistent terminology.
- Make error messages specific and actionable.
- Ensure controls are distinguishable from non-interactive content.

## 16. Content and microcopy

Use concise, friendly, neutral language.

Preferred examples: "Add task", "Start focus", "Pause session", "Resume session", "Finish session", "Mark as done", "No expenses recorded today", "No habits scheduled for today".

Avoid: blaming language such as "You failed"; ambiguous labels such as "Go" or "Do it" when a specific action name is possible; claims such as "You were productive for 3 hours" when the app only knows that a timer was running; unexplained technical terms.

Use consistent terminology across screens. Do not alternate between "focus time", "work time", and "productive time" for the same calculated metric.

## 17. Screen-specific design constraints

All screens use the same system: tokens, tiles, pill charts, icon chips, floating dock.

### Home Dashboard

- Follow section 7.2: header sheet (≤ 230 dp), compact 1–2 line editorial headline, week strip, lavender hero panel (≤ 200 dp) with a pill bar chart, primary quick actions reachable without scrolling, then the 2 × 2 bento grid of summary tiles.
- Priority order: today's progress, quick actions, 2 × 2 summary tiles, tasks, habits, daily review.
- Show the four agreed summary metrics as a 2 × 2 grid of stat tiles (section 9.2) using the tint map in 4.4 and micro-visuals (Rule 8).
- Keep Start focus, Add task, and Add expense easy to reach in the first viewport.
- The hero chart must show real saved data (for example this week's focus time). If there is not enough data, use the flat uniform ghost bars from 11.4. Do not add a calendar, extra charts, or secondary metrics that compete with the core overview.
- Show compact previews of today's tasks and scheduled habits in list containers using Level B empty states when empty.

### Daily Planning

- Make adding and completing tasks fast.
- Clearly distinguish task title, category, planned time, and optional estimate. Do not visually imply that an estimate is required.
- Use `tint.lavender` for module accents.

### Active Work & Focus

- Use compact, structured sections for task planning, habit completion, and focus history; do not create unrelated dashboard cards.
- While a session is active, the **timer is the primary visual focus**: large tabular numerals (48–56 sp) in a hero panel (`tint.mint`), with the linked task or general activity clearly named.
- Pause/Resume and Finish controls are unambiguous: Pause/Resume as a primary ink pill button, Finish as a secondary tonal button.
- Show paused state clearly (label plus a visual change) and exclude paused time from the displayed focused duration.
- Focus history uses the session timeline from 11.2.

### Daily Habits

- Daily completion is a one-tap action, with the check animation from section 13.
- Show whether each habit is due today. Distinguish "not scheduled" from "incomplete" (different visual treatment and label).
- Use `tint.butter` for module accents.

### Finance

- Reuse the same lavender-led light visual system, typography, surface shapes, and spacing as Home.
- Present balance, income, and expense as a coherent summary group (a `tint.blue` or lavender hero plus two tiles) before transaction details, with a single primary "Add entry" CTA.
- Use compact, clearly labelled charts for spending trends (pill bars) and category breakdown (ring with a legend); apply Level C period-empty consolidation when a period has no transactions.
- Make Income and Expense selection explicit (a segmented control or two chips).
- Display amount and currency clearly with tabular numerals, `₹` symbol, and Indian digit grouping (`₹12,34,567`).
- Use separate semantic styling for income and expense, with text labels and icons as well as colour.

### Insights

- Reuse the same language while allowing charts to be the main content.
- Hierarchy: date-range chips, key takeaway sentence, hero trend visualisation, then a bento of supporting comparison tiles (mini bars, heat tiles, ring), applying Level C period-empty consolidation when the period has no records.
- Lavender is the primary chart tint with restrained supporting tints from the tint map; preserve semantic colours for finance data.
- Use readable chart labels and honest empty states.
- Avoid presenting a pattern as a proven cause. Use wording such as "on days with more focus time, you completed more tasks", not "focus time causes…".

### Daily Review

- Keep the reflection workflow lightweight, in `tint.rose` accents.
- Show source summaries or links rather than asking the user to re-enter data.
- Allow saving a review even if some daily data is absent.

### Settings

- Group preferences logically in white container panels with rows.
- Explain the purpose of a permission before requesting it.
- Keep theme selection easy to find.

## 18. UI quality checklist

Before a screen is considered ready, verify:

**System fit**
- [ ] It uses only tokens: no hard-coded colours, sizes, radii, or fonts.
- [ ] It follows the lavender/pastel + ink direction and the module tint map.
- [ ] It includes the signature details that apply (section 3.4).
- [ ] None of the anti-patterns in 3.5 appear.
- [ ] It works in Light and Dark themes (ink inverts correctly).

**Craft**
- [ ] Radii are concentric; tile padding and gutters follow the spacing tokens.
- [ ] There are no more than three ink elements, and one dominant tint plus at most two supporting tints.
- [ ] The squint test passes: blurred, the hero, the primary action, and the selected data are the clear focal points.
- [ ] Summary tiles form a composed 2 × 2 bento grid, not a uniform full-width stack.
- [ ] Number is big and label is small in every stat tile; zero-denominator ratios show `0` in `onTintSecondary` rather than `0 / 0`.
- [ ] Charts use pill bars, direct labels, the hatch/solid distinction, and flat equal-height ghost bars when empty.

**Quality**
- [ ] The primary action is visually obvious and reachable ("Start focus" in first viewport on Home).
- [ ] Text and controls have sufficient contrast (secondary text on tints uses `onTintSecondary` ≥ 4.5:1).
- [ ] Touch targets are at least 48 dp.
- [ ] Labels do not rely on placeholders alone.
- [ ] Empty (3-level system A/B/C), loading, error, and success states are handled where relevant.
- [ ] Long content, large font scales, and small screens do not clip; content clears the floating dock and its 32 dp fade scrim.
- [ ] Keyboard, system bars, and navigation insets are respected.
- [ ] Screen-reader labels and reading order are considered; charts have text summaries.
- [ ] No fake sample data appears in real user analytics.
- [ ] Interactions give clear feedback without unnecessary animation, and reduced motion is honoured.

## 19. Relationship to other documents

- `01_PRODUCT_OVERVIEW.md` defines product vision and scope.
- `02_PRODUCT_REQUIREMENTS.md` defines required product behaviour and acceptance criteria.
- This document defines shared UI/UX rules.
- `04_HOME_DASHBOARD.md` and subsequent module documents define screen-specific layouts and interactions.
- `11_DATA_ARCHITECTURE.md` and `12_TECHNICAL_ARCHITECTURE.md` define data and implementation decisions.

If a screen-specific design appears to conflict with this guideline, resolve the conflict explicitly and update the relevant documents. Do not silently introduce a new design system or expand V1 scope.

## 20. Changelog

**1.3**
- Added Rule 1 (Home composition rule: header sheet ≤ 230 dp, hero panel ≤ 200 dp, and primary quick-action row with "Start focus" in the first viewport on 360 × 800 dp).
- Added Rule 2 (Bento 2 × 2 grid rule for summary metric tiles ≈ 140–160 dp tall).
- Added Rule 3 (Dock separation: soft ink shadow plus 32 dp vertical canvas fade scrim above the dock and full bottom padding clearance).
- Added Rule 4 (3-level empty-state system: Level A first-run Home checklist, Level B single compact per-container block with module-specific icon, Level C consolidated period-empty card or one-line collapsed row).
- Added Rule 5 (Ghost chart rule: uniform, flat, equal-height pills at ≈ 40% opacity with non-overlapping captions).
- Added Rule 6 (Empty-value style in `onTintSecondary`, zero-denominator `0` display instead of `0 / 0`, and ratio with micro progress bar when denominator > 0).
- Added Rule 7 (Unified currency/number formatting: `₹` only, Indian digit grouping `₹12,34,567`, no `$`, tabular numerals).
- Added Rule 8 (Per-tile micro-visuals when data exists: thin progress bar for Tasks, mini pills for Focus/Expenses, row of dots for Habits).
- Added Rule 9 (Header-headline rule: 1–2 lines max on Home so the first interactive card starts above 300 dp from top).

**1.2**
- Added section 0 (how to use with AI tools), 3.4 Signature details, and 3.5 Anti-patterns.
- Made ink the action/emphasis colour; `#7563D8` demoted to `color.accent` (no longer a button fill).
- Added pastel tint tokens (lavender, mint, blue, rose, butter) in three steps for light and dark, plus a tint meaning map.
- Added `onTintSecondary` for contrast on tinted surfaces.
- Increased corner radii (24/32/40) and added the concentric-radius rule.
- Replaced border-first separation with tonal separation; limited shadows to the dock and sheets.
- Specified Urbanist (with fallbacks), a revised type scale, and a Home-only editorial headline.
- Added the floating dock specification and the Home anatomy.
- Added full chart specs: pill bars, hatch pattern, track bars, ring with badges, heat tiles, session timeline, gauge, and empty states.
- Resolved contradictory Home rules and removed leftover implementation notes.
- Added motion specs and a revised checklist.
