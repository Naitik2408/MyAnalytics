# Life Analytics — Growth Direction (Growth Engine)

**Document:** `14_GROWTH_ENGINE.md`
**Product:** Life Analytics
**Version:** 1.0 (V1 addition)
**Status:** Specification — decisions approved
**Platform:** Android
**Related documents:** `01_PRODUCT_OVERVIEW.md`, `02_PRODUCT_REQUIREMENTS.md` (section 11A, `DIR-*`), `03_UI_UX_GUIDELINES.md` (section 9.7), `04_HOME_DASHBOARD.md` (section 5A), `09_LIFE_ANALYTICS.md` (section 12.1), `11_DATA_ARCHITECTURE.md`, `12_TECHNICAL_ARCHITECTURE.md`, `13_IMPLEMENTATION_TESTING.md` (Milestone 12)

---

## 1. Purpose

The app records habits, tasks, focus sessions, and finances well, but it does not tell the user whether things are getting better. This document defines a **Direction** layer: for three pillars (Habits, Tasks, Finance) the app compares the current period with the previous equivalent period and shows whether the recorded metric is Improving, Steady, or Slipping, with a short neutral reason.

Direction is a read-only layer over existing source records. It adds no data entry, no entity, no permission, and no schema change.

## 2. Principles

1. **A rough picture of what was recorded.** Direction describes recorded metrics, like a step counter that misses unworn time. It does not claim to measure quality of life.
2. **Compare with my own past only.** No universal targets or scores.
3. **Never invent data.** If there is not enough data, say so and say when the comparison unlocks (`01` principle 5, `09` section 14).
4. **Transparent.** Every verdict has a one-line reason, and the numbers behind it are one tap away.
5. **No composite number.** The overall result is a verdict word, not a score.
6. **Shared calculations.** Direction reuses the existing habit-consistency, task-completion, and period-net calculations. It never re-implements them.

## 3. Pillars (V1)

| Pillar | Source of truth | Measure | Verdict |
|---|---|---|---|
| Habits | Habit schedule periods and completion records | Completion rate = completed due occurrences ÷ total due occurrences (`09` section 7.3) | Yes |
| Tasks | Task records by planned date | Completion rate = completed tasks ÷ planned tasks (`04` section 5.1) | Yes |
| Finance | Finance transactions | Period net = income − expenses (`09` section 7.4) | Month and Year windows only |

Not pillars in V1: focus time (shown only as a supporting line under Tasks, no separate verdict), mood/review (descriptive only).

## 4. Completed-Days Rule

Direction compares **completed local days only** for Habits and Tasks:
- A day is completed when its local date is strictly before today (`date < today`).
- "Today" is excluded from Habit and Task Direction because the day is still open. Including an incomplete day makes completion rates look artificially low.
- Finance is the exception: financial transactions are point-in-time facts, so Finance comparisons include transactions recorded today.

## 5. Comparison Windows & Unlock Thresholds

A day is **recorded** if at least one habit completion, task created or completed, focus session, or finance transaction is dated on that day.

| Window | Current period | Previous period | Minimum current | Minimum previous | Unlock condition |
|---|---|---|---|---|---|
| **Day** | Yesterday | Day before yesterday | Yesterday completed | Day before completed | At least 1 recorded day on each of the 2 days |
| **Week** | Mon through yesterday (current week) | Full previous Mon–Sun week | ≥ 3 completed days in current week | Full previous week | ≥ 5 recorded days in previous week |
| **Month** | 1st through yesterday (current month) | Full previous calendar month | ≥ 7 completed days in current month | Full previous month | ≥ 14 recorded days in previous month |
| **Year** | Jan 1 through yesterday (current year) | Full previous calendar year | ≥ 30 completed days in current year | Full previous year | ≥ 90 recorded days in previous year |

If the unlock condition is not met, the window state is **Locked**. The UI shows:
- The missing requirement.
- When it will unlock (e.g., "Unlocks Thursday with 3 days recorded this week").

## 6. Verdict Rules

### 6.1 Habit & Task Pillars

Let $\Delta = \text{current rate} - \text{previous rate}$ in percentage points.

| Window | Improving | Steady | Slipping |
|---|---|---|---|
| **Day** | $\Delta \ge +15\text{ pts}$ | $-15\text{ pts} < \Delta < +15\text{ pts}$ | $\Delta \le -15\text{ pts}$ |
| **Week** | $\Delta \ge +5\text{ pts}$ | $-5\text{ pts} < \Delta < +5\text{ pts}$ | $\Delta \le -5\text{ pts}$ |
| **Month** | $\Delta \ge +5\text{ pts}$ | $-5\text{ pts} < \Delta < +5\text{ pts}$ | $\Delta \le -5\text{ pts}$ |
| **Year** | $\Delta \ge +5\text{ pts}$ | $-5\text{ pts} < \Delta < +5\text{ pts}$ | $\Delta \le -5\text{ pts}$ |

Special cases:
- **Tasks with low denominator:** If planned tasks in current or previous period < minimum (Day: 2, Week: 4, Month: 10, Year: 40), verdict is **No verdict** with reason: "Plan more tasks to compare".
- **Habits with no scheduled habits:** Verdict is **No verdict** with reason: "No habits scheduled in period".
- **Both periods at 100%:** Verdict is **Steady** with reason: "Maintained 100%".

### 6.2 Finance Pillar

Evaluated for **Month** and **Year** windows only. Day and Week windows show net and spent amounts with a neutral "vs previous" note, but no verdict.

- **Current period is open:**
  - If current net > previous full period net: **Improving** ("Ahead of last month by ₹X").
  - If current net ≤ previous full period net: **No verdict** ("₹X more to beat last month").
- **Current period is closed (end-of-month review):**
  - Current net > previous net: **Improving** ("Saved ₹X more than previous month").
  - Current net < previous net: **Slipping** ("Saved ₹X less than previous month").
  - Current net == previous net: **Steady** ("Same net as previous month").
- **No income recorded in current period:** **No verdict** ("Waiting for income entry").
- **Previous period has no transactions or < 14 recorded days:** **No verdict** ("Not enough data").

### 6.3 Supporting Line: Focus Time

Under the Tasks pillar, a supporting line shows:
$$\text{"Focus: } X\text{h } Y\text{m (was } A\text{h } B\text{m)"}$$
Focus time does not produce an Improving/Steady/Slipping verdict. It is descriptive context for task completion.

### 6.4 Overall Verdict

Aggregated across pillars that have an active verdict (Improving, Steady, Slipping):

| Condition | Overall Verdict |
|---|---|
| $\ge 1$ Improving, 0 Slipping | **Improving** |
| $\ge 1$ Slipping, 0 Improving | **Slipping** |
| $\ge 1$ Improving AND $\ge 1$ Slipping | **Mixed** |
| All active pillars Steady | **Steady** |
| No pillar has an active verdict | **Building baseline** |

## 7. Architecture

Pure Kotlin engine with zero Android UI dependencies:
```
app/src/main/java/com/lifeanalytics/app/domain/growth/
├── GrowthEngine.kt            # Core calculation and verdict logic
├── GrowthEngineConfig.kt      # Centralized thresholds and minimums
└── GrowthModels.kt            # Value classes, enums, result types
```

