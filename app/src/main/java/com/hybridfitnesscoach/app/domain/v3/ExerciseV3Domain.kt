package com.hybridfitnesscoach.app.domain.v3

@JvmInline value class CanonicalExerciseId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class SkillTargetId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class LegacyExerciseLevel(val value: String) { init { require(value.isNotBlank()) } }

enum class SemanticState { KNOWN, UNKNOWN, NOT_APPLICABLE, VARIABLE, PRESCRIPTION_DEPENDENT, VARIANT_DEPENDENT }
data class SemanticValue<T>(val state: SemanticState, val value: T? = null) {
    init { require((state == SemanticState.KNOWN) == (value != null)) { "KNOWN requires value; non-KNOWN must not carry one" } }
    companion object {
        fun <T> known(value:T)=SemanticValue(SemanticState.KNOWN,value)
        fun <T> unknown()=SemanticValue<T>(SemanticState.UNKNOWN)
        fun <T> notApplicable()=SemanticValue<T>(SemanticState.NOT_APPLICABLE)
        fun <T> variable()=SemanticValue<T>(SemanticState.VARIABLE)
        fun <T> prescriptionDependent()=SemanticValue<T>(SemanticState.PRESCRIPTION_DEPENDENT)
        fun <T> variantDependent()=SemanticValue<T>(SemanticState.VARIANT_DEPENDENT)
    }
}

enum class EquipmentInformationState { KNOWN, UNKNOWN }
enum class EquipmentRequirementMode { NONE, REQUIRED, OPTIONAL, ONE_OF, ASSISTED_BY, EXTERNAL_LOAD }
@JvmInline value class EquipmentId(val value:String) { init { require(value.isNotBlank()) } }
data class EquipmentRequirement(
    val informationState: EquipmentInformationState,
    val mode: EquipmentRequirementMode,
    val equipment: Set<EquipmentId> = emptySet()
) {
    init {
        if (informationState == EquipmentInformationState.UNKNOWN) require(equipment.isEmpty())
        if (informationState == EquipmentInformationState.KNOWN && mode == EquipmentRequirementMode.NONE) require(equipment.isEmpty())
        if (informationState == EquipmentInformationState.KNOWN && mode != EquipmentRequirementMode.NONE) require(equipment.isNotEmpty())
    }
}
object V3Equipment { val SUSPENSION_TRAINER = EquipmentId("suspension_trainer") }

enum class ResistanceSource { BODYWEIGHT, EXTERNAL_LOAD, ASSISTED, NONE, BAND, MACHINE, MANUAL_ASSISTANCE, BAND_ASSISTANCE, MACHINE_ASSISTANCE }
enum class InputUnit { REPETITIONS, SECONDS, METERS, INTERVALS, KILOGRAMS, POUNDS, NEWTONS, BAND_LEVEL, MACHINE_LEVEL, PERCENT_BODYWEIGHT, ARBITRARY_LEVEL }
data class ResistanceSpec(val source: SemanticValue<ResistanceSource>, val loadUnit: SemanticValue<InputUnit> = SemanticValue.notApplicable(), val assistanceUnit: SemanticValue<InputUnit> = SemanticValue.notApplicable())

enum class CapabilityLevel { VERY_LOW, LOW, MODERATE, HIGH, VERY_HIGH }
data class CapabilityRequirements(
    val strengthRequirement: SemanticValue<CapabilityLevel>,
    val technicalDifficulty: SemanticValue<CapabilityLevel>,
    val mobilityRequirement: SemanticValue<CapabilityLevel>,
    val balanceCoordinationRequirement: SemanticValue<CapabilityLevel>
)

@JvmInline value class ExerciseCategoryV3(val value:String) { init { require(value.isNotBlank()) } }
@JvmInline value class ExerciseGroupV3(val value:String) { init { require(value.isNotBlank()) } }
@JvmInline value class MovementPatternV3(val value:String) { init { require(value.isNotBlank()) } }

enum class PrescriptionDimension { REPS, DURATION, DISTANCE, INTERVALS, LOAD, ASSISTANCE, ATTEMPT, QUALITY, OPEN_ENDED }
data class PrescribedValue(val dimension: PrescriptionDimension, val value: Double? = null, val unit: InputUnit? = null) {
    init {
        if (dimension == PrescriptionDimension.OPEN_ENDED || dimension == PrescriptionDimension.ATTEMPT || dimension == PrescriptionDimension.QUALITY) {
            require(value == null || value >= 0.0)
        } else require(value != null && value >= 0.0)
    }
}
data class ExercisePrescription(val prescribed: List<PrescribedValue>) {
    init { require(prescribed.map { it.dimension }.distinct().size == prescribed.size) }
}

enum class SkillRelationType { SKILL_ITSELF, DIRECT_PROGRESSION, DIRECT_REGRESSION, STRENGTH_BUILDER }
enum class SkillTargetType { FAMILY, NODE }
enum class EvidenceState { VERIFIED, DOCUMENTED_UNRESOLVED }
data class SkillRelation(val targetType: SkillTargetType, val targetId: SkillTargetId, val relationType: SkillRelationType, val evidenceState: EvidenceState, val diagnosticProvenanceRef: String? = null)

data class V3Version(val schemaVersion:String, val assetVersion:String? = null, val migrationVersion:String? = null) { init { require(schemaVersion.isNotBlank()) } }

data class ExerciseV3(
    val id: CanonicalExerciseId,
    val equipment: EquipmentRequirement,
    val resistance: ResistanceSpec,
    val capabilities: CapabilityRequirements,
    val category: ExerciseCategoryV3,
    val group: ExerciseGroupV3? = null,
    val movementPatterns: SemanticValue<Set<MovementPatternV3>>,
    val prescription: ExercisePrescription? = null,
    val skillRelations: List<SkillRelation> = emptyList(),
    val legacyLevel: LegacyExerciseLevel? = null,
    val version: V3Version
)
