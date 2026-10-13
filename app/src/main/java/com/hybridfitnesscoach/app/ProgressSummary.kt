package com.hybridfitnesscoach.app

internal data class ProgressWeek(val startEpochDay:Long,val sessions:Int,val completedSets:Int,val volumeKg:Double)
internal data class ExerciseTrend(val name:String,val latestReps:Int?,val previousReps:Int?,val latestLoad:Double?,val previousLoad:Double?,val latestSeconds:Int?,val previousSeconds:Int?)
internal data class ProgressSummary(val weeks:List<ProgressWeek>,val exercises:List<ExerciseTrend>)

/** Calendar weeks and chronological comparisons use completed, observed sets only. */
internal fun progressSummary(sessions:List<SessionRecord>,today:Long):ProgressSummary {
    val monday=today-(java.time.LocalDate.ofEpochDay(today).dayOfWeek.value-1)
    val performed=sessions.filter{it.structuredExercises.any{e->e.sets.any{it.completed}}}
    val weeks=(-3L..0L).map{offset->
        val start=monday+offset*7
        val relevant=performed.filter{Math.floorDiv(it.timestamp,86400000L) in start..start+6}
        ProgressWeek(start,relevant.size,relevant.sumOf{s->s.structuredExercises.sumOf{e->e.sets.count{it.completed}}},relevant.sumOf{s->s.structuredExercises.sumOf(::loadedVolumeKg)})
    }
    val observed=performed.sortedByDescending{it.timestamp}.flatMap{s->
        s.structuredExercises.filter{e->e.sets.any{it.completed}}.groupBy{it.exerciseName}.map{(name,occurrences)->name to occurrences.flatMap{it.sets}.filter{it.completed}}
    }.groupBy({it.first},{it.second})
    val trends=observed.toSortedMap().map{(name,records)->
        val latest=records.first();val previous=records.getOrNull(1).orEmpty()
        ExerciseTrend(name,latest.mapNotNull{it.actualReps}.maxOrNull(),previous.mapNotNull{it.actualReps}.maxOrNull(),latest.mapNotNull{it.actualLoadKg}.maxOrNull(),previous.mapNotNull{it.actualLoadKg}.maxOrNull(),latest.mapNotNull{it.actualSeconds}.maxOrNull(),previous.mapNotNull{it.actualSeconds}.maxOrNull())
    }
    return ProgressSummary(weeks,trends)
}

internal fun calendarWeekState(plan:TrainingPlan,anchor:Long,today:Long):List<CalendarDayState> {
    val monday=anchor-(java.time.LocalDate.ofEpochDay(anchor).dayOfWeek.value-1)
    return (0L..6L).map{offset->val day=monday+offset;CalendarDayState(day,true,day==today,plan.workouts.filter{it.epochDay==day}.sortedBy{it.sequenceInWeek})}
}
