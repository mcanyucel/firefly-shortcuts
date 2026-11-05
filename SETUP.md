# Setup guide

How to build Firefly Shortcuts from source. To install a ready-made APK instead, see [Installation](README.md#installation) in the README. For connecting the app to Firefly III, see [First-time setup](README.md#first-time-setup).

## Prerequisites

- A recent Android Studio, or JDK 17 or newer with the Android SDK command-line tools. Android Studio's bundled JDK works for command-line builds too.
- Android SDK platform 37 (the project's `compileSdk`). Gradle downloads it if it's missing and the SDK licenses are accepted.
- An Android 8.0 (API level 26) or newer device or emulator.
- A Firefly III server to test against (6.x).

Gradle itself comes with the project through the Gradle wrapper (`gradlew`).

## Get the code

```bash
git clone https://github.com/mcanyucel/firefly-shortcuts.git
cd firefly-shortcuts
```

Open the folder in Android Studio, or use the command line as below. On Windows, use `gradlew.bat` instead of `./gradlew`.

If you build outside Android Studio, point `JAVA_HOME` at a JDK 17+ and tell Gradle where the Android SDK is, either with the `ANDROID_HOME` environment variable or with a `local.properties` file in the project root:

```properties
sdk.dir=/path/to/Android/Sdk
```

## Debug builds

```bash
./gradlew installDebug
```

This builds and installs a debug build on the connected device. Debug builds use the application ID `com.mustafacanyucel.firefly_shortcuts.debug` and the version name suffix `-debug`, so they install next to the released app instead of replacing it.

## Tests

```bash
./gradlew test
```

## Release builds

Release builds are minified with R8 and signed with the release key. The signing details are read from a `key.properties` file in the project root, which is ignored by git:

```properties
storeFile=/path/to/release-keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

A relative `storeFile` path is resolved from the `app` folder. Then build:

```bash
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release.apk`. There is one universal APK; the app has no native libraries, so there are no per-ABI builds.

Without `key.properties` the release build can't be signed. To build a release APK for yourself, create your own keystore (Android Studio: **Build → Generate Signed App Bundle or APK**) and point `key.properties` at it. An APK signed with a different key can't be installed over the published app; uninstall that first.

To check which certificate an APK is signed with:

```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

`apksigner` is in the Android SDK's `build-tools/<version>` folder.

## Project layout

All code is in the `app` module, under `app/src/main/java/com/mustafacanyucel/firefly_shortcuts`:

- `auth/`: sign-in with a personal access token or OAuth, token refresh, and the guard that clears local data when the server or user changes.
- `data/api/`: the Firefly III API client and its request and response types.
- `data/db/`: the Room database. Schemas are exported to `app/schemas`; commit the new schema file whenever the database version changes.
- `data/settings/`, `data/secrets/`: settings in DataStore, and tokens encrypted with an Android Keystore key.
- `sync/`: downloads accounts, categories, budgets, bills, piggy banks and tags.
- `run/`: creates a shortcut's transaction in Firefly III.
- `ui/`: the Compose screens (home, editor, sync, settings).
- `widget/`: draws the home screen widget and runs widget taps in the background with WorkManager.
- `upgrade/`: deletes the data left behind by the Flutter app (1.0.x) after an update.

`ShortcutWidgetProvider` stays in the root package on purpose: widgets placed on the home screen are bound to its class name, so moving it would break widgets placed with an earlier version.

The manual dependency container is `AppContainer`, created by the `FireflyApp` application class. Activities, workers and the widget reach it through `context.appContainer`.
