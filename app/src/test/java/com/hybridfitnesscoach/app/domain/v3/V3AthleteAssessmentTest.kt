package com.hybridfitnesscoach.app.domain.v3

import org.junit.Assert.*
import org.junit.Test

class V3AthleteAssessmentTest {
    private val strength = V3AssessmentTarget(V3AssessmentDimension.STRENGTH)
    private val low = SemanticValue.known(CapabilityLevel.LOW)
    private val exercise = ExerciseV3(
        id = CanonicalExerciseId("assessment-test"),
        equipment = EquipmentRequirement(EquipmentInformationState.KNOWN, EquipmentRequirementMode.NONE),
        resistance = ResistanceSpec(SemanticValue.known(ResistanceSource.BODYWEIGHT)),
        capabilities = CapabilityRequirements(low, SemanticValue.notApplicable(),
            SemanticValue.notApplicable(), SemanticValue.notApplicable()),
        category = ExerciseCategoryV3("push"),
        movementPatterns = SemanticValue.known(emptySet()),
        prescription = ExercisePrescription(listOf(PrescribedValue(PrescriptionDimension.REPS, 8.0, InputUnit.REPETITIONS))),
        legacyLevel = LegacyExerciseLevel("ADVANCED"), version = V3Version("3.7")
    )

    private fun declared(id: String, level: V3AssessmentLevel, previous: String? = null) =
        V3AssessmentRevision(
            id = id,
            target = strength,
            level = level,
            provenance = V3AssessmentProvenance.SELF_REPORTED,
            verification = V3AssessmentVerification.DECLARED,
            assessedAtEpochMs = 100L,
            recordedAtEpochMs = 100L,
            protocolId = "guided-self-report",
            protocolVersion = "1",
            rubricVersion = "G1-RUBRIC-0.1-DRAFT",
            observation = null,
            supersedesId = previous
        )

    @Test fun unknownIsNotConvertedToALowCapability() {
        val snapshot = V3AssessmentSnapshot.fromHistory(emptyList(), profileRevision = 7L)

        assertNull(snapshot.current(strength))
        assertEquals(7L, snapshot.profileRevision)
        assertEquals(0L, snapshot.assessmentRevision)
    }

    @Test fun newSelfReportSupersedesWithoutRemovingHistory() {
        val first = declared("assessment-1", V3AssessmentLevel.LOW)
        val second = declared("assessment-2", V3AssessmentLevel.MODERATE, previous = first.id)
        val snapshot = V3AssessmentSnapshot.fromHistory(listOf(first, second), profileRevision = 8L)

        assertEquals(listOf(first, second), snapshot.history)
        assertEquals(second, snapshot.current(strength))
        assertEquals(2L, snapshot.assessmentRevision)
        assertTrue(snapshot.hasContradictoryHistory(strength))
    }

    @Test fun skillTargetRequiresSpecificNodeId() {
        val error = runCatching { V3AssessmentTarget(V3AssessmentDimension.SKILL) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertEquals("pullup_strict", V3AssessmentTarget.skill("pullup_strict").targetId)
    }

    @Test fun selfReportDoesNotSatisfyDirectSkillEvidencePolicy() {
        val skill = V3AssessmentTarget.skill("pullup_strict")
        val row = declared("skill-1", V3AssessmentLevel.HIGH).copy(target = skill)

        assertEquals(V3AssessmentPolicyDecision.ASSESSMENT_REQUIRED,
            V3AssessmentPolicyRegistry.decision(skill, row))
    }

    @Test fun bridgePreservesSelfReportedProvenanceAndNeverReadsLegacyMovementScale() {
        val row = declared("assessment-1", V3AssessmentLevel.HIGH)
        val contract = V3AssessmentBridge.toCapabilityContract(
            V3AssessmentSnapshot.fromHistory(listOf(row), profileRevision = 11L)
        )

        assertEquals(SemanticState.KNOWN, contract.transversal.strength.semantic.state)
        assertEquals(CapabilityLevel.HIGH, contract.transversal.strength.semantic.value)
        assertEquals(V3AssessmentProvenance.SELF_REPORTED, contract.transversal.strength.provenance)
        assertEquals(SemanticState.UNKNOWN, contract.transversal.technical.semantic.state)
        assertNull(contract.movement.assessment(V3MovementDimension.HORIZONTAL_PUSH).semantic.value)
        assertEquals(1L, contract.assessmentRevision)
    }

    @Test fun eligibilityRequiresAssessmentAndPreservesSpecificReason() {
        val result = V3AssessmentEligibility.evaluate(
            exercise, V3AssessmentSnapshot.fromHistory(emptyList(), 0L), V3SelectionEnvironment(), V3SelectionIntent()
        )

        assertFalse(result.eligible)
        assertTrue(V3SelectionRejection.ASSESSMENT_REQUIRED in result.reasons)
        assertTrue(V3SelectionRejection.CAPABILITY_UNRESOLVED in result.reasons)
    }

    @Test fun guidedSelfReportCanSatisfyAnOrdinalCapabilityRequirement() {
        val row = declared("assessment-1", V3AssessmentLevel.LOW)
        val result = V3AssessmentEligibility.evaluate(
            exercise, V3AssessmentSnapshot.fromHistory(listOf(row), 0L), V3SelectionEnvironment(), V3SelectionIntent()
        )

        assertTrue(result.eligible)
        assertTrue(result.reasons.isEmpty())
    }

    @Test fun pdEligibilityRequiresAProtocolBackedEnvelope() {
        val pdExercise = exercise.copy(capabilities = CapabilityRequirements(
            SemanticValue.prescriptionDependent(), low, SemanticValue.notApplicable(), SemanticValue.notApplicable()),
            resistance = ResistanceSpec(SemanticValue.known(ResistanceSource.EXTERNAL_LOAD),
                loadUnit = SemanticValue.known(InputUnit.KILOGRAMS)))
        val technical = declared("technical-1", V3AssessmentLevel.LOW).copy(
            target = V3AssessmentTarget(V3AssessmentDimension.TECHNICAL))
        val snapshot = V3AssessmentSnapshot.fromHistory(listOf(technical), 0L)
        val work = V3WorkDimensions(reps = 5.0)
        val prescription = V3PrescriptionContext(pdExercise.id, ResistanceSource.EXTERNAL_LOAD, work,
            V3QuantifiedResistance(10.0, InputUnit.KILOGRAMS))
        fun envelope(point: V3CapacityEnvelopePoint) = V3PdStrengthContext(prescription,
            V3AthleteExerciseCapacityEnvelope(listOf(point)))
        val unverified = V3CapacityEnvelopePoint(pdExercise.id, ResistanceSource.EXTERNAL_LOAD, work,
            maximumFeasibleLoad = V3QuantifiedResistance(12.0, InputUnit.KILOGRAMS))
        val verified = unverified.copy(provenance = V3AssessmentProvenance.DIRECT_ASSESSMENT,
            protocolId = "capacity-envelope", protocolVersion = "1", observedAtEpochMs = 200L, evidenceId = "observation-1")

        val rejected = V3AssessmentEligibility.evaluate(pdExercise, snapshot, V3SelectionEnvironment(),
            V3SelectionIntent(pdStrengthContext = envelope(unverified)))
        val accepted = V3AssessmentEligibility.evaluate(pdExercise, snapshot, V3SelectionEnvironment(),
            V3SelectionIntent(pdStrengthContext = envelope(verified)))

        assertTrue(V3SelectionRejection.ASSESSMENT_REQUIRED in rejected.reasons)
        assertTrue(accepted.eligible)
    }

    @Test fun exactSkillRequiresVerifiedRelationAndDirectNodeAssessment() {
        val nodeId = SkillTargetId("pullup_strict")
        val skillExercise = exercise.copy(skillRelations = listOf(SkillRelation(
            SkillTargetType.NODE, nodeId, SkillRelationType.SKILL_ITSELF, EvidenceState.VERIFIED)))
        val strengthRow = declared("strength-1", V3AssessmentLevel.LOW)
        val target = V3AssessmentTarget.skill(nodeId.value)
        val skillRow = V3AssessmentRevision(
            "skill-1", target, V3AssessmentLevel.MODERATE, V3AssessmentProvenance.DIRECT_ASSESSMENT,
            V3AssessmentVerification.OBSERVED_VALID, 100L, 100L, "skill-protocol", "1",
            "G1-RUBRIC-0.1-DRAFT", V3AssessmentObservation("pullup_strict", "strict", "pull-up bar, unassisted", "controlled entry and exit"), null
        )
        val snapshot = V3AssessmentSnapshot.fromHistory(listOf(strengthRow, skillRow), 0L)

        val result = V3AssessmentEligibility.evaluate(skillExercise, snapshot, V3SelectionEnvironment(),
            V3SelectionIntent(skillNodeIds = setOf(nodeId)))

        assertTrue(result.eligible)
    }
}
