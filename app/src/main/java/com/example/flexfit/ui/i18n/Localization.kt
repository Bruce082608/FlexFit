package com.example.flexfit.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.flexfit.data.model.ExerciseType
import com.example.flexfit.data.model.PerformanceLevel
import com.example.flexfit.ml.PullUpType

enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    CHINESE("zh");

    val displayName: String
        get() = when (this) {
            ENGLISH -> "English"
            CHINESE -> "中文"
        }

    fun text(en: String, zh: String): String {
        return when (this) {
            ENGLISH -> en
            CHINESE -> zh
        }
    }

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code == code } ?: ENGLISH
        }
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }

@Composable
fun l10n(en: String): String {
    return LocalAppLanguage.current.localize(en)
}

@Composable
fun l10n(en: String, zh: String): String {
    return LocalAppLanguage.current.text(en, zh)
}

fun AppLanguage.localize(en: String): String {
    if (this == AppLanguage.ENGLISH) return en
    return zhTranslations[en] ?: en
}

fun AppLanguage.exerciseName(exercise: ExerciseType): String {
    return when (exercise) {
        ExerciseType.PULL_UP -> text("Pull Up", "引体向上")
        ExerciseType.SHOULDER_PRESS -> text("Shoulder Press", "肩上推举")
        ExerciseType.BENCH_PRESS -> text("Bench Press", "卧推")
        ExerciseType.SEATED_ROW -> text("Seated Row", "坐姿划船")
        ExerciseType.BICEPS_CURL -> text("Biceps Curl", "二头弯举")
        ExerciseType.SQUAT -> text("Squat", "深蹲")
        ExerciseType.LEG_PRESS -> text("45° Leg Press", "45° 腿举")
        ExerciseType.LAT_PULL_DOWN -> text("Lat Pull Down", "高位下拉")
        ExerciseType.TRICEPS_PUSH_DOWN -> text("Triceps Push Down", "绳索下压")
        ExerciseType.PUSH_UP -> text("Push Up", "俯卧撑")
    }
}

fun AppLanguage.exerciseDescription(exercise: ExerciseType): String {
    return when (exercise) {
        ExerciseType.PULL_UP -> text("Wide/Narrow/Normal grip pull-ups", "宽握、窄握、标准握距引体向上")
        ExerciseType.SHOULDER_PRESS -> text("Seated dumbbell shoulder press", "坐姿哑铃肩上推举")
        ExerciseType.BENCH_PRESS -> text("Flat bench press - Coming soon", "平板卧推 - 即将推出")
        ExerciseType.SEATED_ROW -> text("Seated cable row - Coming soon", "坐姿绳索划船 - 即将推出")
        ExerciseType.BICEPS_CURL -> text("Resistance band biceps curl - Coming soon", "弹力带二头弯举 - 即将推出")
        ExerciseType.SQUAT -> text("Bodyweight squat - Coming soon", "自重深蹲 - 即将推出")
        ExerciseType.LEG_PRESS -> text("Incline leg press - Coming soon", "斜板腿举 - 即将推出")
        ExerciseType.LAT_PULL_DOWN -> text("Cable lat pull down - Coming soon", "绳索高位下拉 - 即将推出")
        ExerciseType.TRICEPS_PUSH_DOWN -> text("Cable triceps push down - Coming soon", "绳索肱三头肌下压 - 即将推出")
        ExerciseType.PUSH_UP -> text("Standard push-up - Coming soon", "标准俯卧撑 - 即将推出")
    }
}

fun AppLanguage.pullUpTypeName(type: PullUpType): String {
    return when (type) {
        PullUpType.WIDE -> text("Wide Grip", "宽握")
        PullUpType.NORMAL -> text("Normal Grip", "标准握距")
        PullUpType.NARROW -> text("Narrow Grip", "窄握")
    }
}

fun AppLanguage.workoutName(name: String): String {
    return when {
        name.equals("Pull Up", ignoreCase = true) -> text("Pull Up", "引体向上")
        name.equals("Shoulder Press", ignoreCase = true) -> text("Shoulder Press", "肩上推举")
        name.contains("Wide Grip", ignoreCase = true) -> text(name, "宽握引体向上")
        name.contains("Normal Grip", ignoreCase = true) -> text(name, "标准握距引体向上")
        name.contains("Narrow Grip", ignoreCase = true) -> text(name, "窄握引体向上")
        else -> localize(name)
    }
}

fun AppLanguage.performanceLevelName(level: PerformanceLevel): String {
    return when (level) {
        PerformanceLevel.EXCELLENT -> text("Excellent", "优秀")
        PerformanceLevel.GOOD -> text("Good", "良好")
        PerformanceLevel.AVERAGE -> text("Average", "一般")
        PerformanceLevel.NEEDS_IMPROVEMENT -> text("Needs Improvement", "需要改进")
    }
}

private val zhTranslations = mapOf(
    "Home" to "首页",
    "All" to "全部",
    "Workout" to "训练",
    "Progress" to "进度",
    "Profile" to "我的",
    "Edit Profile" to "编辑资料",
    "Profile Info" to "资料信息",
    "Workout Setup" to "训练设置",
    "Pull-up Camera" to "引体向上相机",
    "Shoulder Press" to "肩上推举",
    "Calibration" to "校准",
    "Coming soon" to "即将推出",
    "Back" to "返回",
    "Start" to "开始",
    "Save" to "保存",
    "Cancel" to "取消",
    "OK" to "确定",
    "Done" to "完成",
    "Clear" to "清空",
    "Settings" to "设置",
    "Language" to "语言",
    "Choose Language" to "选择语言",
    "Current Language" to "当前语言",
    "Edit profile" to "编辑资料",
    "Edit body stats" to "编辑身体数据",
    "Profile avatar" to "头像",
    "Workouts" to "训练",
    "Day Streak" to "连续天数",
    "Avg Score" to "平均得分",
    "Body Stats" to "身体数据",
    "Height" to "身高",
    "Weight" to "体重",
    "Fitness Goal" to "健身目标",
    "Build Strength" to "增强力量",
    "Lose Weight" to "减脂",
    "Stay Fit" to "保持健康",
    "Muscle Tone" to "塑形增肌",
    "Notifications" to "通知",
    "Device Settings" to "设备设置",
    "Feedback" to "反馈",
    "About" to "关于",
    "About FlexFit" to "关于 FlexFit",
    "Rate App" to "评分",
    "Edit Body Stats" to "编辑身体数据",
    "Nickname" to "昵称",
    "Email" to "邮箱",
    "Avatar" to "头像",
    "Choose Photo" to "选择照片",
    "Remove" to "移除",
    "On" to "已选",
    "Use a nickname with 2-32 characters and a valid email address." to "昵称需为 2-32 个字符，并填写有效邮箱地址。",
    "Enter height from 80-250 cm and weight from 25-250 kg." to "请输入 80-250 cm 的身高和 25-250 kg 的体重。",
    "Total Workouts" to "训练总数",
    "Total Minutes" to "训练分钟",
    "Avg Accuracy" to "平均准确率",
    "Streak Days" to "连续天数",
    "Today's goal: complete one focused Pull Up or Shoulder Press session with clean form and stable local scores." to "今日目标：完成一次专注的引体向上或肩上推举训练，保持动作干净，争取稳定得分。",
    "Pick a movement, then choose camera or video analysis." to "选择一个动作，然后使用相机或视频分析。",
    "Available" to "可用",
    "Training source" to "训练来源",
    "Camera" to "相机",
    "Real-time detection" to "实时检测",
    "Video" to "视频",
    "Upload analysis" to "上传分析",
    "Pull Up mode" to "引体向上模式",
    "shoulder width" to "倍肩宽",
    "Training History" to "训练历史",
    "No workouts yet" to "还没有训练记录",
    "No matching workouts" to "没有匹配的训练",
    "Complete a workout to see your history" to "完成一次训练后即可查看历史",
    "Choose another exercise filter" to "请选择其他动作筛选",
    "Clear training history?" to "清空训练历史？",
    "This removes all saved workout records from this device." to "这会删除本设备上保存的所有训练记录。",
    "Weekly Training" to "每周训练",
    "Number of workouts per day" to "每天训练次数",
    "Accuracy Trend" to "准确率趋势",
    "Your accuracy over the past 7 sessions" to "最近 7 次训练的准确率",
    "Complete workouts to see your accuracy trend." to "完成训练后可查看准确率趋势。",
    "Exercise Distribution" to "动作分布",
    "Time spent on each exercise type" to "各动作类型的训练占比",
    "Complete workouts to see exercise distribution." to "完成训练后可查看动作分布。",
    "AI Training Tips" to "AI 训练建议",
    "Complete a workout to generate personalized suggestions." to "完成训练后可生成个性化建议。",
    "Based on your recent training data, here are some personalized suggestions:" to "根据近期训练数据，这里是一些个性化建议：",
    "No major issues detected. Keep your current form." to "未检测到主要问题。请保持当前动作。",
    "Reps" to "次数",
    "Phase" to "阶段",
    "Duration" to "时长",
    "Accuracy" to "准确率",
    "Analyze Video" to "分析视频",
    "End" to "结束",
    "End Workout" to "结束训练",
    "Pause" to "暂停",
    "Resume" to "继续",
    "Reset" to "重置",
    "Switch Camera" to "切换摄像头",
    "Video Mode" to "视频模式",
    "Loading video..." to "正在加载视频...",
    "Ready to analyze" to "准备开始分析",
    "Tap \"Analyze Video\" below to start frame-by-frame analysis" to "点击下方“分析视频”开始逐帧分析",
    "Analyzing frames..." to "正在分析帧...",
    "Pose detection in progress" to "姿态检测进行中",
    "Video Analysis" to "视频分析",
    "Select a workout video to analyze your form" to "选择训练视频来分析动作",
    "Permission Required" to "需要权限",
    "FlexFit needs access to your videos to analyze your workout form." to "FlexFit 需要访问视频来分析你的训练动作。",
    "Video Permission Required" to "需要视频权限",
    "Grant Permission" to "授予权限",
    "Not Now" to "暂不",
    "Choose Video" to "选择视频",
    "Camera Permission Required" to "需要相机权限",
    "Please grant camera permission for motion recognition" to "请授予相机权限以进行动作识别",
    "Camera Permission Denied" to "相机权限被拒绝",
    "Motion recognition requires camera permission. Please enable it in settings." to "动作识别需要相机权限，请在设置中启用。",
    "Request Again" to "再次请求",
    "Initialization Error" to "初始化错误",
    "Workout Complete!" to "训练完成！",
    "Score" to "得分",
    "Details" to "详情",
    "Success Rate" to "成功率",
    "Calories Burned" to "消耗热量",
    "Errors" to "错误",
    "Warnings" to "警告",
    "Discard" to "放弃",
    "Local Scores" to "本地评分",
    "Real-time rule-based scores calculated from pose metrics during training." to "基于训练中的姿态指标实时计算的规则评分。",
    "Depth" to "深度",
    "Align" to "对齐",
    "Stable" to "稳定",
    "Range of motion and full rep completion." to "动作幅度和完整次数完成度。",
    "Alignment" to "对齐",
    "Left-right symmetry and joint positioning." to "左右对称性和关节位置。",
    "Stability" to "稳定性",
    "Body control, shoulder level, and torso steadiness." to "身体控制、肩部水平和躯干稳定度。",
    "Rule-Based Issues" to "规则检测问题",
    "Detected from local pose rules before AI analysis." to "AI 分析前由本地姿态规则检测得出。",
    "No major issues detected" to "未检测到主要问题",
    "Keep the same tempo and full range of motion." to "保持同样节奏和完整动作幅度。",
    "Review this part of your form on the next set." to "下一组训练时重点检查这个动作环节。",
    "Overall Local Score" to "本地综合评分",
    "AI Analysis" to "AI 分析",
    "AI Report" to "AI 报告",
    "Post-workout personalized coaching generated after the local scores are ready." to "本地评分完成后生成个性化训练指导。",
    "Get personalized insights and recommendations powered by AI." to "获取 AI 生成的个性化洞察和建议。",
    "Analyze with AI" to "使用 AI 分析",
    "Analyzing your workout..." to "正在分析训练...",
    "No AI report saved for this workout yet." to "这次训练还没有保存 AI 报告。",
    "Retry AI Analysis" to "重试 AI 分析",
    "Retry" to "重试",
    "AI-powered" to "AI 生成",
    "Local analysis" to "本地分析",
    "Strengths" to "优势",
    "Areas for Improvement" to "待改进项",
    "Recommendations" to "建议"
)
