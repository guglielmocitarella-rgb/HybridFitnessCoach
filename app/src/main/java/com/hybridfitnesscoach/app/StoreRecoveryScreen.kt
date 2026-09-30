package com.hybridfitnesscoach.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Read-only access deliberately keeps recovered data out of coaching and saving. */
@Composable
internal fun StoreRecoveryScreen(store: AppStore) {
    val athlete = store.loadAthlete()
    val sessions = store.loadSessions()
    val plan = store.loadTrainingPlan()
    val active = store.loadActiveWorkout()
    val equipment = store.loadTrainingLocations()
    val progression = store.loadProgressionNodes()
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("Dati da recuperare", style = MaterialTheme.typography.headlineMedium)
                Text("Alcuni dati salvati non sono leggibili o richiedono una versione diversa dell’app. " +
                    "Gli originali sono conservati. Modifiche e allenamenti sono sospesi per proteggerli. " +
                    "Non cancellare i dati dell’app e non disinstallarla: conserva i dati per il recupero con assistenza.")
                Text("Dati leggibili (possono essere incompleti)", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp))
                Text("Atleta: ${athlete.name}")
                equipment.forEach { Text("${it.name}: ${it.capabilities.map(EquipmentCatalog::displayName).sortedBy { name -> name.lowercase() }.joinToString()}") }
                progression.forEach { (skill, node) -> Text("${skill.removePrefix("skill_node_")}: $node") }
                plan?.let {
                    Text("Piano: ${it.id}")
                    it.workouts.forEach { workout ->
                        Text("${workout.id}: ${workout.state} · ${java.time.LocalDate.ofEpochDay(workout.epochDay)}")
                        Text(workout.exercises.joinToString { exercise -> exercise.name })
                    }
                }
                active?.let {
                    Text("Allenamento in corso: ${it.profileName}")
                    RecoveredExercises(it.exercises)
                }
                Text("Storico leggibile: ${sessions.size} sessioni", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp))
                sessions.forEach { session ->
                    Text("${java.time.Instant.ofEpochMilli(session.timestamp)} · ${session.profileName}")
                    Text("Serie registrate: ${session.completedSets}/${session.totalSets}")
                    if (session.structuredExercises.isEmpty()) Text(session.exerciseNames.joinToString())
                    RecoveredExercises(session.structuredExercises)
                }
                Text("Dettagli per assistenza: ${store.issues.joinToString()}", modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}

@Composable
private fun RecoveredExercises(exercises: List<WorkoutExerciseState>) {
    exercises.forEach { exercise ->
        Text(exercise.exerciseName)
        exercise.sets.forEach { set ->
            val actual = listOfNotNull(set.actualReps?.let { "$it ripetizioni" },
                set.actualSeconds?.let { "$it s" }, set.actualDistanceMeters?.let { "$it m" },
                set.actualLoadKg?.let { "$it kg" }, set.actualAssistanceKg?.let { "$it kg assistenza" })
            Text("Serie ${set.index + 1}: ${if (set.completed) "completata" else "non completata"} · ${actual.joinToString()}")
        }
    }
}
