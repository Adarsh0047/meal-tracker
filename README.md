# Meal Tracker

A responsive meal-tracking application with a browser client and a native Android client. Both clients use the same Firebase Firestore data, so meal entries and monthly costs stay synchronized across devices.

**Web app:** [adarsh0047.github.io/meal-tracker](https://adarsh0047.github.io/meal-tracker/)

## Features

- Monthly calendar for breakfast, lunch, and dinner attendance
- Real-time Firestore synchronization with offline caching
- Monthly meal counts and cost calculations
- Separate breakfast, lunch, dinner, and delivery prices for every month
- Cost history with category-level visual breakdowns
- Modern, responsive dark interface
- Firestore diagnostics that can be copied and shared for troubleshooting
- Native weekly reminders with multiple days and multiple times per day

## Native Android app

The native client is in [`android/`](android/) and is written in Kotlin with Jetpack Compose. It supports Android 6.0 (API 23) and newer devices.

The app provides five sections:

- **Today:** browse months, mark meals, and see the selected month's summary
- **Reminders:** schedule independent weekly notifications for one or more days and times
- **Costs:** save the selected month's meal prices and delivery cost
- **History:** review monthly totals and category breakdowns
- **Logs:** inspect Firestore snapshots, writes, cache/server sources, and connection errors

Reminder schedules are stored locally on the phone. Android's system scheduler wakes the app's own broadcast receiver, which posts a standard Meal Tracker notification; it does not open or depend on the Clock/Alarm application. Reminders are restored after a device restart or app update. Android 13 and newer requires notification permission, while Android 12 and newer may request the **Alarms & reminders** permission for exact timing. Manufacturer battery-saving settings can still delay background delivery.

## Build the Android app without Android Studio

Requirements:

- JDK 17 or 21
- Android SDK with API 36 and Build Tools installed
- `ANDROID_HOME` pointing to the Android SDK

From the repository root:

```bash
cd android
./gradlew test assembleDebug
```

The installable debug APK is generated at:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

To build an optimized release:

```bash
cd android
./gradlew assembleRelease
```

Release signing credentials are intentionally not stored in this repository. Supply these values as environment variables or Gradle properties to produce a signed release APK:

- `MEAL_TRACKER_KEYSTORE_PATH`
- `MEAL_TRACKER_KEYSTORE_PASSWORD`
- `MEAL_TRACKER_KEY_ALIAS`
- `MEAL_TRACKER_KEY_PASSWORD`

Without those values, Gradle produces an unsigned release APK. Keep the original keystore safe and reuse it for every update; Android will reject an update signed with a different certificate.

## Install or share an APK

1. Transfer the signed APK to the phone using USB, cloud storage, email, or a messaging service.
2. If the service blocks APK attachments, place the APK inside a ZIP file, send it, and extract it on the phone.
3. Open the APK on the phone and allow installation from that source when Android prompts.
4. Install it. To update an existing installation without removing its data, the package name and signing certificate must match the installed version.

Only the APK is required for installation. Source files, the keystore, and `google-services.json` must not be shared with end users.

## Firebase configuration

The Android client currently initializes Firebase from the same public client configuration as the web app. Firebase client API keys identify the project but are not authorization credentials. Access must be protected with Firestore Security Rules and, for a multi-user application, Firebase Authentication.

For a conventional production setup, register the Android package `io.github.adarsh0047.mealtracker` in Firebase, place the downloaded `google-services.json` in `android/app/`, and integrate the Google Services Gradle plugin. That file is ignored by Git in this repository.

### Firestore rules

The clients read two documents. Both must be permitted or the Costs listener will report `PERMISSION_DENIED`:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /trackers/my_secret_meal_data_9876 {
      allow read, write: if true;
    }

    match /trackers/my_secret_meal_costs_9876 {
      allow read, write: if true;
    }

    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

These example rules allow anyone who can reach the project to read and change both documents. They match the current shared, unauthenticated app but are not suitable for private or multi-user data. Document IDs are not secrets or an access-control mechanism; use Firebase Authentication and user-scoped rules when privacy is required.

## Data model

Data is stored in the `trackers` collection:

| Document | Map key | Stored value |
| --- | --- | --- |
| `my_secret_meal_data_9876` | `YYYY-MM-DD` | `{ breakfast, lunch, dinner }` boolean values |
| `my_secret_meal_costs_9876` | `YYYY-MM` | `{ breakfast, lunch, dinner, delivery }` numeric values |

Delivery is a flat cost for the selected month. Monthly totals combine completed meals at that month's saved prices with its delivery cost.

## Run the web app locally

The browser client is a static site with no build step:

```bash
python3 -m http.server 8000
```

Open `http://localhost:8000`. Pushes to `main` publish the browser client through GitHub Pages.

## Repository safety

The repository intentionally excludes:

- Signing keystores and credentials
- `android/app/google-services.json`
- `android/local.properties`
- Gradle caches and build outputs
- Exported APK and ZIP packages

Do not commit diagnostic reports before checking them for device or project information you do not want to publish.
