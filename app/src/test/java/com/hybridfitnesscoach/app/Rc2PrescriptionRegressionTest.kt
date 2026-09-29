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
    @Test fun runnerRestPrecedenceIsIntervalThenPrescriptionThenCatalogFallback() {
        val base = exercises.first { it.name.equals("Australian Pull-up", ignoreCase = true) }.copy(setRestSec = 90)
        assertEquals(150, runnerSetRestSeconds(base.copy(intervalRestSec = 150), prescribedRestSeconds = 120))
        assertEquals(120, runnerSetRestSeconds(base, prescribedRestSeconds = 120))
        assertEquals(90, runnerSetRestSeconds(base))
    }

    @Test fun sourceBasedRestIsNeverSilentlyReplacedByLegacyHeuristic() {
        val ex = Exercise("Synthetic press","Petto","Bilanciere","Base",category=ExerciseCategory.PUSH,resistanceMode=ResistanceMode.EXTERNAL_LOAD,setRestSec=75)
        val context = PrescriptionContext("Forza","Intermedio",ExerciseCategory.PUSH,5,5,3,null,85,null)
        val sourced = SourcePrescriptionBaselineRegistry.prescribeIfVerified(ex, context)!!
        assertEquals(180, sourced.restSeconds)
        assertEquals(180, runnerSetRestSeconds(ex, sourced.restSeconds))
        assertNotEquals(ex.setRestSec, runnerSetRestSeconds(ex, sourced.restSeconds))
    }

    @Test fun effectiveRunnerPrescriptionUsesVerifiedBaselineForEligibleResistanceExercise() {
        val ex=Exercise("Synthetic press","Petto","Bilanciere","Base",category=ExerciseCategory.PUSH,resistanceMode=ResistanceMode.EXTERNAL_LOAD,setRestSec=75)
        val athlete=AthleteProfile(goal="Forza",level="Intermedio")
        val out=effectiveRunnerPrescription(ex,athlete,emptyList(),85)
        assertTrue(out.sourceBased)
        assertEquals(3,out.sets)
        assertEquals(6.0,out.value,0.001)
        assertEquals(180,out.restSeconds)
    }

    @Test fun effectiveRunnerPrescriptionFallsBackExactlyForUnsupportedExercise() {
        val ex=Exercise("Synthetic run","Cardio","Corpo libero","Principiante",category=ExerciseCategory.CARDIO,measure=ExerciseMeasure.DISTANCE,resistanceMode=ResistanceMode.BODYWEIGHT,targetSets=1,targetValue=1000.0,setRestSec=45)
        val athlete=AthleteProfile(goal="Forza",level="Intermedio")
        val legacy=prescriptionFor(ex)
        val out=effectiveRunnerPrescription(ex,athlete,emptyList(),85)
        assertFalse(out.sourceBased)
        assertEquals(legacy.sets,out.sets)
        assertEquals(legacy.value,out.value,0.001)
        assertEquals(45,out.restSeconds)
    }

}
