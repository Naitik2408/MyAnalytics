# Life Analytics --- Daily Review

**Document:** `10_DAILY_REVIEW.md`\
**Product:** Life Analytics\
**Version:** 1.0 (V1)\
**Status:** Module specification\
**Platform:** Android\
**Related documents:** `01_PRODUCT_OVERVIEW.md`,
`02_PRODUCT_REQUIREMENTS.md`, `03_UI_UX_GUIDELINES.md`,
`04_HOME_DASHBOARD.md`, `05_DAILY_PLANNING.md`,
`06_ACTIVE_WORK_FOCUS.md`, `07_DAILY_HABITS.md`, `08_FINANCE.md`,
`09_LIFE_ANALYTICS.md`, `11_DATA_ARCHITECTURE.md`

------------------------------------------------------------------------

## 1. Purpose

Daily Review gives the user a short, structured way to look back on the
day. It connects what the user planned with what actually happened and
lets them record a few personal observations.

The goal is awareness, not judgement. The review should help the user
notice patterns over time without making them feel that every day must
be perfect.

**Core principle:** Automatically summarize what the app already knows;
ask the user only for information that cannot be inferred from existing
records.

## 2. UX goals

-   Make the review quick enough to complete in a few minutes.
-   Never ask the user to re-enter tasks, habits, focus time, or
    transactions.
-   Show planned and completed activity using source records.
-   Keep reflection prompts optional and concise.
-   Make saving and returning later predictable.
-   Allow the user to skip a review without penalty.
-   Avoid guilt-based language, streak pressure, or judgemental scores.
-   Support incomplete days and reviews completed the next morning.
-   Make past reviews easy to browse and edit.

## 3. V1 scope

### Included

-   One review per local calendar date.
-   Automatic daily summary from Tasks, Focus, Habits, and Finance.
-   Optional short reflection fields.
-   Save as draft and mark review complete.
-   View and edit previous reviews.
-   Date navigation and review history.
-   Clear handling when there is little or no source data.

### Excluded from V1

-   AI-generated reflection summaries or recommendations.
-   Voice recording or voice-to-text.
-   Mood diagnosis or mental-health scoring.
-   Automatic detection of distractions or reasons for unfinished tasks.
-   Social sharing, accountability partners, or public streaks.
-   Complex journaling, rich text editor, photos, or attachments.
-   Automatic creation of tomorrow's plan based on assumptions.

## 4. Daily Review entry points

The user can open Daily Review from:

1.  A Home Dashboard card or end-of-day prompt.
2.  A dedicated entry in the Activity area, if included in navigation.
3.  A date in the review history.

Do not force a review immediately when the user opens the app late at
night. A reminder, if supported, must be configurable and dismissible.

The review must remain accessible even when the user has not completed
all tasks or habits.

## 5. Recommended review flow

Keep the flow on one scrollable screen in V1 rather than creating a
multi-step wizard.

1.  Select or confirm the review date.
2.  Show the automatically generated daily summary.
3.  Show planned-versus-actual task information.
4.  Show focus, habit, and finance summaries.
5.  Offer a small number of optional reflection prompts.
6.  Save draft or mark the review complete.

The user should be able to save without answering any reflection prompt.

## 6. Screen structure

### 6.1 Suggested wireframe

``` text
┌──────────────────────────────────────┐
│ Daily Review                         │
│ Sunday, 15 November 2026       [‹ ›] │
│                                      │
│ YOUR DAY                             │
│ Tasks completed       3 / 5          │
│ Focus time            2h 15m         │
│ Habits completed      2 / 3          │
│ Expenses              ₹450           │
│                                      │
│ PLANNED VS ACTUAL                    │
│ ✓ Finish assignment                  │
│ ✓ Revise JavaScript                  │
│ ○ Read documentation                │
│ ...                                  │
│                                      │
│ REFLECTION (OPTIONAL)                │
│ What went well today?                │
│ [Write a short note...]              │
│                                      │
│ What could be better tomorrow?       │
│ [Write a short note...]              │
│                                      │
│ Anything worth remembering?          │
│ [Write a short note...]              │
│                                      │
│ [Save draft]       [Complete review] │
└──────────────────────────────────────┘
```

Values and task titles above are illustrative only. The actual screen
must use saved records and must not ship with fake daily activity.

## 7. Automatic daily summary

The summary is read-only and derived from source modules. It must not
create duplicate records.

### 7.1 Tasks

Display: - Number of tasks planned for the selected date. - Number
completed. - Optional short list of completed and incomplete tasks.

Rules: - Use the task date/planned date definition from
`05_DAILY_PLANNING.md`. - A task moved to another date must be
attributed according to the source module's final date-history rule. -
Do not count recurring habits as tasks. - Do not imply an incomplete
task was a failure.

### 7.2 Focus

Display: - Total valid focused duration. - Optional number of completed
sessions.

Rules: - Use the authoritative focus-session calculation from
`06_ACTIVE_WORK_FOCUS.md`. - Exclude paused time and unknown
interruption gaps. - Follow the agreed attribution rule for sessions
crossing midnight. - Do not describe recorded focus time as guaranteed
productivity.

### 7.3 Habits

Display: - Completed due habits divided by total habits due. -
Optionally show habit names and completion state.

Rules: - Count only habits scheduled and active on the selected date. -
A habit not scheduled that day must not appear as missed. - Use the
dated completion record, not a manually entered review value.

### 7.4 Finance

Display: - Total recorded expenses for the selected date. - Optionally
show recorded income and net for the day if this is useful and
consistent with `08_FINANCE.md`.

Rules: - Use transaction dates and the selected currency. - Do not call
net for the day an account balance. - If there are no transactions, show
"No transactions recorded" rather than implying the user spent nothing
in reality.

## 8. Planned versus actual

This section should help the user compare their plan with recorded
outcomes.

For each task planned on the selected date, show: - Task title. -
Completion state. - Planned time or estimated duration, if available.

Rules: - Do not infer actual time spent on a task from a focus session
unless the session is explicitly linked to that task. - If a task has
linked focus sessions, optionally show their total recorded focused
duration. - If there is no linked session, show no value or "No focus
session linked"; do not show `0 minutes` as though time was measured. -
Do not automatically mark tasks complete because a focus session
ended. - Do not create or move tasks from the review screen without an
explicit user action. - Keep the source task editable through a clear
navigation action.

## 9. Reflection prompts

Reflection is optional. The user should not be forced to fill every
field.

Recommended V1 prompts:

1.  **What went well today?**
2.  **What could be better tomorrow?**
3.  **Anything worth remembering?**

Guidelines: - Use short multiline text fields. - Keep prompts neutral
and non-judgemental. - Allow any field to remain blank. - Allow the user
to save a review with no reflection text. - Do not impose arbitrary
minimum character counts. - Preserve line breaks and user-entered
text. - Avoid asking for information already captured in the app. - Do
not ask the user to explain every incomplete task.

Optional future prompt ideas should not be added to V1 without a clear
UX need.

## 10. Save, draft, and completion behaviour

A review has a date and a review status.

Recommended statuses: - `draft`: saved but not marked complete. -
`completed`: user explicitly marked the review complete.

Rules: - Saving a draft must not imply completion. - Completing a review
must not lock it permanently. - User can reopen and edit a completed
review. - If the user leaves with unsaved changes, warn before
discarding them. - If autosave is implemented, clearly communicate when
changes are saved. - Do not create duplicate reviews for the same date;
reopen the existing review. - A review can be completed even when source
data is empty. - A skipped review is not a failed day.

## 11. Date and late-review behaviour

-   Default to the current local date when creating a new review.
-   Allow reviewing previous dates.
-   Do not allow future dates to be marked as completed reviews.
-   If the user opens yesterday's review today, keep the selected date
    explicit.
-   Source summaries must reflect the selected date, not the current
    date.
-   If source records are edited after a review was completed, refresh
    the automatic summary when the review is opened.
-   Clearly distinguish automatically updated summary data from
    user-authored reflection text.
-   Preserve the review's selected date when navigating to source
    records and returning.

## 12. Review history

Provide a simple history view with: - Date. - Review status (Draft or
Completed). - Optional compact summary such as tasks completed.

UX requirements: - Sort newest first. - Allow opening a date to view or
edit its review. - Use pagination or lazy loading if history grows. - Do
not display a completion streak or shame-oriented missing-day
indicator. - Dates without reviews may be shown neutrally only if that
helps the user navigate.

## 13. Empty, partial, and error states

### No activity

Message example:

> Nothing has been recorded for this date yet. You can still write a
> reflection or leave the review for later.

Do not populate the screen with fake tasks, habits, or transactions.

### Partial activity

If only some modules contain records, show the available summaries and
neutral empty states for the others. One empty module must not hide the
rest.

### No reflection

Show blank optional prompts. Do not treat missing reflection text as
missing review data.

### Loading or retrieval error

-   Use local loading indicators for summary sections.
-   If one source module fails, show the other available sections.
-   Clearly identify unavailable data; do not replace an error with
    zero.
-   Preserve any reflection text already saved locally.
-   Provide a retry action when appropriate.

## 14. Data ownership and relationships

Daily Review owns only review-specific information, such as: - Review
ID. - Local review date. - Reflection text fields. - Draft/completed
status. - Created and updated timestamps.

It does not own copies of: - Tasks. - Focus sessions. - Habit
definitions or completion records. - Finance transactions.

The daily summary should be calculated from source records when
displayed or through a safely invalidated aggregate. It must not become
a second source of truth.

The data architecture document must define: - Unique review date
constraint. - Local date and timezone representation. - How summary
values are refreshed after source edits. - Whether a review stores a
historical snapshot or always shows current source data.

Recommended V1 behaviour: show current source data for the selected date
and preserve the user's reflection independently. If a historical
snapshot is needed later, add it as an explicit feature rather than
silently freezing stale summary values.

## 15. Privacy and data handling

-   Keep review text local in V1.
-   Do not send reflections to third-party analytics or AI services.
-   Avoid including reflection content in diagnostic logs.
-   Ensure review deletion behaviour is clear and separate from deleting
    source tasks or transactions.
-   Follow the app's backup, export, and data deletion policy once
    defined.

## 16. Accessibility and interaction details

-   Use readable section headings and clear field labels.
-   Make text areas large enough for comfortable typing.
-   Keep save actions reachable without excessive scrolling where
    practical.
-   Ensure keyboard opening does not hide the active field or save
    controls.
-   Support Android back navigation without silently discarding edits.
-   Use clear status text in addition to icons.
-   Support Light and Dark themes.
-   Provide screen-reader descriptions for summary values and task
    states.
-   Use touch targets that meet the app-wide accessibility guidelines.

## 17. Acceptance criteria

### Daily summary

-   [ ] Review displays the selected date and uses that date for every
    source summary.
-   [ ] Task counts match Daily Planning.
-   [ ] Focus duration matches the Focus module's calculation.
-   [ ] Habit counts include only due scheduled habits.
-   [ ] Finance values match the Finance module and selected currency.
-   [ ] No summary data is manually duplicated by the user.

### Planned versus actual

-   [ ] All tasks planned for the selected date are represented.
-   [ ] Completion state matches the source task record.
-   [ ] Linked focus duration is shown only when a real linked session
    exists.
-   [ ] Finishing a focus session does not automatically complete its
    task.

### Reflection and saving

-   [ ] All reflection fields are optional.
-   [ ] User can save a draft with all fields blank.
-   [ ] User can complete a review without reflection text.
-   [ ] User can edit a completed review.
-   [ ] Only one review exists per local calendar date.
-   [ ] Unsaved edits are not silently discarded.

### Dates, errors, and privacy

-   [ ] Past dates can be reviewed.
-   [ ] Future dates cannot be marked complete.
-   [ ] Source changes refresh the automatic summary.
-   [ ] Partial source failures do not erase available sections or saved
    reflection.
-   [ ] Reflection text is not sent to third-party analytics.

## 18. Open decisions for implementation

Resolve with `11_DATA_ARCHITECTURE.md` and
`12_TECHNICAL_ARCHITECTURE.md`:

1.  Whether review summaries are always live or a historical snapshot is
    retained. Recommended: live source summary in V1.
2.  Whether review creation is automatic as a draft or only created
    after the user saves. Recommended: create only on explicit save to
    avoid clutter.
3.  Whether autosave is needed for reflection fields. Recommended:
    implement reliable draft saving; autosave may be used if save state
    is visible.
4.  Whether a reminder is included in V1. Recommended: defer until core
    workflow is stable.
5.  Whether review history is a separate screen or a date selector plus
    list.
6.  How source records changed after review completion are reflected.
7.  Whether the daily summary includes income and net in addition to
    expenses. Keep this compact and consistent with Finance.

Do not add a mandatory mood score, daily rating, or streak system
without explicit product approval.

## 19. Related documents

-   `01_PRODUCT_OVERVIEW.md` --- product purpose and V1 boundaries.
-   `02_PRODUCT_REQUIREMENTS.md` --- cross-module requirements.
-   `03_UI_UX_GUIDELINES.md` --- interaction, typography, accessibility,
    and states.
-   `04_HOME_DASHBOARD.md` --- daily dashboard summary.
-   `05_DAILY_PLANNING.md` --- tasks and planned dates.
-   `06_ACTIVE_WORK_FOCUS.md` --- focus sessions and duration rules.
-   `07_DAILY_HABITS.md` --- scheduled habits and completion records.
-   `08_FINANCE.md` --- income, expenses, and period net.
-   `09_LIFE_ANALYTICS.md` --- cross-period trends and comparisons.
-   `11_DATA_ARCHITECTURE.md` --- storage and date rules.
-   `12_TECHNICAL_ARCHITECTURE.md` --- implementation design.
-   `13_IMPLEMENTATION_TESTING.md` --- test cases and release checks.


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

### Review and analytics boundary
Daily review reflections may provide context but must not be converted into an inferred mood, mental-health, or life score. Only structured, explicitly recorded metrics contribute to numeric category scores.
