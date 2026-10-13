package com.hybridfitnesscoach.app.domain.v3

import com.hybridfitnesscoach.app.AthleteProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class V3NativeAthleteProfileAdapterTest {
    private val dimensions = V3MovementDimension.values().toList()

    @Test fun allMovementAssessmentsRetainTheirExactValuesAndProvenance() {
        val source = AthleteProfile(
            assessedPushCapacity = 1, assessedVerticalPushCapacity = 2,
            assessedHorizontalPullCapacity = 3, assessedPullCapacity = 1,
            assessedSquatCapacity = 2, assessedHingeCapacity = 3,
            assessedCoreCapacity = 1, assessedIsometricCapacity = 2,
            assessedSkillCapacity = 3, assessedTrainingExperience = 3
        )
        val contract = V3NativeAthleteProfileAdapter.fromProfile(source)
        val expected = listOf(1, 2, 3, 1, 2, 3, 1, 2, 3)
        assertEquals(dimensions.toSet(), contract.movement.values.keys)
        dimensions.zip(expected).forEach { (dimension, value) ->
            val assessment = contract.movement.assessment(dimension)
            assertEquals(value, assessment.semantic.value)
            assertEquals(SemanticState.KNOWN, assessment.semantic.state)
            assertEquals(V3AssessmentProvenance.DIRECT_ASSESSMENT, assessment.provenance)
        }
        assertEquals(SemanticState.UNKNOWN, contract.toV3AthleteCapabilities().strength.state)
    }

    @Test fun partialProfileLeavesUnassessedDimensionsUnknown() {
        val contract = V3NativeAthleteProfileAdapter.fromProfile(
            AthleteProfile(assessedPushCapacity = 2, assessedSkillCapacity = 1)
        )
        assertEquals(setOf(V3MovementDimension.HORIZONTAL_PUSH, V3MovementDimension.SKILL),
            contract.movement.values.keys)
        assertEquals(2, contract.movement.assessment(V3MovementDimension.HORIZONTAL_PUSH).semantic.value)
        assertEquals(1, contract.movement.assessment(V3MovementDimension.SKILL).semantic.value)
        assertEquals(SemanticState.UNKNOWN,
            contract.movement.assessment(V3MovementDimension.VERTICAL_PULL).semantic.state)
    }

    @Test fun unassessedProfileAndAllTransversalDimensionsAreUnknown() {
        val contract = V3NativeAthleteProfileAdapter.fromProfile(AthleteProfile())
        assertEquals(emptyMap<V3MovementDimension, V3AssessedValue<Int>>(), contract.movement.values)
        dimensions.forEach { assertEquals(SemanticState.UNKNOWN, contract.movement.assessment(it).semantic.state) }
        val capabilities = contract.toV3AthleteCapabilities()
        listOf(capabilities.strength, capabilities.technical, capabilities.mobility, capabilities.balanceCoordination)
            .forEach { assertEquals(SemanticState.UNKNOWN, it.state); assertEquals(null, it.value) }
    }

    @Test fun explicitlyKnownTransversalAssessmentsReachEngineInputWithoutConversion() {
        val transversal = V3TransversalAssessments(
            strength = V3AssessedValue.direct(CapabilityLevel.HIGH),
            technical = V3AssessedValue.verified(CapabilityLevel.MODERATE),
            mobility = V3AssessedValue.direct(CapabilityLevel.LOW),
            balanceCoordination = V3AssessedValue.verified(CapabilityLevel.VERY_HIGH)
        )
        val contract = V3NativeAthleteProfileAdapter.fromProfile(
            AthleteProfile(assessedPushCapacity = 1), transversal
        )
        assertEquals(transversal, contract.transversal)
        val engineInput: V3AthleteCapabilities = contract.toV3AthleteCapabilities()
        assertEquals(transversal.toV3AthleteCapabilities(), engineInput)
        assertEquals(CapabilityLevel.HIGH, engineInput.strength.value)
        assertEquals(CapabilityLevel.MODERATE, engineInput.technical.value)
        assertEquals(CapabilityLevel.LOW, engineInput.mobility.value)
        assertEquals(CapabilityLevel.VERY_HIGH, engineInput.balanceCoordination.value)
        assertEquals(1, contract.movement.assessment(V3MovementDimension.HORIZONTAL_PUSH).semantic.value)
    }

    @Test fun repeatedConversionIsDeterministic() {
        val profile = AthleteProfile(assessedPushCapacity = 3, assessedHingeCapacity = 1)
        val first = V3NativeAthleteProfileAdapter.fromProfile(profile)
        val second = V3NativeAthleteProfileAdapter.fromProfile(profile)
        assertEquals(first, second)
        assertEquals(first.toV3AthleteCapabilities(), second.toV3AthleteCapabilities())
    }

    @Test fun invalidNativeAssessmentIsNotSilentlyReinterpreted() {
        assertThrows(IllegalArgumentException::class.java) {
            V3NativeAthleteProfileAdapter.fromProfile(AthleteProfile(assessedSquatCapacity = 4))
        }
    }
}
