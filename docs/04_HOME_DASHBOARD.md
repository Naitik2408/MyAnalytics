# Life Analytics --- Home Dashboard

**Document:** `04_HOME_DASHBOARD.md`\
**Product:** Life Analytics\
**Version:** 1.0 (V1)\
**Status:** Screen specification\
**Platform:** Android\
**Related documents:** `01_PRODUCT_OVERVIEW.md`,
`02_PRODUCT_REQUIREMENTS.md`, `03_UI_UX_GUIDELINES.md`

------------------------------------------------------------------------

## 1. Purpose

The Home Dashboard is the user's daily starting point. It should answer
four questions quickly:

1.  What have I planned, and what is completed?
2.  How much focused time have I recorded?
3.  How much have I spent today?
4.  Which scheduled habits have I completed?

It must also make the most common next actions easy to reach: start a
focus session, add a task, and record an expense.

**Experience goal:** The user should understand the current day's status
at a glance and be able to take the next action without navigating
through multiple screens.

## 2. UX principles for this screen

-   Show the most useful information first; do not turn Home into a
    report.
-   Use clear labels and familiar language.
-   Keep the four summary metrics visually balanced.
-   Make quick actions prominent but not visually louder than the page
    title and daily context.
-   Keep task and habit previews compact and actionable.
-   Never ask the user to enter data twice.
-   Every dashboard value must come from its source module.
-   Empty states should guide the next action without making the user
    feel behind.
-   Avoid unnecessary charts on Home; detailed analysis belongs in
    Insights.
-   Make the screen comfortable to use one-handed on a phone.

## 3. Screen structure

The screen is vertically scrollable and uses the shared visual tokens in
`03_UI_UX_GUIDELINES.md`.

Recommended order:

1.  Header
2.  Daily summary metrics
3.  Quick actions
4.  Today's Tasks preview
5.  Today's Habits preview

The primary bottom navigation remains visible according to the app
navigation pattern.

### 3.1 Low-fidelity layout

``` text
┌──────────────────────────────────────┐
│ Good morning                         │
│ Monday, 16 November          [Profile]│
│                                      │
│ Your day at a glance                 │
│ ┌────────────────┐ ┌────────────────┐│
│ │ Tasks completed│ │ Focused time   ││
│ │     3 / 5      │ │    1h 25m      ││
│ └────────────────┘ └────────────────┘│
│ ┌────────────────┐ ┌────────────────┐│
│ │ Today's expenses││ Habits done    ││
│ │    ₹ 350       │ │     2 / 3      ││
│ └────────────────┘ └────────────────┘│
│                                      │
│ Quick actions                        │
│ [ Start Focus ] [ Add Task ] [ + ]   │
│                         Add Expense  │
│                                      │
│ Today's Tasks             View all > │
│ [ ] Study React                     │
│ [✓] Complete assignment             │
│ [ ] Read documentation              │
│                                      │
│ Today's Habits            View all >│
│ [✓] Morning walk                    │
│ [ ] Read 10 pages                   │
│ [✓] Drink enough water              │
│                                      │
├──────────────────────────────────────┤
│   Home     Finance   Activity Insights│
└──────────────────────────────────────┘
```

The values above are illustrative wireframe examples only. They must
never be shipped as hard-coded or seeded user data.

## 4. Header

### Content

-   Greeting that may vary by local time, without requiring a
    permission.
-   Current local date and weekday.
-   Profile/settings icon or another consistent Settings entry point.

### Behaviour

-   Date must reflect the app's configured local date and timezone
    rules.
-   Tapping the Settings icon opens Settings.
-   Greeting is decorative context and must not block the screen if
    unavailable.
-   Avoid a large profile area that pushes useful daily information
    below the fold.

## 5. Daily summary metrics

Show four metrics in a two-column grid on typical phone widths. On very
narrow screens, preserve readable labels and values; use a stacked
layout if necessary rather than squeezing content.

### 5.1 Tasks Completed

**Display:** completed tasks / total tasks planned for the selected
date.

Example: `3 / 5`

Rules: - Numerator = tasks marked complete for that date. - Denominator
= all tasks planned for that date, including completed tasks. - If there
are no tasks, show `0 / 0` or a clear "No tasks planned" state. - Do not
count habits as tasks. - Tapping the card opens Daily Planning for the
displayed date.

### 5.2 Focused Time

**Display:** total valid focused duration for the selected date.

Example: `1h 25m`

Rules: - Sum the focused duration of sessions attributed to that local
date. - Exclude paused intervals. - Include both task-linked and general
focus sessions. - Use a consistent duration format: minutes below one
hour; hours and minutes above one hour. - If no session exists, show
`0m` or "No focus sessions yet". - Tapping the card opens Active Work /
session history for the displayed date. - Do not label this as
distraction-free time or verified productivity.

### 5.3 Today's Expenses

**Display:** total expense amount for the selected date, using the app's
selected currency.

Example: `₹350`

Rules: - Sum only expense transactions dated on the selected date. - Do
not subtract income from this metric. - Use currency-aware formatting
and avoid ambiguous abbreviations. - If there are no expenses, show `₹0`
or "No expenses recorded". - Tapping the card opens Finance filtered to
expenses for that date.

### 5.4 Habits Completed

**Display:** completed scheduled habits / habits due for the selected
date.

Example: `2 / 3`

Rules: - Denominator includes only active habits scheduled for that
date. - Numerator includes completed records for those habits on that
date. - A habit not scheduled for the date must not count as
incomplete. - If no habits are due, show "No habits scheduled". -
Tapping the card opens the Daily Habits screen for that date.

### 5.5 Metric card design

Each metric card should include: - A short, readable label. - A
prominent value. - Optional small icon if it improves recognition. -
Optional concise supporting context only when useful.

Avoid: - Large decorative illustrations. - Red/green performance
judgements. - Tiny trend charts inside every card. - Inconsistent card
heights or number formats.

## 6. Quick actions

Three actions:

1.  **Start Focus** --- opens the focus-start flow.
2.  **Add Task** --- opens the task creation flow with the selected date
    prefilled.
3.  **Add Expense** --- opens the transaction form with type set to
    Expense and date prefilled.

### Interaction rules

-   Start Focus must offer the supported session choices: existing task,
    create a task and start, or general focus.
-   Add Task must return the user to a sensible destination after
    saving.
-   Add Expense must not require the user to navigate through multiple
    unrelated screens.
-   Prevent accidental duplicate saves while an action is processing.
-   Provide immediate confirmation after a successful save or session
    start.
-   If an action cannot proceed, explain why and preserve any
    information the user has entered.

### Visual priority

Start Focus is the primary action because it begins an active workflow.
Add Task and Add Expense should remain clearly visible as secondary
actions. On narrow screens, use a compact row or a two-row arrangement
rather than reducing touch target sizes.

## 7. Today's Tasks preview

### Content

-   Section title: "Today's Tasks".
-   "View all" action.
-   A compact list of today's tasks.
-   Each row shows a completion control and task title.
-   Optional category or planned time may appear as secondary metadata
    if space permits.

### Behaviour

-   Tapping the completion control updates the task's completion state.
-   The Tasks Completed metric updates after the source record is saved.
-   Tapping the task row opens its detail/edit view.
-   View All opens Daily Planning for the selected date.
-   The preview must not create a separate copy of task data.
-   Limit the preview to a small number of tasks (recommended maximum: 3
    or 4), prioritizing incomplete tasks; if completed tasks are
    included, keep the ordering predictable.
-   Provide a clear indication when more tasks exist and can be viewed.

### Empty state

When no tasks are planned: - Show a short message such as "Nothing
planned yet". - Offer "Add a task". - Do not show example tasks.

### Completion feedback

After marking a task complete: - Update the checkbox and visual state
immediately where safe. - Persist the change. - Update the summary
metric from the saved source of truth. - If saving fails, restore the
prior state and show a clear error.

## 8. Today's Habits preview

### Content

-   Section title: "Today's Habits".
-   "View all" action.
-   A compact list of habits scheduled for the selected date.
-   Each row shows a completion control and habit name.

### Behaviour

-   Only habits due on the selected date appear.
-   Tapping the completion control creates or updates that habit's
    completion record for the date.
-   Habits not scheduled today must not appear as missed.
-   Tapping a habit row opens its detail or edit view.
-   View All opens Daily Habits for the selected date.
-   The preview must read the source habit and completion records, not
    maintain its own state copy.
-   Use a predictable order, such as the user's configured order or a
    stable creation order.

### Empty states

Differentiate these cases: - No habits have been created: "Create a
habit to start tracking it." - Habits exist, but none are scheduled
today: "No habits scheduled for today." - Habits are due today but none
are completed: show the due habits with unchecked states; do not replace
them with a generic empty message.

## 9. Bottom navigation

The Home screen uses the shared four-destination bottom navigation:

-   Home
-   Finance
-   Activity
-   Insights

Rules: - Home is selected on this screen. - Navigation labels and icons
follow `03_UI_UX_GUIDELINES.md`. - Do not add Daily Habits as a fifth
bottom navigation item in V1. - Daily Planning and Active Work are
reachable through Activity and the Home actions/previews. - Daily Review
and Settings remain discoverable through their defined entry points.

## 10. Data dependencies and calculation ownership

The dashboard is a read-oriented aggregation of module data. It must not
own duplicate task, session, transaction, or habit records.

  -----------------------------------------------------------------------
  Dashboard element       Source of truth         Calculation
  ----------------------- ----------------------- -----------------------
  Tasks Completed         Task records            Completed tasks / all
                                                  tasks for date

  Focused Time            Focus session records   Sum of valid focused
                                                  duration attributed to
                                                  date

  Today's Expenses        Finance transactions    Sum of Expense amounts
                                                  for date

  Habits Completed        Habits and dated        Completed due habits /
                          completion records      active habits due on
                                                  date

  Task preview            Task records            Tasks planned for date

  Habit preview           Habit definitions and   Active habits scheduled
                          completion records      for date and their
                                                  daily status
  -----------------------------------------------------------------------

### Refresh rules

-   Refresh affected values after a successful create, update, complete,
    or delete operation in a source module.
-   Refresh when returning to Home if source data may have changed
    elsewhere.
-   Avoid stale totals after editing or deleting a source record.
-   Use a consistent local-day boundary for all four metrics.
-   Do not treat missing or failed data as a genuine zero; communicate
    the loading or error state.

## 11. Date handling

V1 Home defaults to the current local date.

If date navigation is introduced in the dashboard: - All four metrics
and both previews must use the same selected date. - The date must be
visible so the user understands which day is being viewed. - Quick
actions must inherit that selected date. - A focus session started from
a past or future date must follow an explicitly defined product rule; do
not silently create a session with a misleading date. - The detailed
date and timezone strategy is defined in `11_DATA_ARCHITECTURE.md`.

If date navigation is not included in the initial Home implementation,
keep Home focused on today and provide historical dates through the
relevant modules and Insights.

## 12. Loading, empty, and error states

### Loading

-   Use lightweight placeholders for metric cards and previews if data
    retrieval is asynchronous.
-   Keep the overall layout stable to reduce visual jumping.
-   Do not show placeholder numbers that could be mistaken for real
    values.

### Empty

-   Show zero or a short, helpful message for each metric.
-   Provide the next relevant action where appropriate.
-   Keep unrelated sections usable when one module has no data.

### Error

-   If one module fails to load, show an error for that section rather
    than hiding the entire dashboard.
-   Do not display stale values as current without indicating that they
    may be outdated.
-   Offer retry where possible.
-   Preserve the rest of the dashboard if its data loaded successfully.

### Success

-   Confirm completed quick actions briefly.
-   Update the affected preview and metric.
-   Avoid blocking dialogs for ordinary successful actions.

## 13. Accessibility and one-handed usability

-   Use readable labels and sufficient contrast in both themes.
-   Make the full intended control area comfortably tappable, preferably
    at least 48 dp.
-   Provide screen-reader labels for metric cards and icon buttons.
-   Announce task and habit completion state meaningfully to
    accessibility services.
-   Do not rely on color alone for completion state.
-   Support larger font settings without clipping card values or hiding
    actions.
-   Keep frequent actions within comfortable reach where the layout
    permits.
-   Ensure scrolling does not conflict with horizontal or nested
    gestures.

## 14. Performance and reliability expectations

-   Home should display promptly and avoid unnecessary full-screen
    loading.
-   Use efficient queries or derived state for daily aggregates.
-   Avoid recomputing expensive analytics on every small UI interaction.
-   Prevent duplicate event handling from rapid repeated taps.
-   Keep dashboard calculations consistent with module screens.
-   If data changes while Home is open, update the affected content
    without unexpectedly resetting scroll position.

## 15. Analytics events for product quality (optional implementation)

If local interaction analytics are implemented for debugging or UX
evaluation, keep them privacy-conscious and do not transmit personal
records.

Potential events: - `home_opened` - `home_start_focus_tapped` -
`home_add_task_tapped` - `home_add_expense_tapped` -
`home_task_completed` - `home_habit_completed` -
`home_view_all_tasks_tapped` - `home_view_all_habits_tapped`

These events are not a requirement to build a remote analytics service.
V1 must remain useful without sending personal activity or finance data
to a third party.

## 16. Acceptance criteria

### Layout and navigation

-   [ ] Home shows the agreed header and four summary metrics.
-   [ ] Metrics use a responsive two-column or readable stacked layout.
-   [ ] Quick actions are visible and have clear labels.
-   [ ] Task and habit previews are present.
-   [ ] Bottom navigation shows Home, Finance, Activity, and Insights.
-   [ ] Layout works on small and large Android phone screens.

### Metric correctness

-   [ ] Tasks Completed matches completed tasks divided by all tasks
    planned for the displayed date.
-   [ ] Focused Time sums valid session duration and excludes pauses.
-   [ ] Today's Expenses sums only expense transactions for the
    displayed date.
-   [ ] Habits Completed counts only habits scheduled for the displayed
    date.
-   [ ] All metrics update after source records change.
-   [ ] No metric uses hard-coded sample data.

### Actions and previews

-   [ ] Start Focus opens the correct session-start flow.
-   [ ] Add Task opens a task form with the correct date.
-   [ ] Add Expense opens an expense form with the correct date and
    type.
-   [ ] Task completion updates the source task and dashboard summary.
-   [ ] Habit completion updates the dated completion record and
    dashboard summary.
-   [ ] View All actions open the correct module and date.

### States and accessibility

-   [ ] Loading, empty, and error states are handled independently by
    section.
-   [ ] A failure in one module does not unnecessarily block other
    dashboard content.
-   [ ] Controls have accessible labels and adequate touch targets.
-   [ ] Light and Dark themes remain readable.
-   [ ] Large text does not cause important information or actions to be
    clipped.

## 17. Implementation notes and unresolved decisions

The following details must be aligned with the data and technical
architecture documents before implementation:

1.  Final Android UI framework and state-management approach.
2.  Whether Home reads a repository-provided daily summary or derives
    aggregates in the presentation layer. Prefer a single, testable
    source for aggregation.
3.  Exact ordering rule for task previews and habit previews.
4.  Whether the initial Home screen includes date navigation or only
    today's data.
5.  Final currency formatting and user-selected currency behaviour.
6.  Exact session attribution rule for sessions that cross local
    midnight.
7.  Offline database query and refresh strategy.

Do not invent answers to these questions in individual UI components.
Resolve them once in the relevant architecture document and apply them
consistently.

## 18. Related documentation

-   `01_PRODUCT_OVERVIEW.md` --- product vision and scope.
-   `02_PRODUCT_REQUIREMENTS.md` --- functional requirements and
    acceptance criteria.
-   `03_UI_UX_GUIDELINES.md` --- shared visual and interaction system.
-   `05_DAILY_PLANNING.md` --- task creation, editing, and daily
    planning details.
-   `06_ACTIVE_WORK_FOCUS.md` --- focus-session lifecycle and timer
    rules.
-   `07_DAILY_HABITS.md` --- habit schedules and daily completion.
-   `08_FINANCE.md` --- transactions and finance calculations.
-   `09_LIFE_ANALYTICS.md` --- historical analytics and charts.
-   `11_DATA_ARCHITECTURE.md` --- data model, persistence, and date
    rules.
