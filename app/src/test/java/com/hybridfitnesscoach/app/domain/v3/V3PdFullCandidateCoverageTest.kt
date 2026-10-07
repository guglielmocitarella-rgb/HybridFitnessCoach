package com.hybridfitnesscoach.app.domain.v3

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class V3PdFullCandidateCoverageTest {
    private fun candidateText():String {
        val candidates=listOf(File("../exercise-db-canonical/exercise-db-v3-final-candidate.json"),File("exercise-db-canonical/exercise-db-v3-final-candidate.json"))
        return candidates.firstOrNull{it.isFile}?.readText() ?: error("frozen candidate not found")
    }
    private fun workFor(x:ExerciseV3)=V3WorkDimensions(
        reps=if(x.prescription!!.prescribed.any{it.dimension==PrescriptionDimension.REPS}) 8.0 else null,
        durationSeconds=if(x.prescription.prescribed.any{it.dimension==PrescriptionDimension.DURATION}) 30.0 else null,
        distanceMeters=if(x.prescription.prescribed.any{it.dimension==PrescriptionDimension.DISTANCE}) 100.0 else null,
        intervals=if(x.prescription.prescribed.any{it.dimension==PrescriptionDimension.INTERVALS}) 3.0 else null
    )
    private fun context(x:ExerciseV3):V3PdStrengthContext {
        val source=x.resistance.source.value!!
        val unit=when(source){ResistanceSource.EXTERNAL_LOAD->x.resistance.loadUnit.value;ResistanceSource.ASSISTED->x.resistance.assistanceUnit.value;else->null}
            ?: error("PD "+x.id.value+" missing typed resistance unit")
        val work=workFor(x); val q=V3QuantifiedResistance(10.0,unit)
        val point=V3CapacityEnvelopePoint(x.id,source,work,
            maximumFeasibleLoad=if(source==ResistanceSource.EXTERNAL_LOAD)q else null,
            minimumRequiredAssistance=if(source==ResistanceSource.ASSISTED)q else null)
        return V3PdStrengthContext(V3PrescriptionContext(x.id,source,work,q),V3AthleteExerciseCapacityEnvelope(listOf(point)))
    }
    @Test fun all441PdStatesTraverseNativeBoundaryWithoutMutation() {
        val loaded=V3AssetLoader.loadAuthoritative(candidateText()).records
        val pd=loaded.filter{it.capabilities.strengthRequirement.state==SemanticState.PRESCRIPTION_DEPENDENT}
        assertEquals(441,pd.size); assertEquals(436,pd.count{it.resistance.source.value==ResistanceSource.EXTERNAL_LOAD}); assertEquals(5,pd.count{it.resistance.source.value==ResistanceSource.ASSISTED})
        assertEquals(0,loaded.count{it.capabilities.technicalDifficulty.state==SemanticState.PRESCRIPTION_DEPENDENT}); assertEquals(0,loaded.count{it.capabilities.mobilityRequirement.state==SemanticState.PRESCRIPTION_DEPENDENT}); assertEquals(0,loaded.count{it.capabilities.balanceCoordinationRequirement.state==SemanticState.PRESCRIPTION_DEPENDENT})
        pd.forEach { x ->
            val before=x.capabilities.strengthRequirement
            assertEquals(x.id.value,V3ContextualEligibilityState.ELIGIBLE,V3PdStrengthResolver.evaluate(x,context(x)).state)
            assertEquals(x.id.value,V3ContextualEligibilityState.EXERCISE_REQUIREMENT_UNRESOLVED,V3PdStrengthResolver.evaluate(x,null).state)
            val insufficient=context(x).copy(athleteCapacity=V3AthleteExerciseCapacityEnvelope(emptyList()))
            assertEquals(x.id.value,V3ContextualEligibilityState.EXERCISE_REQUIREMENT_UNRESOLVED,V3PdStrengthResolver.evaluate(x,insufficient).state)
            assertEquals(x.id.value,before,x.capabilities.strengthRequirement)
        }
    }
    @Test fun terminalStrengthStatesNeverTraversePdResolution() {
        val loaded=V3AssetLoader.loadAuthoritative(candidateText()).records
        loaded.filter{it.capabilities.strengthRequirement.state==SemanticState.EVIDENCE_UNRESOLVED}.forEach {
            assertEquals(V3ContextualEligibilityState.TERMINAL_CANONICAL_STATE,V3PdStrengthResolver.evaluate(it,null).state)
        }
    }
}
