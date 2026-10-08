package com.hybridfitnesscoach.app.domain.v3

import com.hybridfitnesscoach.app.EquipmentId as NativeEquipmentId
import com.hybridfitnesscoach.app.TrainingLocation
import com.hybridfitnesscoach.app.TrainingLocationKind
import org.junit.Assert.*
import org.junit.Test

class V3LoadedEquipmentVocabularyTest {
    private val emptyLoad = V3LoadResult(
        emptyList(), V3LoadAudit(0, 0, 0, 0, 0, 0, 0, 0, 0, emptySet())
    )

    @Test fun emptyLoadedVocabularyHasNoInventedTokens() {
        assertTrue(V3LoadedEquipmentVocabulary.from(emptyLoad).isEmpty())
    }

    @Test fun loadedVocabularyExtractionIsDeterministic() {
        assertEquals(
            V3LoadedEquipmentVocabulary.from(emptyLoad),
            V3LoadedEquipmentVocabulary.fromRecords(emptyLoad.records.reversed())
        )
    }

    @Test fun absentTokenIsUnknownNotGranted() {
        val location = TrainingLocation(
            "gym", "gym", "", TrainingLocationKind.CUSTOM, setOf(NativeEquipmentId.BARBELL)
        )
        val result = V3NativeEnvironmentAdapter.fromLocation(location, emptyLoad)
        assertTrue(result.environment.equipment.isEmpty())
        assertEquals(setOf(NativeEquipmentId.BARBELL), result.unmappedNativeEquipment)
        assertEquals(V3Availability.UNKNOWN, result.equipmentKnowledge)
    }

    @Test fun loadedOverloadMatchesExplicitVocabulary() {
        val location = TrainingLocation(
            "home", "home", "", TrainingLocationKind.CUSTOM, setOf(NativeEquipmentId.BODYWEIGHT)
        )
        assertEquals(
            V3NativeEnvironmentAdapter.fromLocation(location, V3LoadedEquipmentVocabulary.from(emptyLoad)),
            V3NativeEnvironmentAdapter.fromLocation(location, emptyLoad)
        )
    }

    @Test fun noAssistanceOrExternalLoadIsInferred() {
        val result = V3NativeEnvironmentAdapter.fromLocation(null, emptyLoad)
        assertEquals(V3Availability.UNKNOWN, result.assistanceKnowledge)
        assertEquals(V3Availability.UNKNOWN, result.externalLoadKnowledge)
        assertFalse(result.environment.assistanceAvailable)
        assertFalse(result.environment.externalLoadAvailable)
    }
}
