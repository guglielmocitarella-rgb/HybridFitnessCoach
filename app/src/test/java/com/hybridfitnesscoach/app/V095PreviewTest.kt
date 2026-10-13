package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class V095PreviewTest {
    private val selected=Exercise("Chosen","Petto","Corpo libero","Principiante",category=ExerciseCategory.PUSH,movementPattern="horizontal_push")
    private fun flow():WorkoutLaunchFlow = materializeWorkoutLaunch(selectWorkoutLocation(beginWorkoutLaunch(WorkoutLaunchIntent(WorkoutOrigin.CUSTOM)),"home"),
        WorkoutSessionState("preview",1000,"Home",listOf(plannedExerciseState(0,selected).copy(pinned=true),plannedExerciseState(1,selected)),trainingLocationId="home"))
    @Test fun replacementPreservesUntouchedOccurrenceAndOwnership() {
        val before=flow()
        val after=replacePreviewExercise(before,1,selected.copy(name="Alternative"))
        assertEquals(before.workout!!.exercises[0],after.workout!!.exercises[0])
        assertEquals(1,after.workout!!.exercises[1].originalOrder)
        assertEquals("home",after.workout!!.trainingLocationId)
    }
    @Test fun lockedOccurrenceCannotBeRemovedOrReplaced() {
        val before=flow()
        assertEquals(before,removePreviewExercise(before,0))
        assertEquals(before,replacePreviewExercise(before,0,selected.copy(name="Other")))
    }
    @Test fun removingDuplicateNameAffectsOnlyTheSelectedOccurrence() {
        val after=removePreviewExercise(flow(),1)
        assertEquals(1,after.workout!!.exercises.size)
        assertTrue(after.workout!!.exercises.single().pinned)
    }
    @Test fun replacementKeepsLibraryEquipmentFiltersWithoutSkillFocus() {
        val source=selected.copy(equipment="Manubri")
        val dumbbell=source.copy(name="Dumbbell alternative")
        val barbell=source.copy(name="Barbell alternative",equipment="Bilanciere")
        val place=trainingLocations.first{it.kind==TrainingLocationKind.GYM}
        val intent=WorkoutLaunchIntent(WorkoutOrigin.LIBRARY,equipmentFilters=listOf("Manubri"),lockFilters=true)
        assertEquals(listOf(dumbbell),previewAlternatives(source,listOf(dumbbell,barbell),place,AthleteProfile(),emptyList(),intent))
    }
    @Test fun alternativesRejectWrongMovementEquipmentAndSkill() {
        val place=TrainingLocation("home","Home","",TrainingLocationKind.HOME,setOf(EquipmentId.BODYWEIGHT))
        val good=selected.copy(name="Good")
        val bad=selected.copy(name="Wrong",category=ExerciseCategory.PULL,movementPattern="horizontal_pull")
        val loaded=selected.copy(name="Bar",equipment="Bilanciere")
        assertEquals(listOf(good),previewAlternatives(selected,listOf(good,bad,loaded),place,AthleteProfile(),emptyList(),WorkoutLaunchIntent(WorkoutOrigin.CUSTOM)))
    }
}
