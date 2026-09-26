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
}

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
