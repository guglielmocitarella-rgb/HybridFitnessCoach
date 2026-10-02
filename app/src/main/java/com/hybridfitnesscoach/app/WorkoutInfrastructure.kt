package com.hybridfitnesscoach.app

/** The persisted set is authoritative, including after process recreation. */
internal fun runnerInputTarget(exercise:Exercise,set:WorkoutSetState):Double = when(exercise.measure) {
    ExerciseMeasure.REPS -> set.targetReps?.toDouble()
    ExerciseMeasure.SECONDS, ExerciseMeasure.DURATION, ExerciseMeasure.INTERVALS -> set.targetSeconds?.toDouble()
    ExerciseMeasure.DISTANCE -> set.targetDistanceMeters
}.let{requireNotNull(it){"Materialized set is missing its target"}}

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
        return state.copy(exercises=xs,currentExerciseIndex=next,timedSet=state.timedSet?.takeUnless{it.exerciseIndex==index})
    }
    fun unskip(state:WorkoutSessionState,index:Int):WorkoutSessionState {
        if(index !in state.exercises.indices)return state
        val xs=state.exercises.toMutableList();if(xs[index].status==WorkoutExerciseStatus.SKIPPED)xs[index]=xs[index].copy(status=if(xs[index].sets.any{it.completed})WorkoutExerciseStatus.PARTIAL else WorkoutExerciseStatus.PENDING)
        return state.copy(exercises=xs,currentExerciseIndex=index)
    }
    fun undo(state:WorkoutSessionState,index:Int):WorkoutSessionState {
        if(index !in state.exercises.indices)return state
        val xs=state.exercises.toMutableList()
        val sets=undoLastCompletedSet(xs[index].sets)
        xs[index]=xs[index].copy(sets=sets,status=if(sets.any{it.completed})WorkoutExerciseStatus.PARTIAL else WorkoutExerciseStatus.PENDING)
        return state.copy(exercises=xs,currentExerciseIndex=index,restEndsAtEpochMs=null,pausedRestRemainingSec=null,timedSet=null,restStartedAtEpochMs=null)
    }
    fun pause(state:WorkoutSessionState,now:Long):WorkoutSessionState {
        if(state.pausedAtEpochMs!=null)return state
        return state.copy(elapsedBeforePauseMs=elapsedWorkoutSeconds(state,now)*1000L,
            pausedAtEpochMs=now,pausedRestRemainingSec=remainingSeconds(state.restEndsAtEpochMs,now),restEndsAtEpochMs=null)
    }
    fun resume(state:WorkoutSessionState,now:Long):WorkoutSessionState {
        if(state.pausedAtEpochMs==null)return state
        return state.copy(startedAtEpochMs=now-state.elapsedBeforePauseMs,pausedAtEpochMs=null,
            restEndsAtEpochMs=state.pausedRestRemainingSec?.takeIf{it>0 || state.restStartedAtEpochMs!=null}?.let{now+it*1000L},pausedRestRemainingSec=null,
            timedSet=state.timedSet?.let{it.copy(startsAtEpochMs=it.startsAtEpochMs+(now-state.pausedAtEpochMs))},
            restStartedAtEpochMs=state.restStartedAtEpochMs?.let{it+(now-state.pausedAtEpochMs)})
    }
    fun addSet(state:WorkoutSessionState,index:Int):WorkoutSessionState {
        if(index !in state.exercises.indices)return state
        val xs=state.exercises.toMutableList();val e=xs[index];val template=e.sets.lastOrNull()
        val added=(template?:WorkoutSetState(0)).copy(index=e.sets.size,actualReps=null,actualSeconds=null,actualDistanceMeters=null,actualLoadKg=null,actualAssistanceKg=null,completed=false,restUsedSec=null,actualRpe=null,actualStartedAtEpochMs=null,actualFinishedAtEpochMs=null)
        xs[index]=e.copy(sets=e.sets+added,status=if(e.status==WorkoutExerciseStatus.COMPLETED)WorkoutExerciseStatus.PARTIAL else e.status)
        return state.copy(exercises=xs)
    }
    fun move(state:WorkoutSessionState,from:Int,to:Int):WorkoutSessionState {
        if(from !in state.exercises.indices||to !in state.exercises.indices||from==to)return state
        val xs=state.exercises.toMutableList();val item=xs.removeAt(from);xs.add(to,item)
        val reordered=xs.mapIndexed{i,e->e.copy(currentOrder=i)}
        val current=state.currentExerciseIndex
        val selected=when { current==from->to; from<current && to>=current->current-1; from>current && to<=current->current+1; else->current }
        return state.copy(exercises=reordered,currentExerciseIndex=selected)
    }

    fun completeSet(state:WorkoutSessionState, exerciseIndex:Int, setIndex:Int, actualReps:Int?=null, actualSeconds:Int?=null, actualDistanceMeters:Double?=null, actualLoadKg:Double?=null, actualAssistanceKg:Double?=null, restUsedSec:Int?=null):WorkoutSessionState {
        if(exerciseIndex !in state.exercises.indices)return state
        val xs=state.exercises.toMutableList();val e=xs[exerciseIndex];if(setIndex !in e.sets.indices)return state
        val sets=e.sets.toMutableList();val s=sets[setIndex]
        if(s.completed || e.status==WorkoutExerciseStatus.SKIPPED || state.pausedAtEpochMs!=null)return state
        val observed=when(e.measure){
            ExerciseMeasure.REPS -> actualReps?.let{it>0}==true
            ExerciseMeasure.DISTANCE -> actualDistanceMeters?.let{it.isFinite()&&it>0}==true
            else -> actualSeconds?.let{it>0}==true
        }
        if(!observed || actualLoadKg?.let{!it.isFinite()||it<0}==true || actualAssistanceKg?.let{!it.isFinite()||it<0}==true)return state
        sets[setIndex]=s.copy(actualReps=actualReps,actualSeconds=actualSeconds,actualDistanceMeters=actualDistanceMeters,actualLoadKg=actualLoadKg,actualAssistanceKg=actualAssistanceKg,completed=true,restUsedSec=restUsedSec,actualRpe=runnerEffort(e.rpe))
        val completed=sets.count{it.completed};val status=if(completed==sets.size)WorkoutExerciseStatus.COMPLETED else WorkoutExerciseStatus.PARTIAL
        xs[exerciseIndex]=e.copy(sets=sets,status=status)
        val next=if(status==WorkoutExerciseStatus.COMPLETED) xs.indices.firstOrNull{it>exerciseIndex&&xs[it].status!=WorkoutExerciseStatus.COMPLETED&&xs[it].status!=WorkoutExerciseStatus.SKIPPED}?:exerciseIndex else exerciseIndex
        return state.copy(exercises=xs,currentExerciseIndex=next)
    }
    fun reopenSet(state:WorkoutSessionState,exerciseIndex:Int,setIndex:Int):WorkoutSessionState {
        if(exerciseIndex !in state.exercises.indices)return state
        val xs=state.exercises.toMutableList();val e=xs[exerciseIndex];if(setIndex !in e.sets.indices)return state
        val sets=e.sets.toMutableList();sets[setIndex]=sets[setIndex].copy(completed=false,actualReps=null,actualSeconds=null,actualDistanceMeters=null,actualLoadKg=null,actualAssistanceKg=null,restUsedSec=null,actualRpe=null,actualStartedAtEpochMs=null,actualFinishedAtEpochMs=null)
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
    val requiredSessions:Int=2,
    val evidenceClass:String="APP_RULE",
    val evidenceNote:String="Soglia operativa dell'app; non è una soglia clinica o scientifica."
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

object AdaptiveCoach {
    fun decide(readiness:Double?,recentRpe:Int?,completedRatio:Double):AdaptiveDecision = when {
        readiness!=null && readiness<45.0 -> AdaptiveDecision.DELOAD
        completedRatio<0.70 || (recentRpe?:0)>=10 -> AdaptiveDecision.REGRESS
        completedRatio>=0.98 && (recentRpe?:8)<=7 && (readiness?:70.0)>=60.0 -> AdaptiveDecision.PROGRESS
        else -> AdaptiveDecision.MAINTAIN
    }
}



/**
 * Resolve the progression node that should be trained for each selected skill.
 * IN_PROGRESS takes precedence over the next AVAILABLE node; once mastery is
 * demonstrated by persisted history, focus advances to that newly available node.
 * If a graph is fully mastered, keep the terminal node available for maintenance.
 */
internal fun skillWorkoutFocusNames(
    selectedSkills:Collection<String>,
    sessions:List<SessionRecord>
):Set<String> = selectedSkills.flatMap { skill ->
    val states=SkillCatalog.progress(skill,sessions)
    val focus=states.firstOrNull{it.status==ProgressionStatus.IN_PROGRESS}
        ?: states.firstOrNull{it.status==ProgressionStatus.AVAILABLE}
        ?: states.lastOrNull{it.status==ProgressionStatus.MASTERED}
    focus?.let{it.node.exerciseAliases+it.node.exerciseName}.orEmpty()
}.map{it.lowercase()}.toSet()

internal fun customWorkoutCandidatePool(
    location:TrainingLocation,
    athlete:AthleteProfile,
    sessions:List<SessionRecord>,
    mode:String,
    selectedSkills:Collection<String>,
    selectedGroups:Collection<String>,
    catalogue:List<Exercise> = exercises,
    equipmentFilters:Collection<String> = emptyList()
):List<Exercise> {
    val eligible=skillEligibleExerciseNames(catalogue,sessions)
    val capability=athlete.toCapabilityProfile()
    val base=catalogue.filter { ex ->
        ex.name in eligible &&
        levelScore(ex.level)<=capabilityLevelFor(ex,capability) &&
        locationCompatible(ex,location) &&
        (equipmentFilters.isEmpty() || equipmentFilters.any { eq -> ex.equipment.contains(eq,true) || (eq=="Macchine" && ex.equipment.contains("Macchina",true)) }) &&
        (mode!="Parti del corpo" || selectedGroups.isEmpty() || selectedGroups.any{ex.group.contains(it,true)})
    }
    if(mode!="Skill") return base
    if(selectedSkills.isEmpty()) return emptyList()

    val focusNames=skillWorkoutFocusNames(selectedSkills,sessions)
    val skillWork=base.filter{ex->
        selectedSkills.any{s->s in ex.skills} && ex.name.lowercase() in focusNames
    }.map { ex -> applySkillMasteryPrescription(ex,selectedSkills,sessions) }
    val support=base.filter{ex->
        ex !in skillWork &&
        selectedSkills.none{s->s in ex.skills} &&
        ex.role in setOf("Forza","Prerequisito","Accessorio")
    }
    return (skillWork+support).distinct()
}

internal fun applySkillMasteryPrescription(
    exercise:Exercise,
    selectedSkills:Collection<String>,
    sessions:List<SessionRecord>
):Exercise {
    val name=exercise.name.lowercase()
    val state=selectedSkills.asSequence()
        .flatMap{SkillCatalog.progress(it,sessions).asSequence()}
        .firstOrNull { progress ->
            progress.status in setOf(ProgressionStatus.IN_PROGRESS,ProgressionStatus.AVAILABLE,ProgressionStatus.MASTERED) &&
            (progress.node.exerciseAliases+progress.node.exerciseName).any{it.lowercase()==name}
        } ?: return exercise
    val criterion=state.node.criterion
    val target=when(exercise.measure) {
        ExerciseMeasure.REPS -> criterion.minReps?.toDouble()
        ExerciseMeasure.SECONDS, ExerciseMeasure.DURATION, ExerciseMeasure.INTERVALS -> criterion.minSeconds?.toDouble()
        ExerciseMeasure.DISTANCE -> null
    }
    return exercise.copy(
        targetSets=maxOf(exercise.targetSets,criterion.minSets),
        targetValue=target ?: exercise.targetValue
    )
}

/** Re-assert the mastery floor after Adaptive Coach adjustments. */
internal fun enforceSkillMasteryPrescription(
    items:List<Exercise>,
    selectedSkills:Collection<String>,
    sessions:List<SessionRecord>
):List<Exercise> = items.map{applySkillMasteryPrescription(it,selectedSkills,sessions)}

internal fun regenerateSkillWorkout(
    original:List<Exercise>,
    pool:List<Exercise>,
    pinned:Set<String>,
    context:CoachingContext,
    selectedSkills:Collection<String>,
    sessions:List<SessionRecord>
):List<Exercise> {
    val focusNames=skillWorkoutFocusNames(selectedSkills,sessions)
    val regenerated=regenerateCoachedWorkout(original,pool,pinned,context).toMutableList()
    // Generic regeneration optimizes replacement/order and may consume support
    // candidates before the unique focus node. Reinsert the current progression
    // focus if it was displaced; never source it from outside the guarded pool.
    if(regenerated.none{it.name.lowercase() in focusNames}) {
        val focus=pool.firstOrNull{it.name.lowercase() in focusNames}
        if(focus!=null) {
            val replaceAt=regenerated.indexOfFirst{it.name !in pinned}
            if(replaceAt>=0) regenerated[replaceAt]=focus
            else if(regenerated.isEmpty()) regenerated.add(focus)
        }
    }
    val enforced=enforceSkillMasteryPrescription(regenerated,selectedSkills,sessions)
    return enforced.mapIndexed{index,ex->if(regenerated[index].name in pinned)regenerated[index]else ex}
}

internal data class EffectiveRunnerPrescription(
    val sets:Int,
    val value:Double,
    val unit:String,
    val restSeconds:Int,
    val sourceBased:Boolean
)
internal fun effectiveRunnerPrescription(
    exercise:Exercise,
    athlete:AthleteProfile,
    sessions:List<SessionRecord>,
    readinessScore:Int?=null
):EffectiveRunnerPrescription {
    val legacy=prescriptionFor(exercise)
    val latest=sessions.firstOrNull()
    val recent=latest?.let { session ->
        val state=session.structuredExercises.firstOrNull{it.exerciseName.equals(exercise.name,true)}
        state?.sets?.takeIf{it.isNotEmpty()}?.let{sets->sets.count{it.completed}.toDouble()/sets.size}
    }
    val fatigue=latest?.workoutRpe ?: 5
    val context=PrescriptionContext(
        goal=athlete.goal,
        capabilityLevel=capabilityLevelFor(exercise,athlete.toCapabilityProfile()),
        fatigue=fatigue,
        recentPerformance=recent,
        recoveryStatus=readinessScore
    )
    val sourced=SourcePrescriptionBaselineRegistry.prescribeIfVerified(exercise,context)
    return if(sourced!=null) EffectiveRunnerPrescription(
        sourced.sets,sourced.repsOrDuration,legacy.unit,
        runnerSetRestSeconds(exercise,sourced.restSeconds),true
    ) else EffectiveRunnerPrescription(
        legacy.sets,legacy.value,legacy.unit,
        runnerSetRestSeconds(exercise),false
    )
}


internal fun exerciseWithEffectivePrescription(
    exercise:Exercise,
    athlete:AthleteProfile,
    sessions:List<SessionRecord>,
    readinessScore:Int?=null
):Exercise {
    val p=effectiveRunnerPrescription(exercise,athlete,sessions,readinessScore)
    return exercise.copy(
        targetSets=p.sets,
        targetValue=p.value,
        setRestSec=p.restSeconds,
        // The transition after the final set belongs to the same effective
        // recovery prescription. Otherwise sourced set recovery reaches the
        // runner while exercise-transition recovery silently stays legacy.
        exerciseRestSec=if(p.sourceBased) p.restSeconds else exercise.exerciseRestSec
    )
}
internal fun effectiveAutomaticExercises(
    source:List<Exercise>,
    athlete:AthleteProfile,
    sessions:List<SessionRecord>,
    readinessScore:Int?=null,
    restored:Boolean=false
):List<Exercise> = if(restored) source else source.map{exerciseWithEffectivePrescription(it,athlete,sessions,readinessScore)}


internal enum class WorkoutOrigin { PLAN, ADAPTIVE_EXTRA, CUSTOM, SKILL, LIBRARY, SINGLE_EXERCISE }
internal enum class WorkoutLaunchStage { CHOOSE_LOCATION, MATERIALIZE, PREVIEW, RUNNER }

internal data class WorkoutLaunchIntent(
    val origin:WorkoutOrigin,
    val sourceExerciseId:String?=null,
    val plannedWorkoutId:String?=null,
    val focusSkills:List<String> = emptyList(),
    val equipmentFilters:List<String> = emptyList(),
    val groupFilters:List<String> = emptyList(),
    val lockFilters:Boolean=false,
    val durationMinutes:Int=45
) {
    init {
        require(sourceExerciseId==null || sourceExerciseId.isNotBlank())
        require(plannedWorkoutId==null || (origin==WorkoutOrigin.PLAN && plannedWorkoutId.isNotBlank()))
        require(origin!=WorkoutOrigin.PLAN || plannedWorkoutId!=null)
        require(origin!=WorkoutOrigin.SINGLE_EXERCISE || sourceExerciseId!=null)
        require(focusSkills.all{it.isNotBlank()})
        require(durationMinutes in 10..120 && durationMinutes%5==0)
        require(focusSkills.isEmpty() || origin in setOf(WorkoutOrigin.CUSTOM,WorkoutOrigin.SKILL,WorkoutOrigin.LIBRARY))
        require((equipmentFilters.isEmpty() && groupFilters.isEmpty() && !lockFilters) || origin in setOf(WorkoutOrigin.CUSTOM,WorkoutOrigin.SKILL,WorkoutOrigin.LIBRARY))
        require(!lockFilters || origin==WorkoutOrigin.LIBRARY)
        require(origin in setOf(WorkoutOrigin.CUSTOM,WorkoutOrigin.SKILL,WorkoutOrigin.LIBRARY) ||
            (focusSkills.isEmpty() && equipmentFilters.isEmpty() && groupFilters.isEmpty() && !lockFilters))
    }
}

internal data class WorkoutLaunchFlow(
    val intent:WorkoutLaunchIntent,
    val stage:WorkoutLaunchStage=WorkoutLaunchStage.CHOOSE_LOCATION,
    val locationId:String?=null,
    val workout:WorkoutSessionState?=null,
    val errorMessage:String?=null
)

internal fun beginWorkoutLaunch(intent:WorkoutLaunchIntent)=WorkoutLaunchFlow(intent,
    stage=if(intent.origin==WorkoutOrigin.SINGLE_EXERCISE)WorkoutLaunchStage.MATERIALIZE else WorkoutLaunchStage.CHOOSE_LOCATION)

internal fun preferredWorkoutLocation(locations:List<TrainingLocation>,lastUsedLocationId:String?):TrainingLocation? =
    locations.firstOrNull{it.id==lastUsedLocationId} ?: locations.firstOrNull()

internal fun selectWorkoutLocation(flow:WorkoutLaunchFlow,locationId:String):WorkoutLaunchFlow {
    require(flow.stage==WorkoutLaunchStage.CHOOSE_LOCATION)
    require(locationId.isNotBlank())
    return flow.copy(stage=WorkoutLaunchStage.MATERIALIZE,locationId=locationId)
}

internal fun materializeWorkoutLaunch(flow:WorkoutLaunchFlow,workout:WorkoutSessionState):WorkoutLaunchFlow {
    require(flow.stage==WorkoutLaunchStage.MATERIALIZE)
    require(workout.trainingLocationId==flow.locationId) { "Workout must be materialized for the selected location" }
    require(workout.plannedWorkoutId==flow.intent.plannedWorkoutId) { "Planned workout ownership must be preserved" }
    return flow.copy(stage=WorkoutLaunchStage.PREVIEW,workout=workout.copy(workoutOrigin=flow.intent.origin.name))
}


internal fun generatedWorkoutSessionState(
    intent:WorkoutLaunchIntent,locationId:String,exercises:List<Exercise>,startedAtEpochMs:Long,
    pinnedExerciseNames:Set<String> = emptySet(),setRestOverridesSec:Map<String,Int> = emptyMap()
):WorkoutSessionState {
    require(intent.origin in setOf(WorkoutOrigin.CUSTOM,WorkoutOrigin.SKILL,WorkoutOrigin.LIBRARY))
    require(locationId.isNotBlank() && exercises.isNotEmpty())
    require(setRestOverridesSec.values.all{it>0})
    val suffix=intent.focusSkills.takeIf{it.isNotEmpty()}?.joinToString(prefix=" • ").orEmpty()
    val states=exercises.mapIndexed{index,ex->
        val effective=setRestOverridesSec[ex.name]?.let{ex.copy(setRestSec=it)}?:ex
        plannedExerciseState(index,effective).copy(rpe=5,pinned=ex.name in pinnedExerciseNames)
    }
    return WorkoutSessionState("custom-"+startedAtEpochMs,startedAtEpochMs,"Personalizzato$suffix",
        states,runnerKind="custom",trainingLocationId=locationId)
}

internal fun startWorkoutRunner(flow:WorkoutLaunchFlow):WorkoutLaunchFlow {
    require(flow.stage==WorkoutLaunchStage.PREVIEW && flow.workout!=null)
    require(flow.workout.exercises.isNotEmpty() && flow.workout.exercises.all{it.sets.isNotEmpty() && it.exerciseSnapshot!=null})
    return flow.copy(stage=WorkoutLaunchStage.RUNNER)
}

internal data class WorkoutLaunchResolution(
    val plannedWorkout:PlannedWorkout?,
    val resumeActive:Boolean
)
internal fun resolveWorkoutLaunch(active:WorkoutSessionState?, requested:PlannedWorkout?):WorkoutLaunchResolution =
    if(active!=null) WorkoutLaunchResolution(null,true) else WorkoutLaunchResolution(requested,false)


internal fun committedPlanOrCurrent(current:TrainingPlan,candidate:TrainingPlan,persisted:Boolean):TrainingPlan =
    if(persisted) candidate else current


internal fun plannedWeekExercises(
    base:List<Exercise>,
    weekIndex:Int
):List<Exercise> {
    require(weekIndex>=0)
    if(base.isEmpty()) return base
    // Conservative periodization: preserve exercise identity/evidence and progress only existing
    // catalog targets. Three loading weeks followed by one consolidation week.
    val phase=weekIndex%4
    return base.map { ex ->
        val sets=ex.targetSets
        val value=ex.targetValue
        if(phase==3) ex else ex.copy(
            targetSets=(sets + if(phase>=2)1 else 0).coerceAtMost(sets+1),
            targetValue=if(value!=null && ex.measure!=ExerciseMeasure.DISTANCE) value*(1.0+0.025*phase) else value
        )
    }
}


internal data class ExerciseSkillLink(
    val skill:String,
    val progressPercent:Int,
    val currentNode:String?,
    val nextUnlock:String
)
internal fun exerciseSkillLinks(exercise:Exercise,sessions:List<SessionRecord>):List<ExerciseSkillLink> =
    exercise.skills.map { skill ->
        val states=SkillCatalog.progress(skill,sessions)
        val current=states.firstOrNull{it.status==ProgressionStatus.IN_PROGRESS || it.status==ProgressionStatus.AVAILABLE}
        ExerciseSkillLink(skill,ProgressionEngine.skillProgress(states),current?.node?.exerciseName,nextUnlockLabel(states))
    }


internal fun todayPlannedWorkouts(plans:List<TrainingPlan>,today:Long):List<Pair<TrainingPlan,PlannedWorkout>> =
    plans.flatMap { plan ->
        plan.workouts.filter {
            it.epochDay==today &&
            it.state!=PlannedWorkoutState.COMPLETED &&
            it.state!=PlannedWorkoutState.PARTIAL &&
            it.state!=PlannedWorkoutState.SKIPPED
        }.map { plan to it }
    }.sortedWith(compareBy<Pair<TrainingPlan,PlannedWorkout>>{it.first.goal}
        .thenBy{it.second.weekIndex}.thenBy{it.second.sequenceInWeek})


internal fun undoLastCompletedSet(sets:List<WorkoutSetState>):List<WorkoutSetState> {
    val last=sets.indexOfLast{it.completed}
    if(last<0) return sets
    return sets.toMutableList().also { xs ->
        xs[last]=xs[last].copy(
            actualReps=null,actualSeconds=null,actualDistanceMeters=null,
            actualLoadKg=null,actualAssistanceKg=null,completed=false,restUsedSec=null,actualRpe=null,actualStartedAtEpochMs=null,actualFinishedAtEpochMs=null
        )
    }
}


internal fun singleExerciseWorkoutState(
    exercise:Exercise,
    profileName:String,
    startedAtEpochMs:Long=System.currentTimeMillis(),
    athlete:AthleteProfile?=null,
    sessions:List<SessionRecord> = emptyList(),
    readinessScore:Int?=null,
    trainingLocationId:String?=null
):WorkoutSessionState {
    val effective=athlete?.let{exerciseWithEffectivePrescription(exercise,it,sessions,readinessScore)} ?: exercise
    val prescription=prescriptionFor(effective)
    val target=prescription.value
    val sets=(0 until prescription.sets).map { index ->
        WorkoutSetState(
            index=index,
            targetReps=if(exercise.measure==ExerciseMeasure.REPS) target.toInt() else null,
            targetSeconds=if(exercise.measure in setOf(ExerciseMeasure.SECONDS,ExerciseMeasure.DURATION,ExerciseMeasure.INTERVALS)) target.toInt() else null,
            targetDistanceMeters=if(exercise.measure==ExerciseMeasure.DISTANCE) target else null
        )
    }
    val state=WorkoutExerciseState(
        exerciseName=effective.name,
        measure=effective.measure,
        sets=sets,
        status=WorkoutExerciseStatus.PENDING,
        rpe=5,
        setRestSec=effective.setRestSec,
        exerciseRestSec=effective.exerciseRestSec,
        exerciseId=effective.progressionKey,
        exerciseSnapshot=effective
    )
    return WorkoutSessionState(
        id="single-"+startedAtEpochMs,
        startedAtEpochMs=startedAtEpochMs,
        profileName=profileName,
        exercises=listOf(state),
        runnerKind="single",
        trainingLocationId=trainingLocationId
    )
}


/** Structured occurrences are authoritative; name maps aggregate their evidence for coaching. */
internal fun runnerSessionRecord(state:WorkoutSessionState,now:Long):SessionRecord {
    val observed=state.exercises.map{e->
        val done=e.sets.filter{it.completed}
        val efforts=done.mapNotNull{it.actualRpe}
        e.copy(rpe=if(done.isEmpty())null else if(efforts.isNotEmpty())efforts.average().toInt() else e.rpe)
    }
    val grouped=observed.groupBy{it.exerciseName}
    return SessionRecord(now,state.profileName,state.exercises.sumOf{e->e.sets.count{it.completed}},
        state.exercises.sumOf{it.sets.size},state.exercises.map{it.exerciseName},
        grouped.mapNotNull{(name,items)->items.mapNotNull{it.rpe}.takeIf{it.isNotEmpty()}?.let{name to it.average().toInt()}}.toMap(),runnerEffort(state.workoutRpe),
        grouped.mapValues{(_,items)->items.sumOf{e->e.sets.count{it.completed}}},elapsedWorkoutSeconds(state,now),
        observed,state.exercises.filter{it.status==WorkoutExerciseStatus.SKIPPED}.map{it.exerciseName},
        state.modifications,state.plannedWorkoutId,state.trainingLocationId,state.workoutOrigin)
}

/** APP_RULE estimate: four seconds per rep; distance at one metre/second. Includes prescribed rest. */
internal fun estimatedExerciseSeconds(ex:Exercise):Int {
    val p=prescriptionFor(ex)
    val effort=when(ex.measure){ExerciseMeasure.REPS->p.value*4;else->p.value}.toInt().coerceAtLeast(1)
    return p.sets*effort+(p.sets-1).coerceAtLeast(0)*runnerSetRestSeconds(ex)+ex.exerciseRestSec
}
internal fun fitCustomWorkoutDuration(candidates:List<Exercise>,minutes:Int,pinnedNames:Set<String> = emptySet()):List<Exercise> {
    require(minutes in 10..120)
    var remaining=minutes*60-candidates.filter{it.name in pinnedNames}.sumOf(::estimatedExerciseSeconds)
    val selected=mutableListOf<Exercise>()
    for(ex in candidates){
        if(ex.name in pinnedNames){selected.add(ex);continue}
        val fitting=(ex.targetSets downTo 1).asSequence().map{ex.copy(targetSets=it)}.firstOrNull{estimatedExerciseSeconds(it)<=remaining} ?: continue
        selected.add(fitting);remaining-=estimatedExerciseSeconds(fitting)
    }
    return selected
}

/** Freeze load/assistance and effective rest at materialization, never on Runner recomposition. */
internal fun freezeWorkoutTargets(state:WorkoutSessionState,sessions:List<SessionRecord>,readiness:Int?):WorkoutSessionState =
    state.copy(exercises=state.exercises.map { saved ->
        val ex=requireNotNull(saved.exerciseSnapshot)
        val rec=loadRecommendationForExercise(ex,sessions,minOf(exerciseRecovery(ex,recoveryScores(sessions)),readiness?:100))
        saved.copy(rpe=runnerEffort(saved.rpe),setRestSec=runnerSetRestSeconds(ex,saved.setRestSec),sets=saved.sets.map { set ->
            set.copy(targetLoadKg=set.targetLoadKg?:rec.loadKg?.takeIf{ex.resistanceMode==ResistanceMode.EXTERNAL_LOAD},
                targetAssistanceKg=set.targetAssistanceKg?:rec.assistanceKg?.takeIf{ex.resistanceMode==ResistanceMode.ASSISTED})
        })
    })
