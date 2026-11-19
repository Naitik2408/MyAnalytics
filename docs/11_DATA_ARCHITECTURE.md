# Life Analytics --- Data Architecture

**Document:** `11_DATA_ARCHITECTURE.md`\
**Product:** Life Analytics\
**Version:** 1.1 (V1 — data protection update)\
**Status:** Data architecture specification\
**Platform:** Android-only\
**Related documents:** `01_PRODUCT_OVERVIEW.md` through
`10_DAILY_REVIEW.md`, `12_TECHNICAL_ARCHITECTURE.md`,
`13_IMPLEMENTATION_TESTING.md`

------------------------------------------------------------------------

## 1. Purpose

This document defines how Life Analytics stores, relates, validates, and
retrieves application data.

The architecture must support the product's central UX principle:

> Minimum input, maximum meaningful output.

Users should enter information once. Dashboards, daily summaries,
reviews, and analytics must derive their values from the same source
records rather than maintaining duplicate copies.

This document defines: - Core entities and their responsibilities. -
Relationships between modules. - Recommended local-first storage
model. - Date, time, and timezone rules. - Data integrity and deletion
behaviour. - Calculation ownership and aggregate strategy. - Privacy,
backup, and migration expectations.

The exact database library and implementation details are finalized in
`12_TECHNICAL_ARCHITECTURE.md`.

## 2. Architecture principles

1.  **Single source of truth:** Each fact is owned by one entity/module.
2.  **Local-first:** The Room database on the Android device remains the primary source of truth. A separate verified backup copy must exist outside the app sandbox so uninstalling the app does not destroy the only copy.
3.  **No duplicate entry:** Derived screens read existing records.
4.  **Stable IDs:** Records use persistent unique identifiers, not list
    positions.
5.  **Explicit timestamps:** Store event times consistently and preserve
    the user's local date where needed.
6.  **Historical integrity:** Editing a current schedule must not
    rewrite historical records.
7.  **Safe calculations:** Unknown or missing data must not be silently
    converted to zero.
8.  **Deletion is deliberate:** Deleting one record must not
    unexpectedly delete unrelated records.
9.  **Schema evolution:** Database changes must be versioned and
    migrated safely.
10. **Privacy by default:** Personal reflection and finance records
    remain local in V1.

## 3. Recommended storage model

### 3.1 Local database

Use a relational local database for structured application data. SQLite
is the recommended storage foundation because the data contains clear
relationships, needs reliable transactions, and must work offline.

A repository/data-access layer should sit between the UI and database.
UI screens must not issue ad hoc database queries or contain business
calculation logic.

Recommended flow:

``` text
Android UI
   │
   ▼
ViewModel / Screen State
   │
   ▼
Use Cases / Domain Services
   │
   ▼
Repository Interfaces
   │
   ▼
Local Database (SQLite)
   │
   ├── Tasks
   ├── Focus Sessions and Pauses
   ├── Habits and Schedule History
   ├── Habit Completions
   ├── Finance Transactions
   └── Daily Reviews
```

The exact Android persistence library is a technical-architecture
decision. If Kotlin is selected, Room is a suitable SQLite abstraction.

### 3.2 Local-first behaviour

-   Core create, read, update, and delete operations must work without
    internet access.
-   Core create, read, update, and delete operations must never depend on a network connection.
-   A backup capability is in scope because app-private data is removed when the app is uninstalled.
-   Backup is a separate copy, not a second live source of truth.
-   Cloud synchronization and multi-device editing are out of scope for this stage.
-   The cloud provider and authentication approach must be selected only after the existing project and available provider capabilities are audited.

## 4. Entity overview

V1 has six main data areas:

  -----------------------------------------------------------------------
  Entity                  Owns                    Used by
  ----------------------- ----------------------- -----------------------
  Task                    Planned work and        Planning, Home, Focus,
                          completion state        Review, Insights

  Focus Session           Recorded focus          Focus, Home, Review,
                          intervals and duration  Insights

  Habit                   Habit identity and      Habits, Home, Review,
                          current metadata        Insights

  Habit Schedule Period   Effective-dated         Habits, Review,
                          schedule                Insights

  Habit Completion        Completion for one      Habits, Home, Review,
                          habit on one date       Insights

  Finance Transaction     One income or expense   Finance, Home, Review,
                          record                  Insights

  Daily Review            User-authored           Daily Review, History
                          reflection and review   
                          status                  
  -----------------------------------------------------------------------

A settings/preferences record may store currency, theme, and other
app-level preferences. It must not duplicate module records.

## 5. Common data conventions

### 5.1 Identifiers

-   Every persistent entity has a unique primary key.
-   Use UUIDs or another collision-resistant identifier strategy.
-   IDs remain unchanged when a record is edited.
-   Never use a row number, array index, title, or date as a record's
    primary key.
-   Foreign keys must reference stable IDs.

### 5.2 Timestamps

For events that happen at a specific moment, store timestamps in UTC (or
an equivalent unambiguous instant representation).

Examples: - Focus session start and end. - Transaction creation and
update. - Record creation and update.

Also retain the relevant local date or timezone context where business
rules depend on the user's calendar day.

### 5.3 Calendar dates

A calendar date such as a task's planned date, habit completion date, or
daily review date is not a timestamp. Store it as a date-only value,
such as `YYYY-MM-DD`.

Do not convert date-only values to UTC timestamps and risk shifting the
day.

### 5.4 Timezone

-   V1 uses the device's current local timezone for day-based screens.
-   Store event instants unambiguously.
-   Store the timezone identifier or offset context needed to interpret
    historical events where required.
-   Define a single shared `localDateForEvent` policy in the domain
    layer.
-   Do not let each screen independently decide which day an event
    belongs to.
-   When the device timezone changes, historical event instants must not
    be rewritten.
-   The exact attribution of a focus session crossing midnight must be
    finalized consistently with `06_ACTIVE_WORK_FOCUS.md`.

### 5.5 Money

-   Store amounts as integer minor units (for INR, paise) or an exact
    decimal representation supported safely by the chosen database.
-   Do not use binary floating-point values for financial amounts.
-   Store the currency code with each transaction, even if V1 initially
    supports only one configured currency.
-   Do not silently convert currencies.
-   Currency formatting belongs in the presentation layer.

### 5.6 Optional values

Use null for genuinely unknown or not-provided values. Do not substitute
empty strings, zero, or invented defaults where those values have a
different meaning.

## 6. Entity specifications

The following are logical schemas. Final SQL types and naming
conventions are implementation decisions, but field meaning and
constraints must be preserved.

### 6.1 Task

**Purpose:** Represents one planned piece of work.

  Field                          Type / meaning                       Rule
  ------------------------------ ------------------------------------ ------------------------------------
  `id`                           Unique ID                            Primary key
  `title`                        Text                                 Required, non-empty after trimming
  `category`                     Enum: Study, Work, Personal, Other   Required
  `planned_date`                 Local date                           Required
  `planned_time`                 Local time                           Optional
  `estimated_duration_minutes`   Integer                              Optional, greater than zero
  `status`                       Planned, Completed                   Required
  `completed_at`                 Timestamp                            Set when completed; null otherwise
  `created_at`                   Timestamp                            Required
  `updated_at`                   Timestamp                            Required
  `deleted_at`                   Timestamp                            Optional, if soft deletion is used

Rules: - A task can have zero or more linked focus sessions. -
Completion is controlled by the task record, not inferred from focus
sessions. - Reopening a task clears or updates completion metadata
consistently. - A planned duration is an estimate, not actual focus
time. - If task rescheduling history is required, add a task-date
history table rather than overwriting dates without an audit strategy. -
Avoid adding complex priority, dependency, subtask, or project fields in
V1 unless approved.

### 6.2 Focus Session

**Purpose:** Represents one focus attempt, optionally associated with a
task.

  ----------------------------------------------------------------------------
  Field                        Type / meaning          Rule
  ---------------------------- ----------------------- -----------------------
  `id`                         Unique ID               Primary key

  `task_id`                    Task ID                 Optional foreign key

  `session_type`               Task-linked or General  Required

  `description`                Text                    Optional

  `started_at`                 Timestamp               Required

  `ended_at`                   Timestamp               Null while active

  `status`                     Active, Paused,         Required
                               Completed, Interrupted  

  `focused_duration_seconds`   Integer                 Calculated from valid
                                                       intervals

  `created_at`                 Timestamp               Required

  `updated_at`                 Timestamp               Required
  ----------------------------------------------------------------------------

Rules: - At most one session can be Active or Paused at a time. - A
task-linked session must reference an existing task. - A General session
must not require a task. - Focused duration excludes pauses and
unverified interruption gaps. - A completed session has an end
timestamp. - Interrupted sessions remain distinguishable from normally
completed sessions. - Do not delete a task's linked focus history merely
because the task is deleted.

### 6.3 Focus Pause Interval

**Purpose:** Records pauses within a focus session.

  Field          Type / meaning     Rule
  -------------- ------------------ -------------------------------
  `id`           Unique ID          Primary key
  `session_id`   Focus session ID   Required foreign key
  `paused_at`    Timestamp          Required
  `resumed_at`   Timestamp          Null while pause remains open

Rules: - Pause intervals belong to exactly one session. - A session
cannot have overlapping pause intervals. - Only one open pause interval
may exist for the active session. - Paused time is excluded from focused
duration. - The session's stored duration must be recalculated or
reconciled when pause intervals change.

### 6.4 Habit

**Purpose:** Represents a habit identity, independent of any one day's
completion.

  Field              Type / meaning   Rule
  ------------------ ---------------- -------------
  `id`               Unique ID        Primary key
  `name`             Text             Required
  `is_active`        Boolean          Required
  `created_at`       Timestamp        Required
  `updated_at`       Timestamp        Required
  `deactivated_at`   Timestamp        Optional

Rules: - Deactivation preserves history. - Habit completion is not
stored as a mutable boolean on the habit itself. - Schedule details
belong in effective-dated schedule records. - Renaming a habit should
not create a new habit identity.

### 6.5 Habit Schedule Period

**Purpose:** Stores when a habit is scheduled, preserving previous
schedules.

  -----------------------------------------------------------------------
  Field                   Type / meaning          Rule
  ----------------------- ----------------------- -----------------------
  `id`                    Unique ID               Primary key

  `habit_id`              Habit ID                Required foreign key

  `effective_from`        Local date              Inclusive

  `effective_to`          Local date              Optional, inclusive or
                                                  use a documented
                                                  exclusive convention

  `schedule_type`         Every day, Weekdays,    Required
                          Weekends, Custom        

  `weekdays`              Set of weekday values   Required for Custom;
                                                  normalized consistently

  `created_at`            Timestamp               Required
  -----------------------------------------------------------------------

Rules: - Schedule periods for one habit must not overlap. - A schedule
edit creates a new effective period or closes the old one. - Past dates
continue to use the schedule that was effective on those dates. - A
habit is due only if active and scheduled on the selected date. - Choose
one effective-end convention and use it consistently in code and tests.
Recommended: `effective_to` is exclusive to simplify adjacent periods.

### 6.6 Habit Completion

**Purpose:** Records whether a habit was completed on a specific local
date.

  Field               Type / meaning   Rule
  ------------------- ---------------- ------------------------------------
  `id`                Unique ID        Primary key
  `habit_id`          Habit ID         Required foreign key
  `completion_date`   Local date       Required
  `is_completed`      Boolean          Required
  `completed_at`      Timestamp        Optional; set when marked complete
  `updated_at`        Timestamp        Required

Constraints: - Unique pair: (`habit_id`, `completion_date`). -
Unchecking a habit updates the same dated record; it does not create
another record. - Do not create a missed record for every unchecked
day. - A day not in the schedule is not a missed habit day. - Completion
history must remain available if the habit is later deactivated.

### 6.7 Finance Transaction

**Purpose:** Represents one recorded income or expense.

  Field                Type / meaning                   Rule
  -------------------- -------------------------------- ------------------------------------
  `id`                 Unique ID                        Primary key
  `type`               Income or Expense                Required
  `amount_minor`       Integer minor units              Required, greater than zero
  `currency_code`      Currency code                    Required
  `category`           Text or controlled category ID   Required
  `transaction_date`   Local date                       Required
  `note`               Text                             Optional
  `created_at`         Timestamp                        Required
  `updated_at`         Timestamp                        Required
  `deleted_at`         Timestamp                        Optional, if soft deletion is used

Rules: - Store amount as a positive value; transaction type determines
its meaning. - Expense totals sum Expense records only. - Income totals
sum Income records only. - Net for a period is income minus expenses. -
Net is not an account balance. - Editing a transaction updates the
original record. - Deleting a transaction removes it from calculations
according to the chosen deletion policy.

### 6.8 Daily Review

**Purpose:** Stores user-authored reflection for one local calendar
date.

  Field                Type / meaning       Rule
  -------------------- -------------------- -------------
  `id`                 Unique ID            Primary key
  `review_date`        Local date           Required
  `went_well`          Text                 Optional
  `improve_tomorrow`   Text                 Optional
  `remember`           Text                 Optional
  `status`             Draft or Completed   Required
  `created_at`         Timestamp            Required
  `updated_at`         Timestamp            Required

Constraints: - Unique `review_date` for the single-user V1 model. -
Reflection fields are optional. - Review status does not affect task,
focus, habit, or finance records. - Daily summary values are derived
from source entities, not stored as duplicate fields in this table. -
Editing a review must not modify source records.

### 6.9 App Preferences

**Purpose:** Stores app-level preferences.

Potential fields: - Theme: System, Light, Dark. - Display currency
code. - Preferred week start day. - Optional review reminder preference,
if reminders are later approved. - Schema-compatible settings version.

Rules: - Preferences are not business records. - Do not store passwords
or authentication secrets; V1 does not require an account. - Defaults
must be explicit and documented.

## 7. Entity relationships

``` mermaid
erDiagram
    TASK ||--o{ FOCUS_SESSION : "may have"
    FOCUS_SESSION ||--o{ FOCUS_PAUSE_INTERVAL : "contains"
    HABIT ||--o{ HABIT_SCHEDULE_PERIOD : "has schedule history"
    HABIT ||--o{ HABIT_COMPLETION : "has dated completions"
    DAILY_REVIEW {
        string id
        date review_date
        string status
    }
    TASK {
        string id
        string title
        date planned_date
        string status
    }
    FOCUS_SESSION {
        string id
        string task_id
        datetime started_at
        datetime ended_at
    }
    FINANCE_TRANSACTION {
        string id
        string type
        integer amount_minor
        date transaction_date
    }
```

Daily Review, Home Dashboard, and Life Analytics are consumers of source
records. They are not parent records for tasks, sessions, habits, or
transactions.

## 8. Source of truth by metric

  -----------------------------------------------------------------------
  Metric                  Source of truth         Calculation owner
  ----------------------- ----------------------- -----------------------
  Tasks planned           Task records for        Task domain/query
                          selected date           

  Tasks completed         Completed Task records  Task domain/query
                          for selected date       

  Focused time            Focus Session + Pause   Focus domain service
                          Interval records        

  Habits due              Habit + effective       Habit domain service
                          schedule periods        

  Habits completed        Habit Completion        Habit domain service
                          records for due habits  

  Today's expenses        Expense transactions    Finance domain/query
                          for local date          

  Period income           Income transactions in  Finance domain/query
                          period                  

  Period expenses         Expense transactions in Finance domain/query
                          period                  

  Period net              Income minus expenses   Finance domain/query

  Review text/status      Daily Review record     Review repository
  -----------------------------------------------------------------------

All screens must use these shared calculations. Do not implement
separate versions of the same metric in Home, Review, and Insights.

## 9. Derived data and aggregate strategy

V1 should calculate summaries from source records using indexed database
queries and shared domain functions.

Potential indexes: - Task `planned_date`, `status`. - Focus Session
`started_at`, `ended_at`, `task_id`, `status`. - Focus Pause Interval
`session_id`. - Habit Schedule Period `habit_id`, `effective_from`,
`effective_to`. - Habit Completion (`habit_id`, `completion_date`)
unique. - Finance Transaction `transaction_date`, `type`. - Daily Review
`review_date` unique.

Do not introduce cached aggregate tables until performance evidence
shows they are needed.

If aggregates are added later: - They must be rebuildable from source
records. - They must be invalidated when source records change. - They
must never become the only copy of a user's data. - Calculation version
changes must trigger recomputation.

## 10. Data integrity and transactions

Use database transactions for multi-step operations that must succeed or
fail together.

Examples: - Starting a focus session: verify no other session is
active/paused, then create the session atomically. - Pausing a session:
create one open pause interval and update session state together. -
Resuming a session: close the open pause interval and update session
state together. - Completing a habit: insert or update the unique
habit/date completion record. - Changing a habit schedule: close the
previous period and create the new period atomically. - Completing a
focus session: close any valid open interval, set end time, calculate
duration, and update status together.

Enforce constraints in the database where possible, not only in UI
validation.

## 11. Update and deletion rules

### Task deletion

-   Deleting a task must not erase focus-session history.
-   Prefer retaining the task reference as a nullable/deleted reference
    or soft-deleting the task so historical sessions remain
    interpretable.
-   If a task is permanently removed, preserve enough historical context
    to avoid orphaned or misleading focus records.
-   Confirm the final deletion policy in technical architecture.

### Focus session deletion

-   Deleting a session removes or archives its pause intervals
    consistently.
-   Do not silently change linked task completion state.

### Habit deactivation

-   Deactivation stops future due calculations after its effective date.
-   Preserve previous schedule periods and completion records.
-   Do not delete historical completions.

### Finance transaction deletion

-   Deleted transactions must no longer contribute to totals.
-   Use soft deletion only if restore/audit behaviour is intentionally
    supported; otherwise a clear permanent-delete policy may be used.
-   Do not silently alter other transactions.

### Daily Review deletion

-   Deleting a review deletes only its reflection/status record.
-   It must not delete tasks, focus sessions, habits, or transactions
    for that date.

## 12. Data validation

Validate at both UI/domain level and database level where possible.

-   Required text must not be blank after trimming.
-   Durations must be positive and within safe numeric bounds.
-   Money amount must be greater than zero and safely representable.
-   Currency code must be valid for the supported configuration.
-   End timestamps cannot precede start timestamps.
-   Pause intervals cannot overlap.
-   A task-linked focus session must reference a valid task.
-   A habit completion must reference a valid habit.
-   Habit/date completion must be unique.
-   Habit schedule periods must not overlap.
-   Daily Review date must be unique.
-   Future-date review completion must follow the product rule in
    `10_DAILY_REVIEW.md`.

Invalid data must produce a clear error and must not partially update
related records.

## 13. Single-user V1 and future multi-user readiness

V1 is a single-user app on one device. A user/account table and
authentication system are not required.

However: - Keep entity IDs globally unique. - Keep repository boundaries
clear. - Avoid assuming that a database row ID is meaningful outside the
local database. - If multi-user or cloud sync is introduced later, add
an explicit owner/account relationship and conflict-resolution strategy
through a planned migration. - Do not add unused account complexity to
V1.

## 14. Backup, export, and recovery

The user must be able to uninstall Life Analytics, reinstall it later, and restore their previously saved records. App-private Room/SQLite storage alone cannot meet this requirement because Android removes app-private files on uninstall.

Requirements:

- Room/SQLite remains the primary source of truth while the app is installed.
- Maintain a separate backup copy outside the app-private sandbox. A backup stored only inside the app's internal files/cache/database directories is not an uninstall-safe backup.
- The backup must include all user-owned records and the app preferences needed to interpret them: tasks, focus sessions and pause intervals, habits, schedule periods, habit completions, finance transactions, daily reviews, and relevant settings. Explicitly document any excluded data.
- Use a versioned, documented backup format with a manifest containing format version, creation time, app version, database schema version, and record counts. Do not include credentials, signing keys, or unrelated device data.
- Verify backup integrity before reporting success. Use a checksum or authenticated-encryption verification and validate the manifest and payload before restore.
- Protect sensitive backup contents with authenticated encryption. The recovery design must remain usable after uninstall; do not rely solely on an Android Keystore key that may be removed with the app. If a user-held recovery password/key is required, explain that losing it can make the backup unrecoverable and never transmit it to the developer.
- Restore must validate the backup before changing the live database. Restore transactionally where possible; preserve or create a pre-restore safety copy before replacing existing records. Never silently merge duplicate records or overwrite newer records without a defined policy and user-visible explanation.
- Support a user-controlled export/backup destination outside app-private storage. A cloud provider can be used only after its actual integration and restore flow are verified. Do not assume that the Google Drive APK-upload folder or an installed Drive app automatically grants Life Analytics API access.
- Android Auto Backup/device-transfer backup may be an additional convenience, but must not be the sole guarantee for uninstall recovery. Its behaviour depends on Android settings, account/device support, backup rules, and restore conditions.
- Database migrations must preserve records. A failed migration must not silently wipe the database. Recovery errors must be communicated clearly.
- Reflection and finance data must receive the same privacy protection as other local records.

Backup/export implementation details are finalized in `12_TECHNICAL_ARCHITECTURE.md`; acceptance tests are in `13_IMPLEMENTATION_TESTING.md`.

## 15. Privacy and security

-   Keep the working copy of all V1 personal data on-device. A separately encrypted backup may be stored at a user-approved external/cloud destination.
-   Do not send reflection text, finance transactions, or task titles to
    analytics providers.
-   Avoid logging personal text or transaction amounts.
-   Use Android application sandbox protections.
-   Do not store secrets that the app does not need.
-   Before creating or restoring an external/cloud backup, make its data scope and destination clear and obtain explicit user action/consent. Backup does not authorize cloud sync.
-   Data deletion must have a clear, understandable effect.

## 16. Migration and schema versioning

-   Maintain an explicit database schema version.
-   Every schema change requires a migration path.
-   Test migrations from each supported prior version.
-   Never solve a migration issue by silently deleting user data.
-   Seed only essential app configuration, not fake user tasks, habits,
    sessions, or transactions.
-   Keep test fixtures separate from production data.
-   Document migration behaviour in `12_TECHNICAL_ARCHITECTURE.md`.

## 17. Acceptance criteria

### Data ownership

-   [ ] Every business fact has one source of truth.
-   [ ] Home, Daily Review, and Insights reuse shared calculations.
-   [ ] Daily Review stores reflection/status only, not duplicate module
    records.
-   [ ] General focus sessions can exist without a task.
-   [ ] Task-linked focus sessions reference a valid task.

### Dates and time

-   [ ] Date-only values are stored independently from timestamps.
-   [ ] Event timestamps are stored unambiguously.
-   [ ] A shared timezone/date attribution policy is used across
    modules.
-   [ ] Device timezone changes do not rewrite historical event
    timestamps.
-   [ ] Focus sessions crossing midnight follow one documented rule.

### Integrity

-   [ ] Only one active or paused focus session can exist at a time.
-   [ ] Pause intervals cannot overlap.
-   [ ] Habit/date completion is unique.
-   [ ] Habit schedule periods cannot overlap.
-   [ ] Daily Review date is unique.
-   [ ] Multi-step updates are atomic.

### Privacy and durability

-   [ ] V1 works offline.
-   [ ] Personal data is not sent to third-party analytics.
-   [ ] Database migrations preserve existing records.
-   [ ] A verified backup exists outside app-private storage and can be restored after uninstall/reinstall.
-   [ ] Backup payload is versioned, integrity-checked, and protected with authenticated encryption.
-   [ ] Deleting one record does not unexpectedly delete unrelated
    history.
-   [ ] No fake sample data appears in a real user's account.

## 18. Decisions to finalize in Technical Architecture

The following implementation choices must be confirmed in
`12_TECHNICAL_ARCHITECTURE.md`:

1.  Android language and UI framework.
2.  SQLite persistence library (Room if Kotlin is selected).
3.  UUID generation strategy.
4.  Exact SQL column types and naming conventions.
5.  Soft deletion versus permanent deletion per entity.
6.  Focus session midnight attribution and interruption recovery
    implementation.
7.  Device timezone-change handling.
8.  Backup container/provider, authentication, encryption-key recovery, and user export/restore scope.
9.  Whether any aggregate caching is justified by measured performance.
10. Database migration and test strategy.

These decisions must preserve the logical rules in this document.
Technical convenience must not introduce duplicate sources of truth or
change user-visible calculations without updating the product
specifications.

## 19. Related documents

-   `01_PRODUCT_OVERVIEW.md` --- product vision, scope, and principles.
-   `02_PRODUCT_REQUIREMENTS.md` --- functional and non-functional
    requirements.
-   `03_UI_UX_GUIDELINES.md` --- interaction and accessibility
    standards.
-   `04_HOME_DASHBOARD.md` --- dashboard data dependencies.
-   `05_DAILY_PLANNING.md` --- task fields and task behaviour.
-   `06_ACTIVE_WORK_FOCUS.md` --- session lifecycle and duration
    calculation.
-   `07_DAILY_HABITS.md` --- habit schedules and dated completion.
-   `08_FINANCE.md` --- transaction model and money calculations.
-   `09_LIFE_ANALYTICS.md` --- derived metrics and comparisons.
-   `10_DAILY_REVIEW.md` --- reflection and daily summary behaviour.
-   `12_TECHNICAL_ARCHITECTURE.md` --- concrete implementation stack and
    structure.
-   `13_IMPLEMENTATION_TESTING.md` --- implementation plan and
    verification.


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

### Analytics classification data model addendum
Introduce a controlled `life_area` value on Habit and Task records (or a normalized classification relation if the existing schema requires it): `HEALTH_LIFESTYLE`, `CAREER_PRODUCTIVITY`, `FINANCE_WEALTH`. It is nullable for legacy records and required for new/edited records after the V1 feature is enabled. Keep source records authoritative; analytics aggregates are derived and rebuildable. Do not backfill old records automatically.
