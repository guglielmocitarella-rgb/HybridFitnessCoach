package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

/** Release-gate regressions for v0.9 that exercise only local deterministic code. */
class V09RegressionAuditTest {
    @Test fun generationIsDeterministicAndOffline() {
        val gym=trainingLocations.first{it.name=="Palestra"}
        val athlete=AthleteProfile(goal="Calisthenics",assessedTrainingExperience=2,assessedPushCapacity=2,assessedPullCapacity=2,prioritySkills=listOf("Front lever"))
        val a=generateWorkout(gym,athlete,emptyList(),readinessScore=80).map{it.name}
        val b=generateWorkout(gym,athlete,emptyList(),readinessScore=80).map{it.name}
        assertEquals(a,b); assertEquals(6,a.size)
    }
    @Test fun v08StrengthPathRemainsAvailable() {
        val gym=trainingLocations.first{it.name=="Palestra"}
        val workout=generateWorkout(gym,AthleteProfile(goal="Forza",assessedTrainingExperience=2,assessedPushCapacity=2,assessedPullCapacity=2),emptyList())
        assertEquals(6,workout.size); assertTrue(workout.any{it.role=="Forza"})
    }
    @Test fun readinessChangesAdaptivePrescriptionWithoutNetworkState() {
        val fresh=adaptiveWorkoutPlan(0.9,7.0,90,90)
        val tired=adaptiveWorkoutPlan(0.9,7.0,25,30)
        assertNotEquals(fresh.decision,tired.decision); assertTrue(tired.volumeFactor<fresh.volumeFactor)
    }
    @Test fun catalogReleaseGateIsBroadAndUnique() {
        assertTrue("v0.9 catalog must stay near the original ~500 target", exercises.size>=500)
        assertEquals(exercises.size,exercises.map{it.name.lowercase()}.distinct().size)
        assertTrue(exercises.any{it.equipment=="Ab Wheel"}); assertTrue(exercises.any{it.equipment=="Foam Roller"})
    }
    @Test fun readinessBandsAreStableAtUxBoundaries(){assertEquals("RECUPERO",readinessBand(44));assertEquals("CAUTELA",readinessBand(45));assertEquals("BUONO",readinessBand(60));assertEquals("PRONTO",readinessBand(80))}
    @Test fun homeNextUnlockIsExplicitAndEvidenceBased(){val states=SkillCatalog.progress("Front lever",emptyList());val label=nextUnlockLabel(states);assertTrue(label.startsWith("Prossimo sblocco:"));assertTrue(label.contains("sessioni"));assertTrue(label.contains("Tuck front lever"))}
    @Test fun capabilityDimensionsHaveValidatedIndependentBounds() {
CapabilityDimension.entries.forEach{d->
 assertEquals(1,AthleteProfile().withCapability(d,1).toCapabilityProfile().levelFor(d))
 assertEquals(3,AthleteProfile().withCapability(d,3).toCapabilityProfile().levelFor(d))
 assertThrows(IllegalArgumentException::class.java){AthleteProfile().withCapability(d,4)}
 }
 }

    @Test fun movementAssessmentMapsToItsDifficultyCeiling() {
val ex=Exercise("Push test","Petto","Corpo libero","Avanzato",category=ExerciseCategory.PUSH,movementPattern="horizontal-push")
 (1..3).forEach{level->val c=AthleteProfile(assessedPushCapacity=level).toCapabilityProfile();assertEquals(level,capabilityLevelFor(ex,c))}
 assertEquals(1,capabilityLevelFor(ex,AthleteProfile(assessedTrainingExperience=3).toCapabilityProfile()))
 }

}

