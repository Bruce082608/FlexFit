# FlexFit

FlexFit is an Android fitness coaching demo built with Kotlin, Jetpack Compose, CameraX, and ML Kit Pose Detection.

## Open In Android Studio

Open the repository root:

```bash
/Users/fairchan/Desktop/1/FlexFit
```

Do not open only the `app/` folder. Android Studio should import the whole Gradle project.

## Local Environment

This project requires an Android SDK with API 36 installed.

Use one of these local setup options:

1. Create a local `local.properties` file at the repository root:

```properties
sdk.dir=/path/to/Android/sdk
```

On macOS this is commonly:

```properties
sdk.dir=/Users/<your-user>/Library/Android/sdk
```

2. Or export `ANDROID_HOME` in your shell:

```bash
export ANDROID_HOME=/path/to/Android/sdk
```

`local.properties` is machine-specific and must not be committed. It is already ignored by `.gitignore`.

## Build Checks

Minimum checks before submitting changes:

```bash
./gradlew test
./gradlew assembleDebug
```

`assembleDebug` writes the debug APK under:

```bash
app/build/outputs/apk/debug/app-debug.apk
```

## Current Warning Policy

The project currently builds with deprecation warnings from Compose icons, `LocalLifecycleOwner`, `statusBarColor`, and an `ImageProxy.toBitmap` extension shadowing a platform member. These warnings are recorded for follow-up and are not blocking Phase 0.

See [docs/development.md](docs/development.md) for the detailed development baseline.

