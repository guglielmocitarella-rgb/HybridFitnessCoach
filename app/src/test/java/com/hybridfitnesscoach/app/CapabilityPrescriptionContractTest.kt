package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class CapabilityPrescriptionContractTest {
 @Test fun unassessedProfileFailsClosed(){val c=AthleteProfile().toCapabilityProfile();assertFalse(c.assessed);assertEquals(1,c.exerciseLevel)}
 @Test fun assessedProfileDerivesOverallCapabilityFromDimensions(){val c=AthleteProfile(assessedTrainingExperience=3,assessedPushCapacity=1,assessedPullCapacity=3).toCapabilityProfile();assertTrue(c.assessed);assertEquals(2,c.exerciseLevel);assertEquals(3,c.trainingExperience);assertEquals(1,c.pushCapacity);assertEquals(3,c.pullCapacity)}
 @Test fun bodyweightPushAndPullUseSeparateAssessedCapacity(){val c=CapabilityProfile(2,2,1,3,true);val push=Exercise("Capability push","Petto","Corpo libero","Avanzato",category=ExerciseCategory.PUSH,resistanceMode=ResistanceMode.BODYWEIGHT);val pull=Exercise("Capability pull","Dorso","Corpo libero","Avanzato",category=ExerciseCategory.PULL,resistanceMode=ResistanceMode.BODYWEIGHT);assertEquals(1,capabilityLevelFor(push,c));assertEquals(3,capabilityLevelFor(pull,c))}
 @Test fun externalResistanceUsesTrainingExperience(){val ex=Exercise("Capability press","Petto","Bilanciere","Intermedio",category=ExerciseCategory.PUSH,resistanceMode=ResistanceMode.EXTERNAL_LOAD);val a=AthleteProfile(goal="Forza",assessedTrainingExperience=3,assessedPushCapacity=1,assessedPullCapacity=1);assertEquals(3,capabilityLevelFor(ex,a.toCapabilityProfile()));assertTrue(effectiveRunnerPrescription(ex,a,emptyList(),80).sourceBased)}
 @Test fun generatorFiltersPushAndPullByTheirOwnAssessedCapacity(){
  val location=TrainingLocation("test","Test","",TrainingLocationKind.GYM,setOf(EquipmentId.BODYWEIGHT))
  val easyPush=Exercise("Easy push","Petto","Corpo libero","Principiante",category=ExerciseCategory.PUSH,resistanceMode=ResistanceMode.BODYWEIGHT)
  val hardPush=Exercise("Hard push","Petto","Corpo libero","Avanzato",category=ExerciseCategory.PUSH,resistanceMode=ResistanceMode.BODYWEIGHT)
  val hardPull=Exercise("Hard pull","Dorso","Corpo libero","Avanzato",category=ExerciseCategory.PULL,resistanceMode=ResistanceMode.BODYWEIGHT)
  val athlete=AthleteProfile(assessedTrainingExperience=2,assessedPushCapacity=1,assessedPullCapacity=3)
  val workout=generateWorkoutFromCatalogue(listOf(easyPush,hardPush,hardPull),location,athlete,athlete.toCapabilityProfile(),emptyList())
  assertTrue(workout.any{it.name=="Easy push"})
  assertFalse(workout.any{it.name=="Hard push"})
  assertTrue(workout.any{it.name=="Hard pull"})
 }

}
