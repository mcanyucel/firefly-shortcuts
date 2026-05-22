# Setup Guide

This document covers how to build the app from source and configure it against your Firefly III instance.

---

## Development Prerequisites

- Flutter SDK - the project requires Dart SDK ^3.12.0, so Flutter 3.24 or higher is recommended.
- Android SDK with API level 26 or higher (Android 8.0+)
- A running instance of Firefly III (version 6.0.0 or higher) with API access enabled.
- An IDE such as Android Studio, Visual Studio Code, or IntelliJ IDEA with Flutter and Dart plugins installed. Or you can go with a simple text editor and command line.


---

## Installing via Precompiled APK

The latest release APK can be downloaded from the GitHub releases page. I advise using a tool like [Obtainium](https://obtainium.imranr.dev/) to install any app from a repository release, as it is more reliable and can handle updates. 

---

## Building from Source

Clone the repository and fetch dependencies

```bash
git clone https://github.com/mustafacanyucel/firefly-shortcuts.git
cd firefly_shortcuts
flutter pub get
```

The project uses code generation for Riverpod providers and the Drift database schema. Run the generator before building the app:

```bash
dart run build_runner build
```

You only need to run this again if you modify and file annotated with `@riverpod` or `@DriftDatabase` / `@DriftAccessor`.

To run on a connected device or emulator:

```bash
flutter run
```

To build a release APK for Android:

```bash
flutter build apk --release
```

Or split by ABI for smaller APKs:

```bash
flutter build apk --release --split-per-abi
```

The output APK(s) will be located in `build/app/outputs/flutter-apk/`.

---