# Life Analytics --- Technical Architecture

**Document:** `12_TECHNICAL_ARCHITECTURE.md`\
**Product:** Life Analytics\
**Version:** 1.0 (V1)\
**Status:** Implementation architecture specification\
**Target:** Android-only, single-user, local-first application\
**Development environment:** Antigravity Remote\
**Primary deliverable:** Installable Android APK\
**Related documents:** `01_PRODUCT_OVERVIEW.md` through
`11_DATA_ARCHITECTURE.md`, `13_IMPLEMENTATION_TESTING.md`

------------------------------------------------------------------------

## 1. Purpose

This document translates the product and data specifications into an
implementable Android application architecture.

The most important delivery constraint is the intended workflow:

1.  Develop the complete Android project in Antigravity Remote.
2.  Build and test the project in that remote environment.
3.  Generate a downloadable APK artifact.
4.  Install that APK on the user's Android phone.
5.  Verify the real app on the phone, including permissions, lifecycle
    behaviour, and device-specific edge cases.

The architecture must not depend on the developer having Android Studio,
an emulator, a local Android SDK, or a locally connected phone. Those
tools may be useful if available, but they are not prerequisites for the
documented workflow.

**Primary success condition:** The remote project can be built
reproducibly and produces a valid APK that can be downloaded and
installed on the target Android phone.

A successful build alone is not proof that every feature works on a
physical device. Real-device verification remains part of acceptance.

## 2. Architecture principles

1.  Android-native application; no web wrapper.
2.  Kotlin-first implementation.
3.  Jetpack Compose for UI.
4.  Local-first storage; core features work offline.
5.  Room over SQLite for structured persistence.
6.  Clear separation between UI, domain rules, and data access.
7.  One source of truth for every business record and calculation.
8.  Minimal dependencies; avoid libraries without a concrete V1 need.
9.  No mandatory backend, account, cloud service, or network permission
    in V1.
10. Ask for Android permissions only when a user invokes a feature that
    needs them.
11. Handle denied permissions and restricted device states without
    crashing.
12. Build and test from the remote environment throughout development.
13. Never claim a feature or build works without evidence from a
    successful build/test.
14. Preserve user data through app restarts and schema migrations.

## 3. Chosen technology stack

The following is the recommended V1 stack. Versions must be selected as
a compatible set at project initialization, then pinned and documented.
Do not blindly use the newest version of each library independently.

  -----------------------------------------------------------------------
  Area                    Recommended choice      Reason
  ----------------------- ----------------------- -----------------------
  Platform                Native Android          Android is the only V1
                                                  target

  Language                Kotlin                  First-class Android
                                                  support and concise
                                                  code

  UI toolkit              Jetpack Compose         Native declarative UI

  Build system            Gradle Wrapper          Reproducible remote
                                                  builds

  Build scripts           Kotlin DSL              Consistent Kotlin-based
                                                  configuration

  Database                Room over SQLite        Typed entities, DAOs,
                                                  migrations

  UI architecture         MVVM                    Clear UI state
                                                  ownership

  Domain organization     Clean Architecture      Keep business rules
                          principles,             testable without
                          pragmatically applied   excessive abstraction

  Async work              Kotlin Coroutines and   Structured asynchronous
                          Flow                    operations and
                                                  observable data

  Navigation              Navigation Compose      Native Compose
                                                  navigation

  Dependency injection    Hilt, only if the       Consistent dependency
                          project setup remains   construction
                          straightforward         

  Preferences             DataStore               Small app preferences
                                                  such as theme

  Charts                  One maintained          Avoid custom chart
                          Compose-compatible      complexity
                          chart library, selected 
                          after a small           
                          compatibility check     

  Unit testing            JUnit and Kotlin test   Domain and calculation
                          tools                   verification

  UI testing              Compose UI testing      Screen interaction
                                                  verification

  Android instrumentation AndroidX test tools     Device/emulator
                                                  integration tests where
                                                  available
  -----------------------------------------------------------------------

### 3.1 Dependency selection rules

-   Prefer official AndroidX and Kotlin ecosystem libraries.
-   Add a third-party dependency only when it provides a meaningful
    feature that would be costly or risky to implement internally.
-   Verify that each dependency supports the chosen Kotlin, Compose
    compiler/plugin, Gradle, Android Gradle Plugin, and minimum Android
    SDK versions.
-   Pin plugin and library versions; avoid dynamic versions such as
    `latest`.
-   Keep a dependency inventory with purpose and version.
-   Remove unused dependencies.
-   Do not add Firebase, a web server, Node.js backend, MongoDB,
    analytics SDK, or login SDK for V1.
-   If Hilt introduces build complexity disproportionate to this small
    single-user app, use simple constructor injection/manual dependency
    wiring instead. The app must not be blocked by a DI framework.

## 4. Android SDK and build configuration

### 4.1 SDK strategy

-   Set `minSdk` only after reviewing the chosen Android APIs and the
    oldest Android version intended for support.
-   Set `compileSdk` to a stable SDK supported by the selected Android
    Gradle Plugin.
-   Set `targetSdk` to the current required/appropriate Android API
    level at implementation time and verify behaviour changes introduced
    by that target.
-   Do not guess SDK numbers in this document; record the actual pinned
    values in the project README and build configuration.
-   Test at least the minimum supported API and a recent Android API
    level where test devices or remote test infrastructure are
    available.

### 4.2 Reproducible build

-   Commit the Gradle Wrapper scripts and wrapper configuration.
-   Pin the Gradle distribution and Android Gradle Plugin versions.
-   Use a documented JDK version compatible with the selected Gradle/AGP
    pair.
-   Do not rely on a globally installed Gradle version.
-   Keep dependency repositories explicit and minimal.
-   Ensure the remote environment can access required SDK packages and
    Maven artifacts.
-   Do not place credentials, signing passwords, or private keys in
    source control.

### 4.3 Required build commands

The exact task names may be confirmed once the Android project is
created. The standard workflow should include:

``` bash
./gradlew --version
./gradlew clean
./gradlew test
./gradlew assembleDebug
```

If connected-device or emulator testing is available:

``` bash
./gradlew connectedDebugAndroidTest
```

A command that cannot run because no emulator/device is available must
be reported as not run, not as passed.

## 5. Project structure

Use a feature-oriented structure with shared core and data/domain
boundaries. Keep the structure understandable for a single developer and
an AI coding agent; do not create empty layers or excessive modules.

Suggested initial structure:

``` text
LifeAnalytics/
├── AGENTS.md
├── README.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/
│   └── wrapper/
├── gradlew
├── gradlew.bat
├── gradle.properties
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/<package-name>/
│       │   │   ├── MainActivity.kt
│       │   │   ├── app/
│       │   │   │   ├── LifeAnalyticsApp.kt
│       │   │   │   └── AppNavHost.kt
│       │   │   ├── core/
│       │   │   │   ├── common/
│       │   │   │   ├── designsystem/
│       │   │   │   ├── permissions/
│       │   │   │   ├── time/
│       │   │   │   └── error/
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   ├── dao/
│       │   │   │   │   ├── entity/
│       │   │   │   │   └── LifeAnalyticsDatabase.kt
│       │   │   │   └── repository/
│       │   │   ├── domain/
│       │   │   │   ├── model/
│       │   │   │   ├── repository/
│       │   │   │   └── usecase/
│       │   │   └── feature/
│       │   │       ├── home/
│       │   │       ├── planning/
│       │   │       ├── focus/
│       │   │       ├── habits/
│       │   │       ├── finance/
│       │   │       ├── insights/
│       │   │       ├── review/
│       │   │       └── settings/
│       │   ├── res/
│       │   └── assets/
│       ├── test/
│       └── androidTest/
└── docs/
```

Package names, exact paths, and test fixtures must be finalized when the
project is initialized. Do not create a separate Gradle module for every
feature unless build complexity or team scale justifies it.

## 6. Application architecture and data flow

### 6.1 Layer responsibilities

**Presentation / UI** - Compose screens and reusable UI components. -
Screen state and user events. - ViewModels expose immutable UI state. -
No direct SQL, database access, or business calculations in composables.

**Domain** - Business rules and shared calculations. - Use cases for
meaningful operations. - Domain models and repository interfaces. - Must
not depend on Android UI classes.

**Data** - Room entities, DAOs, database configuration. - Repository
implementations. - Mapping between database and domain models. -
Transaction boundaries and persistence details.

### 6.2 Example: adding an expense

``` text
Finance Screen
   │ user submits expense
   ▼
Finance ViewModel
   │ validates UI input and emits state
   ▼
AddTransaction Use Case
   │ validates domain rules
   ▼
Finance Repository
   │
   ▼
Room DAO
   │ transaction inserted
   ▼
Room observable query emits updated data
   ├── Finance screen updates
   ├── Home expense metric updates
   ├── Daily Review summary updates
   └── Insights totals/charts update
```

All dependent screens must read the same saved transaction through
shared repository/query logic. They must not write separate copies of
the expense.

### 6.3 UI state and errors

-   Use immutable screen state objects.
-   Represent loading, content, empty, and error states explicitly.
-   Expose one-way user events to ViewModels.
-   Do not swallow exceptions.
-   Convert expected failures into understandable domain/UI errors.
-   Keep unexpected technical details out of user-facing messages;
    preserve diagnostic details in safe development logs without
    personal content.

## 7. Database implementation

Use Room with SQLite, following `11_DATA_ARCHITECTURE.md`.

### 7.1 Required implementation elements

-   Room `@Entity` classes for the approved data models.
-   DAO interfaces for reads and writes.
-   A versioned Room database.
-   Repository implementations.
-   Type converters only where necessary.
-   Explicit database transactions for multi-step state changes.
-   Indices for date, status, and foreign-key query patterns.
-   Foreign-key behaviour aligned with historical data preservation.
-   Tested migrations for every schema change after first release.

### 7.2 Data rules

-   Store money using integer minor units or exact decimal-safe
    representation, never floating point.
-   Store calendar dates separately from event timestamps.
-   Store event instants unambiguously.
-   Keep one authoritative focus-duration calculation.
-   Enforce unique habit completion per habit/date.
-   Enforce unique daily review per date in the single-user V1 model.
-   Do not seed fake tasks, expenses, habits, or focus sessions in
    production.
-   Use database transactions for focus start/pause/resume/finish and
    habit schedule changes.

### 7.3 Database failure handling

-   Handle database exceptions without crashing the entire app where
    recovery is possible.
-   Never delete the database automatically as a generic response to an
    error.
-   If a migration is missing, fail safely and report the problem during
    development.
-   Preserve existing user data during upgrades.
-   Test migrations using representative prior database versions.
-   Provide a clear recovery path if database access fails.

## 8. Focus timer and Android lifecycle

The focus timer requires special treatment because Android may pause,
restrict, or terminate app processes.

### 8.1 Timer source of truth

-   Persist session start time and pause/resume intervals.
-   Derive elapsed focus duration from timestamps and valid intervals.
-   Do not rely on a UI coroutine or in-memory counter as the only
    source of truth.
-   Refresh the displayed timer from persisted session state.
-   Use a monotonic clock for measuring intervals within a running
    device session where appropriate, while retaining wall-clock
    timestamps for persistence and recovery.
-   Handle device reboot and wall-clock changes explicitly.
-   Unknown time after process death or reboot must not automatically be
    counted as focused time.

### 8.2 Background behaviour

-   The timer must remain logically recoverable if the user navigates
    away from the screen.
-   A foreground service is not automatically required for a simple
    persisted timer. Decide based on the final user-visible requirement
    and Android background-execution rules.
-   Do not request foreground-service permissions or show a persistent
    notification unless the selected implementation genuinely needs a
    foreground service.
-   If a foreground service is introduced, implement the service type
    and permissions required by the target Android version and explain
    the persistent notification to the user.
-   Test screen lock, app backgrounding, process recreation, force-stop,
    and reboot scenarios.

### 8.3 Single active session

-   Enforce at most one active or paused session.
-   Make start/pause/resume/finish operations atomic.
-   Recover the previous session state on app launch.
-   If state is uncertain, mark the session interrupted and ask the user
    to review it rather than inventing duration.

## 9. Android permissions and system integration

Permission handling is a first-class feature requirement, not a final
polish task.

### 9.1 Permission minimization

For every permission or special access, document: - Feature that needs
it. - Whether it is required or optional. - Android API levels where
behaviour changes. - Exact point in the user flow where it is
requested. - Explanation shown before the request, where appropriate. -
Behaviour when denied, revoked, restricted, or unavailable. - Settings
recovery route, if one exists.

Do not request permissions pre-emptively at first launch.

### 9.2 V1 permission inventory

  --------------------------------------------------------------------------------------------
  Capability                                   Permission/access       V1 decision
  -------------------------------------------- ----------------------- -----------------------
  Local Room database                          App-private storage     No storage runtime
                                                                       permission

  Tasks and habits                             None                    Work without special
                                                                       permission

  Focus timer                                  No runtime permission   Persist and recover
                                               for basic in-app timer  session state

  Notifications                                `POST_NOTIFICATIONS` on Request only when user
                                               Android 13/API 33+ when enables/uses reminders
                                               notifications are used  

  Scheduled reminders                          Notification permission Reminders remain
                                               plus appropriate        deferred unless
                                               scheduling API          explicitly included in
                                                                       V1

  Exact alarms                                 Special access on       Do not request unless
                                               applicable Android      exact timing is
                                               versions                essential and approved

  Camera/photos/location/contacts/microphone   Not required by current Do not declare or
                                               V1 scope                request
  --------------------------------------------------------------------------------------------

The manifest must contain only permissions actually required by
implemented features. Re-check the final merged manifest and
dependency-added permissions before release.

### 9.3 Permission request flow

1.  User invokes or enables a feature that needs permission.
2.  Explain the feature benefit in context.
3.  Launch the Android permission request.
4.  Handle the result.
5.  If granted, enable the feature.
6.  If denied, keep the rest of the app functional and explain what is
    unavailable.
7.  If the user later revokes permission in Settings, detect the changed
    state when the app resumes and update the UI.
8.  Offer a Settings action only when useful; never trap the user in a
    repeated permission dialog.

### 9.4 Permission edge cases

Test: - First-time grant. - First-time denial. - Repeated denial and
platform denial behaviour. - Permission revoked through system
Settings. - Permission disabled after an app update. - Notification
channel disabled even when app-level permission is granted. - Device API
levels with different permission behaviour. - App process recreation
after the permission flow. - App restored on a different device, if
backup is later enabled.

A denied optional permission must not crash the app or block unrelated
features.

### 9.5 Android Settings and device restrictions

-   Detect whether notifications are enabled at app and channel level
    where applicable.
-   Explain that battery optimization and OEM background restrictions
    may affect background work.
-   Do not claim that opening battery settings guarantees unrestricted
    execution.
-   Do not request battery-optimization exemption unless a clearly
    justified, policy-compliant feature requires it.
-   Avoid relying on exact alarms for non-critical reminders.
-   Provide an in-app explanation and a user-controlled Settings route
    when supported.
-   Handle device manufacturers' custom restrictions gracefully; do not
    assume all Android devices behave identically.

## 10. Navigation architecture

Use Navigation Compose with a small, explicit route model.

Primary bottom navigation: - Home - Finance - Activity - Insights

Secondary destinations: - Daily Planning - Habits - Focus session -
Daily Review - Settings

Requirements: - Preserve expected back-stack behaviour. - Avoid
duplicate destinations when tapping the selected bottom tab
repeatedly. - Restore suitable screen state after configuration changes
and process recreation. - Pass stable record IDs between screens, not
entire mutable database objects. - Handle invalid/deleted IDs with a
safe not-found state. - Do not make the app dependent on deep links or
external navigation in V1.

## 11. UI, design system, and charts

Follow `03_UI_UX_GUIDELINES.md`.

### 11.1 Design system

Implement reusable Compose components for: - Buttons and icon buttons. -
Text fields and form validation. - Metric cards. - Section headers. -
Empty/loading/error states. - Confirmation dialogs and bottom sheets. -
Date selection. - Consistent chart containers and legends.

### 11.2 Themes

-   Support Light and Dark themes.
-   Respect system theme by default if that is the approved preference.
-   Use the documented purple-blue design direction.
-   Ensure readable contrast and touch targets.
-   Do not hardcode colors separately in each screen.

### 11.3 Charts

-   Select one Compose-compatible chart dependency only after verifying
    current maintenance, API compatibility, accessibility, and build
    compatibility.
-   Keep chart input models independent of the library-specific chart
    API.
-   Provide text summaries and labels; do not communicate values through
    colour alone.
-   Handle empty and insufficient data distinctly from zero.
-   Avoid animations that harm performance or accessibility.
-   If a chart library becomes a build blocker, use a simpler native
    Compose visualization rather than compromising the APK build.

## 12. Asynchronous work and background scheduling

-   Use Coroutines for asynchronous work.
-   Use Flow/StateFlow for observable state where appropriate.
-   Keep database operations off the main thread.
-   Use WorkManager only for deferrable, persistent background work that
    genuinely needs it.
-   Do not use WorkManager as a precise timer.
-   Do not schedule reminders in V1 unless the product scope explicitly
    approves them.
-   Handle cancellation and app lifecycle correctly.
-   Avoid unmanaged global coroutine scopes.

## 13. Error handling, logging, and crash recovery

### 13.1 Error categories

1.  **Validation errors:** Invalid or missing user input.
2.  **Permission errors:** Access denied, revoked, or unavailable.
3.  **Persistence errors:** Database read/write/migration failure.
4.  **Lifecycle errors:** Process death, interrupted timer, reboot.
5.  **Build/dependency errors:** Gradle, SDK, JDK, or library
    incompatibility.
6.  **Unexpected runtime errors:** Unhandled exceptions or crashes.

### 13.2 Handling requirements

-   Validate input before persistence.
-   Show actionable, non-technical messages for recoverable user-facing
    errors.
-   Preserve unsaved user input when possible.
-   Never silently convert an error into a zero metric.
-   Never automatically wipe user data to recover from an exception.
-   Keep diagnostic logs useful but free of reflection text, task
    titles, and financial details.
-   In development builds, retain stack traces in the build/test output.
-   Fix root causes rather than adding broad catch blocks that hide
    failures.
-   Add regression tests for every fixed crash or data-loss bug.

### 13.3 Example messages

  -----------------------------------------------------------------------
  Situation                           User-facing message
  ----------------------------------- -----------------------------------
  Notification permission denied      "Notifications are off. You can
                                      still use the app; reminders will
                                      not be delivered."

  No recorded expenses                "No transactions recorded for this
                                      date."

  Database temporarily unavailable    "Your data could not be loaded.
                                      Please try again. Your saved
                                      records have not been intentionally
                                      deleted."

  Focus session needs review          "Your previous focus session was
                                      interrupted. Review its recorded
                                      time before continuing."

  Invalid amount                      "Enter an amount greater than
                                      zero."
  -----------------------------------------------------------------------

Messages must reflect the actual error state. Do not promise that data
is safe unless that has been verified.

## 14. Testing strategy

Detailed test cases belong in `13_IMPLEMENTATION_TESTING.md`. The
architecture requires these layers:

### 14.1 Unit tests

Test: - Task validation and completion. - Focus duration and pause
calculations. - Interrupted session handling. - Habit schedule and
due-date calculation. - Habit completion uniqueness. - Finance totals
and period net. - Daily Review uniqueness and date handling. - Shared
dashboard and analytics calculations.

### 14.2 Database tests

Test: - DAO insert/update/delete/query. - Foreign-key behaviour. -
Unique constraints. - Atomic multi-step focus operations. - Habit
schedule history. - Daily Review date uniqueness. - Schema migrations. -
Data preservation after app upgrade.

### 14.3 UI tests

Test: - Main navigation. - Task create/edit/complete. -
Start/pause/resume/finish focus. - Habit check-in. - Add/edit/delete
transaction. - Review save/edit. - Empty/loading/error states. - Light
and Dark theme readability.

### 14.4 Android integration tests

Where a device or emulator is available, test: - Permission
grant/deny/revoke. - Notification channel state. - App
background/foreground. - Process death and restoration. - Screen lock. -
Reboot recovery. - Different Android API levels. - APK installation and
update.

A test not executed must be reported as not run. A build passing does
not imply that physical-device integration tests passed.

## 15. Antigravity Remote development workflow

This section is mandatory because the project will be built remotely and
tested on a separate Android phone.

### 15.1 Remote environment preflight

Before implementing features, verify and record: - Remote operating
system and architecture. - Java/JDK version. - Gradle Wrapper
availability. - Android SDK command-line tools availability. - Required
SDK platform and build tools. - Network access to approved dependency
repositories. - Available disk space. - Whether emulator or
connected-device testing is available. - Whether the environment
preserves project files and build artifacts between sessions.

Do not assume that a browser preview, emulator, USB device, or local
Android Studio is available.

### 15.2 Build workflow

For each meaningful implementation milestone:

1.  Inspect the existing project state.
2.  Make a small, coherent change.
3.  Run relevant unit tests.
4.  Run Gradle build.
5.  Inspect errors and fix the cause.
6.  Repeat until the relevant checks pass.
7.  Record commands and results.
8.  Generate a debug APK at agreed milestones.
9.  Verify the APK file exists and is non-empty.
10. Preserve the exact artifact path and provide it to the user through
    the supported remote artifact delivery mechanism.

Do not report "APK ready" until the file has actually been generated and
verified.

### 15.3 APK artifact

Expected debug output for a conventional single app module:

``` text
app/build/outputs/apk/debug/app-debug.apk
```

The exact path must be verified against the actual Gradle project. Do
not invent a path or link if the build output differs.

For release:

``` text
app/build/outputs/apk/release/
```

A release APK may require signing configuration. Never commit signing
credentials or private signing keys. Keep signing configuration in
secure environment variables or a protected secret store if the remote
platform supports it.

### 15.4 Artifact delivery

-   Provide the APK as a downloadable artifact using the actual
    generated file.
-   If the remote environment cannot directly expose the artifact,
    document the supported export/transfer route before implementation
    is considered complete.
-   Verify the downloaded APK can be installed on the target phone.
-   Keep debug and release artifacts clearly named and distinguished.
-   Do not distribute an unsigned or incorrectly signed release APK as a
    final deliverable.
-   Keep a copy of the release signing key securely; future updates must
    use a compatible signing identity.

### 15.5 No mandatory local development dependency

The documented workflow must not require the user to: - Install Android
Studio on their personal computer. - Configure a local JDK or Android
SDK. - Run a local server. - Set up a cloud database. - Connect the
phone to the remote environment through USB.

If a particular test cannot be performed remotely, mark it as requiring
manual phone verification and provide exact steps.

## 16. APK installation and real-device verification

The user will install the APK manually on an Android phone.

Provide a short release test note with: - APK build type and version. -
Minimum Android version. - Package name. - Installation instructions. -
Permissions requested and why. - Features verified by automated tests. -
Features still requiring manual verification. - Known limitations.

Manual phone verification should include: 1. Install APK. 2. Launch app
and inspect first-run experience. 3. Verify no unnecessary permissions
are requested. 4. Create a task and confirm it persists after
closing/reopening. 5. Start and pause a focus session. 6. Background the
app and return. 7. Force-stop/reopen and inspect recovery behaviour. 8.
Add a habit completion and transaction. 9. Verify Home and Insights
update. 10. Save and reopen a Daily Review. 11. Change theme and verify
readability. 12. Check that denied optional permissions do not break
unrelated features.

Do not treat manual checks as passed until the user or a real-device
test reports the result.

## 17. Performance, battery, and reliability

-   Keep database queries indexed and scoped to selected date/period.
-   Avoid loading the entire transaction/task history for every screen.
-   Use lazy lists for long histories.
-   Avoid excessive recomposition and expensive work on the main thread.
-   Do not poll the database continuously for timer display.
-   Derive timer display from persisted timestamps.
-   Avoid unnecessary background services.
-   Avoid frequent wakeups and exact alarms for non-critical features.
-   Test low-storage and process-recreation behaviour where practical.
-   Keep startup work minimal.

## 18. Security and privacy

-   V1 stores user data in app-private local storage.
-   Do not add network access unless a clearly approved feature needs
    it.
-   Do not add third-party analytics or advertising SDKs.
-   Do not log personal reflection text, task titles, or finance
    amounts.
-   Keep signing secrets out of source control.
-   Avoid exporting personal data automatically.
-   If backup is enabled, document what Android backs up and how the
    user can control it.
-   Data deletion behaviour must be explicit and tested.
-   Do not claim encryption at rest unless an actual encryption design
    is implemented and verified.

## 19. Release and versioning

-   Use a stable application ID/package name.
-   Set a human-readable version name and increasing version code.
-   Keep debug and release build types distinct.
-   Release builds must use the intended signing identity.
-   Run the release build and inspect the produced APK.
-   Record build date, commit/revision identifier if available, version,
    and test status.
-   Keep a known-good build artifact until the next build is verified.
-   A failed new build must not replace the last verified deliverable.

## 20. Acceptance criteria

### Architecture and stack

-   [ ] Project uses the approved native Android stack.
-   [ ] Gradle Wrapper and compatible JDK/SDK versions are documented.
-   [ ] Dependencies are pinned and verified as compatible.
-   [ ] UI, domain, and data responsibilities are separated.
-   [ ] No mandatory backend or login exists in V1.

### Data and lifecycle

-   [ ] Room schema follows `11_DATA_ARCHITECTURE.md`.
-   [ ] Shared calculations are reused across screens.
-   [ ] Focus duration is based on persisted valid intervals.
-   [ ] Process death does not invent focus time.
-   [ ] Database migrations preserve existing data.

### Permissions and errors

-   [ ] Manifest includes only required permissions.
-   [ ] Optional permissions are requested in context.
-   [ ] Denial and revocation do not crash unrelated app features.
-   [ ] Notification state is checked at the appropriate Android levels.
-   [ ] Runtime and persistence errors produce safe, understandable
    states.
-   [ ] No automatic database wipe is used as generic recovery.

### Remote build and APK delivery

-   [ ] Project builds in Antigravity Remote using the documented
    wrapper.
-   [ ] Relevant automated tests pass.
-   [ ] Debug APK is generated and exact output path verified.
-   [ ] APK artifact is made available through the supported delivery
    mechanism.
-   [ ] Release signing configuration is secure.
-   [ ] Phone installation and manual checks are documented.
-   [ ] Unrun tests are explicitly reported as not run.

## 21. Open implementation decisions

Resolve during project initialization, based on actual Antigravity
Remote capabilities:

1.  Exact JDK, Gradle, Android Gradle Plugin, Kotlin, Compose, and SDK
    versions.
2.  Actual minimum supported Android API level.
3.  Whether Hilt is worth including or manual dependency wiring is
    simpler.
4.  Which chart library has the best current compatibility and
    maintenance.
5.  Whether emulator/instrumentation tests are available in the remote
    environment.
6.  Supported artifact export/download mechanism from Antigravity
    Remote.
7.  Release signing and secure key retention mechanism.
8.  Whether Android backup is enabled and its exact data scope.
9.  Whether reminders are included in V1; default recommendation remains
    deferred.
10. Whether a foreground service is truly required for any approved
    focus-timer behaviour; default recommendation is to avoid it unless
    the requirements justify it.

Do not guess environment capabilities. Verify them in the remote project
before locking environment-specific values.

## 22. Related documents

-   `01_PRODUCT_OVERVIEW.md` --- product scope and principles.
-   `02_PRODUCT_REQUIREMENTS.md` --- functional and non-functional
    requirements.
-   `03_UI_UX_GUIDELINES.md` --- visual and interaction rules.
-   `04_HOME_DASHBOARD.md` --- dashboard screen.
-   `05_DAILY_PLANNING.md` --- tasks and planning.
-   `06_ACTIVE_WORK_FOCUS.md` --- focus timer behaviour.
-   `07_DAILY_HABITS.md` --- habit schedules and completion.
-   `08_FINANCE.md` --- transaction handling.
-   `09_LIFE_ANALYTICS.md` --- insights and calculations.
-   `10_DAILY_REVIEW.md` --- daily reflection.
-   `11_DATA_ARCHITECTURE.md` --- logical data model and source of
    truth.
-   `13_IMPLEMENTATION_TESTING.md` --- build, test, permission, device,
    and release verification plan.


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

### Analytics implementation constraints
Implement calculations in a testable domain/service layer, not inside UI composables. Derive aggregates from authoritative task, habit schedule/completion, focus-session, and finance transaction records. Use local calendar dates and the configured timezone consistently. Cache only rebuildable aggregates. Missing data must remain distinct from zero.
