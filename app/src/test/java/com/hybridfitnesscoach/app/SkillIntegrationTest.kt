package com.hybridfitnesscoach.app
import org.junit.Assert.*
import org.junit.Test

class SkillIntegrationTest {
    @Test fun everySelectableSkillHasEvidenceGraph() {
        skillProgressionNodes.map{it.skill}.distinct().forEach { skill ->
            assertTrue("Missing graph: $skill",SkillCatalog.forSkill(skill).isNotEmpty())
            assertEquals(0,ProgressionEngine.skillProgress(SkillCatalog.progress(skill,emptyList())))
        }
    }
    @Test fun everyGraphNodeMapsToAnExecutableExercise() {
        SkillCatalog.nodes.forEach { node ->
            assertTrue("Missing exercise: ${node.exerciseName}",exercises.any{ex->(node.exerciseAliases+node.exerciseName).any{it.equals(ex.name,true)}})
        }
    }
    @Test fun newAthleteCannotGenerateLockedProgressions() {
        val gym=profiles.first{it.name=="Palestra"}
        val workout=generateWorkout(gym,AthleteProfile(goal="Calisthenics",level="Avanzato",prioritySkills=listOf("Front lever")),emptyList())
        assertFalse(workout.any{it.name=="Advanced tuck front lever"})
    }
    @Test fun catalogueHasConsistentMetadataAndNoDuplicateNames() {
        assertEquals(exercises.size,exercises.map{it.name.lowercase()}.distinct().size)
        assertTrue(exercises.all{it.movementPattern.isNotBlank()})
        assertTrue(exercises.all{it.category!=ExerciseCategory.OTHER})
        assertTrue(exercises.any{it.category==ExerciseCategory.MOBILITY && it.equipment=="Corpo libero"})
        assertEquals(ResistanceMode.EXTERNAL_LOAD,exercises.first{it.name=="Squat"}.resistanceMode)
    }
    @Test fun catalogHasBroadV09Coverage(){assertTrue(exercises.size>=200);assertEquals(exercises.size,exercises.map{it.name.lowercase()}.distinct().size);assertTrue(exercises.any{it.category==ExerciseCategory.CORE});assertTrue(exercises.any{it.category==ExerciseCategory.MOBILITY});assertTrue(exercises.any{it.category==ExerciseCategory.CARDIO});assertTrue(exercises.any{it.movementPattern=="loaded-carry"});assertTrue(exercises.any{it.role=="Conditioning"})}
}
