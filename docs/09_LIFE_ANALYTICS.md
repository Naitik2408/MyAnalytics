# Life Analytics --- Life Analytics & Insights

**Document:** `09_LIFE_ANALYTICS.md`\
**Product:** Life Analytics\
**Version:** 1.0 (V1)\
**Status:** Module specification\
**Platform:** Android\
**Related documents:** `01_PRODUCT_OVERVIEW.md`,
`02_PRODUCT_REQUIREMENTS.md`, `03_UI_UX_GUIDELINES.md`,
`04_HOME_DASHBOARD.md`, `05_DAILY_PLANNING.md`,
`06_ACTIVE_WORK_FOCUS.md`, `07_DAILY_HABITS.md`, `08_FINANCE.md`,
`11_DATA_ARCHITECTURE.md`

------------------------------------------------------------------------

## 1. Purpose

Life Analytics turns the records created across the app into
understandable summaries, trends, and comparisons. It helps the user
observe how they plan their days, record focused time, maintain habits,
and manage income and expenses.

The module must help the user identify patterns using actual data,
without inventing explanations or judging the user.

**Core principle:** Show what the data supports, make calculations
transparent, and distinguish observation from interpretation.

## 2. UX goals

-   Make important patterns understandable at a glance.
-   Allow the user to move between daily, weekly, and monthly views.
-   Use charts only when they make a comparison or trend easier to
    understand.
-   Explain every metric in plain language.
-   Keep date ranges, units, and denominators visible.
-   Avoid overwhelming the user with too many charts at once.
-   Show insufficient data honestly.
-   Make every insight traceable to records in its source module.
-   Never label recorded focus time as verified productivity or
    distraction-free time.
-   Avoid guilt-based scores, rankings, or performance judgements.

## 3. V1 scope

### Included

-   Daily, weekly, and monthly analytics views.
-   Task completion summaries.
-   Focus duration summaries.
-   Habit consistency summaries.
-   Income, expense, and net-for-period summaries.
-   Simple trends and comparisons between equivalent periods where
    sufficient data exists.
-   Charts and readable summary values.
-   Empty and insufficient-data states.
-   Navigation from analytics to relevant source modules.

### Excluded from V1

-   AI-generated recommendations or explanations.
-   Predictive analytics or forecasts.
-   Automatic causal analysis.
-   Cross-user benchmarks or social rankings.
-   Health or psychological conclusions.
-   Advanced custom report builders.
-   Data science features requiring external services.
-   Claims that a metric proves productivity, attention, or personal
    success.

## 4. Analytics information architecture

The Insights area should provide:

1.  A clear period selector: Daily, Weekly, Monthly.
2.  A visible date or date-range label.
3.  A compact overview of the most useful metrics for that period.
4.  Module-specific sections for Tasks, Focus, Habits, and Finance.
5.  Simple charts that answer a specific question.
6.  A route to view the underlying records.

Recommended screen order:

-   Header and period selector
-   Overall overview
-   Task analytics
-   Focus analytics
-   Habit analytics
-   Finance analytics

On a small phone screen, sections should be vertically stacked and easy
to scan. Avoid putting all charts above the fold.

## 5. Insights screen layout

### 5.1 Suggested wireframe

``` text
┌──────────────────────────────────────┐
│ Insights                             │
│                                      │
│ [Daily] [Weekly] [Monthly]            │
│                                      │
│ 10–16 November 2026           [‹ ›]  │
│                                      │
│ OVERVIEW                             │
│ ┌────────────────┐ ┌────────────────┐│
│ │ Tasks done     │ │ Focused time   ││
│ │ 18 / 24        │ │ 12h 40m        ││
│ └────────────────┘ └────────────────┘│
│ ┌────────────────┐ ┌────────────────┐│
│ │ Habit consistency││ Net for period││
│ │ 78%            │ │ ₹ 4,200        ││
│ └────────────────┘ └────────────────┘│
│                                      │
│ TASKS                                │
│ Task completion trend               │
│ [Bar chart]                          │
│                                      │
│ FOCUS                                │
│ Focus time by day                   │
│ [Line chart]                         │
│                                      │
│ HABITS                               │
│ Habit consistency                  │
│ [Progress/list]                      │
│                                      │
│ FINANCE                              │
│ Income vs expenses                  │
│ [Bar chart]                          │
├──────────────────────────────────────┤
│ Home    Finance   Activity  Insights │
└──────────────────────────────────────┘
```

All numbers and charts in this wireframe are illustrative placeholders
only. The application must render values calculated from real saved
records.

## 6. Period selection and date ranges

### 6.1 Period types

-   **Daily:** one selected local calendar date.
-   **Weekly:** Monday through Sunday, using the app's agreed local
    calendar rules.
-   **Monthly:** first through last day of the selected calendar month.

### 6.2 Interaction

-   Default to the current week or the last-used period, as finalized in
    implementation. Recommended initial default: current week.
-   Allow switching between Daily, Weekly, and Monthly.
-   Provide previous/next period navigation.
-   Display the exact date or date range.
-   Allow direct date/month selection where useful.
-   All sections on the screen must use the same selected period.
-   Changing the period must refresh all metric cards and charts
    consistently.

### 6.3 Date boundaries

-   Use the same timezone and local-day rules as the source modules.
-   Weekly periods must use a consistent week start (Monday in V1).
-   Monthly periods must use calendar months.
-   Do not mix UTC date boundaries with local date labels.
-   Sessions crossing midnight must follow the focus-session attribution
    rule in `11_DATA_ARCHITECTURE.md`.
-   Future dates must not be counted as missed activity or incomplete
    scheduled occurrences.

## 7. Overview metrics

Show a compact overview, not a wall of numbers.

### 7.1 Tasks completed

Display completed tasks and total planned tasks for the period.

``` text
task_completion =
    completed tasks / total planned tasks
```

Rules: - Count tasks according to their planned date within the selected
period. - A task is counted once in its applicable date. - Do not count
habits as tasks. - If no tasks were planned, show "No tasks planned"
rather than 0%. - Show a ratio such as `18 / 24`; a percentage may be
secondary. - Tapping the metric opens Daily Planning filtered to the
selected period/date as supported.

### 7.2 Focused time

Display total valid focused duration in the selected period.

Rules: - Sum valid focus-session durations attributed to dates in the
period. - Exclude paused intervals. - Exclude unknown interruption
gaps. - Include task-linked and general focus sessions. - Do not label
the metric as productivity, attention, or distraction-free time. - If no
sessions exist, show `0m` or "No focus sessions". - Tapping opens focus
history for the selected period.

### 7.3 Habit consistency

Display completed due occurrences divided by total due occurrences.

``` text
habit_consistency =
    completed due occurrences / total due occurrences
```

Rules: - Count only dates when each habit was active and scheduled. -
Exclude not-scheduled days. - Exclude dates before a habit's effective
start date and dates after deactivation. - Preserve schedule history
when a habit's schedule changes. - If no due occurrences exist, show "No
scheduled habits" or "Not enough data", not 0%. - Show numerator and
denominator alongside percentage where practical.

### 7.4 Finance net for period

Display:

``` text
net_for_period = income_total - expense_total
```

Rules: - Use transactions dated within the selected period. - Keep
income and expenses available as separate values. - Label the result
"Net for period". - Do not label it Account Balance or Available
Balance. - Do not combine currencies without an approved conversion
model. - If there are no transactions, show a neutral empty state.

## 8. Task analytics

### 8.1 Key questions

-   How many tasks were planned?
-   How many were completed?
-   How did completion vary by day?
-   How much estimated work was planned?

### 8.2 Metrics

-   Total tasks planned.
-   Total tasks completed.
-   Completion ratio/percentage.
-   Planned estimated duration, when estimates exist.

### 8.3 Charts

Recommended: - Daily/weekly bar chart: completed tasks versus planned
tasks. - Optional completion trend line only if it improves
interpretation.

Rules: - Use task planned date for period attribution. - Do not treat
tasks without an estimate as zero planned minutes. - Distinguish no
tasks from tasks with no duration estimates. - Keep chart axes and
labels readable. - Avoid productivity grades or red/green performance
judgements.

## 9. Focus analytics

### 9.1 Key questions

-   How much focused time was recorded?
-   How did recorded focus time vary across days?
-   How many sessions were completed?
-   What share of recorded time was linked to tasks versus general
    focus, if that distinction is useful?

### 9.2 Metrics

-   Total focused duration.
-   Number of finished sessions.
-   Average focused duration per finished session, only when the
    denominator is greater than zero.
-   Task-linked versus general focus duration, if reliable.

### 9.3 Charts

Recommended: - Bar or line chart: focused duration by day. - Optional
breakdown: task-linked and general focus.

Rules: - Use saved focused duration, not wall-clock duration. - Exclude
paused intervals. - Exclude unverified interruption gaps. - Do not treat
an unfinished session as a finished session. - If interrupted sessions
are included, identify their status and follow the inclusion rule from
the data architecture. - Avoid interpreting longer focus time as
automatically better.

## 10. Habit analytics

### 10.1 Key questions

-   How consistently was each habit completed on days it was due?
-   Which days had scheduled habits?
-   How did completion change over time?

### 10.2 Metrics

-   Completed due occurrences.
-   Total due occurrences.
-   Consistency percentage.
-   Per-habit completion ratio.

### 10.3 Charts and views

Recommended: - Per-habit horizontal progress bars with ratio labels. -
Calendar-style completion history for a selected habit. - Weekly trend
only when enough scheduled occurrences exist.

Rules: - Denominator includes only active scheduled occurrences. -
Non-scheduled days are neutral, not missed. - Do not compare habits
without considering their different schedules. - If no occurrences are
due in the period, show a neutral empty state. - Do not introduce
streaks unless the streak rule is explicitly defined and tested.

## 11. Finance analytics

### 11.1 Key questions

-   How much income was recorded?
-   How much was spent?
-   What was the net for the period?
-   Which expense categories contributed most to recorded expenses?

### 11.2 Metrics

-   Total income.
-   Total expenses.
-   Net for period.
-   Expense totals by category, if category data exists.

### 11.3 Charts

Recommended: - Grouped or stacked bar chart: income versus expenses by
day/week/month. - Category breakdown chart for expenses, only when there
are enough meaningful categories.

Rules: - Keep income and expense separate. - Use the transaction date
for period attribution. - Use the selected currency consistently. - Do
not aggregate different currencies without conversion. - Do not call net
for period an account balance. - Avoid pie charts with too many tiny
categories; group only if the grouping is transparent and does not hide
important information.

## 12. Period comparisons

Comparisons may help the user see change, but they must be fair and
like-for-like.

V1 comparison examples: - Current week versus previous week. - Current
month versus previous month. - Selected day versus the previous day, if
daily comparison is enabled.

Rules: - Compare equivalent period lengths. - Use the same metric
definition and units. - Show absolute values as well as percentage
change where useful. - If the previous period has zero or missing data,
do not divide by zero or show a misleading percentage. - Clearly label
both periods. - Avoid phrases such as "you improved" unless the metric
and context justify the statement. - Prefer neutral descriptions such as
"Recorded focus time was 45 minutes higher than last week."

## 13. Insights and interpretation

V1 should prioritize descriptive insights derived from clear
calculations.

Examples of acceptable statements: - "You completed 12 of 16 planned
tasks this week." - "You recorded 8 hours and 20 minutes of focused
sessions." - "Your reading habit was completed on 4 of its 5 scheduled
days." - "Recorded expenses were ₹600 lower than the previous week."

Avoid: - "You are lazy." - "You wasted your time." - "You are now more
productive." - "Your spending caused your low focus." - Any health,
psychological, or causal conclusion not supported by the data.

If automated text summaries are not reliable, use straightforward metric
labels and comparisons instead of inventing narrative insights.

## 14. Insufficient data and empty states

The analytics screen must distinguish:

1.  **No records:** no relevant records exist in the selected period.
2.  **Insufficient denominator:** a percentage cannot be calculated
    because there are no applicable occurrences.
3.  **Partial data:** some module records exist, while others do not.
4.  **Loading/error:** data could not yet be retrieved or calculated.

Examples: - No tasks: "No tasks planned in this period." - No focus
sessions: "No focus sessions recorded." - No habits due: "No scheduled
habit occurrences in this period." - No finance records: "No
transactions recorded in this period."

Rules: - Do not display a blank chart without explanation. - Do not fill
gaps with sample values. - A failure in one module should not hide
successful analytics from other modules. - Do not treat missing records
as zero when the data failed to load.

## 15. Data sources and calculation ownership

Insights is a read-only analytical layer. It must not own duplicate
copies of source records.

  -----------------------------------------------------------------------
  Analytics area          Source of truth         Key calculation
  ----------------------- ----------------------- -----------------------
  Tasks                   Task records            Completed and planned
                                                  tasks by planned date

  Focus                   Focus session records   Sum of valid focused
                                                  durations by
                                                  attribution date

  Habits                  Habit schedules and     Completed due
                          dated completion        occurrences / total due
                          records                 occurrences

  Finance                 Finance transactions    Income, expenses, and
                                                  net for period
  -----------------------------------------------------------------------

Requirements: - Use shared, testable calculation logic. - Dashboard and
Insights must use the same definitions for shared metrics. - Changes to
source records must be reflected in analytics. - Preserve historical
schedule and session information required for accurate reporting. -
Avoid calculations that differ between a metric card and its detailed
chart.

## 16. Performance and refresh behaviour

-   Load the selected period's summary efficiently.
-   Avoid recomputing all historical data after every small interaction
    if cached aggregates can be safely invalidated.
-   Invalidate or refresh affected analytics after a source record is
    created, edited, completed, or deleted.
-   Do not show stale cached data as current without an indication.
-   Preserve selected period and scroll position when returning from a
    source module where practical.
-   If a chart takes longer to load, show a local loading state rather
    than blocking the entire screen.

## 17. Privacy

-   Analytics must be calculated locally in V1 unless a separate
    approved architecture says otherwise.
-   Do not transmit task titles, habit names, focus history, transaction
    amounts, or category data to third-party analytics.
-   Avoid sensitive values in logs and crash reports.
-   Do not add external AI processing for interpretation in V1.
-   Keep data export and deletion behaviour aligned with the app's data
    policy.

## 18. Accessibility and chart usability

-   Every chart must have a clear title and date range.
-   Provide readable labels, units, and legends.
-   Do not rely on color alone to distinguish series.
-   Provide a textual summary or accessible data representation for
    important chart values.
-   Ensure chart labels remain readable on small screens and with larger
    font settings.
-   Use sufficient contrast in Light and Dark themes.
-   Avoid excessive grid lines, decorative gradients, and 3D effects.
-   Make date selectors and filters accessible to screen readers.

## 19. Acceptance criteria

### Period navigation

-   [ ] User can switch between Daily, Weekly, and Monthly views.
-   [ ] Selected date/range is clearly visible.
-   [ ] Previous/next period navigation works consistently.
-   [ ] All analytics sections use the same selected period.
-   [ ] Week and month boundaries follow the documented local-date
    rules.

### Metric correctness

-   [ ] Task metrics use task planned dates and correct completed/total
    counts.
-   [ ] Focus metrics exclude paused intervals and unknown interruption
    gaps.
-   [ ] Habit consistency counts only active scheduled occurrences.
-   [ ] Finance metrics separate income, expenses, and net for period.
-   [ ] No metric is calculated from fabricated sample data.
-   [ ] Shared metrics match the definitions used by Home and source
    modules.

### Charts and comparisons

-   [ ] Charts use correct units, labels, date ranges, and source data.
-   [ ] Charts distinguish zero data from unavailable data.
-   [ ] Comparisons use equivalent periods.
-   [ ] Percentage changes handle zero/missing previous values safely.
-   [ ] No chart implies unsupported causation or judgement.

### UX, privacy, and accessibility

-   [ ] Screen is readable and navigable on Android phones.
-   [ ] Light and Dark themes are supported.
-   [ ] Charts have accessible labels or textual alternatives.
-   [ ] One module's loading failure does not unnecessarily block other
    sections.
-   [ ] Personal activity and finance records are not sent to
    third-party analytics.

## 20. Open decisions for architecture and implementation

Resolve before implementation:

1.  Whether the default Insights period is current week or last-used
    period. Recommended: current week.
2.  Exact focus-session attribution rule for sessions crossing midnight.
3.  Whether interrupted sessions contribute to analytics and how they
    are labelled.
4.  Whether comparisons are included in the first V1 release or a later
    iteration.
5.  Whether daily focus breakdown uses start date, end date, or split
    intervals for cross-midnight sessions.
6.  How habit schedule history is stored and queried.
7.  Which chart library or native chart components are selected.
8.  Whether analytics aggregates are computed on demand or cached.
9.  How partial data and local database errors are surfaced.
10. Whether users can export analytics in V1. Default: defer unless
    separately approved.

Do not make calculation rules independently in chart components. All
metrics must come from shared, testable domain calculations.

## 21. Related documents

-   `01_PRODUCT_OVERVIEW.md` --- product direction and scope.
-   `02_PRODUCT_REQUIREMENTS.md` --- cross-module requirements.
-   `03_UI_UX_GUIDELINES.md` --- visual and chart guidelines.
-   `04_HOME_DASHBOARD.md` --- daily summary metrics.
-   `05_DAILY_PLANNING.md` --- task records and planned dates.
-   `06_ACTIVE_WORK_FOCUS.md` --- focus-session duration and recovery.
-   `07_DAILY_HABITS.md` --- schedules and dated completion records.
-   `08_FINANCE.md` --- transactions and finance calculations.
-   `10_DAILY_REVIEW.md` --- daily reflection.
-   `11_DATA_ARCHITECTURE.md` --- data sources, date rules, and
    aggregation.
-   `12_TECHNICAL_ARCHITECTURE.md` --- analytics implementation.
-   `13_IMPLEMENTATION_TESTING.md` --- calculation and chart tests.


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

### V1 life analytics model
Use the three life areas and score policy in the product decision update. Provide Daily, Weekly, Monthly, and Yearly/365-day views as appropriate. For comparisons, compare equivalent periods (e.g. Monday–Sunday against the previous Monday–Sunday). Expose performance, change, and consistency as separate concepts.
