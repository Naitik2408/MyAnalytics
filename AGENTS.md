# AGENTS.md — Mandatory Instructions for Life Analytics

## 1. Mission and source of truth

Build and maintain Life Analytics according to this documentation. Read this file and the relevant numbered documents before editing code. Inspect the actual repository, dependencies, database, and current UI before proposing or implementing changes. Do not assume that an older document or a previous implementation is still accurate.

## 2. Product decisions that must be preserved

- Keep operational **Habits** and **Tasks** as separate modules.
- Add an analytics classification layer with exactly three V1 life areas: `HEALTH_LIFESTYLE`, `CAREER_PRODUCTIVITY`, and `FINANCE_WEALTH`.
- New/edited habits and tasks receive a manual life-area selection. Automatic classification is future scope.
- Do not force migration or backfilling of old records. Legacy records may remain unclassified. Never assign a category by guessing.
- English speaking, reading for learning, and skill-building habits belong to Career & Productivity in V1.
- Home gives equal importance to overall analytics/growth and today's tasks/habits.
- The 365-day view distinguishes elapsed days, future days, valid performance scores, and missing data. Leap years contain 366 day cells.
- Use the revised lavender-led, light-mode-first design system in `03_UI_UX_GUIDELINES.md`.

## 3. Metric integrity

- Habit adherence = completed scheduled habit occurrences / scheduled habit occurrences × 100. Respect effective schedules.
- Task completion = completed planned tasks / planned tasks × 100. Show numerator and denominator. V1 gives tasks equal weight; do not invent priority weights.
- Career score combines career-habit adherence (40%) and task completion (60%) when both exist. If only one has eligible data, show a partial score and explain coverage.
- Health score is primarily based on health-habit adherence. Optional sleep/activity data can contribute only when actually recorded and explicitly configured.
- Finance metrics include income, expenses, net cash flow, savings amount/rate, and budget adherence where a budget exists. A Finance score requires a meaningful user-defined target.
- Overall score is the equal-weight average of category scores with sufficient data. Show category coverage. Missing data is never zero.
- Distinguish performance, period-over-period change, and consistency. Never claim scores are scientifically validated measures of life quality, health, or success.
- Focus duration is supporting activity data, not proof of productivity. Daily review text must not be used to infer psychological or medical states.

## 4. Data safety

- Before schema changes, inspect and back up existing data using the repository's supported mechanism.
- Preserve existing task, habit, completion, focus, and finance records.
- Prefer additive, reversible migrations. Never drop or rewrite user data to simplify analytics.
- Keep source records authoritative; derived analytics must be reproducible.
- Use local calendar dates and timezone rules consistently. Handle day boundaries and leap years.

## 5. UI and accessibility

- Follow `03_UI_UX_GUIDELINES.md`; do not introduce a competing design direction.
- Use semantic design tokens, readable labels, accessible contrast, and non-color cues.
- No fabricated production chart data. Clearly label empty, insufficient, and partial-data states.
- Do not use the lowest-performance color for missing data or future dates.

## 6. Engineering workflow

1. Audit the existing project and report the relevant files/schema.
2. Identify the smallest additive change that satisfies the requirement.
3. Explain the plan and data impact before any destructive or irreversible action.
4. Implement in small milestones with clear separation between UI, domain calculations, and persistence.
5. Add unit tests for every calculation and boundary condition.
6. Run the available tests, lint, and build commands; report exact outcomes.
7. Test on a real Android device where platform behaviour matters.
8. Update documentation when implementation changes approved behaviour.

## 7. Required analytics tests

Test scheduled versus unscheduled habit dates; active/deactivated habit ranges; empty denominators; task completion counts; category filtering; missing/partial category data; no-income savings-rate handling; budget absence; equivalent-period comparisons; leap-year 366-day rendering; future dates; and distinct no-data versus low-score presentation.

## 8. Scope discipline

Do not add automatic categorisation, predictive AI, psychological scoring, social comparison, extra life areas, or complex weighted task priority in V1 unless the user explicitly approves a scope change. Do not redesign unrelated modules while implementing analytics.
