package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class Rc2HistoryDetailTest {
    @Test fun persistedExerciseModelDoesNotInventRpeDefault() {
        assertNull(WorkoutExerciseState("Plank").rpe)
    }
    @Test fun historyShowsActualRepsLoadAndRpeSeparatelyFromTargets() {
        val session = SessionRecord(1, "Palestra", 1, 2, structuredExercises = listOf(
            WorkoutExerciseState("Panca", sets = listOf(
                WorkoutSetState(0, targetReps = 6, actualReps = 5, actualLoadKg = 10.0, completed = true),
                WorkoutSetState(1, targetReps = 6)
            ), rpe = 5)
        ))
        val detail = historyExerciseDetails(session).single()
        assertEquals(1, detail.completedSets)
        assertEquals(2, detail.totalSets)
        val text = detail.lines.joinToString("\n")
        assertTrue(text.contains("Target: 6 reps"))
        assertTrue(text.contains("Effettive: 5 reps"))
        assertTrue(text.contains("Carico: 10 kg"))
        assertTrue(text.contains("RPE esercizio: 5"))
        assertFalse(text.contains("Effettive: 6 reps"))
    }
    @Test fun timedHistoryDoesNotInventLoadRepsOrMissingRpe() {
        val session = SessionRecord(1, "Custom", 1, 1, structuredExercises = listOf(
            WorkoutExerciseState("Plank", measure = ExerciseMeasure.SECONDS, sets = listOf(
                WorkoutSetState(0, targetSeconds = 30, actualSeconds = 22, completed = true)
            ))
        ))
        val text = historyExerciseDetails(session).single().lines.joinToString("\n")
        assertTrue(text.contains("Target: 30 sec"))
        assertTrue(text.contains("Effettivo: 22 sec"))
        assertTrue(text.contains("RPE mancante"))
        assertFalse(text.contains("Carico:"))
        assertFalse(text.contains("reps"))
    }
    @Test fun historyShowsExecutedLoadedVolumeForWeightedRepExercise() {
        val session=SessionRecord(1,"Palestra",3,3,structuredExercises=listOf(
            WorkoutExerciseState("Panca",sets=listOf(
                WorkoutSetState(0,actualReps=8,actualLoadKg=50.0,completed=true),
                WorkoutSetState(1,actualReps=7,actualLoadKg=50.0,completed=true),
                WorkoutSetState(2,actualReps=6,actualLoadKg=55.0,completed=true)
            ),rpe=7)
        ))
        val detail=historyExerciseDetails(session).single()
        assertEquals(1080.0,loadedVolumeKg(session.structuredExercises.single()),0.0)
        assertTrue(detail.lines.contains("Volume con carico: 1080 kg"))
    }

    @Test fun loadedVolumeIgnoresIncompleteSetsAndSetsWithoutBothLoadAndReps() {
        val exercise=WorkoutExerciseState("Panca",sets=listOf(
            WorkoutSetState(0,actualReps=8,actualLoadKg=50.0,completed=true),
            WorkoutSetState(1,actualReps=8,actualLoadKg=50.0,completed=false),
            WorkoutSetState(2,actualReps=8,completed=true),
            WorkoutSetState(3,actualLoadKg=50.0,completed=true)
        ))
        assertEquals(400.0,loadedVolumeKg(exercise),0.0)
    }

    @Test fun bodyweightOrTimedHistoryDoesNotInventLoadedVolume() {
        val exercise=WorkoutExerciseState("Plank",measure=ExerciseMeasure.SECONDS,sets=listOf(
            WorkoutSetState(0,actualSeconds=30,completed=true)
        ))
        assertEquals(0.0,loadedVolumeKg(exercise),0.0)
        val session=SessionRecord(1,"Casa",1,1,structuredExercises=listOf(exercise))
        assertFalse(historyExerciseDetails(session).single().lines.any{it.startsWith("Volume con carico:")})
    }

    @Test fun exerciseDetailStatisticsUseOnlyPersistedExecutedPerformance() {
        val history=listOf(
            SessionRecord(1,"Palestra",2,3,structuredExercises=listOf(
                WorkoutExerciseState("Panca",sets=listOf(
                    WorkoutSetState(0,targetReps=10,actualReps=8,actualLoadKg=50.0,completed=true),
                    WorkoutSetState(1,targetReps=10,actualReps=7,actualLoadKg=55.0,completed=true),
                    WorkoutSetState(2,targetReps=10)
                ))
            )),
            SessionRecord(2,"Palestra",1,1,structuredExercises=listOf(
                WorkoutExerciseState("PANCA",sets=listOf(
                    WorkoutSetState(0,targetReps=10,actualReps=6,actualLoadKg=60.0,completed=true)
                ))
            ))
        )
        val stats=ExerciseStatistics.fromHistory("panca",history)
        assertEquals(2,stats.sessions)
        assertEquals(3,stats.completedSets)
        assertEquals(21,stats.totalReps)
        assertEquals(1085.0,stats.totalLoadVolumeKg,0.0)
        assertEquals(60.0,stats.maxLoadKg!!,0.0)
    }

    @Test fun exerciseDetailStatisticsDoNotInventTargetsAsExecutedRepetitions() {
        val history=listOf(SessionRecord(1,"Casa",0,2,structuredExercises=listOf(
            WorkoutExerciseState("Push-up",sets=listOf(
                WorkoutSetState(0,targetReps=20),
                WorkoutSetState(1,targetReps=20)
            ))
        )))
        val stats=ExerciseStatistics.fromHistory("Push-up",history)
        assertEquals(1,stats.sessions)
        assertEquals(0,stats.completedSets)
        assertEquals(0,stats.totalReps)
        assertEquals(0.0,stats.totalLoadVolumeKg,0.0)
        assertNull(stats.maxLoadKg)
    }

}
