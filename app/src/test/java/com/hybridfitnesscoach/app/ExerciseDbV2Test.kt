package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ExerciseDbV2Test {
    private fun records():List<ExerciseV2Record> {
        val candidates=listOf(
            "src/main/assets/exercise-db-v2/exercises.json",
            "app/src/main/assets/exercise-db-v2/exercises.json"
        )
        val f=candidates.map(::File).first{it.exists()}
        return ExerciseDbV2.parseExercises(f.readText())
    }

    @Test fun canonicalAssetContainsIntegrationReadyMovementCountAndUniqueStableIds() {
        val rows=records()
        assertEquals(587,rows.size)
        assertEquals(rows.size,rows.map{it.id}.toSet().size)
        assertTrue(rows.all{it.id.isNotBlank()&&it.canonicalNameEn.isNotBlank()&&it.sources.isNotEmpty()})
    }

    @Test fun legacyResolutionIsFailClosedForAmbiguousAliases() {
        val a=ExerciseV2Record("a","Alpha","Alfa",listOf("same"),"STRENGTH","PRESS",emptyList(),null,"REPS",listOf("s"),"HIGH")
        val b=ExerciseV2Record("b","Beta","Beta",listOf("same"),"STRENGTH","PRESS",emptyList(),null,"REPS",listOf("s"),"HIGH")
        val lookup=ExerciseDbV2.uniqueLookup(listOf(a,b))
        assertNull(lookup["same"])
        assertEquals(a,lookup["alpha"])
        assertEquals(a,lookup["alfa"])
    }

    @Test fun adapterPersistsV2StableIdWithoutDestroyingLegacyPrescription() {
        val row=records().first()
        val legacy=Exercise("Legacy","Petto","Bilanciere","Avanzato",targetSets=4,targetValue=6.0,setRestSec=180)
        val mapped=ExerciseDbV2.toLegacyExercise(row,legacy)
        assertEquals(row.id,mapped.progressionKey)
        assertEquals(4,mapped.targetSets)
        assertEquals(6.0,mapped.targetValue!!,0.001)
        assertEquals(180,mapped.setRestSec)
    }
}
