package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class WorkoutInfrastructureTest {
    @Test fun progressionRecognizesRealExerciseNameAndCase() {
        val wall = WorkoutExerciseState("Wall handstand", sets = (0..2).map {
            WorkoutSetState(it, actualSeconds = 30, completed = true)
        }, rpe = 7)
        val session = SessionRecord(1,"P",3,3,listOf("Wall handstand"),structuredExercises=listOf(wall))
        assertEquals(ProgressionStatus.IN_PROGRESS, SkillCatalog.progress("Handstand", listOf(session)).first().status)
        val taps = WorkoutExerciseState("Handstand shoulder taps", sets = (0..2).map {
            WorkoutSetState(it, actualReps = 8, completed = true)
        }, rpe = 7)
        val second = session.copy(timestamp=2)
        val tapSession = session.copy(timestamp=3, exerciseNames=listOf("Handstand shoulder taps"), structuredExercises=listOf(taps))
        val progress = SkillCatalog.progress("Handstand", listOf(session, second, tapSession))
        assertEquals(ProgressionStatus.MASTERED, progress[0].status)
        assertEquals(ProgressionStatus.IN_PROGRESS, progress[1].status)
    }
    @Test fun expandedSkillGraphLocksNodesUntilActualPrerequisiteMastery() {
        val initial = SkillCatalog.progress("Handstand", emptyList())
        assertEquals(ProgressionStatus.AVAILABLE, initial.first().status)
        assertTrue(initial.drop(1).all { it.status == ProgressionStatus.LOCKED })
        val wall = WorkoutExerciseState("Wall Handstand", sets = (0..2).map {
            WorkoutSetState(it, targetSeconds = 30, actualSeconds = 30, completed = true)
        }, rpe = 7)
        val once = SessionRecord(1,"P",3,3,listOf("Wall Handstand"),structuredExercises=listOf(wall))
        val twice = once.copy(timestamp = 2)
        assertEquals(ProgressionStatus.IN_PROGRESS, SkillCatalog.progress("Handstand", listOf(once)).first().status)
        val progress = SkillCatalog.progress("Handstand", listOf(once, twice))
        assertEquals(ProgressionStatus.MASTERED, progress.first().status)
        assertEquals(ProgressionStatus.AVAILABLE, progress[1].status)
        assertEquals(ProgressionStatus.LOCKED, progress[2].status)
    }
    @Test fun repsPrescriptionIsDataDriven() {
        val ex=Exercise("Pull-up","Dorso","Sbarra","Intermedio",role="Forza",measure=ExerciseMeasure.REPS,targetSets=3,targetValue=8.0)
        val p=prescriptionFor(ex)
        assertEquals(3,p.sets); assertEquals(8.0,p.value,0.0); assertEquals("rip",p.unit)
    }
    @Test fun isometricPrescriptionUsesSeconds() {
        val ex=Exercise("Front lever progression","Dorso / Core","Sbarra","Intermedio",measure=ExerciseMeasure.SECONDS,targetSets=3,targetValue=20.0)
        val p=prescriptionFor(ex)
        assertEquals(20.0,p.value,0.0); assertEquals("sec",p.unit)
    }
    @Test fun distancePrescriptionUsesMeters() {
        val ex=Exercise("Carry","Core","Manubri","Intermedio",measure=ExerciseMeasure.DISTANCE,targetValue=40.0)
        assertEquals("m",prescriptionFor(ex).unit)
    }
    @Test fun timerReconstructsFromAbsoluteDeadline() {
        assertEquals(30,remainingSeconds(130000L,100000L))
        assertEquals(0,remainingSeconds(90000L,100000L))
    }
    @Test fun partialSessionLoadsRecoveryByActualSets() {
        val now=1_000_000_000L
        val partial=SessionRecord(now,"Test",2,3,listOf("Squat"),completedByExercise=mapOf("Squat" to 2))
        val none=SessionRecord(now,"Test",0,3,listOf("Squat"),completedByExercise=mapOf("Squat" to 0))
        assertTrue((recoveryScores(listOf(partial),now)["Gambe"]?:100)<100)
        assertEquals(100,recoveryScores(listOf(none),now)["Gambe"])
    }
    @Test fun workoutStatePreservesPinnedSkippedAndPartialData() {
        val s=WorkoutExerciseState("Pull-up",ExerciseMeasure.REPS,listOf(WorkoutSetState(0,targetReps=8,actualReps=7,completed=true)),WorkoutExerciseStatus.PARTIAL,rpe=8,pinned=true)
        assertTrue(s.pinned); assertEquals(WorkoutExerciseStatus.PARTIAL,s.status); assertEquals(7,s.sets.first().actualReps)
    }
    @Test fun durationPrescriptionIsTimed(){val ex=Exercise("Run","Cardio","Corpo libero","Principiante",measure=ExerciseMeasure.DURATION,targetSets=1,targetValue=600.0);val p=prescriptionFor(ex);assertEquals(600.0,p.value,0.0);assertEquals("sec",p.unit)}
    @Test fun intervalPrescriptionUsesWorkSeconds(){val ex=Exercise("Intervals","Cardio","Corpo libero","Intermedio",measure=ExerciseMeasure.INTERVALS,targetSets=5,targetValue=60.0,intervalRestSec=60);val p=prescriptionFor(ex);assertEquals(5,p.sets);assertEquals("sec lavoro",p.unit)}
    @Test fun unilateralMetadataIsPreserved(){val ex=Exercise("Split","Gambe","Corpo libero","Intermedio",laterality=Laterality.UNILATERAL,targetValue=8.0);assertEquals(Laterality.UNILATERAL,ex.laterality)}
    @Test fun pausedElapsedTimeDoesNotAdvance(){val s=WorkoutSessionState("x",1000,"P",emptyList(),pausedAtEpochMs=11000);assertEquals(10,elapsedWorkoutSeconds(s,50000))}
    @Test fun structuredHistoryCountsOnlyExecutedSets(){val e=WorkoutExerciseState("A",sets=listOf(WorkoutSetState(0,targetReps=8,actualReps=8,completed=true),WorkoutSetState(1,targetReps=8,actualReps=7,completed=true),WorkoutSetState(2,targetReps=8)));val h=SessionRecord(1,"P",3,3,listOf("A"),structuredExercises=listOf(e));assertEquals(2,actualCompletedSets(h));assertEquals(15.0,executedVolume(h),0.0)}
    @Test fun healthReadinessBoundaryCarriesNormalizedInput(){val normalized=NormalizedHealthState(1,sleepScore=90.0);val input=AthleteReadinessInput(mapOf("Dorso" to 80),normalized,listOf(7));assertEquals(90.0,input.health!!.sleepScore!!,0.0);assertEquals(80,input.recoveryByMuscle["Dorso"])}

    @Test fun frontLeverCatalogueHasTimedProgression(){val linked=exercises.filter{"Front lever" in it.skills};assertTrue(linked.isNotEmpty());assertTrue(linked.any{it.measure==ExerciseMeasure.SECONDS})}
    @Test fun catalogueContainsDurationDistanceAndIntervals(){assertTrue(exercises.any{it.measure==ExerciseMeasure.DURATION});assertTrue(exercises.any{it.measure==ExerciseMeasure.DISTANCE});assertTrue(exercises.any{it.measure==ExerciseMeasure.INTERVALS})}
    @Test fun recoveryRisesWithTime(){val s=SessionRecord(1_000_000_000L,"P",3,3,listOf("Squat"),completedByExercise=mapOf("Squat" to 3));val fresh=recoveryScores(listOf(s),1_000_000_000L)["Gambe"]!!;val later=recoveryScores(listOf(s),1_000_000_000L+72*3_600_000L)["Gambe"]!!;assertTrue(later>fresh)}
    @Test fun generatorHonorsEquipmentLevelAndRecovery(){val gym=profiles.first{it.name=="Palestra"};val athlete=AthleteProfile(goal="Forza + Skill Calisthenics",level="Intermedio",prioritySkills=listOf("Front lever"));val base=generateWorkout(gym,athlete,emptyList());assertTrue(base.all{equipmentCompatible(it,gym)});assertTrue(base.all{it.level!="Avanzato"});assertTrue(base.any{"Front lever" in it.skills});val tired=SessionRecord(1_000_000_000L,"P",12,12,listOf("Tuck front lever","Australian pull-up"),completedByExercise=mapOf("Tuck front lever" to 6,"Australian pull-up" to 6));val adapted=generateWorkout(gym,athlete,listOf(tired));assertEquals(6,adapted.size)}

    @Test fun recoveryUsesStructuredPartialInsteadOfDeclaredDone(){val now=2_000_000_000L;val e=WorkoutExerciseState("Squat",sets=listOf(WorkoutSetState(0,targetReps=8,actualReps=8,completed=true),WorkoutSetState(1,targetReps=8),WorkoutSetState(2,targetReps=8)));val structured=SessionRecord(now,"P",3,3,listOf("Squat"),completedByExercise=mapOf("Squat" to 3),structuredExercises=listOf(e));val actual=recoveryScores(listOf(structured),now)["Gambe"]!!;val full=recoveryScores(listOf(SessionRecord(now,"P",3,3,listOf("Squat"),completedByExercise=mapOf("Squat" to 3))),now)["Gambe"]!!;assertTrue(actual>full)}

    @Test fun calisthenicsFrontLeverGuaranteesSkillWork(){val gym=profiles.first{it.name=="Palestra"};val athlete=AthleteProfile(goal="Calisthenics",level="Intermedio",prioritySkills=listOf("Front lever"));val w=generateWorkout(gym,athlete,emptyList());assertTrue(w.take(3).all{"Front lever" in it.skills});assertTrue(w.any{it.name=="Tuck front lever"})}
    @Test fun strengthGoalRemainsSupported(){val gym=profiles.first{it.name=="Palestra"};val w=generateWorkout(gym,AthleteProfile(goal="Forza",level="Intermedio"),emptyList());assertEquals(6,w.size);assertTrue(w.all{equipmentCompatible(it,gym)})}
    @Test fun intervalCatalogueIsExplicitActivity(){val ex=exercises.first{it.measure==ExerciseMeasure.INTERVALS};assertTrue(ex.name.contains("Corsa",ignoreCase=true));assertEquals(60,ex.intervalRestSec)}

    @Test fun progressionAdvancesOnlyAfterStableSuccess(){assertEquals(ProgressionDecision.ADVANCE,progressionDecision(0.95,7.5,85,2));assertEquals(ProgressionDecision.MAINTAIN,progressionDecision(0.95,7.5,85,1))}
    @Test fun progressionDeloadOverridesGoodPerformanceWhenRecoveryIsLow(){assertEquals(ProgressionDecision.DELOAD,progressionDecision(0.95,7.0,30,3))}
    @Test fun progressionRegressesOnLowSuccessRate(){assertEquals(ProgressionDecision.REGRESS,progressionDecision(0.50,7.0,80,0))}
    @Test fun frontLeverSnapshotExposesProgressAndNextUnlock(){val s=skillProgressSnapshot("Front lever","front_adv_tuck",0.8,7.0,80,1);assertEquals(40,s.progressPercent);assertEquals("front_one_leg",s.nextNodeId);assertEquals(ProgressionDecision.MAINTAIN,s.decision)}
    @Test fun loadRecommendationProgressesConservatively(){val r=recommendLoad(100.0,null,1.0,7.0,85);assertEquals(105.0,r.loadKg!!,0.0);assertEquals(5,r.changePercent)}
    @Test fun loadRecommendationDeloadsWhenRecoveryIsPoor(){val r=recommendLoad(100.0,null,1.0,7.0,30);assertEquals(90.0,r.loadKg!!,0.0)}
    @Test fun adaptiveCoachDeloadReducesVolumeAndIntensity(){val p=adaptiveWorkoutPlan(0.95,7.0,30,80);assertEquals(ProgressionDecision.DELOAD,p.decision);assertTrue(p.volumeFactor<1.0);assertTrue(p.intensityFactor<1.0)}
    @Test fun sequencingPrioritizesSkillThenStrength(){val a=Exercise("Accessory","A","Corpo libero","Principiante",role="Accessorio");val s=Exercise("Skill","A","Corpo libero","Principiante",role="Progressione");val f=Exercise("Strength","A","Corpo libero","Principiante",role="Forza");assertEquals(listOf("Skill","Strength","Accessory"),sequenceWorkout(listOf(a,f,s)).map{it.name})}
    @Test fun regenerationKeepsPinnedExerciseAtOriginalPosition(){val a=Exercise("A","A","Corpo libero","Principiante");val b=Exercise("B","A","Corpo libero","Principiante");val c=Exercise("C","A","Corpo libero","Principiante");val x=Exercise("X","A","Corpo libero","Principiante");val out=regenerateUnpinned(listOf(a,b,c),listOf(x),setOf("B"));assertEquals("B",out[1].name)}
    @Test fun v09EquipmentIncludesAbWheelAndFoamRoller(){val gym=profiles.first{it.name=="Palestra"};assertTrue("Ab Wheel" in gym.equipment);assertTrue("Foam Roller" in gym.equipment)}
}
