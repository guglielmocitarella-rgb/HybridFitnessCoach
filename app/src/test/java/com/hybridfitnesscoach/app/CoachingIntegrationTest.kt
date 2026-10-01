package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class CoachingIntegrationTest {
    private val gym=trainingLocations.first{it.name=="Palestra"}
    private val athlete=AthleteProfile(goal="Forza",assessedTrainingExperience=2,assessedPushCapacity=2,assessedPullCapacity=2)
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
        assertFalse(locationCompatible(assisted,TrainingLocation("test:band","Band","",TrainingLocationKind.CUSTOM,setOf(EquipmentId.RESISTANCE_BAND))))
        assertTrue(locationCompatible(assisted,TrainingLocation("test:both","Both","",TrainingLocationKind.CUSTOM,setOf(EquipmentId.PULL_UP_BAR,EquipmentId.RESISTANCE_BAND))))
    }
    @Test fun bodyweightNeedsNoPurchasedEquipment() {
        assertTrue(locationCompatible(Exercise("Push-up","Petto","Corpo libero","Principiante"),gym))
    }
    @Test fun abWheelAndRollerHaveSelectableExercises() {
        for(eq in listOf("Ab Wheel","Foam Roller")) {
            assertTrue(exercises.any{it.equipment==eq && locationCompatible(it,TrainingLocation("test:"+eq,eq,"",TrainingLocationKind.CUSTOM,setOfNotNull(EquipmentCatalog.fromDisplayName(eq))))})
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
    @Test fun activePlanDisruptionSuppressesHistoryBasedAdvance() {
        val now=System.currentTimeMillis()
        fun good(ts:Long)=SessionRecord(ts,"P",10,10,workoutRpe=7)
        val history=listOf(good(now-1_000),good(now-2_000))
        assertEquals(ProgressionDecision.ADVANCE,coachingContext(history,90,now).plan.decision)
        val today=20_000L
        val planned=PlannedWorkout("w",today,0,0,listOf(exercises.first()),state=PlannedWorkoutState.SKIPPED)
        val plan=TrainingPlan("p",today,1,1,listOf(planned))
        val withPlan=coachingContext(history,90,plan,today,now)
        assertEquals(ProgressionDecision.MAINTAIN,withPlan.plan.decision)
        assertEquals(1,withPlan.planDisruptions)
        assertTrue(withPlan.plan.notes.any{it.contains("Progressione sospesa")})
    }

    @Test fun cleanPlanDoesNotEraseHistoryBasedAdvance() {
        val now=System.currentTimeMillis()
        val history=listOf(
            SessionRecord(now-1_000,"P",10,10,workoutRpe=7),
            SessionRecord(now-2_000,"P",10,10,workoutRpe=7)
        )
        val today=20_000L
        val plan=TrainingPlan("p",today,1,1,listOf(PlannedWorkout("w",today+1,0,0,listOf(exercises.first()))))
        val context=coachingContext(history,90,plan,today,now)
        assertEquals(ProgressionDecision.ADVANCE,context.plan.decision)
        assertEquals(0,context.planDisruptions)
        assertEquals(1,context.upcomingSessions)
    }

}
