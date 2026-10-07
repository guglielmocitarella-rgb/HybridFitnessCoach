#!/usr/bin/env python3
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
data=json.loads((ROOT/"exercise-db-canonical/exercise-db-v3-final-candidate.json").read_text())
rows=[]
for x in data:
    if x["capability"]["strengthRequirement"]!="PRESCRIPTION_DEPENDENT": continue
    src=x["resistance"]["source"]
    if src=="EXTERNAL_LOAD": unit=x["resistance"]["loadInput"].get("unitSemantics")
    elif src=="ASSISTED": unit=x["resistance"]["assistanceInput"].get("unitSemantics")
    else: raise SystemExit(f"unsupported PD resistance source {src} for {x['canonicalExerciseId']}")
    if not unit or unit=="UNRESOLVED": raise SystemExit(f"missing typed unit for {x['canonicalExerciseId']}")
    dims={d["type"] for d in x["prescription"]["dimensions"]}
    rows.append((x["canonicalExerciseId"],src,unit,"REPS" in dims,"DURATION" in dims,"DISTANCE" in dims,"INTERVALS" in dims))
if len(rows)!=441: raise SystemExit(f"expected 441 rows, got {len(rows)}")
def b(v): return "true" if v else "false"
entries=",\n".join('Entry(%s,%s,%s,%s,%s,%s,%s)'%(json.dumps(i),json.dumps(s),json.dumps(u),b(r),b(d),b(di),b(iv)) for i,s,u,r,d,di,iv in rows)
code=f'''package com.hybridfitnesscoach.app.domain.v3
import org.junit.Assert.*
import org.junit.Test
class V3PdGeneratedCoverageTest {{
 data class Entry(val id:String,val source:String,val unit:String,val reps:Boolean,val duration:Boolean,val distance:Boolean,val intervals:Boolean)
 private val rows=listOf(
{entries}
 )
 private val low=SemanticValue.known(CapabilityLevel.LOW)
 private fun exercise(e:Entry):ExerciseV3 {{
  val source=ResistanceSource.valueOf(e.source); val unit=InputUnit.valueOf(e.unit)
  return ExerciseV3(CanonicalExerciseId(e.id),EquipmentRequirement(EquipmentInformationState.KNOWN,EquipmentRequirementMode.NONE),
   ResistanceSpec(SemanticValue.known(source),loadUnit=if(source==ResistanceSource.EXTERNAL_LOAD)SemanticValue.known(unit) else SemanticValue.notApplicable(),assistanceUnit=if(source==ResistanceSource.ASSISTED)SemanticValue.known(unit) else SemanticValue.notApplicable()),
   CapabilityRequirements(SemanticValue.prescriptionDependent(),low,low,low),ExerciseCategoryV3("OTHER"),movementPatterns=SemanticValue.known(setOf(MovementPatternV3("OTHER"))),
   prescription=ExercisePrescription(listOf(PrescribedValue(if(source==ResistanceSource.EXTERNAL_LOAD)PrescriptionDimension.LOAD else PrescriptionDimension.ASSISTANCE))),version=V3Version("3.7"))
 }}
 private fun work(e:Entry)=V3WorkDimensions(if(e.reps)8.0 else null,if(e.duration)30.0 else null,if(e.distance)100.0 else null,if(e.intervals)3.0 else null)
 @Test fun frozen441TraverseNativeBoundary() {{
  assertEquals(441,rows.size);assertEquals(436,rows.count{{it.source=="EXTERNAL_LOAD"}});assertEquals(5,rows.count{{it.source=="ASSISTED"}})
  rows.forEach{{e->val x=exercise(e);val source=ResistanceSource.valueOf(e.source);val unit=InputUnit.valueOf(e.unit);val w=work(e);val q=V3QuantifiedResistance(10.0,unit)
   val point=V3CapacityEnvelopePoint(x.id,source,w,maximumFeasibleLoad=if(source==ResistanceSource.EXTERNAL_LOAD)q else null,minimumRequiredAssistance=if(source==ResistanceSource.ASSISTED)q else null)
   val ctx=V3PdStrengthContext(V3PrescriptionContext(x.id,source,w,q),V3AthleteExerciseCapacityEnvelope(listOf(point)))
   assertEquals(e.id,V3ContextualEligibilityState.ELIGIBLE,V3PdStrengthResolver.evaluate(x,ctx).state)
   assertEquals(e.id,V3ContextualEligibilityState.EXERCISE_REQUIREMENT_UNRESOLVED,V3PdStrengthResolver.evaluate(x,null).state)
   assertEquals(e.id,SemanticState.PRESCRIPTION_DEPENDENT,x.capabilities.strengthRequirement.state)
  }}
 }}
}}
'''
out=ROOT/"app/src/test/java/com/hybridfitnesscoach/app/domain/v3/V3PdGeneratedCoverageTest.kt"
out.write_text(code)
print(f"generated {len(rows)} frozen-candidate PD fixtures")
