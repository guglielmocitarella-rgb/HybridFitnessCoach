package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class RecoveryPrescriptionContractTest {
    private val athlete=AthleteProfile(level="Intermedio",goal="Forza")

    @Test fun sourcedSetRestReachesEffectiveExerciseAndPlannedState() {
        val ex=Exercise("Contract press","Petto","Manubri","Intermedio",
            role="Forza",category=ExerciseCategory.PUSH,resistanceMode=ResistanceMode.EXTERNAL_LOAD,
            setRestSec=90,exerciseRestSec=120,targetSets=3,targetValue=6.0)
        val effective=exerciseWithEffectivePrescription(ex,athlete,emptyList(),80)
        assertEquals(180,effective.setRestSec)
        val planned=plannedExerciseState(0,effective)
        assertEquals(180,planned.setRestSec)
    }

    @Test fun intervalRecoveryHasPriorityOverGenericOrSourcedSetRest() {
        val ex=Exercise("Intervals","Cardio","Corpo libero","Intermedio",
            measure=ExerciseMeasure.INTERVALS,targetSets=5,targetValue=60.0,
            setRestSec=90,intervalRestSec=60)
        assertEquals(60,runnerSetRestSeconds(ex,180))
    }

    @Test fun unsourcedExerciseKeepsExplicitCatalogRecovery() {
        val ex=Exercise("Mobility contract","Mobilità","Corpo libero","Principiante",
            category=ExerciseCategory.MOBILITY,resistanceMode=ResistanceMode.BODYWEIGHT,
            setRestSec=45,exerciseRestSec=75,targetSets=1,targetValue=30.0,
            measure=ExerciseMeasure.DURATION)
        val effective=exerciseWithEffectivePrescription(ex,athlete,emptyList(),80)
        assertEquals(45,effective.setRestSec)
        assertEquals(75,effective.exerciseRestSec)
    }

    @Test fun exerciseTransitionRecoveryTracksEffectivePrescription() {
        val ex=Exercise("Contract row","Dorso","Manubri","Intermedio",
            role="Forza",category=ExerciseCategory.PULL,resistanceMode=ResistanceMode.EXTERNAL_LOAD,
            setRestSec=90,exerciseRestSec=90,targetSets=3,targetValue=6.0)
        val effective=exerciseWithEffectivePrescription(ex,athlete,emptyList(),80)
        assertEquals(effective.setRestSec,effective.exerciseRestSec)
    }
}
