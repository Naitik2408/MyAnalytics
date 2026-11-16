# Life Analytics --- Implementation & Testing Plan

**Document:** `13_IMPLEMENTATION_TESTING.md`\
**Product:** Life Analytics\
**Version:** 1.0 (V1)\
**Status:** Implementation and quality plan\
**Platform:** Android-only\
**Development environment:** Antigravity Remote\
**Primary deliverable:** Verified, installable APK\
**Related documents:** `01_PRODUCT_OVERVIEW.md` through
`12_TECHNICAL_ARCHITECTURE.md`

------------------------------------------------------------------------

## 1. Purpose

This document defines how Life Analytics will be implemented, tested,
debugged, and delivered as an Android APK.

The project will be developed in Antigravity Remote. The user will
install the generated APK on an Android phone and verify the real-world
experience.

Testing is not a final phase performed only after coding. Every
implementation milestone must include appropriate checks.

**Core quality principle:** When an error occurs, the coding agent must
investigate it, attempt reasonable fixes, rebuild, and verify the result
before asking the user for help. It must not stop at the first
recoverable error or repeatedly apply blind changes.

## 2. Non-negotiable development rules

1.  Read the relevant product, UX, data, and technical specifications
    before implementing a feature.
2.  Implement one coherent milestone at a time.
3.  Build and run relevant tests after each milestone.
4.  Never claim a build, test, APK, or feature succeeded unless the
    result was actually verified.
5.  When an error occurs, inspect the full relevant output and identify
    the likely root cause.
6.  Try safe, evidence-based fixes autonomously before asking the user.
7.  After each fix, rerun the smallest relevant test and then the
    broader build/test checks.
8.  Do not hide errors with broad exception handling, skipped tests,
    disabled checks, or fake success states.
9.  Do not delete user data, reset the database, remove important
    project files, or weaken security to make a test pass.
10. Ask the user only when a decision, credential, permission, external
    action, or information genuinely unavailable to the agent is
    required.
11. Keep a clear record of what was attempted, what changed, and what
    remains unresolved.
12. Preserve a last-known-good build where practical.

## 3. Autonomous error-resolution protocol

This section is mandatory for the Antigravity agent.

### 3.1 Expected behaviour

When a command, build, test, or runtime check fails, the agent must
follow this loop:

``` text
Failure detected
      │
      ▼
Capture complete error output
      │
      ▼
Classify the failure and inspect relevant files/configuration
      │
      ▼
Identify likely root cause
      │
      ▼
Apply one minimal, evidence-based fix
      │
      ▼
Run the smallest relevant verification
      │
      ├── Pass ──► Run broader build/tests
      │
      └── Fail ──► Reassess root cause and try another justified approach
                         │
                         ▼
              Review attempt history and remaining options
                         │
                         ▼
              Ask user only if genuinely blocked
```

### 3.2 Retry and investigation expectations

For a recoverable failure, the agent should make up to **three distinct,
reasoned repair attempts** before escalating to the user. This is a
default investigation budget, not permission to repeat the same command
or make arbitrary edits.

Each attempt must: - State the current root-cause hypothesis internally
or in the work log. - Inspect evidence relevant to that hypothesis. -
Make a targeted change. - Rerun the relevant verification. - Record the
outcome.

A distinct attempt means a meaningfully different diagnosis or repair
strategy. Re-running the same failed command without changing anything
does not count as another attempt.

If the failure clearly requires a user action immediately (for example,
account access, a missing secret, an unavailable approval, or a remote
environment restriction), do not waste retries. Explain the exact
blocker and ask for the minimum necessary input.

### 3.3 Escalation conditions

Ask the user for help when: - The remote environment lacks a required
capability and the agent cannot install or enable it. - An account
sign-in, permission, approval, or credential is required. - A product
decision is ambiguous and cannot safely be inferred from approved
documents. - A destructive or irreversible action would be needed. - The
available evidence is insufficient to choose safely between materially
different fixes. - Three distinct, reasonable repair attempts have
failed. - The problem is caused by an external service or infrastructure
outside the agent's control.

The request to the user must include: 1. What failed. 2. What has
already been checked. 3. Which repair attempts were made and their
outcomes. 4. What exact user action or decision is required. 5. What
work can continue independently, if any.

Do not ask vague questions such as "It isn't working, what should I do?"

### 3.4 Safety boundaries during autonomous repair

The agent must not: - Delete or recreate the project to solve an
ordinary build error. - Delete the database or user data to bypass a
persistence issue. - Disable tests, linting, validation, or permission
checks merely to get a green result. - Downgrade security settings or
request broader permissions without justification. - Insert fake
credentials, signing keys, or environment values. - Change unrelated
features while fixing one failure. - Claim that a warning is harmless
without checking its impact. - Continue making speculative changes after
evidence no longer supports them.

For high-risk changes, stop and ask for approval before acting.

## 4. Failure classification and repair playbook

  ---------------------------------------------------------------------------------------
  Failure type            Initial investigation         Reasonable repair path
  ----------------------- ----------------------------- ---------------------------------
  JDK/Gradle              Read full Gradle error;       Align to a documented compatible
  incompatibility         inspect wrapper, AGP and JDK  version set; rerun wrapper/build
                          versions                      

  Android SDK missing     Inspect SDK environment and   Install supported required
                          installed platforms/build     components if remote permissions
                          tools                         allow; otherwise escalate

  Dependency resolution   Identify exact                Check repository configuration,
                          artifact/repository/version   network, version compatibility
                          failure                       and transitive conflict

  Kotlin/Compose compile  Inspect first meaningful      Fix source/API/plugin mismatch;
  error                   compiler error and related    compile affected module again
                          source                        

  Room schema/migration   Inspect schema, migration     Correct entity/query/migration;
  failure                 path, failing query and test  run migration tests without
                          database                      deleting data

  Unit test failure       Inspect assertion, fixture    Determine product bug vs test
                          and changed logic             bug; fix correct source and rerun

  UI test failure         Inspect semantics,            Stabilize state/semantics; do not
                          navigation, timing and test   add arbitrary sleeps as the
                          output                        default fix

  Permission behaviour    Inspect manifest, target SDK, Correct manifest/request flow and
                          API level and permission      test grant/deny/revoke paths
                          state                         

  Focus timer inaccurate  Inspect persisted timestamps, Fix time calculation/recovery;
                          pause intervals and lifecycle add regression tests
                          events                        

  APK generation failure  Inspect Gradle task output,   Fix build configuration and rerun
                          signing config and output     package task
                          directories                   

  APK installation        Inspect package ID, min SDK,  Correct
  failure                 signature, device API and     incompatibility/signing/package
                          install error                 configuration and rebuild

  Runtime crash           Capture stack trace and       Fix root cause and add regression
                          reproduce exact flow          test

  Remote artifact         Verify actual output path and Use supported artifact export
  unavailable             platform artifact support     path; ask user only if remote
                                                        capability is unavailable
  ---------------------------------------------------------------------------------------

## 5. Implementation sequence

Implement in small, verifiable milestones. Do not start every feature at
once.

### Milestone 0 --- Remote environment preflight

-   Inspect remote operating system and architecture.
-   Verify JDK, Gradle Wrapper, Android SDK, platform tools, and build
    tools.
-   Verify dependency repository access.
-   Confirm disk space and project persistence.
-   Confirm whether emulator/instrumentation testing is available.
-   Confirm how generated APK artifacts can be exported to the user.
-   Record actual environment versions and limitations.

**Exit criteria:** A minimal native Android project can build a debug
APK in the remote environment, and the artifact can be located and its
export route is known.

### Milestone 1 --- Project foundation

-   Create Android project and package ID.
-   Configure Kotlin, Compose, Gradle and SDK versions.
-   Add app theme and root navigation shell.
-   Add Room and DataStore foundations.
-   Configure unit and UI test source sets.
-   Add project instructions and build commands.

**Exit criteria:** Clean build succeeds; initial app launches in
available test environment; no unnecessary permissions or backend
dependencies.

### Milestone 2 --- Data foundation

-   Implement approved entities and DAOs.
-   Add database and repositories.
-   Add date/time and money utility rules.
-   Add migration test strategy.
-   Add shared domain calculations.

**Exit criteria:** DAO and domain tests pass; schema follows
`11_DATA_ARCHITECTURE.md`.

### Milestone 3 --- Daily Planning

-   Implement task list and date selection.
-   Implement create/edit/complete/reopen/delete.
-   Validate required title and category.
-   Connect task-linked focus entry point.

**Exit criteria:** Task CRUD and date attribution tests pass; data
persists after app restart.

### Milestone 4 --- Focus Timer

-   Implement general and task-linked session starts.
-   Implement pause/resume/finish.
-   Persist pause intervals and duration.
-   Implement lifecycle and interruption recovery.
-   Enforce one active/paused session.

**Exit criteria:** Duration and lifecycle tests pass; no unknown
interval is silently counted as focus.

### Milestone 5 --- Daily Habits

-   Implement habit create/edit/deactivate.
-   Implement effective-dated schedules.
-   Implement dated completion toggle.
-   Preserve historical schedule/completion.

**Exit criteria:** Schedule, due-date, history and uniqueness tests
pass.

### Milestone 6 --- Finance

-   Implement income/expense forms and transaction list.
-   Implement edit/delete and category selection.
-   Implement period totals and net.
-   Validate amount and currency handling.

**Exit criteria:** Money arithmetic tests pass; no floating-point
currency errors.

### Milestone 7 --- Home Dashboard

-   Connect task, focus, habit and expense summaries.
-   Add quick actions and previews.
-   Implement loading, empty and error states.

**Exit criteria:** Dashboard metrics match source module queries for the
same dates.

### Milestone 8 --- Insights

-   Implement daily, weekly and monthly periods.
-   Add charts and textual observations only where data supports them.
-   Add equivalent-period comparisons when enough data exists.

**Exit criteria:** Chart values match tested domain calculations; empty
and insufficient data are distinct from zero.

### Milestone 9 --- Daily Review

-   Implement automatic daily summary.
-   Implement planned-versus-actual display.
-   Implement optional reflection fields.
-   Implement draft/completed status and history.

**Exit criteria:** Review does not duplicate source data and reflection
persists independently.

### Milestone 10 --- Settings, permissions, and polish

-   Implement theme and currency preferences.
-   Implement only approved permission flows.
-   Verify notification state if reminders are included.
-   Review accessibility, keyboard behaviour and error states.
-   Remove unused dependencies and debug-only UI.

**Exit criteria:** Permission and settings tests pass; no unnecessary
permission is requested.

### Milestone 11 --- Release readiness

-   Run full test suite.
-   Run clean debug and release builds.
-   Inspect merged manifest and dependencies.
-   Generate APK.
-   Verify artifact path, size, package ID, version and signing state.
-   Prepare manual phone test checklist and known limitations.

**Exit criteria:** Build and test evidence is recorded; APK is actually
available for download; unrun tests are clearly identified.

## 6. Test strategy

Testing must be continuous and layered.

### 6.1 Unit tests

Test pure business rules without Android UI: - Task validation and
completion. - Focus duration with multiple pause intervals. -
Interrupted session and unknown gaps. - Session crossing midnight
according to the chosen attribution policy. - Habit schedule
weekdays/custom schedules. - Schedule changes and historical due
dates. - Habit completion uniqueness. - Finance income, expense and net
calculations. - Currency minor-unit arithmetic. - Daily Review
uniqueness and optional fields. - Daily/weekly/monthly metric
calculations. - Empty vs zero vs insufficient-data states.

### 6.2 Database tests

-   Insert, query, update and delete each entity.
-   Foreign-key behaviour.
-   Unique habit/date completion.
-   Unique daily review date.
-   Non-overlapping habit schedule periods.
-   Atomic focus start/pause/resume/finish operations.
-   Transaction rollback on partial failure.
-   Migrations from every supported previous schema.
-   Historical data preservation.

### 6.3 UI tests

-   Navigation and back behaviour.
-   Create/edit/complete/reopen task.
-   Start/pause/resume/finish focus.
-   Habit check/uncheck.
-   Add/edit/delete income and expense.
-   Save/edit/complete Daily Review.
-   Empty/loading/error states.
-   Keyboard does not hide essential form controls.
-   Light/Dark theme readability.
-   Accessibility labels and touch targets.

### 6.4 Integration and real-device tests

Where remote emulator/device testing is available, test there.
Otherwise, mark the scenario as requiring manual phone verification.

-   APK installation and first launch.
-   Permission grant, deny and revoke.
-   Notification channel disabled.
-   App background/foreground.
-   Screen lock.
-   Process death and relaunch.
-   Force-stop and relaunch.
-   Device reboot.
-   Timezone or device clock change.
-   Battery saver/background restriction.
-   Low storage.
-   App upgrade over an earlier installed version.
-   Uninstall/reinstall behaviour and data consequences.

Do not mark an unavailable emulator/device test as passed.

## 7. Permission test matrix

The app must request only permissions required by approved V1
functionality.

  -----------------------------------------------------------------------
  Scenario                            Expected result
  ----------------------------------- -----------------------------------
  No optional permission requested    Core app works

  Notification permission granted     Approved notification feature can
                                      operate

  Notification permission denied      App remains usable; reminders
                                      unavailable

  Permission revoked in Settings      App detects state on resume and
                                      updates UI

  Notification channel disabled       Explain channel-level setting; do
                                      not claim notifications are enabled

  User declines repeatedly            Do not trap in repeated request
                                      dialogs

  API level has different permission  Use correct platform behaviour
  model                               

  Optional permission unavailable     Unrelated features continue to work
  -----------------------------------------------------------------------

If reminders are deferred from V1, notification permission tests verify
that the app does not request the permission unnecessarily.

## 8. Feature acceptance test matrix

### Home Dashboard

-   [ ] Task completion ratio matches task records.
-   [ ] Focus time matches focus domain calculation.
-   [ ] Today's expenses match expense transactions.
-   [ ] Habit completion ratio includes only habits due.
-   [ ] Empty state is not presented as a real zero when data is
    unavailable.
-   [ ] Quick actions navigate to the correct flow.

### Daily Planning

-   [ ] Required title and category are validated.
-   [ ] Planned date defaults correctly.
-   [ ] Optional time and estimate can be omitted.
-   [ ] Completion and reopening persist.
-   [ ] Deleting a task does not erase linked focus history.

### Focus Timer

-   [ ] Only one active/paused session can exist.
-   [ ] Pause intervals are excluded.
-   [ ] General focus works without a task.
-   [ ] Linked focus references the correct task.
-   [ ] Backgrounding does not reset the persisted session.
-   [ ] Unknown interruption gap is not counted.
-   [ ] Recovery clearly identifies interrupted sessions.

### Daily Habits

-   [ ] Schedule types work correctly.
-   [ ] Unscheduled dates are not counted as missed.
-   [ ] Completion toggles the same date record.
-   [ ] Schedule edits preserve historical due dates.
-   [ ] Deactivation preserves history.

### Finance

-   [ ] Amount must be positive.
-   [ ] Income and expense totals are separate.
-   [ ] Net equals income minus expenses.
-   [ ] Net is not labelled account balance.
-   [ ] Edit/delete updates summaries.
-   [ ] Money arithmetic uses exact minor units.

### Insights

-   [ ] Daily, weekly and monthly ranges are correct.
-   [ ] Charts match source calculations.
-   [ ] Comparisons use equivalent periods.
-   [ ] Insufficient data is explained.
-   [ ] No unsupported causal or psychological claim is shown.

### Daily Review

-   [ ] One review per local date.
-   [ ] Reflection fields are optional.
-   [ ] Draft and completed states behave correctly.
-   [ ] Source summaries update when source records change.
-   [ ] Review edits do not modify source records.

## 9. Regression testing

Whenever a bug is fixed: 1. Reproduce it before the fix if possible. 2.
Add a test that fails for the original bug. 3. Apply the fix. 4. Run the
new test. 5. Run tests for the affected module. 6. Run the broader
build/test suite before release. 7. Record the regression test and
result.

A fix is not complete merely because the original error message
disappeared.

## 10. Remote build and APK verification

### 10.1 Build verification

At minimum, use the project's Gradle Wrapper:

``` bash
./gradlew --version
./gradlew clean
./gradlew test
./gradlew assembleDebug
```

Run Android instrumentation tests only when a configured emulator/device
is available:

``` bash
./gradlew connectedDebugAndroidTest
```

Record the exact command, exit status, and relevant output. Do not
describe a command as successful if it was not executed to completion.

### 10.2 APK verification

After build: - Confirm the expected APK file exists. - Confirm it is
non-empty. - Inspect package/application ID. - Inspect version name and
version code. - Confirm minimum SDK. - Confirm signing state and build
type. - Inspect manifest permissions. - Confirm the artifact is
accessible through the remote platform's supported download mechanism. -
Do not invent or guess the artifact path or download link.

A debug APK is suitable for development installation. A release APK must
be signed with the intended release key. Keep signing secrets outside
source control.

### 10.3 Phone installation checklist

The user should be given: - Exact APK download artifact. - Clear
installation steps. - Permission explanation. - Build version and
date. - Known limitations. - Manual verification checklist.

Phone checks: - \[ \] APK installs successfully. - \[ \] App opens
without immediate crash. - \[ \] Home screen loads. - \[ \] Task can be
created and remains after reopening. - \[ \] Focus session can start,
pause, resume and finish. - \[ \] Background/force-stop recovery is
understandable. - \[ \] Habit completion persists. - \[ \] Finance
transaction persists and summaries update. - \[ \] Insights reflect real
records. - \[ \] Daily Review saves and reopens. - \[ \] Optional
permission denial does not break core app. - \[ \] Light and Dark themes
are readable.

Only checks actually performed may be marked passed.

## 11. Test data and environments

-   Use synthetic test fixtures only in test environments.
-   Never seed fake activities into a user's real app.
-   Use deterministic clocks in unit tests for date/time calculations.
-   Test boundary dates, month changes, leap years, week starts,
    timezone changes, and midnight transitions.
-   Keep development and release application IDs/configuration
    appropriately separated if needed.
-   Do not use real personal financial or reflection content in logs or
    test fixtures.

## 12. Defect severity and prioritization

  -----------------------------------------------------------------------
  Severity                Meaning                 Release decision
  ----------------------- ----------------------- -----------------------
  Critical                Data loss,              Block release
                          security/privacy        
                          breach, app cannot      
                          launch, or core records 
                          corrupted               

  High                    Core feature unusable,  Block release
                          incorrect financial     
                          calculation, or serious 
                          timer/data integrity    
                          issue                   

  Medium                  Feature works with a    Fix before release
                          workaround; noticeable  where practical;
                          incorrect or confusing  otherwise document and
                          behaviour               obtain user decision

  Low                     Minor visual or wording May defer with
                          issue without           documented follow-up
                          functional impact       
  -----------------------------------------------------------------------

Do not downgrade severity merely to meet a delivery date.

## 13. Completion report format

At each major milestone, provide a concise report:

-   **Implemented:** What changed.
-   **Build:** Exact build command and pass/fail.
-   **Tests:** Which tests ran and results.
-   **Not tested:** Checks unavailable in the remote environment.
-   **Issues:** Remaining known issues.
-   **Artifact:** Exact APK path/link if generated and verified.
-   **Next step:** The next milestone or the exact user input required.

Never say "all tests passed" if only unit tests or only a build was run.

## 14. User escalation format

If autonomous repair cannot safely resolve an issue, ask the user with a
specific explanation:

``` text
Issue:
What failed and what the user may notice.

Evidence:
Relevant error and environment information.

Checks already completed:
What was inspected.

Repair attempts:
1. Attempt and result.
2. Attempt and result.
3. Attempt and result.

Why I am blocked:
The exact missing access, decision, or capability.

What I need from you:
One specific action or answer.

Work that can continue:
Any independent implementation or tests that are not blocked.
```

Do not ask the user to diagnose a technical issue that the agent can
reasonably investigate itself.

## 15. Final release checklist

### Product and implementation

-   [ ] All approved V1 modules implemented.
-   [ ] Explicitly excluded features have not slipped into scope.
-   [ ] UI follows the approved design guidelines.
-   [ ] Data ownership follows `11_DATA_ARCHITECTURE.md`.
-   [ ] Technical stack follows `12_TECHNICAL_ARCHITECTURE.md`.

### Quality

-   [ ] Critical and high-severity defects resolved.
-   [ ] Relevant unit tests pass.
-   [ ] Database tests and migrations pass.
-   [ ] UI tests pass where runnable.
-   [ ] Permission scenarios verified or explicitly marked not run.
-   [ ] Focus lifecycle scenarios verified or marked for manual phone
    testing.
-   [ ] No fake data or success states.
-   [ ] No unnecessary permissions or unused dependencies.

### APK

-   [ ] Clean build succeeds in Antigravity Remote.
-   [ ] APK exists and has been inspected.
-   [ ] Package ID, version and minimum SDK recorded.
-   [ ] Signing status verified.
-   [ ] APK artifact is actually downloadable.
-   [ ] Installation steps provided.
-   [ ] Known limitations and unrun tests disclosed.

### Handover

-   [ ] README contains setup and build instructions.
-   [ ] AGENTS.md contains implementation and autonomous repair rules.
-   [ ] Documentation index is current.
-   [ ] User receives the APK and a concise test report.
-   [ ] Release signing key handling is documented securely if release
    signing is configured.

## 16. Related documents

-   `01_PRODUCT_OVERVIEW.md` --- product vision and V1 scope.
-   `02_PRODUCT_REQUIREMENTS.md` --- functional and non-functional
    requirements.
-   `03_UI_UX_GUIDELINES.md` --- design and interaction standards.
-   `04_HOME_DASHBOARD.md` --- Home screen behaviour.
-   `05_DAILY_PLANNING.md` --- task planning.
-   `06_ACTIVE_WORK_FOCUS.md` --- timer lifecycle and duration rules.
-   `07_DAILY_HABITS.md` --- habit schedules and completion.
-   `08_FINANCE.md` --- finance transactions and calculations.
-   `09_LIFE_ANALYTICS.md` --- insights and chart calculations.
-   `10_DAILY_REVIEW.md` --- daily reflection.
-   `11_DATA_ARCHITECTURE.md` --- entities, relationships and data
    ownership.
-   `12_TECHNICAL_ARCHITECTURE.md` --- Android stack, layers, remote
    build and APK delivery.
