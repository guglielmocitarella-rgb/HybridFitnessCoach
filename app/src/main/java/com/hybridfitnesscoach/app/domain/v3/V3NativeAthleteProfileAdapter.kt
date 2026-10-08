package com.hybridfitnesscoach.app.domain.v3

import com.hybridfitnesscoach.app.AthleteProfile

/**
 * Transient bridge from the actual athlete assessments to the V3 input contract.
 * No 1..3 movement score is converted to a transversal five-level capability.
 */
object V3NativeAthleteProfileAdapter {
    fun fromProfile(
        profile: AthleteProfile,
        transversal: V3TransversalAssessments = V3TransversalAssessments()
    ): V3NativeAthleteCapabilityContract {
        val assessed = linkedMapOf<V3MovementDimension, V3AssessedValue<Int>>()
        fun put(dimension: V3MovementDimension, value: Int?) {
            if (value != null) {
                require(value in 1..3) { "Invalid native assessment for $dimension: $value" }
                assessed[dimension] = V3AssessedValue.direct(value)
            }
        }
        put(V3MovementDimension.HORIZONTAL_PUSH, profile.assessedPushCapacity)
        put(V3MovementDimension.VERTICAL_PUSH, profile.assessedVerticalPushCapacity)
        put(V3MovementDimension.HORIZONTAL_PULL, profile.assessedHorizontalPullCapacity)
        put(V3MovementDimension.VERTICAL_PULL, profile.assessedPullCapacity)
        put(V3MovementDimension.SQUAT, profile.assessedSquatCapacity)
        put(V3MovementDimension.HINGE, profile.assessedHingeCapacity)
        put(V3MovementDimension.CORE, profile.assessedCoreCapacity)
        put(V3MovementDimension.ISOMETRIC, profile.assessedIsometricCapacity)
        put(V3MovementDimension.SKILL, profile.assessedSkillCapacity)
        return V3NativeAthleteCapabilityContract(
            movement = V3MovementAssessments(assessed.toMap()),
            transversal = transversal
        )
    }
}
