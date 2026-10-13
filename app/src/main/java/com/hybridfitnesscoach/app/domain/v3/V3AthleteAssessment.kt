package com.hybridfitnesscoach.app.domain.v3

import org.json.JSONObject

/** Five ordinal levels used only by the new, V3-native assessment store. */
enum class V3AssessmentLevel { VERY_LOW, LOW, MODERATE, HIGH, VERY_HIGH }

/** Movement levels intentionally have their own type; legacy 1..3 values never enter this scale. */
enum class V3MovementLevel { VERY_LOW, LOW, MODERATE, HIGH, VERY_HIGH }

enum class V3AssessmentDimension {
    STRENGTH, TECHNICAL, MOBILITY, BALANCE_COORDINATION,
    HORIZONTAL_PUSH, VERTICAL_PUSH, HORIZONTAL_PULL, VERTICAL_PULL,
    SQUAT, HINGE, CORE, ISOMETRIC, SKILL
}

data class V3AssessmentTarget(val dimension: V3AssessmentDimension, val targetId: String? = null) {
    init {
        if (dimension == V3AssessmentDimension.SKILL) require(!targetId.isNullOrBlank()) {
            "Skill assessments require a canonical node ID"
        }
        if (dimension != V3AssessmentDimension.SKILL) require(targetId == null) {
            "Only skill assessments accept a target ID"
        }
    }

    companion object {
        fun skill(nodeId: String) = V3AssessmentTarget(V3AssessmentDimension.SKILL, nodeId.trim())
    }
}

enum class V3AssessmentVerification { DECLARED, OBSERVED_VALID, DERIVED_VALID, INVALIDATED, UNKNOWN }

/** Structured evidence; the note is display-only and is never used for eligibility. */
data class V3AssessmentObservation(
    val canonicalExerciseId: String,
    val variant: String,
    val equipmentAndAssistance: String,
    val outcome: String,
    val note: String? = null
) {
    init {
        require(canonicalExerciseId.isNotBlank() && variant.isNotBlank())
        require(equipmentAndAssistance.isNotBlank() && outcome.isNotBlank())
    }
}

data class V3AssessmentDerivation(
    val ruleId: String,
    val ruleVersion: String,
    val inputs: Map<String, String>,
    val result: String
) {
    init {
        require(ruleId.isNotBlank() && ruleVersion.isNotBlank() && result.isNotBlank())
        require(inputs.isNotEmpty() && inputs.all { it.key.isNotBlank() && it.value.isNotBlank() })
    }
}

/** Immutable append-only row. Invalidations and unknown answers are rows, not destructive edits. */
data class V3AssessmentRevision(
    val id: String,
    val target: V3AssessmentTarget,
    val level: V3AssessmentLevel?,
    val provenance: V3AssessmentProvenance,
    val verification: V3AssessmentVerification,
    val assessedAtEpochMs: Long?,
    val recordedAtEpochMs: Long,
    val protocolId: String?,
    val protocolVersion: String?,
    val rubricVersion: String?,
    val observation: V3AssessmentObservation?,
    val supersedesId: String?,
    val invalidationReason: String? = null,
    val derivation: V3AssessmentDerivation? = null
) {
    init {
        require(id.isNotBlank())
        require(recordedAtEpochMs >= 0L)
        require(supersedesId != id)
        when {
            verification == V3AssessmentVerification.INVALIDATED -> {
                require(level == null && provenance == V3AssessmentProvenance.UNKNOWN)
                require(!invalidationReason.isNullOrBlank())
            }
            level == null -> {
                require(provenance == V3AssessmentProvenance.UNKNOWN)
                require(verification == V3AssessmentVerification.UNKNOWN)
                require(assessedAtEpochMs == null && rubricVersion == null && observation == null && derivation == null)
            }
            else -> {
                require(assessedAtEpochMs != null && assessedAtEpochMs >= 0L)
                require(!rubricVersion.isNullOrBlank())
                require(invalidationReason == null)
                when (provenance) {
                    V3AssessmentProvenance.SELF_REPORTED -> {
                        require(verification == V3AssessmentVerification.DECLARED)
                        require(!protocolId.isNullOrBlank() && !protocolVersion.isNullOrBlank())
                        require(observation == null && derivation == null)
                    }
                    V3AssessmentProvenance.DIRECT_ASSESSMENT -> {
                        require(verification == V3AssessmentVerification.OBSERVED_VALID)
                        require(!protocolId.isNullOrBlank() && !protocolVersion.isNullOrBlank())
                        require(observation != null && derivation == null)
                    }
                    V3AssessmentProvenance.VERIFIED_DERIVATION -> {
                        require(verification == V3AssessmentVerification.DERIVED_VALID)
                        require(!protocolId.isNullOrBlank() && !protocolVersion.isNullOrBlank())
                        require(derivation != null && protocolId == derivation.ruleId && protocolVersion == derivation.ruleVersion)
                    }
                    V3AssessmentProvenance.UNKNOWN -> error("A known level cannot have UNKNOWN provenance")
                }
            }
        }
    }

    internal fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("dimension", target.dimension.name)
        .put("targetId", target.targetId)
        .put("level", level?.name)
        .put("provenance", provenance.name)
        .put("verification", verification.name)
        .put("assessedAtEpochMs", assessedAtEpochMs)
        .put("recordedAtEpochMs", recordedAtEpochMs)
        .put("protocolId", protocolId)
        .put("protocolVersion", protocolVersion)
        .put("rubricVersion", rubricVersion)
        .put("observation", observation?.let {
            JSONObject().put("canonicalExerciseId", it.canonicalExerciseId).put("variant", it.variant)
                .put("equipmentAndAssistance", it.equipmentAndAssistance).put("outcome", it.outcome).put("note", it.note)
        })
        .put("supersedesId", supersedesId)
        .put("invalidationReason", invalidationReason)
        .put("derivation", derivation?.let { d ->
            JSONObject().put("ruleId", d.ruleId).put("ruleVersion", d.ruleVersion)
                .put("inputs", JSONObject(d.inputs)).put("result", d.result)
        })

    companion object {
        internal fun fromJson(o: JSONObject): V3AssessmentRevision {
            val observation = if (o.isNull("observation")) null else o.getJSONObject("observation").let {
                V3AssessmentObservation(it.getString("canonicalExerciseId"), it.getString("variant"),
                    it.getString("equipmentAndAssistance"), it.getString("outcome"), it.optString("note").takeIf(String::isNotBlank))
            }
            val derivation = if (o.isNull("derivation")) null else o.getJSONObject("derivation").let { d ->
                val inputs = d.getJSONObject("inputs")
                V3AssessmentDerivation(d.getString("ruleId"), d.getString("ruleVersion"),
                    inputs.keys().asSequence().associateWith { inputs.getString(it) }, d.getString("result"))
            }
            return V3AssessmentRevision(
                id = o.getString("id"),
                target = V3AssessmentTarget(V3AssessmentDimension.valueOf(o.getString("dimension")),
                    o.optString("targetId").takeIf(String::isNotBlank)),
                level = o.optString("level").takeIf(String::isNotBlank)?.let(V3AssessmentLevel::valueOf),
                provenance = V3AssessmentProvenance.valueOf(o.getString("provenance")),
                verification = V3AssessmentVerification.valueOf(o.getString("verification")),
                assessedAtEpochMs = o.takeIf { !it.isNull("assessedAtEpochMs") }?.getLong("assessedAtEpochMs"),
                recordedAtEpochMs = o.getLong("recordedAtEpochMs"),
                protocolId = o.optString("protocolId").takeIf(String::isNotBlank),
                protocolVersion = o.optString("protocolVersion").takeIf(String::isNotBlank),
                rubricVersion = o.optString("rubricVersion").takeIf(String::isNotBlank),
                observation = observation,
                supersedesId = o.optString("supersedesId").takeIf(String::isNotBlank),
                invalidationReason = o.optString("invalidationReason").takeIf(String::isNotBlank),
                derivation = derivation
            )
        }
    }
}

class V3AssessmentSnapshot private constructor(
    val history: List<V3AssessmentRevision>,
    val profileRevision: Long,
    val assessmentRevision: Long,
    private val conflictingTargets: Set<V3AssessmentTarget>
) {
    fun hasConflict(target: V3AssessmentTarget): Boolean = target in conflictingTargets

    fun hasContradictoryHistory(target: V3AssessmentTarget): Boolean {
        val levels = history.filter { it.target == target && it.level != null && it.verification != V3AssessmentVerification.INVALIDATED }
            .mapNotNull { it.level }.distinct()
        return levels.size > 1
    }
    fun current(target: V3AssessmentTarget): V3AssessmentRevision? {
        if (target in conflictingTargets) return null
        val superseded = history.mapNotNull { it.supersedesId }.toSet()
        return history.lastOrNull { it.target == target && it.id !in superseded }
    }

    fun currentRows(): List<V3AssessmentRevision> = history.mapNotNull { current(it.target) }.distinctBy { it.id }

    companion object {
        fun fromHistory(history: List<V3AssessmentRevision>, profileRevision: Long): V3AssessmentSnapshot {
            require(profileRevision >= 0L)
            require(history.map { it.id }.distinct().size == history.size) { "Duplicate assessment revision ID" }
            val byId = history.associateBy { it.id }
            history.forEach { row ->
                row.supersedesId?.let { prior ->
                    val old = requireNotNull(byId[prior]) { "Superseded revision is missing" }
                    require(old.target == row.target) { "A revision can supersede only the same target" }
                }
            }
            val referenced = history.mapNotNull { it.supersedesId }.toSet()
            val conflicts = history.filter { it.id !in referenced }.groupBy { it.target }
                .filterValues { it.size > 1 }.keys
            return V3AssessmentSnapshot(history.toList(), profileRevision, history.size.toLong(), conflicts)
        }
    }
}

enum class V3AssessmentPolicyDecision { ALLOWED, ASSESSMENT_REQUIRED, POLICY_UNRESOLVED }

/** Versioned app policy. Self-report can satisfy ordinal capability requirements, never a skill gate. */
object V3AssessmentPolicyRegistry {
    const val VERSION = "G1-V3-EVIDENCE-POLICY-1"

    fun decision(
        target: V3AssessmentTarget,
        revision: V3AssessmentRevision?,
        policyVersion: String = VERSION
    ): V3AssessmentPolicyDecision {
        if (policyVersion != VERSION) return V3AssessmentPolicyDecision.POLICY_UNRESOLVED
        if (revision == null || revision.target != target || revision.level == null ||
            revision.verification == V3AssessmentVerification.INVALIDATED) {
            return V3AssessmentPolicyDecision.ASSESSMENT_REQUIRED
        }
        if (target.dimension == V3AssessmentDimension.SKILL) {
            return if (revision.provenance == V3AssessmentProvenance.DIRECT_ASSESSMENT &&
                revision.verification == V3AssessmentVerification.OBSERVED_VALID && revision.observation != null)
                V3AssessmentPolicyDecision.ALLOWED else V3AssessmentPolicyDecision.ASSESSMENT_REQUIRED
        }
        return when (revision.provenance) {
            V3AssessmentProvenance.SELF_REPORTED -> if (revision.verification == V3AssessmentVerification.DECLARED &&
                revision.rubricVersion?.isNotBlank() == true) V3AssessmentPolicyDecision.ALLOWED
                else V3AssessmentPolicyDecision.POLICY_UNRESOLVED
            V3AssessmentProvenance.DIRECT_ASSESSMENT -> if (revision.verification == V3AssessmentVerification.OBSERVED_VALID &&
                revision.protocolId != null && revision.protocolVersion != null && revision.observation != null)
                V3AssessmentPolicyDecision.ALLOWED else V3AssessmentPolicyDecision.POLICY_UNRESOLVED
            V3AssessmentProvenance.VERIFIED_DERIVATION -> if (revision.verification == V3AssessmentVerification.DERIVED_VALID &&
                revision.protocolId != null && revision.protocolVersion != null && revision.observation != null)
                V3AssessmentPolicyDecision.ALLOWED else V3AssessmentPolicyDecision.POLICY_UNRESOLVED
            V3AssessmentProvenance.UNKNOWN -> V3AssessmentPolicyDecision.ASSESSMENT_REQUIRED
        }
    }

    fun pdEnvelopeDecision(point: V3CapacityEnvelopePoint, policyVersion: String = VERSION): V3AssessmentPolicyDecision {
        if (policyVersion != VERSION) return V3AssessmentPolicyDecision.POLICY_UNRESOLVED
        return when (point.provenance) {
            V3AssessmentProvenance.DIRECT_ASSESSMENT, V3AssessmentProvenance.VERIFIED_DERIVATION ->
                if (!point.protocolId.isNullOrBlank() && !point.protocolVersion.isNullOrBlank() &&
                    point.observedAtEpochMs != null && !point.evidenceId.isNullOrBlank()) V3AssessmentPolicyDecision.ALLOWED
                else V3AssessmentPolicyDecision.POLICY_UNRESOLVED
            V3AssessmentProvenance.SELF_REPORTED, V3AssessmentProvenance.UNKNOWN -> V3AssessmentPolicyDecision.ASSESSMENT_REQUIRED
        }
    }
}

/** Materialized, transient bridge. It never reads AthleteProfile's legacy 1..3 fields. */
object V3AssessmentBridge {
    fun toCapabilityContract(snapshot: V3AssessmentSnapshot): V3NativeAthleteCapabilityContract {
        fun transversal(dimension: V3AssessmentDimension): V3AssessedValue<CapabilityLevel> {
            val row = snapshot.current(V3AssessmentTarget(dimension)) ?: return V3AssessedValue.unknown()
            val level = row.level ?: return V3AssessedValue.unknown()
            return V3AssessedValue(SemanticValue.known(CapabilityLevel.valueOf(level.name)), row.provenance)
        }
        val movement = buildMap {
            movementDimensions.forEach { (assessmentDimension, movementDimension) ->
                val row = snapshot.current(V3AssessmentTarget(assessmentDimension)) ?: return@forEach
                val level = row.level ?: return@forEach
                put(movementDimension, V3AssessedValue(SemanticValue.known(V3MovementLevel.valueOf(level.name)), row.provenance))
            }
        }
        return V3NativeAthleteCapabilityContract(
            transversal = V3TransversalAssessments(
                transversal(V3AssessmentDimension.STRENGTH), transversal(V3AssessmentDimension.TECHNICAL),
                transversal(V3AssessmentDimension.MOBILITY), transversal(V3AssessmentDimension.BALANCE_COORDINATION)
            ),
            movementV3 = V3MovementV3Assessments(movement),
            assessmentRevision = snapshot.assessmentRevision
        )
    }

    private val movementDimensions = mapOf(
        V3AssessmentDimension.HORIZONTAL_PUSH to V3MovementDimension.HORIZONTAL_PUSH,
        V3AssessmentDimension.VERTICAL_PUSH to V3MovementDimension.VERTICAL_PUSH,
        V3AssessmentDimension.HORIZONTAL_PULL to V3MovementDimension.HORIZONTAL_PULL,
        V3AssessmentDimension.VERTICAL_PULL to V3MovementDimension.VERTICAL_PULL,
        V3AssessmentDimension.SQUAT to V3MovementDimension.SQUAT,
        V3AssessmentDimension.HINGE to V3MovementDimension.HINGE,
        V3AssessmentDimension.CORE to V3MovementDimension.CORE,
        V3AssessmentDimension.ISOMETRIC to V3MovementDimension.ISOMETRIC
    )
}
