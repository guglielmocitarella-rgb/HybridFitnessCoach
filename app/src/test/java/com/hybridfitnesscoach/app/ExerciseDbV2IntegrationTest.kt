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
    private fun records() = ExerciseDbV2.load(RuntimeEnvironment.getApplication())

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
}
