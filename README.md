# HymnLink

HymnLink is an Android app for coordinating church music and service rosters. It lets approved members view assignments, submit appointments for approval, download sheet music and audio, and stay up to date with reminders.

## Features

- Email and password sign-in with administrator approval for new accounts
- Roster management for conductors, organists, choir members, ministers, and leaders
- Appointment scheduling and approval with urgency levels, grouped by urgency on the dashboard, plus a monthly calendar with event dots
- Services record the conductor, organist and hymn numbers for each segment (Before service Choir, BS, OH, AT, Call up, Communion Hymns 1 and 2, AS)
- Meetings record an event coordinator and who must attend (pick members or type names)
- Dashboard with a word of the day (updated daily by an admin) and announcements with a level of importance
- Music tab: admins upload sheet music and audio, members download them
- Tools tab: a pitch pipe and a metronome
- Conductor reminders 6 days, 3 days and the day before an upcoming service, delivered to the appointed conductor's device
- Firebase Cloud Messaging support
- CSV and PDF roster exports, plus sharing
- Blue, grey and white theme with light and dark appearance settings

## Tech stack

- Kotlin and Android Views, with Jetpack Compose support
- Firebase Authentication, Cloud Firestore, Cloud Storage, and Firebase Cloud Messaging
- WorkManager for the background reminder refresh
- Hilt dependency injection and Kotlin coroutines/Flow
- Gradle wrapper

## Requirements

- Android Studio with Android SDK 36 installed
- JDK 17 or newer
- A Firebase project configured for the app

## Firebase setup

Firebase configuration is intentionally excluded from this repository. To run the app:

1. Create or select a Firebase project and register an Android app with package name `com.example.divineroster`.
2. Enable Email/Password sign-in, create a Cloud Firestore database, and enable Cloud Storage (needed for the Music tab; newer Firebase projects may need the Blaze plan for this). Configure Firebase Cloud Messaging if push notifications are needed.
3. Download that app's `google-services.json` and place it in the `app/` directory.
4. Publish the rules in `firestore.rules` to the project's Firestore database. You can use the Firebase console or the Firebase CLI:

   ```shell
   firebase deploy --only firestore:rules
   ```

5. Publish the rules in `storage.rules` to Cloud Storage (Storage > Rules in the console, or `firebase deploy --only storage`). Download `google-services.json` again after enabling Storage so it includes the storage bucket.
6. Sign up in the app, then set that user's `role` to `ADMIN` and `approvalStatus` to `APPROVED` in the Firestore console to create the first administrator.

The app also uses Android alarm reminders. Exact-alarm availability depends on the device's Android version and user settings; when exact alarms are not allowed the reminders still fire, but may be delayed slightly. A conductor's reminders are scheduled on their own device once the app has seen the assignment, and are refreshed in the background about every six hours.

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
