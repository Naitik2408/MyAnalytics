# Life Analytics --- Finance

**Document:** `08_FINANCE.md`\
**Product:** Life Analytics\
**Version:** 1.0 (V1)\
**Status:** Module specification\
**Platform:** Android\
**Related documents:** `01_PRODUCT_OVERVIEW.md`,
`02_PRODUCT_REQUIREMENTS.md`, `03_UI_UX_GUIDELINES.md`,
`04_HOME_DASHBOARD.md`, `09_LIFE_ANALYTICS.md`,
`11_DATA_ARCHITECTURE.md`

------------------------------------------------------------------------

## 1. Purpose

The Finance module lets the user record personal income and expenses,
organize transactions into categories, and understand where money is
coming from and going.

The module must make transaction entry fast and financial summaries
trustworthy. All totals must be calculated from saved transactions. The
app must clearly distinguish money received, money spent, and account
balance.

**Core principle:** Record each transaction once. Calculate totals
automatically. Never imply that a calculated transaction total is a
verified bank balance.

## 2. UX goals

-   Record an expense or income in a few steps.
-   Make the transaction type obvious before the user enters an amount.
-   Use a numeric keyboard and currency-aware formatting.
-   Keep category selection quick and understandable.
-   Make today's expenses easy to find.
-   Allow users to correct an incorrectly entered transaction.
-   Show period summaries without mixing income and expenses.
-   Avoid unnecessary accounting terminology in the everyday interface.
-   Be transparent about what the app can and cannot know about the
    user's actual money.

## 3. V1 scope

### Included

-   Add income transaction.
-   Add expense transaction.
-   Edit and delete transactions.
-   Assign a date, category, and optional description.
-   Support predefined categories and user-managed custom categories if
    included in the final implementation.
-   View transaction history.
-   Filter transactions by date range and type.
-   Calculate income and expense totals.
-   Show daily, weekly, and monthly summaries.
-   Configure the currency used by the app.
-   Feed the Home Today's Expenses metric and Finance insights.

### Excluded from V1

-   Bank account linking or automatic bank transaction import.
-   UPI or payment-provider integrations.
-   Automatic receipt scanning or OCR.
-   Investment, loan, tax, or net-worth management.
-   Multi-currency conversion and exchange-rate services.
-   Budgets and spending alerts unless separately approved.
-   Financial advice or AI-based recommendations.

## 4. Finance information architecture

The Finance area should provide:

1.  A clear period selector (default: current month).
2.  Summary of income and expenses for that period.
3.  A balance figure only if its calculation basis is explicitly defined
    and reliable.
4.  A transaction list.
5.  A prominent Add Transaction action.
6.  Simple filters for transaction type and date range.
7.  Access to category management and currency settings.

Avoid showing too many cards or charts above the transaction list. Users
should be able to find and record a transaction quickly.

## 5. Finance screen structure

### 5.1 Suggested layout

``` text
┌──────────────────────────────────────┐
│ Finance                         [⋮] │
│                                      │
│        November 2026          [‹ ›]  │
│                                      │
│ ┌────────────────┐ ┌────────────────┐│
│ │ Total Income   │ │ Total Expenses ││
│ │ ₹ 25,000       │ │ ₹ 8,450        ││
│ └────────────────┘ └────────────────┘│
│                                      │
│ Net for this period                  │
│ ₹ 16,550                             │
│                                      │
│ Transactions              [Filter]   │
│                                      │
│ TODAY                                │
│ ↓ Groceries                 -₹ 450   │
│   Food · 10:30 AM                    │
│                                      │
│ ↑ Freelance payment        +₹ 5,000 │
│   Work · 09:00 AM                    │
│                                      │
│ YESTERDAY                            │
│ ↓ Transport                 -₹ 120  │
│   Travel · 06:20 PM                  │
│                                      │
│                         [+ Add]      │
├──────────────────────────────────────┤
│ Home    Finance   Activity  Insights │
└──────────────────────────────────────┘
```

All amounts above are illustrative wireframe values only. Never seed the
real user's account with these examples.

## 6. Currency

### 6.1 V1 currency behaviour

-   The user must have one selected currency for V1.
-   The initial default may be INR (₹), given the initial product
    context, but the currency must be configurable.
-   Display the currency symbol or code consistently.
-   Store the currency code with each transaction, even if V1 uses only
    one currency.
-   Do not silently reinterpret existing amounts when the user changes
    the selected currency.

### 6.2 Currency change

Changing currency after transactions exist is a consequential action.

Recommended V1 rule: - Ask for confirmation. - Explain that changing the
currency setting does not convert historical amounts. - Prefer
preventing a currency change once transactions exist unless the app
supports a clear migration/conversion workflow. - Do not imply that
`₹100` automatically becomes `$100` with equivalent value.

Final currency-change behaviour must be confirmed in
`11_DATA_ARCHITECTURE.md`.

### 6.3 Amount formatting

-   Use currency-aware formatting, including decimal places appropriate
    to the selected currency.
-   Keep the currency symbol/code visible.
-   Use grouping separators for large amounts.
-   Do not round stored values for display calculations in a way that
    changes totals.
-   Use a decimal-safe representation for money; avoid binary
    floating-point arithmetic for persisted financial amounts.

## 7. Add Transaction flow

### 7.1 Entry points

-   Add button on Finance.
-   Add Expense quick action on Home.
-   Any approved Add Income shortcut.

### 7.2 Transaction form

The form should be short and optimized for one-handed use.

``` text
┌──────────────────────────────────────┐
│ Add transaction                  [X] │
│                                      │
│ [ Expense ] [ Income ]               │
│                                      │
│ Amount                               │
│ [ ₹ 0.00                          ]  │
│                                      │
│ Category                             │
│ [ Select category                 ▾ ]│
│                                      │
│ Date                                 │
│ [ Today                           ▾ ]│
│                                      │
│ Note (optional)                      │
│ [ Add a short description         ]  │
│                                      │
│             [Cancel] [Save]          │
└──────────────────────────────────────┘
```

### 7.3 Form behaviour

-   Expense is the default type when launched from Home's Add Expense
    action.
-   The user must explicitly choose Income or Expense when entering from
    the general Finance flow, or use a clearly visible default with an
    easy way to change it.
-   Focus the amount field when the form opens.
-   Show the selected currency in or adjacent to the amount field.
-   Use a numeric/decimal keyboard appropriate to the currency.
-   Show categories appropriate to the selected transaction type.
-   Default the date to today.
-   Note/description is optional.
-   Keep Save reachable when the keyboard is visible.
-   Disable duplicate Save actions while a save is processing.
-   Preserve entered values if validation or saving fails.

### 7.4 Required fields

  -----------------------------------------------------------------------
  Field                   Required                Rules
  ----------------------- ----------------------- -----------------------
  Type                    Yes                     Income or Expense.

  Amount                  Yes                     Numeric amount greater
                                                  than zero; valid
                                                  currency precision.

  Category                Yes                     A valid category
                                                  suitable for the
                                                  selected type.

  Date                    Yes                     Valid transaction date.

  Note                    No                      Optional short
                                                  description.
  -----------------------------------------------------------------------

### 7.5 Validation

-   Reject empty, zero, or negative amounts.
-   Reject malformed numbers and unsupported decimal precision.
-   Reject an invalid category or type.
-   Reject invalid dates.
-   Do not accept currency symbols as part of the stored numeric amount.
-   Show a specific validation message near the field.
-   Never silently turn an invalid amount into a different value.

## 8. Transaction types

### Expense

Money paid or spent by the user.

Examples: - Food - Transport - Shopping - Bills - Education - Health -
Entertainment - Other

### Income

Money received by the user.

Examples: - Salary - Freelance - Business - Allowance - Gift - Refund -
Other

These are initial suggestions, not mandatory fixed categories. The final
category model should allow clear, maintainable categorization.

### Type-specific categories

-   An expense category must not accidentally be assigned to an income
    transaction unless categories are intentionally shared.
-   Switching type in the form must revalidate the selected category.
-   If the current category is not valid for the new type, ask the user
    to select a suitable category; do not silently substitute one.

## 9. Category management

### 9.1 V1 baseline

Provide a useful set of default categories for Income and Expense. If
custom categories are included: - The user can create a category with a
non-empty name. - Category names must be unique within the relevant
type, ignoring case and surrounding whitespace. - The user can rename or
deactivate a category. - Deactivating a category must not remove it from
historical transactions. - A category with existing transactions must
not be hard-deleted in a way that breaks history. - The transaction form
should show active categories only.

### 9.2 Avoid overcomplication

Do not force the user to build a complex category tree. V1 should use a
flat list with clear names. Category icons and colors are optional
visual aids, not required data.

## 10. Transaction list and history

### 10.1 Transaction row

Each row should show: - Transaction category or clear title. - Amount
with currency. - Income/Expense direction, using text/icon as well as
color. - Date or time context. - Optional note as secondary information.

### 10.2 Ordering

-   Show newest transactions first.
-   Use transaction date and creation timestamp as a stable ordering
    basis.
-   Group by date when this improves scanning.
-   Do not reorder unpredictably after an edit.

### 10.3 Transaction details

Tapping a transaction opens a detail/edit view with: - Type - Amount and
currency - Category - Date - Optional note - Edit action - Delete action

### 10.4 Filters

V1 should support: - Date range or period selection. - All, Income, or
Expense type filter.

Category filter is useful but may be deferred if it adds too much
complexity to the initial screen.

### 10.5 Empty states

-   No transactions in the selected period: "No transactions recorded
    for this period."
-   No results for the active filter: "No transactions match these
    filters."
-   Provide a clear way to add a transaction or clear filters.

## 11. Edit and delete transactions

### 11.1 Edit

-   Pre-fill the form with saved values.
-   Allow editing type, amount, category, date, and note.
-   Revalidate the entire transaction before saving.
-   Update all affected summaries after a successful save.
-   If the date changes, remove the transaction from the old date's
    totals and include it in the new date's totals.
-   Do not change the amount or type without an explicit user action.

### 11.2 Delete

-   Require confirmation or provide a reliable undo action.
-   Clearly state that the transaction will be removed from totals and
    history.
-   On successful deletion, update period totals and Home's Today's
    Expenses where relevant.
-   Do not show a success message before the delete is persisted.
-   If deletion fails, preserve the transaction and explain the error.

## 12. Finance calculations

### 12.1 Period income

Sum the amounts of all Income transactions in the selected period.

``` text
period_income = sum(income transaction amounts in period)
```

### 12.2 Period expenses

Sum the amounts of all Expense transactions in the selected period.

``` text
period_expenses = sum(expense transaction amounts in period)
```

### 12.3 Net for period

``` text
period_net = period_income - period_expenses
```

Label this value **Net for this period**, not Account Balance.

It describes the difference between recorded income and expenses within
the selected date range. It does not necessarily represent the user's
available money.

### 12.4 Today's Expenses

Home's Today's Expenses must: - Sum only Expense transactions. - Include
transactions whose local transaction date is today. - Exclude Income
transactions. - Use the same currency and aggregation rules as Finance.

### 12.5 Account balance

An actual account balance cannot be reliably inferred from only a
partial list of income and expense records.

For V1: - Do not display an "Account Balance" unless an opening balance
or a complete balance model is explicitly implemented. - If a balance
feature is later approved, define opening balance, account scope,
transfers, and reconciliation rules first. - Do not call `period_net`
the user's bank balance or available balance.

## 13. Period selection and date handling

-   Default Finance to the current month.
-   Allow previous/next period navigation.
-   Provide a clear period label, such as "November 2026".
-   Daily totals must use the selected local date.
-   Weekly and monthly totals must use consistent local date boundaries.
-   Date filtering must be inclusive and predictable.
-   The date/time storage and timezone strategy is defined in
    `11_DATA_ARCHITECTURE.md`.
-   Editing a transaction's date must update all affected period and
    daily summaries.

## 14. Summary visualization

The top of Finance should prioritize income, expenses, and net for the
selected period.

-   Use simple summary cards.
-   Keep income and expense visually distinct.
-   Use positive/negative signs or labels as well as color.
-   Avoid complex charts on the primary transaction-entry screen.
-   Detailed spending trends and category comparisons belong in
    Insights.
-   Do not show percentages or trends when the underlying data is
    insufficient.

## 15. Home and Insights integration

### Home

-   Add Expense opens the transaction form with Expense selected and
    date set to today.
-   Today's Expenses reads from saved Finance transactions.
-   After an expense is saved, edited, or deleted, Home's metric
    refreshes.
-   Home must not maintain its own transaction records.

### Insights

Insights may display: - Income and expenses over time. - Expense
breakdown by category. - Net for selected periods. - Comparisons between
equivalent periods.

All values must use the same transaction inclusion, currency, and date
rules as Finance. Do not imply a category caused a change in the user's
finances.

## 16. Loading, empty, error, and success states

### Loading

-   Use a stable layout and lightweight placeholders when needed.
-   Do not show sample financial amounts as if they were real.

### Empty

-   Explain that no transactions have been recorded in the selected
    period.
-   Offer one clear Add Transaction action.
-   Distinguish no transactions from no results under a filter.

### Error

-   Explain whether loading or saving failed.
-   Preserve form data after a save failure.
-   Offer retry where safe.
-   Do not display stale totals as current without a clear indication.

### Success

-   Confirm the saved transaction briefly.
-   Update the list and affected summaries.
-   Avoid blocking success dialogs.

## 17. Privacy and security

Finance records are sensitive personal data.

-   Keep finance data local in V1 unless a separately approved
    sync/backup design is introduced.
-   Do not send transaction details to third-party analytics.
-   Do not include amounts, categories, or notes in diagnostic logs.
-   Use appropriate Android storage protections and secure coding
    practices.
-   Avoid exposing full transaction notes in notifications.
-   Provide a clear data export/deletion policy when those capabilities
    are implemented.

## 18. Accessibility and usability

-   Use a numeric keyboard and sensible decimal handling.
-   Keep amount and currency readable at larger text sizes.
-   Provide accessible labels for Income/Expense selectors.
-   Do not rely on red/green alone to distinguish transaction types.
-   Ensure category selection and date controls are usable with screen
    readers.
-   Keep Save and Cancel actions reachable when the keyboard is open.
-   Use plain language such as "Money in", "Money out", and "Net for
    this period" where it improves comprehension.
-   Support Light and Dark themes with sufficient contrast.

## 19. Acceptance criteria

### Transaction entry

-   [ ] User can add Income and Expense transactions.
-   [ ] Add Expense from Home opens with Expense selected and today's
    date.
-   [ ] Amount must be greater than zero.
-   [ ] Currency precision is validated.
-   [ ] Category and date are required.
-   [ ] Note is optional.
-   [ ] Save failure preserves form input.
-   [ ] Duplicate taps do not create duplicate transactions.

### Transaction management

-   [ ] User can view transactions in newest-first order.
-   [ ] User can edit all supported fields.
-   [ ] User can delete a transaction with suitable protection.
-   [ ] Editing or deleting updates all affected totals.
-   [ ] Filters distinguish Income, Expense, and date range.
-   [ ] Empty filter results are clearly explained.

### Financial calculations

-   [ ] Period income sums only Income transactions.
-   [ ] Period expenses sum only Expense transactions.
-   [ ] Net for period equals income minus expenses.
-   [ ] Home Today's Expenses sums only today's Expense transactions.
-   [ ] Period net is not labelled Account Balance.
-   [ ] Currency formatting is consistent and precise.

### UX, privacy, and accessibility

-   [ ] Transaction entry is short and easy to use one-handed.
-   [ ] Light and Dark themes are supported.
-   [ ] Amount and type are accessible to screen readers.
-   [ ] Empty, loading, error, and success states are defined.
-   [ ] Financial details are not sent to third-party analytics.
-   [ ] No fabricated transaction values appear.

## 20. Open decisions for architecture and implementation

Resolve before implementation:

1.  Final default currency and whether it can be changed after
    transactions exist.
2.  Whether custom categories are included in V1 or only predefined
    categories.
3.  Exact default income and expense category lists.
4.  Whether account balance/opening balance is excluded entirely from
    V1. Recommended: exclude it.
5.  Whether category filtering is included in the first release.
6.  Maximum note length and amount precision rules.
7.  Decimal-safe amount representation in the database.
8.  Exact date and timezone storage strategy.
9.  Whether multiple accounts or wallets are excluded from V1.
    Recommended: one consolidated personal ledger.
10. Whether data export and backup are included in V1 or deferred.

Do not add a balance, account, or transfer model in the UI without a
matching data model and calculation specification.

## 21. Related documents

-   `01_PRODUCT_OVERVIEW.md` --- product direction and V1 scope.
-   `02_PRODUCT_REQUIREMENTS.md` --- cross-module requirements.
-   `03_UI_UX_GUIDELINES.md` --- shared design and interaction rules.
-   `04_HOME_DASHBOARD.md` --- Today's Expenses metric and Add Expense
    action.
-   `09_LIFE_ANALYTICS.md` --- financial trends and comparisons.
-   `10_DAILY_REVIEW.md` --- daily reflection and summaries.
-   `11_DATA_ARCHITECTURE.md` --- transaction schema, money
    representation, and date rules.
-   `12_TECHNICAL_ARCHITECTURE.md` --- persistence and implementation.
-   `13_IMPLEMENTATION_TESTING.md` --- calculation, validation, and
    data-integrity tests.


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

### Finance score interpretation addendum
Finance analytics must show income, expenses, net cash flow, savings amount/rate, and budget adherence when a budget exists. A numeric Finance score is optional until the user has supplied a valid target (budget or savings goal). Do not treat lower expenses alone as success and do not call net cash flow an account balance.
