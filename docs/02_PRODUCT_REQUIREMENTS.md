# Life Analytics --- Product Requirements

**Document:** `02_PRODUCT_REQUIREMENTS.md`\
**Product:** Life Analytics\
**Version:** 1.0 (V1 scope)\
**Status:** Initial requirements baseline\
**Platform:** Android\
**Primary user:** Single personal user

------------------------------------------------------------------------

## 1. Purpose

This document defines the functional requirements, business rules,
validations, and acceptance criteria for Life Analytics V1.

It translates the product direction in `01_PRODUCT_OVERVIEW.md` into
requirements that can guide UI design, implementation, and testing.
Individual screen documents may add detail, but they must not contradict
the requirements in this document.

## 2. Scope

### 2.1 Included in V1

-   Home Dashboard
-   Daily Planning and task management
-   Active Work & Focus Timer
-   Daily Habits
-   Finance
-   Life Analytics / Insights
-   Daily Review
-   Settings and permission handling

### 2.2 Excluded from V1

-   Learning Process or learning progress tracking
-   Career Goal Tracker
-   AI recommendations or AI assistant
-   Voice-based data entry
-   Automatic distraction detection
-   Multi-user, social, or team features
-   Web or desktop application

Any excluded capability requires a separate product decision before it
can be added.

## 3. Requirement conventions

Each requirement has a unique ID:

-   `GEN` --- general application requirements
-   `NAV` --- navigation
-   `DASH` --- Home Dashboard
-   `TASK` --- Daily Planning and tasks
-   `FOCUS` --- Active Work & Focus Timer
-   `HABIT` --- Daily Habits
-   `FIN` --- Finance
-   `INS` --- Life Analytics / Insights
-   `REVIEW` --- Daily Review
-   `SET` --- Settings and permissions
-   `DATA` --- data integrity and cross-module rules

Priority meanings:

-   **Must:** required for V1 acceptance.
-   **Should:** important, but may be deferred if a documented
    implementation constraint exists.
-   **Could:** optional enhancement; not a release blocker.

Unless explicitly stated otherwise, requirements marked Must are part of
V1.

## 4. General application requirements

  -----------------------------------------------------------------------
  ID                      Priority                Requirement
  ----------------------- ----------------------- -----------------------
  GEN-001                 Must                    The application must
                                                  run on Android phones.

  GEN-002                 Must                    The initial product
                                                  must support one
                                                  personal user and must
                                                  not require multi-user
                                                  or team workflows.

  GEN-003                 Must                    The application must
                                                  provide Light and Dark
                                                  themes.

  GEN-004                 Must                    The application must
                                                  use consistent
                                                  navigation, typography,
                                                  spacing, and
                                                  interaction patterns
                                                  across screens.

  GEN-005                 Must                    The application must
                                                  provide clear loading,
                                                  empty, success, and
                                                  error states where
                                                  relevant.

  GEN-006                 Must                    User-entered records
                                                  must be editable and
                                                  deletable where the
                                                  relevant module permits
                                                  it.

  GEN-007                 Must                    The app must validate
                                                  user input before
                                                  saving records.

  GEN-008                 Must                    The app must not
                                                  fabricate activity,
                                                  transaction, habit, or
                                                  analytics data.

  GEN-009                 Must                    The app must make
                                                  frequent actions
                                                  possible with as few
                                                  steps as practical.

  GEN-010                 Should                  The app should support
                                                  a clear first-use
                                                  experience when no
                                                  records exist.
  -----------------------------------------------------------------------

## 5. Navigation requirements

  -----------------------------------------------------------------------
  ID                      Priority                Requirement
  ----------------------- ----------------------- -----------------------
  NAV-001                 Must                    Primary bottom
                                                  navigation must provide
                                                  access to Home,
                                                  Finance, Activity, and
                                                  Insights.

  NAV-002                 Must                    Activity must provide
                                                  access to Daily
                                                  Planning, Active Work,
                                                  and Time Audit/history
                                                  views as defined by the
                                                  screen specifications.

  NAV-003                 Must                    Daily Habits must be
                                                  accessible from Home
                                                  and through a clearly
                                                  discoverable route.

  NAV-004                 Must                    Daily Review must be
                                                  accessible from a clear
                                                  daily workflow entry
                                                  point.

  NAV-005                 Must                    Settings must be
                                                  accessible from the app
                                                  header or another
                                                  consistent global entry
                                                  point.

  NAV-006                 Must                    Navigation must
                                                  preserve user-entered
                                                  data when the user
                                                  moves between screens.
  -----------------------------------------------------------------------

## 6. Home Dashboard requirements

### 6.1 Summary metrics

  -----------------------------------------------------------------------
  ID                      Priority                Requirement
  ----------------------- ----------------------- -----------------------
  DASH-001                Must                    Home must show the
                                                  current date and a
                                                  suitable greeting or
                                                  header.

  DASH-002                Must                    Home must show Tasks
                                                  Completed as completed
                                                  tasks divided by total
                                                  tasks planned for the
                                                  selected day.

  DASH-003                Must                    Home must show Focused
                                                  Time as the sum of
                                                  valid focus-session
                                                  time for the selected
                                                  day.

  DASH-004                Must                    Paused intervals must
                                                  not be included in
                                                  Focused Time.

  DASH-005                Must                    Home must show Today's
                                                  Expenses as the sum of
                                                  expense transactions
                                                  recorded for the
                                                  selected day.

  DASH-006                Must                    Home must show Habits
                                                  Completed as completed
                                                  habits divided by
                                                  habits scheduled for
                                                  that day.

  DASH-007                Must                    Each metric must be
                                                  calculated from its
                                                  source module records
                                                  rather than separately
                                                  entered or maintained
                                                  dashboard data.

  DASH-008                Must                    When no relevant
                                                  records exist, the
                                                  dashboard must show a
                                                  clear zero or empty
                                                  state rather than
                                                  invented sample data.
  -----------------------------------------------------------------------

### 6.2 Quick actions and previews

  -----------------------------------------------------------------------
  ID                      Priority                Requirement
  ----------------------- ----------------------- -----------------------
  DASH-009                Must                    Home must provide a
                                                  quick action to start a
                                                  focus session.

  DASH-010                Must                    Home must provide a
                                                  quick action to add a
                                                  task.

  DASH-011                Must                    Home must provide a
                                                  quick action to add an
                                                  expense.

  DASH-012                Must                    Home must show a
                                                  preview of today's
                                                  tasks and their
                                                  completion states.

  DASH-013                Must                    Home must show a
                                                  preview of today's
                                                  scheduled habits and
                                                  their completion
                                                  states.

  DASH-014                Must                    View All actions must
                                                  navigate to the
                                                  corresponding full
                                                  module screen.
  -----------------------------------------------------------------------

## 7. Daily Planning and task requirements

### 7.1 Task creation and management

  -----------------------------------------------------------------------
  ID                      Priority                Requirement
  ----------------------- ----------------------- -----------------------
  TASK-001                Must                    The user must be able
                                                  to create a task for a
                                                  selected date,
                                                  defaulting to today.

  TASK-002                Must                    A task must have a
                                                  non-empty title.

  TASK-003                Must                    A task must support a
                                                  category, with the
                                                  initial categories
                                                  Study, Work, Personal,
                                                  and Other.

  TASK-004                Must                    A task may include an
                                                  optional planned time.

  TASK-005                Must                    A task may include an
                                                  optional estimated
                                                  duration.

  TASK-006                Must                    The user must be able
                                                  to edit a task.

  TASK-007                Must                    The user must be able
                                                  to mark a task complete
                                                  or incomplete.

  TASK-008                Must                    The user must be able
                                                  to delete a task after
                                                  an appropriate
                                                  confirmation or undo
                                                  opportunity.

  TASK-009                Must                    Tasks must be
                                                  associated with their
                                                  planned date.

  TASK-010                Must                    The task list must
                                                  distinguish completed
                                                  and incomplete tasks.

  TASK-011                Should                  The user should be able
                                                  to reorder tasks within
                                                  a day's plan.
  -----------------------------------------------------------------------

### 7.2 Planning summaries

  -----------------------------------------------------------------------
  ID                      Priority                Requirement
  ----------------------- ----------------------- -----------------------
  TASK-012                Must                    The screen must show
                                                  the number of completed
                                                  tasks and total tasks
                                                  for the selected day.

  TASK-013                Must                    The screen must show
                                                  total planned duration
                                                  when estimates are
                                                  available.

  TASK-014                Must                    Tasks without an
                                                  estimated duration must
                                                  remain valid and must
                                                  not be assigned an
                                                  invented duration.

  TASK-015                Must                    The user must be able
                                                  to start a focus
                                                  session from a task.
  -----------------------------------------------------------------------

## 8. Active Work & Focus Timer requirements

### 8.1 Starting a session

  -----------------------------------------------------------------------
  ID                      Priority                Requirement
  ----------------------- ----------------------- -----------------------
  FOCUS-001               Must                    The user must be able
                                                  to start a session
                                                  linked to an existing
                                                  planned task.

  FOCUS-002               Must                    The user must be able
                                                  to create a new task as
                                                  part of starting a
                                                  session.

  FOCUS-003               Must                    The user must be able
                                                  to start a general
                                                  session without linking
                                                  it to a task.

  FOCUS-004               Must                    A general session may
                                                  include an optional
                                                  description.

  FOCUS-005               Must                    The interface must
                                                  clearly identify the
                                                  task or activity
                                                  associated with the
                                                  active session.

  FOCUS-006               Must                    Only one active focus
                                                  session may exist at a
                                                  time.

  FOCUS-007               Must                    If a session is already
                                                  active, the app must
                                                  not silently start a
                                                  second session.
  -----------------------------------------------------------------------

### 8.2 Timer controls and duration

  -----------------------------------------------------------------------
  ID                      Priority                Requirement
  ----------------------- ----------------------- -----------------------
  FOCUS-008               Must                    The timer must support
                                                  Start, Pause, Resume,
                                                  and Finish actions.

  FOCUS-009               Must                    Paused intervals must
                                                  be excluded from the
                                                  session's focused
                                                  duration.

  FOCUS-010               Must                    Finishing a session
                                                  must save its start
                                                  time, end time, and
                                                  calculated focused
                                                  duration.

  FOCUS-011               Must                    A session must retain
                                                  its optional task
                                                  relationship.

  FOCUS-012               Must                    A general session
                                                  without a task must
                                                  still contribute to the
                                                  daily Focused Time
                                                  metric.

  FOCUS-013               Must                    The app must handle app
                                                  backgrounding and
                                                  process recreation
                                                  without silently losing
                                                  an active session's
                                                  timing state.

  FOCUS-014               Must                    The user must be able
                                                  to review previous
                                                  sessions.

  FOCUS-015               Must                    Session history must
                                                  identify the activity
                                                  or task, date,
                                                  start/end information
                                                  where available,
                                                  duration, and status.

  FOCUS-016               Must                    The app must not
                                                  describe timer duration
                                                  as proof of
                                                  distraction-free work.
  -----------------------------------------------------------------------

### 8.3 Session integrity

  -----------------------------------------------------------------------
  ID                      Priority                Requirement
  ----------------------- ----------------------- -----------------------
  FOCUS-017               Must                    The app must prevent
                                                  negative or impossible
                                                  durations.

  FOCUS-018               Must                    An unfinished or
                                                  interrupted session
                                                  must have an explicit
                                                  recoverable state or a
                                                  clearly defined
                                                  recovery outcome.

  FOCUS-019               Must                    Editing or correcting a
                                                  session must update all
                                                  dependent summaries.
  -----------------------------------------------------------------------

## 9. Daily Habits requirements

  -----------------------------------------------------------------------
  ID                      Priority                Requirement
  ----------------------- ----------------------- -----------------------
  HABIT-001               Must                    The user must be able
                                                  to create a habit with
                                                  a non-empty name.

  HABIT-002               Must                    A habit must support a
                                                  schedule that
                                                  determines which days
                                                  it is due.

  HABIT-003               Must                    The user must be able
                                                  to edit and deactivate
                                                  or delete a habit.

  HABIT-004               Must                    The user must be able
                                                  to mark a due habit
                                                  complete for a specific
                                                  date.

  HABIT-005               Must                    The user must be able
                                                  to correct a habit's
                                                  completion state for a
                                                  date.

  HABIT-006               Must                    Habit completion must
                                                  be stored by habit and
                                                  date, not as a single
                                                  permanent completed
                                                  flag.

  HABIT-007               Must                    The daily habit summary
                                                  denominator must
                                                  include only habits
                                                  scheduled for that
                                                  date.

  HABIT-008               Must                    A habit not scheduled
                                                  for a date must not be
                                                  counted as missed for
                                                  that date.

  HABIT-009               Must                    Habit completion must
                                                  remain separate from
                                                  task completion.

  HABIT-010               Should                  The user should be able
                                                  to review habit
                                                  completion over a
                                                  selected date range.
  -----------------------------------------------------------------------

## 10. Finance requirements

### 10.1 Transactions

  -----------------------------------------------------------------------
  ID                      Priority                Requirement
  ----------------------- ----------------------- -----------------------
  FIN-001                 Must                    The user must be able
                                                  to record an income
                                                  transaction.

  FIN-002                 Must                    The user must be able
                                                  to record an expense
                                                  transaction.

  FIN-003                 Must                    A transaction must have
                                                  a positive amount
                                                  greater than zero.

  FIN-004                 Must                    A transaction must have
                                                  a date.

  FIN-005                 Must                    A transaction must have
                                                  a category appropriate
                                                  to its type.

  FIN-006                 Must                    A transaction may
                                                  include an optional
                                                  note or description.

  FIN-007                 Must                    The user must be able
                                                  to edit and delete a
                                                  transaction.

  FIN-008                 Must                    The app must clearly
                                                  distinguish income from
                                                  expenses.

  FIN-009                 Must                    The app must use one
                                                  explicitly selected
                                                  currency consistently
                                                  throughout V1.

  FIN-010                 Must                    The app must not
                                                  silently change a
                                                  transaction's amount,
                                                  date, or type.
  -----------------------------------------------------------------------

### 10.2 Finance summaries

  -----------------------------------------------------------------------
  ID                      Priority                Requirement
  ----------------------- ----------------------- -----------------------
  FIN-011                 Must                    The app must calculate
                                                  income and expense
                                                  totals from saved
                                                  transactions.

  FIN-012                 Must                    Today's Expenses on
                                                  Home must include
                                                  expense transactions
                                                  dated today according
                                                  to the app's
                                                  date-handling rules.

  FIN-013                 Must                    The user must be able
                                                  to view transactions
                                                  for a selected period.

  FIN-014                 Must                    The app must
                                                  distinguish transaction
                                                  totals from account
                                                  balance.

  FIN-015                 Must                    Any displayed balance
                                                  must follow a
                                                  documented calculation
                                                  rule and must not be
                                                  inferred from
                                                  incomplete transaction
                                                  history.
  -----------------------------------------------------------------------

## 11. Life Analytics / Insights requirements

  -----------------------------------------------------------------------
  ID                      Priority                Requirement
  ----------------------- ----------------------- -----------------------
  INS-001                 Must                    Insights must provide
                                                  summaries for supported
                                                  date ranges, at minimum
                                                  daily, weekly, and
                                                  monthly views.

  INS-002                 Must                    Insights must use
                                                  source records from
                                                  tasks, focus sessions,
                                                  habits, and finance.

  INS-003                 Must                    Task analytics must
                                                  distinguish planned
                                                  tasks from completed
                                                  tasks.

  INS-004                 Must                    Focus analytics must
                                                  exclude paused
                                                  intervals.

  INS-005                 Must                    Habit analytics must
                                                  account for each
                                                  habit's schedule and
                                                  days due.

  INS-006                 Must                    Finance analytics must
                                                  distinguish income and
                                                  expenses.

  INS-007                 Must                    Charts must use
                                                  accurate labels, date
                                                  ranges, and units.

  INS-008                 Must                    Empty or insufficient
                                                  data must be
                                                  represented clearly.

  INS-009                 Must                    Insights must not imply
                                                  causation from simple
                                                  correlations or
                                                  summaries.

  INS-010                 Should                  The user should be able
                                                  to compare equivalent
                                                  periods, such as one
                                                  week with the previous
                                                  week.
  -----------------------------------------------------------------------

## 12. Daily Review requirements

  -----------------------------------------------------------------------
  ID                      Priority                Requirement
  ----------------------- ----------------------- -----------------------
  REVIEW-001              Must                    The user must be able
                                                  to create a review for
                                                  a specific date.

  REVIEW-002              Must                    The review must support
                                                  optional written
                                                  reflection.

  REVIEW-003              Must                    The review must show or
                                                  link to the day's
                                                  relevant task, focus,
                                                  habit, and finance
                                                  summaries.

  REVIEW-004              Must                    The user must be able
                                                  to edit a saved review.

  REVIEW-005              Must                    The app must allow a
                                                  review to be saved even
                                                  if some modules have no
                                                  data for that day.

  REVIEW-006              Must                    Saving a review must
                                                  not create duplicate
                                                  tasks, sessions,
                                                  transactions, or habit
                                                  records.
  -----------------------------------------------------------------------

## 13. Settings and permissions requirements

  -----------------------------------------------------------------------
  ID                      Priority                Requirement
  ----------------------- ----------------------- -----------------------
  SET-001                 Must                    The user must be able
                                                  to switch between Light
                                                  and Dark themes.

  SET-002                 Must                    The app must explain
                                                  the purpose of a
                                                  permission before
                                                  requesting it when the
                                                  request is initiated by
                                                  the app.

  SET-003                 Must                    The app must not
                                                  request permissions
                                                  unrelated to an enabled
                                                  V1 feature.

  SET-004                 Must                    Core manual tracking
                                                  must remain usable when
                                                  optional permissions
                                                  are denied.

  SET-005                 Must                    The app must provide a
                                                  clear route to relevant
                                                  Android settings when a
                                                  denied permission must
                                                  be changed by the user.

  SET-006                 Should                  The user should be able
                                                  to configure the
                                                  preferred currency
                                                  before recording
                                                  finance transactions.
  -----------------------------------------------------------------------

## 14. Cross-module and data integrity requirements

  -----------------------------------------------------------------------
  ID                      Priority                Requirement
  ----------------------- ----------------------- -----------------------
  DATA-001                Must                    Each business record
                                                  must have a stable
                                                  unique identifier.

  DATA-002                Must                    Dashboard and Insights
                                                  values must be derived
                                                  from source records.

  DATA-003                Must                    Updating or deleting a
                                                  source record must
                                                  update dependent
                                                  summaries.

  DATA-004                Must                    A focus session may
                                                  reference a task, but
                                                  the task reference must
                                                  be optional.

  DATA-005                Must                    Deleting or changing a
                                                  task must not silently
                                                  corrupt historical
                                                  focus-session records.
                                                  The relationship and
                                                  deletion behaviour must
                                                  be defined in the data
                                                  model.

  DATA-006                Must                    Habit completion must
                                                  reference a habit and a
                                                  specific date.

  DATA-007                Must                    Finance transactions
                                                  must retain their
                                                  transaction type,
                                                  amount, currency, and
                                                  date.

  DATA-008                Must                    Date and time
                                                  calculations must use a
                                                  documented timezone
                                                  strategy and handle day
                                                  boundaries
                                                  consistently.

  DATA-009                Must                    The app must prevent
                                                  duplicate saves caused
                                                  by repeated taps or
                                                  retry behaviour where
                                                  practical.

  DATA-010                Must                    User-facing totals must
                                                  be recalculated
                                                  consistently after
                                                  record creation,
                                                  editing, or deletion.

  DATA-011                Must                    The app must
                                                  distinguish missing
                                                  data from a true
                                                  numeric zero where that
                                                  distinction affects
                                                  interpretation.
  -----------------------------------------------------------------------

## 15. Validation rules

The following baseline validations apply across V1. Detailed field-level
rules belong in the relevant module specifications.

  -----------------------------------------------------------------------
  Data                                Validation
  ----------------------------------- -----------------------------------
  Task title                          Required; must contain
                                      non-whitespace characters.

  Task category                       Must be one of the configured
                                      categories or a valid user-defined
                                      category if that capability is
                                      later approved.

  Estimated duration                  Optional; if entered, must be
                                      greater than zero and within
                                      supported duration limits.

  Habit name                          Required; must contain
                                      non-whitespace characters.

  Habit schedule                      Must contain at least one valid
                                      scheduled day.

  Transaction amount                  Required; numeric and greater than
                                      zero; precision must follow the
                                      selected currency.

  Transaction type                    Must be Income or Expense.

  Transaction date                    Required and valid.

  Focus duration                      Must not be negative; paused
                                      intervals are excluded.

  Review text                         Optional; enforce a reasonable
                                      maximum length in the detailed UI
                                      specification.
  -----------------------------------------------------------------------

Validation errors must be understandable and shown near the relevant
field where practical. Invalid input must not be silently converted into
a different value.

## 16. Non-functional requirements

  -----------------------------------------------------------------------
  ID                      Priority                Requirement
  ----------------------- ----------------------- -----------------------
  NFR-001                 Must                    Common screens and
                                                  actions must remain
                                                  responsive on supported
                                                  Android devices.

  NFR-002                 Must                    The app must handle
                                                  temporary
                                                  interruptions,
                                                  including backgrounding
                                                  and returning to the
                                                  app.

  NFR-003                 Must                    Local date and time
                                                  handling must be
                                                  consistent across
                                                  modules.

  NFR-004                 Must                    Personal records must
                                                  be stored and accessed
                                                  using appropriate
                                                  Android security
                                                  practices.

  NFR-005                 Must                    The UI must remain
                                                  usable in both Light
                                                  and Dark themes.

  NFR-006                 Must                    Interactive controls
                                                  must have clear labels
                                                  and touch-friendly
                                                  target sizes.

  NFR-007                 Must                    Important actions such
                                                  as delete or
                                                  destructive reset must
                                                  have suitable
                                                  confirmation or
                                                  recovery behaviour.

  NFR-008                 Should                  The app should support
                                                  accessibility features
                                                  such as scalable text
                                                  and meaningful content
                                                  descriptions.

  NFR-009                 Must                    The app must not depend
                                                  on network access for
                                                  core manual record
                                                  creation and viewing
                                                  unless a later approved
                                                  architecture decision
                                                  requires it.
  -----------------------------------------------------------------------

## 17. Acceptance criteria by module

### Home Dashboard

-   [ ] Shows the four agreed metrics: Tasks Completed, Focused Time,
    Today's Expenses, and Habits Completed.
-   [ ] Each metric matches the source records for the displayed date.
-   [ ] Quick actions navigate to the correct workflows.
-   [ ] Task and habit previews show the correct states.
-   [ ] Empty states do not contain fabricated sample data.

### Daily Planning

-   [ ] A user can create, edit, complete, reopen, and delete a task.
-   [ ] A task can be saved without an optional time or duration
    estimate.
-   [ ] Daily completion and planned-duration summaries are correct.
-   [ ] A task can launch a linked focus session.

### Active Work & Focus Timer

-   [ ] A user can start a task-linked, newly created task, or general
    session.
-   [ ] Only one session can be active at a time.
-   [ ] Pause and resume work correctly.
-   [ ] Paused time is excluded from saved duration and dashboard
    totals.
-   [ ] A finished session appears in history and contributes to the
    correct date's total.
-   [ ] An active session survives ordinary app backgrounding and has a
    defined recovery path after process interruption.

### Daily Habits

-   [ ] A user can create a habit and choose its scheduled days.
-   [ ] Completion can be recorded and corrected for a particular date.
-   [ ] Only habits due on that date appear in the daily denominator.
-   [ ] Habit totals match the stored completion records.

### Finance

-   [ ] A user can add, edit, and delete income and expense
    transactions.
-   [ ] Invalid or zero/negative amounts are rejected.
-   [ ] Daily and period totals match saved transactions.
-   [ ] Income and expenses are distinguishable.

### Life Analytics / Insights

-   [ ] Daily, weekly, and monthly views use the correct date
    boundaries.
-   [ ] Each chart is traceable to source records and has clear units.
-   [ ] Paused focus time is excluded.
-   [ ] Habit schedule rules are respected.
-   [ ] Insufficient data is communicated clearly.

### Daily Review

-   [ ] A user can save and edit a review for a date.
-   [ ] The review can be saved even when some module data is absent.
-   [ ] The review does not duplicate underlying records.

### Settings and permissions

-   [ ] Light and Dark themes can be selected.
-   [ ] Permission explanations are clear.
-   [ ] Denying an optional permission does not block unrelated manual
    tracking.

## 18. Open decisions to resolve before implementation

The following decisions must be settled in the relevant detailed
documents before the affected feature is implemented:

1.  **Technology stack:** final Android framework, language, and project
    structure.
2.  **Storage:** local database choice and schema migration strategy.
3.  **Backup and export:** whether V1 includes manual export, backup, or
    restore.
4.  **Currency:** confirm default currency and whether the user can
    change it after transactions exist.
5.  **Finance balance:** decide whether V1 tracks a starting balance or
    only income/expense totals.
6.  **Task recurrence:** confirm whether recurring tasks are excluded
    from V1 or supported.
7.  **Habit editing:** define how schedule changes affect historical
    dates.
8.  **Focus interruption:** define exact recovery behaviour after
    force-stop, reboot, or device time changes.
9.  **Date and timezone:** define storage timestamps and local-day
    aggregation rules.
10. **Data deletion:** define whether deleting a task preserves its
    linked historical focus sessions and how that relationship is
    displayed.

These are explicit design decisions, not permission to make arbitrary
assumptions during implementation.

## 19. Traceability and change control

-   `01_PRODUCT_OVERVIEW.md` defines the product direction and V1 scope.
-   This document defines cross-module product requirements.
-   `03_UI_UX_GUIDELINES.md` and the individual module documents define
    screen-level behaviour and interaction details.
-   `11_DATA_ARCHITECTURE.md` defines persistence, relationships, and
    data integrity.
-   `12_TECHNICAL_ARCHITECTURE.md` defines implementation architecture.
-   `13_IMPLEMENTATION_TESTING.md` maps requirements to implementation
    phases and tests.

When a requirement changes, update this document and all affected
module, data, design, and test documents. Do not silently expand V1
scope.


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

### V1 analytics requirements addendum
Habit and Task create/edit flows must expose a required manual life-area selector for new/edited records. Existing unclassified records remain valid and are excluded from category-specific scoring until classified; they must not be silently assigned. Analytics must expose metric definitions, numerator/denominator, period, and data coverage.
