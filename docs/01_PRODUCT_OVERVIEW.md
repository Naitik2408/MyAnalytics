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
