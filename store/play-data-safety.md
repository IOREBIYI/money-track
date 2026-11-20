# Play Console: answers for the accounts release

Use these when you ship the version with sign in / sign up. The app now has the INTERNET permission and collects an email address, so the earlier "no data collected" answers are no longer true.

> Re-check the data types against Google's own guide for Firebase Authentication ("Firebase and Google Play data disclosure") before submitting. Google's wording and what the SDK collects can change, and you are responsible for the answers.

## App content > Data safety

**Does your app collect or share any required user data types?** Yes (collect), No (share).

| Section | Answer |
|---|---|
| Data collected: **Personal info > Email address** | Collected. Required (not optional). Purpose: **Account management**. Not shared. |
| Data collected: **Personal info > User IDs** | Collected. Required. Purpose: **Account management**. Not shared. |
| Anything else (financial info, app activity, location, photos, contacts, analytics, ads) | **Not collected.** Transactions and budgets never leave the device. |
| Data shared with third parties | **None.** Firebase (Google) acts as a service provider processing data on your behalf, which Play does not count as sharing. |
| **Is all of the user data collected by your app encrypted in transit?** | **Yes** (HTTPS/TLS). |
| **Do you provide a way for users to request that their data be deleted?** | **Yes.** |
| **Data deletion URL** | `https://github.com/IOREBIYI/money-track/blob/main/ACCOUNT_DELETION.md` |
| Data that users can choose to delete | Email address and user ID, via Settings > Delete my account. |

## Other declarations that change
- **Privacy policy URL:** `https://github.com/IOREBIYI/money-track/blob/main/PRIVACY.md` (updated for accounts).
- **App access:** because the app now needs an account, Play reviewers must be able to get in. Under **App access**, choose "All or some functionality is restricted" and give a **test account** (an email and password you create in your Firebase project for review). Keep it working.
- **Ads:** still "No".
- **Target audience:** keep **18 and over**.
- **Content rating:** answer the questionnaire again. It should still come out "Everyone", and the app now allows account creation but no user-to-user content.

## Store listing text that must change
These claims are no longer true:
- "No account" / "no sign-up" / "needs no internet" in the full description and short description.
- "no internet permission" in the description and in the in-app Privacy text (already updated in the app).

Suggested replacement for the privacy paragraph:

```
YOUR DATA STAYS YOURS
• Your transactions and budgets are stored only on your phone and are never uploaded
• Sign in with your email to protect your ledger; forgot your password? Get a reset link by email
• No ads, no tracking. Delete your account any time from Settings
• Export all your transactions to a CSV file for backup
```

Short description (80 characters max), for example:
`Track spending, set budgets and see where your money goes. Private by design.`

## Graphics that need a refresh
- **Feature graphic:** the "No account" badge is now wrong. Replace it (for example with "Your data stays on your phone").
- **Screenshots:** consider adding the new sign-in screen, and retake any that mention accounts or privacy.
