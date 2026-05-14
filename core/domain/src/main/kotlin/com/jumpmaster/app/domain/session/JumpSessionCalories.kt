package com.jumpmaster.app.domain.session

/**
 * Rough kcal estimate for history display (aligned with demo seed ratios, ~0.18 kcal / jump).
 */
object JumpSessionCalories {
    fun fromJumpCount(jumpCount: Int): Int {
        if (jumpCount <= 0) return 0
        return maxOf(1, jumpCount * 18 / 100)
    }
}
