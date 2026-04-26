# Development Baseline

This document records the Phase 0 engineering baseline for FlexFit.

## Goals

- Every developer can open, sync, build, and run the app from a clean checkout.
- Local machine paths stay out of version control.
- CI runs the minimum build checks: `test` and `assembleDebug`.
- Existing deprecation warnings are recorded, not treated as blockers yet.

## Required Local Setup

Install Android Studio and an Android SDK that includes API 36.

Then use either `local.properties`:

```properties
sdk.dir=/Users/<your-user>/Library/Android/sdk
```

or `ANDROID_HOME`:

```bash
export ANDROID_HOME=/Users/<your-user>/Library/Android/sdk
```

Do not commit `local.properties`. It is intentionally ignored because it contains local machine configuration.

## Required Commands

Run these before handing off changes:

```bash
./gradlew test
./gradlew assembleDebug
```

Expected results:

- `./gradlew test` completes successfully.
- `./gradlew assembleDebug` creates `app/build/outputs/apk/debug/app-debug.apk`.

## Gradle Wrapper

The Gradle wrapper script must be executable on Unix-like systems:

```bash
chmod +x gradlew
```

The executable bit should be preserved in Git as `100755`.

## CI

The repository includes `.github/workflows/android.yml`.

The workflow:

- Uses JDK 17.
- Installs Android SDK platform 36.
- Runs `./gradlew test`.
- Runs `./gradlew assembleDebug`.

## Known Non-Blocking Warnings

Current builds emit deprecation warnings in these areas:

- `Icons.Filled.ShowChart`, `Icons.Outlined.ShowChart`, and `Icons.Filled.TrendingUp` should eventually use AutoMirrored variants.
- `LocalLifecycleOwner` imports should eventually move to `androidx.lifecycle.compose`.
- `window.statusBarColor` is deprecated on newer Android versions.
- `ImageProxy.toBitmap()` extension in `PullUpCameraScreen.kt` is shadowed by a platform member.

These warnings do not block Phase 0. They should be cleaned up in a later compatibility/refactor pass.

