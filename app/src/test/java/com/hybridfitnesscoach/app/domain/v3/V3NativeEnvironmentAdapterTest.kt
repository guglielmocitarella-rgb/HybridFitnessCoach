package com.hybridfitnesscoach.app.domain.v3

import com.hybridfitnesscoach.app.EquipmentId as NativeEquipmentId
import com.hybridfitnesscoach.app.TrainingLocation
import com.hybridfitnesscoach.app.TrainingLocationKind
import org.junit.Assert.*
import org.junit.Test

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
}
