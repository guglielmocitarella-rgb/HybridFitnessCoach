package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class RecoveryPrescriptionContractTest {
    private val athlete=AthleteProfile(level="Intermedio",assessedTrainingExperience=2,assessedPushCapacity=2,assessedPullCapacity=2,goal="Forza")

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
    @Test fun customCoachingThenEffectivePrescriptionPropagatesSourceRecoveryWithoutLosingFallbackAdaptation() {
        val source=Exercise("Custom source press","Petto","Bilanciere","Intermedio",
            category=ExerciseCategory.PUSH,resistanceMode=ResistanceMode.EXTERNAL_LOAD,
            setRestSec=75,targetSets=3,targetValue=6.0)
        val fallback=Exercise("Custom fallback run","Cardio","Corpo libero","Principiante",
            category=ExerciseCategory.CARDIO,resistanceMode=ResistanceMode.BODYWEIGHT,
            measure=ExerciseMeasure.DISTANCE,setRestSec=45,targetSets=2,targetValue=1000.0)
        val coaching=coachingContext(emptyList(),30)
        val coached=applyCoaching(listOf(source,fallback),coaching)
        val effective=effectiveAutomaticExercises(coached,athlete,emptyList(),30)
        assertTrue(effective.first().setRestSec>=180)
        assertEquals(coached[1].targetSets,effective[1].targetSets)
        assertEquals(coached[1].targetValue,effective[1].targetValue)
        assertEquals(45,effective[1].setRestSec)
    }

    @Test fun installedSourceGuidanceConstrainsRecoveryAndCarriesProvenance() {
        val previous=ExerciseDbV2.installedProgrammingGuidance()
        try {
            ExerciseDbV2.installProgrammingGuidance(listOf(
                ExerciseV2Guidance("strength-rest","NSCA_ESS5","max_strength:resistance_training",
                    mapOf("min_seconds" to (240..240),"max_seconds" to (300..300)))
            ))
            val ex=Exercise("Guidance press","Petto","Bilanciere","Intermedio",
                category=ExerciseCategory.PUSH,resistanceMode=ResistanceMode.EXTERNAL_LOAD)
            val context=PrescriptionContext("Forza",2,5,null,80)
            val baseline=SourcePrescriptionBaselineRegistry.baselineFor(ex,context)!!
            assertEquals(240,baseline.restSeconds)
            assertEquals("NSCA_ESS5",baseline.evidence.single().sourceId)
        } finally {
            ExerciseDbV2.installProgrammingGuidance(previous)
        }
    }

}
