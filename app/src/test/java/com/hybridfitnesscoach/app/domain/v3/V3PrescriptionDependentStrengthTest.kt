package com.hybridfitnesscoach.app.domain.v3

import org.junit.Assert.*
import org.junit.Test

class V3PrescriptionDependentStrengthTest {
    private val low=SemanticValue.known(CapabilityLevel.LOW)
    private val athlete=V3AthleteCapabilities(low,low,low,low)
    private fun ex(source:ResistanceSource=ResistanceSource.EXTERNAL_LOAD, state:SemanticState=SemanticState.PRESCRIPTION_DEPENDENT):ExerciseV3 {
        val unit=if(source==ResistanceSource.EXTERNAL_LOAD) InputUnit.KILOGRAMS else InputUnit.BAND_LEVEL
        return ExerciseV3(
            CanonicalExerciseId("pd"), EquipmentRequirement(EquipmentInformationState.KNOWN,EquipmentRequirementMode.NONE),
            ResistanceSpec(SemanticValue.known(source),
                loadUnit=if(source==ResistanceSource.EXTERNAL_LOAD) SemanticValue.known(unit) else SemanticValue.notApplicable(),
                assistanceUnit=if(source==ResistanceSource.ASSISTED) SemanticValue.known(unit) else SemanticValue.notApplicable()),
            CapabilityRequirements(SemanticValue(state),low,low,low), ExerciseCategoryV3("PUSH"),
            movementPatterns=SemanticValue.known(setOf(MovementPatternV3("HORIZONTAL_PUSH"))),
            prescription=ExercisePrescription(listOf(PrescribedValue(if(source==ResistanceSource.EXTERNAL_LOAD) PrescriptionDimension.LOAD else PrescriptionDimension.ASSISTANCE))),
            version=V3Version("3.7")
        )
    }
    private fun ctx(source:ResistanceSource, prescribed:Double, capacity:Double, unit:InputUnit=if(source==ResistanceSource.EXTERNAL_LOAD) InputUnit.KILOGRAMS else InputUnit.BAND_LEVEL)=V3PdStrengthContext(
        V3PrescriptionContext(CanonicalExerciseId("pd"),source,V3WorkDimensions(),V3QuantifiedResistance(prescribed,unit)),
        V3AthleteExerciseCapacityEnvelope(listOf(V3CapacityEnvelopePoint(CanonicalExerciseId("pd"),source,V3WorkDimensions(),
            maximumFeasibleLoad=if(source==ResistanceSource.EXTERNAL_LOAD)V3QuantifiedResistance(capacity,unit) else null,
            minimumRequiredAssistance=if(source==ResistanceSource.ASSISTED)V3QuantifiedResistance(capacity,unit) else null)))
    )

    @Test fun externalLoadEqualityIsEligible(){assertEquals(V3ContextualEligibilityState.ELIGIBLE,V3PdStrengthResolver.evaluate(ex(),ctx(ResistanceSource.EXTERNAL_LOAD,20.0,20.0)).state)}
    @Test fun externalLoadAboveCapacityIsIneligible(){assertEquals(V3ContextualEligibilityState.INELIGIBLE,V3PdStrengthResolver.evaluate(ex(),ctx(ResistanceSource.EXTERNAL_LOAD,21.0,20.0)).state)}
    @Test fun assistedEqualityIsEligible(){assertEquals(V3ContextualEligibilityState.ELIGIBLE,V3PdStrengthResolver.evaluate(ex(ResistanceSource.ASSISTED),ctx(ResistanceSource.ASSISTED,2.0,2.0)).state)}
    @Test fun assistedBelowMinimumIsIneligible(){assertEquals(V3ContextualEligibilityState.INELIGIBLE,V3PdStrengthResolver.evaluate(ex(ResistanceSource.ASSISTED),ctx(ResistanceSource.ASSISTED,1.0,2.0)).state)}
    @Test fun noContextIsUnresolved(){assertEquals(V3ContextualEligibilityState.EXERCISE_REQUIREMENT_UNRESOLVED,V3PdStrengthResolver.evaluate(ex(),null).state)}
    @Test fun missingAthleteCapacityIsDistinct(){val c=V3PdStrengthContext(V3PrescriptionContext(CanonicalExerciseId("pd"),ResistanceSource.EXTERNAL_LOAD,V3WorkDimensions(),V3QuantifiedResistance(20.0,InputUnit.KILOGRAMS)),null);assertEquals(V3ContextualEligibilityState.ATHLETE_CAPABILITY_UNKNOWN,V3PdStrengthResolver.evaluate(ex(),c).state)}
    @Test fun incompatibleUnitIsInvalid(){assertEquals(V3ContextualEligibilityState.INVALID_PRESCRIPTION_CONTEXT,V3PdStrengthResolver.evaluate(ex(),ctx(ResistanceSource.EXTERNAL_LOAD,20.0,20.0,InputUnit.POUNDS)).state)}
    @Test fun mismatchedEnvelopeIsUnresolved(){val c=ctx(ResistanceSource.EXTERNAL_LOAD,20.0,20.0).copy(athleteCapacity=V3AthleteExerciseCapacityEnvelope(emptyList()));assertEquals(V3ContextualEligibilityState.EXERCISE_REQUIREMENT_UNRESOLVED,V3PdStrengthResolver.evaluate(ex(),c).state)}
    @Test fun terminalNeverResolves(){assertEquals(V3ContextualEligibilityState.TERMINAL_CANONICAL_STATE,V3PdStrengthResolver.evaluate(ex(state=SemanticState.EVIDENCE_UNRESOLVED),ctx(ResistanceSource.EXTERNAL_LOAD,1.0,100.0)).state)}
    @Test fun canonicalPdRemainsPdAndRepeatIsDeterministic(){val x=ex();val c=ctx(ResistanceSource.EXTERNAL_LOAD,20.0,20.0);val a=V3PdStrengthResolver.evaluate(x,c);val b=V3PdStrengthResolver.evaluate(x,c);assertEquals(a,b);assertEquals(SemanticState.PRESCRIPTION_DEPENDENT,x.capabilities.strengthRequirement.state)}
    @Test fun changedContextDoesNotLeak(){val x=ex();assertEquals(V3ContextualEligibilityState.ELIGIBLE,V3PdStrengthResolver.evaluate(x,ctx(ResistanceSource.EXTERNAL_LOAD,20.0,20.0)).state);assertEquals(V3ContextualEligibilityState.INELIGIBLE,V3PdStrengthResolver.evaluate(x,ctx(ResistanceSource.EXTERNAL_LOAD,30.0,20.0)).state)}
    @Test fun selectionExposesPdReasons(){val x=ex();val no=V3SelectionEngine.evaluate(x,athlete,V3SelectionEnvironment(),V3SelectionIntent());assertTrue(V3SelectionRejection.PD_STRENGTH_CONTEXT_INSUFFICIENT in no.reasons);val yes=V3SelectionEngine.evaluate(x,athlete,V3SelectionEnvironment(),V3SelectionIntent(pdStrengthContext=ctx(ResistanceSource.EXTERNAL_LOAD,20.0,20.0)));assertTrue(yes.eligible)}
    @Test fun knownStrengthStillUsesOrdinalComparison(){val x=ex().copy(capabilities=CapabilityRequirements(SemanticValue.known(CapabilityLevel.HIGH),low,low,low));assertTrue(V3SelectionRejection.CAPABILITY_INSUFFICIENT in V3SelectionEngine.evaluate(x,athlete,V3SelectionEnvironment(),V3SelectionIntent()).reasons)}
    @Test fun externalLoadBelowCapacityIsEligible(){assertEquals(V3ContextualEligibilityState.ELIGIBLE,V3PdStrengthResolver.evaluate(ex(),ctx(ResistanceSource.EXTERNAL_LOAD,10.0,20.0)).state)}
    @Test fun assistedNoContextIsUnresolved(){assertEquals(V3ContextualEligibilityState.EXERCISE_REQUIREMENT_UNRESOLVED,V3PdStrengthResolver.evaluate(ex(ResistanceSource.ASSISTED),null).state)}
    @Test fun assistedInsufficientEnvelopeIsUnresolved(){val c=ctx(ResistanceSource.ASSISTED,2.0,2.0).copy(athleteCapacity=V3AthleteExerciseCapacityEnvelope(emptyList()));assertEquals(V3ContextualEligibilityState.EXERCISE_REQUIREMENT_UNRESOLVED,V3PdStrengthResolver.evaluate(ex(ResistanceSource.ASSISTED),c).state)}
    @Test fun assistedInvalidUnitIsInvalid(){assertEquals(V3ContextualEligibilityState.INVALID_PRESCRIPTION_CONTEXT,V3PdStrengthResolver.evaluate(ex(ResistanceSource.ASSISTED),ctx(ResistanceSource.ASSISTED,2.0,2.0,InputUnit.KILOGRAMS)).state)}
    @Test fun zeroOccurrenceFuturePdDimensionsFailClosed(){
        val base=ex()
        val variants=listOf(
            base.copy(capabilities=base.capabilities.copy(technicalDifficulty=SemanticValue.prescriptionDependent())),
            base.copy(capabilities=base.capabilities.copy(mobilityRequirement=SemanticValue.prescriptionDependent())),
            base.copy(capabilities=base.capabilities.copy(balanceCoordinationRequirement=SemanticValue.prescriptionDependent()))
        )
        variants.forEach{assertTrue(V3SelectionRejection.CAPABILITY_UNRESOLVED in V3SelectionEngine.evaluate(it,athlete,V3SelectionEnvironment(),V3SelectionIntent(pdStrengthContext=ctx(ResistanceSource.EXTERNAL_LOAD,10.0,20.0))).reasons)}
    }
    @Test fun malformedCapacityIsRejected(){try{V3QuantifiedResistance(-1.0,InputUnit.KILOGRAMS);fail("expected invalid")}catch(_:IllegalArgumentException){}}
}
