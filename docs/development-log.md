# FlexFit Development Log

Updated: 2026-04-27

This log records the current FlexFit demo app status, the main implementation
milestones, and the quality checks used before publishing the local project to
GitHub.

## Product Scope

FlexFit is an Android fitness coaching demo built with Kotlin, Jetpack Compose,
CameraX, ML Kit Pose Detection, DataStore, Retrofit, and Media3. The demo scope
focuses on two implemented exercises:

- Pull Up
- Shoulder Press

Other exercise entries remain visible as `Coming soon` so the app can present
the intended product direction without exposing unfinished training flows.

The app keeps real-time coaching local. Camera and video frames are analyzed on
device with pose detection and rule-based exercise analyzers. DeepSeek-compatible
LLM analysis is only used after a workout result is available.

## Implemented Features

### App Shell And Navigation

- Bottom navigation for Home, Workout, Progress, and Profile.
- Home shortcuts into the supported demo exercises.
- Workout exercise selection center with implemented and coming-soon states.
- Dedicated Pull Up grip selection before entering camera or video training.
- Shared training surface for camera and video modes.

### Pose And Exercise Analysis

- Unified 33-keypoint pose protocol aligned with ML Kit / MediaPipe landmark
  ordering.
- Shared `ExerciseAnalyzer` contract for supported exercises.
- Pull Up analyzer with state transitions, rep counting, local scoring, issue
  detection, feedback messages, and tests.
- Shoulder Press analyzer with state transitions, rep counting, local scoring,
  issue detection, feedback messages, and tests.
- Score breakdown for Depth, Alignment, and Stability.
- Local issues and improvement suggestions surfaced in the result dialog.

### Camera Training

- CameraX preview with ML Kit pose detection.
- Runtime camera permission request, rationale, and retry flow.
- Front/back camera switching.
- Pose skeleton overlay with landmark confidence support.
- Start, pause, resume, and end workout controls.
- Voice feedback hooks for phase and form feedback.

### Video Analysis

- Local video selection flow.
- Media3 preview player.
- Video frame extraction at a controlled sampling rate.
- Reuse of the same pose detector and exercise analyzer pipeline used by camera
  training.
- Progress reporting, current timestamp display, and failure messages.
- Result dialog and history saving after video analysis completes.

### Results, AI Analysis, And Fallback

- Unified result dialog with workout stats, local rule-based score breakdown,
  rule-based issue summaries, and AI Analysis.
- DeepSeek-compatible post-workout analysis through `data.llm` and
  `data.remote`.
- LLM config is loaded from `local.properties` through generated `BuildConfig`
  fields.
- Missing key, invalid key, network failure, empty response, or parse failure
  falls back to local analysis without blocking the demo path.
- LLM request timeouts are capped so a bad network does not stall the result page
  for too long.

### Training History And Progress

- Workout history stored locally with DataStore.
- Workout records include exercise stats and optional LLM analysis data.
- Progress page reads real saved history.
- Exercise filter chips update history and statistics.
- Clear-history confirmation removes saved workout records from the device.
- Profile metrics derive from saved records.

### Demo Readiness

- README documents local setup, DeepSeek placeholder configuration, and Windows
  build validation commands.
- `docs/demo-quality-gate.md` records the required manual demo checklist.
- App name is `FlexFit`.
- Launcher icons and adaptive icon resources are included.
- Permissions for camera, video access, and post-workout internet analysis are
  declared.
- The old placeholder `com.example.flexfit.api.*` API layer is no longer used by
  source code.

## Development Timeline

### Phase 0: Android Baseline

- Established the Android Gradle project baseline.
- Documented local SDK setup and minimum build commands.
- Added CI-oriented build expectations for `test` and `assembleDebug`.

### Phase 1: Pose Protocol Stabilization

- Converged the pose data path around a 33-keypoint protocol.
- Reduced analyzer risk from mismatched landmark indexes.
- Added analyzer tests to protect key motion and scoring behavior.

### Phase 2: Training Screen Convergence

- Moved camera, video, permission, timer, pose overlay, feedback, and result
  behavior into a shared training flow.
- Prepared the app for multiple exercise analyzers without duplicating the full
  training UI.

### Phase 3: Pull Up MVP

- Completed Pull Up state tracking, rep counting, scoring, errors, warnings, and
  result presentation.
- Supported grip-width selection before training.
- Kept local analysis available with no network dependency.

### Phase 4: Exercise Selection And Shoulder Press

- Turned Workout into a broader exercise selection center.
- Added Shoulder Press as the second implemented demo exercise.
- Added Shoulder Press analyzer logic, scoring, warnings, and tests.

### Phase 5: Video, History, And Explainability

- Added local video analysis and progress reporting.
- Persisted workout records with DataStore.
- Updated Progress/Profile surfaces to use real saved records.
- Improved result explainability with local score sections and rule-based issue
  summaries.

### Phase 6: AI Analysis And Demo Quality Gate

- Integrated DeepSeek-compatible post-workout AI analysis.
- Added local fallback behavior for missing keys, network failures, API errors,
  and parser failures.
- Removed tracked LLM placeholder config from `gradle.properties`; real keys must
  live only in ignored `local.properties`.
- Added the manual demo quality gate checklist.
- Verified the debug build and unit tests locally.

## Validation

Latest local validation:

```powershell
$env:JAVA_HOME='<Android Studio JBR path>'
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

Result:

- `test`: passed
- `assembleDebug`: passed
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Remaining Manual Demo Work

The build is ready for device installation, but the live-demo checklist still
needs to be run on a physical Android device:

- First launch permission grant.
- Permission denial and retry.
- Front/back camera switch.
- Pull Up camera and video paths.
- Shoulder Press camera and video paths.
- DeepSeek success response with a valid local key.
- DeepSeek failure fallback with no network or invalid key.
- Training history save, filter, and clear.
