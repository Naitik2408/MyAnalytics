# 14 — Product Design Redesign, Information Architecture & Visualization Decisions

**Date:** November 20, 2026  
**Scope:** Independent holistic product audit, IA cleanup, domain analytics visualizations, and cross-screen UX refinements across the Life Analytics Android application.

---

## 1. Product Design Audit

| Area | Finding | Decision |
|---|---|---|
| **Navigation & IA** | `ProfileScreen` listed *Activity history* (`ActivityHubScreen`), which only launched *Daily Planning*, *Daily Habits*, and *Daily Review* — all 3 already directly listed in `ProfileScreen`. | Removed the redundant *Activity history* entry from `ProfileScreen` to eliminate a duplicate navigation layer. |
| **Secondary Back Navigation** | `InsightsScreen` had no `onNavigateBack` action in `LaScreenHeader` despite no longer being on the bottom dock. | Added explicit back button wiring (`onNavigateBack = { navController.popBackStack() }`). |
| **Home Dashboard** | Performance Trend card showed only the aggregate score without explaining which pillar drove it; `Today's tasks` silently truncated at 5 items. | Added a compact 3-pillar score strip (`Health · Career · Finance`, `—` when missing) inside the Trend card, a completion count subtitle, and an explicit `"+N more tasks • Tap View all"` footer when >5 tasks exist. |
| **Health & Lifestyle** | Showed only two period counters (`Health Tasks`, `Health Habits`) and today's checkboxes — no visibility into whether health adherence was trending up/down or which routine was slipping. | Added **Health consistency trend** (`LaPerformanceLineChart` in mint) and **Routine adherence** (`LaHorizontalTrackBar` sorted lowest-first), plus contextual `HEALTH_LIFESTYLE` form pre-selection. |
| **Career & Productivity** | Showed only two counters and today's lists, plus stale subtitle copy mentioning `"focus"`. | Added **Career progress trend** (`LaPerformanceLineChart` in blue), **Learning habit consistency** (`LaHorizontalTrackBar`), fixed subtitle, and added contextual `CAREER_PRODUCTIVITY` form pre-selection. |
| **Finance** | Displayed current period totals without period-over-period spending context. | Added real previous-period expense delta (`Expenses ±X% vs prev month/day`) in the Net Cash Flow hero subtitle when previous-period data exists. |

---

## 2. Final Information Architecture

- **Primary Bottom Dock (4 Pillars):**
  1. **Home (`home`)** — Daily command center + yearly progress heatmap + macro trend + quick capture.
  2. **Health (`health`)** — Health tasks & habits summary, consistency trend, per-routine adherence, and today's health items.
  3. **Career (`career`)** — Career tasks & habits summary, progress trend, learning habit consistency, and today's career items.
  4. **Finance (`finance`)** — Net cash flow with period comparison, 2×2 KPI bento (Income, Expenses, Savings Rate, Budget), spending bar chart, category ring chart, and ledger.
- **Secondary Hub (`profile`, via Home avatar):**
  - **Daily review (`review`)** — Quantitative day recap + qualitative journal.
  - **Insights (`insights`)** — Cross-pillar analytics deep-dive (with explicit back navigation).
  - **Tasks planner (`planning`)** — Full date-navigable task management.
  - **Habits (`habits`)** — Full habit schedule & history management.
  - **Settings (`settings`)** — Appearance, currency, AES-256-GCM cloud/local backup, and isolated historical sample data controls.

---

## 3. Data Visualization Decisions

All visualizations strictly reuse the native lavender-led design system in `Charts.kt` and respect metric-integrity rules (missing data = `null` / `—`, never `0`; future dates excluded):

| Visualization | Screen | Question Answered | Format Justification |
|---|---|---|---|
| **Days Passed / Remaining + 365-Day Heatmap** | Home | *How much of the year is left, and how consistently have I used elapsed days?* | Separates time (numeric progress) from daily execution (20×19 intensity matrix distinguishing scored, no-data, and future days). |
| **Overall Performance Line Chart + 3-Pillar Strip** | Home | *Is my overall trajectory improving, and which pillar moved it?* | Smooth curve for trend + single-line pillar breakdown on the selected point without adding extra cards. |
| **Category Consistency Line Chart** | Health (mint), Career (blue) | *Is my Health / Career performance improving across weeks/months?* | Projected via `LifeAnalyticsEngine.buildCategoryTrendSeries` using the exact same `LaPerformanceLineChart` component. |
| **Routine Adherence Track Bars** | Health (mint), Career (blue) | *Which specific habit is slipping in the selected period?* | `LaHorizontalTrackBar` sorted lowest-adherence-first surfaces actionable gaps immediately without re-adding CRUD lists. |
| **Weekly Spending Pill Bar + Category Ring Chart** | Finance | *When did I spend most, and where did the money go?* | `LaPillBarChart` for temporal comparison + `LaRingChart` for proportional category share. |

---

## 4. Implementation Summary

- **Domain:** Added `LifeAnalyticsEngine.buildCategoryTrendSeries` + unit tests in `LifeAnalyticsEngineTest.kt`.
- **Forms:** Added optional `defaultLifeArea` to `HabitFormDialog` and `TaskFormDialog` (preserving full manual override).
- **Navigation:** Wired `onNavigateBack` on `InsightsScreen`; removed redundant `ActivityHubScreen` entry from `ProfileScreen`.
- **UI & Analytics:** Upgraded `HomeDashboardScreen`, `HealthScreen`, `CareerScreen`, and `FinanceScreen` with zero database schema changes and zero risk to existing personal records or backups.

---

## 5. Testing & Verification Report

- **Automated Verification:** `./gradlew testDebugUnitTest assembleDebug` — all unit and Room integration tests pass, including new `buildCategoryTrendSeries` boundary tests (missing intervals remain `null`, never `0`).
- **Limitation:** No Android emulator or physical device is attached to this Linux VM; visual verification on physical hardware should be performed with the newly built `LifeAnalytics_Debug_v13.apk`.
