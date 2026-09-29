package com.hybridfitnesscoach.app

/** The persisted set is authoritative, including after process recreation. */
internal fun runnerInputTarget(exercise:Exercise,set:WorkoutSetState?):Double = when(exercise.measure) {
    ExerciseMeasure.REPS -> set?.targetReps?.toDouble()
    ExerciseMeasure.SECONDS, ExerciseMeasure.DURATION, ExerciseMeasure.INTERVALS -> set?.targetSeconds?.toDouble()
    ExerciseMeasure.DISTANCE -> set?.targetDistanceMeters
} ?: prescriptionFor(exercise).value

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
    val accumulated=state.elapsedBeforePauseMs.coerceAtLeast(0L)
    if(state.pausedAtEpochMs!=null){
        if(accumulated>0L)return (accumulated/1000L).toInt()
        val legacyPausedElapsed=(state.pausedAtEpochMs-state.startedAtEpochMs.coerceAtMost(state.pausedAtEpochMs)).coerceAtLeast(0L)
        return (legacyPausedElapsed/1000L).toInt()
    }
    val wallClock=(now-state.startedAtEpochMs.coerceAtMost(now)).coerceAtLeast(0L)
    return (maxOf(accumulated,wallClock)/1000L).toInt()
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
    val source:String="health-connect",
    val baselineRestingHeartRateBpm:Double?=null,
    val baselineHrvMs:Double?=null
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


// --- v0.9 adaptive domain ---
enum class ProgressionStatus { LOCKED, AVAILABLE, IN_PROGRESS, MASTERED }
enum class AdaptiveDecision { PROGRESS, MAINTAIN, REGRESS, DELOAD }

data class MasteryCriterion(
    val minSets:Int=3,
    val minReps:Int?=null,
    val minSeconds:Int?=null,
    val maxRpe:Int?=8,
    val requiredSessions:Int=2
)
data class SkillNode(
    val id:String,
    val exerciseName:String,
    val skill:String,
    val prerequisites:Set<String> = emptySet(),
    val criterion:MasteryCriterion = MasteryCriterion(),
    val exerciseAliases:Set<String> = emptySet()
)
data class SkillNodeProgress(val node:SkillNode,val status:ProgressionStatus,val completion:Double)
data class LoadSuggestion(val loadKg:Double?,val calibrationRequired:Boolean,val reason:String)

object ProgressionEngine {
    fun evaluate(node:SkillNode, mastered:Set<String>, sessions:List<SessionRecord>):SkillNodeProgress {
        if(!mastered.containsAll(node.prerequisites)) return SkillNodeProgress(node,ProgressionStatus.LOCKED,0.0)
        val names=node.exerciseAliases+node.exerciseName
        val attempts=sessions.distinctBy{it.timestamp}.map { session ->
            session.structuredExercises.filter { attempt -> names.any { it.equals(attempt.exerciseName, ignoreCase=true) } }
        }.filter{it.isNotEmpty()}
        if(attempts.isEmpty()) return SkillNodeProgress(node,ProgressionStatus.AVAILABLE,0.0)
        // At most one qualifying observation per session, even with repeated exercise entries.
        val qualified=attempts.count { entries -> entries.any { e ->
            val done=e.sets.filter{it.completed}
            done.size>=node.criterion.minSets &&
            (node.criterion.minReps==null || done.all{(it.actualReps?:0)>=node.criterion.minReps}) &&
            (node.criterion.minSeconds==null || done.all{(it.actualSeconds?:0)>=node.criterion.minSeconds}) &&
            (node.criterion.maxRpe==null || (e.rpe?:10)<=node.criterion.maxRpe)
        }}
        val completion=(qualified.toDouble()/node.criterion.requiredSessions.coerceAtLeast(1)).coerceIn(0.0,1.0)
        val status=if(completion>=1.0)ProgressionStatus.MASTERED else ProgressionStatus.IN_PROGRESS
        return SkillNodeProgress(node,status,completion)
    }
    fun skillProgress(nodes:List<SkillNodeProgress>):Int {
        if(nodes.isEmpty()) return 0
        val achieved=nodes.sumOf{when(it.status){ProgressionStatus.MASTERED->1.0;ProgressionStatus.IN_PROGRESS->0.5*it.completion;else->0.0}}
        return (100.0*achieved/nodes.size).toInt().coerceIn(0,100)
    }
}

object LoadRecommendationEngine {
    fun suggest(exerciseName:String,sessions:List<SessionRecord>,incrementKg:Double=2.5,targetRpe:Int=8):LoadSuggestion {
        val observations=sessions.asSequence().flatMap{it.structuredExercises.asSequence()}
            .filter{it.exerciseName==exerciseName}.flatMap{it.sets.asSequence()}
            .filter{it.completed&&it.actualLoadKg!=null}.toList()
        if(observations.isEmpty()) return LoadSuggestion(null,true,"Carico da calibrare")
        val last=observations.last();val load=last.actualLoadKg!!
        val repsOk=(last.targetReps==null)||(last.actualReps?:0)>=last.targetReps
        val exerciseRpe=sessions.asSequence().flatMap{it.structuredExercises.asSequence()}.lastOrNull{it.exerciseName==exerciseName}?.rpe
        val raw=when { !repsOk || (exerciseRpe?:targetRpe)>targetRpe+1 -> load-incrementKg
            (exerciseRpe?:targetRpe)<=targetRpe-1 -> load+incrementKg
            else -> load }
        val rounded=(kotlin.math.round(raw.coerceAtLeast(incrementKg)/incrementKg)*incrementKg)
        return LoadSuggestion(rounded,false,when{rounded>load->"Progressione da storico e margine";rounded<load->"Riduzione per performance/fatica";else->"Mantieni il carico"})
    }
}

object AdaptiveCoach {
    fun decide(readiness:Double?,recentRpe:Int?,completedRatio:Double):AdaptiveDecision = when {
        readiness!=null && readiness<45.0 -> AdaptiveDecision.DELOAD
        completedRatio<0.70 || (recentRpe?:0)>=10 -> AdaptiveDecision.REGRESS
        completedRatio>=0.98 && (recentRpe?:8)<=7 && (readiness?:70.0)>=60.0 -> AdaptiveDecision.PROGRESS
        else -> AdaptiveDecision.MAINTAIN
    }
}



internal fun customWorkoutCandidatePool(
    profile:EquipmentProfile,
    athlete:AthleteProfile,
    sessions:List<SessionRecord>,
    mode:String,
    selectedSkills:Collection<String>,
    selectedGroups:Collection<String>
):List<Exercise> {
    val eligible=skillEligibleExercises(sessions)
    val base=exercises.filter { ex ->
        ex.name in eligible &&
        levelScore(ex.level)<=levelScore(athlete.level) &&
        equipmentCompatible(ex,profile) &&
        (mode!="Parti del corpo" || selectedGroups.isEmpty() || selectedGroups.any{ex.group.contains(it,true)})
    }
    if(mode!="Skill") return base
    val skillWork=base.filter{ex->selectedSkills.any{s->s in ex.skills}}
    val support=base.filter{it !in skillWork && (it.role=="Forza"||it.role=="Prerequisito"||it.role=="Accessorio")}
    return (skillWork+support).distinct()
}
