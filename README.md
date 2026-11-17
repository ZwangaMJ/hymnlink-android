# HymnLink

HymnLink is an Android app for coordinating church music and service rosters. It lets approved members view assignments, submit appointments for approval, and stay up to date with reminders.

## Features

- Email and password sign-in with administrator approval for new accounts
- Roster management for conductors, organists, choir members, ministers, and leaders
- Appointment scheduling, approval, urgency levels, and a monthly calendar
- Task tracking and completion status
- Appointment reminders and Firebase Cloud Messaging support
- CSV and PDF roster exports, plus sharing
- Light and dark appearance settings

## Tech stack

- Kotlin and Android Views, with Jetpack Compose support
- Firebase Authentication, Cloud Firestore, and Firebase Cloud Messaging
- Hilt dependency injection and Kotlin coroutines/Flow
- Gradle wrapper

## Requirements

- Android Studio with Android SDK 36 installed
- JDK 17 or newer
- A Firebase project configured for the app

## Firebase setup

Firebase configuration is intentionally excluded from this repository. To run the app:

1. Create or select a Firebase project and register an Android app with package name `com.example.divineroster`.
2. Enable Email/Password sign-in and create a Cloud Firestore database. Configure Firebase Cloud Messaging if push notifications are needed.
3. Download that app's `google-services.json` and place it in the `app/` directory.
4. Publish the rules in `firestore.rules` to the project's Firestore database. You can use the Firebase console or the Firebase CLI:

   ```shell
   firebase deploy --only firestore:rules
   ```

The app also uses Android alarm reminders. Exact-alarm availability depends on the device's Android version and user settings.

## Build and test

On Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
```

On macOS or Linux:

```shell
./gradlew assembleDebug
./gradlew test
```
