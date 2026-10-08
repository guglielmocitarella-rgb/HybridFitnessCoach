package com.hybridfitnesscoach.app.domain.v3

import com.hybridfitnesscoach.app.EquipmentId as NativeEquipmentId
import com.hybridfitnesscoach.app.TrainingLocation
import com.hybridfitnesscoach.app.TrainingLocationKind
import com.hybridfitnesscoach.app.trainingLocations
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class V3NativeEnvironmentAdapterTest {
    private val vocabulary = NativeEquipmentId.entries.map { EquipmentId(it.token) }.toSet()
    private fun location(id: String, vararg equipment: NativeEquipmentId) =
        TrainingLocation(id, id, "", TrainingLocationKind.CUSTOM, equipment.toSet())

    @Test fun completeInventoryMapsExactTokens() {
        val result = V3NativeEnvironmentAdapter.fromLocation(
            location("gym", *NativeEquipmentId.entries.toTypedArray()), vocabulary
        )
        assertEquals(vocabulary, result.environment.equipment)
        assertTrue(result.unmappedNativeEquipment.isEmpty())
    }

    @Test fun partialInventoryDoesNotGainMissingEquipment() {
        val result = V3NativeEnvironmentAdapter.fromLocation(
            location("home", NativeEquipmentId.BODYWEIGHT, NativeEquipmentId.RESISTANCE_BAND), vocabulary
        )
        assertEquals(setOf(EquipmentId("bodyweight"), EquipmentId("resistance_band")), result.environment.equipment)
        assertFalse(EquipmentId("barbell") in result.environment.equipment)
    }

    @Test fun emptyLocationHasNoEquipment() {
        val result = V3NativeEnvironmentAdapter.fromLocation(location("empty"), vocabulary)
        assertTrue(result.environment.equipment.isEmpty())
        assertEquals(V3Availability.AVAILABLE, result.equipmentKnowledge)
    }

    @Test fun unknownVocabularyNeverGrantsEquipment() {
        val result = V3NativeEnvironmentAdapter.fromLocation(
            location("gym", NativeEquipmentId.BARBELL), emptySet()
        )
        assertTrue(result.environment.equipment.isEmpty())
        assertEquals(setOf(NativeEquipmentId.BARBELL), result.unmappedNativeEquipment)
        assertEquals(V3Availability.UNKNOWN, result.equipmentKnowledge)
    }

    @Test fun switchingLocationChangesOnlyActualInventory() {
        val home = V3NativeEnvironmentAdapter.fromLocation(location("home", NativeEquipmentId.BODYWEIGHT), vocabulary)
        val gym = V3NativeEnvironmentAdapter.fromLocation(location("gym", NativeEquipmentId.BARBELL), vocabulary)
        assertNotEquals(home.environment.equipment, gym.environment.equipment)
        assertEquals("gym", gym.locationId)
    }

    @Test fun deterministicRegardlessOfInventoryOrder() {
        val a = V3NativeEnvironmentAdapter.fromLocation(
            location("x", NativeEquipmentId.BARBELL, NativeEquipmentId.BENCH), vocabulary
        )
        val b = V3NativeEnvironmentAdapter.fromLocation(
            location("x", NativeEquipmentId.BENCH, NativeEquipmentId.BARBELL), vocabulary
        )
        assertEquals(a, b)
    }

    @Test fun noArtificialAssistanceOrExternalLoad() {
        val result = V3NativeEnvironmentAdapter.fromLocation(
            location("gym", NativeEquipmentId.MACHINE, NativeEquipmentId.BARBELL), vocabulary
        )
        assertFalse(result.environment.assistanceAvailable)
        assertFalse(result.environment.externalLoadAvailable)
        assertEquals(V3Availability.UNKNOWN, result.assistanceKnowledge)
        assertEquals(V3Availability.UNKNOWN, result.externalLoadKnowledge)
    }

    @Test fun missingLocationIsUnknownNotAvailable() {
        val result = V3NativeEnvironmentAdapter.fromLocation(null, vocabulary)
        assertNull(result.locationId)
        assertEquals(V3Availability.UNKNOWN, result.equipmentKnowledge)
        assertEquals(V3SelectionEnvironment(), result.environment)
    }

    @Test fun builtInGymHomeAndParkMapOnlyTheirDeclaredV3Equipment() {
        val asset = File(requireNotNull(System.getProperty("hfc.v3.asset")) { "Missing hfc.v3.asset test property" })
        val loaded = V3AssetLoader.loadAuthoritative(asset.readText(Charsets.UTF_8))
        val vocabularyFromAsset = V3LoadedEquipmentVocabulary.from(loaded)
        val results = trainingLocations.associate { location ->
            location.id to V3NativeEnvironmentAdapter.fromLocation(location, loaded)
        }

        assertEquals(setOf("builtin:gym", "builtin:home", "builtin:outdoor"), results.keys)
        val home = results.getValue("builtin:home")
        val park = results.getValue("builtin:outdoor")
        for ((location, result) in trainingLocations.map { it to results.getValue(it.id) }) {
            assertEquals(location.id, result.locationId)
            val expectedRecognized = location.capabilities.mapNotNull { native ->
                vocabularyFromAsset.singleOrNull { it.value == native.token }
            }.toSet()
            assertEquals(expectedRecognized, result.environment.equipment)
            val expectedUnmapped = location.capabilities.filterTo(linkedSetOf()) { native ->
                vocabularyFromAsset.none { it.value == native.token }
            }
            assertEquals(expectedUnmapped, result.unmappedNativeEquipment)
            assertEquals(
                if (expectedUnmapped.isEmpty()) V3Availability.AVAILABLE else V3Availability.UNKNOWN,
                result.equipmentKnowledge
            )
            assertFalse(result.environment.equipment.any { mapped ->
                location.capabilities.none { it.token == mapped.value }
            })
            assertFalse(result.environment.assistanceAvailable)
            assertFalse(result.environment.externalLoadAvailable)
            assertEquals(V3Availability.UNKNOWN, result.assistanceKnowledge)
            assertEquals(V3Availability.UNKNOWN, result.externalLoadKnowledge)

            location.capabilities.forEach { native ->
                val tokenInV3 = vocabularyFromAsset.any { it.value == native.token }
                val mapped = EquipmentId(native.token) in result.environment.equipment
                if (tokenInV3) assertTrue("Present native equipment must be AVAILABLE", mapped)
                else assertFalse("Equipment absent from V3 vocabulary must not be invented", mapped)
            }
            vocabularyFromAsset.forEach { equipment ->
                val native = NativeEquipmentId.entries.singleOrNull { it.token == equipment.value }
                if (native != null && native !in location.capabilities) {
                    assertFalse(
                        "Listed V3 equipment absent from this location is UNAVAILABLE",
                        equipment in result.environment.equipment
                    )
                }
            }
        }
        assertTrue(home.environment.equipment.containsAll(setOf(EquipmentId("bodyweight"), EquipmentId("pull_up_bar"))))
        assertTrue(park.environment.equipment.containsAll(setOf(EquipmentId("bodyweight"), EquipmentId("pull_up_bar"))))
        assertNotEquals(home.environment.equipment, park.environment.equipment)
        assertNotEquals(results.getValue("builtin:gym").environment.equipment, home.environment.equipment)
    }
}
