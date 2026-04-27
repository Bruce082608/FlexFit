# FlexFit Next Development Plan

本文基于 `app设计文档.pdf`、当前代码实现和最近工作区状态整理，用于指导下一阶段开发。目标是把 FlexFit 从“引体向上可演示 MVP”推进到“符合设计文档 Demo 范围的双动作 AI 健身教练 App”。

## 1. Product Target

FlexFit 的产品定位是低成本、实时化、可解释的 AI 私人健身教练 App。下一阶段应继续坚持以下边界：

- 实时训练主链路只使用端侧姿态识别和本地规则，保证低延迟。
- LLM 只用于训练结束后的个性化总结，不参与实时逐帧纠错。
- Demo 阶段重点完成 `Pull Up` 和 `Shoulder Press` 两个动作，其余动作只保留入口和 Coming soon 状态。
- App 内所有用户可见 UI 文案继续保持英文。

## 2. Current Baseline

当前项目已经具备这些基础能力：

- Android Kotlin + Jetpack Compose + CameraX + ML Kit Pose Detection。
- 统一训练容器 `TrainingScreen`，支持相机训练、视频模式、骨架叠加、计时、反馈和结算弹窗。
- 33 点姿态关键点协议 `PoseKeypoints`，与 ML Kit / MediaPipe 风格关键点对齐。
- `PullUpAnalyzer` 已有较完整的状态机、计数、错误检测、Depth / Alignment / Stability 评分和单元测试。
- `WorkoutRecordRepository` 已使用 DataStore 保存训练记录，并驱动 Progress/Profile 数据。
- 训练后 AI 分析框架已接入 DeepSeek 兼容 API，请求训练摘要并解析结构化 JSON 结果。
- 视频分析已有 `VideoFrameExtractor` 和 `VideoAnalysisController`，但仍需要真机验证、进度准确性和异常处理收口。

当前主要缺口：

- `ShoulderPressAnalyzer` 仍偏占位，只能做基础计数，缺少可解释评分、错误检测和测试。
- `WorkoutScreen` 仍直接进入默认引体向上，没有符合设计文档的完整动作选择中心。
- 旧的 `com.example.flexfit.api.*` LLM 占位层已从源码清理；发布前需持续确认没有新引用。
- 视频分析链路还需要验证抽帧、姿态检测、结果保存和失败提示的端到端体验。
- 真机质量门槛已固化到 `docs/demo-quality-gate.md`，仍需按清单逐台设备执行。

## 3. Milestone Plan

### Milestone 1: Stabilize AI Analysis and Training Result Flow

目标：确保用户运动结束后，结算页稳定显示本地规则结果，并自动加载 AI Analysis。

Implementation:

- 收敛 LLM 代码路径，只保留 `data.llm.LlmAnalysisRepository` + `data.remote.LlmApiService` 作为训练后分析入口。
- 保持旧的 `com.example.flexfit.api.LlmApiService` 和 `RetrofitClient` 不再被源码引用，避免后续误用。
- 为 AI 分析增加最小单元测试：DeepSeek JSON 正常解析、Markdown fenced JSON 容错、空响应 fallback、本地 fallback 文案不为空。
- 保存训练记录时保留 AI 分析结果；用户先保存、后请求 AI 分析时，也应能更新对应记录。
- 结果弹窗中保持四种状态：Loading、Success、Error with local fallback、Retry。

Acceptance criteria:

- 结束训练后自动触发 AI 分析。
- 无网络、API key 缺失、DeepSeek 返回异常时，结算页仍显示本地分析，不崩溃。
- `./gradlew.bat test` 和 `./gradlew.bat assembleDebug` 通过。

### Milestone 2: Build the Exercise Selection Center

目标：让 Workout 页面成为设计文档要求的动作选择模块，展示 10 个动作，并区分可训练动作和占位动作。

Implementation:

- 将 `WorkoutScreen` 从“直接打开默认引体向上训练”改为动作列表页。
- 使用 `ExerciseType` 作为单一动作源，展示：
  - Pull Up
  - Shoulder Press
  - Bench Press
  - Seated Row
  - Biceps Curl
  - Squat
  - 45° Leg Press
  - Lat Pull Down
  - Triceps Push Down
  - Push Up
- 仅 `Pull Up` 和 `Shoulder Press` 提供可点击训练入口，其余动作显示 `Coming soon`。
- Pull Up 保留宽握、标准、窄握选择；Shoulder Press 直接进入训练模式选择。
- 相机 / 视频两个训练模式入口保持一致，最终都进入 `TrainingScreen`。

Acceptance criteria:

- Home、Workout、Pull-up Select 入口不会互相割裂。
- 选择 Pull Up 可进入握距选择，再进入相机或视频训练。
- 选择 Shoulder Press 可进入相机或视频训练。
- 未实现动作不能进入训练页，但有明确 Coming soon 状态。

### Milestone 3: Complete Shoulder Press MVP

目标：让坐姿哑铃肩推达到与引体向上同等级的 Demo 闭环。

Implementation:

- 完善 `ShoulderPressAnalyzer` 状态机：
  - Preparing
  - Rack Position
  - Pressing
  - Lockout
  - Return to Rack
- 计算关键指标：
  - 左右肘角度
  - 手腕相对肩部和鼻子的高度
  - 左右手同步性
  - 肩部水平稳定性
  - 躯干后仰或左右偏移
- 实现错误类型：
  - `range_of_motion`: 没有完整下放或没有推到锁定位置
  - `left_right_imbalance`: 左右手高度或肘角差异过大
  - `trunk_lean`: 躯干后仰或侧偏明显
  - `shoulder_instability`: 肩部高度波动过大
  - `pose_incomplete`: 关键点不足
- 输出 `ExerciseScoreBreakdown`：
  - Depth: 下放和推起幅度
  - Alignment: 左右对称、肘腕肩对齐
  - Stability: 躯干和肩部稳定
- 增加 `MockPoseSequence.shoulderPressFrame` 的完整 rep 序列。
- 新增 `ShoulderPressAnalyzerTest` 覆盖准备、计数、幅度不足、左右不平衡、躯干后仰、短数组不崩溃。

Acceptance criteria:

- 正确肩推动作可以计数。
- 明显错误动作不计数或产生 warning/error。
- 结算页显示真实评分、错误次数、主要问题和 AI 分析。
- 肩推与引体向上共用同一训练页、骨架层、记录保存和 AI 分析。

### Milestone 4: Harden Video Analysis

目标：把上传视频分析从“可运行链路”打磨为可演示功能。

Implementation:

- 确认 `VideoFrameExtractor` 抽帧频率，默认使用 5 fps，后续可配置为 Fast / Balanced / Accurate。
- 修正视频分析进度，使用视频 duration 和 frame interval 估算 total frames，不显示负数或不确定状态。
- 在视频分析中复用同一套 `PoseDetectorWrapper` 和 `ExerciseAnalyzer`，确保结果与实时相机一致。
- 处理异常：
  - 无视频权限
  - 选中文件无法打开
  - 视频无有效时长
  - ML Kit 未检测到人体
  - 分析中取消或返回
- 视频分析结束后进入同一结算弹窗，并保存到训练历史。

Acceptance criteria:

- 选择本地视频后能预览。
- 点击分析后显示进度、当前时间和分析状态。
- 分析完成后展示计数、评分、错误建议和 AI Analysis。
- 失败时有英文错误提示，且可重新选择视频。

### Milestone 5: Visual Feedback and Explainability Polish

目标：让设计文档中的“可解释”在 UI 上更直观。

Implementation:

- `PoseOverlay` 使用统一连接表和关键点置信度。
- 根据当前 `ExerciseAnalysisResult` 高亮：
  - Green: correct / success
  - Yellow: warning
  - Red: error
- 在训练底部面板展示当前 phase、rep count、accuracy、Depth、Alignment、Stability。
- 在结果页将本地规则分析和 AI Analysis 区分显示：
  - Local Scores: 本地实时规则得分
  - AI Analysis: 训练后个性化建议
- 对主要错误展示对应建议，避免只显示泛泛提示。

Acceptance criteria:

- 用户能从颜色和文字理解当前动作哪里出了问题。
- 结果页能解释分数来源，而不是只给总分。
- 训练和结果 UI 所有可见文案为英文。

### Milestone 6: Quality Gate and Demo Readiness

目标：固定演示前质量标准，减少真机现场风险。

Implementation:

- 建立手动测试清单：
  - 首次启动权限授权
  - 权限拒绝后重试
  - 前后摄像头切换
  - Pull Up 相机训练
  - Pull Up 视频分析
  - Shoulder Press 相机训练
  - Shoulder Press 视频分析
  - DeepSeek 成功返回
  - DeepSeek 失败 fallback
  - 训练历史保存、筛选和清空
- 整理 build 验证命令到 README：
  - `.\gradlew.bat test`
  - `.\gradlew.bat assembleDebug`
- 清理发布前风险：
  - API key 只保存在 `local.properties`
  - 不提交本地路径和真实密钥
  - 旧占位 API 层不再被引用
  - App icon、app name、权限说明完整

Acceptance criteria:

- Debug APK 可稳定安装运行。
- Demo 路径无需联网也能完成本地规则分析。
- 联网且 DeepSeek key 有效时，结算页能显示 AI Analysis。

## 4. Recommended Immediate Task Order

1. 收敛 LLM 代码路径，补 AI 分析解析和 fallback 测试。
2. 将 `WorkoutScreen` 改造成完整动作选择中心。
3. 完成 Shoulder Press MVP，包括规则、评分、错误检测和测试。
4. 打磨视频分析进度、错误提示和结果保存。
5. 优化骨架高亮和结果页可解释性。
6. 跑完整真机 Demo checklist，最后再做图标、权限说明和发布准备。

## 5. Test Strategy

Unit tests:

- `PullUpAnalyzerTest`: 保持现有覆盖，防止引体向上回归。
- `ShoulderPressAnalyzerTest`: 新增肩推准备、计数、错误和评分测试。
- `WorkoutRecordRepositoryTest`: 覆盖 LLM 分析数据序列化与更新。
- `LlmAnalysisRepository` 或可抽出的 parser tests: 覆盖 DeepSeek JSON 解析和 fallback。

Build checks:

```powershell
$env:JAVA_HOME='<Android Studio JBR path>'
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

Manual checks:

- 真机摄像头链路必须手测，因为 JVM 单元测试无法覆盖 CameraX、ML Kit 和权限弹窗。
- 视频分析必须至少使用一段 Pull Up 视频和一段 Shoulder Press 视频验证。
- DeepSeek 请求应使用测试训练结果触发，避免在逐帧分析中消耗 token。

## 6. Non-goals for the Next Stage

- 不扩展第三个真实动作。Bench Press、Row、Curl、Squat、Leg Press、Lat Pull Down、Triceps Push Down、Push Up 只保留入口。
- 不把 LLM 放入实时纠错链路。
- 不重做整体视觉风格，除非影响动作识别反馈和结果可解释性。
- 不引入复杂后端；Demo 阶段继续使用本地 DataStore + LLM API 即可。
