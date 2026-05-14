package com.jumpmaster.app.domain.session

import org.junit.Assert.assertEquals
import org.junit.Test

class JumpSessionCaloriesTest {

    @Test
    fun zeroJumps_yieldsZero() {
        assertEquals(0, JumpSessionCalories.fromJumpCount(0))
        assertEquals(0, JumpSessionCalories.fromJumpCount(-3))
    }

    @Test
    fun demoRatio_420Jumps_matchesSeedData() {
        assertEquals(75, JumpSessionCalories.fromJumpCount(420))
    }

    @Test
    fun smallPositive_nonZeroMinimum() {
        assertEquals(1, JumpSessionCalories.fromJumpCount(1))
    }
}
