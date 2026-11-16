# AGENTS.md --- Mandatory Instructions for Life Analytics

This file contains project-wide instructions for every AI coding agent
and contributor working on Life Analytics.

**Read this file before inspecting or modifying the project.** Follow it
together with `README.md` and the numbered specifications in `docs/`.

If an instruction in this file conflicts with a lower-level
implementation preference, follow this file unless the user explicitly
approves a change. Product scope and user-approved decisions must not be
changed silently.

------------------------------------------------------------------------

## 1. Project mission

Build **Life Analytics**, a native Android personal app that helps one
user record and understand daily tasks, focus sessions, habits, and
finances.

The core product principle is:

> Minimum input, maximum meaningful output.

The user enters information once. Home, Daily Review, and Insights
derive their values from the same source records.

The expected development workflow is:

1.  Develop in Antigravity Remote.
2.  Build and test in that remote environment.
3.  Generate a valid APK.
4.  Make the actual APK artifact available for download through the
    supported remote mechanism.
5.  The user installs the APK on an Android phone and manually verifies
    it.
6.  Fix issues found during real-device testing and provide a verified
    build.

**The remote build workflow and successful APK delivery are top
priorities.** Do not choose tools, dependencies, or architecture that
unnecessarily complicate this workflow.

## 2. Mandatory first actions

Before writing code:

1.  Read `README.md` completely.
2.  Read all relevant numbered documents in `docs/`. For a new project,
    read all 13 in numerical order.
3.  Inspect the current repository and preserve existing user work.
4.  Determine whether an Android project already exists. Do not
    overwrite or recreate it blindly.
5.  Inspect the actual Antigravity Remote environment:
    -   Operating system and architecture.
    -   JDK version.
    -   Gradle Wrapper availability.
    -   Android SDK command-line tools.
    -   Installed Android SDK platforms and build tools.
    -   Dependency repository/network access.
    -   Available disk space.
    -   Whether emulator or connected-device testing is available.
    -   How generated files can be exported/downloaded to the user.
6.  Record actual versions and limitations. Do not guess.
7.  Confirm that a minimal Android project can build a debug APK before
    committing to a large implementation.

If the environment is missing a requirement, investigate whether it can
be installed or configured safely. Ask the user only if a permission,
credential, approval, or unavailable capability genuinely blocks
progress.

Do not claim that the app, project, or APK already exists merely because
documentation exists.

## 3. Product scope is fixed for V1

### Included modules

-   Home Dashboard.
-   Daily Planning / Tasks.
-   Active Work & Focus Timer.
-   Daily Habits.
-   Finance.
-   Life Analytics / Insights.
-   Daily Review.
-   Settings and only the system integration required by approved
    features.

### Excluded unless the user explicitly approves a scope change

-   Learning Process.
-   Career Goal Tracker.
-   AI assistant or AI recommendations.
-   Voice entry or voice journaling.
-   Advanced distraction detection.
-   Multi-user, social, or team features.
-   Desktop or web app.
-   Mandatory login or account system.
-   Remote backend, Firebase, Node.js server, or MongoDB.
-   Bank integrations, investment, tax, UPI, and currency conversion.
-   Reminders and exact alarms if not explicitly approved.
-   Camera, microphone, location, contacts, or photo access.

Do not add a feature just because it seems useful or a library supports
it. If a change appears necessary, explain its impact and ask for
approval before expanding scope.

## 4. Technology and architecture direction

Use the documented native Android direction:

-   Kotlin.
-   Jetpack Compose.
-   Gradle Wrapper.
-   Room over SQLite.
-   MVVM with pragmatic Clean Architecture principles.
-   Kotlin Coroutines and Flow.
-   Navigation Compose.
-   DataStore for app preferences.
-   A Compose-compatible chart library only after checking compatibility
    and maintenance.
-   Hilt only if it remains straightforward; manual dependency wiring is
    acceptable.

Exact JDK, Gradle, Android Gradle Plugin, Kotlin, Compose, SDK, and
dependency versions must be selected as a compatible set based on the
actual remote environment and pinned. Never mix arbitrary latest
versions.

Use a simple, understandable project structure. Do not create excessive
modules, abstractions, services, or dependencies for a single-user V1
app.

No mandatory backend or internet connection should be needed for core
functionality. Do not add network permission unless an approved feature
genuinely requires it.

## 5. Source-of-truth and data rules

`docs/11_DATA_ARCHITECTURE.md` is authoritative for logical data
ownership and relationships.

-   Each business fact has one source of truth.
-   Tasks, focus sessions, habits, habit completions, finance
    transactions, and Daily Reviews remain separate source records.
-   Home, Review, and Insights derive summaries from those source
    records.
-   Do not duplicate transaction, task, session, or habit data into
    dashboard or analytics tables.
-   Use stable persistent IDs and appropriate foreign keys.
-   Store calendar dates separately from event timestamps.
-   Use one shared timezone/date attribution policy.
-   Store money using integer minor units or an exact decimal-safe
    representation; never use floating point for currency.
-   Preserve historical habit schedules and completion records.
-   Do not infer task completion from a focus session.
-   Unknown interruption gaps must not be counted as focused time.
-   Net income is not an account balance.
-   Distinguish zero from missing, unavailable, and insufficient data.
-   Do not insert fake user data into production.

Use database constraints and transactions for important invariants. Do
not silently delete historical records when a parent record is edited or
removed.

## 6. UX and visual rules

Follow `docs/03_UI_UX_GUIDELINES.md` and the individual screen
specifications.

-   Keep forms short and input minimal.
-   Avoid duplicate entry.
-   Prefer clear, predictable interactions.
-   Use the approved purple-blue visual direction, rounded cards,
    readable typography, and Light/Dark themes.
-   Use neutral, non-judgemental language.
-   Avoid guilt-based streaks, arbitrary productivity scores, or
    unsupported behavioural claims.
-   Provide loading, empty, partial, and error states.
-   Do not display fake sample metrics.
-   Make touch targets, contrast, keyboard behaviour, and screen-reader
    labels usable.
-   Do not invent extra screens or interactions that conflict with the
    specifications.

## 7. Permission and Android system rules

Permissions are a first-class requirement. Follow
`docs/12_TECHNICAL_ARCHITECTURE.md` and the relevant feature
specifications.

For each permission: 1. Confirm that an implemented, approved feature
genuinely needs it. 2. Document the Android API levels and permission
behaviour. 3. Request it only in context, when the user invokes/enables
that feature. 4. Explain why it is needed. 5. Handle grant, denial,
repeated denial, revocation, and unavailable states. 6. Provide a
Settings route when useful and supported. 7. Keep unrelated app
functionality available when an optional permission is denied.

Specific rules: - Do not request storage permission for app-private Room
storage. - Do not request camera, microphone, location, contacts, or
photo permissions for current V1 scope. - Notification permission is
relevant only if notifications/reminders are approved and implemented. -
Do not request exact-alarm access unless exact timing is essential and
approved. - Do not add a foreground service unless a verified
requirement justifies it. - Do not assume battery optimization
exemptions can guarantee background execution. - Inspect the final
merged Android manifest, including permissions introduced transitively
by dependencies. - Test permission grant, denial, revocation through
Settings, notification channel disablement, and API-level differences.

A denied optional permission must not crash the app or block unrelated
features.

## 8. Focus timer reliability

The timer is a high-risk feature and must follow
`docs/06_ACTIVE_WORK_FOCUS.md`.

-   Persist session start, end, and pause intervals.
-   Do not rely on an in-memory counter as the source of truth.
-   Derive focused duration from valid recorded intervals.
-   Exclude paused time and unknown interruption gaps.
-   Enforce at most one active or paused session.
-   Make start/pause/resume/finish operations atomic.
-   Restore known state after process recreation.
-   Treat uncertain sessions as interrupted and ask the user to review
    them.
-   Do not invent elapsed focus time after force-stop, reboot, or
    unknown clock gaps.
-   Test backgrounding, screen lock, process death, force-stop, reboot,
    and wall-clock/timezone changes.
-   Do not claim the timer proves distraction-free attention or
    productivity.

## 9. Autonomous debugging and retry protocol

**Do not stop at the first recoverable error.** Investigate and attempt
reasonable repairs autonomously before asking the user.

For each failure:

1.  Capture the complete relevant command output, error, and stack
    trace.
2.  Identify the earliest meaningful error; distinguish root cause from
    cascading errors.
3.  Inspect relevant source files, Gradle configuration, dependency
    versions, manifest, environment, or test fixture.
4.  Form a specific root-cause hypothesis.
5.  Apply one minimal, evidence-based change.
6.  Run the smallest relevant verification.
7.  If it passes, run the affected module tests and broader build
    checks.
8.  If it fails, reassess the evidence and try a meaningfully different
    repair.
9.  Record each attempt and its result.

### Retry budget

For a recoverable problem, make up to **three distinct, reasoned repair
attempts** before escalating to the user.

A distinct attempt must use a different diagnosis or repair strategy.
Re-running the same failed command without a meaningful change is not
another attempt.

Do not waste retries when a clear external blocker already requires user
action.

### Ask the user only when necessary

Escalate when: - Remote environment permissions or capabilities prevent
a required setup. - An account connection, credential, approval, or
external action is required. - The product decision is ambiguous and
cannot safely be inferred. - A destructive or irreversible action would
be needed. - Evidence is insufficient to choose safely between
materially different options. - Three distinct reasonable repair
attempts have failed. - An external service or infrastructure is
unavailable beyond the agent's control.

When asking, provide: - What failed. - Relevant evidence. - What was
inspected. - Each repair attempt and outcome. - Why the agent is
blocked. - The exact user action or decision required. - What
independent work can continue.

Ask one clear, specific question or request at a time. Do not ask the
user to diagnose a problem the agent can reasonably investigate.

### Safety limits during repair

Never: - Hide an error or claim success without verification. - Disable
tests, linting, validation, or security checks just to get a green
result. - Delete the project, database, or user data as a generic fix. -
Reset or overwrite user work without explicit approval. - Commit fake
credentials, API keys, passwords, signing keys, or secrets. - Weaken
permission/security settings to bypass a failure. - Make unrelated
speculative changes. - Suppress exceptions with broad catch blocks that
conceal failures. - Continue blind retries after evidence no longer
supports the approach.

For any destructive or high-risk action, stop and request approval.

## 10. Testing requirements

Testing is continuous, not a final afterthought. Follow
`docs/13_IMPLEMENTATION_TESTING.md`.

Use the appropriate test layer:

-   Unit tests for domain rules and calculations.
-   Room/DAO tests for persistence, constraints, transactions, and
    migrations.
-   Compose UI tests for screen behaviour and states.
-   Android integration tests for permissions and lifecycle where a
    device/emulator is available.
-   Manual phone tests for real installation and device behaviour.

At minimum test: - Task creation, editing, completion, and
persistence. - Focus duration, pause/resume, interruptions, and
recovery. - Habit schedules, dated completion, and history. - Finance
arithmetic, editing, deletion, and period summaries. - Dashboard
calculations against source records. - Insights calculations and
empty/insufficient states. - Daily Review save/edit and source-summary
refresh. - Permission grant/deny/revoke and notification channel state
if applicable. - App backgrounding, process recreation, force-stop,
reboot, and app upgrade. - Light/Dark themes, navigation, accessibility,
and keyboard behaviour.

A test that could not run must be reported as **not run**, not passed. A
successful Gradle build does not prove real-device behaviour.

Every fixed bug should receive a regression test where practical.

## 11. Milestone-based implementation

Follow the order in `docs/13_IMPLEMENTATION_TESTING.md`:

0.  Remote environment preflight.
1.  Project foundation and minimal APK.
2.  Data foundation.
3.  Daily Planning.
4.  Focus Timer.
5.  Daily Habits.
6.  Finance.
7.  Home Dashboard.
8.  Insights.
9.  Daily Review.
10. Settings, permissions, and polish.
11. Release readiness.

Keep the project buildable at meaningful milestones. Do not implement
every module in one large unverified change.

After each milestone, report what was implemented, build/test status,
issues, and next step.

## 12. Remote build and APK delivery

The primary build environment is Antigravity Remote.

-   Use the committed Gradle Wrapper, not an assumed globally installed
    Gradle.
-   Verify JDK, SDK, Gradle, dependency access, disk space, and artifact
    export.
-   Run relevant tests and build tasks.
-   Do not assume an emulator, USB device, local Android Studio, or
    browser preview is available.
-   Generate a debug APK at appropriate milestones.
-   Verify the actual output path, file existence, non-zero size,
    package ID, version, min SDK, and signing state.
-   Verify that the remote platform can expose the APK as a downloadable
    artifact.
-   Provide only a real, verified artifact link/path.
-   Do not invent a sandbox path or claim an artifact is downloadable
    without checking.
-   If artifact export is blocked, investigate supported alternatives
    and explain the exact blocker.

Typical commands, once the project exists:

``` bash
./gradlew --version
./gradlew clean
./gradlew test
./gradlew assembleDebug
```

If an emulator/device is available:

``` bash
./gradlew connectedDebugAndroidTest
```

The conventional debug APK path is:

``` text
app/build/outputs/apk/debug/app-debug.apk
```

Verify the actual project output; never assume this path.

### Signing and secrets

-   Debug APKs are for development testing.
-   Release APKs must be signed with the intended release identity.
-   Keep signing keys and passwords outside source control.
-   Never ask the user to paste secrets into project files or chat when
    a secure secret mechanism is available.
-   Preserve the release key safely because future updates need
    compatible signing.
-   Do not label a debug APK as a production release.

## 13. Real-phone verification

The user installs the APK on an Android phone. Provide exact
installation and test instructions.

Verify or ask the user to verify: - APK installs and launches. - No
immediate crash. - Core data persists after app close/reopen. - Focus
timer pause/resume and recovery. - Optional permission denial does not
break unrelated screens. - Notification settings are handled accurately
if notifications exist. - Habits and finance records persist. - Home and
Insights reflect source records. - Daily Review saves and reopens. -
Light and Dark themes are readable.

Do not mark manual phone checks as passed unless they were actually
performed or the user confirms the result.

## 14. Privacy and security

-   Keep V1 personal data local.
-   Do not add third-party analytics, ads, or AI services.
-   Do not send reflection text, finance details, or task titles to
    external services.
-   Do not log sensitive user content.
-   Do not claim encryption unless implemented and verified.
-   Make backup/export behaviour explicit.
-   Keep Android app-private storage and platform sandbox protections.
-   Avoid network access unless an approved feature requires it.

## 15. Code quality and maintainability

-   Prefer readable, idiomatic Kotlin.
-   Use clear names and small, focused functions.
-   Keep business logic out of composables.
-   Avoid duplicate calculation implementations.
-   Avoid unnecessary abstractions and premature optimization.
-   Use stable dependency versions.
-   Remove unused code and dependencies.
-   Add comments for non-obvious decisions, not for every line.
-   Keep formatting consistent.
-   Do not leave placeholder implementations for required features while
    reporting them complete.
-   Keep test fixtures separate from production data.
-   Update documentation when an approved decision changes.

## 16. Git and project safety

If version control is available: - Inspect `git status` before
changes. - Preserve user changes and untracked files. - Make focused
commits when the workflow supports it. - Never force-push, rewrite
history, or discard user work without explicit approval. - Do not commit
build outputs, local SDK files, secrets, signing keys, or personal
database files. - Keep Gradle Wrapper and source files under version
control. - Use `.gitignore` appropriate for Android/Gradle.

Do not assume a Git remote or credentials exist. Verify before
attempting remote operations.

## 17. Progress reporting

Keep reports concise and evidence-based.

At each meaningful milestone, report:

-   **Implemented:** What changed.
-   **Build:** Exact command and actual result.
-   **Tests:** Which tests ran and results.
-   **Not run:** Tests unavailable in the remote environment.
-   **Errors:** What failed and current status.
-   **Repairs:** Attempts made and verification outcome.
-   **APK:** Exact verified artifact path/link, if generated.
-   **Next:** Next milestone or the exact user input needed.

Do not send repetitive empty progress messages. Do not say "done",
"fixed", "ready", "passed", or "downloadable" until verified.

## 18. Definition of done

The project is not complete until: - All approved V1 modules are
implemented. - Product and data rules match the specifications. -
Relevant automated tests pass. - Critical and high-severity defects are
resolved. - Permissions and Android edge cases are handled. - The remote
build succeeds reproducibly. - The APK is generated and inspected. - The
APK is genuinely available for download. - Phone installation and manual
test instructions are provided. - Unrun tests and known limitations are
disclosed. - Release signing is handled securely if a release APK is
delivered. - README and all numbered documentation reflect the actual
project state.

**Final instruction:** Prioritize the user's working workflow, data
integrity, and reliable Android APK delivery over adding features,
dependencies, or visual complexity. Investigate and repair recoverable
problems autonomously; ask the user only when their input or permission
is genuinely required.
