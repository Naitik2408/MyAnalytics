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
