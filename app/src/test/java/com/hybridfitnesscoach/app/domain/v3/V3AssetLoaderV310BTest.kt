package com.hybridfitnesscoach.app.domain.v3

import org.junit.Assert.*
import org.junit.Test

class V3AssetLoaderV310BTest {
    private fun fixture(
        id:String="ex-1", equipmentMode:String="REQUIRED", equipmentState:String="KNOWN",
        equipment:String="\"suspension_trainer\"", capability:String="UNKNOWN",
        targetType:String="NODE", relationType:String="DIRECT_PROGRESSION", schema:String="3.7", provenance:String="\"fixture\""
    )="""[
      {"schemaVersion":"$schema","canonicalExerciseId":"$id","canonicalName":"Test","aliases":[],"identityState":"KNOWN",
       "categoryGroup":{"state":"KNOWN","category":"PULL","group":null,"secondaryGroups":[],"groupState":"SCHEMA_UNRESOLVED"},
       "equipmentRequirement":{"state":"$equipmentState","mode":"$equipmentMode","required":[$equipment],"alternatives":[],"optional":[],"assistance":[],"externalLoadSupport":false},
       "resistance":{"state":"KNOWN","source":"BODYWEIGHT","loadInput":{"state":"NOT_APPLICABLE","unitSemantics":null},"assistanceInput":{"state":"NOT_APPLICABLE","unitSemantics":null},"quantification":"NOT_APPLICABLE"},
       "capability":{"state":"EVIDENCE_UNRESOLVED","strengthRequirement":"$capability","technicalDifficulty":"UNKNOWN","mobilityRequirement":"UNKNOWN","balanceCoordinationRequirement":"UNKNOWN","legacyLevel":null,"legacyLevelStatus":"UNKNOWN","migrationRule":"NO_BLIND_SCALAR_MAPPING"},
       "skillRelations":[{"canonicalExerciseId":"$id","confidence":"HIGH","evidence":"fixture","provenance":$provenance,"reason":"fixture","relationType":"$relationType","source":"fixture","sourceRecordId":"1","targetId":"node1","targetType":"$targetType"}],
       "prescription":{"state":"KNOWN","primaryMeasure":"REPS","dimensions":[{"type":"REPS","state":"KNOWN","source":"legacy.measure"}],"openEnded":false},
       "movementPatterns":{"state":"KNOWN","values":["VERTICAL_PULL"]},"engineMetadata":{},"legacy":{},"unresolved":[],"provenance":{}}
    ]"""

    @Test fun structuredSkillProvenanceIsPreservedAsCanonicalJson() {
        val json = """{"record":"r5-1","source":{"shard":"bucket-a"}}"""
        val relation = V3AssetLoader.load(fixture(provenance=json),1).records.single().skillRelations.single()
        assertNull(relation.diagnosticProvenanceRef)
        assertEquals("""{"record":"r5-1","source":{"shard":"bucket-a"}}""",
            relation.diagnosticProvenancePayload)
    }

    @Test fun positiveMappingPreservesV3Semantics() {
        val result=V3AssetLoader.load(fixture(),1)
        assertEquals(1,result.audit.mappedRecords)
        assertEquals(0,result.audit.silentSemanticLoss)
        assertEquals("suspension_trainer",result.records.single().equipment.equipment.single().value)
        assertEquals(SemanticState.EVIDENCE_UNRESOLVED,result.records.single().capabilities.overallState)
        assertNull(result.records.single().legacyLevel)
    }

    @Test fun allEquipmentModesAreRepresentable() {
        EquipmentRequirementMode.values().forEach { mode ->
            when(mode) {
                EquipmentRequirementMode.NONE -> EquipmentRequirement(EquipmentInformationState.KNOWN,mode)
                EquipmentRequirementMode.REQUIRED -> EquipmentRequirement(EquipmentInformationState.KNOWN,mode,setOf(V3Equipment.SUSPENSION_TRAINER))
                EquipmentRequirementMode.OPTIONAL -> EquipmentRequirement(EquipmentInformationState.KNOWN,mode,setOf(V3Equipment.SUSPENSION_TRAINER))
                EquipmentRequirementMode.ONE_OF -> EquipmentRequirement(EquipmentInformationState.KNOWN,mode,alternatives=setOf(V3Equipment.SUSPENSION_TRAINER))
                EquipmentRequirementMode.ASSISTED_BY -> EquipmentRequirement(EquipmentInformationState.KNOWN,mode,assistance=setOf(V3Equipment.SUSPENSION_TRAINER))
                EquipmentRequirementMode.EXTERNAL_LOAD -> EquipmentRequirement(EquipmentInformationState.KNOWN,mode,externalLoadSupport=true)
                EquipmentRequirementMode.UNKNOWN -> EquipmentRequirement(EquipmentInformationState.UNKNOWN,mode,sourceState=SemanticState.EVIDENCE_UNRESOLVED)
            }
        }
    }

    @Test fun unresolvedStatesRemainDistinct() {
        assertNotEquals(SemanticState.UNKNOWN,SemanticState.EVIDENCE_UNRESOLVED)
        assertNotEquals(SemanticState.EVIDENCE_UNRESOLVED,SemanticState.SCHEMA_UNRESOLVED)
        assertNotEquals(SemanticState.VARIABLE,SemanticState.VARIANT_DEPENDENT)
        assertNotEquals(SemanticState.PRESCRIPTION_DEPENDENT,SemanticState.NOT_APPLICABLE)
    }

    @Test fun legacyLevelIsNeverMappedToCapability() {
        val r=V3AssetLoader.load(fixture(),1).records.single()
        assertEquals(SemanticState.UNKNOWN,r.capabilities.strengthRequirement.state)
        assertNull(r.legacyLevel)
    }

    @Test fun strictNegativeCasesFailClosed() {
        reject(fixture(equipmentMode="BOGUS"))
        reject(fixture(capability="ADVANCED"))
        reject(fixture(targetType="THING"))
        reject(fixture(relationType="HELPER"))
        reject(fixture(schema="9"))
        reject(fixture(id=" "))
        reject(fixture().dropLast(2)+","+fixture("ex-1").removePrefix("["))
        reject("{")
    }

    private fun reject(json:String) {
        try { V3AssetLoader.load(json,1); fail("expected validation failure") }
        catch (_: V3AssetValidationException) {}
    }
}
