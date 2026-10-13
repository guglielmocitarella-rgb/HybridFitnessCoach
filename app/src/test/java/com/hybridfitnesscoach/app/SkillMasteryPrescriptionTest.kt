package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class SkillMasteryPrescriptionTest {
    @Test fun skillCandidateUsesCurrentNodeMasteryPrescription() {
        val gym=trainingLocations.first{it.name=="Palestra"}
        val athlete=AthleteProfile(assessedTrainingExperience=3,assessedPushCapacity=3,assessedPullCapacity=3)
        val first=SkillCatalog.forSkill("Muscle-up").first{it.id=="pullup_strict"}
        val pool=customWorkoutCandidatePool(gym,athlete,emptyList(),"Skill",listOf("Muscle-up"),emptyList())
        val focus=pool.first{ex -> (first.exerciseAliases+first.exerciseName).any{it.equals(ex.name,true)}}
        assertEquals(first.criterion.minSets,focus.targetSets)
        assertEquals(first.criterion.minReps!!.toDouble(),focus.targetValue!!,0.0)
    }

    @Test fun adaptiveReductionCannotDropSkillBelowMasteryCriterion() {
        val first=SkillCatalog.forSkill("Muscle-up").first{it.id=="pullup_strict"}
        val source=exercises.first{ex -> (first.exerciseAliases+first.exerciseName).any{it.equals(ex.name,true)}}.copy(
            targetSets=first.criterion.minSets,
            targetValue=first.criterion.minReps!!.toDouble()
        )
        val reduced=source.copy(targetSets=1,targetValue=1.0)
        val enforced=enforceSkillMasteryPrescription(listOf(reduced),listOf("Muscle-up"),emptyList()).single()
        assertTrue(enforced.targetSets>=first.criterion.minSets)
        assertEquals(first.criterion.minReps!!.toDouble(),enforced.targetValue!!,0.0)
    }

    @Test fun inProgressNodeKeepsItsOwnMasteryPrescriptionAndNextStaysLocked() {
        val first=SkillCatalog.forSkill("Muscle-up").first{it.id=="pullup_strict"}
        val attempt=SessionRecord(
            timestamp=10,profileName="Palestra",completedSets=3,totalSets=3,
            structuredExercises=listOf(WorkoutExerciseState(
                exerciseName=first.exerciseName,rpe=7,
                sets=(1..3).map{WorkoutSetState(it,actualReps=8,completed=true)}
            ))
        )
        val states=SkillCatalog.progress("Muscle-up",listOf(attempt))
        assertEquals(ProgressionStatus.IN_PROGRESS,states.first{it.node.id==first.id}.status)
        assertEquals(ProgressionStatus.LOCKED,states.first{it.node.id=="pullup_chest"}.status)
        val gym=trainingLocations.first{it.name=="Palestra"}
        val pool=customWorkoutCandidatePool(gym,AthleteProfile(assessedTrainingExperience=3,assessedPushCapacity=3,assessedPullCapacity=3),listOf(attempt),"Skill",listOf("Muscle-up"),emptyList())
        val focus=pool.first{ex -> (first.exerciseAliases+first.exerciseName).any{it.equals(ex.name,true)}}
        assertEquals(8.0,focus.targetValue!!,0.0)
        assertTrue(pool.none{it.name.equals("Chest-to-bar Pull-up",true) || it.name.equals("Chest to bar pull-up",true)})
    }
    @Test fun skillRegenerationCannotReplaceFocusWithLockedOrGenericProgression() {
        val gym=trainingLocations.first{it.name=="Palestra"}
        val athlete=AthleteProfile(assessedTrainingExperience=3,assessedPushCapacity=3,assessedPullCapacity=3)
        val skill="Muscle-up"
        val first=SkillCatalog.forSkill(skill).first{it.id=="pullup_strict"}
        val pool=customWorkoutCandidatePool(gym,athlete,emptyList(),"Skill",listOf(skill),emptyList())
        val focus=pool.first{ex -> (first.exerciseAliases+first.exerciseName).any{it.equals(ex.name,true)}}
        val original=listOf(focus)+pool.filter{it!=focus}.take(4)
        val regenerated=regenerateSkillWorkout(original,pool,emptySet(),coachingContext(emptyList()),listOf(skill),emptyList())
        assertTrue(regenerated.any{ex -> (first.exerciseAliases+first.exerciseName).any{it.equals(ex.name,true)}})
        assertTrue(regenerated.none{ex -> ex.name.equals("Chest-to-bar Pull-up",true) || ex.name.equals("Chest to bar pull-up",true)})
        val prescribed=regenerated.first{ex -> (first.exerciseAliases+first.exerciseName).any{it.equals(ex.name,true)}}
        assertEquals(first.criterion.minSets,prescribed.targetSets)
        assertEquals(first.criterion.minReps!!.toDouble(),prescribed.targetValue!!,0.0)
    }

}
