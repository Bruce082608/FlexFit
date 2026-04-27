# FlexFit

FlexFit is an Android fitness coaching demo built with Kotlin, Jetpack Compose, CameraX, and ML Kit Pose Detection.

## Open In Android Studio

Open the repository root in Android Studio. Do not open only the `app/` folder;
Android Studio should import the whole Gradle project.

## Local Environment

This project requires an Android SDK with API 36 installed.

Use one of these local setup options:

1. Create a local `local.properties` file at the repository root:

```properties
sdk.dir=/path/to/Android/sdk
```

Example paths:

```properties
sdk.dir=C\:\\path\\to\\Android\\Sdk
# or
sdk.dir=/Users/<your-user>/Library/Android/sdk
```

2. Or export `ANDROID_HOME` in your shell:

```bash
export ANDROID_HOME=/path/to/Android/sdk
```

`local.properties` is machine-specific and must not be committed. It is already ignored by `.gitignore`.

Optional DeepSeek-compatible post-workout AI analysis is configured from the same
local file:

```properties
LLM_API_KEY=your_deepseek_api_key_here
LLM_BASE_URL=https://api.deepseek.com
LLM_MODEL=deepseek-chat
```

Use `local.properties.example` as the template. Never commit real API keys.

## Build Checks

Minimum checks before submitting changes:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

`assembleDebug` writes the debug APK under:

```bash
app/build/outputs/apk/debug/app-debug.apk
```

## Demo Quality Gate

Before a live demo, run the build checks above and complete the manual checklist
in [docs/demo-quality-gate.md](docs/demo-quality-gate.md). The demo must still
complete local rule-based analysis without network access; DeepSeek is only used
for post-workout AI Analysis when a valid local key and network are available.

## Current Warning Policy

The project currently builds with deprecation warnings from Compose icons, `LocalLifecycleOwner`, `statusBarColor`, and an `ImageProxy.toBitmap` extension shadowing a platform member. These warnings are recorded for follow-up and are not blocking Phase 0.

See [docs/development.md](docs/development.md) for the detailed development baseline.

