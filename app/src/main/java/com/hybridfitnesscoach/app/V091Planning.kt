package com.hybridfitnesscoach.app
import kotlin.math.roundToInt
enum class EvidenceType { GUIDELINE, TEXTBOOK, TECHNIQUE_MANUAL, PROGRESSION_REFERENCE, INTERNAL_OBSERVATION }
data class EvidenceRef(val sourceId:String,val sourceTitle:String,val section:String?=null,val pageOrLocation:String?=null,val evidenceType:EvidenceType)
data class ExerciseEvidence(val exerciseName:String,val technique:List<EvidenceRef> = emptyList(),val anatomy:List<EvidenceRef> = emptyList(),val prescription:List<EvidenceRef> = emptyList(),val progression:List<EvidenceRef> = emptyList()){val all:List<EvidenceRef> get()=(technique+anatomy+prescription+progression).distinct()}
data class PrescriptionContext(val goal:String,val athleteLevel:String,val exerciseType:ExerciseCategory,val intensity:Int,val volume:Int,val fatigue:Int,val recentPerformance:Double?,val recoveryStatus:Int?,val skillLevel:Int?)
data class SourcePrescriptionBaseline(val sets:Int,val repsOrDuration:Double,val loadKg:Double?=null,val rirOrRpe:Double?=null,val restSeconds:Int,val tempo:String?=null,val evidence:List<EvidenceRef>){init{require(sets>0);require(repsOrDuration>0);require(restSeconds>0);require(evidence.isNotEmpty()){"A source-based baseline requires provenance"}}}
data class ContextualPrescription(val sets:Int,val repsOrDuration:Double,val loadKg:Double?,val rirOrRpe:Double?,val restSeconds:Int,val tempo:String?,val progressionSuggestion:AdaptiveDecision,val evidence:List<EvidenceRef>,val explanation:String)
object ContextualPrescriptionEngine {
 fun prescribe(base:SourcePrescriptionBaseline,context:PrescriptionContext):ContextualPrescription{
  val recovery=context.recoveryStatus?.coerceIn(0,100);val performance=context.recentPerformance?.coerceIn(0.0,1.0)
  val decision=when{recovery!=null&&recovery<45->AdaptiveDecision.DELOAD;context.fatigue>=8||(performance!=null&&performance<0.65)->AdaptiveDecision.REGRESS;recovery!=null&&recovery>=70&&context.fatigue<=5&&(performance?:0.0)>=0.90->AdaptiveDecision.PROGRESS;else->AdaptiveDecision.MAINTAIN}
  val setFactor=when(decision){AdaptiveDecision.DELOAD->0.70;AdaptiveDecision.REGRESS->0.85;else->1.0};val targetFactor=when(decision){AdaptiveDecision.DELOAD->0.90;AdaptiveDecision.REGRESS->0.95;AdaptiveDecision.PROGRESS->1.025;AdaptiveDecision.MAINTAIN->1.0};val restFactor=when(decision){AdaptiveDecision.DELOAD->1.10;AdaptiveDecision.REGRESS->1.05;else->1.0}
  return ContextualPrescription((base.sets*setFactor).roundToInt().coerceAtLeast(1),(base.repsOrDuration*targetFactor).coerceAtLeast(1.0),base.loadKg?.let{(it*targetFactor*2.0).roundToInt()/2.0},base.rirOrRpe,(base.restSeconds*restFactor).roundToInt().coerceAtLeast(base.restSeconds),base.tempo,decision,base.evidence,when(decision){AdaptiveDecision.PROGRESS->"Baseline source-based adattata per trend positivo stabile";AdaptiveDecision.MAINTAIN->"Baseline source-based mantenuta";AdaptiveDecision.REGRESS->"Baseline source-based ridotta per performance/fatica";AdaptiveDecision.DELOAD->"Baseline source-based ridotta per recupero insufficiente"})
 }
}
enum class PlannedWorkoutState { PLANNED, COMPLETED, SKIPPED, RESCHEDULED, ADAPTED }
data class PlannedWorkout(val id:String,val epochDay:Long,val weekIndex:Int,val sequenceInWeek:Int,val exercises:List<Exercise>,val state:PlannedWorkoutState=PlannedWorkoutState.PLANNED,val originalEpochDay:Long=epochDay,val adaptationReason:String?=null,val actualExecutionEpochDay:Long?=null)
data class TrainingPlan(val id:String,val startEpochDay:Long,val weeks:Int,val daysPerWeek:Int,val workouts:List<PlannedWorkout>)
object TrainingPlanEngine {
 fun schedule(planId:String,startEpochDay:Long,weeks:Int,trainingDays:Set<Int>,workoutFactory:(week:Int,session:Int)->List<Exercise>):TrainingPlan{
  require(weeks in 1..52);require(trainingDays.isNotEmpty());require(trainingDays.all{it in 1..7});val startDayOfWeek=java.time.LocalDate.ofEpochDay(startEpochDay).dayOfWeek.value;val sorted=trainingDays.sortedBy{(it-startDayOfWeek+7)%7}
  val workouts=buildList{for(week in 0 until weeks)sorted.forEachIndexed{session,dayOfWeek->val offset=((dayOfWeek-startDayOfWeek+7)%7)+week*7L;add(PlannedWorkout(planId+"-w"+(week+1)+"-s"+(session+1),startEpochDay+offset,week,session,workoutFactory(week,session)))}}
  return TrainingPlan(planId,startEpochDay,weeks,sorted.size,workouts)
 }
 /** Creates an initial calendar from the creation date. Recovery-based changes belong to AdaptiveScheduler. */
 fun scheduleImmediately(planId:String,startEpochDay:Long,weeks:Int,sessionsPerWeek:Int,availableWeekdays:Set<Int>?=null,workoutFactory:(week:Int,session:Int)->List<Exercise>):TrainingPlan{
  require(weeks in 1..52);require(sessionsPerWeek in 1..7)
  require(availableWeekdays==null || (availableWeekdays.size>=sessionsPerWeek && availableWeekdays.all{it in 1..7}))
  val workouts=buildList{
   for(week in 0 until weeks){
    val weekStart=startEpochDay+week*7L
    val candidateDays=(0..6).filter{offset->availableWeekdays==null || java.time.LocalDate.ofEpochDay(weekStart+offset).dayOfWeek.value in availableWeekdays}
    val selected=if(availableWeekdays==null)(0 until sessionsPerWeek).map{it*7/sessionsPerWeek} else candidateDays.take(sessionsPerWeek)
    selected.forEachIndexed{session,offset->add(PlannedWorkout(planId+"-w"+(week+1)+"-s"+(session+1),weekStart+offset,week,session,workoutFactory(week,session)))}
   }
  }
  return TrainingPlan(planId,startEpochDay,weeks,sessionsPerWeek,workouts)
 }
 fun reschedule(plan:TrainingPlan,workoutId:String,newEpochDay:Long,minimumGapDays:Int=1):TrainingPlan{val target=plan.workouts.firstOrNull{it.id==workoutId}?:return plan;val others=plan.workouts.filterNot{it.id==workoutId};val nearest=others.minOfOrNull{kotlin.math.abs(it.epochDay-newEpochDay)}?:Long.MAX_VALUE;val reason=if(nearest<minimumGapDays)"Sessione vicina: richiede rivalutazione Adaptive Coach" else null;val changed=target.copy(epochDay=newEpochDay,state=PlannedWorkoutState.RESCHEDULED,adaptationReason=reason);return plan.copy(workouts=(others+changed).sortedBy{it.epochDay})}
 fun skip(plan:TrainingPlan,workoutId:String):TrainingPlan=plan.copy(workouts=plan.workouts.map{if(it.id==workoutId)it.copy(state=PlannedWorkoutState.SKIPPED)else it})
}

enum class ScheduleAction { KEEP_DATE, MOVE_EARLIER, MOVE_LATER, SKIP, RESCHEDULE, ADAPT_SESSION }
data class ScheduleDecision(val workoutId:String,val originalDate:Long,val currentDate:Long,val suggestedDate:Long,val action:ScheduleAction,val reason:String)
data class ScheduleUpdate(val plan:TrainingPlan,val decisions:List<ScheduleDecision>)
object AdaptiveScheduler {
 /** minimumGapDays must come from the caller's validated recovery prescription. */
 fun complete(plan:TrainingPlan,workoutId:String,actualEpochDay:Long,minimumGapDays:Int):ScheduleUpdate {
  require(minimumGapDays>=0)
  val target=plan.workouts.firstOrNull{it.id==workoutId}?:return ScheduleUpdate(plan,emptyList())
  if(target.state==PlannedWorkoutState.COMPLETED)return ScheduleUpdate(plan,emptyList())
  val future=plan.workouts.filter{it.id!=workoutId && it.state!=PlannedWorkoutState.COMPLETED && it.state!=PlannedWorkoutState.SKIPPED && it.epochDay>=target.epochDay}.sortedBy{it.epochDay}
  val next=future.firstOrNull()
  val newDate=next?.let{maxOf(it.epochDay,actualEpochDay+minimumGapDays)}
  val decision=if(next!=null && newDate!=null && newDate>next.epochDay)ScheduleDecision(next.id,next.originalEpochDay,next.epochDay,newDate,ScheduleAction.MOVE_LATER,"Recupero dopo la sessione eseguita")else null
  val changed=plan.workouts.map{item->when(item.id){workoutId->item.copy(state=PlannedWorkoutState.COMPLETED,actualExecutionEpochDay=actualEpochDay);decision?.workoutId->item.copy(epochDay=decision.suggestedDate,state=PlannedWorkoutState.RESCHEDULED,adaptationReason=decision.reason);else->item}}
  return ScheduleUpdate(plan.copy(workouts=changed),listOfNotNull(decision))
 }
}
