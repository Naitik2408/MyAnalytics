# Life Analytics --- Active Work & Focus

**Document:** `06_ACTIVE_WORK_FOCUS.md`\
**Product:** Life Analytics\
**Version:** 1.0 (V1)\
**Status:** Module specification\
**Platform:** Android\
**Related documents:** `01_PRODUCT_OVERVIEW.md`,
`02_PRODUCT_REQUIREMENTS.md`, `03_UI_UX_GUIDELINES.md`,
`04_HOME_DASHBOARD.md`, `05_DAILY_PLANNING.md`

------------------------------------------------------------------------

## 1. Purpose

Active Work & Focus lets the user record periods of focused work using a
simple timer. A session may be linked to a planned task or recorded as
general focus without a task.

The module must make starting and controlling a session effortless while
preserving accurate session history. It must also be honest about what
the timer measures: it records a user-started session and its active
elapsed duration; it cannot prove that the user was continuously
attentive or free from distractions.

**Core principle:** One reliable timer engine, one active session at a
time, and no duplicate time entry.

## 2. UX goals

-   Start a focus session with minimal steps.
-   Reuse an existing task without asking the user to type its title
    again.
-   Allow general focus when the user has not planned a task.
-   Make the active state and timer controls immediately understandable.
-   Make Pause, Resume, and Finish distinct and hard to trigger
    accidentally.
-   Recover safely if the app is backgrounded or interrupted.
-   Preserve completed and interrupted session history.
-   Keep planned task duration separate from actual recorded focus
    duration.
-   Never require the user to enter focused minutes manually as part of
    the normal workflow.

## 3. V1 scope

### Included

-   Start a session linked to an existing task.
-   Create a new task and start a linked session.
-   Start general focus without a task, with an optional activity
    description.
-   Start, pause, resume, and finish a session.
-   Maintain only one active session at a time.
-   Calculate focused duration automatically, excluding paused
    intervals.
-   Recover active or interrupted session state.
-   Store and view session history.
-   Show daily and historical focus totals in Home and Insights.

### Excluded from V1

-   Automatic distraction detection.
-   Screen/app usage monitoring.
-   Website or app blocking.
-   Pomodoro schedules and complex interval programs.
-   Social focus rooms or team sessions.
-   AI productivity scoring.
-   Claims that timer duration proves attention or productivity.

## 4. Core terminology

  -----------------------------------------------------------------------
  Term                                Meaning
  ----------------------------------- -----------------------------------
  Focus session                       A user-started timed work interval.

  Active session                      A session that has started and is
                                      currently running.

  Paused session                      A session that has started but
                                      whose timer is temporarily stopped.

  Finished session                    A session explicitly ended by the
                                      user.

  Interrupted session                 A session that did not reach a
                                      normal Finish action because of an
                                      interruption or recovery event.

  Focused duration                    Total elapsed time counted as
                                      active, excluding paused intervals.

  Wall-clock duration                 Time between session start and end
                                      timestamps; this may include
                                      pauses.
  -----------------------------------------------------------------------

Use these terms consistently in the interface and data model.

## 5. Entry points

The user can start a focus session from:

1.  Home --- Start Focus quick action.
2.  Daily Planning --- Start Focus action on a task.
3.  Activity --- Active Work / Focus screen.

Every entry point must call the same session-start logic. Do not
implement separate timers for different screens.

## 6. Starting a session

When the user taps Start Focus without a task already selected, show a
simple choice:

-   **Choose a planned task**
-   **Create a task and start**
-   **Start general focus**

Keep the choice compact and avoid a long configuration wizard.

### 6.1 Existing planned task

Flow: 1. User selects a task. 2. Show the selected task title so the
user can confirm the context. 3. User taps Start. 4. The app checks
whether another session is active or paused. 5. If no session is in
progress, create and start a session linked to the task. 6. Navigate to
the active session screen.

Rules: - Pass the task ID and title from Daily Planning; do not ask the
user to re-enter them. - Only eligible tasks for the selected workflow
should be offered. The default list should prioritize incomplete
tasks. - Starting a session must not automatically mark the task
complete. - A task may have multiple historical focus sessions.

### 6.2 Create a task and start

Flow: 1. User chooses Create a task and start. 2. Show a compact task
form. 3. Require only the task title; use the same category and date
defaults as Daily Planning. 4. Save the task. 5. Start a focus session
linked to the newly saved task. 6. Navigate to the active session
screen.

Important: - Do not start the timer before the task save succeeds. - If
task creation fails, do not create a session with a broken task
reference. - Preserve form input if saving fails. - Avoid showing a
second redundant task form after returning to Daily Planning.

### 6.3 General focus

Flow: 1. User chooses Start general focus. 2. Optionally enter a short
activity description, such as "Reading" or "Planning". 3. User taps
Start. 4. The app creates a session with no task ID and starts the
timer. 5. Navigate to the active session screen.

Rules: - Description is optional. - General focus sessions contribute to
Focused Time. - A missing description must not prevent starting. - Do
not create a fake task for a general focus session.

## 7. Single active-session rule

Only one session may be active or paused at a time.

Before starting a new session: - Check the persisted session state, not
only the currently visible screen. - If a running or paused session
exists, do not create another. - Offer to return to the existing
session. - If the user wants to finish or discard the existing session,
require a clear action and preserve the history rules. - Prevent
duplicate session creation from rapid repeated taps.

This rule must be enforced in the domain/data layer, not only by
disabling a button in the UI.

## 8. Active session screen

### 8.1 Screen goals

The active screen should remove unnecessary distractions and keep the
current session context obvious.

### 8.2 Suggested layout

``` text
┌──────────────────────────────────────┐
│ < Back                    Active Work│
│                                      │
│             FOCUS SESSION            │
│                                      │
│             00:24:18                 │
│          Focused duration            │
│                                      │
│        Working on                    │
│        Prepare project presentation  │
│        Study · Planned task           │
│                                      │
│          [   Pause   ]               │
│                                      │
│          [ Finish session ]          │
│                                      │
│     Started at 10:02 AM              │
│                                      │
└──────────────────────────────────────┘
```

The timer value is illustrative only.

### 8.3 Required content

-   Large elapsed focused-duration display.
-   Current task title or general activity description.
-   Session state: Running or Paused.
-   Start time or equivalent session context.
-   Primary control appropriate to the current state.
-   Finish action.
-   Clear indication that paused time is not counted.

Do not fill this screen with unrelated analytics, charts, or task lists.

## 9. Timer state machine

A session follows this state model:

``` mermaid
stateDiagram-v2
    [*] --> Running: Start
    Running --> Paused: Pause
    Paused --> Running: Resume
    Running --> Finished: Finish
    Paused --> Finished: Finish
    Running --> Interrupted: Recovery detects interruption
    Paused --> Interrupted: Recovery detects interruption
    Interrupted --> Finished: User confirms completion
    Interrupted --> Running: User explicitly resumes, if supported
    Finished --> [*]
```

The exact recovery path must be consistent with the rules below. A
session must never silently transition to Finished merely because the
app was closed.

### State definitions

  -----------------------------------------------------------------------
  State                   Meaning                 Timer behaviour
  ----------------------- ----------------------- -----------------------
  Running                 Session is active       Focused duration
                                                  increases

  Paused                  User paused the session Focused duration does
                                                  not increase

  Finished                User ended the session  Duration is fixed

  Interrupted             App/device interruption No unverified time is
                          requires recovery       added until recovery is
                                                  resolved
  -----------------------------------------------------------------------

## 10. Timer controls

### 10.1 Start

-   Create a unique session record.
-   Persist the start timestamp and Running state.
-   Start the elapsed-time display.
-   Prevent a second active session.
-   Provide immediate visual confirmation.

### 10.2 Pause

-   Record the pause timestamp.
-   Change state to Paused.
-   Stop adding time to focused duration.
-   Keep the session and its task association.
-   Change the primary action to Resume.
-   Show a clear Paused label.

### 10.3 Resume

-   Record the resume timestamp.
-   Close the current pause interval.
-   Change state to Running.
-   Continue the same session; do not create a new session record.
-   Exclude the completed pause interval from focused duration.

### 10.4 Finish

-   Available from Running and Paused states.
-   End the session and persist its final duration.
-   If paused, do not count the time since Pause.
-   Show a concise confirmation with the recorded focused duration.
-   Make the session available in history.
-   Return to a sensible Activity or Home destination.

A confirmation step may be used if accidental Finish is a realistic
risk, but it must not make the common finish action unnecessarily slow.
If confirmation is used, clearly state that finishing saves the session.

## 11. Focus duration calculation

### 11.1 Required calculation

Focused duration is the sum of running intervals only.

Conceptually:

``` text
focused_duration =
    sum(all completed running intervals)
    + current running interval, if session is still running
```

Equivalent timestamp model:

``` text
focused_duration =
    (finished_at - started_at)
    - total_paused_duration
```

The timestamp formula is valid only when all pause intervals and
interruption gaps are accounted for correctly. Prefer storing explicit
state transitions or interval records so recovery can be audited.

### 11.2 Example

-   Start: 10:00 AM
-   Pause: 10:25 AM
-   Resume: 10:35 AM
-   Finish: 11:10 AM

Wall-clock duration = 70 minutes.\
Paused duration = 10 minutes.\
Focused duration = 60 minutes.

The app must show 60 minutes as focused duration.

### 11.3 Display formatting

-   Under one hour: `25m`
-   One hour or more: `1h 10m`
-   Use seconds only if they materially help the live timer; avoid noisy
    seconds in history and dashboard summaries.
-   Use tabular numerals where supported to reduce visual jitter.

## 12. Timekeeping and app lifecycle

A timer must not depend on a UI animation loop as its source of truth.

Requirements: - Persist session state and timestamps. - Derive displayed
elapsed time from persisted time anchors and pause intervals. - Refresh
the display when the app returns to the foreground. - Handle activity
recreation and ordinary process recreation. - Use a monotonic clock for
measuring intervals while the process/device session is continuous,
together with wall-clock timestamps for history and recovery. - Do not
add time merely because the UI remained visible. - Avoid counting time
while a session is Paused. - Do not silently assume the app was
continuously running through an unknown reboot or device shutdown
interval.

The exact Android implementation (for example, lifecycle handling,
foreground-service requirements, alarms, or platform APIs) belongs in
`12_TECHNICAL_ARCHITECTURE.md`. Request only permissions that are
genuinely needed for the chosen implementation.

## 13. Interruption and recovery

Interruptions may include: - App process killed by Android. - User
force-stops the app. - Device reboot. - App update or crash. - Device
clock or timezone changes. - Long time gap between last known state and
next app launch.

### 13.1 Recovery principles

-   Never silently discard a session.
-   Never silently count an unknown gap as focused time.
-   Preserve the last known session state and reliable time anchors.
-   Present an understandable recovery choice when the elapsed interval
    cannot be trusted.
-   Make any correction visible and auditable.

### 13.2 Proposed recovery UX

When an unresolved session is found:

``` text
┌──────────────────────────────────────┐
│ Resume your focus session?           │
│                                      │
│ Your previous session was interrupted│
│ and its timer could not be verified. │
│                                      │
│ [Review session]                     │
│ [Finish at last confirmed time]      │
│ [Resume from now]                    │
└──────────────────────────────────────┘
```

This is a proposed interaction. Exact options depend on the interruption
type and technical capabilities.

Rules: - **Review session:** show last confirmed start/pause/resume
information and any uncertain interval. - **Finish at last confirmed
time:** save only reliably measured duration and mark the session as
interrupted/recovered. - **Resume from now:** resume the same session
from the current time, after explicitly informing the user that the
unknown gap will not be counted. - Do not offer a normal Resume action
if the app cannot establish a trustworthy timing baseline. - Do not
backfill unknown time automatically.

### 13.3 Device clock changes

-   Wall-clock changes must not create negative or inflated focus
    duration.
-   Use monotonic elapsed time for intervals when possible.
-   If a clock change makes an interval ambiguous, flag it for recovery
    rather than guessing.
-   Date attribution and timezone changes must follow the shared rules
    in `11_DATA_ARCHITECTURE.md`.

## 14. Session history

Provide a history view accessible from Activity / Active Work.

### 14.1 History row

Show: - Task title or general activity description. - Date. - Start/end
times where reliable. - Focused duration. - State/status if interrupted
or recovered.

### 14.2 History behaviour

-   Newest sessions appear first by default.
-   The user can inspect a session's details.
-   Do not merge separate sessions just because they reference the same
    task.
-   General sessions must be clearly distinguishable from task-linked
    sessions.
-   Interrupted or recovered sessions must be visibly identified.
-   The history total must equal the sum of the included session
    durations for the selected date/range.

### 14.3 Editing and deleting history

V1 should avoid casual editing of historical durations because it can
undermine trust in analytics.

Recommended V1 rule: - Allow the user to correct a session only through
a clearly explained correction flow, if included. - If deletion is
supported, require confirmation and update all dependent summaries. -
Preserve a clear status for recovered or corrected records. - Do not
allow an edit to create negative duration or overlap that violates the
single-session rule.

Whether history correction and deletion are included in the first
release must be finalized in `11_DATA_ARCHITECTURE.md` and
`13_IMPLEMENTATION_TESTING.md`.

## 15. Relationship with Daily Planning

-   A session may reference one task.
-   The task reference is optional.
-   Multiple sessions may be linked to the same task.
-   Starting from a task passes the task ID and title into the Focus
    module.
-   Finishing a session does not automatically complete the task.
-   Completing a task does not automatically finish its active session.
-   Editing a task title should not erase historical session context.
-   Deleting a task must not silently delete its focus history.

The data architecture must define how historical sessions display a task
that has since been deleted or renamed. Preserve enough snapshot
information to keep history understandable.

## 16. Relationship with Home and Insights

### Home

-   Focused Time is the sum of valid focus-session durations attributed
    to the selected local date.
-   Include task-linked and general sessions.
-   Exclude paused intervals and unverified interruption gaps.
-   Update after a session is finished or corrected.
-   If a session is running, the product may show a live provisional
    total, but it must be clearly derived from the current session state
    and must not be double-counted after finish.

### Insights

-   Daily, weekly, and monthly focus totals must use the same session
    inclusion and date-attribution rules.
-   Interrupted sessions must be handled consistently and transparently.
-   Do not label focus duration as verified productivity or
    distraction-free time.

## 17. Notifications and background operation

Notifications or a foreground service may be needed depending on the
final Android implementation and the expected behaviour while the app is
backgrounded.

Product requirements: - Do not request notification permission without
explaining its purpose. - If notification permission is denied, explain
any effect on background behaviour accurately. - The timer's data
integrity must not depend solely on a notification being visible. - Do
not add unrelated notification features in V1. - The exact background
execution strategy must be decided in `12_TECHNICAL_ARCHITECTURE.md`
after choosing the Android framework and target OS behaviour.

## 18. Privacy and permissions

-   Focus session records are personal activity data.
-   Store only the information needed for the feature.
-   Do not transmit focus history to third parties in V1.
-   Do not request usage-access permission for distraction detection;
    that feature is explicitly out of scope.
-   Any notification or foreground-service permission/notice must
    correspond to the actual implementation and Android platform
    requirements.
-   Provide clear user-facing explanations for permission requests.

## 19. Error handling

### Session start failure

-   Do not navigate to an active timer screen.
-   Preserve the selected task or entered description.
-   Explain that the session could not be started.
-   Allow retry.

### Pause/resume failure

-   Do not display a state that differs from the persisted state.
-   Refresh from the session source of truth.
-   Explain the issue and provide a safe next action.

### Finish failure

-   Do not show a false success message.
-   Keep the session recoverable.
-   Prevent creation of a duplicate finished session on retry.

### Recovery failure

-   Preserve the last known reliable state.
-   Do not guess a duration.
-   Show a clear message and provide a safe recovery route.

## 20. Accessibility and interaction details

-   The timer value must be readable at larger text sizes.
-   Controls must have descriptive labels, not icon-only meanings.
-   Running and Paused states must be communicated in text as well as
    color.
-   Pause and Finish must be visually and spatially distinct.
-   Use adequate touch targets, preferably at least 48 dp.
-   Avoid excessive animation or flashing timer effects.
-   Screen readers should receive useful timer updates without
    announcing every second.
-   Confirm destructive or irreversible actions clearly.

## 21. Acceptance criteria

### Starting sessions

-   [ ] User can start a session linked to an existing task.
-   [ ] User can create a task and start a linked session.
-   [ ] User can start general focus without a task.
-   [ ] General focus description is optional.
-   [ ] Task title is not requested again when starting from a task.
-   [ ] Only one active or paused session can exist.

### Timer controls

-   [ ] Start creates one persisted session.
-   [ ] Pause stops adding to focused duration.
-   [ ] Resume continues the same session.
-   [ ] Finish saves the final duration from reliable intervals.
-   [ ] Paused time is excluded.
-   [ ] Repeated taps do not create duplicate sessions or transitions.

### Recovery

-   [ ] Ordinary backgrounding and app return preserve the correct timer
    state.
-   [ ] Process recreation restores a recoverable session.
-   [ ] Unknown interruption gaps are not silently counted.
-   [ ] Device clock changes do not create impossible durations.
-   [ ] The user can understand and resolve an interrupted session.

### History and integration

-   [ ] Finished sessions appear in history.
-   [ ] Task-linked and general sessions are distinguishable.
-   [ ] Session duration contributes correctly to Home and Insights.
-   [ ] Completing a task does not silently finish a session.
-   [ ] Finishing a session does not automatically complete its task.
-   [ ] Historical focus records survive task edits/deletion according
    to the data model.

### UX and accessibility

-   [ ] The active screen emphasizes timer, activity context, state, and
    controls.
-   [ ] Running and Paused states are visually and textually clear.
-   [ ] The screen works in Light and Dark themes.
-   [ ] Controls have accessible labels and adequate touch targets.
-   [ ] Error and recovery states do not lose session data.

## 22. Open decisions for technical architecture

Resolve these before implementing the timer engine:

1.  Final Android framework and language.
2.  Whether a foreground service is needed for reliable background
    timing in the chosen app design.
3.  Which clock sources and timestamp strategy will be used for running
    intervals and recovery.
4.  Exact behaviour after force-stop, reboot, and long interruption.
5.  Whether recovered sessions can be resumed or only finished at the
    last confirmed time.
6.  Whether users can edit or delete historical sessions in V1.
7.  Exact local-date attribution for sessions crossing midnight.
8.  How a task snapshot is retained when the linked task is renamed or
    deleted.
9.  Whether the active session screen can be restored from every
    supported navigation path.
10. Which Android permissions or system notices are genuinely required
    by the final implementation.

Do not invent timing or recovery behaviour in the UI layer. The timer
domain logic must be centralized and testable.

## 23. Related documents

-   `01_PRODUCT_OVERVIEW.md` --- product direction and V1 scope.
-   `02_PRODUCT_REQUIREMENTS.md` --- cross-module requirements.
-   `03_UI_UX_GUIDELINES.md` --- shared design and interaction rules.
-   `04_HOME_DASHBOARD.md` --- Focused Time metric and Start Focus
    action.
-   `05_DAILY_PLANNING.md` --- task selection and task-linked start
    flow.
-   `09_LIFE_ANALYTICS.md` --- historical focus trends.
-   `11_DATA_ARCHITECTURE.md` --- session schema, timestamps, and
    relationships.
-   `12_TECHNICAL_ARCHITECTURE.md` --- Android timer and lifecycle
    implementation.
-   `13_IMPLEMENTATION_TESTING.md` --- timer, recovery, and
    data-integrity tests.
