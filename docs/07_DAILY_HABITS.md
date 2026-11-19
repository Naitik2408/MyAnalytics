# Life Analytics --- Daily Habits

**Document:** `07_DAILY_HABITS.md`\
**Product:** Life Analytics\
**Version:** 1.0 (V1)\
**Status:** Module specification\
**Platform:** Android\
**Related documents:** `01_PRODUCT_OVERVIEW.md`,
`02_PRODUCT_REQUIREMENTS.md`, `03_UI_UX_GUIDELINES.md`,
`04_HOME_DASHBOARD.md`, `09_LIFE_ANALYTICS.md`,
`11_DATA_ARCHITECTURE.md`

------------------------------------------------------------------------

## 1. Purpose

Daily Habits helps the user define recurring behaviours and record
whether they completed each habit on the days it was scheduled.

The module should make daily check-ins quick and provide a reliable
history of consistency over time. It must distinguish a habit's schedule
from its daily completion record.

**Core distinction:** A task is a piece of planned work that may be done
once. A habit is a behaviour the user intends to repeat on a schedule.
Habits must not be stored or counted as tasks.

## 2. UX goals

-   Make it quick to create a habit.
-   Make today's due habits easy to review and check off.
-   Make it clear which habits are due today and which are not
    scheduled.
-   Allow the user to correct a completion record.
-   Show progress without guilt-inducing language.
-   Preserve historical completion when a habit's schedule changes.
-   Avoid making users re-enter the same habit every day.
-   Keep daily check-in interactions simple and touch-friendly.

## 3. V1 scope

### Included

-   Create a habit with a name.
-   Choose the days on which it is scheduled.
-   View habits due on a selected date.
-   Mark a habit complete or incomplete for a specific date.
-   Edit habit name and schedule.
-   Deactivate or delete a habit.
-   View basic habit completion history.
-   Calculate daily and period consistency from scheduled days and dated
    completion records.
-   Show today's habit progress on Home.

### Excluded from V1

-   AI habit recommendations.
-   Automatic detection from phone sensors or app usage.
-   Social challenges, teams, and leaderboards.
-   Complex habit goals with multiple milestones.
-   Habit reminders and notification schedules unless separately
    approved.
-   Health integrations or medical advice.
-   Automatically generating habits from tasks.

## 4. Key concepts and terminology

  -----------------------------------------------------------------------
  Term                                Meaning
  ----------------------------------- -----------------------------------
  Habit                               A recurring behaviour defined by
                                      the user.

  Schedule                            The weekdays on which the habit is
                                      due.

  Due habit                           An active habit scheduled for a
                                      particular date.

  Completion record                   A dated record indicating whether a
                                      habit was completed.

  Consistency                         Completed due occurrences divided
                                      by total due occurrences in a
                                      selected period.

  Deactivated habit                   A habit no longer scheduled for
                                      future tracking, with historical
                                      records retained.
  -----------------------------------------------------------------------

Use these terms consistently. Avoid calling an unscheduled day a missed
day.

## 5. Habit list screen

### 5.1 Suggested layout

``` text
┌──────────────────────────────────────┐
│ < Back             Daily Habits      │
│                                      │
│ Monday, 16 November 2026      [Date] │
│                                      │
│ Today's progress                    │
│ ┌──────────────────────────────────┐ │
│ │ 2 of 3 habits completed          │ │
│ │ [progress indicator]             │ │
│ └──────────────────────────────────┘ │
│                                      │
│ Habits due today                     │
│                                      │
│ [✓] Morning walk                     │
│     Mon · Wed · Fri                  │
│                                      │
│ [ ] Read 10 pages                    │
│     Every day                        │
│                                      │
│ [✓] Drink enough water               │
│     Mon · Tue · Wed · Thu · Fri      │
│                                      │
│                                      │
│ Other habits                         │
│ Meditation · Tue, Thu, Sat           │
│                                      │
│                    [+ Add Habit]     │
├──────────────────────────────────────┤
│ Home    Finance   Activity  Insights │
└──────────────────────────────────────┘
```

All habit names and completion values above are illustrative only. Do
not ship them as pre-populated user records.

### 5.2 Screen structure

Recommended order: 1. Header and selected date. 2. Daily completion
summary. 3. Habits due on the selected date. 4. Optional section for
habits not due today, if it helps users understand their schedules. 5.
Add Habit action.

Do not let the "Other habits" section visually compete with today's due
habits. It may be omitted from the initial version if it adds clutter.

## 6. Create Habit flow

### 6.1 Entry points

-   Add Habit on the Daily Habits screen.
-   Any approved Home shortcut or empty-state action.

### 6.2 Required fields

  -----------------------------------------------------------------------
  Field                   Required                Rules
  ----------------------- ----------------------- -----------------------
  Habit name              Yes                     Must contain at least
                                                  one non-whitespace
                                                  character.

  Schedule                Yes                     At least one weekday
                                                  must be selected.
  -----------------------------------------------------------------------

Optional fields such as notes, reminders, targets, or habit categories
should not be added to the initial form unless a clear V1 requirement is
approved.

### 6.3 Suggested form

``` text
┌──────────────────────────────────────┐
│ Add habit                         [X]│
│                                      │
│ Habit name                           │
│ [ e.g. Read 10 pages              ]  │
│                                      │
│ Repeat on                            │
│ [Mon] [Tue] [Wed] [Thu] [Fri]        │
│ [Sat] [Sun]                          │
│                                      │
│ Quick schedule                       │
│ [Every day] [Weekdays] [Weekends]    │
│                                      │
│              [Cancel] [Save habit]   │
└──────────────────────────────────────┘
```

### 6.4 Schedule selection UX

-   Display weekdays in a consistent Monday-to-Sunday order.
-   Selected days must be visually and textually identifiable.
-   Provide quick choices for Every day, Weekdays, and Weekends.
-   Quick choices must update the weekday selection; users can then
    customize individual days.
-   Require at least one selected day.
-   Do not silently default to every day unless the user explicitly
    selects that option.
-   Make the selected schedule understandable before saving.

### 6.5 Save behaviour

-   Save the habit definition and its schedule.
-   Do not create completion records for past or future dates when the
    habit is created.
-   The habit becomes eligible for due status according to its effective
    start date and schedule.
-   Show a brief success confirmation and return to the habit list.
-   If saving fails, preserve entered values and explain the issue.

## 7. Daily completion workflow

### 7.1 Mark complete

-   The user taps the completion control on a habit due that day.
-   Create or update the completion record for that habit and date.
-   Update the daily summary.
-   Provide immediate visual feedback.
-   Do not create a new habit definition or duplicate record.

### 7.2 Mark incomplete / undo

-   The user can tap a completed habit again to undo completion.
-   Update the same habit/date completion record.
-   Recalculate daily and period summaries.
-   Do not erase the habit definition.
-   Provide clear feedback if the save fails and restore the previous
    visible state.

### 7.3 Completion states

A habit occurrence for a date should have one of these effective states:

-   **Due and incomplete**
-   **Due and complete**
-   **Not scheduled**
-   **Not applicable** because the habit was not active on that date

Do not label a not-scheduled day as missed.

## 8. Date navigation

-   Default to today's local date.
-   Allow previous/next date navigation and direct date selection.
-   Display the selected date clearly.
-   Show only habits that were active and scheduled for that date in the
    Due Today list.
-   Completion controls must update the selected date's record, not
    automatically today's record.
-   Historical dates must remain viewable.
-   Future dates may show which habits are scheduled, but should not be
    marked complete before the date occurs unless a separate product
    decision explicitly allows advance completion.

If a user views a future date, disable completion controls or explain
that completion is recorded on the day itself. Use one consistent
behaviour.

## 9. Edit Habit

The user must be able to edit: - Habit name. - Scheduled weekdays.

### 9.1 Edit behaviour

-   Pre-fill the form with current values.
-   Validate that the name is not empty.
-   Require at least one scheduled weekday.
-   Preserve all existing historical completion records.
-   Do not rewrite past schedules or retroactively mark days
    due/incomplete simply because the current schedule changed.

### 9.2 Schedule change effective date

A schedule change must have a clear effective date.

Recommended V1 rule: - Changes apply from the current local date
onward. - Past due dates and completion history retain the schedule that
applied at that time. - Store schedule history or an equivalent
effective-dated representation so historical consistency calculations
remain accurate.

If implementing schedule history is not feasible in the first release,
the limitation must be explicitly documented and historical reports must
not pretend the current schedule applied to the past. Resolve this in
`11_DATA_ARCHITECTURE.md` before implementation.

## 10. Deactivate and delete Habit

### 10.1 Deactivate

Deactivation is the preferred way to stop tracking a habit without
losing history.

-   The habit stops appearing as due from its deactivation effective
    date onward.
-   Past completion records remain available.
-   Historical consistency remains based on the schedule that applied at
    the time.
-   The habit may appear in an Inactive section or history filter.

### 10.2 Delete

If permanent deletion is supported in V1: - Ask for confirmation. -
Explain the impact on history and analytics. - Do not silently delete
associated completion records if doing so would distort historical
reports. - Prefer deactivation for ordinary "I no longer do this"
cases. - Follow the retention and deletion rules in
`11_DATA_ARCHITECTURE.md`.

Suggested copy:

> Stop tracking this habit? You can keep its history and prevent it from
> appearing in future daily check-ins.

Prefer "Deactivate" over "Delete" as the default action.

## 11. Daily summary calculation

Display:

`completed due habits / total habits due`

Example: `2 of 3 completed`

Rules: - Denominator = active habits scheduled for the selected date and
effective on that date. - Numerator = those due habits with a completion
record marked complete for that date. - Not-scheduled habits are
excluded. - Deactivated habits are excluded from dates on or after their
effective deactivation date. - If no habits are due, show "No habits
scheduled". - Never display `0%` as a failure when the denominator is
zero. - Update immediately after a completion change is saved.

## 12. Consistency and history

The user should be able to inspect habit completion over a date range.

### 12.1 Basic history

For a selected habit, show: - Habit name. - Current schedule and, where
available, past schedule periods. - Completed due occurrences. - Total
due occurrences. - Completion percentage or ratio. - A simple calendar
or list of dated states.

### 12.2 Consistency formula

For a selected period:

``` text
consistency =
    completed due occurrences
    / total due occurrences
    × 100
```

Rules: - Count only dates on which the habit was active and scheduled. -
Exclude dates before the habit's effective start date. - Exclude dates
on or after its deactivation effective date. - Do not count future dates
as missed. - Exclude dates with no applicable schedule. - If total due
occurrences is zero, show "Not enough scheduled days" or an equivalent
neutral state, not 0%.

### 12.3 Streaks

Streaks are not required for V1. If introduced later: - Define whether a
streak means consecutive scheduled occurrences or consecutive calendar
days. - Non-scheduled days must not break a schedule-based streak. - Do
not show a streak value unless its calculation is clearly defined and
tested.

## 13. Home Dashboard integration

The Home Habits Completed metric uses the same daily summary
calculation.

-   Home shows a compact preview of habits due today.
-   The completion control updates the same dated completion record as
    the Daily Habits screen.
-   Home and Daily Habits must not maintain separate completion states.
-   View All opens Daily Habits for today.
-   If no habits are due, Home shows "No habits scheduled" with an
    appropriate route to manage habits.
-   Updating a habit schedule must refresh Home if today's due set
    changes.

## 14. Insights integration

Insights may use habit records to show: - Daily completion ratio. -
Weekly and monthly consistency. - Completion trends by habit. -
Comparison between equivalent periods, if implemented.

Rules: - Use the same due-date and schedule-history logic as the Daily
Habits module. - Do not count not-scheduled dates as failures. - Clearly
label the date range and denominator. - Do not claim that habit
completion caused a change in another metric. - Do not infer health
outcomes from habit check-ins.

## 15. Notifications and reminders

Habit reminders are not required for core V1 functionality.

If reminders are considered later: - They must be optional and
configured by the user. - Explain the purpose before requesting
notification permission. - Allow per-habit reminder time only if the UX
remains simple. - Editing or deactivating a habit must update or cancel
its reminders. - Denying notification permission must not prevent manual
habit tracking. - Reminder scheduling must follow local timezone and
daylight-saving rules where applicable.

Do not request notification permission just to display the daily habit
checklist.

## 16. Empty, loading, error, and success states

### No habits created

Show a short explanation and one Add Habit action.

Example: "Create a habit you want to track regularly."

### No habits scheduled today

Show: "No habits scheduled for today."

Provide a route to view or manage all habits, but do not imply the user
missed anything.

### Due habits incomplete

Show the due habits with unchecked controls. Do not use an error or
failure presentation.

### Loading

Use a stable layout and lightweight placeholders if required. Do not
show fake habit names.

### Error

-   Explain whether habits or completion states could not be loaded or
    saved.
-   Preserve user input in the create/edit form.
-   Restore the previous checkbox state if a completion update fails.
-   Offer retry where possible.

### Success

-   Update the completion state and summary.
-   Use short, non-blocking feedback.
-   Avoid celebratory animations that make repeated check-ins slow.

## 17. Data ownership and integrity

The Daily Habits module owns: - Habit identity and name. -
Effective-dated schedule. - Active/deactivated status and effective
dates. - Dated completion records.

Rules: - A completion record must reference one habit and one local
date. - There must not be duplicate completion records for the same
habit/date. - Habit definitions and daily completion records must be
separate entities. - Editing a habit must not overwrite historical
completion records. - Dashboard and Insights must read from these source
records. - Date and timezone interpretation must follow the shared data
architecture. - A habit that is deactivated must remain available for
historical reporting unless the user explicitly requests permanent
deletion under the defined deletion policy.

## 18. Accessibility and usability

-   Use a sufficiently large completion control, preferably at least 48
    dp.
-   Provide an accessible label such as "Mark Morning Walk complete" or
    "Mark Morning Walk incomplete".
-   Communicate completion with a checkmark and text/state, not color
    alone.
-   Make selected weekdays clear to both sighted users and accessibility
    services.
-   Support larger text without clipping habit names or weekday labels.
-   Keep the daily check-in action easy to perform one-handed.
-   Use neutral language; avoid "failed", "lazy", or guilt-based
    messages.
-   Ensure the screen works in Light and Dark themes.

## 19. Acceptance criteria

### Habit creation

-   [ ] User can create a habit with a name and at least one scheduled
    weekday.
-   [ ] User can choose Every day, Weekdays, Weekends, or custom
    weekdays.
-   [ ] Empty names and empty schedules are rejected with clear
    messages.
-   [ ] Creating a habit does not fabricate past completion records.

### Daily check-in

-   [ ] Only habits due on the selected date appear in the due list.
-   [ ] User can mark a due habit complete.
-   [ ] User can undo completion.
-   [ ] Completion is stored for the selected date.
-   [ ] Repeated taps do not create duplicate completion records.
-   [ ] Not-scheduled days are not counted as missed.

### Editing and deactivation

-   [ ] User can edit a habit name and schedule.
-   [ ] Schedule changes preserve historical completion.
-   [ ] Changes have a documented effective date.
-   [ ] User can deactivate a habit without losing history.
-   [ ] Delete behaviour is clear and protected against accidental loss.

### Analytics and integration

-   [ ] Daily summary uses completed due habits / total due habits.
-   [ ] Consistency uses only applicable scheduled occurrences.
-   [ ] Zero due occurrences produce a neutral empty state, not 0%.
-   [ ] Home and Insights use the same source records and calculation
    rules.
-   [ ] Habit records remain separate from tasks.

### UX quality

-   [ ] Daily check-in is fast and understandable.
-   [ ] Empty, loading, error, and success states are defined.
-   [ ] Light and Dark themes are supported.
-   [ ] Controls have accessible labels and adequate touch targets.
-   [ ] No guilt-based language or fake habit data appears.

## 20. Open decisions for data and technical architecture

Resolve before implementation:

1.  How schedule history and effective dates are stored.
2.  Exact rule for applying schedule edits from the current day.
3.  Whether permanent deletion is included in V1 or only deactivation.
4.  Whether habit history uses a calendar, list, or both.
5.  Whether streaks are deferred entirely from V1.
6.  Whether habit categories or notes are needed; default is to omit
    them from V1.
7.  Whether reminders are deferred entirely from V1.
8.  How local dates are stored and handled if timezone changes.
9.  How a habit created during the day becomes due on its creation date.
10. Whether a completion can be edited for a past date and what limits,
    if any, apply.

Do not let UI components invent their own schedule or consistency rules.
Centralize these rules and test them.

## 21. Related documents

-   `01_PRODUCT_OVERVIEW.md` --- product direction and V1 scope.
-   `02_PRODUCT_REQUIREMENTS.md` --- cross-module requirements.
-   `03_UI_UX_GUIDELINES.md` --- shared design and interaction rules.
-   `04_HOME_DASHBOARD.md` --- Home habit preview and daily metric.
-   `09_LIFE_ANALYTICS.md` --- habit consistency and historical trends.
-   `10_DAILY_REVIEW.md` --- daily reflection and summaries.
-   `11_DATA_ARCHITECTURE.md` --- schedule history, dated records, and
    persistence.
-   `12_TECHNICAL_ARCHITECTURE.md` --- implementation details.
-   `13_IMPLEMENTATION_TESTING.md` --- schedule, date, and consistency
    tests.


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

### Habit classification addendum
Add manual life-area selection to Habit create/edit. A habit has one life area in V1. Its scheduled completion denominator must respect its effective schedule; unscheduled days and dates before habit creation/after deactivation are not misses. Existing habits may remain unclassified until edited; do not force a historical migration.
