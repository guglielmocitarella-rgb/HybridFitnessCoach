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
        val recovered = PlannedWorkoutReconciler.afterDeletion(partial, session, emptyList()).let { p -> p.copy(workouts=p.workouts.map { w -> w.copy(exercises=emptyList(),intent=PlannedSessionIntent("Forza",sequenceInWeek=w.sequenceInWeek)) }) }
        assertEquals(6, coachingContext(emptyList(), null, listOf(recovered), 1001).upcomingSessions)
        assertEquals(0, coachingContext(emptyList(), null, listOf(recovered), 1001).planDisruptions)
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
    @Test fun restoringRescheduledSessionRestoresFollowingPendingSuffix() {
        val original = plan()
        val moved = TrainingPlanEngine.reschedule(original, original.workouts[0].id, 1008, 0)
        val restored = TrainingPlanEngine.restoreOriginalDate(moved, original.workouts[0].id, 0)
        assertEquals(original.workouts.map { it.epochDay }, restored.workouts.map { it.epochDay })
        assertEquals(original.workouts.map { PlannedWorkoutState.PLANNED }, restored.workouts.map { it.state })
    }

    @Test fun restoringThirdSessionLeavesEarlierSessionsAndRestoresLaterSuffix() {
        val original = plan()
        val moved = TrainingPlanEngine.reschedule(original, original.workouts[2].id, 1012, 0)
        val restored = TrainingPlanEngine.restoreOriginalDate(moved, original.workouts[2].id, 0)
        assertEquals(original.workouts.take(2), restored.workouts.take(2))
        assertEquals(original.workouts.drop(2).map { it.epochDay }, restored.workouts.drop(2).map { it.epochDay })
    }

    @Test fun calendarKeepsFutureSkippedAndPartialWorkoutsOnTheirScheduledDate() {
        val future = plan().workouts.first().copy(epochDay = 1010)
        val skipped = future.copy(state = PlannedWorkoutState.SKIPPED)
        val partial = future.copy(id = "partial", state = PlannedWorkoutState.PARTIAL)
        val calendarPlan = plan().copy(workouts = listOf(skipped, partial))
        val date = java.time.LocalDate.ofEpochDay(1010)
        val month = calendarMonthState(calendarPlan, date.year, date.monthValue, 1000)
        val day = month.days.first { it.epochDay == 1010L }
        assertEquals(listOf(skipped, partial), day.workouts)
        assertEquals(setOf(PlannedWorkoutState.SKIPPED, PlannedWorkoutState.PARTIAL), day.states)
    }

    @Test fun calendarLaunchNeverReplacesAnActiveWorkout() {
        val requested=plan().workouts.first()
        val active=WorkoutSessionState("active-1",1234,"Palestra",listOf(WorkoutExerciseState("Push-up")),plannedWorkoutId="other-plan")
        val resolved=resolveWorkoutLaunch(active,requested)
        assertTrue(resolved.resumeActive)
        assertNull(resolved.plannedWorkout)
    }

    @Test fun calendarLaunchPreservesRequestedWorkoutIdentityWhenNoActiveWorkoutExists() {
        val requested=plan().workouts[1]
        val resolved=resolveWorkoutLaunch(null,requested)
        assertFalse(resolved.resumeActive)
        assertEquals(requested.id,resolved.plannedWorkout?.id)
        assertEquals(requested.intent,resolved.plannedWorkout?.intent)
    }

    @Test fun completedOrPartialWorkoutCannotBeCompletedAgainThroughReconciler() {
        val original=plan()
        val target=original.workouts.first()
        val session=SessionRecord(1,"Palestra",3,3,plannedWorkoutId=target.id)
        val once=PlannedWorkoutReconciler.complete(original,session,target.epochDay,0).plan
        val twice=PlannedWorkoutReconciler.complete(once,session.copy(timestamp=2),target.epochDay+1,0)
        assertEquals(once,twice.plan)
        assertTrue(twice.decisions.isEmpty())
    }

    @Test fun fullFiftyTwoWeekPlanStoresIntentAndMaterializesAgainstExecutionProfile() {
        val profile=trainingLocations.first{it.name=="Palestra"}
        val athlete=AthleteProfile(goal="Calisthenics",assessedTrainingExperience=2,assessedPushCapacity=2,assessedPullCapacity=2,daysPerWeek=7,prioritySkills=listOf("Front lever"))
        val plan=generateTrainingPlan("year-profile",20_000,PlanConfiguration("Calisthenics",52,7,(1..7).toSet(),listOf("Front lever")))
        assertEquals(364,plan.workouts.size)
        assertTrue(plan.workouts.all{it.exercises.isEmpty() && it.intent!=null})
        val capability=athlete.toCapabilityProfile()
        plan.workouts.take(14).forEach { workout ->
            val realized=materializePlannedSession(workout,profile,athlete,emptyList(),80)
            assertTrue(realized.exercises.isNotEmpty())
            assertTrue(realized.exercises.all{locationCompatible(it,profile)})
            assertTrue(realized.exercises.all{levelScore(it.level)<=capabilityLevelFor(it,capability)})
        }
    }

    @Test fun failedCalendarPersistenceCannotExposeCandidatePlan() {
        val current=plan()
        val candidate=TrainingPlanEngine.skip(current,current.workouts.first().id)
        assertEquals(current,committedPlanOrCurrent(current,candidate,false))
        assertEquals(PlannedWorkoutState.PLANNED,committedPlanOrCurrent(current,candidate,false).workouts.first().state)
    }

    @Test fun successfulCalendarPersistenceExposesCandidatePlan() {
        val current=plan()
        val candidate=TrainingPlanEngine.skip(current,current.workouts.first().id)
        val visible=committedPlanOrCurrent(current,candidate,true)
        assertEquals(candidate,visible)
        assertEquals(PlannedWorkoutState.SKIPPED,visible.workouts.first().state)
    }

    @Test fun multiWeekPlanUsesConservativeThreePlusOneProgressionWithoutChangingExercises() {
        val reps=Exercise("Progress reps","Core","Corpo libero","Principiante",targetValue=10.0)
        val sets=Exercise("Progress sets","Core","Corpo libero","Principiante",targetSets=3,targetValue=8.0)
        val distance=Exercise("Progress distance","Cardio","Corpo libero","Principiante",measure=ExerciseMeasure.DISTANCE,targetValue=1000.0)
        val base=listOf(reps,sets,distance)
        val w0=plannedWeekExercises(base,0)
        val w1=plannedWeekExercises(base,1)
        val w2=plannedWeekExercises(base,2)
        val w3=plannedWeekExercises(base,3)
        assertEquals(base.map{it.name},w2.map{it.name})
        assertEquals(10.0,w0[0].targetValue!!,0.001)
        assertEquals(10.25,w1[0].targetValue!!,0.001)
        assertEquals(10.5,w2[0].targetValue!!,0.001)
        assertEquals(10.0,w3[0].targetValue!!,0.001)
        assertEquals(3,w0[1].targetSets)
        assertEquals(3,w1[1].targetSets)
        assertEquals(4,w2[1].targetSets)
        assertEquals(3,w3[1].targetSets)
        assertEquals(1000.0,w2[2].targetValue!!,0.001)
    }

    @Test fun weekProgressionRepeatsDeterministicallyAcrossLongPlans() {
        val ex=Exercise("Progress","Core","Corpo libero","Principiante",targetValue=20.0)
        assertEquals(plannedWeekExercises(listOf(ex),0),plannedWeekExercises(listOf(ex),4))
        assertEquals(plannedWeekExercises(listOf(ex),1),plannedWeekExercises(listOf(ex),5))
        assertEquals(plannedWeekExercises(listOf(ex),2),plannedWeekExercises(listOf(ex),50))
        assertEquals(plannedWeekExercises(listOf(ex),3),plannedWeekExercises(listOf(ex),51))
    }

    @Test fun homeSuggestionIdentifiesTodayPlanWithoutPreMaterializedExercises() {
        val original=plan()
        val today=original.workouts.first().epochDay
        val adaptiveExercise=Exercise(name="Adaptive exercise", group="Schiena", equipment="Nessuna", level="Intermedio")

        val suggestion=resolveSuggestedWorkout(original,today){listOf(adaptiveExercise)}

        assertEquals(original.workouts.first().id,suggestion.plannedWorkout?.id)
        assertTrue(suggestion.exercises.isEmpty())
    }

    @Test fun homeSuggestionFallsBackToAdaptiveWhenTodayPlanIsNotRunnable() {
        val original=plan()
        val today=original.workouts.first().epochDay
        val adaptiveExercise=Exercise(name="Adaptive exercise", group="Schiena", equipment="Nessuna", level="Intermedio")
        val completed=original.copy(workouts=original.workouts.mapIndexed { i,w -> if(i==0) w.copy(state=PlannedWorkoutState.COMPLETED) else w })

        val suggestion=resolveSuggestedWorkout(completed,today){listOf(adaptiveExercise)}

        assertNull(suggestion.plannedWorkout)
        assertEquals(listOf("Adaptive exercise"),suggestion.exercises.map{it.name})
    }

    @Test fun homeLaunchSelectsOnlyRunnableWorkoutScheduledForToday() {
        val original=plan()
        val today=original.workouts.first().epochDay
        assertEquals(original.workouts.first().id,todayPlannedWorkout(original,today)?.id)
        val completed=original.copy(workouts=original.workouts.mapIndexed{i,w->if(i==0)w.copy(state=PlannedWorkoutState.COMPLETED) else w})
        assertNull(todayPlannedWorkout(completed,today))
        val skipped=original.copy(workouts=original.workouts.mapIndexed{i,w->if(i==0)w.copy(state=PlannedWorkoutState.SKIPPED) else w})
        assertNull(todayPlannedWorkout(skipped,today))
        val partial=original.copy(workouts=original.workouts.mapIndexed{i,w->if(i==0)w.copy(state=PlannedWorkoutState.PARTIAL) else w})
        assertNull(todayPlannedWorkout(partial,today))
        assertNull(todayPlannedWorkout(original,today+1))
    }

}
