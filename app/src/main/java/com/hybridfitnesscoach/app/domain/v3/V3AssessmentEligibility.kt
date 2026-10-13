package com.hybridfitnesscoach.app.domain.v3

/** Assessment-aware hard gate; scoring receives only the eligible result from this path. */
object V3AssessmentEligibility {
    fun evaluate(
        exercise: ExerciseV3,
        snapshot: V3AssessmentSnapshot,
        environment: V3SelectionEnvironment,
        intent: V3SelectionIntent,
        policyVersion: String = V3AssessmentPolicyRegistry.VERSION
    ): V3Eligibility {
        val contract = V3AssessmentBridge.toCapabilityContract(snapshot)
        val base = V3SelectionEngine.evaluate(exercise, contract.toV3AthleteCapabilities(), environment, intent)
        val reasons = base.reasons.toMutableSet()
        val requirements = exercise.capabilities
        if (requirements.strengthRequirement.state == SemanticState.PRESCRIPTION_DEPENDENT && base.eligible) {
            val context = intent.pdStrengthContext
            val prescription = context?.prescription
            val points = if (context?.athleteCapacity != null && prescription != null) {
                context.athleteCapacity.points.filter { point ->
                    point.exerciseId == exercise.id && point.resistanceSource == prescription.resistanceSource &&
                        point.work.completeFor(prescription.work)
                }
            } else emptyList()
            points.singleOrNull()?.let { point ->
                when (V3AssessmentPolicyRegistry.pdEnvelopeDecision(point, policyVersion)) {
                    V3AssessmentPolicyDecision.ALLOWED -> Unit
                    V3AssessmentPolicyDecision.ASSESSMENT_REQUIRED -> reasons += V3SelectionRejection.ASSESSMENT_REQUIRED
                    V3AssessmentPolicyDecision.POLICY_UNRESOLVED -> reasons += V3SelectionRejection.ASSESSMENT_POLICY_UNRESOLVED
                }
            }
        }
        listOf(
            requirements.strengthRequirement to V3AssessmentDimension.STRENGTH,
            requirements.technicalDifficulty to V3AssessmentDimension.TECHNICAL,
            requirements.mobilityRequirement to V3AssessmentDimension.MOBILITY,
            requirements.balanceCoordinationRequirement to V3AssessmentDimension.BALANCE_COORDINATION
        ).forEach { (requirement, dimension) ->
            // PD strength is evaluated exclusively through its typed prescription/capacity envelope.
            if (dimension == V3AssessmentDimension.STRENGTH &&
                requirement.state == SemanticState.PRESCRIPTION_DEPENDENT) return@forEach
            if (requirement.state != SemanticState.KNOWN) return@forEach
            val target = V3AssessmentTarget(dimension)
            when (V3AssessmentPolicyRegistry.decision(target, snapshot.current(target), policyVersion)) {
                V3AssessmentPolicyDecision.ALLOWED -> Unit
                V3AssessmentPolicyDecision.ASSESSMENT_REQUIRED -> reasons += V3SelectionRejection.ASSESSMENT_REQUIRED
                V3AssessmentPolicyDecision.POLICY_UNRESOLVED -> reasons += V3SelectionRejection.ASSESSMENT_POLICY_UNRESOLVED
            }
        }

        intent.movementRequirements.forEach { (dimension, required) ->
            val assessmentDimension = dimension.assessmentDimensionOrNull()
            if (assessmentDimension == null) {
                reasons += V3SelectionRejection.MOVEMENT_REQUIREMENT_UNRESOLVED
                return@forEach
            }
            val target = V3AssessmentTarget(assessmentDimension)
            val row = snapshot.current(target)
            when (V3AssessmentPolicyRegistry.decision(target, row, policyVersion)) {
                V3AssessmentPolicyDecision.ASSESSMENT_REQUIRED -> reasons += V3SelectionRejection.ASSESSMENT_REQUIRED
                V3AssessmentPolicyDecision.POLICY_UNRESOLVED -> reasons += V3SelectionRejection.ASSESSMENT_POLICY_UNRESOLVED
                V3AssessmentPolicyDecision.ALLOWED -> {
                    val level = row?.level
                    if (level == null) reasons += V3SelectionRejection.MOVEMENT_REQUIREMENT_UNRESOLVED
                    else if (V3MovementLevel.valueOf(level.name).ordinal < required.ordinal)
                        reasons += V3SelectionRejection.CAPABILITY_INSUFFICIENT
                }
            }
        }

        if (intent.skillFamilyIds.isNotEmpty() || intent.skillNodeIds.isNotEmpty()) {
            val verifiedRelations = exercise.skillRelations.filter { relation ->
                relation.evidenceState == EvidenceState.VERIFIED && when (relation.targetType) {
                    SkillTargetType.FAMILY -> relation.targetId in intent.skillFamilyIds
                    SkillTargetType.NODE -> relation.targetId in intent.skillNodeIds
                }
            }
            if (verifiedRelations.isEmpty()) {
                reasons += V3SelectionRejection.SKILL_RELATION_UNRESOLVED
            } else {
                val assessedNode = verifiedRelations.asSequence()
                    .filter { it.targetType == SkillTargetType.NODE }
                    .map { V3AssessmentTarget.skill(it.targetId.value) }
                    .any { target -> V3AssessmentPolicyRegistry.decision(target, snapshot.current(target), policyVersion) == V3AssessmentPolicyDecision.ALLOWED }
                if (!assessedNode) reasons += V3SelectionRejection.ASSESSMENT_REQUIRED
            }
        }
        return V3Eligibility(reasons.isEmpty(), reasons)
    }

    private fun V3MovementDimension.assessmentDimensionOrNull(): V3AssessmentDimension? = when (this) {
        V3MovementDimension.HORIZONTAL_PUSH -> V3AssessmentDimension.HORIZONTAL_PUSH
        V3MovementDimension.VERTICAL_PUSH -> V3AssessmentDimension.VERTICAL_PUSH
        V3MovementDimension.HORIZONTAL_PULL -> V3AssessmentDimension.HORIZONTAL_PULL
        V3MovementDimension.VERTICAL_PULL -> V3AssessmentDimension.VERTICAL_PULL
        V3MovementDimension.SQUAT -> V3AssessmentDimension.SQUAT
        V3MovementDimension.HINGE -> V3AssessmentDimension.HINGE
        V3MovementDimension.CORE -> V3AssessmentDimension.CORE
        V3MovementDimension.ISOMETRIC -> V3AssessmentDimension.ISOMETRIC
        V3MovementDimension.SKILL -> null
    }
}
