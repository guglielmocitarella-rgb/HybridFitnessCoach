package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class ExerciseDbV2IntegrationTest {
    private fun records():List<ExerciseV2Record> {
        val f=listOf(java.io.File("src/main/assets/exercise-db-v2/exercises.json"),java.io.File("app/src/main/assets/exercise-db-v2/exercises.json")).first{it.exists()}
        return ExerciseDbV2.parseExercises(f.readText())
    }

    @Test fun canonicalAssetLoadsWithStableUniqueIds() {
        val db=records()
        assertEquals(587,db.size)
        assertEquals(db.size,db.map{it.id}.distinct().size)
        assertTrue(db.all{it.id.isNotBlank() && it.nameIt.isNotBlank() && it.sources.isNotEmpty()})
    }

    @Test fun canonicalRecordsMapToExecutableAppExercises() {
        val mapped=records().map{ExerciseDbV2.toLegacyExercise(it)}
        assertEquals(587,mapped.size)
        assertTrue(mapped.all{it.name.isNotBlank() && it.progressionKey!=null})
    }

    @Test fun legacyResolutionIsFailClosedForUnknownNames() {
        val db=records()
        assertNull(ExerciseDbV2.resolveLegacy("definitely-not-a-real-exercise",db))
        val known=db.first()
        assertEquals(known.id,ExerciseDbV2.resolveLegacy(known.canonicalNameEn,db)?.id)
        assertEquals(known.id,ExerciseDbV2.resolveLegacy(known.nameIt,db)?.id)
    }

    @Test fun runtimeInstallUsesCanonicalIdsWithoutDestroyingLegacySnapshots() {
        val snapshot=Exercise("Legacy snapshot","Core","Corpo libero","Principiante",targetSets=7,targetValue=13.0)
        installExerciseDbV2(records())
        assertEquals(587,exercises.size)
        assertEquals(587,exercises.mapNotNull{it.progressionKey}.distinct().size)
        assertEquals(7,snapshot.targetSets)
        assertEquals(13.0,snapshot.targetValue!!,0.001)
    }
    @Test fun canonicalEquipmentMapsIntoProfileTaxonomyAndGeneratorModel() {
        val db=records()
        val barbell=db.first{ "barbell" in it.equipment }
        val mapped=ExerciseDbV2.toLegacyExercise(barbell)
        assertEquals("Bilanciere",mapped.equipment)
        assertTrue(equipmentCompatible(mapped,EquipmentProfile("Test","",listOf("Bilanciere"))))
        assertNotEquals(ResistanceMode.NONE,mapped.resistanceMode)
    }

    @Test fun sourceBackedProgressionGraphHasReferentialIntegrityForRuntimeIds() {
        val ids=records().map{it.id}.toSet()
        val f=listOf(java.io.File("src/main/assets/exercise-db-v2/progressions.json"),java.io.File("app/src/main/assets/exercise-db-v2/progressions.json")).first{it.exists()}
        val context=RuntimeEnvironment.getApplication()
        val edges=ExerciseDbV2.loadProgressions(context)
        assertEquals(113,edges.size)
        assertTrue(edges.all{it.from in ids && it.to in ids && it.sources.isNotEmpty()})
        assertTrue(edges.any{it.relationship=="MAIN_PROGRESSION"})
    }

    @Test fun canonicalResistanceRecordRemainsCompatibleWithPrescriptionEngine() {
        val mapped=records().asSequence().map{ExerciseDbV2.toLegacyExercise(it)}
            .first{it.measure==ExerciseMeasure.REPS && it.resistanceMode!=ResistanceMode.NONE &&
                it.category in setOf(ExerciseCategory.PUSH,ExerciseCategory.PULL,ExerciseCategory.LEGS,ExerciseCategory.CORE)}
        val context=PrescriptionContext("Forza","Intermedio",mapped.category,5,5,5,null,80,null)
        assertNotNull(SourcePrescriptionBaselineRegistry.baselineFor(mapped,context))
    }
    @Test fun emptyEquipmentIsUnknownAndFailsClosedForConstrainedGeneration() {
        val row=records().first{it.equipment.isEmpty()}
        val mapped=ExerciseDbV2.toLegacyExercise(row)
        assertEquals("Non specificato",mapped.equipment)
        assertFalse(equipmentCompatible(mapped,EquipmentProfile("Home","",emptyList())))
    }

    @Test fun multiTokenEquipmentPreservesAllRequiredResources() {
        val row=records().first{it.equipment.size>1}
        val mapped=ExerciseDbV2.toLegacyExercise(row)
        assertTrue(mapped.equipment.contains(" / "))
        val all=EquipmentProfile("All","",mapped.equipment.split(" / "))
        assertTrue(equipmentCompatible(mapped,all))
        assertFalse(equipmentCompatible(mapped,EquipmentProfile("Partial","",listOf(mapped.equipment.substringBefore(" / ")))))
    }
}
