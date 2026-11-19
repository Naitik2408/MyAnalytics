# Life Analytics --- Product Overview

**Document:** `01_PRODUCT_OVERVIEW.md`\
**Product:** Life Analytics\
**Version:** 1.0 (V1 scope)\
**Status:** Initial draft for review\
**Platform:** Android mobile application\
**Primary user:** Single personal user

------------------------------------------------------------------------

## 1. Product summary

Life Analytics is a personal Android application that helps a user
understand how they spend their time, manage their money, maintain daily
habits, and review their day through structured records and simple
analytics.

The app brings these areas together in one place without forcing them
into one data model. Each module records its own information, while the
Home Dashboard and Insights area present useful summaries across the
modules.

The product is intended for personal use initially. V1 is designed
around one user and an Android phone rather than a multi-user or
desktop-first experience.

## 2. Product vision

Help the user make better-informed decisions about everyday life by
turning small, consistent records into clear observations.

The app should make it easier to answer questions such as:

-   What did I plan to do, and what did I actually complete?
-   How much time did I spend in recorded focus sessions?
-   Where did my money go?
-   Which habits am I consistently completing or missing?
-   How did my activity change across days and weeks?

The app should present information clearly and avoid claiming that
recorded data proves more than it does.

## 3. Core product principles

1.  **Minimum input, meaningful output:** Keep daily data entry short
    and make calculations and summaries automatic wherever practical.
2.  **Clear user experience:** Every action should have an
    understandable purpose, with simple labels and predictable
    navigation.
3.  **Reliable records:** Store source records once and derive dashboard
    metrics from those records. Avoid duplicate manual entry.
4.  **Separate modules, connected insights:** Finance, planning, focus
    sessions, and habits keep their own records but can contribute to
    shared analytics.
5.  **Honest analytics:** Distinguish measured or user-entered facts
    from estimates and interpretations.
6.  **Personal control:** The user should be able to review, correct,
    and manage their own entries.
7.  **Focused V1 scope:** Build the agreed core experience before adding
    AI or other advanced features.

## 4. V1 goals

V1 will provide the following capabilities:

-   A Home Dashboard with a concise view of today's activity.
-   Daily Planning to create, edit, and complete planned tasks.
-   Active Work & Focus Timer to record time spent on planned, newly
    created, or general work sessions.
-   Daily Habits to define recurring habits and record daily completion.
-   Finance to record income and expenses, organise transactions by
    category, and calculate balances.
-   Life Analytics / Insights to display daily, weekly, and monthly
    summaries based on available records.
-   Daily Review to reflect on the day and compare planned activity with
    recorded outcomes.
-   Settings and permission controls needed to support the Android app.

## 5. V1 modules at a glance

### 5.1 Home Dashboard

The main landing screen. It provides four key daily metrics:

-   **Tasks Completed:** completed planned tasks compared with the
    number of planned tasks.
-   **Focused Time:** total valid recorded focus-session duration for
    the day.
-   **Today's Expenses:** expenses recorded for the current day.
-   **Habits Completed:** completed scheduled habits compared with the
    number of habits due.

The dashboard also provides quick actions for **Start Focus**, **Add
Task**, and **Add Expense**, plus previews of today's tasks and habits.

Dashboard values must be derived from the relevant module records, not
maintained as separate duplicate records.

### 5.2 Daily Planning

Allows the user to create and manage tasks for a day. A task may include
a title, category, optional planned time or estimated duration, and
completion status.

Tasks created in Daily Planning can be selected when starting a focus
session. The user can also start work on an unplanned task or create a
new task from the focus-start flow.

### 5.3 Active Work & Focus Timer

Records work sessions using a timer. A session can be associated with an
existing planned task, a newly created task, or a general activity that
is not linked to a specific task.

The timer supports starting, pausing, resuming, and finishing a session.
Paused time is excluded from focused duration. The app should prevent
multiple simultaneous active sessions.

**Important distinction:** Focused Time is a calculated dashboard
metric, not a separate manual-entry feature. Starting and recording a
valid focus session updates the total automatically. A timer measures
recorded session duration; it cannot by itself prove that the user was
free from distractions.

### 5.4 Daily Habits

Allows the user to create and manage recurring habits and record whether
each habit due on a given day was completed.

Habit completion is tracked separately from task completion. Tasks
represent planned pieces of work; habits represent recurring behaviours.

### 5.5 Finance

Allows the user to record income and expenses, assign categories, and
review totals and available balance.

Finance records remain separate from productivity and habit records.
Finance analytics are derived from the transaction history and any
explicitly configured starting balance or other approved balance rules.

### 5.6 Life Analytics / Insights

Presents summaries and trends based on data from the V1 modules.
Examples include task completion, recorded focus duration, expense
patterns, and habit consistency.

Insights must respect the available data range. The app must not
fabricate values or present conclusions when there is insufficient data.

### 5.7 Daily Review

Provides a short end-of-day review. It can help the user reflect on what
was planned, what was completed, and any relevant observations or
distractions.

Daily Review supplements the underlying records; it must not replace or
duplicate task, focus, finance, or habit records.

### 5.8 Settings & Permissions

Provides app preferences and access to relevant Android permission
explanations and controls. Any feature that depends on an Android
permission must explain why that permission is needed and continue to
work gracefully when permission is not granted.

## 6. Core user experience

A typical day may work like this:

1.  The user opens Home and reviews today's summary.
2.  The user creates or reviews a small set of planned tasks in Daily
    Planning.
3.  The user starts work from a planned task, creates a new task, or
    starts a general focus session.
4.  The app records the session duration. Pauses are excluded from
    focused time.
5.  The user records expenses or income when transactions occur.
6.  The user marks recurring habits as completed when appropriate.
7.  The user completes a short Daily Review.
8.  Home and Insights reflect the updated source records.

This is a flexible workflow, not a requirement that every user action
happen in a fixed order.

## 7. High-level data relationship

Each module owns its source records:

-   Daily Planning owns task records.
-   Focus tracking owns focus-session records and may reference a task.
-   Daily Habits owns habit definitions and dated completion records.
-   Finance owns income and expense transactions and categories.
-   Daily Review owns review entries.
-   Insights reads and aggregates records from these modules.

The Home Dashboard reads from these modules and calculates its summary
metrics. It should not create duplicate copies of their records.

A focus session may have an optional task relationship. General focus
sessions are valid records even when no task is linked.

Detailed entities, identifiers, relationships, calculations, and
database constraints will be defined in `11_DATA_ARCHITECTURE.md`.

## 8. Platform and design direction

-   **Platform:** Android mobile application.
-   **Initial usage:** Personal, single-user use.
-   **Themes:** Light and Dark.
-   **Visual direction:** Modern, clean purple-blue visual language,
    based on the approved Home Dashboard concept.
-   **Interaction:** Touch-friendly controls, clear hierarchy, readable
    typography, and minimal steps for frequent actions.

Detailed screen layouts, navigation, components, states, and design
tokens will be specified in the UI/UX and individual screen documents.

## 9. Explicitly out of scope for V1

The following are not part of the agreed V1 scope:

-   Learning progress or Learning Process module.
-   Career Goal Tracker or career recommendations.
-   AI-based personal recommendations or an AI assistant.
-   Voice-based entry.
-   Advanced automated distraction detection.
-   A desktop or web application interface.
-   Multi-user, social, or team functionality.

These may be considered for a future version only after separate
requirements are agreed.

## 10. Privacy, permissions, and data integrity

The app handles personal finance and daily activity records.
Implementation must use appropriate platform security and minimise
access to device data.

-   Request only permissions that are necessary for an approved feature.
-   Explain permission purpose before requesting it.
-   Do not imply that optional permissions are mandatory for core manual
    tracking.
-   Validate user input and handle edits and deletions consistently.
-   Preserve timestamps and dates using a clearly defined local-time
    strategy.
-   Do not silently discard incomplete focus sessions or user-entered
    records.
-   Define backup, export, and recovery behaviour in the technical
    requirements before implementation.

Specific security, storage, backup, and permission decisions will be
detailed in the technical architecture.

## 11. Success criteria for V1

V1 will be considered successful when:

-   The user can record and review tasks, focus sessions, habits,
    income, and expenses.
-   Home metrics update from their source records without duplicate
    manual entry.
-   Focus sessions can be linked to planned tasks or recorded as general
    sessions.
-   Paused time is excluded from focused duration.
-   Task completion and habit completion remain distinct.
-   Daily, weekly, and monthly summaries use accurate available data.
-   Empty, loading, error, and permission-denied states are handled
    clearly.
-   The app is usable in both Light and Dark themes on an Android phone.

## 12. Related project documents

The project documentation set is planned as follows:

### Product and design

1.  `01_PRODUCT_OVERVIEW.md` --- product vision, scope, principles, and
    high-level modules.
2.  `02_PRODUCT_REQUIREMENTS.md` --- detailed functional and
    non-functional requirements and V1 boundaries.
3.  `03_UI_UX_GUIDELINES.md` --- visual system, navigation, components,
    and interaction guidelines.
4.  `04_HOME_DASHBOARD.md` --- Home Dashboard screen specification.
5.  `05_DAILY_PLANNING.md` --- Daily Planning screen and task workflows.
6.  `06_ACTIVE_WORK_FOCUS.md` --- focus session setup, timer, controls,
    and history.
7.  `07_DAILY_HABITS.md` --- habit setup and daily completion.
8.  `08_FINANCE.md` --- transactions, categories, balances, and finance
    views.
9.  `09_LIFE_ANALYTICS.md` --- metrics, charts, filters, and insight
    rules.
10. `10_DAILY_REVIEW.md` --- end-of-day review workflow.

### Architecture and delivery

11. `11_DATA_ARCHITECTURE.md` --- data model, relationships,
    constraints, and calculations.
12. `12_TECHNICAL_ARCHITECTURE.md` --- Android technology choices,
    application structure, storage, integrations, and security.
13. `13_IMPLEMENTATION_TESTING.md` --- implementation phases, test
    strategy, and acceptance criteria.

### Project root files

-   `README.md` --- project introduction and documentation index.
-   `AGENTS.md` --- instructions and constraints for AI coding agents
    working in the repository.

## 13. Document status and change control

This document captures the agreed high-level V1 direction. It is an
initial product overview, not a substitute for the detailed requirements
and screen specifications.

If a requirement changes: 1. Update this overview if the product vision
or V1 scope changes. 2. Update the relevant module or architecture
document. 3. Check related documents for conflicts. 4. Record whether
the change is approved for V1 or deferred.

Do not implement features that are not documented and approved for the
current version.


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

### V1 product decision addendum
The product has two distinct layers: operational tracking (Habits, Tasks, Focus, Finance) and life-area analytics (Health & Lifestyle, Career & Productivity, Finance & Wealth). The analytics layer classifies records; it does not replace the modules that create them.
