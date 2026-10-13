package com.hybridfitnesscoach.app.domain.v3

import org.junit.Assert.*
import org.junit.Test

class RuntimeDomainModelV310ATest {
 @Test fun identityIsStableAndValidated(){ assertEquals(CanonicalExerciseId("ex:1"),CanonicalExerciseId("ex:1")); assertFails { CanonicalExerciseId(" ") } }
 @Test fun equipmentModesAndSuspensionTrainerAreLossless(){
   val s=V3Equipment.SUSPENSION_TRAINER; assertEquals("suspension_trainer",s.value)
   // Only construct combinations admitted by the domain invariants. UNKNOWN is
   // intentionally not a valid mode for KNOWN equipment information.
   EquipmentRequirementMode.values().forEach { mode ->
     if (mode == EquipmentRequirementMode.UNKNOWN) {
       assertFails { EquipmentRequirement(EquipmentInformationState.KNOWN, mode) }
     } else {
       val requirement = when (mode) {
         EquipmentRequirementMode.NONE -> EquipmentRequirement(EquipmentInformationState.KNOWN, mode)
         EquipmentRequirementMode.REQUIRED, EquipmentRequirementMode.ONE_OF ->
           EquipmentRequirement(EquipmentInformationState.KNOWN, mode, equipment = setOf(s))
         EquipmentRequirementMode.OPTIONAL ->
           EquipmentRequirement(EquipmentInformationState.KNOWN, mode, optional = setOf(s))
         EquipmentRequirementMode.ASSISTED_BY ->
           EquipmentRequirement(EquipmentInformationState.KNOWN, mode, assistance = setOf(s))
         EquipmentRequirementMode.EXTERNAL_LOAD ->
           EquipmentRequirement(EquipmentInformationState.KNOWN, mode, externalLoadSupport = true)
         EquipmentRequirementMode.UNKNOWN -> error("handled above")
       }
       assertEquals(mode, requirement.mode)
     }
   }
   assertEquals(EquipmentInformationState.UNKNOWN,EquipmentRequirement(EquipmentInformationState.UNKNOWN,EquipmentRequirementMode.REQUIRED).informationState)
 }
 @Test fun resistanceDoesNotAssumeKg(){
   val r=ResistanceSpec(SemanticValue.known(ResistanceSource.BAND_ASSISTANCE),assistanceUnit=SemanticValue.known(InputUnit.BAND_LEVEL))
   assertEquals(InputUnit.BAND_LEVEL,r.assistanceUnit.value)
 }
 @Test fun fourCapabilitiesRemainIndependentAndSpecialStatesRemainDistinct(){
   val c=CapabilityRequirements(SemanticValue.known(CapabilityLevel.HIGH),SemanticValue.variable(),SemanticValue.notApplicable(),SemanticValue.variantDependent())
   assertEquals(SemanticState.KNOWN,c.strengthRequirement.state); assertEquals(SemanticState.VARIABLE,c.technicalDifficulty.state)
   assertEquals(SemanticState.NOT_APPLICABLE,c.mobilityRequirement.state); assertEquals(SemanticState.VARIANT_DEPENDENT,c.balanceCoordinationRequirement.state)
 }
 @Test fun categoryGroupMovementAreDifferentTypes(){
   assertEquals("push",ExerciseCategoryV3("push").value); assertEquals("chest",ExerciseGroupV3("chest").value); assertEquals("horizontal_push",MovementPatternV3("horizontal_push").value)
 }
 @Test fun prescriptionSupportsCombinations(){
   val p=ExercisePrescription(listOf(PrescribedValue(PrescriptionDimension.REPS,8.0,InputUnit.REPETITIONS),PrescribedValue(PrescriptionDimension.LOAD,20.0,InputUnit.KILOGRAMS),PrescribedValue(PrescriptionDimension.ASSISTANCE,2.0,InputUnit.BAND_LEVEL)))
   assertEquals(3,p.prescribed.size)
 }
 @Test fun skillRelationsAreTyped(){
   val r=SkillRelation(SkillTargetType.NODE,SkillTargetId("front_lever:tuck"),SkillRelationType.DIRECT_PROGRESSION,EvidenceState.VERIFIED)
   assertEquals(SkillTargetType.NODE,r.targetType); assertEquals(SkillRelationType.DIRECT_PROGRESSION,r.relationType)
 }
 @Test fun unresolvedStatesNeverCollapseToNullMeaning(){
   val states=listOf(SemanticValue.unknown<String>(),SemanticValue.notApplicable(),SemanticValue.variable(),SemanticValue.prescriptionDependent(),SemanticValue.variantDependent()).map{it.state}.toSet()
   assertEquals(5,states.size)
 }
 private fun assertFails(block:()->Unit){ try{block(); fail("expected failure")}catch(_:IllegalArgumentException){} }
}
