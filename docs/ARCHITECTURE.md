# MoneyTrack: Architecture

MoneyTrack is a single-module Android app using **MVVM**. The UI is Jetpack Compose, state is exposed from `ViewModel`s as Kotlin `Flow`/`StateFlow`, and data is stored locally in Room. Firebase Auth is the only external service.

![MoneyTrack architecture diagram](architecture.svg)

The same diagram as Mermaid (renders on GitHub):

```mermaid
flowchart TB
    User([User])

    subgraph Device["Android device"]
        direction TB
        Entry["MoneyApp + MainActivity<br/>(MoneyTheme, AuthGate)"]

        subgraph UI["UI layer: Jetpack Compose (ui/)"]
            AuthScreens["AuthGate / AuthScreens"]
            AppRoot["AppRoot: 4 tabs + FAB"]
            Tx["TransactionsScreen + Editor"]
            Bud["BudgetsScreen"]
            Rep["ReportsScreen"]
            Set["SettingsScreen"]
        end

        subgraph State["State layer: ViewModels"]
            MVM["MoneyViewModel<br/>transactions, budgets, month, currency, CSV"]
            AVM["AuthViewModel<br/>AuthUiState"]
        end

        subgraph Data["Data layer (data/)"]
            Dao["MoneyDao (Room)"]
            Csv["Csv.kt"]
            Cur["Currencies.kt"]
            Prefs[("SharedPreferences<br/>currency")]
        end

        subgraph Auth["Auth layer (auth/)"]
            Repo["AuthRepository"]
            FRepo["FirebaseAuthRepository"]
            URepo["UnconfiguredAuthRepository"]
            Val["AuthValidation + error mapping"]
        end

        DB[("SQLite: transactions, budgets<br/>never leaves the device")]
    end

    Firebase[["Firebase Auth<br/>email + user ID"]]

    User --> Entry --> AuthScreens
    AuthScreens -->|signed in| AppRoot
    AppRoot --> Tx & Bud & Rep & Set
    Tx & Bud & Rep & Set --> MVM
    AuthScreens & Set --> AVM
    MVM --> Dao --> DB
    MVM --> Csv
    MVM --> Cur
    MVM --> Prefs
    AVM --> Val
    AVM --> Repo
    Repo --- FRepo
    Repo --- URepo
    FRepo -. HTTPS .-> Firebase
```

## Layers

| Layer | Package / files | Responsibility |
|---|---|---|
| Entry | `MoneyApp.kt`, `MainActivity.kt` | Initialises Firebase (if configured), hosts Compose, wraps content in `AuthGate`. |
| UI | `ui/` | Stateless-ish composables. `AppRoot` owns tab selection and the editor dialog; screens receive data and callbacks. |
| State | `MoneyViewModel.kt`, `auth/AuthViewModel.kt` | Expose `StateFlow`s (`WhileSubscribed(5_000)`), launch coroutines for writes. |
| Data | `data/` | `Database.kt` (entities, DAO, `AppDatabase`), `Csv.kt` (export/parse), `Currencies.kt` (39 currencies). |
| Auth | `auth/` | `AuthRepository` interface with Firebase and "unconfigured" implementations, validation and error mapping that do not depend on Firebase types. |

## Key design decisions

- **Auth gate.** `MainActivity` renders `AuthGate(auth) { AppRoot(vm, auth) }`. The main app only composes once the user is signed in.
- **Swappable auth backend.** `AuthBackend.create` returns `FirebaseAuthRepository` when Firebase is configured, otherwise `UnconfiguredAuthRepository`, whose calls return `AuthError.NotConfigured`. Debug builds run without any Firebase config.
- **Auth logic is plain Kotlin.** `AuthValidation`, `AuthError` and `mapAuthException` are unit-testable without Android.
- **Reactive reads.** The DAO returns `Flow<List<...>>`; the ViewModel turns them into `StateFlow`s and the UI collects them with `collectAsStateWithLifecycle`.
- **Money as cents.** Amounts are `Long` cents; formatting uses the selected currency (display only, no conversion).
- **Atomic restore.** `MoneyDao.replaceAll` clears and re-inserts transactions in one DAO operation.
- **Local-only data.** No network code touches transactions or budgets. This is a product invariant (see [PRIVACY.md](../PRIVACY.md)).

## Build and delivery

```mermaid
flowchart LR
    Dev[Developer] --> PR[Pull request]
    PR --> CI["GitHub Actions: pr-checks"]
    CI --> U[Unit tests]
    CI --> L[Lint]
    CI --> B[Debug build]
    CI --> I["Emulator tests + Firebase Auth emulator"]
    PR -->|merge| Main[main]
    Main --> Rel["Local release build<br/>bundleRelease / assembleRelease"]
    Rel --> AAB[AAB to Google Play]
    Rel --> APK[Signed APK to GitHub Releases]
```

Release builds need `app/google-services.json` and signing config (`keystore.properties`); both are git-ignored and never held by CI.

## Repository map

```
app/src/main/java/com/moneytrack/app/   application code (see layers above)
app/src/test/                           JUnit 4 unit tests
app/src/androidTest/                    Room DAO and Firebase Auth emulator tests
app/src/debug/                          debug manifest + network config for the emulator
site/                                   static website (landing, privacy, account deletion)
store/                                  Play assets and Data safety answers
docs/                                   this documentation
.github/workflows/pr-checks.yml         CI
```
