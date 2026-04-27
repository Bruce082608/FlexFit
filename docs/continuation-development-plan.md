# FlexFit 后续开发流程

本文基于以下材料整理：

- GitHub 仓库：`https://github.com/Bruce082608/FlexFit`
- 设计文档：`app设计文档.pdf`，共 6 页，创建时间为 2026-04-25 20:29:42 +08:00
- 当前代码基线：`af1931b Initial commit: FlexFit fitness tracking app`

## 1. 产品目标复述

FlexFit 的目标是做一个低成本、实时化、可解释的 AI 私人健身教练 App。Demo 阶段应优先完成：

- 通过手机摄像头实时识别人体关键点。
- 在画面上叠加关键点和骨架。
- 基于生物力学规则判断动作是否标准。
- 对错误动作给出即时文本或语音反馈。
- 训练结束后给出评分、总结和改进建议。
- 预留 LLM API，用于训练结束后的个性化分析。
- Demo 动作重点为引体向上和坐姿哑铃肩推，其余动作先保留入口或占位。

设计文档列出的动作列表与当前 `ExerciseType` 基本一致：引体向上、坐姿哑铃肩推、平板卧推、划船、弹力带肱二头肌弯举、深蹲、45 度腿举、高位下拉、绳索下压、俯卧撑。

## 2. 当前代码状态

### 2.1 工程与依赖

- Android 工程，Kotlin + Jetpack Compose。
- `compileSdk`/`targetSdk` 为 36，`minSdk` 为 24。
- 已接入 CameraX、Accompanist Permissions、DataStore、Retrofit、OkHttp、Gson、ML Kit Pose Detection。
- 当前使用的是 ML Kit Pose，不是设计文档里提到的 MediaPipe。代码里保留了 MediaPipe 占位类。

### 2.2 已有页面

- `MainActivity` 启动 Compose，并进入 `FlexFitNavigation`。
- 底部 Tab：Home、Workout、Progress、Profile。
- Home 已有今日训练、快捷入口、推荐动作。
- Pull-up 选择页支持宽握、标准、窄握，以及实时相机/视频分析模式。
- Pull-up 相机页已有 CameraX 预览、权限处理、骨架叠加、状态面板、反馈卡片。
- Progress 页面已有训练记录、统计卡片、趋势图区域。
- Profile 页面已有用户资料、身高体重、目标和设置项 UI。

### 2.3 已有业务模块

- `PoseDetectorWrapper` 使用 ML Kit Accurate Pose Detector 处理 CameraX 帧。
- `ml/PullUpAnalyzer` 已实现引体向上状态机：准备、底部、上拉、顶部，并带有计数、错误检测、语音动作。
- `VoiceGuideManager` 已可播放开始、成功、失败、摆动、耸肩、高度不足等音频。
- `WorkoutRecordRepository` 当前是内存存储，带示例数据。
- `LlmApiService` 和 `LlmRepository` 已预留训练分析、实时建议、动作推荐接口。

### 2.4 当前验证结果

首次直接执行 `./gradlew test` 失败，因为仓库没有 `local.properties`，环境也没有 `ANDROID_HOME`。

使用本机 SDK 临时环境变量后验证通过：

```bash
ANDROID_HOME=/path/to/Android/sdk ./gradlew test
ANDROID_HOME=/path/to/Android/sdk ./gradlew assembleDebug
```

结果：

- `test` 通过。
- `assembleDebug` 通过。
- 只有弃用警告，没有编译错误。
- `gradlew` 在 Git 中是 `100644`，直接执行会报 permission denied，建议后续将可执行位提交为 `100755`。

## 3. 主要风险与优先修复项

### P0 - 姿态关键点格式不一致

当前最关键的问题是关键点索引体系混乱：

- `PoseModels.kt` 定义的是 33 点 MediaPipe 风格索引，如 `LEFT_HIP = 23`、`RIGHT_HIP = 24`。
- `PoseDetectorWrapper` 输出的是 `FloatArray(19 * 3)` 的自定义 19 点格式。
- `ml/PullUpAnalyzer` 使用了 `LEFT_HIP = 23`、`RIGHT_HIP = 24`，但输入数组只有 19 个点。

这意味着实时相机进入分析后，很可能在访问髋部索引时出现运行时越界，或者得到错误判断。后续开发必须先统一姿态数据标准。

### P0 - 双训练页实现重复

当前有两套训练页：

- `ui/screens/workout/WorkoutScreen.kt`
- `ui/screens/pullup/PullUpCameraScreen.kt`

两者都有相机、视频、骨架、计时、反馈逻辑，文件都超过 1000 行。继续开发前应收敛为一个训练容器和多个动作分析器，否则肩推、深蹲等动作会重复堆代码。

### P0 - 视频分析仍是模拟

视频模式现在没有真正解码视频帧，使用 mock keypoints 模拟流程。要符合“上传视频分析”的产品能力，需要接入 ExoPlayer 或 MediaMetadataRetriever 抽帧，再走同一套姿态检测和动作分析链路。

### P1 - 评分和训练结果仍偏 Demo

结果页已有 UI，但数据存在 Demo 逻辑：

- `totalReps = analysisResult.count + (0..2).random()` 是随机补数。
- 评分只按状态给固定分。
- 还没有按设计文档拆分 Depth、Alignment、Stability 的真实分数。

### P1 - 数据持久化未落地

训练记录当前仅在内存中保存，重启 App 会丢失。设计文档要求训练结果分析和进度查看，建议落地 Room 或 DataStore。

### P1 - LLM 只预留接口

`RetrofitClient` 仍使用 `https://api.your-llm-service.com/`，认证、错误处理、提示词组装、结果缓存都未完成。按设计文档，LLM 应主要用于训练结束后的总结，不建议放到实时纠错主路径。

## 4. 推荐开发流程

### 阶段 0：工程基线与环境固定

目标：让每个开发者都能稳定构建和运行。

任务：

- 提交 `gradlew` 可执行权限。
- 在 README 或开发文档里说明本地 `local.properties` 或 `ANDROID_HOME` 配置方式。
- 增加最小 CI 命令：`test`、`assembleDebug`。
- 清理或记录当前弃用警告，先不强制全部修。

验收标准：

- 新环境按文档配置后能执行 `./gradlew test`。
- `./gradlew assembleDebug` 能产出 debug APK。
- 仓库没有提交本机 `local.properties`。

### 阶段 1：统一姿态数据模型

目标：建立稳定的姿态输入协议，避免算法层和检测层互相猜索引。

建议方案：

- 统一使用 33 点 MediaPipe/ML Kit 标准索引，和 `PoseLandmarkMapping` 对齐。
- 将 `PoseDetectorWrapper` 改为输出 `DetectedPose` 或 `FloatArray(33 * 3)`。
- 对所有关键点增加 visibility/confidence。
- 骨架绘制使用 `SkeletonConnections`，不要在 UI 中手写连接列表。
- 给 `PullUpAnalyzer` 加最小单元测试：输入准备姿势、上拉、顶部、回到底部，验证不会越界且计数正确。

验收标准：

- 相机帧、mock 帧、视频帧都走同一套姿态数据结构。
- 引体向上分析器不再直接依赖 19 点自定义格式。
- 至少覆盖准备、计数、错误反馈三个单元测试。

### 阶段 2：收敛训练页架构

目标：把重复 UI 和状态逻辑抽出来，便于添加肩推和后续动作。

建议结构：

- `TrainingScreen`：统一相机/视频/权限/计时/开始暂停结束。
- `ExerciseAnalyzer`：动作分析接口。
- `PullUpAnalyzer`：引体向上具体规则。
- `ShoulderPressAnalyzer`：肩推具体规则。
- `PoseOverlay`：统一骨架绘制。
- `TrainingResultDialog`：统一结果展示。
- `TrainingSessionViewModel`：统一状态流。

验收标准：

- 删除或下线重复训练页，只保留一条导航主路径。
- Home、Workout、Pull-up Select 都能进入统一训练流程。
- UI 层不直接写动作判断规则。

### 阶段 3：完成引体向上 MVP

目标：让引体向上成为可演示、可测试、可解释的第一条完整闭环。

任务：

- 修复关键点索引后完善宽握/标准/窄握判断。
- 明确准备姿势、上拉、顶部、下降的阈值。
- 实现常见错误：摆动、耸肩、未过杠或高度不足、动作幅度不足。
- 评分拆分为 Depth、Alignment、Stability，并计算总分。
- 骨架颜色按状态显示：绿色正确、黄色警告、红色错误。
- 结果页展示本次训练总分、完成次数、错误次数、主要问题、改进建议。

验收标准：

- 真机相机下能完成一次从开始到结束的训练。
- 正确动作能计数，明显错误动作不计数或给出警告。
- 结果页数据来自真实分析，不再使用随机补数。

### 阶段 4：补齐坐姿哑铃肩推 Demo

目标：满足设计文档 Demo 阶段的第二个实做动作。

任务：

- 在动作选择页加入肩推入口。
- 定义肩推关键点：肩、肘、腕、髋、躯干。
- 判断维度：手肘起始角度、推起高度、左右对称、躯干后仰、肩部稳定。
- 复用统一训练页、骨架、结果页和记录保存。

验收标准：

- 肩推可从动作选择进入训练。
- 可计数、可纠错、可生成结果。
- 与引体向上共用姿态检测和训练状态管理。

### 阶段 5：训练记录持久化与进度页真实化

目标：让训练记录跨重启保留，并驱动 Progress/Profile 数据。

任务：

- 引入 Room 或使用 DataStore 保存轻量训练记录。
- 将 `WorkoutRecordRepository` 从内存示例数据改为真实数据源。
- Progress 页面读取真实历史，支持按动作筛选。
- Profile 中的总训练次数、连续天数、平均分从 Repository 派生。

验收标准：

- 训练结果保存后，重启 App 仍能看到历史记录。
- 清空数据、筛选动作、统计卡片均能正确更新。

### 阶段 6：训练后 LLM 分析

目标：把 LLM 用在训练结束后的解释与建议，不影响实时链路延迟。

任务：

- 将 LLM base URL 和 API key 放入安全配置，不硬编码。
- 设计请求摘要，只传训练统计和关键错误，不上传视频。
- 设计 Prompt：总体评价、优点、不足、下一次训练建议。
- 结果页增加“AI 分析”区域，支持加载中、失败重试、缓存上次结果。

验收标准：

- 无网络或接口失败时，结果页仍能显示本地规则分析。
- LLM 成功时返回结构化 strengths、weaknesses、recommendations。
- 不在实时帧分析中调用 LLM。

### 阶段 7：视频分析真实化

目标：把“上传视频”从模拟改为实际分析。

任务：

- 接入 ExoPlayer 预览或 MediaMetadataRetriever 抽帧。
- 控制抽帧频率，避免性能过载。
- 复用姿态检测和动作分析器。
- 将视频分析结果和实时训练结果统一保存。

验收标准：

- 选择本地视频后能逐帧分析并显示进度。
- 结果页展示视频中的计数、评分和错误反馈。

### 阶段 8：质量、性能与发布准备

任务：

- 真机测试前置摄像头、弱光、远近距离、单人场景。
- 性能目标：相机预览不卡顿，姿态分析延迟可接受。
- 增加崩溃保护：相机初始化失败、姿态未检测、权限拒绝、LLM 超时。
- 统一英文/中文文案，按目标用户决定最终语言。
- 准备图标、应用名、隐私说明和权限说明。

验收标准：

- 长时间训练不明显发热或掉帧。
- 权限拒绝、无网络、未检测到人体都有可理解提示。
- Demo 可稳定演示完整闭环。

## 5. 建议的近期任务顺序

最建议按下面顺序开工：

1. 固定本地构建基线：`gradlew` 权限、SDK 配置文档、README。
2. 修复姿态关键点索引体系，这是当前最大的运行时风险。
3. 为 `PullUpAnalyzer` 写测试，先保证算法不会越界且计数逻辑可验证。
4. 收敛 `WorkoutScreen` 和 `PullUpCameraScreen`，避免后续重复开发。
5. 完成引体向上的真实评分和结果页数据。
6. 接入持久化，让 Progress/Profile 展示真实训练历史。
7. 接入训练后 LLM 分析。
8. 开始肩推动作。
9. 最后补视频真实分析、性能优化和发布准备。

## 6. 每次开发迭代流程

建议每个功能按以下步骤执行：

1. 先写清楚用户路径，例如“选择引体向上 - 授权相机 - 开始训练 - 完成 3 次 - 查看结果 - 保存记录”。
2. 明确输入和输出：姿态点、动作状态、反馈、评分、训练记录。
3. 先补或调整模型层，再接算法，再接 UI。
4. 每个动作分析器至少配一组 mock keypoints 单元测试。
5. 每次提交前运行：

```bash
ANDROID_HOME=/path/to/Android/sdk ./gradlew test
ANDROID_HOME=/path/to/Android/sdk ./gradlew assembleDebug
```

6. 真机验证相机链路，因为 JVM 单元测试无法覆盖 CameraX、ML Kit 和权限交互。

## 7. 不建议立即做的事

- 不建议先扩展 10 个动作。应先把引体向上闭环做扎实，再复制到肩推。
- 不建议在实时纠错中调用 LLM。实时反馈应由本地规则完成，LLM 用于训练后总结。
- 不建议继续在 UI 文件里堆动作规则。动作规则应在 analyzer 层。
- 不建议先重做视觉风格。当前更大的风险是姿态数据、训练状态和结果数据真实性。

