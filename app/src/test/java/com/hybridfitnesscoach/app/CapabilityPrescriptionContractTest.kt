package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class CapabilityPrescriptionContractTest {
    @Test fun unassessedProfileFailsClosedInsteadOfTrustingLegacyLevel() {
        val capability=AthleteProfile(level="Avanzato").toCapabilityProfile()
        assertFalse(capability.assessed)
        assertEquals(1,capability.exerciseLevel)
    }

    @Test fun assessedProfileDerivesOverallCapabilityWithoutLegacyLevel() {
        val beginnerLabel=AthleteProfile(level="Principiante",assessedTrainingExperience=3,assessedPushCapacity=1,assessedPullCapacity=3).toCapabilityProfile()
        val advancedLabel=AthleteProfile(level="Avanzato",assessedTrainingExperience=3,assessedPushCapacity=1,assessedPullCapacity=3).toCapabilityProfile()
        assertEquals(beginnerLabel,advancedLabel)
        assertTrue(beginnerLabel.assessed)
    }

    @Test fun bodyweightPushAndPullUseSeparateAssessedCapacity() {
        val capability=CapabilityProfile(exerciseLevel=2,trainingExperience=2,pushCapacity=1,pullCapacity=3,assessed=true)
        val push=Exercise("Capability push","Petto","Corpo libero","Avanzato",category=ExerciseCategory.PUSH,resistanceMode=ResistanceMode.BODYWEIGHT)
        val pull=Exercise("Capability pull","Dorso","Corpo libero","Avanzato",category=ExerciseCategory.PULL,resistanceMode=ResistanceMode.BODYWEIGHT)
        assertEquals(1,capabilityLevelFor(push,capability))
        assertEquals(3,capabilityLevelFor(pull,capability))
    }

    @Test fun externalResistanceUsesTrainingExperienceNotLegacyLevel() {
        val ex=Exercise("Capability press","Petto","Bilanciere","Intermedio",category=ExerciseCategory.PUSH,resistanceMode=ResistanceMode.EXTERNAL_LOAD)
        val low=AthleteProfile(level="Principiante",goal="Forza",assessedTrainingExperience=3,assessedPushCapacity=1,assessedPullCapacity=1)
        val high=low.copy(level="Avanzato")
        assertEquals(effectiveRunnerPrescription(ex,low,emptyList(),80),effectiveRunnerPrescription(ex,high,emptyList(),80))
        assertEquals(3,capabilityLevelFor(ex,low.toCapabilityProfile()))
    }
}
