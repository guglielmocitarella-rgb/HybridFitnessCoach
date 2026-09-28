package com.hybridfitnesscoach.app
import org.junit.Assert.*
import org.junit.Test
class V091PlanningTest {
 private val source=EvidenceRef("test-source","Synthetic test source","section","p.1",EvidenceType.GUIDELINE)
 @Test fun prescriptionCannotExistWithoutEvidence(){assertThrows(IllegalArgumentException::class.java){SourcePrescriptionBaseline(3,8.0,restSeconds=120,evidence=emptyList())}}
 @Test fun contextualPrescriptionKeepsProvenanceAndDeloadsDeterministically(){val base=SourcePrescriptionBaseline(4,8.0,100.0,8.0,180,evidence=listOf(source));val out=ContextualPrescriptionEngine.prescribe(base,PrescriptionContext("Forza","Intermedio",ExerciseCategory.PUSH,8,32,8,0.8,30,null));assertEquals(AdaptiveDecision.DELOAD,out.progressionSuggestion);assertTrue(out.sets<base.sets);assertTrue(out.loadKg!!<base.loadKg!!);assertEquals(source,out.evidence.single())}
 @Test fun multiWeekPlanCreatesDatedNonAliasedSessions(){val ex=Exercise("Test","Core","Corpo libero","Principiante");val plan=TrainingPlanEngine.schedule("p",1005,4,setOf(1,3,5)){week,session->listOf(ex.copy(name="Test w"+week+" s"+session))};assertEquals(12,plan.workouts.size);assertEquals(4,plan.weeks);assertEquals(12,plan.workouts.map{it.id}.distinct().size);assertEquals(12,plan.workouts.map{it.exercises.first().name}.distinct().size);assertEquals(listOf(1005L,1007L,1009L),plan.workouts.take(3).map{it.epochDay})}
 @Test fun reschedulePreservesOriginalDateAndFlagsRecoveryConflict(){val ex=Exercise("Test","Core","Corpo libero","Principiante");val plan=TrainingPlanEngine.schedule("p",1005,1,setOf(1,3)){_,_->listOf(ex)};val moved=TrainingPlanEngine.reschedule(plan,"p-w1-s1",1007,minimumGapDays=2);val item=moved.workouts.first{it.id=="p-w1-s1"};assertEquals(1005,item.originalEpochDay);assertEquals(1007,item.epochDay);assertEquals(PlannedWorkoutState.RESCHEDULED,item.state);assertNotNull(item.adaptationReason)}
 @Test fun skippedWorkoutRemainsInCalendar(){val ex=Exercise("Test","Core","Corpo libero","Principiante");val plan=TrainingPlanEngine.schedule("p",1000,1,setOf(1)){_,_->listOf(ex)};val skipped=TrainingPlanEngine.skip(plan,"p-w1-s1");assertEquals(PlannedWorkoutState.SKIPPED,skipped.workouts.single().state)}
 @Test fun scheduleUsesWeekdaysWhenStartIsWednesday(){val ex=Exercise("Test","Core","Corpo libero","Principiante");val wednesday=java.time.LocalDate.of(2026,9,30).toEpochDay();val plan=TrainingPlanEngine.schedule("wed",wednesday,2,setOf(1,3,5)){_,_->listOf(ex)};assertEquals(listOf(0L,2L,5L,7L,9L,12L),plan.workouts.map{it.epochDay-wednesday})}
 @Test fun immediatePlanStartsTodayAndSpreadsSessions(){val ex=Exercise("Test","Core","Corpo libero","Principiante");val today=java.time.LocalDate.of(2026,9,30).toEpochDay();for(frequency in 2..5){val plan=TrainingPlanEngine.scheduleImmediately("p",today,2,frequency){_,_->listOf(ex)};assertEquals(today,plan.workouts.first().epochDay);assertEquals(frequency*2,plan.workouts.size);assertEquals(plan.workouts.size,plan.workouts.map{it.epochDay}.distinct().size);assertTrue(plan.workouts.zipWithNext().all{(a,b)->a.epochDay<b.epochDay})}}
 @Test fun immediatePlanRespectsExplicitUnavailableToday(){val ex=Exercise("Test","Core","Corpo libero","Principiante");val today=java.time.LocalDate.of(2026,9,30).toEpochDay();val plan=TrainingPlanEngine.scheduleImmediately("p",today,1,2,setOf(1,5)){_,_->listOf(ex)};assertEquals(today+2,plan.workouts.first().epochDay)}
 @Test fun actualCompletionMovesOnlyConflictingNextSession(){val ex=Exercise("Test","Core","Corpo libero","Principiante");val plan=TrainingPlanEngine.scheduleImmediately("p",1005,1,2){_,_->listOf(ex)};val first=plan.workouts.first();val next=plan.workouts[1];val update=AdaptiveScheduler.complete(plan,first.id,next.epochDay,minimumGapDays=2);assertEquals(PlannedWorkoutState.COMPLETED,update.plan.workouts.first{it.id==first.id}.state);assertEquals(next.epochDay,update.plan.workouts.first{it.id==first.id}.actualExecutionEpochDay);assertEquals(next.originalEpochDay,update.plan.workouts.first{it.id==next.id}.originalEpochDay);assertEquals(next.epochDay+2,update.plan.workouts.first{it.id==next.id}.epochDay);assertEquals(1,update.decisions.size);assertEquals(ScheduleAction.MOVE_LATER,update.decisions.single().action)}
 @Test fun onTimeCompletionKeepsFutureCalendar(){val ex=Exercise("Test","Core","Corpo libero","Principiante");val plan=TrainingPlanEngine.scheduleImmediately("p",1005,1,3){_,_->listOf(ex)};val update=AdaptiveScheduler.complete(plan,plan.workouts.first().id,1005,minimumGapDays=2);assertTrue(update.decisions.isEmpty());assertEquals(plan.workouts.drop(1),update.plan.workouts.drop(1))}
 @Test fun delayedCompletionDoesNotStackThreeSessions(){val ex=Exercise("Test","Core","Corpo libero","Principiante");val plan=TrainingPlanEngine.scheduleImmediately("p",1005,1,3){_,_->listOf(ex)};val update=AdaptiveScheduler.complete(plan,plan.workouts.first().id,1007,minimumGapDays=2);assertEquals(listOf(1005L,1009L,1011L),update.plan.workouts.map{it.epochDay});assertEquals(listOf(ScheduleAction.MOVE_LATER,ScheduleAction.MOVE_LATER),update.decisions.map{it.action});assertEquals(3,update.plan.workouts.map{it.id}.distinct().size)}
 @Test fun exerciseStatsUseOnlyActualCompletedSets(){val sets=listOf(WorkoutSetState(0,actualReps=8,actualLoadKg=20.0,completed=true),WorkoutSetState(1,actualReps=12,actualLoadKg=40.0,completed=false));val entry=SessionRecord(1000,"Palestra",1,2,structuredExercises=listOf(WorkoutExerciseState("Curl",sets=sets)));val stats=ExerciseStatistics.fromHistory("Curl",listOf(entry));assertEquals(1,stats.completedSets);assertEquals(8,stats.totalReps);assertEquals(160.0,stats.totalLoadVolumeKg,0.001);assertEquals(20.0,stats.maxLoadKg!!,0.001);assertEquals(1,stats.sessions)}
 @Test fun exerciseStatsHandleLegacyHistoryWithoutStructuredSets(){val legacy=SessionRecord(1000,"Palestra",2,2,exerciseNames=listOf("Curl"));val stats=ExerciseStatistics.fromHistory("Curl",listOf(legacy));assertEquals(0,stats.completedSets);assertEquals(0,stats.totalReps);assertEquals(0.0,stats.totalLoadVolumeKg,0.001)}
 @Test fun videoCatalogOnlyExposesVerifiedEntries(){val verified=ExerciseVideo("Curl","https://example.org/curl","Example","curl",verificationState=VideoVerificationState.VERIFIED);val pending=ExerciseVideo("Squat","https://example.org/squat","Example","squat",verificationState=VideoVerificationState.PENDING);assertEquals(verified,ExerciseVideoCatalog.findVerified("Curl",listOf(verified,pending)));assertNull(ExerciseVideoCatalog.findVerified("Squat",listOf(verified,pending)));assertNull(ExerciseVideoCatalog.findVerified("Unknown",listOf(verified,pending)))}
 @Test fun skippedWorkoutDoesNotAutomaticallyStackOrDeleteFutureSessions(){val ex=Exercise("Test","Core","Corpo libero","Principiante");val plan=TrainingPlanEngine.scheduleImmediately("p",1005,2,3){_,_->listOf(ex)};val update=AdaptiveScheduler.skip(plan,plan.workouts.first().id);assertEquals(PlannedWorkoutState.SKIPPED,update.plan.workouts.first().state);assertEquals(plan.workouts.drop(1),update.plan.workouts.drop(1));assertEquals(ScheduleAction.SKIP,update.decisions.single().action);assertEquals(plan.workouts.size,update.plan.workouts.size)}

 @Test fun unlinkedSessionNeverConsumesPlanByExerciseOverlap(){val a=Exercise("A","Core","Corpo libero","Principiante");val b=Exercise("B","Core","Corpo libero","Principiante");val c=Exercise("C","Core","Corpo libero","Principiante");val plan=TrainingPlan("p",1000,1,2,listOf(PlannedWorkout("near",1000,0,0,listOf(a)),PlannedWorkout("match",1005,0,1,listOf(a,b,c))));val session=SessionRecord(0,"Palestra",3,3,exerciseNames=listOf("A","B","C"));assertNull(PlannedWorkoutReconciler.findCandidate(plan,session,1000));assertEquals(plan,PlannedWorkoutReconciler.complete(plan,session,1000,2).plan)}
 @Test fun reconcilerDoesNotCompletePlanWhenWorkoutHasNoExerciseOverlap(){val a=Exercise("A","Core","Corpo libero","Principiante");val b=Exercise("B","Core","Corpo libero","Principiante");val plan=TrainingPlan("p",1000,1,1,listOf(PlannedWorkout("w",1000,0,0,listOf(a))));val session=SessionRecord(0,"Palestra",1,1,exerciseNames=listOf("B"));val update=PlannedWorkoutReconciler.complete(plan,session,1000,2);assertEquals(plan,update.plan);assertTrue(update.decisions.isEmpty())}
 @Test fun reconcilerCompletesMatchedWorkoutAndPreservesImmutableOriginalDate(){val a=Exercise("A","Core","Corpo libero","Principiante");val plan=TrainingPlanEngine.scheduleImmediately("p",1000,1,2){_,_->listOf(a)};val first=plan.workouts.first();val session=SessionRecord(0,"Palestra",1,1,exerciseNames=listOf("A"),plannedWorkoutId=first.id);val update=PlannedWorkoutReconciler.complete(plan,session,1002,2);val done=update.plan.workouts.first{it.id==first.id};assertEquals(PlannedWorkoutState.COMPLETED,done.state);assertEquals(1000,done.originalEpochDay);assertEquals(1002L,done.actualExecutionEpochDay)}
 @Test fun customSessionCannotConsumePlannedWorkoutByExerciseOverlap(){val a=Exercise("A","Core","Corpo libero","Principiante");val plan=TrainingPlanEngine.scheduleImmediately("p",1000,1,2){_,_->listOf(a)};val custom=SessionRecord(0,"Personalizzato • Core",1,1,exerciseNames=listOf("A"));assertNull(PlannedWorkoutReconciler.findCandidate(plan,custom,1000));assertEquals(plan,PlannedWorkoutReconciler.complete(plan,custom,1000,2).plan)}
 @Test fun ambiguousUnlinkedSessionDoesNotCompleteFuturePlan(){val a=Exercise("A","Core","Corpo libero","Principiante");val plan=TrainingPlanEngine.scheduleImmediately("p",1000,1,2){_,_->listOf(a)};val session=SessionRecord(0,"Palestra",1,1,exerciseNames=listOf("A"));assertNull(PlannedWorkoutReconciler.findCandidate(plan,session,1002))}
 @Test fun reconcilerRecordsPartiallyExecutedWorkoutAsPartial(){val a=Exercise("A","Core","Corpo libero","Principiante");val plan=TrainingPlanEngine.scheduleImmediately("p",1000,1,2){_,_->listOf(a)};val first=plan.workouts.first();val session=SessionRecord(0,"Palestra",1,3,exerciseNames=listOf("A"),plannedWorkoutId=first.id);val update=PlannedWorkoutReconciler.complete(plan,session,1000,0);assertEquals(PlannedWorkoutState.PARTIAL,update.plan.workouts.first().state);assertEquals(1000L,update.plan.workouts.first().actualExecutionEpochDay)}
 @Test fun plannedExerciseHistoryIntroducesControlledVariety(){val profile=profiles.first{it.name=="Palestra"};val athlete=AthleteProfile(goal="Forza",level="Intermedio");val first=generateWorkout(profile,athlete,emptyList());assertTrue(first.isNotEmpty());val second=generateWorkout(profile,athlete,emptyList(),avoidPlannedNames=first.map{it.name}.toSet());assertTrue(second.isNotEmpty());assertNotEquals(first.map{it.name},second.map{it.name});assertTrue(second.all{equipmentCompatible(it,profile)})}
 @Test fun completedOrPartialWorkoutCannotBeRescheduled(){val a=Exercise("A","Core","Corpo libero","Principiante");val plan=TrainingPlanEngine.scheduleImmediately("p",1000,1,2){_,_->listOf(a)};for(state in listOf(PlannedWorkoutState.COMPLETED,PlannedWorkoutState.PARTIAL)){val locked=plan.copy(workouts=plan.workouts.mapIndexed{index,w->if(index==0)w.copy(state=state,actualExecutionEpochDay=1001L)else w});assertEquals(locked,TrainingPlanEngine.reschedule(locked,locked.workouts.first().id,1010));assertEquals(locked,TrainingPlanEngine.skip(locked,locked.workouts.first().id))}}
 @Test fun deletingOnlyLinkedSessionReopensPlanEntryWithoutChangingOtherDates(){
  val ex=Exercise("A","Core","Corpo libero","Principiante")
  val plan=TrainingPlanEngine.scheduleImmediately("p",1000,1,2){_,_->listOf(ex)}
  val first=plan.workouts.first()
  val completed=plan.copy(workouts=plan.workouts.map{if(it.id==first.id)it.copy(state=PlannedWorkoutState.COMPLETED,actualExecutionEpochDay=1001L)else it})
  val deleted=SessionRecord(1,"Palestra",1,1,plannedWorkoutId=first.id)
  val restored=PlannedWorkoutReconciler.afterDeletion(completed,deleted,emptyList())
  assertEquals(PlannedWorkoutState.PLANNED,restored.workouts.first().state)
  assertNull(restored.workouts.first().actualExecutionEpochDay)
  assertEquals(completed.workouts.drop(1),restored.workouts.drop(1))
  assertEquals(completed,PlannedWorkoutReconciler.afterDeletion(completed,deleted,listOf(deleted.copy(timestamp=2))))
 }

 @Test fun adaptiveCoachReadsPlanAndSuppressesAdvanceAfterDisruption(){
  val ex=Exercise("A","Core","Corpo libero","Principiante")
  val now=1_800_000_000_000L
  val sessions=listOf(
   SessionRecord(now-86_400_000,"Palestra",10,10,workoutRpe=7,structuredExercises=listOf(WorkoutExerciseState("A",sets=listOf(WorkoutSetState(0,actualReps=10,completed=true))))),
   SessionRecord(now-2*86_400_000,"Palestra",10,10,workoutRpe=7,structuredExercises=listOf(WorkoutExerciseState("A",sets=listOf(WorkoutSetState(0,actualReps=10,completed=true)))))
  )
  val today=java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneOffset.UTC).toLocalDate().toEpochDay()
  val disrupted=TrainingPlan("p",today-7,2,2,listOf(
   PlannedWorkout("done",today-2,0,0,listOf(ex),state=PlannedWorkoutState.COMPLETED),
   PlannedWorkout("skip",today-1,0,1,listOf(ex),state=PlannedWorkoutState.SKIPPED),
   PlannedWorkout("next",today+2,1,0,listOf(ex))
  ))
  val historyOnly=coachingContext(sessions,readiness=90,now=now)
  assertEquals(ProgressionDecision.ADVANCE,historyOnly.plan.decision)
  val combined=coachingContext(sessions,90,disrupted,today)
  assertEquals(ProgressionDecision.MAINTAIN,combined.plan.decision)
  assertEquals(1,combined.planDisruptions)
  assertEquals(1,combined.upcomingSessions)
  assertTrue(combined.plan.notes.any{it.startsWith("Piano:")})
 }
 @Test fun adaptiveCoachNeverCreatesProgressionFromPlanAlone(){
  val ex=Exercise("A","Core","Corpo libero","Principiante")
  val plan=TrainingPlan("p",1000,1,1,listOf(PlannedWorkout("next",1001,0,0,listOf(ex))))
  val combined=coachingContext(emptyList(),90,plan,1000)
  assertNotEquals(ProgressionDecision.ADVANCE,combined.plan.decision)
  assertEquals(1,combined.upcomingSessions)
 }

 @Test fun schedulingRejectsZeroWeeksAndZeroTrainingDays(){
  val ex=Exercise("A","Core","Corpo libero","Principiante")
  assertThrows(IllegalArgumentException::class.java){TrainingPlanEngine.schedule("p",1000,0,setOf(1)){_,_->listOf(ex)}}
  assertThrows(IllegalArgumentException::class.java){TrainingPlanEngine.schedule("p",1000,1,emptySet()){_,_->listOf(ex)}}
 }
 @Test fun oneWeekOneDayCreatesExactlyOneStableWorkout(){
  val ex=Exercise("A","Core","Corpo libero","Principiante")
  val plan=TrainingPlanEngine.schedule("p",1000,1,setOf(java.time.LocalDate.ofEpochDay(1000).dayOfWeek.value)){_,_->listOf(ex)}
  assertEquals(1,plan.workouts.size);assertEquals(1000L,plan.workouts.single().epochDay);assertEquals("p-w1-s1",plan.workouts.single().id)
 }
 @Test fun completionPreservesWorkoutIdentityAcrossAdaptiveMoves(){
  val ex=Exercise("A","Core","Corpo libero","Principiante")
  val plan=TrainingPlanEngine.scheduleImmediately("p",1000,2,2){_,_->listOf(ex)}
  val ids=plan.workouts.map{it.id}
  val update=AdaptiveScheduler.complete(plan,ids.first(),1004,2)
  assertEquals(ids,update.plan.workouts.map{it.id})
  assertEquals(ids.size,update.plan.workouts.map{it.id}.distinct().size)
 }

 @Test fun evidenceAuditClaimsOnlyVerifiedGeneralResistancePrescriptionCoverage(){
  val audit=ExerciseEvidenceRegistry.audit(exercises)
  assertEquals(exercises.distinctBy{it.name}.size,audit.totalExercises)
  assertTrue(audit.exercisesWithAnyEvidence>0)
  assertEquals(audit.exercisesWithAnyEvidence,audit.exercisesWithPrescriptionEvidence)
  assertEquals(0,audit.completeExercises)
  assertTrue(audit.missingExerciseNames.isNotEmpty())
 }
 @Test fun acsmGeneralEvidenceIsPrescriptionOnlyAndNotTechniqueEvidence(){
  val resistance=Exercise("Synthetic squat","Gambe","Bilanciere","Base",category=ExerciseCategory.LEGS,resistanceMode=ResistanceMode.EXTERNAL_LOAD)
  val evidence=ExerciseEvidenceRegistry.evidenceFor(resistance)!!
  assertEquals(1,evidence.prescription.size)
  assertTrue(evidence.technique.isEmpty())
  assertEquals(EvidenceCoverage.PRESCRIPTION_ONLY,ExerciseEvidenceRegistry.coverage(resistance))
 }
 @Test fun evidenceRegistryDefaultsToNoneInsteadOfInventingProvenance(){
  assertEquals(EvidenceCoverage.NONE,ExerciseEvidenceRegistry.coverage(Exercise("__unknown__","Altro","Nessuna","Base",category=ExerciseCategory.OTHER,resistanceMode=ResistanceMode.NONE)))
  assertNull(ExerciseEvidenceRegistry.evidenceFor("__unknown__"))
 }

 @Test fun resistanceEvidenceExposesSourceLocationForExerciseDetail(){
  val resistance=Exercise("Synthetic press","Petto","Bilanciere","Base",category=ExerciseCategory.PUSH,resistanceMode=ResistanceMode.EXTERNAL_LOAD)
  val source=ExerciseEvidenceRegistry.evidenceFor(resistance)!!.prescription.single()
  assertTrue(source.sourceTitle.contains("ACSM"))
  assertTrue(source.section!!.contains("Resistance Training"))
  assertTrue(source.pageOrLocation!!.contains("353"))
  assertEquals(EvidenceCoverage.PRESCRIPTION_ONLY,ExerciseEvidenceRegistry.coverage(resistance))
 }

 @Test fun contextualPrescriptionIsFailClosedWithoutVerifiedNumericBaseline(){
  val ex=Exercise("Synthetic press","Petto","Bilanciere","Base",category=ExerciseCategory.PUSH,resistanceMode=ResistanceMode.EXTERNAL_LOAD)
  val context=PrescriptionContext("Forza","Intermedio",ExerciseCategory.PUSH,5,5,3,0.95,90,null)
  val baseline=SourcePrescriptionBaselineRegistry.baselineFor(ex,context)
  assertNotNull(baseline)
  assertEquals(3,baseline!!.sets)
  assertEquals(6.0,baseline.repsOrDuration,0.001)
  assertEquals(180,baseline.restSeconds)
  assertNotNull(SourcePrescriptionBaselineRegistry.prescribeIfVerified(ex,context))
 }
 @Test fun generalPrescriptionEvidenceDoesNotBecomeNumericBaseline(){
  val ex=Exercise("Synthetic squat","Gambe","Bilanciere","Base",category=ExerciseCategory.LEGS,resistanceMode=ResistanceMode.EXTERNAL_LOAD)
  assertEquals(EvidenceCoverage.PRESCRIPTION_ONLY,ExerciseEvidenceRegistry.coverage(ex))
  val context=PrescriptionContext("Ipertrofia","Intermedio",ExerciseCategory.LEGS,5,5,4,0.8,70,null)
  assertNotNull(SourcePrescriptionBaselineRegistry.baselineFor(ex,context))
 }

 @Test fun acsmBeginnerBaselineUsesSupportedGeneralPublicRange(){
  val ex=Exercise("Synthetic row","Dorso","Cavi","Principiante",category=ExerciseCategory.PULL,resistanceMode=ResistanceMode.EXTERNAL_LOAD)
  val c=PrescriptionContext("Benessere","Principiante",ExerciseCategory.PULL,4,4,4,null,70,null)
  val b=SourcePrescriptionBaselineRegistry.baselineFor(ex,c)!!
  assertEquals(2,b.sets);assertEquals(10.0,b.repsOrDuration,0.001);assertEquals(120,b.restSeconds)
 }
 @Test fun nonResistanceAndNonRepExercisesRemainOutsideRtBaseline(){
  val cardio=Exercise("Run","Cardio","Corpo libero","Principiante",category=ExerciseCategory.CARDIO,measure=ExerciseMeasure.DURATION,resistanceMode=ResistanceMode.BODYWEIGHT)
  val c=PrescriptionContext("Benessere","Principiante",ExerciseCategory.CARDIO,4,4,4,null,70,null)
  assertNull(SourcePrescriptionBaselineRegistry.baselineFor(cardio,c))
 }

 @Test fun verifiedPrescriptionProducesRunnerTargetsWithinSourceBaseline(){
  val ex=Exercise("Synthetic press","Petto","Bilanciere","Base",category=ExerciseCategory.PUSH,resistanceMode=ResistanceMode.EXTERNAL_LOAD)
  val c=PrescriptionContext("Forza","Intermedio",ExerciseCategory.PUSH,5,5,3,null,85,null)
  val p=SourcePrescriptionBaselineRegistry.prescribeIfVerified(ex,c)!!
  assertEquals(3,p.sets);assertEquals(6.0,p.repsOrDuration,0.001);assertEquals(180,p.restSeconds)
  assertTrue(p.evidence.any{it.sourceTitle.contains("ACSM")})
 }
 @Test fun bodyweightMobilityDoesNotInheritResistanceEvidenceOrNumericTargets(){
  val mobility=Exercise("Synthetic mobility","Mobilità","Corpo libero","Principiante",category=ExerciseCategory.MOBILITY,measure=ExerciseMeasure.REPS,resistanceMode=ResistanceMode.BODYWEIGHT)
  val context=PrescriptionContext("Forza","Principiante",ExerciseCategory.MOBILITY,3,3,3,null,80,null)
  assertEquals(EvidenceCoverage.NONE,ExerciseEvidenceRegistry.coverage(mobility))
  assertNull(SourcePrescriptionBaselineRegistry.baselineFor(mobility,context))
 }
}
