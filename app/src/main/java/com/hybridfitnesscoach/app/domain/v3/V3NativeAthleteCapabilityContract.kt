package com.hybridfitnesscoach.app.domain.v3

/**
 * Transient input contract for Step 10A.2. Movement assessments are kept on their
 * original 1..3 scale; they never imply a cross-cutting V3 capability level.
 * This is not a persisted athlete profile.
 */
enum class V3AssessmentProvenance { SELF_REPORTED, DIRECT_ASSESSMENT, VERIFIED_DERIVATION, UNKNOWN }

data class V3AssessedValue<T>(
    val semantic: SemanticValue<T>,
    val provenance: V3AssessmentProvenance? = null
) {
    init {
        require((semantic.state == SemanticState.KNOWN) == (provenance != null)) {
            "Known assessments require provenance; unknown assessments cannot claim provenance"
        }
    }

    companion object {
        fun <T> unknown(): V3AssessedValue<T> = V3AssessedValue(SemanticValue.unknown())
        fun <T> direct(value: T): V3AssessedValue<T> =
            V3AssessedValue(SemanticValue.known(value), V3AssessmentProvenance.DIRECT_ASSESSMENT)
        fun <T> verified(value: T): V3AssessedValue<T> =
            V3AssessedValue(SemanticValue.known(value), V3AssessmentProvenance.VERIFIED_DERIVATION)
    }
}

enum class V3MovementDimension {
    HORIZONTAL_PUSH, VERTICAL_PUSH, HORIZONTAL_PULL, VERTICAL_PULL,
    SQUAT, HINGE, CORE, ISOMETRIC, SKILL
}

data class V3MovementAssessments(
    val values: Map<V3MovementDimension, V3AssessedValue<Int>> = emptyMap()
) {
    init {
        require(values.values.all { it.semantic.value == null || it.semantic.value in 1..3 }) {
            "Movement assessments must retain the native 1..3 scale"
        }
    }

    fun assessment(dimension: V3MovementDimension): V3AssessedValue<Int> =
        values[dimension] ?: V3AssessedValue.unknown()
}

/** New five-level movement capability scale; separate from legacy movement Int values. */
data class V3MovementV3Assessments(
    val values: Map<V3MovementDimension, V3AssessedValue<V3MovementLevel>> = emptyMap()
)

/** The four independent cross-cutting V3 assessments, without inferred defaults. */
data class V3TransversalAssessments(
    val strength: V3AssessedValue<CapabilityLevel> = V3AssessedValue.unknown(),
    val technical: V3AssessedValue<CapabilityLevel> = V3AssessedValue.unknown(),
    val mobility: V3AssessedValue<CapabilityLevel> = V3AssessedValue.unknown(),
    val balanceCoordination: V3AssessedValue<CapabilityLevel> = V3AssessedValue.unknown()
) {
    fun toV3AthleteCapabilities(): V3AthleteCapabilities = V3AthleteCapabilities(
        strength = strength.semantic,
        technical = technical.semantic,
        mobility = mobility.semantic,
        balanceCoordination = balanceCoordination.semantic
    )
}

/** A non-persistent snapshot that preserves provenance and both distinct assessment axes. */
data class V3NativeAthleteCapabilityContract(
    val movement: V3MovementAssessments = V3MovementAssessments(),
    val transversal: V3TransversalAssessments = V3TransversalAssessments(),
    val movementV3: V3MovementV3Assessments = V3MovementV3Assessments(),
    val assessmentRevision: Long = 0L
) {
    init { require(assessmentRevision >= 0L) }
    fun toV3AthleteCapabilities(): V3AthleteCapabilities =
        transversal.toV3AthleteCapabilities()
}
