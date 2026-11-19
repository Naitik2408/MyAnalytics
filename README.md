# Life Analytics

Personal life tracking and analytics application for Android. This documentation bundle contains 13 product, design, module, data, architecture, and testing documents plus the repository README and agent instructions.

## Current product decisions (19 November 2026)

- Operational tracking remains split into **Habits** and **Tasks**.
- Analytics adds three life areas: **Health & Lifestyle**, **Career & Productivity**, and **Finance & Wealth**.
- Users manually classify new/edited habits and tasks. Automatic categorisation is future scope.
- Existing records do not need migration; preserve them and do not silently classify them.
- Home balances overall performance/growth analytics and today's execution. It includes a period trend chart and a 365-day calendar whose elapsed-day state is separate from performance intensity.
- Missing data is not zero. Scores are descriptive tracking indicators, not validated measures of life quality or health.
- UI uses the revised lavender-led, light-mode-first visual direction in `03_UI_UX_GUIDELINES.md`.

## Documentation index

| File | Purpose |
|---|---|
| `01_PRODUCT_OVERVIEW.md` | Product vision, scope, principles |
| `02_PRODUCT_REQUIREMENTS.md` | Functional and non-functional requirements |
| `03_UI_UX_GUIDELINES.md` | Approved visual system and interaction rules |
| `04_HOME_DASHBOARD.md` | Home dashboard structure and behaviour |
| `05_DAILY_PLANNING.md` | Daily tasks and planning |
| `06_ACTIVE_WORK_FOCUS.md` | Focus sessions and timer |
| `07_DAILY_HABITS.md` | Habit scheduling and completion |
| `08_FINANCE.md` | Income, expenses, and budgets |
| `09_LIFE_ANALYTICS.md` | Metrics, scores, comparisons, insights |
| `10_DAILY_REVIEW.md` | Daily reflection |
| `11_DATA_ARCHITECTURE.md` | Entities, relationships, source of truth |
| `12_TECHNICAL_ARCHITECTURE.md` | App architecture and implementation constraints |
| `13_IMPLEMENTATION_TESTING.md` | Milestones, test plan, acceptance criteria |
| `AGENTS.md` | Mandatory instructions for coding agents |

## Recommended reading order

1. `AGENTS.md`
2. `01_PRODUCT_OVERVIEW.md` and `02_PRODUCT_REQUIREMENTS.md`
3. `03_UI_UX_GUIDELINES.md` and `04_HOME_DASHBOARD.md`
4. Module documents `05`–`10`
5. `11_DATA_ARCHITECTURE.md`, `12_TECHNICAL_ARCHITECTURE.md`, and `13_IMPLEMENTATION_TESTING.md`

## Important implementation note

Treat each source module as the source of truth. Analytics must derive from saved records and be rebuildable. Do not rewrite existing tracking flows or perform destructive migrations to implement the analytics layer. If a requirement conflicts, the 19 November 2026 product-decision addendum in the numbered documents takes precedence for V1.
