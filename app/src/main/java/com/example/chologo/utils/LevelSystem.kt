package com.example.chologo.utils

data class LevelInfo(
    val level: Int,
    val currentXp: Long,
    val currentLevelStartXp: Long,
    val nextLevelXp: Long,
    val progressFraction: Float,
    val xpNeededForNextLevel: Long,
    val levelTitle: String
)

object LevelSystem {

    // XP needed to REACH each level
    // Index 0 = Level 1, Index 1 = Level 2, etc.
    private val levelThresholds = listOf(
        0L,    // Level 1
        100L,  // Level 2
        250L,  // Level 3
        450L,  // Level 4
        700L,  // Level 5
        1000L, // Level 6
        1400L, // Level 7
        1900L, // Level 8
        2500L, // Level 9
        3200L  // Level 10
    )

    fun getLevelInfo(xp: Long): LevelInfo {
        val safeXp = xp.coerceAtLeast(0L)

        var level = 1
        for (i in levelThresholds.indices) {
            if (safeXp >= levelThresholds[i]) {
                level = i + 1
            } else {
                break
            }
        }

        val currentLevelStartXp = levelThresholds[level - 1]

        val nextLevelXp = if (level < levelThresholds.size) {
            levelThresholds[level]
        } else {
            // After max defined level, continue increasing by 1000 XP each level
            currentLevelStartXp + 1000L
        }

        val progressInLevel = safeXp - currentLevelStartXp
        val levelRange = (nextLevelXp - currentLevelStartXp).coerceAtLeast(1L)

        val progressFraction = (progressInLevel.toFloat() / levelRange.toFloat())
            .coerceIn(0f, 1f)

        val xpNeededForNextLevel = (nextLevelXp - safeXp).coerceAtLeast(0L)

        return LevelInfo(
            level = level,
            currentXp = safeXp,
            currentLevelStartXp = currentLevelStartXp,
            nextLevelXp = nextLevelXp,
            progressFraction = progressFraction,
            xpNeededForNextLevel = xpNeededForNextLevel,
            levelTitle = getLevelTitle(level)
        )
    }

    fun getLevelTitle(level: Int): String {
        return when (level) {
            1 -> "Beginner"
            2 -> "Explorer"
            3 -> "Regular"
            4 -> "Trusted"
            5 -> "Advanced"
            6 -> "Expert"
            7 -> "Elite"
            8 -> "Master"
            9 -> "Champion"
            10 -> "Legend"
            else -> "Ultimate"
        }
    }

    fun getProgressPercent(xp: Long): Int {
        return (getLevelInfo(xp).progressFraction * 100).toInt()
    }

    fun getXpText(xp: Long): String {
        val info = getLevelInfo(xp)
        return "${info.currentXp} XP"
    }

    fun getNextLevelText(xp: Long): String {
        val info = getLevelInfo(xp)
        return "${info.xpNeededForNextLevel} XP needed for next level"
    }
}