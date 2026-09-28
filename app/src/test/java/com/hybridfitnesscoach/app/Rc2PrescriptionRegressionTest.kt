package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class Rc2PrescriptionRegressionTest {
    @Test fun reducingWorkloadDoesNotMultiplyRestOrAccumulateFractionalSeconds() {
        val context = coachingContext(emptyList(), 30)
        for (name in listOf("Rematore bilanciere", "Trazioni prone", "Australian Pull-up")) {
            val base = exercises.first { it.name.equals(name, ignoreCase = true) }
            val adapted = applyCoaching(listOf(base), context).single()
            assertEquals(base.setRestSec, adapted.setRestSec)
            assertTrue(adapted.targetSets < base.targetSets)
        }
    }

    @Test fun repeatedRegenerationKeepsPinnedPrescriptionAndDoesNotCompoundRest() {
        val context = coachingContext(emptyList(), 30)
        val pool = exercises.filter { it.name.equals("Rematore bilanciere", true) || it.name.equals("Trazioni prone", true) || it.name.equals("Australian pull-up", true) }
        var workout = applyCoaching(pool, context)
        val pinned = workout.first()
        repeat(12) { workout = regenerateCoachedWorkout(workout, pool, setOf(pinned.name), context) }
        assertEquals(pinned, workout.first { it.name == pinned.name })
        workout.forEach { ex -> assertEquals(pool.first { it.name == ex.name }.setRestSec, ex.setRestSec) }
    }
}
