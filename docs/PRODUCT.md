# MoneyTrack: Product Overview

MoneyTrack is a simple, private personal finance tracker for Android. You record income and expenses, set monthly budgets, and see where your money goes. Your financial data stays on your phone.

- **Package:** `com.moneytrack.app`
- **Platform:** Android 8.0+ (minSdk 26, compile/target SDK 36)
- **Stack:** Kotlin, Jetpack Compose, Material 3, Room, Firebase Auth
- **Distribution:** Google Play and a signed APK on [GitHub Releases](https://github.com/IOREBIYI/money-track/releases/latest)
- **Version:** see `versionName` / `versionCode` in `app/build.gradle.kts`

## Who it is for

People who want a quick, no-frills way to track spending without handing their transactions to an ad network or a cloud service.

## Core idea: private by design

| What | Where it lives |
|---|---|
| Transactions and budgets | On-device Room (SQLite) database only. Never uploaded. |
| Currency choice | On-device `SharedPreferences`. |
| Email and user ID | Firebase Authentication (needed for sign-in). |
| Password | Sent to Firebase Auth over HTTPS; the app does not store it. |
| Ads, analytics, trackers | None. |

Details: [PRIVACY.md](../PRIVACY.md) and [ACCOUNT_DELETION.md](../ACCOUNT_DELETION.md).

## Features

### Accounts
- Sign up, sign in and "forgot password" by email.
- Validation before any network call: well-formed email, password of 8+ characters with at least one letter and one digit.
- Friendly error messages (wrong credentials, email already used, no network, too many attempts, and so on).
- In-app account deletion (asks for the password to re-authenticate).
- If a build has no Firebase config, the app shows "Accounts aren't set up" instead of crashing.

### Transactions
- Add, edit and delete income or expenses with an amount, category, date and note.
- Month switcher to browse any month.
- Amounts are stored as integer cents to avoid rounding errors.

### Budgets
- Set a monthly limit per category.
- Progress bars show how much of each budget is used. Setting a limit of 0 removes the budget.

### Reports
- Spending by category for the selected month.
- Six-month income vs. expenses chart.

### Settings and backup
- Pick from 39 currencies. This is **display only**; existing amounts are never converted.
- Export all transactions to CSV and restore from a CSV (also how you move to a new phone). Restore **replaces** all current transactions.
- Sign out and delete account.

### Design
A ledger-style theme with its own palette and a matching dark mode (`ui/Theme.kt`).

## Screens

The app has four bottom-navigation tabs, with an add-transaction button on the Transactions tab:

1. **Transactions**: list for the selected month, tap to edit.
2. **Budgets**: per-category limits and progress.
3. **Reports**: category breakdown and 6-month chart.
4. **Settings**: currency, CSV export/restore, sign out, delete account.

Screenshots are in [`store/screenshots/`](../store/screenshots/).

## Data model

| Table | Fields |
|---|---|
| `transactions` | `id` (auto PK), `isIncome`, `amountCents`, `category`, `note`, `epochDay` |
| `budgets` | `category` (PK), `limitCents` |

CSV format: header `date,type,amount,category,note`, with `type` being `income` or `expense` and `date` in ISO `yyyy-MM-dd`.

## Quality and delivery

- **Unit tests (JUnit 4):** CSV, currencies, auth validation, auth error mapping, auth view model, percent labels.
- **Instrumented tests:** Room DAO and the Firebase sign-in flows against the local Auth emulator.
- **CI (`.github/workflows/pr-checks.yml`):** on every pull request, unit tests, lint and a debug build, plus emulator-based integration tests. CI holds no release secrets.
- **Release:** R8 minification and shrinking, signed upload key, Play App Signing. Each Play upload needs a higher `versionCode`.

## Related documents

- [Architecture](ARCHITECTURE.md), with the diagram in [architecture.svg](architecture.svg)
- [Flow charts](FLOWCHART.md)
- [HTML overview](index.html)
- [README](../README.md)
