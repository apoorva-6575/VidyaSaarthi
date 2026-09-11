package com.hackx.ruraledtech.domain.model

data class Mastery(
    val learnerId: String,
    val conceptId: String,
    val conceptName: String,
    val score: Float,
    val confidence: Float,
    val attemptCount: Int,
    val lastUpdated: Long,
) {
    val level: MasteryLevel
        get() = MasteryLevel.fromScore(score)
}

enum class MasteryLevel(val floor: Float, val label: String) {
    BEGINNER(0.00f, "Beginner"),
    DEVELOPING(0.40f, "Developing"),
    PROFICIENT(0.70f, "Proficient"),
    MASTERED(0.85f, "Mastered"),
    ;

    companion object {
        fun fromScore(score: Float): MasteryLevel =
            entries.sortedByDescending { it.floor }.first { score >= it.floor }
    }
}
