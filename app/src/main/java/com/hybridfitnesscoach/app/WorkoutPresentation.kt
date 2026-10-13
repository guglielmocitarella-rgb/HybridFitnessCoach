package com.hybridfitnesscoach.app

import java.util.Locale

internal data class HistoryExerciseDetail(
    val name:String, val completedSets:Int?, val totalSets:Int?, val lines:List<String>
)

private fun displayNumber(value:Double):String =
    if(value % 1.0 == 0.0)value.toLong().toString() else String.format(Locale.ITALY,"%.1f",value)

/** Executed external-load volume: completed reps × actually recorded load. */
internal fun loadedVolumeKg(exercise:WorkoutExerciseState):Double =
    exercise.sets.asSequence()
        .filter{it.completed}
        .sumOf{set -> (set.actualReps?:0).toDouble() * (set.actualLoadKg?:0.0)}

/** Display persisted performance only. Legacy counters cannot reconstruct actual sets. */
internal fun historyExerciseDetails(session:SessionRecord):List<HistoryExerciseDetail> =
    (session.structuredExercises.map{it.exerciseName to it} +
        session.exerciseNames.distinct().filter{name->session.structuredExercises.none{it.exerciseName==name}}.map{it to null}).map { (name,saved) ->
        if(saved==null) HistoryExerciseDetail(name,session.completedByExercise[name],null,
            listOfNotNull("Dati effettivi per serie non disponibili per questo allenamento storico.",
                session.exerciseRpe[name]?.let{"RPE esercizio: $it"}))
        else HistoryExerciseDetail(name,saved.sets.count{it.completed},saved.sets.size,buildList {
            if(saved.status==WorkoutExerciseStatus.SKIPPED)add("Esercizio saltato; le serie eseguite sono conservate.")
            add("Recupero prescritto: ${saved.setRestSec} sec tra serie • ${saved.exerciseRestSec} sec tra esercizi")
            saved.sets.forEach { set ->
                add("Serie ${set.index+1}" + if(set.completed) "" else " — non completata")
                set.targetReps?.let{add("Target: $it reps")}
                set.targetSeconds?.let{add("Target: $it sec")}
                set.targetDistanceMeters?.let{add("Target: ${displayNumber(it)} m")}
                set.targetLoadKg?.let{add("Carico suggerito: ${displayNumber(it)} kg")}
                set.targetAssistanceKg?.let{add("Assistenza suggerita: ${displayNumber(it)} kg")}
                if(set.completed) {
                    set.actualReps?.let{add("Effettive: $it reps")}
                    set.actualSeconds?.let{add("Effettivo: $it sec")}
                    set.actualDistanceMeters?.let{add("Effettivo: ${displayNumber(it)} m")}
                    set.actualLoadKg?.let{add("Carico: ${displayNumber(it)} kg")}
                    set.actualAssistanceKg?.let{add("Assistenza: ${displayNumber(it)} kg")}
                    set.restUsedSec?.let{add("Recupero effettivo: $it sec")}
                    set.actualRpe?.let{add("RPE serie: $it/10")}
                }
            }
            val loadedVolume=loadedVolumeKg(saved)
            if(loadedVolume>0.0)add("Volume con carico: ${displayNumber(loadedVolume)} kg")
            val rpe=saved.rpe ?: session.exerciseRpe[name]
            if(rpe!=null)add("RPE esercizio: $rpe")
            else add("RPE mancante: la prestazione non può validare una progressione che richiede RPE.")
        })
    }
