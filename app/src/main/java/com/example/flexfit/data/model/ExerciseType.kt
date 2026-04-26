package com.example.flexfit.data.model

/**
 * Enum representing different exercise types supported by the app.
 */
enum class ExerciseType(
    val displayName: String,
    val description: String,
    val isImplemented: Boolean = false
) {
    // Implemented exercises (Demo stage)
    PULL_UP(
        displayName = "Pull Up",
        description = "Wide/Narrow/Normal grip pull-ups",
        isImplemented = true
    ),
    SHOULDER_PRESS(
        displayName = "Shoulder Press",
        description = "Seated dumbbell shoulder press",
        isImplemented = true
    ),

    // Reserved for future development
    BENCH_PRESS(
        displayName = "Bench Press",
        description = "Flat bench press - Coming soon",
        isImplemented = false
    ),
    SEATED_ROW(
        displayName = "Seated Row",
        description = "Seated cable row - Coming soon",
        isImplemented = false
    ),
    BICEPS_CURL(
        displayName = "Biceps Curl",
        description = "Resistance band biceps curl - Coming soon",
        isImplemented = false
    ),
    SQUAT(
        displayName = "Squat",
        description = "Bodyweight squat - Coming soon",
        isImplemented = false
    ),
    LEG_PRESS(
        displayName = "45° Leg Press",
        description = "Incline leg press - Coming soon",
        isImplemented = false
    ),
    LAT_PULL_DOWN(
        displayName = "Lat Pull Down",
        description = "Cable lat pull down - Coming soon",
        isImplemented = false
    ),
    TRICEPS_PUSH_DOWN(
        displayName = "Triceps Push Down",
        description = "Cable triceps push down - Coming soon",
        isImplemented = false
    ),
    PUSH_UP(
        displayName = "Push Up",
        description = "Standard push-up - Coming soon",
        isImplemented = false
    )
}

/**
 * Pull up variations
 */
enum class PullUpVariation(val displayName: String) {
    WIDE_GRIP("Wide Grip"),
    NARROW_GRIP("Narrow Grip"),
    NORMAL_GRIP("Normal Grip")
}
