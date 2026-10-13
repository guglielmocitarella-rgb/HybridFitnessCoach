package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class V095CapabilityTest {
    private fun exercise(category:ExerciseCategory,pattern:String,measure:ExerciseMeasure=ExerciseMeasure.REPS)=
        Exercise("Assessment","Core","Corpo libero","Avanzato",category=category,movementPattern=pattern,measure=measure)
    @Test fun strongPushAndPullDoNotPromoteUnassessedAxes() {
        val c=AthleteProfile(assessedTrainingExperience=3,assessedPushCapacity=3,assessedPullCapacity=3).toCapabilityProfile()
        assertEquals(1,capabilityLevelFor(exercise(ExerciseCategory.PUSH,"vertical-push"),c))
        assertEquals(1,capabilityLevelFor(exercise(ExerciseCategory.PULL,"horizontal-pull"),c))
        assertEquals(1,capabilityLevelFor(exercise(ExerciseCategory.LEGS,"hinge"),c))
        assertEquals(1,capabilityLevelFor(exercise(ExerciseCategory.LEGS,"knee-dominant"),c))
        assertEquals(1,capabilityLevelFor(exercise(ExerciseCategory.CORE,"core-control"),c))
    }
    @Test fun anAssessedAxisWorksWithoutRequiringEveryOtherAssessment() {
        val c=AthleteProfile(assessedHorizontalPullCapacity=3).toCapabilityProfile()
        assertEquals(3,capabilityLevelFor(exercise(ExerciseCategory.PULL,"horizontal-pull"),c))
        assertEquals(1,capabilityLevelFor(exercise(ExerciseCategory.PULL,"vertical-pull"),c))
    }
    @Test fun timedSkillRequiresItsIsometricAndMovementCapabilities() {
        val c=AthleteProfile(assessedSkillCapacity=3,assessedVerticalPushCapacity=3,assessedIsometricCapacity=1).toCapabilityProfile()
        assertEquals(1,capabilityLevelFor(exercise(ExerciseCategory.SKILL,"vertical-push",ExerciseMeasure.SECONDS),c))
    }
    @Test fun skillDoesNotPromoteUnassessedSquatOrCore() {
        val c=AthleteProfile(assessedSkillCapacity=3).toCapabilityProfile()
        assertEquals(1,capabilityLevelFor(exercise(ExerciseCategory.SKILL,"knee-dominant"),c))
        assertEquals(1,capabilityLevelFor(exercise(ExerciseCategory.SKILL,"core-control"),c))
    }
    @Test fun externalLoadUsesItsMovementDimensionRatherThanGeneralExperience() {
        val c=AthleteProfile(assessedTrainingExperience=3,assessedPushCapacity=1).toCapabilityProfile()
        val ex=exercise(ExerciseCategory.PUSH,"horizontal-push").copy(resistanceMode=ResistanceMode.EXTERNAL_LOAD)
        assertEquals(1,capabilityLevelFor(ex,c))
    }
}
