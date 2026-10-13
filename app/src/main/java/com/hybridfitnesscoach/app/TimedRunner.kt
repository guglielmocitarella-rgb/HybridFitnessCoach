package com.hybridfitnesscoach.app

data class TimedSetState(
    val exerciseIndex:Int,
    val setIndex:Int,
    val startsAtEpochMs:Long,
    val targetSeconds:Int,
    val loadKg:Double?=null,
    val assistanceKg:Double?=null
)

internal object TimedRunner {
    fun start(state:WorkoutSessionState,now:Long,afterRest:Boolean=false,loadKg:Double?=null,assistanceKg:Double?=null):WorkoutSessionState {
        if(state.pausedAtEpochMs!=null || state.timedSet!=null)return state
        val i=state.currentExerciseIndex
        val e=state.exercises.getOrNull(i) ?: return state
        if(e.status==WorkoutExerciseStatus.SKIPPED || e.measure !in setOf(ExerciseMeasure.SECONDS,ExerciseMeasure.DURATION,ExerciseMeasure.INTERVALS))return state
        val j=e.sets.indexOfFirst{!it.completed};val target=e.sets.getOrNull(j)?.targetSeconds ?: return state
        if(target<=0 || listOfNotNull(loadKg,assistanceKg).any{!it.isFinite() || it<0})return state
        val begins=if(afterRest)state.restEndsAtEpochMs ?: now+5000L else now+5000L
        return state.copy(timedSet=TimedSetState(i,j,begins,target,loadKg,assistanceKg))
    }
    fun observedResistance(state:WorkoutSessionState,index:Int,load:Double?,assistance:Double?):WorkoutSessionState {
        val clock=state.timedSet ?: return state
        if(clock.exerciseIndex!=index || listOfNotNull(load,assistance).any{!it.isFinite() || it<0})return state
        return state.copy(timedSet=clock.copy(loadKg=load,assistanceKg=assistance))
    }
    fun preparationSeconds(state:WorkoutSessionState,now:Long):Int {
        val clock=state.timedSet ?: return 0
        val remaining=clock.startsAtEpochMs-(state.pausedAtEpochMs ?: now)
        return if(remaining in 1..5000)((remaining+999)/1000).toInt() else 0
    }
    fun executedSeconds(state:WorkoutSessionState,now:Long):Int {
        val clock=state.timedSet ?: return 0
        return (((state.pausedAtEpochMs ?: now)-clock.startsAtEpochMs).coerceAtLeast(0)/1000).toInt().coerceAtMost(clock.targetSeconds)
    }
    fun tick(state:WorkoutSessionState,now:Long):WorkoutSessionState {
        val clock=state.timedSet ?: return state
        return if(state.pausedAtEpochMs==null && now>=clock.startsAtEpochMs+clock.targetSeconds*1000L)stop(state,clock.startsAtEpochMs+clock.targetSeconds*1000L) else state
    }
    fun stop(state:WorkoutSessionState,now:Long):WorkoutSessionState {
        val clock=state.timedSet ?: return state
        val effectiveNow=state.pausedAtEpochMs ?: now
        val actual=executedSeconds(state,now)
        val cleared=state.copy(timedSet=null,pausedAtEpochMs=null)
        if(actual==0)return state.copy(timedSet=null)
        val end=minOf(effectiveNow,clock.startsAtEpochMs+clock.targetSeconds*1000L)
        val completed=completeRunnerSetWithRest(cleared,clock.exerciseIndex,clock.setIndex,end,
            seconds=actual,load=clock.loadKg,assistance=clock.assistanceKg)
        return completed.copy(pausedAtEpochMs=state.pausedAtEpochMs,pausedRestRemainingSec=if(state.pausedAtEpochMs!=null)remainingSeconds(completed.restEndsAtEpochMs,end)else completed.pausedRestRemainingSec,exercises=completed.exercises.mapIndexed{i,e->if(i!=clock.exerciseIndex)e else
            e.copy(sets=e.sets.mapIndexed{j,s->if(j!=clock.setIndex)s else s.copy(actualStartedAtEpochMs=clock.startsAtEpochMs,actualFinishedAtEpochMs=end)})})
    }
}

/** Shared rest/transition semantics for both observed repetitions and real timed work. */
internal fun completeRunnerSetWithRest(
    state:WorkoutSessionState,index:Int,setIndex:Int,now:Long,
    reps:Int?=null,seconds:Int?=null,distance:Double?=null,load:Double?=null,assistance:Double?=null
):WorkoutSessionState {
    val consumed=state.restStartedAtEpochMs?.let{start->state.restEndsAtEpochMs?.let{end->
        ((minOf(end,now)-start).coerceAtLeast(0)/1000).toInt()}}
    val done=WorkoutRunnerEngine.completeSet(state,index,setIndex,reps,seconds,distance,load,assistance,consumed)
    if(done==state)return state
    val exercise=done.exercises[index]
    val finished=exercise.sets.all{it.completed}
    val rest=if(finished)exercise.exerciseRestSec else exercise.setRestSec
    val resting=done.copy(restKind=if(finished)"Esercizio"else "Serie",restStartedAtEpochMs=now,restEndsAtEpochMs=now+rest*1000L)
    return TimedRunner.start(resting,now,afterRest=true)
}

/** Skip at the frozen pause reference so resume does not count the pause twice. */
internal fun skipRunnerRest(state:WorkoutSessionState,now:Long):WorkoutSessionState {
    val effective=state.pausedAtEpochMs ?: now
    return state.copy(restEndsAtEpochMs=effective,pausedRestRemainingSec=if(state.pausedAtEpochMs!=null)0 else null,
        timedSet=state.timedSet?.copy(startsAtEpochMs=effective))
}
