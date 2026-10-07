package com.hybridfitnesscoach.app.domain.v3

/** Native V3 Step 8A prescription-dependent strength context. */
data class V3WorkDimensions(
    val reps: Double? = null,
    val durationSeconds: Double? = null,
    val distanceMeters: Double? = null,
    val intervals: Double? = null,
    val variation: String? = null,
    val tempo: String? = null,
    val rangeOfMotion: String? = null,
    val laterality: String? = null
) {
    init {
        require(listOfNotNull(reps, durationSeconds, distanceMeters, intervals).all { it >= 0.0 })
    }
    fun completeFor(other: V3WorkDimensions): Boolean =
        reps == other.reps && durationSeconds == other.durationSeconds &&
        distanceMeters == other.distanceMeters && intervals == other.intervals &&
        variation == other.variation && tempo == other.tempo &&
        rangeOfMotion == other.rangeOfMotion && laterality == other.laterality
}

data class V3QuantifiedResistance(val value: Double, val unit: InputUnit) {
    init { require(value >= 0.0) }
}

data class V3CapacityEnvelopePoint(
    val exerciseId: CanonicalExerciseId,
    val resistanceSource: ResistanceSource,
    val work: V3WorkDimensions,
    val maximumFeasibleLoad: V3QuantifiedResistance? = null,
    val minimumRequiredAssistance: V3QuantifiedResistance? = null
) {
    init {
        when (resistanceSource) {
            ResistanceSource.EXTERNAL_LOAD -> require(maximumFeasibleLoad != null && minimumRequiredAssistance == null)
            ResistanceSource.ASSISTED -> require(minimumRequiredAssistance != null && maximumFeasibleLoad == null)
            else -> require(maximumFeasibleLoad == null && minimumRequiredAssistance == null)
        }
    }
}

data class V3AthleteExerciseCapacityEnvelope(val points: List<V3CapacityEnvelopePoint>) {
    init { require(points.distinct().size == points.size) }
}

data class V3PrescriptionContext(
    val exerciseId: CanonicalExerciseId,
    val resistanceSource: ResistanceSource,
    val work: V3WorkDimensions,
    val resistance: V3QuantifiedResistance?
)

data class V3PdStrengthContext(
    val prescription: V3PrescriptionContext?,
    val athleteCapacity: V3AthleteExerciseCapacityEnvelope?
)

enum class V3ContextualEligibilityState {
    ELIGIBLE,
    INELIGIBLE,
    EXERCISE_REQUIREMENT_UNRESOLVED,
    ATHLETE_CAPABILITY_UNKNOWN,
    INVALID_PRESCRIPTION_CONTEXT,
    UNSUPPORTED_CONTEXT,
    TERMINAL_CANONICAL_STATE
}

enum class V3PdReasonCode {
    PD_STRENGTH_CONTEXT_RESOLVED,
    PD_STRENGTH_CONTEXT_INSUFFICIENT,
    PD_STRENGTH_CONTEXT_INVALID,
    ATHLETE_CAPABILITY_UNKNOWN,
    CAPACITY_ENVELOPE_MISMATCH,
    TERMINAL_CANONICAL_STATE,
    UNSUPPORTED_CONTEXT
}

data class V3ContextualEligibility(
    val state: V3ContextualEligibilityState,
    val reason: V3PdReasonCode
)

object V3PdStrengthResolver {
    fun evaluate(exercise: ExerciseV3, context: V3PdStrengthContext?): V3ContextualEligibility {
        val requirement = exercise.capabilities.strengthRequirement
        if (requirement.state == SemanticState.EVIDENCE_UNRESOLVED)
            return V3ContextualEligibility(V3ContextualEligibilityState.TERMINAL_CANONICAL_STATE, V3PdReasonCode.TERMINAL_CANONICAL_STATE)
        if (requirement.state != SemanticState.PRESCRIPTION_DEPENDENT)
            return V3ContextualEligibility(V3ContextualEligibilityState.UNSUPPORTED_CONTEXT, V3PdReasonCode.UNSUPPORTED_CONTEXT)

        val p = context?.prescription
            ?: return unresolved()
        if (p.exerciseId != exercise.id || p.resistanceSource != exercise.resistance.source.value)
            return invalid()
        if (p.resistanceSource !in setOf(ResistanceSource.EXTERNAL_LOAD, ResistanceSource.ASSISTED))
            return V3ContextualEligibility(V3ContextualEligibilityState.UNSUPPORTED_CONTEXT, V3PdReasonCode.UNSUPPORTED_CONTEXT)
        val prescribed = p.resistance ?: return unresolved()
        val expectedUnit = when (p.resistanceSource) {
            ResistanceSource.EXTERNAL_LOAD -> exercise.resistance.loadUnit.value
            ResistanceSource.ASSISTED -> exercise.resistance.assistanceUnit.value
            else -> null
        }
        if (expectedUnit == null || prescribed.unit != expectedUnit) return invalid()

        val envelope = context.athleteCapacity
            ?: return V3ContextualEligibility(V3ContextualEligibilityState.ATHLETE_CAPABILITY_UNKNOWN, V3PdReasonCode.ATHLETE_CAPABILITY_UNKNOWN)
        val candidates = envelope.points.filter {
            it.exerciseId == exercise.id && it.resistanceSource == p.resistanceSource && it.work.completeFor(p.work)
        }
        if (candidates.isEmpty())
            return V3ContextualEligibility(V3ContextualEligibilityState.EXERCISE_REQUIREMENT_UNRESOLVED, V3PdReasonCode.CAPACITY_ENVELOPE_MISMATCH)
        val matched = candidates.singleOrNull()
            ?: return invalid()

        val capacity = when (p.resistanceSource) {
            ResistanceSource.EXTERNAL_LOAD -> matched.maximumFeasibleLoad
            ResistanceSource.ASSISTED -> matched.minimumRequiredAssistance
            else -> null
        } ?: return invalid()
        if (capacity.unit != prescribed.unit) return invalid()

        val eligible = when (p.resistanceSource) {
            ResistanceSource.EXTERNAL_LOAD -> prescribed.value <= capacity.value
            ResistanceSource.ASSISTED -> prescribed.value >= capacity.value
            else -> false
        }
        return V3ContextualEligibility(
            if (eligible) V3ContextualEligibilityState.ELIGIBLE else V3ContextualEligibilityState.INELIGIBLE,
            V3PdReasonCode.PD_STRENGTH_CONTEXT_RESOLVED
        )
    }

    private fun unresolved() = V3ContextualEligibility(
        V3ContextualEligibilityState.EXERCISE_REQUIREMENT_UNRESOLVED,
        V3PdReasonCode.PD_STRENGTH_CONTEXT_INSUFFICIENT
    )
    private fun invalid() = V3ContextualEligibility(
        V3ContextualEligibilityState.INVALID_PRESCRIPTION_CONTEXT,
        V3PdReasonCode.PD_STRENGTH_CONTEXT_INVALID
    )
}
