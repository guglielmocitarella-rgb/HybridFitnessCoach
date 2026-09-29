package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class Rc2PlanningRegressionTest {
    private fun plan() = TrainingPlanEngine.scheduleImmediately("rc2", 1000, 2, 3) { _, _ -> emptyList() }

    @Test fun skippedTargetCanBeRescheduledWithoutLosingIdentity() {
        val original = plan()
        val skipped = TrainingPlanEngine.skip(original, original.workouts.first().id)
        val moved = TrainingPlanEngine.reschedule(skipped, original.workouts.first().id, 1008)
        assertEquals(PlannedWorkoutState.RESCHEDULED, moved.workouts.first().state)
        assertEquals(1000L, moved.workouts.first().originalEpochDay)
        assertEquals(original.workouts.map { it.id }, moved.workouts.map { it.id })
        assertTrue(moved.workouts.zipWithNext().all { (a,b) -> a.epochDay < b.epochDay })
    }

    @Test fun movingFirstSessionAcrossWeekPreservesSequenceSpacingAndIdentity() {
        val original = plan()
        val moved = TrainingPlanEngine.reschedule(original, original.workouts[0].id, 1008, 0)
        assertEquals(listOf(1008L, 1010L, 1012L, 1015L, 1017L, 1019L), moved.workouts.map { it.epochDay })
        assertEquals(original.workouts.map { it.id }, moved.workouts.map { it.id })
        assertEquals(original.workouts.map { it.originalEpochDay }, moved.workouts.map { it.originalEpochDay })
    }

    @Test fun movingOneDayPreservesPrescribedSpacing() {
        val original = plan()
        val moved = TrainingPlanEngine.reschedule(original, original.workouts[0].id, 1001)
        assertEquals(listOf(1001L, 1003L, 1005L, 1008L, 1010L, 1012L), moved.workouts.map { it.epochDay })
    }

    @Test fun movingEarlierCannotOvertakePreviousPendingSession() {
        val original = plan()
        val moved = TrainingPlanEngine.reschedule(original, original.workouts[1].id, 999)
        assertEquals(1002L, moved.workouts[1].epochDay)
        assertEquals(original.workouts.first(), moved.workouts.first())
    }

    @Test fun completedSessionsStayImmutableDuringCascade() {
        val original = plan().let { p -> p.copy(workouts = p.workouts.mapIndexed { i, w ->
            if (i == 1) w.copy(state = PlannedWorkoutState.COMPLETED, actualExecutionEpochDay = 1002) else w
        }) }
        val moved = TrainingPlanEngine.reschedule(original, original.workouts[0].id, 1008)
        assertEquals(original.workouts[1], moved.workouts.first { it.id == original.workouts[1].id })
        val pending = moved.workouts.filter { it.state != PlannedWorkoutState.COMPLETED }
        assertTrue(pending.zipWithNext().all { (a, b) -> a.epochDay < b.epochDay })
    }

    @Test fun deletedPartialSessionIsCountedAsPendingEvenWhenItsDateHasPassed() {
        val original = plan()
        val first = original.workouts.first()
        val session = SessionRecord(1, "Palestra", 1, 3, plannedWorkoutId = first.id)
        val partial = PlannedWorkoutReconciler.complete(original, session, 1000, 0).plan
        val recovered = PlannedWorkoutReconciler.afterDeletion(partial, session, emptyList())
        assertEquals(6, coachingContext(emptyList(), null, recovered, 1001).upcomingSessions)
        assertEquals(0, coachingContext(emptyList(), null, recovered, 1001).planDisruptions)
    }

    @Test fun firstPendingSessionCanMoveEarlierWithoutLosingIdentity() {
        val original = plan()
        val moved = TrainingPlanEngine.reschedule(original, original.workouts.first().id, 998, 0)
        assertEquals(998L, moved.workouts.first().epochDay)
        assertEquals(original.workouts.map { it.id }, moved.workouts.map { it.id })
        assertEquals(original.workouts.map { it.originalEpochDay }, moved.workouts.map { it.originalEpochDay })
    }

    @Test fun rescheduledSessionCanRestoreItsOriginalDate() {
        val original = plan()
        val first = original.workouts.first()
        val moved = TrainingPlanEngine.reschedule(original, first.id, 1001, 0)
        assertEquals(1001L, moved.workouts.first().epochDay)
        val restored = TrainingPlanEngine.restoreOriginalDate(moved, first.id, 0)
        assertEquals(first.originalEpochDay, restored.workouts.first().epochDay)
        assertEquals(PlannedWorkoutState.PLANNED, restored.workouts.first().state)
    }
    @Test fun futureSkippedWorkoutRemainsInFutureCalendar() {
        val future = plan().workouts.first().copy(epochDay = 1010)
        val skipped = future.copy(state = PlannedWorkoutState.SKIPPED)
        assertFalse(isPastCalendarWorkout(skipped, 1000))
        assertTrue(isPastCalendarWorkout(skipped, 1011))
        assertTrue(isPastCalendarWorkout(future.copy(state = PlannedWorkoutState.PARTIAL), 1000))
        assertFalse(isPastCalendarWorkout(future, 1000))
    }

}
