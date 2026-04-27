# FlexFit Demo Quality Gate

Use this checklist before every demo build. The demo is ready only when the build
checks pass and each manual path below has either passed on a real device or has
a documented reason for being skipped.

## Build Gate

Run from the repository root on Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

The debug APK should be created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Local Configuration Gate

- `local.properties` exists only on the developer machine and is ignored by Git.
- `LLM_API_KEY`, `LLM_BASE_URL`, and `LLM_MODEL` are read only from
  `local.properties`.
- `local.properties.example` contains placeholders only.
- No real API keys, personal SDK paths, or machine-specific absolute paths are
  committed.
- The old `com.example.flexfit.api.*` placeholder API layer is not referenced by
  app or test source code.

## App Identity And Permission Gate

- Launcher app name is `FlexFit`.
- Launcher icon and round icon render correctly on the test device.
- Camera permission is declared and the in-app camera permission message is
  understandable.
- Video access permission is declared for the Android versions that need it, and
  the in-app video permission message is understandable.
- Internet permission is declared for post-workout AI analysis only; local rules
  must work without network.

## Manual Device Checklist

Record device model, Android version, APK build time, and tester initials before
running the list.

| Check | Expected result | Status |
| --- | --- | --- |
| First launch camera permission grant | Camera permission prompt appears, granting permission opens the training preview. | Not run |
| Retry after camera permission denial | Denying permission shows an English retry/rationale screen, and Request Again can recover when permission is granted. | Not run |
| Front/back camera switch | Camera switch button toggles between front and back lenses without crashing or freezing. | Not run |
| Pull Up camera training | Pull Up starts from Workout or Pull-up Select, counts valid reps, shows local scores, and opens the result dialog. | Not run |
| Pull Up video analysis | A local Pull Up video can be selected, previewed, analyzed with progress, and saved to history. | Not run |
| Shoulder Press camera training | Shoulder Press starts from Workout, counts valid reps, shows local scores, and opens the result dialog. | Not run |
| Shoulder Press video analysis | A local Shoulder Press video can be selected, previewed, analyzed with progress, and saved to history. | Not run |
| DeepSeek success response | With network and a valid key, the result dialog shows `AI Analysis` with an AI-powered source label. | Not run |
| DeepSeek failure fallback | With no network or an invalid/missing key, the result dialog still shows local analysis and never blocks saving. | Not run |
| Training history save/filter/clear | Saved records appear on Progress, exercise filters update the list, and Clear removes all records after confirmation. | Not run |

## Demo Acceptance

- Debug APK installs and launches reliably on the demo device.
- The demo path can complete local rule-based analysis with airplane mode on.
- When network is available and the DeepSeek key is valid, the result page shows
  AI Analysis after the local scores are ready.
