package com.hybridfitnesscoach.app.domain.v3

data class V3AthleteCapabilities(
    val strength: SemanticValue<CapabilityLevel>,
    val technical: SemanticValue<CapabilityLevel>,
    val mobility: SemanticValue<CapabilityLevel>,
    val balanceCoordination: SemanticValue<CapabilityLevel>
)

data class V3SelectionEnvironment(
    val equipment: Set<EquipmentId> = emptySet(),
    val assistanceAvailable: Boolean = false,
    val externalLoadAvailable: Boolean = false
)

data class V3SelectionIntent(
    val categories: Set<ExerciseCategoryV3> = emptySet(),
    val skillFamilyIds: Set<SkillTargetId> = emptySet(),
    val skillNodeIds: Set<SkillTargetId> = emptySet(),
    val requirePrescriptionFeasibility: Boolean = false
)

enum class V3SelectionRejection {
    IDENTITY_UNRESOLVED, CATEGORY_UNRESOLVED, CATEGORY_MISMATCH,
    EQUIPMENT_UNRESOLVED, EQUIPMENT_UNAVAILABLE,
    CAPABILITY_UNRESOLVED, CAPABILITY_INSUFFICIENT,
    SKILL_RELATION_UNRESOLVED, SKILL_MISMATCH,
    PRESCRIPTION_UNRESOLVED
}

data class V3Eligibility(val eligible:Boolean,val reasons:Set<V3SelectionRejection>)
enum class V3PoolStatus { VALID_POOL, LEGITIMATE_ZERO_POOL, INVALID_ZERO_POOL }
data class V3SelectionPool(val eligible:List<ExerciseV3>,val status:V3PoolStatus,val rejected:Map<CanonicalExerciseId,Set<V3SelectionRejection>>)

object V3SelectionEngine {
    fun evaluate(exercise:ExerciseV3, athlete:V3AthleteCapabilities, environment:V3SelectionEnvironment, intent:V3SelectionIntent):V3Eligibility {
        val reasons=linkedSetOf<V3SelectionRejection>()
        if(exercise.identityState!=SemanticState.KNOWN) reasons+=V3SelectionRejection.IDENTITY_UNRESOLVED
        if(exercise.categoryState!=SemanticState.KNOWN) reasons+=V3SelectionRejection.CATEGORY_UNRESOLVED
        else if(intent.categories.isNotEmpty() && exercise.category !in intent.categories) reasons+=V3SelectionRejection.CATEGORY_MISMATCH
        reasons+=equipmentReasons(exercise.equipment,environment)
        reasons+=capabilityReasons(exercise.capabilities,athlete)
        reasons+=skillReasons(exercise.skillRelations,intent)
        if(intent.requirePrescriptionFeasibility && !prescriptionFeasible(exercise.prescription)) reasons+=V3SelectionRejection.PRESCRIPTION_UNRESOLVED
        return V3Eligibility(reasons.isEmpty(),reasons)
    }

    fun pool(exercises:List<ExerciseV3>, athlete:V3AthleteCapabilities, environment:V3SelectionEnvironment, intent:V3SelectionIntent, legitimateZeroPool:Boolean=false):V3SelectionPool {
        val decisions=exercises.associateWith{evaluate(it,athlete,environment,intent)}
        val eligible=decisions.filterValues{it.eligible}.keys.sortedBy{it.id.value}
        val rejected=decisions.filterValues{!it.eligible}.mapKeys{it.key.id}.mapValues{it.value.reasons}
        val status=when { eligible.isNotEmpty()->V3PoolStatus.VALID_POOL; legitimateZeroPool->V3PoolStatus.LEGITIMATE_ZERO_POOL; else->V3PoolStatus.INVALID_ZERO_POOL }
        return V3SelectionPool(eligible,status,rejected)
    }

    fun deterministicOrder(exercises:Collection<ExerciseV3>,seed:Long):List<ExerciseV3> =
        exercises.sortedWith(compareBy<ExerciseV3>({stableKey(it.id.value,seed)},{it.id.value}))

    fun validSubstitutes(original:ExerciseV3,candidates:Collection<ExerciseV3>,athlete:V3AthleteCapabilities,environment:V3SelectionEnvironment,intent:V3SelectionIntent):List<ExerciseV3> =
        deterministicOrder(candidates.filter{it.id!=original.id && it.category==original.category && evaluate(it,athlete,environment,intent).eligible},0L)

    private fun equipmentReasons(r:EquipmentRequirement,e:V3SelectionEnvironment):Set<V3SelectionRejection> {
        if(r.informationState!=EquipmentInformationState.KNOWN || r.mode==EquipmentRequirementMode.UNKNOWN || r.sourceState!=SemanticState.KNOWN)
            return setOf(V3SelectionRejection.EQUIPMENT_UNRESOLVED)
        val have=e.equipment
        val ok=when(r.mode) {
            EquipmentRequirementMode.NONE,EquipmentRequirementMode.OPTIONAL -> true
            EquipmentRequirementMode.REQUIRED -> r.equipment.isNotEmpty() && have.containsAll(r.equipment)
            EquipmentRequirementMode.ONE_OF -> (r.equipment+r.alternatives).any{it in have}
            EquipmentRequirementMode.ASSISTED_BY -> e.assistanceAvailable && r.assistance.any{it in have}
            EquipmentRequirementMode.EXTERNAL_LOAD -> e.externalLoadAvailable && r.externalLoadSupport && (r.equipment.isEmpty() || have.containsAll(r.equipment))
            EquipmentRequirementMode.UNKNOWN -> false
        }
        return if(ok) emptySet() else setOf(V3SelectionRejection.EQUIPMENT_UNAVAILABLE)
    }

    private fun capabilityReasons(r:CapabilityRequirements,a:V3AthleteCapabilities):Set<V3SelectionRejection> {
        if(r.overallState !in setOf(SemanticState.KNOWN,SemanticState.NOT_APPLICABLE)) return setOf(V3SelectionRejection.CAPABILITY_UNRESOLVED)
        val pairs=listOf(r.strengthRequirement to a.strength,r.technicalDifficulty to a.technical,r.mobilityRequirement to a.mobility,r.balanceCoordinationRequirement to a.balanceCoordination)
        var unresolved=false; var insufficient=false
        for((req,have) in pairs) {
            if(req.state==SemanticState.NOT_APPLICABLE) continue
            if(req.state!=SemanticState.KNOWN || have.state!=SemanticState.KNOWN){unresolved=true;continue}
            if(have.value!!.ordinal<req.value!!.ordinal) insufficient=true
        }
        return buildSet { if(unresolved)add(V3SelectionRejection.CAPABILITY_UNRESOLVED); if(insufficient)add(V3SelectionRejection.CAPABILITY_INSUFFICIENT) }
    }

    private fun skillReasons(relations:List<SkillRelation>,intent:V3SelectionIntent):Set<V3SelectionRejection> {
        if(intent.skillFamilyIds.isEmpty() && intent.skillNodeIds.isEmpty()) return emptySet()
        val usable=relations.filter{it.evidenceState!=EvidenceState.DOCUMENTED_UNRESOLVED}
        if(usable.size!=relations.size && usable.isEmpty()) return setOf(V3SelectionRejection.SKILL_RELATION_UNRESOLVED)
        val match=usable.any{r->when(r.targetType){SkillTargetType.FAMILY->r.targetId in intent.skillFamilyIds;SkillTargetType.NODE->r.targetId in intent.skillNodeIds}}
        return if(match) emptySet() else setOf(V3SelectionRejection.SKILL_MISMATCH)
    }

    private fun prescriptionFeasible(p:ExercisePrescription?):Boolean =
        p!=null && p.sourceState==SemanticState.KNOWN && (p.openEnded || p.prescribed.isNotEmpty())

    private fun stableKey(id:String,seed:Long):Long {
        var h=seed xor -3750763034362895579L
        for(c in id){ h=(h xor c.code.toLong())*1099511628211L }
        return h
    }
}
