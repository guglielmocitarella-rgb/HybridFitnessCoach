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
        assertEquals(HomePlanStatus.TODAY,state.status)
        assertEquals("Sessione di oggi",state.primaryLabel)
    }

    @Test fun homeStateHasNoDuplicatedUiFallbackWhenPlanMissing() {
        val state=deriveHomePlanState(null,100L)
        assertEquals(false,state.active)
        assertNull(state.todayWorkout)
        assertNull(state.nextWorkout)
        assertEquals(HomePlanStatus.NO_PLAN,state.status)
        assertEquals("Nessun programma attivo",state.primaryLabel)
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

    @Test fun homeStateDistinguishesUpcomingFromFinishedPlan() {
        val today=100L
        val upcoming=deriveHomePlanState(TrainingPlan("p",today,2,1,listOf(workout("next",today+2))),today)
        assertEquals(HomePlanStatus.UPCOMING,upcoming.status)
        assertEquals("Nessuna sessione pianificata per oggi",upcoming.primaryLabel)
        assertEquals("Prossima sessione disponibile",upcoming.secondaryLabel)

        val finished=deriveHomePlanState(TrainingPlan("p",today-14,2,1,listOf(workout("done",today-1,PlannedWorkoutState.COMPLETED))),today)
        assertEquals(HomePlanStatus.NO_FUTURE,finished.status)
        assertEquals("Programma senza sessioni future",finished.primaryLabel)
        assertNull(finished.nextWorkout)
    }

    @Test fun adaptedSessionRemainsActionableOnHome() {
        val today=100L
        val state=deriveHomePlanState(TrainingPlan("p",today,1,1,listOf(workout("adapted",today,PlannedWorkoutState.ADAPTED))),today)
        assertEquals(HomePlanStatus.TODAY,state.status)
        assertEquals("adapted",state.todayWorkout?.id)
    }

    @Test fun homeAndAggregateLookupChooseSameDeterministicWorkoutForToday() {
        val today=100L
        val later=PlannedWorkout("later",today,1,2,emptyList(),intent=PlannedSessionIntent("Forza"))
        val earlier=PlannedWorkout("earlier",today,0,1,emptyList(),intent=PlannedSessionIntent("Forza"))
        val plan=TrainingPlan("p",today,2,2,listOf(later,earlier))
        assertEquals("earlier",deriveHomePlanState(plan,today).todayWorkout?.id)
        assertEquals("earlier",todayPlannedWorkouts(listOf(plan),today).first().second.id)
    }


    @Test fun emptyGeneratedPreviewHasNoSelectionAndNoScrollTarget() {
        val navigation=previewNavigationAfterGeneration(emptyList())
        assertNull(navigation.exerciseName)
        assertEquals(-1,navigation.scrollIndex)
    }

    @Test fun everyRegeneratedPreviewDerivesNavigationOnlyFromNewGeneration() {
        val oldFirst=Exercise("Old first","Push","Corpo libero","Principiante")
        val oldLast=Exercise("Old last","Pull","Corpo libero","Principiante")
        assertEquals("Old first",previewNavigationAfterGeneration(listOf(oldFirst,oldLast)).exerciseName)

        val newFirst=Exercise("New first","Core","Corpo libero","Principiante")
        val newLast=Exercise("New last","Gambe","Corpo libero","Principiante")
        val navigation=previewNavigationAfterGeneration(listOf(newFirst,newLast))
        assertEquals("New first",navigation.exerciseName)
        assertEquals(0,navigation.scrollIndex)
    }

    @Test fun pinnedExerciseKeepsItsOriginalPositionButPreviewStillStartsAtIndexZero() {
        val first=Exercise("First","Push","Corpo libero","Principiante")
        val pinned=Exercise("Pinned middle","Core","Corpo libero","Principiante")
        val oldLast=Exercise("Old last","Pull","Corpo libero","Principiante")
        val replacement=Exercise("Replacement","Pull","Corpo libero","Principiante")
        val regenerated=regenerateUnpinned(listOf(first,pinned,oldLast),listOf(replacement),setOf("Pinned middle"))
        assertSame(pinned,regenerated[1])
        val navigation=previewNavigationAfterGeneration(regenerated)
        assertEquals(regenerated.first().name,navigation.exerciseName)
        assertEquals(0,navigation.scrollIndex)
    }

}
