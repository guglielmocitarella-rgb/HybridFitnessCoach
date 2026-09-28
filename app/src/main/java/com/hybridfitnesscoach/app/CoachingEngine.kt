package com.hybridfitnesscoach.app

import kotlin.math.floor
import kotlin.math.ceil

/** Snapshot shared by preview and runner. Missing history never implies failure. */
data class CoachingContext(
    val plan:AdaptiveWorkoutPlan,
    val recovery:Map<String,Int>,
    val readiness:Int?,
    val evidenceSessions:Int,
    val plannedEvidence:Int=0,
    val planDisruptions:Int=0,
    val upcomingSessions:Int=0
)

internal fun coachingContext(sessions:List<SessionRecord>, readiness:Int?=null, now:Long=System.currentTimeMillis()):CoachingContext {
    val recent=sessions.filter{it.timestamp<=now && now-it.timestamp<=28L*86_400_000}.sortedByDescending{it.timestamp}.take(5)
    val recovery=recoveryScores(sessions,now)
    val planned=recent.sumOf{if(it.structuredExercises.isNotEmpty())it.structuredExercises.sumOf{e->e.sets.size} else it.totalSets}
    val done=recent.sumOf{actualCompletedSets(it)}
    val rpes=recent.flatMap{session ->
        val exerciseRpes=session.structuredExercises.mapNotNull{it.rpe}.filter{it in 1..10}
        if(exerciseRpes.isNotEmpty())exerciseRpes else listOf(session.workoutRpe).filter{it in 1..10}
    }
    val recoveryScore=recovery.values.minOrNull()?:100
    val base=adaptiveWorkoutPlan(if(planned>0)done.toDouble()/planned else 0.8,if(rpes.isNotEmpty())rpes.average() else 8.5,recoveryScore,readiness)
    // A single successful workout is not a stable trend.
    val plan=if(base.decision==ProgressionDecision.ADVANCE && (recent.size<2 || rpes.isEmpty()))
        adaptiveWorkoutPlan(0.8,8.5,recoveryScore,readiness) else base
    val evidence=if(recent.isEmpty())"Calibrazione iniziale: nessuno storico recente" else "Basato su ${recent.size} workout recenti e serie effettive"
    return CoachingContext(plan.copy(notes=plan.notes+evidence),recovery,readiness,recent.size)
}

/**
 * Combines execution history with the persisted calendar without inventing physiological
 * recovery rules. Plan state can conservatively suppress ADVANCE after a recent
 * partial/skipped planned session; it never manufactures a progression signal.
 */
internal fun coachingContext(
    sessions:List<SessionRecord>,
    readiness:Int?,
    trainingPlan:TrainingPlan?,
    todayEpochDay:Long
):CoachingContext {
    val history=coachingContext(sessions,readiness)
    val plan=trainingPlan ?: return history
    val recentStart=todayEpochDay-28
    val recent=plan.workouts.filter{it.epochDay in recentStart..todayEpochDay}
    val disruptions=recent.count{it.state==PlannedWorkoutState.PARTIAL || it.state==PlannedWorkoutState.SKIPPED}
    val completed=recent.count{it.state==PlannedWorkoutState.COMPLETED}
    val upcoming=plan.workouts.count{it.state in setOf(PlannedWorkoutState.PLANNED,PlannedWorkoutState.RESCHEDULED,PlannedWorkoutState.ADAPTED)}
    val base=history.plan
    val adjusted=if(base.decision==ProgressionDecision.ADVANCE && disruptions>0)
        base.copy(
            intensityFactor=1.0,
            volumeFactor=1.0,
            decision=ProgressionDecision.MAINTAIN,
            notes=base.notes+listOf("Progressione sospesa: piano recente con sessioni parziali o saltate")
        )
    else base
    val planNote=when {
        recent.isEmpty()->"Piano attivo: nessuna sessione pianificata conclusa nelle ultime 4 settimane"
        else->"Piano: $completed completate, $disruptions parziali/saltate, $upcoming da eseguire"
    }
    return history.copy(
        plan=adjusted.copy(notes=adjusted.notes+planNote),
        plannedEvidence=recent.size,
        planDisruptions=disruptions,
        upcomingSessions=upcoming
    )
}

internal fun exerciseRecovery(ex:Exercise,recovery:Map<String,Int>):Int =
    recovery.filterKeys{ex.group.contains(it,true)||it in ex.secondaryGroups}.values.minOrNull()?:100

internal fun applyCoaching(items:List<Exercise>,context:CoachingContext):List<Exercise> = items.map { ex ->
    val p=prescriptionFor(ex)
    val reduced=context.plan.volumeFactor<1.0
    val sets=if(reduced)floor(p.sets*context.plan.volumeFactor).toInt().coerceAtLeast(1) else p.sets
    ex.copy(targetSets=sets,
        targetValue=when {
            context.plan.intensityFactor<1.0->floor(p.value*context.plan.intensityFactor).coerceAtLeast(1.0)
            context.plan.intensityFactor>1.0 && ex.resistanceMode!=ResistanceMode.EXTERNAL_LOAD && ex.resistanceMode!=ResistanceMode.ASSISTED->ceil(p.value*context.plan.intensityFactor)
            else->p.value
        })
    // APP_RULE: reduced workload retains the baseline rest. Volume reduction is not
    // evidence for a proportional rest increase; never compound rest on adapted copies.
}

/** The pool contains base catalogue prescriptions; existing/pinned copies are already adapted. */
internal fun regenerateCoachedWorkout(original:List<Exercise>,pool:List<Exercise>,pinned:Set<String>,context:CoachingContext):List<Exercise> {
    val ranked=pool.distinctBy{it.name}.sortedBy{candidate->if(original.any{it.name==candidate.name})1 else 0}
    val adapted=applyCoaching(ranked,context)
    return sequenceWorkout(regenerateUnpinned(original,adapted,pinned),pinned)
}

/** Persist execution from actual sets; counters and planned targets cannot manufacture performance. */
internal fun recordedExerciseState(index:Int,ex:Exercise,actual:List<WorkoutSetState>?,rpe:Int?,skipped:Boolean,pinned:Boolean):WorkoutExerciseState {
    val planned=plannedExerciseState(index,ex)
    val sets=actual?:planned.sets
    val done=sets.count{it.completed}
    val status=when {
        skipped->WorkoutExerciseStatus.SKIPPED
        sets.isNotEmpty()&&done==sets.size->WorkoutExerciseStatus.COMPLETED
        done>0->WorkoutExerciseStatus.PARTIAL
        else->WorkoutExerciseStatus.PENDING
    }
    return planned.copy(sets=sets,status=status,rpe=rpe,pinned=pinned)
}

internal fun coachingLabel(decision:ProgressionDecision)=when(decision){
    ProgressionDecision.ADVANCE->"Progressione controllata"
    ProgressionDecision.MAINTAIN->"Consolida la prestazione"
    ProgressionDecision.REGRESS->"Riparti con più margine"
    ProgressionDecision.DELOAD->"Oggi recupero attivo"
}

/** Resume persisted prescriptions, never regenerate targets from today's readiness. */
internal fun restoreExercises(state:WorkoutSessionState):List<Exercise> = state.exercises.mapNotNull { saved ->
    exercises.firstOrNull{it.name==saved.exerciseName}?.let { ex ->
        val first=saved.sets.firstOrNull()
        ex.copy(targetSets=saved.sets.size,measure=saved.measure,
            targetValue=first?.targetReps?.toDouble()?:first?.targetSeconds?.toDouble()?:first?.targetDistanceMeters?:ex.targetValue,
            setRestSec=saved.setRestSec,exerciseRestSec=saved.exerciseRestSec)
    }
}
