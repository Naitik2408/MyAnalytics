# Life Analytics --- UI/UX Guidelines

**Document:** `03_UI_UX_GUIDELINES.md`\
**Product:** Life Analytics\
**Version:** 1.0 (V1)\
**Status:** Initial design baseline\
**Platform:** Android\
**Design direction:** Modern, clean, calm, purple-blue visual identity

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

### 3.1 Overall style

Use a modern personal productivity and analytics aesthetic:

-   Clean layouts with generous but efficient spacing.
-   Rounded cards and input surfaces.
-   Subtle shadows or tonal surface separation; avoid heavy elevation
    everywhere.
-   Purple-blue as the primary brand direction.
-   Neutral backgrounds for long sessions of reading and data review.
-   Restrained use of gradients, decorative illustrations, and accent
    colors.
-   Clear icons paired with text labels for important actions.
-   Consistent chart styling and meaningful legends.

Avoid:

-   Crowded dashboards with too many metrics.
-   Excessive gradients, glows, or strong shadows.
-   Tiny labels and low-contrast secondary text.
-   Color as the only way to communicate a status.
-   Decorative charts that do not encode real data.
-   Excessive animations that slow down routine actions.

## 4. Color system

The palette should be implemented as semantic design tokens rather than
hard-coded colors in individual screens. The values below are the
initial design direction and may be tuned during implementation for
contrast and platform consistency.

### 4.1 Light theme

  --------------------------------------------------------------------------
  Token                      Suggested value         Usage
  -------------------------- ----------------------- -----------------------
  `color.primary`            `#6558D3`               Primary actions,
                                                     selected navigation,
                                                     key highlights

  `color.primaryContainer`   `#EAE7FF`               Soft primary surfaces

  `color.secondary`          `#4776C8`               Secondary highlights
                                                     and supporting data

  `color.background`         `#F7F8FC`               Main app background

  `color.surface`            `#FFFFFF`               Cards, sheets, dialogs

  `color.surfaceVariant`     `#F0F1F7`               Secondary surfaces and
                                                     input backgrounds

  `color.textPrimary`        `#202235`               Main text

  `color.textSecondary`      `#686B7D`               Supporting text

  `color.outline`            `#DCDD E8`              Borders and dividers;
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

**Implementation note:** The light-theme outline value above contains a
spacing typo in its illustrative token label; use the valid hex value
`#DCDDE8`. Verify all final color pairs against WCAG contrast guidance
before release. Do not assume that matching hex values across themes
produce equivalent contrast.

### 4.3 Color usage rules

-   Use the primary color for the most important action on a screen.
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

-   Make Income and Expense selection explicit.
-   Display amount and currency clearly.
-   Use separate semantic styling for income and expense, with text
    labels as well as color.

### Insights

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

-   [ ] It follows the agreed purple-blue visual direction.
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
