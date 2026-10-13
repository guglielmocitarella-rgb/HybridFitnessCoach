package com.hybridfitnesscoach.app.domain.v3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class V3NativeAthleteCapabilityContractTest {
    @Test fun emptyProfileIsUnknown() {
        val profile = V3NativeAthleteCapabilityContract()
        assertEquals(SemanticState.UNKNOWN, profile.movement.assessment(V3MovementDimension.SQUAT).semantic.state)
        val capabilities = profile.toV3AthleteCapabilities()
        listOf(capabilities.strength, capabilities.technical, capabilities.mobility, capabilities.balanceCoordination)
            .forEach { assertEquals(SemanticState.UNKNOWN, it.state); assertNull(it.value) }
    }

    @Test fun movementValuesStayNativeAndNeverConvert() {
        val movement = V3MovementAssessments(mapOf(
            V3MovementDimension.HORIZONTAL_PUSH to V3AssessedValue.direct(3),
            V3MovementDimension.VERTICAL_PULL to V3AssessedValue.verified(1)
        ))
        val profile = V3NativeAthleteCapabilityContract(movement = movement)
        assertEquals(3, profile.movement.assessment(V3MovementDimension.HORIZONTAL_PUSH).semantic.value)
        assertEquals(1, profile.movement.assessment(V3MovementDimension.VERTICAL_PULL).semantic.value)
        assertEquals(V3AssessmentProvenance.VERIFIED_DERIVATION,
            profile.movement.assessment(V3MovementDimension.VERTICAL_PULL).provenance)
        assertEquals(SemanticState.UNKNOWN, profile.toV3AthleteCapabilities().strength.state)
    }

    @Test fun knownTransversalValuesAndProvenanceSurviveConversion() {
        val strength = V3AssessedValue.direct(CapabilityLevel.HIGH)
        val technique = V3AssessedValue.verified(CapabilityLevel.MODERATE)
        val transversal = V3TransversalAssessments(
            strength = strength, technical = technique,
            mobility = V3AssessedValue.direct(CapabilityLevel.LOW),
            balanceCoordination = V3AssessedValue.verified(CapabilityLevel.VERY_HIGH)
        )
        val result = transversal.toV3AthleteCapabilities()
        assertEquals(CapabilityLevel.HIGH, result.strength.value)
        assertEquals(CapabilityLevel.MODERATE, result.technical.value)
        assertEquals(CapabilityLevel.LOW, result.mobility.value)
        assertEquals(CapabilityLevel.VERY_HIGH, result.balanceCoordination.value)
        assertEquals(V3AssessmentProvenance.DIRECT_ASSESSMENT, transversal.strength.provenance)
        assertEquals(V3AssessmentProvenance.VERIFIED_DERIVATION, transversal.technical.provenance)
    }

    @Test fun partialTransversalDoesNotFillMissingValues() {
        val result = V3TransversalAssessments(
            mobility = V3AssessedValue.direct(CapabilityLevel.VERY_LOW)
        ).toV3AthleteCapabilities()
        assertEquals(CapabilityLevel.VERY_LOW, result.mobility.value)
        assertEquals(SemanticState.UNKNOWN, result.strength.state)
        assertEquals(SemanticState.UNKNOWN, result.technical.state)
        assertEquals(SemanticState.UNKNOWN, result.balanceCoordination.state)
    }

    @Test fun invalidMovementScaleIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            V3MovementAssessments(mapOf(V3MovementDimension.CORE to V3AssessedValue.direct(4)))
        }
    }

    @Test fun provenanceCannotBeInventedForUnknown() {
        assertThrows(IllegalArgumentException::class.java) {
            V3AssessedValue(SemanticValue.unknown<CapabilityLevel>(), V3AssessmentProvenance.DIRECT_ASSESSMENT)
        }
        assertThrows(IllegalArgumentException::class.java) {
            V3AssessedValue(SemanticValue.known(CapabilityLevel.HIGH))
        }
    }
}
