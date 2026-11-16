# MoneyTrack

A simple, private personal finance tracker for Android. Built with Kotlin, Jetpack Compose and Room.

## Features
- Track income and expenses with category, date and note
- Monthly budgets per category with progress bars
- Reports: spending by category and a 6-month income vs. expenses chart
- CSV export and restore (backup / move to a new phone)
- 100% offline: no account, no ads, no network permission. See [PRIVACY.md](PRIVACY.md)

## Build
Requires Android Studio (or JDK 17+ and the Android SDK 36).

```
./gradlew assembleDebug      # debug APK -> app/build/outputs/apk/debug
./gradlew bundleRelease      # Play Store bundle (.aab)
```

### Release signing
Create `keystore.properties` in the project root (git-ignored):

```
storeFile=keystore/upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```
Without it, the release build is unsigned. **Never commit your keystore or passwords.**

## License
MIT
