# Life Analytics --- Daily Planning

**Document:** `05_DAILY_PLANNING.md`\
**Product:** Life Analytics\
**Version:** 1.0 (V1)\
**Status:** Module specification\
**Platform:** Android\
**Related documents:** `01_PRODUCT_OVERVIEW.md`,
`02_PRODUCT_REQUIREMENTS.md`, `03_UI_UX_GUIDELINES.md`,
`04_HOME_DASHBOARD.md`

------------------------------------------------------------------------

## 1. Purpose

Daily Planning helps the user decide what they intend to do on a
particular day, organize those tasks, and track whether each task was
completed.

The experience must make planning feel lightweight rather than like
managing a complicated project tool. A user should be able to add a task
quickly, understand the day's workload, and update progress with minimal
effort.

**Core distinction:** Tasks represent planned work. Habits represent
recurring behaviours and are managed separately in `07_DAILY_HABITS.md`.

## 2. UX goals

-   Add a simple task in a few seconds.
-   Keep the task form short and avoid mandatory fields that are not
    essential.
-   Make today's plan easy to scan.
-   Make completing or reopening a task a simple action.
-   Keep planned work separate from measured focus time.
-   Allow a task to start a linked focus session without re-entering its
    title.
-   Show honest daily progress without judging the user.
-   Preserve the user's data if validation fails or they leave a form
    accidentally.

## 3. V1 scope

### Included

-   View tasks for a selected date.
-   Create, edit, complete, reopen, and delete tasks.
-   Set a task title and category.
-   Optionally set planned time and estimated duration.
-   View daily task completion and planned-duration summaries.
-   Start a focus session linked to a task.
-   Access planning from Home and Activity navigation.

### Not included in V1

-   Complex projects, subtasks, dependencies, and team assignments.
-   Task sharing or collaboration.
-   AI-generated task plans.
-   Automatic calendar synchronization.
-   Recurring task automation, unless separately approved.
-   Advanced priority scoring or productivity grading.

## 4. Information architecture

The Daily Planning experience should provide:

1.  **Date context:** clearly identify which day's plan is being viewed.
2.  **Daily summary:** completed tasks, total tasks, and planned
    duration when estimates exist.
3.  **Task list:** tasks grouped or ordered in a predictable way.
4.  **Add task action:** always easy to find.
5.  **Task actions:** complete/reopen, edit, delete, and start focus.

The primary entry point is Activity. Home also provides a compact
preview and an Add Task shortcut.

## 5. Screen structure

Recommended screen layout:

``` text
┌──────────────────────────────────────┐
│ < Back          Daily Planning   [⋮] │
│                                      │
│ Monday, 16 November 2026      [Date] │
│                                      │
│ Your progress                        │
│ ┌──────────────────────────────────┐ │
│ │ 3 of 5 tasks completed           │ │
│ │ [progress indicator]             │ │
│ │ Planned time: 3h 30m             │ │
│ └──────────────────────────────────┘ │
│                                      │
│ Tasks                     [+ Add]    │
│                                      │
│ ○  Study React              [▶] [⋮]  │
│    Study · 10:00 AM · 1h             │
│                                      │
│ ✓  Complete assignment      [⋮]     │
│    Study · Completed                 │
│                                      │
│ ○  Buy groceries            [▶] [⋮]  │
│    Personal · 5:00 PM · No estimate  │
│                                      │
│                          [+ Add task]│
├──────────────────────────────────────┤
│ Home    Finance   Activity  Insights │
└──────────────────────────────────────┘
```

All values in this wireframe are illustrative. The production app must
show only actual saved records.

## 6. Date selection and day navigation

### Requirements

-   Default to the current local date.
-   Show the selected date and weekday clearly.
-   Allow the user to navigate to previous and next dates.
-   Provide a date picker for direct selection.
-   Visually distinguish today from another selected date.
-   Keep the selected date consistent across the summary and task list.

### Behaviour

-   Changing the selected date reloads that date's tasks and summary.
-   Adding a task from a selected date pre-fills that date.
-   Returning from a task edit should preserve the selected date and
    reasonable list position.
-   Future and past dates may be viewed and planned.
-   Starting a focus session from a task on a past or future date must
    follow the session-start rule defined in `06_ACTIVE_WORK_FOCUS.md`;
    do not silently backdate or future-date an actual session.

## 7. Daily summary

Show a compact summary at the top of the screen.

### 7.1 Task completion

Display completed tasks divided by total tasks for the selected date.

Example: `3 of 5 completed`

Rules: - Count tasks planned for the selected date. - Completed tasks
form the numerator. - All tasks for the date form the denominator. - If
no tasks exist, show a friendly empty summary such as "No tasks
planned". - Update the summary after create, complete, reopen, or delete
actions.

### 7.2 Planned duration

Display the sum of estimated durations for tasks on the selected date,
where estimates exist.

Rules: - Include estimates from both completed and incomplete tasks
unless a later product decision specifies otherwise. - Do not treat a
task without an estimate as zero-duration work in explanatory copy. - If
no task has an estimate, show "No time estimates". - Clearly label this
as planned/estimated time, not actual focused time. - Actual focused
duration comes from saved focus sessions.

### 7.3 Progress indicator

-   A simple progress bar or ring may represent completed tasks / total
    tasks.
-   Do not use a progress indicator when there are zero tasks; show the
    empty summary instead.
-   Do not use alarming red styling for ordinary incomplete tasks.
-   Always provide a textual completion value for accessibility.

## 8. Task data and fields

### 8.1 Required field

  -----------------------------------------------------------------------
  Field                   Required                Rules
  ----------------------- ----------------------- -----------------------
  Title                   Yes                     Must contain at least
                                                  one non-whitespace
                                                  character; trim
                                                  leading/trailing
                                                  whitespace.

  -----------------------------------------------------------------------

### 8.2 Optional fields

  -----------------------------------------------------------------------
  Field                   Required                Rules
  ----------------------- ----------------------- -----------------------
  Category                Yes for V1 UI           Choose one of Study,
                                                  Work, Personal, or
                                                  Other; default may be
                                                  Other if the user does
                                                  not choose.

  Planned date            Yes                     Defaults to the
                                                  currently selected
                                                  date.

  Planned time            No                      Optional local time for
                                                  when the user intends
                                                  to do the task.

  Estimated duration      No                      Optional positive
                                                  duration.

  Notes                   No                      Do not add to the
                                                  initial form unless
                                                  there is a clear UX
                                                  need; may be considered
                                                  later.
  -----------------------------------------------------------------------

The category is required in the V1 task model/UI to keep tasks
filterable and understandable, but the form should provide a sensible
default so it does not slow down quick entry.

## 9. Add Task flow

### 9.1 Entry points

-   Add Task quick action on Home.
-   Add button on Daily Planning.
-   Any other approved task-creation entry point.

### 9.2 Form design

Keep the initial form compact:

``` text
┌──────────────────────────────────────┐
│ Add task                         [X] │
│                                      │
│ Task name                            │
│ [ e.g. Prepare project presentation ]│
│                                      │
│ Category                             │
│ [ Other                         ▾ ]  │
│                                      │
│ Planned date                         │
│ [ Monday, 16 November            ▾ ] │
│                                      │
│ Planned time (optional)              │
│ [ Add time                       ▾ ]│
│                                      │
│ Estimated duration (optional)        │
│ [ 30 minutes                     ▾ ]│
│                                      │
│             [Cancel] [Save task]     │
└──────────────────────────────────────┘
```

### 9.3 Form interaction rules

-   Focus the task title field when the form opens.
-   Use a text keyboard for the title.
-   Use a suitable picker for category, date, time, and duration.
-   Make optional fields visibly optional.
-   Do not require planned time or estimated duration.
-   Keep the Save action easy to reach when the keyboard is open.
-   Disable or guard Save while a save is in progress.
-   Preserve entered values if validation fails.
-   On successful save, close the form and show the task in the selected
    day's list.
-   Provide brief confirmation without a blocking success dialog.

### 9.4 Validation

-   Empty or whitespace-only title must be rejected.
-   Trim unnecessary whitespace from the title.
-   Reject invalid date/time values.
-   If an estimate is entered, it must be greater than zero and within
    supported duration limits.
-   Category must be a valid configured category.
-   Display a specific, understandable message next to the invalid
    field.
-   Never silently replace an invalid value with a different one.

### 9.5 Duplicate task handling

The app should not prevent users from intentionally creating two tasks
with the same title. If the same title and date already exist, it may
show a non-blocking reminder, but must allow the user to continue.

## 10. Task list and ordering

### 10.1 Task row

Each task row should show:

-   Completion checkbox/control.
-   Task title.
-   Category as secondary information.
-   Planned time if present.
-   Estimated duration if present.
-   Start Focus action where appropriate.
-   Overflow menu for less frequent actions.

Keep the title visually dominant. Avoid displaying empty metadata
labels.

### 10.2 Default ordering

Use a stable, understandable ordering:

1.  Incomplete tasks before completed tasks.
2.  Tasks with planned times ordered by planned time.
3.  Tasks without planned times ordered consistently after scheduled
    tasks, using creation order or an explicit user-defined order.
4.  Completed tasks grouped below incomplete tasks, with a clear
    Completed label or section.

If the user manually reorders tasks, preserve that order and do not
unexpectedly reshuffle the list after every refresh. The final ordering
strategy must be implemented consistently.

### 10.3 Task completion

-   Tapping the completion control marks the task complete for its
    planned date.
-   Show immediate visual feedback.
-   Persist the change and update the daily summary.
-   Completed tasks remain available and can be reopened.
-   Do not delete completed tasks automatically.
-   Do not automatically finish a focus session when its task is marked
    complete; the session lifecycle is controlled by the Focus module.
-   If saving fails, restore the prior state and explain the error.

### 10.4 Reopening a task

-   The user can mark a completed task incomplete.
-   Update the completion summary after saving.
-   Do not delete any linked focus-session history.

## 11. Task detail and editing

Tapping a task row opens a task detail/edit screen or a clearly
structured edit sheet.

The user must be able to: - Change the title. - Change category. -
Change planned date. - Change or remove planned time. - Change or remove
estimated duration. - Save or cancel changes. - Start a linked focus
session. - Mark the task complete or incomplete. - Delete the task.

### Editing rules

-   Pre-fill the form with current values.
-   Do not discard changes without warning if the user attempts to leave
    with unsaved edits.
-   Updating a task must update the corresponding date's plan and
    summaries.
-   If the planned date changes, the task must no longer count in the
    previous date's planned-task total and must count on the new date.
-   Historical focus sessions linked to the task must remain intact
    according to the data-retention rule in `11_DATA_ARCHITECTURE.md`.

## 12. Delete task flow

-   Provide Delete in the task overflow menu or edit screen.
-   Ask for confirmation or provide a reliable undo action.
-   Clearly state that the task will be removed from the plan.
-   Do not silently delete linked focus-session records.
-   The exact relationship behaviour for deleted tasks must follow the
    data architecture decision: preserve historical session details and
    avoid broken references.
-   After deletion, update the list and daily summary.

Suggested confirmation copy:

> Delete this task? Its task details will be removed from your plan.
> Recorded focus sessions will be handled according to your activity
> history settings.

Do not use this exact copy until the final historical-reference
behaviour is confirmed.

## 13. Starting Focus from a task

Each eligible incomplete task may provide a Start Focus action.

Expected flow: 1. User taps the task's Start Focus action. 2. The app
opens or starts the focus flow with that task already selected. 3. The
task title and task ID are passed to the Focus module; the user must not
re-enter the task title. 4. The Focus module checks whether another
session is already active. 5. If a session is already active, the app
explains the situation and offers the appropriate route to the active
session. 6. The new session is linked to the task only after the session
is successfully started.

The Focus module owns timer state, pause/resume, duration calculations,
and session history. Daily Planning must not implement a separate timer.

## 14. Filters and search

### V1 baseline

-   Date selection is required.
-   Category filtering is desirable if it can be implemented without
    clutter.
-   Search may be deferred if the user's task volume is expected to
    remain small in V1.

If category filtering is provided: - Categories must match the
configured task categories. - Provide an obvious way to clear the
filter. - Empty filter results must not be confused with a day that has
no tasks.

Do not add advanced filtering controls that make routine planning
slower.

## 15. Empty, loading, error, and success states

### No tasks for selected date

Show: - A concise message such as "No tasks planned for this day". - A
primary Add Task action. - No fake task rows or progress chart.

### No estimates

If tasks exist but none has an estimated duration, show "No time
estimates" rather than `0m` in a way that implies the user planned zero
minutes.

### Loading

-   Keep the screen structure stable.
-   Use lightweight placeholders if required.
-   Avoid showing sample tasks while real data loads.

### Error

-   Explain whether tasks could not be loaded or a save/update failed.
-   Preserve unsaved form values.
-   Offer retry when possible.
-   Do not convert an error into an empty list.

### Success

-   Show the saved task or updated state promptly.
-   Update summaries and previews.
-   Use brief non-blocking feedback.

## 16. Data ownership and relationships

Daily Planning owns task records and their planning/completion
attributes.

The Focus module owns focus-session records. A focus session may
optionally reference a task.

Important rules: - A task is not a focus session. - Planned duration is
not actual focused duration. - Completing a task does not create a focus
session. - Starting a focus session does not automatically mark the task
complete. - Deleting or editing a task must not silently erase
historical focus activity. - Dashboard and Insights read task data; they
must not maintain duplicate task records.

The detailed schema, identifiers, deletion behaviour, and date/time
storage strategy belong in `11_DATA_ARCHITECTURE.md`.

## 17. Notifications and reminders

Notifications are not required for the core V1 planning workflow.

Do not request notification permission simply because a task has a
planned time. If reminders are added later: - They must be explicitly
enabled by the user. - The user must understand what will be
scheduled. - Denying notifications must not affect task creation or
completion. - Reminder timing, editing, cancellation, and timezone
behaviour must be specified separately.

## 18. Accessibility and usability

-   Task completion controls must have accessible labels that include
    the task title and current state.
-   Make task rows and overflow actions easy to use with one hand.
-   Do not rely only on strikethrough or color to indicate completion.
-   Support larger text without clipping titles or hiding actions.
-   Keep category and time metadata readable but visually secondary.
-   Ensure date and time pickers are understandable and
    keyboard-accessible where applicable.
-   Preserve a logical screen-reader reading order.

## 19. Acceptance criteria

### Viewing and date navigation

-   [ ] The screen opens on today's local date by default.
-   [ ] The user can navigate to another date.
-   [ ] The selected date is clearly visible.
-   [ ] Tasks and summary values correspond to the same selected date.
-   [ ] The screen handles dates with no tasks correctly.

### Creating tasks

-   [ ] The user can create a task with only the required title and
    default category/date.
-   [ ] Planned time and estimated duration are optional.
-   [ ] Invalid titles and invalid estimates are rejected with clear
    messages.
-   [ ] The task appears on the correct date after saving.
-   [ ] Duplicate titles are allowed.

### Managing tasks

-   [ ] The user can edit every supported task field.
-   [ ] The user can complete and reopen a task.
-   [ ] The user can delete a task with appropriate protection against
    accidental deletion.
-   [ ] Summaries update after every successful change.
-   [ ] Historical focus sessions are not silently deleted or corrupted.

### Focus integration

-   [ ] A task can start a linked focus session.
-   [ ] The task title is passed to the Focus module without re-entry.
-   [ ] Daily Planning does not run its own timer.
-   [ ] An already-active session is handled clearly.

### UX quality

-   [ ] Common task creation requires minimal input.
-   [ ] The screen is readable on small and large Android phones.
-   [ ] Empty, loading, error, and success states are defined.
-   [ ] The screen works in Light and Dark themes.
-   [ ] Touch targets and accessibility labels are adequate.
-   [ ] No fake tasks or analytics are displayed.

## 20. Decisions to resolve in architecture or later design

1.  Final persistence model and task identifier strategy.
2.  Whether manual task reordering is included in V1.
3.  Whether category filtering and task search are included in the
    initial release.
4.  Exact maximum title length and estimated-duration limits.
5.  Exact deletion and historical focus-session reference behaviour.
6.  Whether task reminders are explicitly deferred from V1.
7.  How a task's planned date is stored and interpreted across timezone
    changes.
8.  Whether task creation supports optional notes in a later iteration.

These decisions must be resolved once and reflected consistently in the
data model, technical design, and tests.

## 21. Related documents

-   `01_PRODUCT_OVERVIEW.md` --- product direction and V1 scope.
-   `02_PRODUCT_REQUIREMENTS.md` --- product requirements and acceptance
    criteria.
-   `03_UI_UX_GUIDELINES.md` --- visual and interaction guidelines.
-   `04_HOME_DASHBOARD.md` --- Home task preview and Add Task entry
    point.
-   `06_ACTIVE_WORK_FOCUS.md` --- linked focus session lifecycle.
-   `07_DAILY_HABITS.md` --- recurring habits, separate from tasks.
-   `11_DATA_ARCHITECTURE.md` --- persistence, relationships, and date
    rules.
-   `13_IMPLEMENTATION_TESTING.md` --- implementation and test coverage.


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

### Task classification addendum
Add a manual required life-area selection to Task create/edit: Health & Lifestyle, Career & Productivity, or Finance & Wealth. Tasks remain dynamic daily responsibilities. Task completion analytics use the selected planned date and completed status; a task is counted once. No priority weighting is required in V1.
