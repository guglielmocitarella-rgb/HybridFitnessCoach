package com.hybridfitnesscoach.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class F10F11RegressionTest {
    private fun workout(id:String,day:Long,state:PlannedWorkoutState=PlannedWorkoutState.PLANNED,sequence:Int=0) =
        PlannedWorkout(id,day,0,sequence,emptyList(),state)

    @Test fun homeStateDerivesTodayAndNextFromPlanDomain() {
        val today=100L
        val plan=TrainingPlan(
            id="p",startEpochDay=today,weeks=2,daysPerWeek=3,
            workouts=listOf(
                workout("done",today,PlannedWorkoutState.COMPLETED),
                workout("today",today,PlannedWorkoutState.RESCHEDULED,1),
                workout("skipped",today+1,PlannedWorkoutState.SKIPPED),
                workout("next",today+2)
            )
        )
        val state=deriveHomePlanState(plan,today)
        assertEquals(true,state.active)
        assertEquals("today",state.todayWorkout?.id)
        assertEquals("next",state.nextWorkout?.id)
    }

    @Test fun homeStateHasNoDuplicatedUiFallbackWhenPlanMissing() {
        val state=deriveHomePlanState(null,100L)
        assertEquals(false,state.active)
        assertNull(state.todayWorkout)
        assertNull(state.nextWorkout)
    }

    @Test fun newPreviewAlwaysStartsFromFirstGeneratedExercise() {
        val first=Exercise("A","Push","Corpo libero","Principiante")
        val second=Exercise("B","Pull","Corpo libero","Principiante")
        val navigation=previewNavigationAfterGeneration(listOf(first,second))
        assertEquals("A",navigation.exerciseName)
        assertEquals(0,navigation.scrollIndex)
    }

    @Test fun regenerationPreservesPinnedExerciseWhilePreviewNavigationResets() {
        val pinned=Exercise("Pinned","Push","Corpo libero","Principiante")
        val old=Exercise("Old","Pull","Corpo libero","Principiante")
        val replacement=Exercise("Replacement","Pull","Corpo libero","Principiante")
        val regenerated=regenerateUnpinned(listOf(pinned,old),listOf(replacement),setOf("Pinned"))
        assertSame(pinned,regenerated.first())
        assertEquals("Replacement",regenerated[1].name)
        val navigation=previewNavigationAfterGeneration(regenerated)
        assertEquals("Pinned",navigation.exerciseName)
        assertEquals(0,navigation.scrollIndex)
    }
}
