package com.hybridfitnesscoach.app

/**
 * Pure state transitions for the v0.8 runner. UI layers can dispatch these operations
 * without owning workout semantics. v0.9 sequencing can reuse the same state.
 */
object WorkoutRunnerEngine {
    fun select(state:WorkoutSessionState,index:Int)=state.copy(currentExerciseIndex=index.coerceIn(0,(state.exercises.size-1).coerceAtLeast(0)))
    fun skip(state:WorkoutSessionState,index:Int):WorkoutSessionState {
        if(index !in state.exercises.indices)return state
        val xs=state.exercises.toMutableList();xs[index]=xs[index].copy(status=WorkoutExerciseStatus.SKIPPED)
        val next=xs.indices.firstOrNull{it>index&&xs[it].status!=WorkoutExerciseStatus.COMPLETED&&xs[it].status!=WorkoutExerciseStatus.SKIPPED}
            ?:xs.indices.firstOrNull{xs[it].status==WorkoutExerciseStatus.PENDING||xs[it].status==WorkoutExerciseStatus.PARTIAL}?:index
        return state.copy(exercises=xs,currentExerciseIndex=next)
    }
    fun unskip(state:WorkoutSessionState,index:Int):WorkoutSessionState {
        if(index !in state.exercises.indices)return state
        val xs=state.exercises.toMutableList();if(xs[index].status==WorkoutExerciseStatus.SKIPPED)xs[index]=xs[index].copy(status=WorkoutExerciseStatus.PENDING)
        return state.copy(exercises=xs,currentExerciseIndex=index)
    }
    fun addSet(state:WorkoutSessionState,index:Int):WorkoutSessionState {
        if(index !in state.exercises.indices)return state
        val xs=state.exercises.toMutableList();val e=xs[index];val template=e.sets.lastOrNull()
        val added=(template?:WorkoutSetState(0)).copy(index=e.sets.size,actualReps=null,actualSeconds=null,actualDistanceMeters=null,actualLoadKg=null,actualAssistanceKg=null,completed=false,restUsedSec=null)
        xs[index]=e.copy(sets=e.sets+added,status=if(e.status==WorkoutExerciseStatus.COMPLETED)WorkoutExerciseStatus.PARTIAL else e.status)
        return state.copy(exercises=xs)
    }
    fun move(state:WorkoutSessionState,from:Int,to:Int):WorkoutSessionState {
        if(from !in state.exercises.indices||to !in state.exercises.indices||from==to)return state
        val xs=state.exercises.toMutableList();val item=xs.removeAt(from);xs.add(to,item)
        val reordered=xs.mapIndexed{i,e->e.copy(currentOrder=i)}
        val currentName=state.exercises.getOrNull(state.currentExerciseIndex)?.exerciseName
        return state.copy(exercises=reordered,currentExerciseIndex=reordered.indexOfFirst{it.exerciseName==currentName}.coerceAtLeast(0))
    }

    fun completeSet(state:WorkoutSessionState, exerciseIndex:Int, setIndex:Int, actualReps:Int?=null, actualSeconds:Int?=null, actualDistanceMeters:Double?=null, actualLoadKg:Double?=null, actualAssistanceKg:Double?=null, restUsedSec:Int?=null):WorkoutSessionState {
        if(exerciseIndex !in state.exercises.indices)return state
        val xs=state.exercises.toMutableList();val e=xs[exerciseIndex];if(setIndex !in e.sets.indices)return state
        val sets=e.sets.toMutableList();val s=sets[setIndex]
        sets[setIndex]=s.copy(actualReps=actualReps?:s.targetReps,actualSeconds=actualSeconds?:s.targetSeconds,actualDistanceMeters=actualDistanceMeters?:s.targetDistanceMeters,actualLoadKg=actualLoadKg?:s.targetLoadKg,actualAssistanceKg=actualAssistanceKg?:s.targetAssistanceKg,completed=true,restUsedSec=restUsedSec)
        val completed=sets.count{it.completed};val status=if(completed==sets.size)WorkoutExerciseStatus.COMPLETED else WorkoutExerciseStatus.PARTIAL
        xs[exerciseIndex]=e.copy(sets=sets,status=status)
        val next=if(status==WorkoutExerciseStatus.COMPLETED) xs.indices.firstOrNull{it>exerciseIndex&&xs[it].status!=WorkoutExerciseStatus.COMPLETED&&xs[it].status!=WorkoutExerciseStatus.SKIPPED}?:exerciseIndex else exerciseIndex
        return state.copy(exercises=xs,currentExerciseIndex=next)
    }
    fun reopenSet(state:WorkoutSessionState,exerciseIndex:Int,setIndex:Int):WorkoutSessionState {
        if(exerciseIndex !in state.exercises.indices)return state
        val xs=state.exercises.toMutableList();val e=xs[exerciseIndex];if(setIndex !in e.sets.indices)return state
        val sets=e.sets.toMutableList();sets[setIndex]=sets[setIndex].copy(completed=false)
        xs[exerciseIndex]=e.copy(sets=sets,status=if(sets.any{it.completed})WorkoutExerciseStatus.PARTIAL else WorkoutExerciseStatus.PENDING)
        return state.copy(exercises=xs,currentExerciseIndex=exerciseIndex)
    }
}

internal fun elapsedWorkoutSeconds(state:WorkoutSessionState, now:Long):Int {
    val end=state.pausedAtEpochMs?:now
    return ((end-state.startedAtEpochMs).coerceAtLeast(0L)/1000L).toInt()
}
internal fun actualCompletedSets(session:SessionRecord):Int =
    if(session.structuredExercises.isNotEmpty()) session.structuredExercises.sumOf{e->e.sets.count{it.completed}}
    else session.completedSets
internal fun executedVolume(session:SessionRecord):Double =
    session.structuredExercises.sumOf { e -> e.sets.filter{it.completed}.sumOf { s ->
        when(e.measure){
            ExerciseMeasure.REPS -> (s.actualReps?:0).toDouble()
            ExerciseMeasure.SECONDS,ExerciseMeasure.DURATION,ExerciseMeasure.INTERVALS -> (s.actualSeconds?:0).toDouble()
            ExerciseMeasure.DISTANCE -> s.actualDistanceMeters?:0.0
        }
    }}

data class RawHealthSnapshot(
    val capturedAtEpochMs:Long,
    val sleepMinutes:Int?=null,
    val restingHeartRateBpm:Double?=null,
    val hrvMs:Double?=null,
    val activeMinutes:Int?=null,
    val steps:Long?=null,
    val source:String="health-connect"
)
data class NormalizedHealthState(
    val capturedAtEpochMs:Long,
    val sleepScore:Double?=null,
    val cardiovascularRecoveryScore:Double?=null,
    val activityLoadScore:Double?=null
)
data class AthleteReadinessInput(
    val recoveryByMuscle:Map<String,Int>,
    val health:NormalizedHealthState?=null,
    val recentWorkoutRpe:List<Int> = emptyList()
)
interface HealthDataNormalizer { fun normalize(raw:RawHealthSnapshot):NormalizedHealthState }
/** Boundary only: v0.8 does not fabricate Health Connect readings or make adaptive decisions from them. */
interface ReadinessProvider { fun readiness(input:AthleteReadinessInput):Double? }
