# Life Analytics --- UI/UX Guidelines

**Document:** `03_UI_UX_GUIDELINES.md`\
**Product:** Life Analytics\
**Version:** 1.1 (V1)\
**Status:** Revised visual direction — approved reference language\
**Platform:** Android\
**Design direction:** Premium light-mode-first health-and-habit-tracker visual language adapted for Life Analytics; lavender-led pastel palette, clean typography, structured information-dense layouts, expressive but restrained charts

------------------------------------------------------------------------

## 1. Purpose

This document defines the visual language and interaction principles for
Life Analytics V1. It is the shared design reference for all screens and
reusable components.

The UI must help the user record daily activity quickly and understand
personal patterns without making the app feel complicated or judgmental.

This document defines design rules, not final pixel-perfect screen
layouts. Individual screen documents may specify exact content and
screen-specific behaviour while following these guidelines.

## 2. Product design principles

1.  **Minimum input, maximum meaningful output:** avoid asking the user
    to enter information that the app can calculate.
2.  **Clarity before decoration:** visual hierarchy and readable
    information are more important than ornamental elements.
3.  **Consistent interactions:** similar actions must look and behave
    similarly across modules.
4.  **Fast daily use:** common actions such as adding a task, recording
    an expense, checking a habit, and starting focus must be easy to
    reach.
5.  **Honest analytics:** charts and summaries must represent actual
    saved records and clearly communicate missing data.
6.  **Calm, non-judgmental tone:** the app should help the user observe
    behaviour, not shame them for missed tasks or habits.
7.  **Accessible by default:** text, controls, contrast, and touch
    targets must work for a broad range of users.
8.  **Privacy-aware design:** show only the information needed for the
    current task and explain permission requests.

## 3. Visual direction

### 3.1 Approved visual reference and product adaptation

The approved visual direction is based on the selected **Health & Habit Tracker** reference shared during design review. Adopt its visual language as the primary reference across Life Analytics. Do not blend in the earlier dark dashboard, finance-app, editorial, or unrelated concepts. The reference is inspiration for visual treatment, not a source of product features or content.

Translate the reference into Life Analytics using these characteristics:

- Light-mode-first interface with a soft, very light neutral/lavender canvas.
- Lavender and purple as the dominant visual identity, supported sparingly by mint, pale blue, and warm neutral pastel surfaces.
- Crisp, dark, highly readable typography with a clear hierarchy.
- Rounded, soft surfaces with consistent geometry and subtle tonal separation.
- Compact but legible charts with expressive shapes, clear labels, and meaningful data encoding.
- Thoughtful use of small illustrations or decorative vector details only where they improve orientation or explain an empty state.
- A composed, information-rich layout: show useful information without crowding or making every element a separate card.
- Consistent alignment, deliberate whitespace, and repeatable section patterns.

The selected reference's strength is that it communicates substantial information while remaining calm and organized. Preserve that balance. Do not simplify the design by removing useful information, and do not make it busy by adding decorative sections.

### 3.2 Information density and composition

- Organize content into a clear page hierarchy: screen header, primary summary, important actions, and supporting sections.
- Use a consistent grid and alignment system across screens.
- Group related metrics together; avoid a separate oversized card for every metric.
- Prefer compact metric blocks, aligned rows, and well-composed chart panels where they suit the content.
- Use whitespace between sections, not excessive empty padding inside every component.
- Keep headings concise and avoid oversized typography that pushes useful content below the fold.
- Do not force a calendar, illustration, chart, or card onto a screen unless it supports the actual feature and available data.
- Keep the existing four primary destinations: Home, Finance, Activity, and Insights.

### 3.3 Avoid

- Mixing unrelated design directions or introducing a new visual identity on individual screens.
- Random pastel colors, excessive gradients, glows, heavy shadows, or decorative backgrounds.
- Dense clusters of cards with inconsistent sizes, radii, and padding.
- Oversized headings, awkward text wrapping, clipped labels, and isolated single-word line breaks.
- Charts that dominate the page without adding useful insight, or charts that use fabricated data.
- Generic placeholder illustrations repeated across unrelated modules.
- Redesigning product workflows, navigation, data models, calculations, or feature scope as part of visual work.

## 4. Color system

The palette should be implemented as semantic design tokens rather than
hard-coded colors in individual screens. The values below are the
initial design direction and may be tuned during implementation for
contrast and platform consistency.

### 4.1 Light theme

  --------------------------------------------------------------------------
  Token                      Suggested value         Usage
  -------------------------- ----------------------- -----------------------
  `color.primary`            `#7563D8`               Primary actions,
                                                     selected navigation,
                                                     key highlights

  `color.primaryContainer`   `#EEE9FF`               Soft primary surfaces

  `color.secondary`          `#8AA9E8`               Secondary highlights
                                                     and supporting data

  `color.background`         `#F8F7FC`               Main app background

  `color.surface`            `#FFFFFF`               Cards, sheets, dialogs

  `color.surfaceVariant`     `#F1EFF8`               Secondary surfaces and
                                                     input backgrounds

  `color.textPrimary`        `#28243A`               Main text

  `color.textSecondary`      `#777386`               Supporting text

  `color.outline`            `#E2DEED`              Borders and dividers;
                                                     normalize to a valid
                                                     hex value during
                                                     implementation

  `color.success`            `#23865A`               Completed/success
                                                     states

  `color.warning`            `#B7791F`               Caution and pending
                                                     states

  `color.error`              `#C44752`               Errors and destructive
                                                     actions

  `color.income`             `#23865A`               Finance income

  `color.expense`            `#C44752`               Finance expense
  --------------------------------------------------------------------------

### 4.2 Dark theme

  --------------------------------------------------------------------------
  Token                      Suggested value         Usage
  -------------------------- ----------------------- -----------------------
  `color.primary`            `#A69BFF`               Primary actions and
                                                     highlights

  `color.primaryContainer`   `#302A59`               Soft primary surfaces

  `color.secondary`          `#8FB5FF`               Secondary highlights

  `color.background`         `#12131B`               Main app background

  `color.surface`            `#1C1E29`               Cards, sheets, dialogs

  `color.surfaceVariant`     `#272A38`               Secondary surfaces and
                                                     input backgrounds

  `color.textPrimary`        `#F2F2FA`               Main text

  `color.textSecondary`      `#B1B3C2`               Supporting text

  `color.outline`            `#3B3E4D`               Borders and dividers

  `color.success`            `#63C995`               Completed/success
                                                     states

  `color.warning`            `#E5B45E`               Caution and pending
                                                     states

  `color.error`              `#F07A84`               Errors and destructive
                                                     actions

  `color.income`             `#63C995`               Finance income

  `color.expense`            `#F07A84`               Finance expense
  --------------------------------------------------------------------------

**Implementation note:** Use the valid light-theme outline token `#E2DEED`. These are starting design tokens, not a reason to hard-code colors. Verify final color pairs against WCAG-informed contrast guidance before release. Do not assume matching hex values across themes produce equivalent contrast.

### 4.3 Color usage rules

-   Use lavender/purple as the signature accent, not as a fill for every component.
-   Use mint and pale blue as limited supporting accents for distinct data groups or status categories.
-   Keep the overall canvas and most surfaces light and neutral; reserve stronger color for emphasis.
-   Use success and error colors only when their meaning is clear.
-   Income and expense must be distinguishable through both color and
    text/icon labels.
-   Completed states must include a checkmark, label, or other non-color
    cue.
-   Do not use red merely to indicate an ordinary missed task or
    incomplete habit.
-   Charts must use a consistent series palette and a visible legend
    when multiple series exist.
-   Avoid using primary color for every element; preserve its visual
    importance.

## 5. Typography

Use a clean Android-supported sans-serif typeface. Prefer the platform's
native typography system or a consistent modern sans-serif such as Inter
if bundled and licensed appropriately.

Suggested type scale:

  ------------------------------------------------------------------------
  Style                   Suggested size Weight           Usage
  ---------------- --------------------- ---------------- ----------------
  Display / screen             28--32 sp Semibold/Bold    Large headline
  highlight                                               or key timer
                                                          value

  Screen title                 22--24 sp Semibold         Main screen
                                                          heading

  Section title                18--20 sp Semibold         Card and section
                                                          headings

  Body                         14--16 sp Regular          Main content and
                                                          descriptions

  Supporting                   12--14 sp Regular          Metadata, dates,
                                                          helper text

  Small label                  11--12 sp Medium           Compact chart
                                                          labels; use
                                                          sparingly
  ------------------------------------------------------------------------

Typography rules:

-   Use `sp` for text sizing so Android font scaling is respected.
-   Keep body text comfortably readable; do not shrink essential content
    to fit a card.
-   Use tabular numerals for timers, durations, and financial amounts
    where supported.
-   Use consistent number formatting and currency symbols.
-   Do not communicate hierarchy using font size alone; also use weight
    and spacing.
-   Support system font scaling without clipping or overlapping.

## 6. Spacing, sizing, and shape

Use a consistent spacing scale based on 4 dp:

  Token          Value
  ------------ -------
  `space.1`       4 dp
  `space.2`       8 dp
  `space.3`      12 dp
  `space.4`      16 dp
  `space.5`      20 dp
  `space.6`      24 dp
  `space.8`      32 dp
  `space.10`     40 dp

Guidelines:

-   Use 16 dp as a common horizontal screen inset, adjusting for device
    and layout constraints.
-   Use 20--24 dp between major sections.
-   Use 12--16 dp inside standard cards.
-   Use 8--12 dp between related controls.
-   Use consistent corner radii: approximately 12 dp for inputs and
    compact cards, and 16--20 dp for larger cards or sheets.
-   Keep card heights content-driven; avoid fixed heights that clip
    larger text.
-   Use dividers only when spacing or surface separation is
    insufficient.

## 7. Layout and screen structure

### 7.1 General screen structure

A typical screen should use:

1.  A clear top app bar with a title and relevant actions.
2.  A scrollable content area.
3.  Grouped sections with clear headings.
4.  A persistent bottom navigation bar for the four primary
    destinations, where appropriate.
5.  A safe-area-aware layout that does not overlap system bars,
    keyboard, or gesture navigation.

### 7.2 Responsive behaviour

-   Design for Android phone widths first.
-   Support small and large phones without horizontal scrolling for
    ordinary screens.
-   Allow long lists and analytics content to scroll vertically.
-   Cards may adapt from a grid to a stacked layout on narrow screens.
-   Respect display cutouts, system insets, and keyboard visibility.
-   Do not rely on a single fixed screen size.

## 8. Navigation guidelines

The primary bottom navigation destinations are:

  Destination   Purpose
  ------------- --------------------------------------------------
  Home          Daily overview and quick actions
  Finance       Income, expenses, and finance summaries
  Activity      Daily Planning, Active Work, and session history
  Insights      Daily, weekly, and monthly analytics

Rules:

-   Use both a recognizable icon and a short text label.
-   Clearly indicate the selected destination.
-   Preserve a user's place in a section when practical.
-   Do not add every module as a bottom navigation item.
-   Daily Habits should be accessible from Home and its dedicated module
    route.
-   Daily Review and Settings should have clear, discoverable entry
    points.
-   Use Android back behaviour consistently; avoid trapping the user in
    nested screens.

## 9. Reusable component guidelines

Create reusable components so the app remains visually consistent.
Component names below are conceptual and may be adapted to the chosen
framework.

### 9.1 Buttons

**Primary button** - Used for the main action, such as Save, Start
Focus, or Add Expense. - Filled primary surface with high-contrast
label. - One dominant primary action per screen or dialog where
practical.

**Secondary button** - Used for less prominent actions. - Outlined or
tonal treatment.

**Text button** - Used for low-emphasis actions such as Cancel, View
All, or Edit.

**Icon button** - Used for compact actions such as Settings or
overflow. - Must have an accessible content description and adequate
touch target.

Button states must include enabled, pressed, disabled, and loading where
relevant. Prevent accidental duplicate submissions while a save action
is in progress.

### 9.2 Cards

-   Use cards to group related information, not to wrap every single
    text element.
-   Maintain consistent internal padding and corner radius.
-   Keep card titles and actions in predictable positions.
-   Use subtle borders or surface contrast before adding strong shadows.
-   Avoid deeply nested cards.

### 9.3 Inputs

-   Every input must have a persistent label; placeholder text is not a
    replacement for a label.
-   Show required/optional status where it is not obvious.
-   Use the correct Android keyboard type for numbers, amounts, dates,
    and text.
-   Show validation feedback close to the relevant field.
-   Preserve entered values when validation fails.
-   Provide clear focus, error, disabled, and read-only states.
-   Do not clear a form unexpectedly when the user navigates back.

### 9.4 Lists and rows

-   Use a consistent row structure for tasks, habits, transactions, and
    focus sessions.
-   Keep the primary label visually strongest.
-   Put supporting metadata on a secondary line or aligned trailing
    area.
-   Make the whole row tappable only when that behaviour is clear.
-   Keep checkboxes and row actions separate enough to avoid accidental
    activation.
-   Provide useful empty states when a list has no records.

### 9.5 Dialogs and bottom sheets

Use a dialog for a short, important decision. Use a bottom sheet for
compact creation or selection flows when it improves the Android
interaction.

-   State the action and consequence clearly.
-   Provide explicit Cancel and confirm actions for destructive
    operations.
-   Do not use a dialog for routine information that can be shown
    inline.
-   Ensure the keyboard does not obscure the active field or primary
    action.

## 10. Iconography

-   Use one consistent icon family throughout the app.
-   Prefer simple, recognizable line icons.
-   Use icons with labels for primary navigation and important actions.
-   Do not use an icon as the only explanation for an unfamiliar action.
-   Maintain consistent visual weight and optical size.
-   Use the same icon meaning consistently across modules.

## 11. Charts and data visualization

Charts exist to help the user understand real behaviour, not to decorate
the dashboard.

Rules:

-   Every chart must have a clear title, date range, unit, and legend
    when needed.
-   Use the correct chart type for the data.
-   Use bar charts for comparisons across categories or periods.
-   Use line charts for trends over time.
-   Use a pie/donut chart only when showing a small number of meaningful
    parts of a whole.
-   Avoid 3D charts and unnecessary visual effects.
-   Label important values directly when doing so improves
    comprehension.
-   Do not imply precision beyond the underlying data.
-   Show an empty state or a clear insufficient-data message instead of
    a misleading chart.
-   Keep colors consistent: income, expenses, completed states, and
    focus metrics must retain their semantic meaning.
-   Make chart information available in a readable textual form where
    practical.

## 12. Light and Dark theme behaviour

-   Both themes must be supported across every V1 screen and reusable
    component.
-   Theme selection must apply consistently to backgrounds, cards, text,
    borders, icons, charts, dialogs, and system bars.
-   Avoid hard-coded light-only or dark-only colors in feature screens.
-   Charts must remain legible in both themes.
-   Images and illustrations, if used, must not create unreadable
    contrast.
-   The selected theme must persist between app launches.
-   If system-following theme behaviour is offered later, it must be an
    explicit setting and must not remove manual Light/Dark selection.

## 13. Interaction and motion

-   Use motion to communicate state changes, navigation, or completion.
-   Keep transitions short and purposeful.
-   Avoid animation that delays a user from completing a frequent
    action.
-   Provide immediate visual feedback after checking a habit or
    completing a task.
-   Use subtle feedback when starting, pausing, resuming, or finishing a
    focus session.
-   Respect Android accessibility settings such as reduced motion where
    available.
-   Never rely on animation alone to communicate success or failure.

## 14. Empty, loading, error, and success states

Every data-driven screen must define relevant states.

**Empty state** - Explain what the section is for. - Offer one clear
next action, such as Add Task or Record Expense. - Do not show fake
records or fake analytics.

**Loading state** - Use a restrained progress indicator or skeleton when
loading is genuinely needed. - Avoid indefinite loading without
explanation or recovery.

**Error state** - Explain what failed in plain language. - Preserve
user-entered data where possible. - Offer a retry or alternative action
when appropriate. - Do not expose raw stack traces or internal error
messages.

**Success state** - Confirm that the action completed. - Keep
confirmation brief and non-disruptive. - Update relevant lists and
summaries promptly.

## 15. Accessibility and usability

-   Meet WCAG-informed contrast targets where practical: 4.5:1 for
    normal text and 3:1 for large text and essential graphical controls.
-   Do not rely on color alone to communicate completion, transaction
    type, or errors.
-   Provide accessible labels/content descriptions for icon-only
    controls.
-   Ensure touch targets are preferably at least 48 dp in each
    dimension.
-   Support Android font scaling and avoid clipped text.
-   Maintain logical screen-reader reading order.
-   Use clear, simple language and consistent terminology.
-   Make error messages specific and actionable.
-   Ensure controls are distinguishable from non-interactive content.

## 16. Content and microcopy

Use concise, friendly, neutral language.

Preferred examples: - "Add task" - "Start focus" - "Pause session" -
"Resume session" - "Finish session" - "Mark as done" - "No expenses
recorded today" - "No habits scheduled for today"

Avoid: - Blaming language such as "You failed". - Ambiguous labels such
as "Go" or "Do it" when a specific action name is possible. - Claims
such as "You were productive for 3 hours" when the app only knows that a
timer was running. - Unexplained technical terms.

Use consistent terminology across screens. For example, do not alternate
between "focus time", "work time", and "productive time" when they refer
to the same calculated metric.

## 17. Screen-specific design constraints

### Home Dashboard

-   Use the approved reference language as the closest visual match: clear greeting/date header, a prominent but compact daily overview, and organized pastel metric groups.
-   Keep today's progress, quick actions, tasks, habits, and focus preview in a clear priority order.
-   Use a chart or progress visualization only when it communicates real saved data; do not add a calendar merely because the reference contains one.
-   Preserve a calm composition with aligned sections and consistent spacing; do not turn every item into an independent card.


-   Prioritize the four agreed summary metrics.
-   Keep Start Focus, Add Task, and Add Expense easy to reach.
-   Show compact previews of today's tasks and scheduled habits.
-   Avoid adding charts or secondary metrics that compete with the core
    daily overview.

### Daily Planning

-   Make adding and completing tasks fast.
-   Clearly distinguish task title, category, planned time, and optional
    estimate.
-   Do not visually imply that an optional estimate is required.

### Active Work & Focus

-   Use compact, structured sections for task planning, habit completion, and focus history; do not create unrelated dashboard cards.
-   Make the timer the primary visual focus while a session is active.
-   Clearly show the linked task or general activity.
-   Make Pause/Resume and Finish controls unambiguous.
-   Show paused state clearly and do not include paused time in the
    displayed focused duration.

### Daily Habits

-   Make daily completion a one-tap action.
-   Show whether each habit is due today.
-   Distinguish not scheduled from incomplete.

### Finance

-   Reuse the same lavender-led light visual system, typography, surface shapes, and spacing as Home.
-   Present balance, income, and expense as a coherent summary group before transaction details.
-   Use compact, clearly labelled charts for spending trends and category breakdowns; do not overload the page with multiple competing visualizations.
-   Make Income and Expense selection explicit.
-   Display amount and currency clearly.
-   Use separate semantic styling for income and expense, with text
    labels as well as color.

### Insights

-   Reuse the same reference language while allowing charts to be the main content where appropriate.
-   Use a clear hierarchy of date range, key takeaway, trend visualization, and supporting comparisons.
-   Use lavender as the primary chart color with restrained supporting pastel series; preserve semantic colors for finance data.
-   Prioritize date range and key takeaway.
-   Use readable chart labels and honest empty states.
-   Avoid presenting a pattern as a proven cause.

### Daily Review

-   Keep the reflection workflow lightweight.
-   Show source summaries or links rather than asking the user to
    re-enter them.
-   Allow saving a review even if some daily data is absent.

### Settings

-   Group preferences logically.
-   Explain permission purpose before a permission request.
-   Keep theme selection easy to find.

## 18. UI quality checklist

Before a screen is considered ready, verify:

-   [ ] It follows the approved Health & Habit Tracker-inspired lavender/pastel visual direction.
-   [ ] It maintains the same visual language and component rules as the other modules.
-   [ ] It is information-rich but calm, aligned, and free of visual clutter.
-   [ ] It works in Light and Dark themes.
-   [ ] It uses shared spacing, typography, shape, and component tokens.
-   [ ] The primary action is visually obvious.
-   [ ] Text and controls have sufficient contrast.
-   [ ] Touch targets are comfortable.
-   [ ] Labels do not rely on placeholders alone.
-   [ ] Empty, loading, error, and success states are handled where
    relevant.
-   [ ] Long content and small-screen layouts do not clip.
-   [ ] Keyboard, system bars, and navigation insets are respected.
-   [ ] Screen-reader labels and logical reading order are considered.
-   [ ] No fake sample data appears in real user analytics.
-   [ ] Interactions provide clear feedback without unnecessary
    animation.

## 19. Relationship to other documents

-   `01_PRODUCT_OVERVIEW.md` defines product vision and scope.
-   `02_PRODUCT_REQUIREMENTS.md` defines required product behaviour and
    acceptance criteria.
-   This document defines shared UI/UX rules.
-   `04_HOME_DASHBOARD.md` and subsequent module documents define
    screen-specific layouts and interactions.
-   `11_DATA_ARCHITECTURE.md` and `12_TECHNICAL_ARCHITECTURE.md` define
    data and implementation decisions.

If a screen-specific design appears to conflict with this guideline,
resolve the conflict explicitly and update the relevant documents. Do
not silently introduce a new design system or expand V1 scope.


---

# Product decisions update — 19 November 2026

This section records the latest decisions made during product planning. Where an older paragraph in this document conflicts with this update, **this update takes precedence for V1**. Existing unrelated module requirements remain in force.

## Confirmed product structure

- Keep the existing **Habits** and **Tasks** as separate tracking systems. Do not replace them with life-area modules.
- Add an analytics classification layer above existing records.
- V1 life areas are exactly: **Health & Lifestyle**, **Career & Productivity**, and **Finance & Wealth**. Do not add a fourth life area in V1.
- When creating or editing a Habit or Task, the user manually selects one life area. Automatic categorisation is explicitly deferred to a future version.
- Do not require migration or backfilling of old records for V1 analytics. New records can be classified going forward. Preserve existing records and existing tracking workflows.
- Personal development activities (for example English-speaking practice and reading for learning) belong under Career & Productivity for V1.

## Metric catalogue and scoring rules

### Health & Lifestyle

- **Scheduled health-habit adherence:** completed scheduled health habits divided by scheduled health habits for the selected period, multiplied by 100. Examples include hydration, meditation, exercise, and other user-defined health habits.
- **Sleep duration / consistency:** optional supporting metric only when the user records sleep data. Do not infer sleep from unrelated activity.
- **Physical activity:** represented by manually categorised exercise/activity habits in V1; do not require a separate duplicate activity log.
- **Mental well-being / recovery:** optional user-entered reflection or habit data. Do not make psychological or medical claims.

### Career & Productivity

- **Task completion rate:** completed planned tasks divided by all tasks planned for the selected period, multiplied by 100. Show the numerator and denominator. In V1 each task has equal weight unless a priority/weighting feature is separately approved.
- **Career-habit adherence:** completed scheduled habits categorised as Career & Productivity divided by scheduled career habits, multiplied by 100. Examples include English-speaking practice, reading for learning, and skill practice.
- **Focus time:** show recorded focus duration as a supporting activity metric, not proof of productivity, attention, or quality of work.

### Finance & Wealth

- **Income:** sum of recorded income transactions for the selected period.
- **Expenses:** sum of recorded expense transactions for the selected period.
- **Net cash flow for period:** income minus expenses; this is not an account balance.
- **Savings amount and savings rate:** savings amount is income minus expenses; savings rate is (income minus expenses) divided by income, multiplied by 100, only when income is greater than zero.
- **Budget adherence:** compare actual expenses with a user-defined budget only when a budget exists. Lower spending is not automatically better if it conflicts with the user's goals or essential needs.

## Score calculation policy

- Keep **Habit adherence**, **Task completion**, **Finance metrics**, **category performance**, and **overall life performance** distinguishable. Do not add raw task counts directly to habit percentages.
- Health category score is based primarily on scheduled Health & Lifestyle habit adherence. Optional sleep/activity metrics may be added only when actual data exists and their weights are explicitly configured.
- Career category score combines Career & Productivity habit adherence and task completion rate. V1 starting weights: 40% career-habit adherence and 60% task completion rate. If one component has no eligible data, calculate from the available component and label the score as partial; do not silently treat missing data as zero.
- Finance category score must be goal/budget-aware. V1 may show financial metrics without a numeric Finance score until a valid budget or savings goal provides a fair target. Never reward simply spending less or having higher income without context.
- Overall Life Score is the equal-weight average of category scores that have sufficient eligible data. Show **data coverage** (for example, 2 of 3 categories available). Never substitute zero for missing or untracked data. If no category has sufficient data, show “Not enough data yet”.
- Scores are descriptive personal tracking indicators, not scientifically validated measures of life quality, health, mental health, or success. No universal therapist/psychologist formula is claimed.
- Distinguish **performance** (score for the selected period), **change** (point difference versus an equivalent previous period), and **consistency** (repeated adherence over time). A higher score is not automatically proof of long-term growth.

## Home dashboard and visualisation decisions

The Home screen balances two purposes equally: (1) overall life performance and growth trends, and (2) today's execution. Its key sections are:

1. Overall Life Score with data coverage and comparison to the previous equivalent period.
2. Health, Career, and Finance category breakdowns, with unavailable/insufficient-data states clearly labelled.
3. Performance-over-time line chart with Daily, Weekly, Monthly, and longer-range views where data density permits.
4. A 365-day year view: one dot/cell per calendar day, with elapsed days visually distinct from future days. For elapsed days with a valid score, color intensity represents performance. Future days remain neutral/unfilled. Days with no score use a distinct missing-data style, never the lowest-performance color. Leap years use 366 days.
5. Daily execution summary: today's habits and tasks, including completed/total counts.
6. Selecting a calendar day opens that date's score, category breakdown, and source activity.

The line chart and calendar must use saved records only. Do not render illustrative/sample values in production. The calendar communicates both time awareness (days elapsed) and recorded performance; these are separate visual meanings.

## UI/UX reference

Use the latest revised `03_UI_UX_GUIDELINES.md` as the single source of truth: light-mode-first, lavender-led pastel identity, clean readable typography, rounded soft surfaces, structured information density, restrained mint/blue/warm-neutral supporting colors, and clear chart encodings. Do not revive earlier unrelated dark-dashboard or finance-app visual directions.

## V1 and future boundary

- V1: manual life-area selection on Habit/Task create/edit; transparent rule-based calculations; honest missing-data states; dashboard graph and 365-day view.
- Future: automatic categorisation suggestions with manual correction, advanced priority-weighted task scoring, predictive/AI insights, and additional life areas. These are not V1 requirements.
- Existing tracking data is not required to be migrated. No destructive schema or workflow change is permitted merely to enable analytics.

### Analytics visual implementation addendum
Use lavender/purple as the primary identity. For the 365-day calendar, use a neutral elapsed-day marker plus performance intensity only where a valid score exists; future and missing-data states must be visually distinct. Color must not be the only means of communicating score: provide accessible labels/tooltips and a legend.
