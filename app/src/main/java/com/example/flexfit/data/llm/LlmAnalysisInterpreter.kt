package com.example.flexfit.data.llm

import com.example.flexfit.data.model.AnalysisSource
import com.example.flexfit.data.model.WorkoutAnalysisResult
import com.google.gson.Gson
import com.google.gson.JsonObject

internal object LlmAnalysisInterpreter {
    private val gson = Gson()

    fun parseStructuredResponse(content: String): WorkoutAnalysisResult? {
        return runCatching {
            val json = gson.fromJson(extractJsonObject(content), JsonObject::class.java)
            val summary = json.get("summary")?.asString.orEmpty()
            WorkoutAnalysisResult(
                summary = summary,
                strengths = json.getAsJsonArray("strengths")?.map { it.asString } ?: emptyList(),
                weaknesses = json.getAsJsonArray("weaknesses")?.map { it.asString } ?: emptyList(),
                recommendations = json.getAsJsonArray("recommendations")?.map { it.asString } ?: emptyList(),
                source = AnalysisSource.LLM
            ).takeIf {
                it.summary.isNotBlank() ||
                    it.strengths.isNotEmpty() ||
                    it.weaknesses.isNotEmpty() ||
                    it.recommendations.isNotEmpty()
            }
        }.getOrNull()
    }

    fun localFallback(
        stats: LlmWorkoutStats,
        responseLanguage: LlmResponseLanguage = LlmResponseLanguage.ENGLISH
    ): WorkoutAnalysisResult {
        if (responseLanguage == LlmResponseLanguage.CHINESE) {
            return localChineseFallback(stats)
        }

        val accuracy = stats.averageAccuracy
        val successRate = stats.successRate
        val errors = stats.errorsCount
        val warnings = stats.warningsCount

        val summary = when {
            accuracy >= 85 && errors <= 2 -> "Great workout! Your form was consistent throughout."
            accuracy >= 70 -> "Good effort! There is room to improve on your form."
            accuracy >= 50 -> "Keep practicing! Focus on the key areas identified below."
            else -> "Every rep counts. Build up your strength and form gradually."
        }

        val strengths = buildList {
            if (successRate >= 85) add("High rep completion rate at $successRate%")
            if (errors <= 2) add("Minimal form errors detected")
            if (stats.stabilityScore >= 80) add("Strong core stability throughout the movement")
            if (accuracy >= 75) add("Consistent movement quality")
            if (isEmpty()) add("Determination and effort throughout the session")
        }

        val weaknesses = buildList {
            if (accuracy < 75) add("Movement accuracy below target range")
            if (errors > 5) add("Multiple form errors were detected - focus on control")
            if (stats.alignmentScore < 70) add("Body alignment needs improvement")
            if (stats.stabilityScore < 70) add("Stability could be improved with core work")
            if (warnings > 10) add("Too many compensatory movements detected")
            if (isEmpty()) add("Minor adjustments needed for optimal form")
        }

        val recommendations = buildList {
            add("Warm up for 5 minutes before starting")
            if (accuracy < 75) add("Focus on controlled, full-range repetitions")
            if (stats.alignmentScore < 70) add("Practice in front of a mirror to check alignment")
            if (stats.stabilityScore < 70) add("Add planks and core exercises to your routine")
            if (errors > 5) add("Reduce speed to prioritize form over reps")
            add("Stay consistent - regular practice leads to improvement")
        }

        return WorkoutAnalysisResult(
            summary = summary,
            strengths = strengths,
            weaknesses = weaknesses,
            recommendations = recommendations,
            source = AnalysisSource.LOCAL_FALLBACK
        )
    }

    private fun localChineseFallback(stats: LlmWorkoutStats): WorkoutAnalysisResult {
        val accuracy = stats.averageAccuracy
        val successRate = stats.successRate
        val errors = stats.errorsCount
        val warnings = stats.warningsCount

        val summary = when {
            accuracy >= 85 && errors <= 2 -> "很棒的一次训练！你的动作质量在整个过程中都比较稳定。"
            accuracy >= 70 -> "训练完成得不错！动作质量还有进一步提升空间。"
            accuracy >= 50 -> "继续保持练习！下一步可以重点关注下面这些动作细节。"
            else -> "每一次重复都有价值。先稳住动作，再逐步提升力量和次数。"
        }

        val strengths = buildList {
            if (successRate >= 85) add("完成率较高，达到 $successRate%")
            if (errors <= 2) add("检测到的明显动作错误较少")
            if (stats.stabilityScore >= 80) add("动作过程中核心稳定性表现良好")
            if (accuracy >= 75) add("整体动作质量比较一致")
            if (isEmpty()) add("本次训练中展现了持续的专注和努力")
        }

        val weaknesses = buildList {
            if (accuracy < 75) add("动作准确率低于目标范围")
            if (errors > 5) add("检测到多次动作错误，建议优先控制动作节奏")
            if (stats.alignmentScore < 70) add("身体对齐度还需要改善")
            if (stats.stabilityScore < 70) add("稳定性可以通过核心训练继续提升")
            if (warnings > 10) add("代偿动作偏多，需要减少不必要的晃动")
            if (isEmpty()) add("只需要做一些小幅调整，就能进一步优化动作")
        }

        val recommendations = buildList {
            add("训练前先进行 5 分钟热身")
            if (accuracy < 75) add("专注于受控、完整幅度的重复动作")
            if (stats.alignmentScore < 70) add("可以对着镜子练习，检查身体对齐")
            if (stats.stabilityScore < 70) add("在日常训练中加入平板支撑等核心练习")
            if (errors > 5) add("适当放慢速度，把动作质量放在次数之前")
            add("保持规律训练，稳定练习会带来持续进步")
        }

        return WorkoutAnalysisResult(
            summary = summary,
            strengths = strengths,
            weaknesses = weaknesses,
            recommendations = recommendations,
            source = AnalysisSource.LOCAL_FALLBACK
        )
    }

    private fun extractJsonObject(content: String): String {
        val trimmed = content.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) {
            trimmed.substring(start, end + 1)
        } else {
            trimmed
        }
    }
}
