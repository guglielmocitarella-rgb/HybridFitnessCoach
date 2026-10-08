package com.hybridfitnesscoach.app.domain.v3

import com.hybridfitnesscoach.app.HybridFitnessCoachApplication
import com.hybridfitnesscoach.app.trainingLocations
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HybridFitnessCoachApplication::class)
class V3PackagedAssetTest {
    @Test
    fun packagedAndroidAssetLoadsThroughAssetManagerAndDrivesApplicationRuntime() = runBlocking {
        val application = RuntimeEnvironment.getApplication() as HybridFitnessCoachApplication
        val component = application.v3RuntimeComponent
        component.activate(trainingLocations.single { it.id == "builtin:home" })
        val ready = withTimeout(120_000) { component.state.first { it is V3RuntimeState.Ready } }
            as V3RuntimeState.Ready
        val loaded = ready.loadResult
        val diagnostics = requireNotNull(component.diagnostics())

        assertEquals(1087, loaded.records.size)
        assertEquals(1087, loaded.records.map { it.id.value }.toSet().size)
        assertTrue(loaded.records.all { it.version.schemaVersion == "3.7" })
        assertEquals(1087, loaded.audit.inputRecords)
        assertEquals(1087, loaded.audit.parsedRecords)
        assertEquals(1087, loaded.audit.validatedRecords)
        assertEquals(1087, loaded.audit.mappedRecords)
        assertEquals(0, loaded.audit.silentSemanticLoss)
        val vocabulary = V3LoadedEquipmentVocabulary.from(loaded)
        assertTrue("V3 equipment vocabulary is empty", vocabulary.isNotEmpty())

        assertEquals("Ready", diagnostics.state)
        assertEquals("3.7", diagnostics.schemaVersion)
        assertEquals(1087, diagnostics.recordCount)
        assertEquals(1, diagnostics.loadAttempts)
        assertNotNull(diagnostics.loadDurationMillis)
        assertFalse(
            "The packaged JSON must not be parsed on the main thread",
            diagnostics.loadThreadIsMain ?: true
        )

        for (profile in trainingLocations) {
            component.activate(profile)
            val profileState = component.state.value as V3RuntimeState.Ready
            assertEquals(profile.id, profileState.environment.locationId)
            assertEquals(1, requireNotNull(component.diagnostics()).loadAttempts)
            assertEquals(
                profile.capabilities.mapNotNull { native ->
                    vocabulary.singleOrNull { it.value == native.token }
                }.toSet(),
                profileState.environment.environment.equipment
            )
            assertFalse(
                profileState.environment.environment.equipment.any { mapped ->
                    profile.capabilities.none { it.token == mapped.value }
                }
            )
        }

        val homeEnvironment = V3NativeEnvironmentAdapter.fromLocation(
            trainingLocations.single { it.id == "builtin:home" }, loaded
        ).environment.equipment
        val parkEnvironment = V3NativeEnvironmentAdapter.fromLocation(
            trainingLocations.single { it.id == "builtin:outdoor" }, loaded
        ).environment.equipment
        assertFalse("V3 must preserve the native Casa/Parco distinction", homeEnvironment == parkEnvironment)
    }
}
