package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class CoachingIntegrationTest {
    private val gym=profiles.first{it.name=="Palestra"}
    private val athlete=AthleteProfile(goal="Forza",level="Intermedio")
    private fun history(rpe:Int,done:Int)=listOf(SessionRecord(System.currentTimeMillis(),"P",done,18,workoutRpe=rpe))

    @Test fun highFatigueReducesActualGeneratedSets() {
        val base=generateWorkout(gym,athlete,emptyList())
        val tired=generateWorkout(gym,athlete,history(10,18))
        assertTrue(tired.sumOf{prescriptionFor(it).sets}<base.sumOf{prescriptionFor(it).sets})
    }
    @Test fun failedWorkoutReducesNextPrescription() {
        val base=generateWorkout(gym,athlete,emptyList())
        val failed=generateWorkout(gym,athlete,history(8,5))
        assertTrue(failed.sumOf{prescriptionFor(it).sets}<base.sumOf{prescriptionFor(it).sets})
    }
    @Test fun compoundEquipmentRequiresAllResources() {
        val assisted=Exercise("Assisted pull", "Dorso", "Sbarra / Elastici", "Principiante")
        assertFalse(equipmentCompatible(assisted,EquipmentProfile("Band","",listOf("Elastici"))))
        assertTrue(equipmentCompatible(assisted,EquipmentProfile("Both","",listOf("Sbarra","Elastici"))))
    }
    @Test fun bodyweightNeedsNoPurchasedEquipment() {
        assertTrue(equipmentCompatible(Exercise("Push-up","Petto","Corpo libero","Principiante"),gym))
    }
    @Test fun abWheelAndRollerHaveSelectableExercises() {
        for(eq in listOf("Ab Wheel","Foam Roller")) {
            assertTrue(exercises.any{it.equipment==eq && equipmentCompatible(it,EquipmentProfile(eq,"",listOf(eq)))})
        }
    }

    @Test fun noHistoryMaintainsTargetsAndRequestsCalibration() {
        val context=coachingContext(emptyList())
        assertEquals(ProgressionDecision.MAINTAIN,context.plan.decision)
        assertEquals(0,context.evidenceSessions)
    }
    @Test fun lowReadinessChangesWorkoutWithoutHistory() {
        val base=generateWorkout(gym,athlete,emptyList(),90)
        val low=generateWorkout(gym,athlete,emptyList(),25)
        assertTrue(low.sumOf{it.targetSets}<base.sumOf{it.targetSets})
        assertTrue(low.zip(base).all{(a,b)->prescriptionFor(a).value<=prescriptionFor(b).value})
    }
    @Test fun resumePreservesAdaptedPrescription() {
        val ex=generateWorkout(gym,athlete,emptyList(),25).first()
        val p=prescriptionFor(ex)
        val saved=WorkoutExerciseState(ex.name,ex.measure,(0 until p.sets).map{WorkoutSetState(it,targetReps=p.value.toInt())},setRestSec=ex.setRestSec)
        val resumed=restoreExercises(WorkoutSessionState("x",1,"P",listOf(saved))).single()
        assertEquals(p,prescriptionFor(resumed))
        assertEquals(ex.setRestSec,resumed.setRestSec)
    }
    @Test fun multiMuscleLoadUsesLeastRecoveredMuscle() {
        val ex=Exercise("Dip","Petto / Tricipiti","Parallele","Intermedio")
        assertEquals(25,exerciseRecovery(ex,mapOf("Petto" to 90,"Tricipiti" to 25)))
    }
}
