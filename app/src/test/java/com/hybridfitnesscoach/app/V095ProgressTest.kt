package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class V095ProgressTest {
    private fun record(day:Long,reps:Int?,load:Double?,completed:Boolean=true):SessionRecord {
        val e=WorkoutExerciseState("Press",sets=listOf(WorkoutSetState(0,targetReps=99,targetLoadKg=999.0,actualReps=reps,actualLoadKg=load,completed=completed)))
        return runnerSessionRecord(WorkoutSessionState("s$day",day*86400000,"Gym",listOf(e)),day*86400000)
    }
    @Test fun trendsUseObservedPerformanceAndChronologicalOrder() {
        val result=progressSummary(listOf(record(20,8,12.0),record(10,5,10.0)),21)
        val trend=result.exercises.single()
        assertEquals(8,trend.latestReps)
        assertEquals(5,trend.previousReps)
        assertEquals(12.0,trend.latestLoad!!,0.0)
        assertEquals(146.0,result.weeks.sumOf{it.volumeKg},0.0)
    }
    @Test fun pendingTargetsCannotCountAsProgress() {
        val result=progressSummary(listOf(record(20,null,null,false)),21)
        assertTrue(result.exercises.isEmpty())
        assertEquals(0,result.weeks.sumOf{it.sessions})
        assertEquals(0.0,result.weeks.sumOf{it.volumeKg},0.0)
    }
    @Test fun weekGridCrossesYearWithoutLosingOwnership() {
        val monday=java.time.LocalDate.of(2026,12,28).toEpochDay()
        val plan=generateTrainingPlan("p",monday,PlanConfiguration.defaults())
        val days=calendarWeekState(plan,monday+6,monday)
        assertEquals(7,days.size)
        assertEquals(monday,days.first().epochDay)
        assertEquals(monday+6,days.last().epochDay)
        assertTrue(days.flatMap{it.workouts}.all{it.id.startsWith("p-")})
    }
}
