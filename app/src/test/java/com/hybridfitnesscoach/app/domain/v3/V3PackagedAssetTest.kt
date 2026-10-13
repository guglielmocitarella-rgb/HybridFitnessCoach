package com.hybridfitnesscoach.app.domain.v3

import android.content.pm.ApplicationInfo
import com.hybridfitnesscoach.app.HybridFitnessCoachApplication
import java.io.File
import java.util.zip.ZipFile
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
@Config(sdk = [28], application = HybridFitnessCoachApplication::class)
class V3PackagedAssetTest {
    @Test
    fun packagedAndroidAssetLoadsThroughAssetManagerAndDrivesApplicationRuntime() = runBlocking {
        val application = RuntimeEnvironment.getApplication() as HybridFitnessCoachApplication
        application.applicationInfo.flags = application.applicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE
        val apk = File(requireNotNull(System.getProperty("hfc.v3.apk")) { "Missing packaged debug APK property" })
        assertTrue("Debug APK was not packaged before the runtime test", apk.isFile)
        ZipFile(apk).use { archive ->
            val entryName = "assets/${V3AndroidAssetReader.ASSET_PATH}"
            val entry = requireNotNull(archive.getEntry(entryName)) { "V3 asset missing from APK: $entryName" }
            assertTrue("Packaged V3 asset is empty", entry.size > 0L)
        }
        attachApkAssets(application.assets, apk)

        val component = application.v3RuntimeComponent
        component.activate(trainingLocations.single { it.id == "builtin:home" })
        val terminal = withTimeout(240_000) {
            component.state.first { it is V3RuntimeState.Ready || it is V3RuntimeState.Failed }
        }
        val ready = when (terminal) {
            is V3RuntimeState.Ready -> terminal
            is V3RuntimeState.Failed -> throw AssertionError("V3 packaged asset load failed: ${terminal.cause}")
            else -> error("Unexpected terminal V3 state")
        }
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
        assertFalse("BODYWEIGHT is implicit, not an invented equipment token", EquipmentId("bodyweight") in vocabulary)
        val noEquipment = loaded.records.filter {
            it.equipment.informationState == EquipmentInformationState.KNOWN &&
                it.equipment.mode == EquipmentRequirementMode.NONE &&
                it.resistance.source.value == ResistanceSource.BODYWEIGHT
        }
        assertTrue("The packaged canonical database must contain bodyweight exercises without equipment", noEquipment.isNotEmpty())
        val high = SemanticValue.known(CapabilityLevel.HIGH)
        noEquipment.forEach { exercise ->
            val reasons = V3SelectionEngine.evaluate(exercise, V3AthleteCapabilities(high, high, high, high),
                V3SelectionEnvironment(), V3SelectionIntent()).reasons
            assertFalse("No-equipment bodyweight must not require a bodyweight inventory token",
                V3SelectionRejection.EQUIPMENT_UNAVAILABLE in reasons || V3SelectionRejection.EQUIPMENT_UNRESOLVED in reasons)
        }

        val athleteContext = V3NativeAthleteContextAdapter.fromNative(
            com.hybridfitnesscoach.app.AthleteProfile(name = "Runtime test", goal = "Forza", assessedPushCapacity = 2),
            emptyList()
        )
        component.activate(trainingLocations.single { it.id == "builtin:home" }, athleteContext)
        val personalized = component.state.value as V3RuntimeState.Ready
        assertEquals(athleteContext, personalized.athleteContext)
        org.junit.Assert.assertSame(loaded, personalized.loadResult)
        assertEquals(1, requireNotNull(component.diagnostics()).loadAttempts)

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

        val homeResult = V3NativeEnvironmentAdapter.fromLocation(
            trainingLocations.single { it.id == "builtin:home" }, loaded
        )
        val parkResult = V3NativeEnvironmentAdapter.fromLocation(
            trainingLocations.single { it.id == "builtin:outdoor" }, loaded
        )
        assertTrue(
            "V3 context must distinguish Casa and Parco with mapped or explicitly unmapped inventory",
            homeResult.environment.equipment != parkResult.environment.equipment ||
                homeResult.unmappedNativeEquipment != parkResult.unmappedNativeEquipment
        )
        println(
            "V3_RUNTIME_DIAGNOSTICS state=${diagnostics.state} records=${diagnostics.recordCount} " +
                "schema=${diagnostics.schemaVersion} attempts=${diagnostics.loadAttempts} " +
                "loadDurationMs=${diagnostics.loadDurationMillis} mainThread=${diagnostics.loadThreadIsMain} " +
                "homeRecognized=${homeResult.environment.equipment.size} " +
                "homeUnmapped=${homeResult.unmappedNativeEquipment.map { it.token }.sorted()} " +
                "parkRecognized=${parkResult.environment.equipment.size} " +
                "parkUnmapped=${parkResult.unmappedNativeEquipment.map { it.token }.sorted()}"
        )
    }

    private fun attachApkAssets(assetManager: android.content.res.AssetManager, apk: File) {
        // Call the real SDK 28 hidden API. With Android resources enabled Robolectric
        // uses the binary-resource shadow, which has no legacy addAssetPath method.
        val method = android.content.res.AssetManager::class.java.getMethod(
            "addAssetPath", String::class.java
        )
        val cookie = method.invoke(assetManager, apk.absolutePath) as Number
        assertTrue("Robolectric rejected the packaged APK asset path", cookie.toInt() > 0)
    }
}
