# MoneyTrack: Flow Charts

All charts are Mermaid and render on GitHub.

## 1. App launch and authentication

```mermaid
flowchart TD
    A([Open app]) --> B{Firebase configured?}
    B -- No --> C["Show 'Accounts aren't set up'"]
    B -- Yes --> D{Signed in?}
    D -- Yes --> M([Main app: 4 tabs])
    D -- No --> E[Sign in screen]
    E --> F{Choose}
    F -- Sign in --> G[Validate email + password present]
    F -- Create account --> H["Validate email format<br/>password 8+ chars, letter + digit"]
    F -- Forgot password --> R[Validate email]
    G --> I[Firebase signIn]
    H --> J[Firebase createUser]
    R --> S[Firebase sendPasswordReset]
    H -- invalid --> X[Show validation message]
    R -- invalid --> X
    I --> K{Result}
    J --> K
    K -- OK --> M
    K -- Error --> L["Map to AuthError<br/>show friendly message"]
    S --> T["Show 'If an account exists, a reset link is on its way'"]
    L --> E
    X --> E
    T --> E
```

## 2. Add or edit a transaction

```mermaid
flowchart TD
    A([Transactions tab]) --> B{Action}
    B -- Tap + --> C[Open editor, empty]
    B -- Tap a row --> D[Open editor with row]
    C --> E[Enter type, amount, category, date, note]
    D --> E
    E --> F{Valid amount?}
    F -- No --> E
    F -- Yes --> G["MoneyViewModel.save → dao.upsert"]
    G --> H[(Room database)]
    H -- Flow emits --> I[List, budgets and reports refresh]
    D --> J[Delete] --> K["dao.delete"] --> H
```

## 3. Budgets and reports

```mermaid
flowchart LR
    T[(transactions)] --> V[MoneyViewModel]
    Bd[(budgets)] --> V
    V --> M{Selected month}
    M --> BP["Budgets tab:<br/>spent / limit per category,<br/>progress bar"]
    M --> RC["Reports: spending by category"]
    V --> R6["Reports: last 6 months<br/>income vs expenses"]
    S["Set limit"] --> W{"Limit > 0?"}
    W -- Yes --> U[upsertBudget]
    W -- No --> D[deleteBudget]
    U --> Bd
    D --> Bd
```

## 4. CSV backup and restore

```mermaid
flowchart TD
    A([Settings tab]) --> B{Choose}
    B -- Export --> C[Pick save location]
    C --> D["Csv.export(all transactions)"]
    D --> E[Write file] --> F["'Exported successfully'"]
    B -- Restore --> G[Pick CSV file]
    G --> H["Csv.parse"]
    H --> I{Valid?}
    I -- "No: bad header, row, amount or date" --> J["'Import failed: reason'<br/>data unchanged"]
    I -- Yes --> K["dao.replaceAll<br/>(clear + insert)"]
    K --> L["'Restored N transactions'"]
```

## 5. Sign out and delete account

```mermaid
flowchart TD
    A([Settings tab]) --> B{Choose}
    B -- Sign out --> C[Firebase signOut] --> D[Back to sign in screen]
    B -- Delete account --> E[Confirm and enter password]
    E --> F[Firebase re-authenticate + delete user]
    F --> G{Result}
    G -- OK --> D
    G -- "RequiresRecentLogin / bad password / network" --> H[Show error, account kept]
```

## 6. Release flow

```mermaid
flowchart TD
    A[Tests pass on main, tree clean] --> B["Bump versionCode + versionName"]
    B --> C["bundleRelease + assembleRelease"]
    C --> D{"google-services.json<br/>and keystore present?"}
    D -- No --> E["Build fails / unsigned: do not publish"]
    D -- Yes --> F[Tag vX.Y.Z]
    F --> G["GitHub release: APK + SHA-256"]
    F --> H["Play Console: upload AAB<br/>internal / closed track first"]
    H --> I["Check Data safety, privacy URL,<br/>account-deletion URL, test account"]
```
