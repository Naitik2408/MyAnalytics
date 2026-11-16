# Life Analytics

An Android-only personal life-tracking app for planning daily tasks,
recording focus sessions, tracking habits and finances, and
understanding patterns through clear summaries and charts.

**Primary goal:** Minimum input, maximum meaningful output.

Life Analytics is designed to help one person observe their daily
activities using their own recorded data. It should make useful
information easy to capture and easy to understand, without turning
everyday tracking into a complicated process.

## Project status

This repository contains the product and implementation documentation
for Life Analytics V1. The documentation is being prepared before
implementation so that the Antigravity Remote coding agent has a clear,
consistent source of truth.

The application is intended to be developed in **Antigravity Remote**,
built there as an Android APK, and then downloaded and installed on the
user's Android phone for real-device testing.

Do not assume that the Android project or APK already exists merely
because this documentation exists.

## V1 product scope

### Included modules

1.  **Home Dashboard** --- daily overview of tasks, focus time, habits,
    and expenses.
2.  **Daily Planning** --- lightweight task creation, scheduling,
    completion, and history.
3.  **Active Work & Focus Timer** --- general or task-linked focus
    sessions with pause/resume and recovery handling.
4.  **Daily Habits** --- habit definitions, schedules, and dated
    completion records.
5.  **Finance** --- manually recorded income and expenses.
6.  **Life Analytics / Insights** --- daily, weekly, and monthly
    summaries, charts, and evidence-based observations.
7.  **Daily Review** --- automatic daily summary with optional personal
    reflection.
8.  **Settings** --- app preferences and only the system access required
    by approved features.

### Explicitly out of scope for V1

-   Learning Process module.
-   Career Goal Tracker.
-   AI recommendations or an in-app AI assistant.
-   Voice entry or voice journaling.
-   Advanced distraction detection.
-   Multi-user, social, or team features.
-   Desktop or web application.
-   Mandatory account, login, cloud database, or backend server.
-   Automatic bank integration.
-   Investment, tax, UPI, or multi-currency conversion features.
-   Reminders or exact-alarm scheduling unless separately approved.

Do not add excluded features simply because a library makes them easy to
implement. Any scope change must be explicitly approved and reflected in
the documentation.

## Product principles

-   Keep user input minimal and avoid duplicate data entry.
-   Save each business fact once; derive dashboard and analytics values
    from source records.
-   Keep V1 local-first and usable offline.
-   Use clear, neutral, non-judgemental language.
-   Distinguish zero from missing, unavailable, or insufficient data.
-   Do not infer productivity, attention, mood, or causes without
    evidence.
-   Preserve historical records when schedules or current settings
    change.
-   Ask for Android permissions only when a user invokes a feature that
    genuinely needs them.
-   Never let denial of an optional permission disable unrelated app
    functionality.
-   Prefer simple, maintainable implementation over unnecessary
    abstraction.

## Technology direction

The recommended implementation stack is:

  -----------------------------------------------------------------------
  Area                                Recommendation
  ----------------------------------- -----------------------------------
  Platform                            Native Android

  Language                            Kotlin

  UI                                  Jetpack Compose

  Build                               Gradle Wrapper

  Database                            Room over SQLite

  Architecture                        MVVM with pragmatic Clean
                                      Architecture principles

  Async operations                    Kotlin Coroutines and Flow

  Navigation                          Navigation Compose

  Preferences                         DataStore

  Dependency injection                Hilt only if it remains
                                      straightforward; manual injection
                                      is acceptable

  Charts                              One compatible, maintained Compose
                                      chart library after verification

  Testing                             Unit, database, UI, and Android
                                      integration tests
  -----------------------------------------------------------------------

Exact JDK, Gradle, Android Gradle Plugin, Kotlin, Compose, SDK, and
library versions must be verified as a compatible set in the actual
Antigravity Remote environment and then pinned. Do not guess version
compatibility.

V1 does not require Firebase, Node.js, MongoDB, a remote API, or a login
service.

## Development and delivery workflow

The required workflow is:

1.  Inspect and prepare the Antigravity Remote environment.
2.  Implement the Android project in small milestones.
3.  Run relevant tests and Gradle builds continuously.
4.  Investigate and repair errors autonomously using the rules in
    `AGENTS.md`.
5.  Generate a debug APK at meaningful milestones.
6.  Verify the APK artifact exists and its exact path and export
    mechanism.
7.  Provide the APK through the remote platform's supported download
    mechanism.
8.  Install and test the APK on the user's Android phone.
9.  Fix issues found during real-device testing.
10. Produce a verified release APK when release criteria are met.

The remote environment must be checked for JDK, Android SDK, Gradle,
dependency access, disk space, and artifact export capability before
implementation assumes they are available.

A successful compile is not proof of correct phone behaviour.
Device-specific permission, lifecycle, notification, background
restriction, and installation behaviour must be tested on a real device
or explicitly marked as not yet verified.

## Autonomous debugging requirement

The coding agent must not stop at the first recoverable error.

For a failure, it must: - Capture and inspect the relevant error
output. - Identify a likely root cause. - Apply a targeted,
evidence-based repair. - Rerun the smallest relevant test. - Run broader
build/test checks after the targeted check passes. - Keep a record of
attempts and outcomes.

For recoverable issues, make up to three distinct, reasoned repair
attempts before asking the user. Do not repeat the same failed command
without a meaningful change and call it another attempt.

Ask the user sooner only when an account permission, credential,
approval, external action, irreversible decision, or genuinely
unavailable information/capability is required.

Never: - Hide an error or claim success without verification. - Disable
tests or validation just to obtain a green build. - Delete user data or
reset the database as a generic fix. - Add fake credentials or commit
secrets. - Make speculative unrelated changes. - Ask the user to
diagnose something the agent can reasonably investigate.

When escalating, explain what failed, evidence collected, checks
performed, repair attempts and results, why the agent is blocked, and
the exact user action required.

## Repository documentation

The detailed specifications are in `docs/`:

  -----------------------------------------------------------------------
  File                                Purpose
  ----------------------------------- -----------------------------------
  `01_PRODUCT_OVERVIEW.md`            Product vision, scope, and
                                      principles

  `02_PRODUCT_REQUIREMENTS.md`        Functional and non-functional
                                      requirements

  `03_UI_UX_GUIDELINES.md`            Visual system, interaction,
                                      accessibility

  `04_HOME_DASHBOARD.md`              Dashboard layout, metrics, and
                                      states

  `05_DAILY_PLANNING.md`              Task planning and task behaviour

  `06_ACTIVE_WORK_FOCUS.md`           Focus session lifecycle and
                                      duration rules

  `07_DAILY_HABITS.md`                Habit schedules and dated
                                      completion

  `08_FINANCE.md`                     Income, expense, and period
                                      calculations

  `09_LIFE_ANALYTICS.md`              Analytics, charts, and comparisons

  `10_DAILY_REVIEW.md`                Daily summary and optional
                                      reflection

  `11_DATA_ARCHITECTURE.md`           Entities, relationships, and source
                                      of truth

  `12_TECHNICAL_ARCHITECTURE.md`      Android stack, project structure,
                                      remote build and APK delivery

  `13_IMPLEMENTATION_TESTING.md`      Implementation sequence, autonomous
                                      debugging, tests, and release
                                      checks
  -----------------------------------------------------------------------

`AGENTS.md` contains mandatory project-wide instructions for the coding
agent. It must be read before implementation and followed throughout the
project.

## Getting started

### Before implementation

1.  Read `AGENTS.md` completely.
2.  Read the documentation in the numbered order above.
3.  Inspect the actual remote environment and report available tools and
    limitations.
4.  Confirm the project directory and whether an Android project already
    exists.
5.  Verify the SDK/JDK/Gradle setup and APK artifact export path.
6.  Report any genuine blocker before making assumptions.

### During implementation

-   Follow the milestone sequence in `13_IMPLEMENTATION_TESTING.md`.
-   Keep changes small and testable.
-   Update documentation when an approved decision changes.
-   Do not add unapproved features or dependencies.
-   Keep the project buildable at each meaningful milestone.

### Building a debug APK

Once the Android Gradle project exists, the typical command is:

``` bash
./gradlew assembleDebug
```

The conventional output path for a single app module is:

``` text
app/build/outputs/apk/debug/app-debug.apk
```

Verify the actual path in the current project; do not assume it without
checking.

### Running tests

Typical commands include:

``` bash
./gradlew test
./gradlew assembleDebug
```

If a configured emulator or connected Android device is available:

``` bash
./gradlew connectedDebugAndroidTest
```

Report tests that could not run as **not run**, not passed.

## Important implementation notes

-   The app is intended for Android phones; do not build a desktop
    `.exe`.
-   The expected installable deliverable is an `.apk`.
-   Keep debug and release APKs clearly distinguished.
-   Release signing keys and credentials must never be committed to
    source control.
-   Keep personal finance and reflection data local in V1.
-   Do not request storage, camera, contacts, location, or microphone
    permissions for the current V1 scope.
-   Notification permission is relevant only if a notification feature
    is approved and implemented.
-   Do not introduce a foreground service or exact-alarm access unless a
    verified requirement justifies it.
-   Do not claim real-device verification unless it has actually been
    performed.

## Definition of done

The V1 project is ready for handover only when:

-   Approved V1 modules are implemented.
-   Core data rules and calculations match the specifications.
-   Relevant automated tests pass.
-   Critical and high-severity defects are resolved.
-   Permission denial and recovery behaviour are handled.
-   The remote Gradle build succeeds.
-   The APK is generated, inspected, and available through a verified
    download route.
-   Installation and manual phone-testing instructions are provided.
-   Unrun tests, known limitations, and remaining issues are disclosed.
-   Release signing and key-retention handling are documented if a
    release APK is produced.

------------------------------------------------------------------------

**Product principle to remember:** Build a dependable, simple personal
tool. Do not sacrifice the remote build workflow, data integrity, or
real-phone reliability for extra features or visual complexity.
