package com.hybridfitnesscoach.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Accent = Color(0xFF3568E8)
private val Bg = Color(0xFFF7F8FA)

internal enum class TrainingLocationKind(val stableId:String) { GYM("gym"), HOME("home"), OUTDOOR("outdoor"), CUSTOM("custom") }
internal data class TrainingLocation(val id:String,val name:String,val icon:String,val kind:TrainingLocationKind,val capabilities:Set<EquipmentId>) {
    init { require(id.isNotBlank()); require(name.isNotBlank()) }
}
internal fun locationCompatible(exercise:Exercise,location:TrainingLocation):Boolean {
    val required=EquipmentCatalog.requirementsFor(exercise)
    return required.isNotEmpty() && required.all{it in location.capabilities}
}
internal fun runnerEffort(value:Int?):Int = value?.takeIf { it in 1..10 } ?: 5
internal fun recommendedSetRest(role:String,name:String,equipment:String):Int {
    // RC2: rest must not be inferred from exercise-name substrings. Keep the
    // fallback inside the sourced programming ranges; richer goal/load-aware
    // prescriptions can override this value downstream.
    return when (role) {
        "Forza" -> 150
        "Progressione" -> 120
        "Prerequisito" -> 90
        else -> 90
    }
}
internal fun recommendedExerciseRest(role:String,name:String,equipment:String):Int =
    recommendedSetRest(role,name,equipment)
enum class ExerciseMeasure { REPS, SECONDS, DURATION, DISTANCE, INTERVALS }
enum class ExerciseCategory { PUSH, PULL, LEGS, CORE, CARDIO, MOBILITY, SKILL, OTHER }
enum class Laterality { BILATERAL, UNILATERAL, ALTERNATING, NONE }
enum class ResistanceMode { BODYWEIGHT, EXTERNAL_LOAD, ASSISTED, NONE }

/**
 * Universal exercise definition for the v0.8 evolved runner.
 * Legacy constructor fields stay first and keep defaults so the existing catalogue and
 * generated workouts remain source/data compatible. progressionKey is intentionally
 * opaque: v0.9 can attach a progression graph without changing this schema again.
 */
data class Exercise(
    val name: String,
    val group: String,
    val equipment: String,
    val level: String,
    val skills: List<String> = emptyList(),
    val role: String = "Base",
    val setRestSec: Int = recommendedSetRest(role, name, equipment),
    val exerciseRestSec: Int = recommendedExerciseRest(role, name, equipment),
    val secondaryGroups: List<String> = emptyList(),
    val movementPattern: String = "",
    val category: ExerciseCategory = ExerciseCategory.OTHER,
    val intensity: Int = 5,
    val systemicFatigue: Int = 5,
    val technicalComplexity: Int = 5,
    val measure: ExerciseMeasure = ExerciseMeasure.REPS,
    val laterality: Laterality = Laterality.BILATERAL,
    val resistanceMode: ResistanceMode = ResistanceMode.NONE,
    val progressionKey: String? = null,
    val targetSets: Int = 3,
    val targetValue: Double? = null,
    val intervalRestSec: Int? = null
)

enum class WorkoutExerciseStatus { PENDING, ACTIVE, COMPLETED, SKIPPED, PARTIAL }
data class WorkoutSetState(
    val index: Int,
    val targetReps: Int? = null,
    val targetSeconds: Int? = null,
    val targetDistanceMeters: Double? = null,
    val targetLoadKg: Double? = null,
    val targetAssistanceKg: Double? = null,
    val actualReps: Int? = null,
    val actualSeconds: Int? = null,
    val actualDistanceMeters: Double? = null,
    val actualLoadKg: Double? = null,
    val actualAssistanceKg: Double? = null,
    val completed: Boolean = false,
    val restUsedSec: Int? = null
)
data class WorkoutExerciseState(
    val exerciseName: String,
    val measure: ExerciseMeasure = ExerciseMeasure.REPS,
    val sets: List<WorkoutSetState> = emptyList(),
    val status: WorkoutExerciseStatus = WorkoutExerciseStatus.PENDING,
    val rpe: Int? = null,
    val pinned: Boolean = false,
    val setRestSec: Int = 90,
    val exerciseRestSec: Int = 120,
    val originalOrder: Int = 0,
    val currentOrder: Int = originalOrder,
    val exerciseId: String? = null,
    val exerciseSnapshot: Exercise? = null
)
data class WorkoutSessionState(
    val id: String,
    val startedAtEpochMs: Long,
    val profileName: String,
    val exercises: List<WorkoutExerciseState>,
    val currentExerciseIndex: Int = 0,
    val elapsedBeforePauseMs: Long = 0L,
    val pausedAtEpochMs: Long? = null,
    val restEndsAtEpochMs: Long? = null,
    val workoutRpe: Int? = null,
    val finishedAtEpochMs: Long? = null,
    val runnerKind: String = "automatic",
    val restKind: String? = null,
    val modifications: List<String> = emptyList(),
    val plannedWorkoutId: String? = null,
    val pausedRestRemainingSec: Int? = null,
    val trainingLocationId: String? = null
)
data class AthleteProfile(val name: String = "", val goal: String = "Forza + skill", val daysPerWeek: Int = 3, val age:Int=0, val heightCm:Float=0f, val weightKg:Float=0f, val sex:String="Non specificato", val prioritySkills:List<String> = emptyList(), val assessedTrainingExperience:Int?=null, val assessedPushCapacity:Int?=null, val assessedPullCapacity:Int?=null)
data class CapabilityProfile(
    val exerciseLevel:Int,
    val trainingExperience:Int? = null,
    val pushCapacity:Int? = null,
    val pullCapacity:Int? = null,
    val assessed:Boolean = false
)
internal fun AthleteProfile.toCapabilityProfile():CapabilityProfile {
    val experience=assessedTrainingExperience?.takeIf{it in 1..3}
    val push=assessedPushCapacity?.takeIf{it in 1..3}
    val pull=assessedPullCapacity?.takeIf{it in 1..3}
    val assessed=listOf(experience,push,pull).all{it!=null}
    val overall=if(assessed) {
        val score=(experience!!-1)+(push!!-1)+(pull!!-1)
        when { score>=5->3; score>=2->2; else->1 }
    } else 1
    return CapabilityProfile(overall,experience,push,pull,assessed)
}
data class FitnessLevelAssessment(val trainingMonths:Int,val pushUps:Int,val pullUps:Int)
internal fun capabilityFromAssessment(input:FitnessLevelAssessment):CapabilityProfile {
    require(input.trainingMonths>=0 && input.pushUps>=0 && input.pullUps>=0)
    val experience=when { input.trainingMonths>=24->3; input.trainingMonths>=6->2; else->1 }
    val push=when { input.pushUps>=30->3; input.pushUps>=10->2; else->1 }
    val pull=when { input.pullUps>=10->3; input.pullUps>=3->2; else->1 }
    val score=(experience-1)+(push-1)+(pull-1)
    val overall=when { score>=5->3; score>=2->2; else->1 }
    return CapabilityProfile(overall,experience,push,pull,assessed=true)
}
internal fun capabilityLevelName(level:Int):String = when(level.coerceIn(1,3)){1->"Principiante";2->"Intermedio";else->"Avanzato"}
data class SessionRecord(val timestamp: Long, val profileName: String, val completedSets: Int, val totalSets: Int, val exerciseNames: List<String> = emptyList(), val exerciseRpe: Map<String,Int> = emptyMap(), val workoutRpe:Int=0, val completedByExercise:Map<String,Int> = emptyMap(), val durationSec:Int=0, val structuredExercises:List<WorkoutExerciseState> = emptyList(), val skippedExercises:List<String> = emptyList(), val modifications:List<String> = emptyList(), val plannedWorkoutId:String?=null)
data class CalisthenicsSkill(val name:String, val category:String, val level:String)

internal val trainingLocations = listOf(
    TrainingLocation("builtin:gym","Palestra","🏋️",TrainingLocationKind.GYM,setOf(EquipmentId.AB_WHEEL,EquipmentId.BARBELL,EquipmentId.CABLE,EquipmentId.FOAM_ROLLER,EquipmentId.KETTLEBELL,EquipmentId.LEG_PRESS,EquipmentId.MACHINE,EquipmentId.DUMBBELL,EquipmentId.BENCH,EquipmentId.DIP_BARS,EquipmentId.PULL_UP_BAR,EquipmentId.BODYWEIGHT)),
    TrainingLocation("builtin:home","Casa","🏠",TrainingLocationKind.HOME,setOf(EquipmentId.AB_WHEEL,EquipmentId.BODYWEIGHT,EquipmentId.RESISTANCE_BAND,EquipmentId.FOAM_ROLLER,EquipmentId.PULL_UP_BAR)),
    TrainingLocation("builtin:outdoor","Parco","🌳",TrainingLocationKind.OUTDOOR,setOf(EquipmentId.BODYWEIGHT,EquipmentId.DIP_BARS,EquipmentId.PULL_UP_BAR))
)

enum class ProgressionDecision { ADVANCE, MAINTAIN, REGRESS, DELOAD }
data class SkillProgressionNode(val id:String,val skill:String,val label:String,val level:Int,val prerequisites:List<String> = emptyList())
data class SkillProgressSnapshot(val skill:String,val currentNodeId:String,val progressPercent:Int,val nextNodeId:String?,val decision:ProgressionDecision,val reason:String)

internal val skillProgressionNodes = listOf(
    SkillProgressionNode("front_tuck","Front lever","Tuck front lever",1),
    SkillProgressionNode("front_adv_tuck","Front lever","Advanced tuck front lever",2,listOf("front_tuck")),
    SkillProgressionNode("front_one_leg","Front lever","One-leg front lever",3,listOf("front_adv_tuck")),
    SkillProgressionNode("front_straddle","Front lever","Straddle front lever",4,listOf("front_one_leg")),
    SkillProgressionNode("front_full","Front lever","Full front lever",5,listOf("front_straddle")),
    SkillProgressionNode("planche_frog","Planche","Frog stand",1),
    SkillProgressionNode("planche_tuck","Planche","Tuck planche",2,listOf("planche_frog")),
    SkillProgressionNode("planche_adv_tuck","Planche","Advanced tuck planche",3,listOf("planche_tuck")),
    SkillProgressionNode("planche_straddle","Planche","Straddle planche",4,listOf("planche_adv_tuck")),
    SkillProgressionNode("planche_full","Planche","Full planche",5,listOf("planche_straddle")),
    SkillProgressionNode("pull_1","Trazioni","Australian pull-up",1),
    SkillProgressionNode("pull_2","Trazioni","Trazioni prone",2,listOf("pull_1")),
    SkillProgressionNode("pull_3","Trazioni","Trazioni chest-to-bar",3,listOf("pull_2")),
    SkillProgressionNode("pull_4","Trazioni","Trazioni zavorrate",4,listOf("pull_3")),
    SkillProgressionNode("dip_1","Dip","Dip assistite",1),
    SkillProgressionNode("dip_2","Dip","Dip parallele",2,listOf("dip_1")),
    SkillProgressionNode("dip_3","Dip","Dip profondi",3,listOf("dip_2")),
    SkillProgressionNode("dip_4","Dip","Dip zavorrate",4,listOf("dip_3")),
    SkillProgressionNode("muscle_1","Muscle-up","Trazioni esplosive",1),
    SkillProgressionNode("muscle_2","Muscle-up","Chest-to-bar esplosive",2,listOf("muscle_1")),
    SkillProgressionNode("muscle_3","Muscle-up","Muscle-up assistito",3,listOf("muscle_2")),
    SkillProgressionNode("muscle_4","Muscle-up","Muscle-up",4,listOf("muscle_3")),
    SkillProgressionNode("back_1","Back lever","Skin the cat",1),
    SkillProgressionNode("back_2","Back lever","Tuck back lever",2,listOf("back_1")),
    SkillProgressionNode("back_3","Back lever","Advanced tuck back lever",3,listOf("back_2")),
    SkillProgressionNode("back_4","Back lever","Straddle back lever",4,listOf("back_3")),
    SkillProgressionNode("back_5","Back lever","Full back lever",5,listOf("back_4")),
    SkillProgressionNode("hs_1","Handstand","Pike hold",1),
    SkillProgressionNode("hs_2","Handstand","Wall handstand",2,listOf("hs_1")),
    SkillProgressionNode("hs_3","Handstand","Handstand assistito",3,listOf("hs_2")),
    SkillProgressionNode("hs_4","Handstand","Handstand libero",4,listOf("hs_3")),
    SkillProgressionNode("hspu_1","Handstand push-up","Pike push-up",1),
    SkillProgressionNode("hspu_2","Handstand push-up","Elevated pike push-up",2,listOf("hspu_1")),
    SkillProgressionNode("hspu_3","Handstand push-up","Wall HSPU",3,listOf("hspu_2")),
    SkillProgressionNode("hspu_4","Handstand push-up","Handstand push-up",4,listOf("hspu_3")),
    SkillProgressionNode("lsit_1","L-sit","Tuck support",1),
    SkillProgressionNode("lsit_2","L-sit","One-leg L-sit",2,listOf("lsit_1")),
    SkillProgressionNode("lsit_3","L-sit","L-sit",3,listOf("lsit_2")),
    SkillProgressionNode("vsit_1","V-sit","L-sit",1),
    SkillProgressionNode("vsit_2","V-sit","Compression lift",2,listOf("vsit_1")),
    SkillProgressionNode("vsit_3","V-sit","Tuck V-sit",3,listOf("vsit_2")),
    SkillProgressionNode("vsit_4","V-sit","V-sit",4,listOf("vsit_3")),
    SkillProgressionNode("flag_1","Human flag","Side plank",1),
    SkillProgressionNode("flag_2","Human flag","Vertical flag",2,listOf("flag_1")),
    SkillProgressionNode("flag_3","Human flag","Tuck human flag",3,listOf("flag_2")),
    SkillProgressionNode("flag_4","Human flag","Straddle human flag",4,listOf("flag_3")),
    SkillProgressionNode("flag_5","Human flag","Human flag",5,listOf("flag_4")),
    SkillProgressionNode("dragon_1","Dragon flag","Reverse crunch",1),
    SkillProgressionNode("dragon_2","Dragon flag","Dragon flag negative",2,listOf("dragon_1")),
    SkillProgressionNode("dragon_3","Dragon flag","Tuck dragon flag",3,listOf("dragon_2")),
    SkillProgressionNode("dragon_4","Dragon flag","Dragon flag",4,listOf("dragon_3")),
    SkillProgressionNode("pistol_1","Pistol squat","Box pistol squat",1),
    SkillProgressionNode("pistol_2","Pistol squat","Assisted pistol squat",2,listOf("pistol_1")),
    SkillProgressionNode("pistol_3","Pistol squat","Pistol squat",3,listOf("pistol_2")),
    SkillProgressionNode("shrimp_1","Shrimp squat","Split squat",1),
    SkillProgressionNode("shrimp_2","Shrimp squat","Assisted shrimp squat",2,listOf("shrimp_1")),
    SkillProgressionNode("shrimp_3","Shrimp squat","Shrimp squat",3,listOf("shrimp_2")),
    SkillProgressionNode("oap_1","One arm pull-up","Archer pull-up",1),
    SkillProgressionNode("oap_2","One arm pull-up","Typewriter pull-up",2,listOf("oap_1")),
    SkillProgressionNode("oap_3","One arm pull-up","One-arm eccentric",3,listOf("oap_2")),
    SkillProgressionNode("oap_4","One arm pull-up","One arm pull-up",4,listOf("oap_3")),
    SkillProgressionNode("oapu_1","One arm push-up","Incline one-arm push-up",1),
    SkillProgressionNode("oapu_2","One arm push-up","Archer push-up",2,listOf("oapu_1")),
    SkillProgressionNode("oapu_3","One arm push-up","One-arm negative",3,listOf("oapu_2")),
    SkillProgressionNode("oapu_4","One arm push-up","One arm push-up",4,listOf("oapu_3"))
)

data class SkillPerformance(val successRate:Double,val averageRpe:Double,val recovery:Int,val consecutiveSuccessfulSessions:Int)

internal fun skillPerformance(skill:String,sessions:List<SessionRecord>):SkillPerformance {
    val relevantNames=exercises.filter{ex->ex.skills.any{it.equals(skill,true)}}.map{it.name}.toSet()
    val relevant=sessions.filter { session ->
        session.structuredExercises.any{it.exerciseName in relevantNames} || session.exerciseNames.any{it in relevantNames}
    }
    if(relevant.isEmpty()) return SkillPerformance(0.0,7.0,100,0)
    val recent=relevant.take(5)
    fun relevantAttempts(session:SessionRecord)=session.structuredExercises.filter{it.exerciseName in relevantNames}.distinctBy{it.exerciseName}
    val planned=recent.sumOf { session -> relevantAttempts(session).sumOf{it.sets.size}.takeIf{it>0} ?: relevantNames.sumOf{name->session.completedByExercise[name]?:0} }
    val completed=recent.sumOf { session -> relevantAttempts(session).sumOf{ex->ex.sets.count{it.completed}}.takeIf{it>0} ?: relevantNames.sumOf{name->session.completedByExercise[name]?:0} }
    val success=if(planned>0) completed.toDouble()/planned else 0.0
    val rpes=recent.flatMap{session->session.exerciseRpe.filterKeys{it in relevantNames}.values}.filter{it>0}
    val avgRpe=if(rpes.isEmpty()) 7.0 else rpes.average()
    val recoveryMap=recoveryScores(sessions)
    val groups=exercises.filter{it.name in relevantNames}.flatMap{listOf(it.group)+it.secondaryGroups}.filter{it in recoveryGroups}
    val recovery=groups.mapNotNull{recoveryMap[it]}.minOrNull() ?: 100
    var streak=0
    for(session in relevant){
        val attempts=relevantAttempts(session)
        val plannedSession=attempts.sumOf{it.sets.size}
        val doneSession=attempts.sumOf{ex->ex.sets.count{it.completed}}
        val rate=if(plannedSession>0) doneSession.toDouble()/plannedSession else 0.0
        if(rate>=0.90) streak++ else break
    }
    return SkillPerformance(success.coerceIn(0.0,1.0),avgRpe,recovery,streak)
}

internal fun progressionDecision(successRate:Double, averageRpe:Double, recovery:Int, consecutiveSuccessfulSessions:Int):ProgressionDecision = when {
    recovery < 35 || averageRpe >= 9.5 -> ProgressionDecision.DELOAD
    successRate < 0.60 -> ProgressionDecision.REGRESS
    successRate >= 0.90 && averageRpe <= 8.0 && recovery >= 70 && consecutiveSuccessfulSessions >= 2 -> ProgressionDecision.ADVANCE
    else -> ProgressionDecision.MAINTAIN
}

internal fun skillProgressSnapshot(skill:String,currentNodeId:String,successRate:Double,averageRpe:Double,recovery:Int,consecutiveSuccessfulSessions:Int):SkillProgressSnapshot {
    val chain=skillProgressionNodes.filter{it.skill==skill}.sortedBy{it.level}
    val current=chain.firstOrNull{it.id==currentNodeId} ?: chain.firstOrNull()
        ?: return SkillProgressSnapshot(skill,currentNodeId,0,null,ProgressionDecision.MAINTAIN,"Skill senza grafo configurato")
    val decision=progressionDecision(successRate,averageRpe,recovery,consecutiveSuccessfulSessions)
    val next=chain.firstOrNull{it.level==current.level+1}?.id
    val progress=((current.level.toDouble()/chain.maxOf{it.level})*100.0).toInt().coerceIn(0,100)
    val reason=when(decision){
        ProgressionDecision.ADVANCE -> "Prestazione stabile: progressione disponibile"
        ProgressionDecision.MAINTAIN -> "Consolida il livello corrente"
        ProgressionDecision.REGRESS -> "Riduci temporaneamente la difficoltà"
        ProgressionDecision.DELOAD -> "Recupero o RPE richiedono deload"
    }
    return SkillProgressSnapshot(skill,current.id,progress,next,decision,reason)
}

data class LoadRecommendation(val loadKg:Double?,val assistanceKg:Double?,val changePercent:Int,val reason:String)
data class AdaptiveWorkoutPlan(val intensityFactor:Double,val volumeFactor:Double,val decision:ProgressionDecision,val notes:List<String>)

internal fun recommendLoad(previousLoadKg:Double?,previousAssistanceKg:Double?,completionRate:Double,averageRpe:Double,recovery:Int):LoadRecommendation {
    if(previousAssistanceKg!=null){
        val delta=when{recovery<40||averageRpe>=9.5->5.0;completionRate>=0.95&&averageRpe<=8.0&&recovery>=70->-2.5;else->0.0}
        return LoadRecommendation(null,(previousAssistanceKg+delta).coerceAtLeast(0.0),if(delta<0)-5 else if(delta>0)10 else 0,if(delta<0)"Riduci assistenza" else if(delta>0)"Aumenta assistenza per recupero/tecnica" else "Mantieni assistenza")
    }
    if(previousLoadKg==null) return LoadRecommendation(null,null,0,"Calibrazione carico necessaria")
    val pct=when{recovery<40||averageRpe>=9.5->-10;completionRate>=0.95&&averageRpe<=8.0&&recovery>=70->5;completionRate<0.70->-5;else->0}
    val suggested=(previousLoadKg*(1.0+pct/100.0)*2.0).toInt()/2.0
    return LoadRecommendation(suggested,null,pct,when{pct>0->"Prestazione solida: incremento conservativo";pct<0->"Riduzione per qualità e recupero";else->"Mantieni il carico"})
}

internal fun loadRecommendationForExercise(ex:Exercise,sessions:List<SessionRecord>,recovery:Int):LoadRecommendation {
    val recent=sessions.sortedByDescending{it.timestamp}.asSequence().flatMap{it.structuredExercises.asSequence()}.firstOrNull{it.exerciseName==ex.name}
    val completed=recent?.sets?.filter{it.completed}.orEmpty()
    val previousLoad=completed.mapNotNull{it.actualLoadKg}.lastOrNull()
    val previousAssistance=completed.mapNotNull{it.actualAssistanceKg}.lastOrNull()
    val successful=completed.count { set ->
        (set.targetReps==null || (set.actualReps?:0)>=set.targetReps) &&
        (set.targetSeconds==null || (set.actualSeconds?:0)>=set.targetSeconds) &&
        (set.targetDistanceMeters==null || (set.actualDistanceMeters?:0.0)>=set.targetDistanceMeters)
    }
    val completionRate=if(recent==null||recent.sets.isEmpty())0.0 else successful.toDouble()/recent.sets.size
    // Missing effort is not evidence that the athlete has margin to progress.
    val rpe=recent?.rpe?.takeIf{it in 1..10}?.toDouble()?:8.5
    return recommendLoad(previousLoad,previousAssistance,completionRate,rpe,recovery)
}

internal fun adaptiveWorkoutPlan(successRate:Double,averageRpe:Double,recovery:Int,readinessScore:Int?):AdaptiveWorkoutPlan {
    val readiness=readinessScore?:recovery
    val decision=if(readinessScore!=null && readinessScore<45)ProgressionDecision.DELOAD else progressionDecision(successRate,averageRpe,minOf(recovery,readiness),if(successRate>=0.9)2 else 0)
    val intensity=when(decision){ProgressionDecision.ADVANCE->1.03;ProgressionDecision.MAINTAIN->1.0;ProgressionDecision.REGRESS->0.92;ProgressionDecision.DELOAD->0.85}
    val volume=when(decision){ProgressionDecision.ADVANCE->1.0;ProgressionDecision.MAINTAIN->1.0;ProgressionDecision.REGRESS->0.90;ProgressionDecision.DELOAD->0.70}
    val notes=buildList{add("Decisione: "+decision.name);if(readinessScore!=null)add("Readiness: $readinessScore/100");if(recovery<50)add("Recupero muscolare basso");if(averageRpe>=9.0)add("RPE recente elevato")}
    return AdaptiveWorkoutPlan(intensity,volume,decision,notes)
}

internal fun sequenceWorkout(items:List<Exercise>,pinnedNames:Set<String> = emptySet()):List<Exercise> {
    if(items.size<2) return items
    fun rolePriority(ex:Exercise)=when(ex.role){"Progressione"->0;"Prerequisito"->1;"Forza"->2;"Accessorio"->3;"Recupero"->6;else->4}
    fun setupCost(previous:Exercise?,candidate:Exercise):Int {
        if(previous==null) return 0
        var cost=0
        if(previous.equipment!=candidate.equipment) cost+=2
        if(previous.movementPattern==candidate.movementPattern && candidate.movementPattern.isNotBlank()) cost+=3
        if(previous.group==candidate.group) cost+=1
        return cost
    }
    val remaining=items.toMutableList(); val result=mutableListOf<Exercise>()
    while(remaining.isNotEmpty()){
        val originalIndex=items.indexOfFirst{it.name in pinnedNames && it.name !in result.map{r->r.name}}
        if(originalIndex==result.size){val pinned=items[originalIndex];remaining.removeAll{it.name==pinned.name};result.add(pinned);continue}
        val next=remaining.minWithOrNull(compareBy<Exercise>({rolePriority(it)},{setupCost(result.lastOrNull(),it)},{it.technicalComplexity},{items.indexOf(it)}))!!
        remaining.remove(next);result.add(next)
    }
    if(pinnedNames.isEmpty()) return result
    val out=result.toMutableList()
    items.forEachIndexed{i,original->if(original.name in pinnedNames){out.removeAll{it.name==original.name};out.add(i.coerceAtMost(out.size),original)}}
    return out
}

internal fun regenerateUnpinned(original:List<Exercise>,replacementPool:List<Exercise>,pinnedNames:Set<String>):List<Exercise> {
    val used=original.filter{it.name in pinnedNames}.map{it.name}.toMutableSet()
    val pool=(replacementPool+original).distinctBy{it.name}
    return original.map { ex ->
        if(ex.name in pinnedNames) ex else {
            val replacement=pool.firstOrNull{it.name !in used} ?: ex
            used.add(replacement.name)
            replacement
        }
    }
}

private val calisthenicsSkills = listOf(
 CalisthenicsSkill("Trazioni","Pull","Principiante"),CalisthenicsSkill("Dip","Push","Principiante"),CalisthenicsSkill("L-sit","Core","Intermedio"),CalisthenicsSkill("Pistol squat","Gambe","Intermedio"),CalisthenicsSkill("Handstand","Verticale","Intermedio"),CalisthenicsSkill("Muscle-up","Pull/Push","Avanzato"),CalisthenicsSkill("Front lever","Pull/Core","Avanzato"),CalisthenicsSkill("Back lever","Pull/Core","Avanzato"),CalisthenicsSkill("Planche","Push/Core","Avanzato"),CalisthenicsSkill("Handstand push-up","Verticale/Push","Avanzato"),CalisthenicsSkill("Human flag","Pull/Push/Core","Avanzato"),CalisthenicsSkill("Dragon flag","Core","Avanzato"),CalisthenicsSkill("V-sit","Core","Avanzato"),CalisthenicsSkill("One arm pull-up","Pull","Avanzato"),CalisthenicsSkill("One arm push-up","Push","Avanzato"),CalisthenicsSkill("Shrimp squat","Gambe","Intermedio")
)
private val coreExerciseCatalog = listOf(
    Exercise("Trazioni prone", "Dorso", "Sbarra", "Intermedio", listOf("Muscle-up", "Front lever"), "Prerequisito", category=ExerciseCategory.PULL, resistanceMode=ResistanceMode.BODYWEIGHT),
    Exercise("Trazioni esplosive", "Dorso", "Sbarra", "Avanzato", listOf("Muscle-up"), "Progressione"),
    Exercise("Australian pull-up", "Dorso", "Sbarra", "Principiante", listOf("Trazioni", "Front lever"), "Prerequisito"),
    Exercise("Lat machine", "Dorso", "Macchina", "Principiante", listOf("Trazioni"), "Accessorio"),
    Exercise("Panca piana", "Petto", "Bilanciere", "Intermedio", emptyList(), "Forza", resistanceMode=ResistanceMode.EXTERNAL_LOAD),
    Exercise("Push-up", "Petto", "Corpo libero", "Principiante", listOf("Planche"), "Prerequisito"),
    Exercise("Pseudo planche push-up", "Petto / Spalle", "Corpo libero", "Avanzato", listOf("Planche"), "Progressione"),
    Exercise("Dip alle parallele", "Petto / Tricipiti", "Parallele", "Intermedio", listOf("Muscle-up"), "Prerequisito"),
    Exercise("Squat", "Gambe", "Bilanciere", "Intermedio", emptyList(), "Forza"),
    Exercise("Bulgarian split squat", "Gambe", "Manubri / corpo libero", "Intermedio", listOf("Pistol squat"), "Accessorio", category=ExerciseCategory.LEGS, laterality=Laterality.UNILATERAL, resistanceMode=ResistanceMode.EXTERNAL_LOAD),
    Exercise("Pistol squat assistito", "Gambe", "Corpo libero", "Intermedio", listOf("Pistol squat"), "Progressione", category=ExerciseCategory.SKILL, laterality=Laterality.UNILATERAL, resistanceMode=ResistanceMode.ASSISTED),
    Exercise("Pike push-up", "Spalle", "Corpo libero", "Intermedio", listOf("Handstand push-up"), "Prerequisito"),
    Exercise("Wall handstand", "Spalle / Core", "Corpo libero", "Intermedio", listOf("Handstand", "Handstand push-up"), "Progressione", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Hollow body hold", "Core", "Corpo libero", "Principiante", listOf("Front lever", "Planche"), "Prerequisito", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Tuck front lever", "Dorso / Core", "Sbarra", "Intermedio", listOf("Front lever"), "Progressione", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Chin-up", "Dorso / Bicipiti", "Sbarra", "Intermedio", listOf("Muscle-up"), "Prerequisito"),
    Exercise("Scapular pull-up", "Dorso", "Sbarra", "Principiante", listOf("Trazioni", "Front lever"), "Accessorio"),
    Exercise("Rematore bilanciere", "Dorso", "Bilanciere", "Intermedio", emptyList(), "Forza"),
    Exercise("Rematore manubrio", "Dorso", "Manubri", "Principiante", emptyList(), "Accessorio"),
    Exercise("Pulley basso", "Dorso", "Cavi", "Principiante", emptyList(), "Accessorio"),
    Exercise("Military press", "Spalle", "Bilanciere", "Intermedio", listOf("Handstand push-up"), "Forza"),
    Exercise("Shoulder press manubri", "Spalle", "Manubri", "Principiante", listOf("Handstand push-up"), "Forza"),
    Exercise("Alzate laterali", "Spalle", "Manubri", "Principiante", emptyList(), "Accessorio"),
    Exercise("Chest press", "Petto", "Macchine", "Principiante", emptyList(), "Forza"),
    Exercise("Croci ai cavi", "Petto", "Cavi", "Principiante", emptyList(), "Accessorio"),
    Exercise("Diamond push-up", "Petto / Tricipiti", "Corpo libero", "Intermedio", listOf("Planche"), "Accessorio"),
    Exercise("Affondi", "Gambe", "Manubri / corpo libero", "Principiante", listOf("Pistol squat"), "Accessorio"),
    Exercise("Goblet squat", "Gambe", "Kettlebell", "Principiante", listOf("Pistol squat"), "Forza"),
    Exercise("Leg press", "Gambe", "Leg press", "Principiante", emptyList(), "Forza"),
    Exercise("Romanian deadlift", "Gambe / Dorso", "Bilanciere", "Intermedio", emptyList(), "Forza"),
    Exercise("Calf raise", "Gambe", "Corpo libero", "Principiante", emptyList(), "Accessorio"),
    Exercise("Plank", "Core", "Corpo libero", "Principiante", listOf("Planche"), "Prerequisito", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Side plank", "Core", "Corpo libero", "Principiante", emptyList(), "Accessorio", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Hanging knee raise", "Core", "Sbarra", "Intermedio", listOf("Front lever"), "Accessorio"),
    Exercise("L-sit tuck", "Core", "Parallele", "Intermedio", listOf("L-sit"), "Progressione", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Band pull-apart", "Dorso / Spalle", "Elastici", "Principiante", emptyList(), "Accessorio"),
    Exercise("Face pull elastico", "Dorso / Spalle", "Elastici", "Principiante", emptyList(), "Accessorio"),
    Exercise("Curl manubri", "Bicipiti", "Manubri", "Principiante", emptyList(), "Accessorio"),
    Exercise("Pushdown cavo", "Tricipiti", "Cavi", "Principiante", emptyList(), "Accessorio"),
    Exercise("Stacco da terra", "Gambe / Dorso", "Bilanciere", "Intermedio", emptyList(), "Forza"),
    Exercise("Hip thrust", "Gambe", "Bilanciere / Panca", "Principiante", emptyList(), "Forza"),
    Exercise("Leg curl", "Gambe", "Macchine", "Principiante", emptyList(), "Accessorio"),
    Exercise("Leg extension", "Gambe", "Macchine", "Principiante", emptyList(), "Accessorio"),
    Exercise("Hack squat", "Gambe", "Macchine", "Intermedio", emptyList(), "Forza"),
    Exercise("Step-up", "Gambe", "Manubri / Panca", "Principiante", listOf("Pistol squat"), "Accessorio"),
    Exercise("Panca inclinata manubri", "Petto", "Manubri / Panca", "Intermedio", emptyList(), "Forza"),
    Exercise("Chest fly manubri", "Petto", "Manubri / Panca", "Principiante", emptyList(), "Accessorio"),
    Exercise("Dips panca", "Tricipiti", "Panca", "Principiante", emptyList(), "Accessorio"),
    Exercise("French press", "Tricipiti", "Manubri", "Intermedio", emptyList(), "Accessorio"),
    Exercise("Curl martello", "Bicipiti", "Manubri", "Principiante", emptyList(), "Accessorio"),
    Exercise("Curl bilanciere", "Bicipiti", "Bilanciere", "Principiante", emptyList(), "Accessorio"),
    Exercise("Face pull cavo", "Dorso / Spalle", "Cavi", "Principiante", emptyList(), "Accessorio"),
    Exercise("Reverse fly", "Dorso / Spalle", "Manubri", "Principiante", emptyList(), "Accessorio"),
    Exercise("Arnold press", "Spalle", "Manubri", "Intermedio", emptyList(), "Forza"),
    Exercise("Farmer walk", "Core / Spalle", "Manubri", "Principiante", emptyList(), "Accessorio", measure=ExerciseMeasure.DISTANCE, targetValue=30.0),
    Exercise("Ab wheel rollout", "Core", "Ab Wheel", "Intermedio", listOf("Front lever"), "Accessorio", category=ExerciseCategory.CORE, movementPattern="anti-extension", resistanceMode=ResistanceMode.BODYWEIGHT),
    Exercise("Dragon flag negativa", "Core", "Panca", "Avanzato", listOf("Dragon flag"), "Progressione"),
    Exercise("Toes to bar", "Core", "Sbarra", "Avanzato", listOf("Front lever"), "Progressione"),
    Exercise("Muscle-up negativo", "Dorso / Petto / Tricipiti", "Sbarra", "Avanzato", listOf("Muscle-up"), "Progressione"),
    Exercise("Jumping muscle-up", "Dorso / Petto", "Sbarra", "Intermedio", listOf("Muscle-up"), "Progressione"),
    Exercise("Front lever raises tuck", "Dorso / Core", "Sbarra", "Avanzato", listOf("Front lever"), "Progressione"),
    Exercise("Advanced tuck front lever", "Dorso / Core", "Sbarra", "Avanzato", listOf("Front lever"), "Progressione", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Planche lean", "Spalle / Core", "Corpo libero", "Intermedio", listOf("Planche"), "Progressione", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Tuck planche", "Spalle / Core", "Parallele", "Avanzato", listOf("Planche"), "Progressione", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Handstand shoulder taps", "Spalle / Core", "Corpo libero", "Avanzato", listOf("Handstand", "Handstand push-up"), "Progressione"),
    Exercise("Wall HSPU negativa", "Spalle / Tricipiti", "Corpo libero", "Avanzato", listOf("Handstand push-up"), "Progressione"),
    Exercise("L-sit", "Core", "Parallele", "Avanzato", listOf("L-sit"), "Progressione", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Ring row", "Dorso", "Anelli", "Principiante", listOf("Front lever"), "Prerequisito"),
    Exercise("Ring dip", "Petto / Tricipiti", "Anelli", "Avanzato", listOf("Muscle-up"), "Progressione"),
    Exercise("TRX row", "Dorso", "TRX", "Principiante", emptyList(), "Accessorio"),
    Exercise("TRX push-up", "Petto / Core", "TRX", "Intermedio", emptyList(), "Accessorio"),
    Exercise("Kettlebell swing", "Gambe / Core", "Kettlebell", "Intermedio", emptyList(), "Forza"),
    Exercise("Pullover manubrio", "Dorso / Petto", "Manubri / Panca", "Principiante", emptyList(), "Accessorio"),
    Exercise("Rematore T-bar", "Dorso", "Macchine", "Intermedio", emptyList(), "Forza"),
    Exercise("High row macchina", "Dorso", "Macchine", "Principiante", emptyList(), "Accessorio"),
    Exercise("Straight arm pulldown", "Dorso", "Cavi", "Principiante", listOf("Front lever"), "Accessorio"),
    Exercise("Shrug manubri", "Spalle", "Manubri", "Principiante", emptyList(), "Accessorio"),
    Exercise("Alzate frontali", "Spalle", "Manubri", "Principiante", emptyList(), "Accessorio"),
    Exercise("Landmine press", "Spalle / Petto", "Bilanciere", "Intermedio", emptyList(), "Forza"),
    Exercise("Panca declinata", "Petto", "Bilanciere / Panca", "Intermedio", emptyList(), "Forza"),
    Exercise("Cable press", "Petto", "Cavi", "Principiante", emptyList(), "Accessorio"),
    Exercise("Squeeze press", "Petto", "Manubri / Panca", "Intermedio", emptyList(), "Accessorio"),
    Exercise("Push-up inclinato", "Petto", "Corpo libero", "Principiante", listOf("One arm push-up"), "Prerequisito"),
    Exercise("Push-up declinato", "Petto / Spalle", "Corpo libero", "Intermedio", listOf("Handstand push-up"), "Prerequisito"),
    Exercise("Archer push-up", "Petto / Tricipiti", "Corpo libero", "Avanzato", listOf("One arm push-up"), "Progressione"),
    Exercise("One arm push-up assistito", "Petto / Tricipiti / Core", "Corpo libero", "Avanzato", listOf("One arm push-up"), "Progressione"),
    Exercise("Close grip bench press", "Petto / Tricipiti", "Bilanciere / Panca", "Intermedio", emptyList(), "Forza"),
    Exercise("Overhead triceps extension", "Tricipiti", "Cavi", "Principiante", emptyList(), "Accessorio"),
    Exercise("Skull crusher", "Tricipiti", "Bilanciere / Panca", "Intermedio", emptyList(), "Accessorio"),
    Exercise("Preacher curl", "Bicipiti", "Bilanciere", "Principiante", emptyList(), "Accessorio"),
    Exercise("Incline dumbbell curl", "Bicipiti", "Manubri / Panca", "Intermedio", emptyList(), "Accessorio"),
    Exercise("Cable curl", "Bicipiti", "Cavi", "Principiante", emptyList(), "Accessorio"),
    Exercise("Reverse curl", "Bicipiti", "Bilanciere", "Intermedio", emptyList(), "Accessorio"),
    Exercise("Front squat", "Gambe / Core", "Bilanciere", "Avanzato", emptyList(), "Forza"),
    Exercise("Sumo deadlift", "Gambe / Dorso", "Bilanciere", "Intermedio", emptyList(), "Forza"),
    Exercise("Walking lunge", "Gambe", "Manubri", "Intermedio", emptyList(), "Accessorio"),
    Exercise("Reverse lunge", "Gambe", "Manubri / corpo libero", "Principiante", emptyList(), "Accessorio"),
    Exercise("Nordic curl assistito", "Gambe", "Corpo libero", "Avanzato", emptyList(), "Accessorio"),
    Exercise("Single leg Romanian deadlift", "Gambe / Core", "Manubri", "Intermedio", listOf("Pistol squat"), "Accessorio"),
    Exercise("Glute bridge", "Gambe", "Corpo libero", "Principiante", emptyList(), "Accessorio"),
    Exercise("Seated calf raise", "Gambe", "Macchine", "Principiante", emptyList(), "Accessorio"),
    Exercise("Cable crunch", "Core", "Cavi", "Principiante", emptyList(), "Accessorio"),
    Exercise("Dead bug", "Core", "Corpo libero", "Principiante", emptyList(), "Prerequisito"),
    Exercise("Bird dog", "Core", "Corpo libero", "Principiante", emptyList(), "Prerequisito"),
    Exercise("Reverse crunch", "Core", "Corpo libero", "Principiante", listOf("Dragon flag"), "Prerequisito"),
    Exercise("Hanging leg raise", "Core", "Sbarra", "Avanzato", listOf("L-sit"), "Progressione"),
    Exercise("V-sit tuck", "Core", "Parallele", "Avanzato", listOf("V-sit"), "Progressione"),
    Exercise("Compression lift", "Core", "Corpo libero", "Intermedio", listOf("L-sit"), "Progressione"),
    Exercise("Back lever tuck", "Dorso / Core", "Sbarra", "Intermedio", listOf("Back lever"), "Progressione", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Back lever advanced tuck", "Dorso / Core", "Sbarra", "Avanzato", listOf("Back lever"), "Progressione", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("German hang", "Dorso / Spalle", "Sbarra", "Intermedio", listOf("Back lever"), "Prerequisito", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Skin the cat", "Dorso / Spalle / Core", "Anelli", "Intermedio", listOf("Back lever"), "Progressione"),
    Exercise("Human flag tuck", "Dorso / Spalle / Core", "Sbarra", "Avanzato", listOf("Human flag"), "Progressione", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Side plank star", "Core / Spalle", "Corpo libero", "Intermedio", listOf("Human flag"), "Prerequisito", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Archer pull-up", "Dorso / Bicipiti", "Sbarra", "Avanzato", listOf("One arm pull-up"), "Progressione"),
    Exercise("Typewriter pull-up", "Dorso / Bicipiti", "Sbarra", "Avanzato", listOf("One arm pull-up"), "Progressione"),
    Exercise("Assisted one arm pull-up", "Dorso / Bicipiti", "Sbarra / Elastici", "Avanzato", listOf("One arm pull-up"), "Progressione"),
    Exercise("Shrimp squat assistito", "Gambe", "Corpo libero", "Intermedio", listOf("Shrimp squat"), "Progressione"),
    Exercise("Shrimp squat", "Gambe / Core", "Corpo libero", "Avanzato", listOf("Shrimp squat"), "Progressione"),
    Exercise("Ring support hold", "Spalle / Tricipiti / Core", "Anelli", "Intermedio", listOf("Dip"), "Prerequisito", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Ring muscle-up transition", "Dorso / Petto / Tricipiti", "Anelli", "Avanzato", listOf("Muscle-up"), "Progressione"),
    Exercise("Band assisted muscle-up", "Dorso / Petto / Tricipiti", "Sbarra / Elastici", "Intermedio", listOf("Muscle-up"), "Progressione"),
    Exercise("Chest to bar pull-up", "Dorso / Bicipiti", "Sbarra", "Intermedio", listOf("Muscle-up"), "Progressione"),
    Exercise("Straight bar dip", "Petto / Tricipiti", "Sbarra", "Intermedio", listOf("Muscle-up"), "Progressione"),
    Exercise("Frog stand", "Spalle / Core", "Corpo libero", "Principiante", listOf("Planche"), "Prerequisito", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Crow pose", "Spalle / Core", "Corpo libero", "Principiante", listOf("Handstand"), "Prerequisito", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Wall walk", "Spalle / Core", "Corpo libero", "Intermedio", listOf("Handstand"), "Progressione"),
    Exercise("Handstand kick-up", "Spalle / Core", "Corpo libero", "Intermedio", listOf("Handstand"), "Progressione"),
    Exercise("Freestanding handstand hold", "Spalle / Core", "Corpo libero", "Avanzato", listOf("Handstand"), "Progressione", measure=ExerciseMeasure.SECONDS, targetValue=30.0),
    Exercise("Elevated pike push-up", "Spalle / Tricipiti", "Panca", "Intermedio", listOf("Handstand push-up"), "Progressione"),
    Exercise("Wall handstand push-up", "Spalle / Tricipiti", "Corpo libero", "Avanzato", listOf("Handstand push-up"), "Progressione"),
    Exercise("Planche lean push-up", "Petto / Spalle / Core", "Corpo libero", "Avanzato", listOf("Planche"), "Progressione"),
    Exercise("Front lever negative", "Dorso / Core", "Sbarra", "Avanzato", listOf("Front lever"), "Progressione"),
    Exercise("Kettlebell clean", "Gambe / Spalle / Core", "Kettlebell", "Avanzato", emptyList(), "Forza"),
    Exercise("Kettlebell press", "Spalle / Core", "Kettlebell", "Intermedio", emptyList(), "Forza"),
    Exercise("TRX face pull", "Dorso / Spalle", "TRX", "Principiante", emptyList(), "Accessorio"),
    Exercise("TRX fallout", "Core", "TRX", "Intermedio", listOf("Front lever"), "Accessorio"),
    Exercise("Band row", "Dorso", "Elastici", "Principiante", emptyList(), "Accessorio"),
    Exercise("Band chest press", "Petto", "Elastici", "Principiante", emptyList(), "Accessorio"),
    Exercise("Band lateral raise", "Spalle", "Elastici", "Principiante", emptyList(), "Accessorio"),
    Exercise("Band curl", "Bicipiti", "Elastici", "Principiante", emptyList(), "Accessorio"),
    Exercise("Band triceps extension", "Tricipiti", "Elastici", "Principiante", emptyList(), "Accessorio"),
    Exercise("Foam roller quadricipiti", "Gambe", "Foam Roller", "Principiante", role="Recupero", category=ExerciseCategory.MOBILITY, movementPattern="recovery", measure=ExerciseMeasure.DURATION, targetSets=1, targetValue=60.0),
    Exercise("Foam roller dorsale", "Dorso", "Foam Roller", "Principiante", role="Recupero", category=ExerciseCategory.MOBILITY, movementPattern="recovery", measure=ExerciseMeasure.DURATION, targetSets=1, targetValue=60.0),
    Exercise("Muscle-up", "Dorso / Core", "Sbarra", "Avanzato", listOf("Muscle-up"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.REPS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=6.0),
    Exercise("Straddle Back Lever", "Dorso / Core", "Sbarra", "Avanzato", listOf("Back lever"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.SECONDS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=15.0),
    Exercise("Back Lever", "Dorso / Core", "Sbarra", "Avanzato", listOf("Back lever"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.SECONDS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=15.0),
    Exercise("Vertical Flag Hold", "Dorso / Core", "Sbarra", "Intermedio", listOf("Human flag"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.SECONDS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=15.0),
    Exercise("Straddle Human Flag", "Dorso / Core", "Sbarra", "Avanzato", listOf("Human flag"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.SECONDS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=15.0),
    Exercise("Human Flag", "Dorso / Core", "Sbarra", "Avanzato", listOf("Human flag"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.SECONDS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=15.0),
    Exercise("V-sit", "Core", "Parallele", "Avanzato", listOf("L-sit", "V-sit"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.SECONDS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=15.0),
    Exercise("Box Pistol Squat", "Gambe / Core", "Panca", "Intermedio", listOf("Pistol squat"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.REPS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=6.0),
    Exercise("Pistol Squat", "Gambe / Core", "Corpo libero", "Avanzato", listOf("Pistol squat"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.REPS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=6.0),
    Exercise("One-leg front lever", "Dorso / Core", "Sbarra", "Avanzato", listOf("Front lever"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.SECONDS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=15.0),
    Exercise("Straddle front lever", "Dorso / Core", "Sbarra", "Avanzato", listOf("Front lever"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.SECONDS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=15.0),
    Exercise("Full front lever", "Dorso / Core", "Sbarra", "Avanzato", listOf("Front lever"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.SECONDS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=15.0),
    Exercise("Advanced tuck planche", "Spalle / Core", "Corpo libero", "Avanzato", listOf("Planche"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.SECONDS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=15.0),
    Exercise("Straddle planche", "Spalle / Core", "Corpo libero", "Avanzato", listOf("Planche"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.SECONDS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=15.0),
    Exercise("Full planche", "Spalle / Core", "Corpo libero", "Avanzato", listOf("Planche"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.SECONDS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=15.0),
    Exercise("Trazioni zavorrate", "Dorso / Core", "Sbarra", "Avanzato", listOf("Trazioni"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.REPS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=6.0),
    Exercise("Dip assistite", "Spalle / Core", "Parallele / Elastici", "Intermedio", listOf("Dip"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.REPS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=6.0),
    Exercise("Dip profondi", "Spalle / Core", "Parallele", "Avanzato", listOf("Dip"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.REPS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=6.0),
    Exercise("Dip zavorrate", "Spalle / Core", "Parallele", "Avanzato", listOf("Dip"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.REPS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=6.0),
    Exercise("Handstand push-up", "Spalle / Core", "Corpo libero", "Avanzato", listOf("Handstand push-up"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.REPS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=6.0),
    Exercise("Tuck dragon flag", "Core", "Panca", "Avanzato", listOf("Dragon flag"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.REPS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=6.0),
    Exercise("Dragon flag", "Core", "Panca", "Avanzato", listOf("Dragon flag"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.REPS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=6.0),
    Exercise("Split squat", "Gambe / Core", "Corpo libero", "Intermedio", listOf("Shrimp squat"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.REPS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=6.0),
    Exercise("One-arm eccentric", "Dorso / Core", "Sbarra", "Avanzato", listOf("One arm pull-up"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.REPS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=6.0),
    Exercise("One arm pull-up", "Dorso / Core", "Sbarra", "Avanzato", listOf("One arm pull-up"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.REPS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=6.0),
    Exercise("Incline one-arm push-up", "Spalle / Core", "Corpo libero", "Intermedio", listOf("One arm push-up"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.REPS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=6.0),
    Exercise("One-arm negative", "Spalle / Core", "Corpo libero", "Avanzato", listOf("One arm push-up"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.REPS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=6.0),
    Exercise("One arm push-up", "Spalle / Core", "Corpo libero", "Avanzato", listOf("One arm push-up"), "Progressione", category=ExerciseCategory.SKILL, measure=ExerciseMeasure.REPS, resistanceMode=ResistanceMode.BODYWEIGHT, targetValue=6.0),
    Exercise("Mobilità caviglia al muro", "Gambe", "Corpo libero", "Principiante", role="Recupero", category=ExerciseCategory.MOBILITY, movementPattern="ankle-mobility", measure=ExerciseMeasure.DURATION, targetSets=1, targetValue=45.0),
    Exercise("Rotazioni toraciche quadrupedia", "Dorso", "Corpo libero", "Principiante", role="Recupero", category=ExerciseCategory.MOBILITY, movementPattern="thoracic-mobility", measure=ExerciseMeasure.DURATION, targetSets=1, targetValue=45.0),
    Exercise("Stretch flessori anca", "Gambe", "Corpo libero", "Principiante", role="Recupero", category=ExerciseCategory.MOBILITY, movementPattern="hip-mobility", measure=ExerciseMeasure.DURATION, targetSets=1, targetValue=40.0),
    Exercise("Stretch dorsali in quadrupedia", "Dorso", "Corpo libero", "Principiante", role="Recupero", category=ExerciseCategory.MOBILITY, movementPattern="shoulder-mobility", measure=ExerciseMeasure.DURATION, targetSets=1, targetValue=40.0),
    Exercise("Mobilità polsi", "Avambracci", "Corpo libero", "Principiante", role="Recupero", category=ExerciseCategory.MOBILITY, movementPattern="wrist-mobility", measure=ExerciseMeasure.DURATION, targetSets=1, targetValue=45.0),
    Exercise("Respirazione diaframmatica", "Core", "Corpo libero", "Principiante", role="Recupero", category=ExerciseCategory.MOBILITY, movementPattern="recovery", measure=ExerciseMeasure.DURATION, targetSets=1, targetValue=120.0),
    Exercise("Stretch pettorale al muro", "Petto", "Corpo libero", "Principiante", role="Recupero", category=ExerciseCategory.MOBILITY, movementPattern="shoulder-mobility", measure=ExerciseMeasure.DURATION, targetSets=1, targetValue=40.0),
    Exercise("Ab Wheel rollout in ginocchio", "Core", "Ab Wheel", "Intermedio", role="Forza", category=ExerciseCategory.CORE, movementPattern="anti-extension", resistanceMode=ResistanceMode.BODYWEIGHT, targetSets=3, targetValue=8.0),
    Exercise("Ab Wheel rollout da piedi", "Core", "Ab Wheel", "Avanzato", role="Progressione", category=ExerciseCategory.CORE, movementPattern="anti-extension", resistanceMode=ResistanceMode.BODYWEIGHT, targetSets=3, targetValue=5.0, technicalComplexity=4),
    Exercise("Foam Roller torace", "Mobilità", "Foam Roller", "Principiante", role="Recupero", category=ExerciseCategory.MOBILITY, movementPattern="thoracic-mobility", resistanceMode=ResistanceMode.BODYWEIGHT, measure=ExerciseMeasure.DURATION, targetSets=1, targetValue=60.0),
    Exercise("Foam Roller polpacci", "Polpacci", "Foam Roller", "Principiante", role="Recupero", category=ExerciseCategory.MOBILITY, movementPattern="recovery", resistanceMode=ResistanceMode.BODYWEIGHT, measure=ExerciseMeasure.DURATION, targetSets=1, targetValue=60.0),
    Exercise("Dead bug alternato", "Core", "Corpo libero", "Principiante", role="Accessorio", category=ExerciseCategory.CORE, movementPattern="anti-extension", resistanceMode=ResistanceMode.BODYWEIGHT, targetSets=3, targetValue=10.0),
    Exercise("Bird dog controllato", "Core", "Corpo libero", "Principiante", role="Accessorio", category=ExerciseCategory.CORE, movementPattern="anti-rotation", resistanceMode=ResistanceMode.BODYWEIGHT, targetSets=3, targetValue=10.0),
    Exercise("Pallof press", "Core", "Cavi", "Intermedio", role="Accessorio", category=ExerciseCategory.CORE, movementPattern="anti-rotation", resistanceMode=ResistanceMode.EXTERNAL_LOAD, targetSets=3, targetValue=12.0),
    Exercise("Suitcase carry", "Core", "Kettlebell", "Intermedio", role="Accessorio", category=ExerciseCategory.CORE, movementPattern="loaded-carry", resistanceMode=ResistanceMode.EXTERNAL_LOAD, measure=ExerciseMeasure.DISTANCE, targetSets=3, targetValue=30.0),
    Exercise("Farmer carry", "Avambracci / Core", "Manubri", "Intermedio", role="Accessorio", movementPattern="loaded-carry", resistanceMode=ResistanceMode.EXTERNAL_LOAD, measure=ExerciseMeasure.DISTANCE, targetSets=3, targetValue=40.0),
    Exercise("Bear crawl", "Core / Spalle", "Corpo libero", "Intermedio", role="Conditioning", category=ExerciseCategory.CORE, movementPattern="locomotion", resistanceMode=ResistanceMode.BODYWEIGHT, measure=ExerciseMeasure.DISTANCE, targetSets=3, targetValue=20.0),
    Exercise("Mountain climber", "Core / Cardio", "Corpo libero", "Principiante", role="Conditioning", category=ExerciseCategory.CARDIO, movementPattern="conditioning", resistanceMode=ResistanceMode.BODYWEIGHT, measure=ExerciseMeasure.DURATION, targetSets=4, targetValue=30.0),
    Exercise("Burpee", "Total body", "Corpo libero", "Intermedio", role="Conditioning", category=ExerciseCategory.CARDIO, movementPattern="conditioning", resistanceMode=ResistanceMode.BODYWEIGHT, targetSets=4, targetValue=10.0),
    Exercise("Kettlebell snatch", "Total body", "Kettlebell", "Avanzato", role="Conditioning", movementPattern="hinge", resistanceMode=ResistanceMode.EXTERNAL_LOAD, targetSets=4, targetValue=6.0, technicalComplexity=4),
    Exercise("Turkish get-up", "Total body / Core", "Kettlebell", "Avanzato", role="Forza", category=ExerciseCategory.CORE, movementPattern="stability", resistanceMode=ResistanceMode.EXTERNAL_LOAD, targetSets=3, targetValue=3.0, technicalComplexity=4),
    Exercise("Monster walk elastico", "Gambe / Glutei", "Elastici", "Principiante", role="Accessorio", category=ExerciseCategory.LEGS, movementPattern="hip-stability", resistanceMode=ResistanceMode.EXTERNAL_LOAD, targetSets=3, targetValue=12.0),
    Exercise("Copenhagen plank", "Core / Adduttori", "Corpo libero", "Intermedio", role="Accessorio", category=ExerciseCategory.CORE, movementPattern="anti-lateral-flexion", resistanceMode=ResistanceMode.BODYWEIGHT, measure=ExerciseMeasure.DURATION, targetSets=3, targetValue=25.0),
    Exercise("Hollow body rock", "Core", "Corpo libero", "Intermedio", role="Accessorio", category=ExerciseCategory.CORE, movementPattern="anti-extension", resistanceMode=ResistanceMode.BODYWEIGHT, targetSets=3, targetValue=12.0),
    Exercise("Jefferson curl leggero", "Mobilità", "Kettlebell", "Intermedio", role="Recupero", category=ExerciseCategory.MOBILITY, movementPattern="spinal-mobility", resistanceMode=ResistanceMode.EXTERNAL_LOAD, targetSets=2, targetValue=8.0),
    Exercise("90/90 anche", "Mobilità", "Corpo libero", "Principiante", role="Recupero", category=ExerciseCategory.MOBILITY, movementPattern="hip-mobility", resistanceMode=ResistanceMode.BODYWEIGHT, measure=ExerciseMeasure.DURATION, targetSets=1, targetValue=60.0),
    Exercise("World greatest stretch", "Mobilità", "Corpo libero", "Principiante", role="Recupero", category=ExerciseCategory.MOBILITY, movementPattern="full-body-mobility", resistanceMode=ResistanceMode.BODYWEIGHT, measure=ExerciseMeasure.DURATION, targetSets=1, targetValue=60.0),
    Exercise("Scapular wall slide", "Spalle / Dorso", "Corpo libero", "Principiante", role="Recupero", category=ExerciseCategory.MOBILITY, movementPattern="shoulder-mobility", resistanceMode=ResistanceMode.BODYWEIGHT, targetSets=2, targetValue=12.0),
    Exercise("Foam Roller dorsali", "Dorso", "Foam Roller", "Principiante", role="Recupero", category=ExerciseCategory.MOBILITY, movementPattern="recovery", resistanceMode=ResistanceMode.BODYWEIGHT, measure=ExerciseMeasure.DURATION, targetSets=1, targetValue=60.0),
    Exercise("Foam Roller glutei", "Glutei", "Foam Roller", "Principiante", role="Recupero", category=ExerciseCategory.MOBILITY, movementPattern="recovery", resistanceMode=ResistanceMode.BODYWEIGHT, measure=ExerciseMeasure.DURATION, targetSets=1, targetValue=60.0),
    Exercise("Ab Wheel rollout diagonale", "Core", "Ab Wheel", "Avanzato", role="Progressione", category=ExerciseCategory.CORE, movementPattern="anti-extension", resistanceMode=ResistanceMode.BODYWEIGHT, targetSets=3, targetValue=6.0, technicalComplexity=4),
    Exercise("Corsa continua", "Cardio", "Corpo libero", "Principiante", role="Base", category=ExerciseCategory.CARDIO, measure=ExerciseMeasure.DURATION, resistanceMode=ResistanceMode.BODYWEIGHT, targetSets=1, targetValue=600.0),
    Exercise("Corsa distanza", "Cardio", "Corpo libero", "Principiante", role="Base", category=ExerciseCategory.CARDIO, measure=ExerciseMeasure.DISTANCE, resistanceMode=ResistanceMode.BODYWEIGHT, targetSets=1, targetValue=1000.0),
    Exercise("Corsa intervallata 60/60", "Cardio", "Corpo libero", "Intermedio", role="Base", category=ExerciseCategory.CARDIO, measure=ExerciseMeasure.INTERVALS, resistanceMode=ResistanceMode.BODYWEIGHT, targetSets=5, targetValue=60.0, intervalRestSec=60)

).map(::withExerciseMetadata)

private val legacyExercises: List<Exercise> = (coreExerciseCatalog + extendedExerciseCatalog)
    .distinctBy { it.name.trim().lowercase() }
internal var exercises: List<Exercise> = legacyExercises
internal fun installExerciseDbV2(records:List<ExerciseV2Record>) {
    if (records.isEmpty()) return
    ExerciseDbV2.install(records)
    val lookup=ExerciseDbV2.uniqueLookup(records)
    val migrated=records.map { record ->
        val legacy=record.lookupNames().asSequence().mapNotNull { name ->
            legacyExercises.firstOrNull { it.name.trim().lowercase()==name }
        }.firstOrNull()
        ExerciseDbV2.toLegacyExercise(record,legacy)
    }
    val migratedLegacyNames=migrated.map { it.name.trim().lowercase() }.toSet()
    // These are current-model programming entries still required by SkillCatalog but not yet
    // represented by a source-backed v2 record. They are retained for current v0.9.3 behavior,
    // not for backward compatibility with older app data.
    val currentModelOnly=legacyExercises.filter { it.name.trim().lowercase() !in migratedLegacyNames &&
        ExerciseDbV2.resolveLegacy(it.name,records)==null }
    exercises=(migrated+currentModelOnly).distinctBy { it.progressionKey ?: it.name.trim().lowercase() }
    if (exercises.isEmpty()) exercises=legacyExercises
}

internal data class ExercisePrescription(val sets:Int,val value:Double,val unit:String)
internal fun prescriptionFor(ex:Exercise):ExercisePrescription {
    // APP_RULE baseline required by the workout runner; source programming examples
    // are intentionally not treated as universal exercise prescriptions.
    val defaultValue = when(ex.measure) {
        ExerciseMeasure.REPS -> when(ex.role){"Forza"->6.0;"Progressione"->5.0;else->10.0}
        ExerciseMeasure.SECONDS, ExerciseMeasure.DURATION -> when(ex.level){"Principiante"->20.0;"Intermedio"->30.0;else->40.0}
        ExerciseMeasure.DISTANCE -> 30.0
        ExerciseMeasure.INTERVALS -> 60.0
    }
    val unit=when(ex.measure){ExerciseMeasure.REPS->"rip";ExerciseMeasure.SECONDS,ExerciseMeasure.DURATION->"sec";ExerciseMeasure.DISTANCE->"m";ExerciseMeasure.INTERVALS->"sec lavoro"}
    return ExercisePrescription(ex.targetSets,ex.targetValue?:defaultValue,unit)
}
internal fun plannedExerciseState(index:Int,ex:Exercise):WorkoutExerciseState {
    val p=prescriptionFor(ex)
    val sets=(0 until p.sets).map { n -> WorkoutSetState(index=n,targetReps=if(ex.measure==ExerciseMeasure.REPS)p.value.toInt() else null,targetSeconds=if(ex.measure==ExerciseMeasure.SECONDS||ex.measure==ExerciseMeasure.DURATION||ex.measure==ExerciseMeasure.INTERVALS)p.value.toInt() else null,targetDistanceMeters=if(ex.measure==ExerciseMeasure.DISTANCE)p.value else null) }
    return WorkoutExerciseState(ex.name,ex.measure,sets,WorkoutExerciseStatus.PENDING,null,false,ex.setRestSec,ex.exerciseRestSec,index,index,ex.progressionKey,ex)
}
internal fun runnerSetRestSeconds(exercise:Exercise, prescribedRestSeconds:Int?=null):Int =
    exercise.intervalRestSec ?: prescribedRestSeconds ?: exercise.setRestSec
private fun buildActiveWorkout(profileName:String,items:List<Exercise>,now:Long=System.currentTimeMillis())=WorkoutSessionState(now.toString(),now,profileName,items.mapIndexed(::plannedExerciseState))
private fun completedExerciseState(index:Int,ex:Exercise,completed:Int,rpe:Int?):WorkoutExerciseState {
    val planned=plannedExerciseState(index,ex)
    val sets=planned.sets.mapIndexed { n,s -> if(n<completed) s.copy(actualReps=s.targetReps,actualSeconds=s.targetSeconds,actualDistanceMeters=s.targetDistanceMeters,completed=true) else s }
    val status=when { sets.isNotEmpty()&&completed>=sets.size->WorkoutExerciseStatus.COMPLETED;completed>0->WorkoutExerciseStatus.PARTIAL;else->WorkoutExerciseStatus.PENDING }
    return planned.copy(sets=sets,status=status,rpe=rpe)
}
private fun WorkoutSetState?.orEmptySet():List<WorkoutSetState> = this?.let{listOf(it)}?:emptyList()
internal fun remainingSeconds(endEpochMs:Long?, now:Long=System.currentTimeMillis()):Int =
    if(endEpochMs==null) 0 else kotlin.math.ceil((endEpochMs-now).coerceAtLeast(0L)/1000.0).toInt()

internal fun exerciseEquipmentOptions(source:List<Exercise>):List<String> =
    source.flatMap { EquipmentCatalog.requirementsFor(it) }
        .map(EquipmentCatalog::displayName)
        .distinct()
        .sortedBy { it.lowercase() }
internal fun profileEquipmentOptions(source:List<Exercise>):List<String> =
    (exerciseEquipmentOptions(source) + EquipmentCatalog.displayName(EquipmentId.BODYWEIGHT))
        .distinct().sortedBy { it.lowercase() }

internal fun levelScore(level: String) = when(level) { "Principiante" -> 1; "Intermedio" -> 2; else -> 3 }
internal fun capabilityLevelFor(ex:Exercise, capability:CapabilityProfile):Int {
    if(!capability.assessed) return 1
    return when {
        ex.resistanceMode==ResistanceMode.BODYWEIGHT && ex.category==ExerciseCategory.PULL ->
            capability.pullCapacity ?: 1
        ex.resistanceMode==ResistanceMode.BODYWEIGHT && ex.category==ExerciseCategory.PUSH ->
            capability.pushCapacity ?: 1
        else -> capability.trainingExperience ?: 1
    }
}

private val recoveryGroups = listOf("Dorso", "Petto", "Spalle", "Gambe", "Core", "Bicipiti", "Tricipiti")

internal fun recoveryScores(sessions: List<SessionRecord>, now: Long = System.currentTimeMillis()): Map<String, Int> {
    val fatigue = recoveryGroups.associateWith { 0.0 }.toMutableMap()
    sessions.take(16).forEach { session ->
        val ageHours = (now - session.timestamp).coerceAtLeast(0L) / 3_600_000.0
        if (ageHours < 96.0 && session.totalSets > 0) {
            val completion = if(session.completedByExercise.isEmpty()) (session.completedSets.toDouble() / session.totalSets).coerceIn(0.0, 1.0) else 1.0
            // Recovery model V0.8: exponential fatigue decay, time constant 38h. A single fatigue contribution falls to ~28% after 48h and ~15% after 72h; UI returns toward 100 progressively.
            val recoveryFactor = kotlin.math.exp(-ageHours / 38.0)
            val setsPerExercise = session.totalSets.toDouble() / session.exerciseNames.size.coerceAtLeast(1)
            session.exerciseNames.mapNotNull { n -> exercises.firstOrNull { it.name == n } }.forEach { ex ->
                val actualSets=session.structuredExercises.firstOrNull{it.exerciseName==ex.name}?.sets?.count{it.completed}?.toDouble() ?: if(session.completedByExercise.isNotEmpty()) (session.completedByExercise[ex.name]?:0).toDouble() else setsPerExercise
                if(actualSets<=0) return@forEach
                val matched = recoveryGroups.filter { ex.group.contains(it, ignoreCase = true) }
                matched.forEachIndexed { index, group ->
                    val muscleWeight = if (index == 0) 1.0 else 0.55
                    fatigue[group] = (fatigue[group] ?: 0.0) + (actualSets * 5.0 * completion * recoveryFactor * muscleWeight)
                }
            }
        }
    }
    return fatigue.mapValues { (_, load) -> (100.0 - load.coerceAtMost(80.0)).toInt().coerceIn(20, 100) }
}

internal fun generateWorkout(profile: TrainingLocation, athlete: AthleteProfile, sessions: List<SessionRecord>, readinessScore:Int?=null, avoidPlannedNames:Set<String> = emptySet()): List<Exercise> =
    generateWorkoutFromCatalogue(exercises,profile,athlete,athlete.toCapabilityProfile(),sessions,readinessScore,avoidPlannedNames)
internal fun generateWorkoutFromCatalogue(catalogue:List<Exercise>,location:TrainingLocation, athlete: AthleteProfile, capability:CapabilityProfile, sessions: List<SessionRecord>, readinessScore:Int?=null, avoidPlannedNames:Set<String> = emptySet()): List<Exercise> {
    val context=coachingContext(sessions,readinessScore)
    val eligible=skillEligibleExerciseNames(catalogue,sessions)
    val available = catalogue.filter { it.name in eligible && locationCompatible(it,location) && levelScore(it.level) <= capabilityLevelFor(it,capability) }
    val preferredRoles = when (athlete.goal) {
        "Calisthenics" -> listOf("Progressione", "Prerequisito", "Forza", "Accessorio")
        "Ipertrofia" -> listOf("Forza", "Accessorio", "Prerequisito", "Progressione")
        "Forza + skill", "Forza + Skill Calisthenics" -> if(athlete.prioritySkills.isEmpty()) listOf("Forza","Prerequisito","Accessorio","Progressione") else listOf("Prerequisito","Progressione","Forza","Accessorio")
        else -> listOf("Forza", "Prerequisito", "Accessorio", "Progressione")
    }
    val recentNames = sessions.take(3).flatMap { it.exerciseNames }.groupingBy { it }.eachCount()
    val recovery = recoveryScores(sessions)
    val freshnessSorted = available.sortedWith(
        compareBy<Exercise> { ex ->
            val repeatPenalty = (recentNames[ex.name] ?: 0) * 45
            val musclePenalty = recovery.filterKeys { ex.group.contains(it, true) }.values.minOrNull()?.let { score ->
                when { score < 40 -> 140; score < 60 -> 85; score < 80 -> 40; else -> 0 }
            } ?: 0
            val skillBonus = if(athlete.prioritySkills.any { it in ex.skills }) -70 else 0
            repeatPenalty + musclePenalty + skillBonus + if(ex.name in avoidPlannedNames) 100 else 0
        }.thenBy { preferredRoles.indexOf(it.role).let { i -> if(i < 0) 99 else i } }
    )
    val result = mutableListOf<Exercise>()
    if(athlete.goal=="Calisthenics" && athlete.prioritySkills.isNotEmpty()){
        val skillPool=freshnessSorted.filter { ex -> athlete.prioritySkills.any { it in ex.skills } }
        skillPool.take(3).forEach { if(it !in result) result.add(it) }
    }
    preferredRoles.forEach { role -> freshnessSorted.filter { it.role == role }.forEach { if (it !in result && result.size < 6) result.add(it) } }
    freshnessSorted.forEach { if (it !in result && result.size < 6) result.add(it) }
    return applyCoaching(sequenceWorkout(result).sortedBy{ex->if(athlete.prioritySkills.any{it in ex.skills})0 else 1},context)
}

internal data class PlannedSessionRealization(
 val exercises:List<Exercise>,
 val plannedWorkoutId:String
)
internal fun materializePlannedSession(
 planned:PlannedWorkout,
 location:TrainingLocation,
 athlete:AthleteProfile,
 sessions:List<SessionRecord>,
 readinessScore:Int?,
 catalogue:List<Exercise> = exercises
):PlannedSessionRealization {
 val intent=requireNotNull(planned.intent) { "Planned session ${planned.id} has no intent" }
 val effectiveAthlete=athlete.copy(goal=intent.goal,prioritySkills=intent.prioritySkills)
 val compatibleCatalogue=catalogue.filter{locationCompatible(it,location)}
 val generated=generateWorkoutFromCatalogue(compatibleCatalogue,location,effectiveAthlete,effectiveAthlete.toCapabilityProfile(),sessions,readinessScore)
 val focused=if(intent.focusCategories.isEmpty()) generated else generated.filter{it.category in intent.focusCategories}
 val realized=focused.takeIf{it.isNotEmpty()} ?: generated
 return PlannedSessionRealization(realized,planned.id)
}

internal fun generateTrainingPlan(planId:String,startEpochDay:Long,configuration:PlanConfiguration):TrainingPlan {
 val valid=configuration.validated()
 val scheduled=TrainingPlanEngine.scheduleImmediately(planId,startEpochDay,valid.weeks,valid.daysPerWeek,valid.trainingDays){_,_->emptyList()}
 return scheduled.copy(goal=valid.goal,trainingDays=valid.trainingDays,prioritySkills=valid.prioritySkills,
  workouts=scheduled.workouts.map{w->w.copy(intent=PlannedSessionIntent(valid.goal,valid.prioritySkills,emptySet(),w.sequenceInWeek).validated())})
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Install the canonical exercise catalogue before any store-backed state is read.
        // Persisted plans keep only session intent; concrete exercises are materialized
        // against the current catalogue and execution location when the workout starts.
        installExerciseDbV2(ExerciseDbV2.load(this))
        ExerciseDbV2.installSources(ExerciseDbV2.loadSources(this))
        ExerciseDbV2.installProgressions(ExerciseDbV2.loadProgressions(this))
        ExerciseDbV2.installProgrammingGuidance(ExerciseDbV2.loadProgrammingGuidance(this))
        setContent { HybridFitnessApp() }
    }
}

enum class Tab { HOME, PLAN, PROGRESS, PROFILE }

private val italianCalendarDate = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy", java.util.Locale.ITALY)
private fun calendarDate(epochDay:Long):String = java.time.LocalDate.ofEpochDay(epochDay).format(italianCalendarDate)

@Composable
fun HybridFitnessApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { AppStore(context) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    val customProfiles = remember { mutableStateListOf<TrainingLocation>().apply { addAll(store.loadTrainingLocations()) } }
    var activeProfile by remember { mutableStateOf(customProfiles.firstOrNull { it.id == store.loadActiveTrainingLocationId() } ?: customProfiles.firstOrNull() ?: trainingLocations.first()) }
    var athlete by remember { mutableStateOf(store.loadAthlete()) }
    var sessions by remember { mutableStateOf(store.loadSessions()) }
    var calendarWorkout by remember { mutableStateOf<PlannedWorkout?>(null) }
    var workoutLaunch by remember { mutableStateOf<WorkoutLaunchFlow?>(null) }
    fun saveFinishedWorkout(record: SessionRecord): Boolean {
        val saved = store.finishWorkout(record)
        if (saved) sessions = store.loadSessions()
        else android.widget.Toast.makeText(context, "Salvataggio non riuscito: allenamento conservato per il recupero.", android.widget.Toast.LENGTH_LONG).show()
        return saved
    }
    if (store.issues.isNotEmpty()) {
        StoreRecoveryScreen(store)
        return
    }
    val health=rememberHealthConnectState()
    CompositionLocalProvider(LocalHealthConnect provides health) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Accent, background = Bg, surface = Color.White)) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    listOf(
                        Triple(Tab.HOME, Icons.Default.Home, "Oggi"),
                        Triple(Tab.PLAN, Icons.Default.DateRange, "Piano"),
                        Triple(Tab.PROGRESS, Icons.Default.TrendingUp, "Progressi"),
                        Triple(Tab.PROFILE, Icons.Default.Person, "Profilo")
                    ).forEach { (item, icon, label) ->
                        NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(icon, label) }, label = { Text(label) })
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                val launchScreen=workoutLaunch?.takeIf{it.stage!=WorkoutLaunchStage.RUNNER}
                if(launchScreen!=null){
                    val locationOptions=(trainingLocations+customProfiles+activeProfile).distinctBy{it.id}
                    val allPlans=store.loadTrainingPlans()
                    val enabledPlans=store.loadEnabledTrainingPlans()
                    val extraAthlete=athlete.copy(
                        goal=enabledPlans.joinToString(" + "){it.goal}.ifBlank{athlete.goal},
                        prioritySkills=enabledPlans.flatMap{it.prioritySkills}.distinct()
                    )
                    val sourceExercise=launchScreen.intent.sourceExerciseId?.let{id->
                        exercises.firstOrNull{it.progressionKey==id || it.name==id}
                    }
                    val compatibleLocations=remember(launchScreen.intent,locationOptions,athlete,sessions,health.readiness){
                        locationOptions.filter{place->
                            when(launchScreen.intent.origin){
                                WorkoutOrigin.SINGLE_EXERCISE -> sourceExercise?.let{locationCompatible(it,place)}==true
                                WorkoutOrigin.PLAN -> allPlans.asSequence().flatMap{it.workouts.asSequence()}
                                    .firstOrNull{it.id==launchScreen.intent.plannedWorkoutId}
                                    ?.let{materializePlannedSession(it,place,athlete,sessions,health.readiness).exercises.isNotEmpty()}==true
                                WorkoutOrigin.ADAPTIVE_EXTRA -> generateWorkout(place,extraAthlete,sessions,health.readiness).isNotEmpty()
                                else -> false
                            }
                        }
                    }
                    WorkoutLaunchScreen(
                        flow=launchScreen,locations=compatibleLocations,
                        preferredLocationId=store.loadActiveTrainingLocationId(),readinessScore=health.readiness,
                        onChooseLocation={place->
                            val selected=selectWorkoutLocation(launchScreen,place.id)
                            val now=System.currentTimeMillis()
                            val materialized=when(launchScreen.intent.origin){
                                WorkoutOrigin.SINGLE_EXERCISE -> singleExerciseWorkoutState(
                                    exercise=requireNotNull(sourceExercise),profileName=place.name,startedAtEpochMs=now,
                                    athlete=athlete,sessions=sessions,readinessScore=health.readiness,trainingLocationId=place.id
                                )
                                WorkoutOrigin.PLAN -> {
                                    val planned=requireNotNull(allPlans.asSequence().flatMap{it.workouts.asSequence()}
                                        .firstOrNull{it.id==launchScreen.intent.plannedWorkoutId})
                                    val realization=materializePlannedSession(planned,place,athlete,sessions,health.readiness)
                                    WorkoutSessionState(
                                        id="plan-"+planned.id+"-"+now,startedAtEpochMs=now,profileName=place.name,
                                        exercises=realization.exercises.mapIndexed(::plannedExerciseState),
                                        plannedWorkoutId=realization.plannedWorkoutId,trainingLocationId=place.id
                                    )
                                }
                                WorkoutOrigin.ADAPTIVE_EXTRA -> {
                                    val generated=generateWorkout(place,extraAthlete,sessions,health.readiness)
                                    val effective=effectiveAutomaticExercises(generated,athlete,sessions,health.readiness)
                                    WorkoutSessionState(
                                        id="extra-"+now,startedAtEpochMs=now,profileName=place.name,
                                        exercises=effective.mapIndexed(::plannedExerciseState).map{it.copy(rpe=5)},
                                        trainingLocationId=place.id
                                    )
                                }
                                else -> error("Workout origin has no materializer yet: "+launchScreen.intent.origin)
                            }
                            workoutLaunch=materializeWorkoutLaunch(selected,materialized)
                        },
                        onStart={
                            val place=locationOptions.firstOrNull{it.id==launchScreen.locationId}
                            if(place!=null){activeProfile=place;store.saveActiveTrainingLocationId(place.id)}
                            workoutLaunch=startWorkoutRunner(launchScreen)
                        },
                        onCancel={workoutLaunch=null}
                    )
                } else {
                    when (tab) {
                        Tab.HOME -> Home(
                            profile=activeProfile,athlete=athlete,sessions=sessions,store=store,
                            changeProfile={tab=Tab.PROFILE},managePlan={tab=Tab.PLAN},
                            calendarWorkout=calendarWorkout,onCalendarWorkoutConsumed={calendarWorkout=null},
                            onStartPlannedWorkout={workout->workoutLaunch=beginWorkoutLaunch(WorkoutLaunchIntent(WorkoutOrigin.PLAN,plannedWorkoutId=workout.id))},
                            onStartAdaptiveExtra={workoutLaunch=beginWorkoutLaunch(WorkoutLaunchIntent(WorkoutOrigin.ADAPTIVE_EXTRA))},
                            onSessionSaved={record->saveFinishedWorkout(record)}
                        )
                        Tab.PLAN -> TrainingPlanScreen(store) { workout -> calendarWorkout=workout;tab=Tab.HOME }
                        Tab.PROGRESS -> Progress(athlete,sessions,{updated->athlete=updated;store.saveAthlete(updated)},{timestamp->store.deleteSession(timestamp);sessions=store.loadSessions()},showAthleteProfile=false)
                        Tab.PROFILE -> ProfileHub(
                            active=activeProfile,athlete=athlete,sessions=sessions,store=store,customProfiles=customProfiles,
                            onAthleteSave={updated->athlete=updated;store.saveAthlete(updated)},
                            selectLocation={selected->activeProfile=selected;store.saveActiveTrainingLocationId(selected.id)},
                            addLocation={created->customProfiles.add(created);store.saveTrainingLocations(customProfiles);activeProfile=created;store.saveActiveTrainingLocationId(created.id)},
                            editLocation={old,updated->val i=customProfiles.indexOfFirst{it.id==old.id};if(i>=0){customProfiles[i]=updated;store.saveTrainingLocations(customProfiles);if(activeProfile.id==old.id){activeProfile=updated;store.saveActiveTrainingLocationId(updated.id)}}},
                            deleteLocation={doomed->customProfiles.removeAll{it.id==doomed.id};store.saveTrainingLocations(customProfiles);if(activeProfile.id==doomed.id){activeProfile=customProfiles.firstOrNull()?:trainingLocations.first();store.saveActiveTrainingLocationId(activeProfile.id)}},
                            onExecuteExercise={exercise->workoutLaunch=beginWorkoutLaunch(WorkoutLaunchIntent(WorkoutOrigin.SINGLE_EXERCISE,sourceExerciseId=exercise.progressionKey?:exercise.name))},
                            onSessionSaved={record->saveFinishedWorkout(record)}
                        )
                    }
                }
            }
        }
        workoutLaunch?.takeIf{it.stage==WorkoutLaunchStage.RUNNER}?.let { flow ->
            val place=(trainingLocations+customProfiles+activeProfile).firstOrNull{it.id==flow.locationId}?:activeProfile
            WorkoutDialog(
                profile=place,athlete=athlete,sessions=sessions,store=store,restored=flow.workout,
                plannedWorkout=flow.workout?.plannedWorkoutId?.let{id->store.loadTrainingPlans().asSequence().flatMap{it.workouts.asSequence()}.firstOrNull{it.id==id}},
                onDismiss={workoutLaunch=null},
                onSave={record->val saved=saveFinishedWorkout(record);if(saved)workoutLaunch=null;saved}
            )
        }
    }
}
}

@Composable
private fun WorkoutLaunchScreen(
    flow:WorkoutLaunchFlow,
    locations:List<TrainingLocation>,
    preferredLocationId:String,
    readinessScore:Int?,
    onChooseLocation:(TrainingLocation)->Unit,
    onStart:()->Unit,
    onCancel:()->Unit
) {
    var selectedLocationId by remember(flow.intent,locations,preferredLocationId) {
        mutableStateOf(preferredWorkoutLocation(locations,preferredLocationId)?.id)
    }
    val originLabel=when(flow.intent.origin) {
        WorkoutOrigin.PLAN -> "Sessione del piano"
        WorkoutOrigin.ADAPTIVE_EXTRA -> "Allenamento extra adattivo"
        WorkoutOrigin.CUSTOM -> "Workout personalizzato"
        WorkoutOrigin.SKILL -> "Allenamento skill"
        WorkoutOrigin.LIBRARY -> "Workout dalla libreria"
        WorkoutOrigin.SINGLE_EXERCISE -> "Singolo esercizio"
    }
    Column(Modifier.fillMaxSize().background(Bg)) {
        Header(if(flow.stage==WorkoutLaunchStage.CHOOSE_LOCATION)"Avvia allenamento" else "Anteprima allenamento",
            originLabel+" • readiness "+(readinessScore?.toString()?: "non disponibile"))
        if(flow.stage==WorkoutLaunchStage.CHOOSE_LOCATION) {
            Text("Scegli il luogo",fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=20.dp,vertical=8.dp))
            if(locations.isEmpty()) {
                Text("Nessun luogo configurato è compatibile con questo allenamento.",color=Color.Gray,modifier=Modifier.padding(20.dp))
            } else {
                LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=16.dp,vertical=4.dp)) {
                    items(locations,key={it.id}){place->
                        Card(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{selectedLocationId=place.id},
                            colors=CardDefaults.cardColors(containerColor=if(selectedLocationId==place.id)MaterialTheme.colorScheme.primaryContainer else Color.White)) {
                            Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                                RadioButton(selectedLocationId==place.id,{selectedLocationId=place.id})
                                Text(place.icon+"  "+place.name,modifier=Modifier.weight(1f),fontWeight=FontWeight.SemiBold)
                                if(place.id==preferredLocationId)Text("ULTIMO USATO",fontSize=10.sp,color=Accent)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick=onCancel,modifier=Modifier.weight(1f)){Text("ANNULLA")}
                Button(onClick={locations.firstOrNull{it.id==selectedLocationId}?.let(onChooseLocation)},
                    enabled=locations.any{it.id==selectedLocationId},modifier=Modifier.weight(1f)){Text("CONTINUA")}
            }
        } else if(flow.stage==WorkoutLaunchStage.PREVIEW) {
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=20.dp,vertical=8.dp)) {
                item{Text("Luogo: "+(locations.firstOrNull{it.id==flow.locationId}?.name?:flow.locationId.orEmpty()),color=Color.Gray)}
                itemsIndexed(flow.workout?.exercises.orEmpty(),key={index,exercise->index.toString()+"-"+(exercise.exerciseId?:exercise.exerciseName)}){_,exercise->
                    val first=exercise.sets.firstOrNull()
                    val target=when(exercise.measure){
                        ExerciseMeasure.REPS -> (first?.targetReps?.toString()?: "—")+" ripetizioni"
                        ExerciseMeasure.SECONDS,ExerciseMeasure.DURATION,ExerciseMeasure.INTERVALS -> (first?.targetSeconds?.toString()?: "—")+" secondi"
                        ExerciseMeasure.DISTANCE -> (first?.targetDistanceMeters?.toString()?: "—")+" metri"
                    }
                    val load=first?.targetLoadKg?.let{" • "+it+" kg"}.orEmpty()
                    val assist=first?.targetAssistanceKg?.let{" • assistenza "+it+" kg"}.orEmpty()
                    Card(Modifier.fillMaxWidth().padding(vertical=5.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(exercise.exerciseName,fontWeight=FontWeight.Bold)
                            Text(exercise.sets.size.toString()+" serie • "+target+" • recupero "+exercise.setRestSec+" s"+load+assist,color=Color.Gray)
                            Text("RPE "+runnerEffort(exercise.rpe),fontSize=12.sp,color=Accent)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick=onCancel,modifier=Modifier.weight(1f)){Text("ANNULLA")}
                Button(onClick=onStart,enabled=flow.workout?.exercises?.isNotEmpty()==true,modifier=Modifier.weight(1f)){Text("AVVIA")}
            }
        }
    }
}

@Composable
private fun ProfileHub(
    active:TrainingLocation, athlete:AthleteProfile, sessions:List<SessionRecord>, store:AppStore,
    customProfiles:List<TrainingLocation>, onAthleteSave:(AthleteProfile)->Unit,
    selectLocation:(TrainingLocation)->Unit, addLocation:(TrainingLocation)->Unit,
    editLocation:(TrainingLocation,TrainingLocation)->Unit, deleteLocation:(TrainingLocation)->Unit,
    onExecuteExercise:(Exercise)->Unit, onSessionSaved:(SessionRecord)->Boolean
) {
    var section by remember { mutableStateOf("Profilo") }
    var editAthlete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Header("Profilo","Atleta, luoghi e libreria")
        LazyRow(Modifier.padding(horizontal=20.dp,vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            items(listOf("Profilo","Luoghi","Esercizi")) { item ->
                FilterChip(selected=section==item,onClick={section=item},label={Text(item)})
            }
        }
        when(section) {
            "Profilo" -> LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp)) {
                item {
                    Card(Modifier.fillMaxWidth().clickable{editAthlete=true}) {
                        Column(Modifier.padding(18.dp)) {
                            Text(if(athlete.name.isBlank())"Configura profilo atleta" else athlete.name,fontSize=20.sp,fontWeight=FontWeight.Bold)
                            val capability=athlete.toCapabilityProfile()
                            Text(if(capability.assessed)"Capability Profile • esperienza ${capability.trainingExperience}/3 • push ${capability.pushCapacity}/3 • pull ${capability.pullCapacity}/3" else "Capability Profile: valutazione da completare",color=Color.Gray)
                            Text("MODIFICA PROFILO",color=Accent,fontSize=12.sp,modifier=Modifier.padding(top=8.dp))
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth().padding(top=12.dp).clickable{section="Luoghi"}) {
                        Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text(active.icon,fontSize=28.sp);Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)){Text("Luogo attivo",color=Color.Gray);Text(active.name,fontWeight=FontWeight.Bold)}
                            Text("GESTISCI",color=Accent,fontSize=12.sp)
                        }
                    }
                }
                item { Box(Modifier.padding(top=12.dp)){HealthConnectCard()} }
            }
            "Luoghi" -> Equipment(active,customProfiles,selectLocation,addLocation,editLocation,deleteLocation)
            else -> Library(active,athlete,sessions,store,onExecuteExercise,onSessionSaved)
        }
    }
    if(editAthlete) AthleteDialog(athlete,{editAthlete=false}){updated->onAthleteSave(updated);editAthlete=false}
}

@Composable private fun Header(title: String, subtitle: String? = null) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        subtitle?.let { Text(it, color = Color.Gray, modifier = Modifier.padding(top = 4.dp)) }
    }
}

internal fun readinessBand(score:Int):String = when { score>=80->"PRONTO";score>=60->"BUONO";score>=45->"CAUTELA";else->"RECUPERO" }

@Composable private fun ReadinessOverview(readiness:Int?, sessions:List<SessionRecord>) {
    val recovery=remember(sessions){recoveryScores(sessions)}
    val avg=if(recovery.isEmpty())100 else recovery.values.average().toInt()
    val score=readiness?:avg
    Card(Modifier.fillMaxWidth().padding(vertical=8.dp),shape=RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Stato di forma • "+readinessBand(score),fontWeight=FontWeight.Bold);Text("$score/100",fontWeight=FontWeight.Bold,color=Accent)}
        LinearProgressIndicator(progress={score.coerceIn(0,100)/100f},modifier=Modifier.fillMaxWidth().padding(vertical=10.dp))
        Text(if(readiness!=null)"Health Connect • recupero medio $avg%" else "Stima locale • recupero medio $avg%",fontSize=12.sp,color=Color.Gray)
        recovery.minByOrNull{it.value}?.let{Text("Da proteggere: "+it.key+" "+it.value+"%",fontSize=12.sp)}
    }}
}

@Composable
private fun CoachSummary(context:CoachingContext) {
    Card(Modifier.fillMaxWidth().padding(vertical=8.dp),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("HYBRID • ADAPTIVE COACH",fontSize=11.sp,fontWeight=FontWeight.Bold)
            Text(coachingLabel(context.plan.decision),fontSize=20.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=6.dp))
            Text(if(context.readiness!=null)"Readiness ${context.readiness}/100" else "Recupero stimato dallo storico",fontSize=13.sp)
            Text("Volume ${((context.plan.volumeFactor-1)*100).toInt()}% • intensità ${((context.plan.intensityFactor-1)*100).toInt()}%",fontSize=13.sp)
            context.plan.notes.filterNot{it.startsWith("Decisione:")}.forEach{Text(it,fontSize=12.sp)}
        }
    }
}

internal fun nextUnlockLabel(states:List<SkillNodeProgress>):String {
    val next=states.firstOrNull{it.status==ProgressionStatus.IN_PROGRESS || it.status==ProgressionStatus.AVAILABLE || it.status==ProgressionStatus.LOCKED}
        ?: return "Percorso completato"
    val criterion=next.node.criterion
    val target=criterion.minSeconds?.let{"$it s"}?:criterion.minReps?.let{"$it rep"}?:"target"
    return "Prossimo sblocco: ${next.node.exerciseName} • ${criterion.minSets}×$target • ${criterion.requiredSessions} sessioni"
}

@Composable
private fun Home(profile: TrainingLocation, athlete: AthleteProfile, sessions: List<SessionRecord>, store: AppStore, changeProfile: () -> Unit, managePlan: () -> Unit, calendarWorkout:PlannedWorkout?, onCalendarWorkoutConsumed:()->Unit, onStartPlannedWorkout:(PlannedWorkout)->Unit, onStartAdaptiveExtra:()->Unit, onSessionSaved: (SessionRecord) -> Boolean) {
    val readinessScore=LocalHealthConnect.current.readiness
    val restoredWorkout = remember { store.loadActiveWorkout() }
    var workoutOpen by remember { mutableStateOf(restoredWorkout != null) }
    var customWorkoutOpen by remember { mutableStateOf(false) }
    var treeSkill by remember { mutableStateOf<String?>(null) }
    var treePicker by remember { mutableStateOf(false) }
    var customSkill by remember { mutableStateOf("Nessuna") }
    var enabledPlans by remember { mutableStateOf(store.loadEnabledTrainingPlans()) }
    LaunchedEffect(calendarWorkout?.id) {
        calendarWorkout?.let { requested ->
            val launch=resolveWorkoutLaunch(restoredWorkout,requested)
            if(launch.resumeActive) workoutOpen=true else launch.plannedWorkout?.let(onStartPlannedWorkout)
            onCalendarWorkoutConsumed()
        }
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            val planLabel=if(enabledPlans.isEmpty())"Nessun programma attivo" else enabledPlans.size.toString()+" piani attivi • "+enabledPlans.sumOf{it.daysPerWeek}+" sessioni/settimana"
            Header(if (athlete.name.isBlank()) "Buongiorno 👋" else "Ciao, " + athlete.name + " 👋", planLabel)
        }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.icon, fontSize = 30.sp); Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text("Ti alleni in", color = Color.Gray); Text(profile.name, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                        TextButton(onClick = changeProfile) { Text("CAMBIA") }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    Text("Oggi", fontWeight = FontWeight.SemiBold)
                    val today=java.time.LocalDate.now().toEpochDay()
                    val todaySessions=todayPlannedWorkouts(enabledPlans,today)
                    val nextSession=enabledPlans.flatMap{plan->plan.workouts.filter{w->w.epochDay>today && w.state==PlannedWorkoutState.PLANNED}.map{w->plan to w}}
                        .minByOrNull{it.second.epochDay}
                    val combinedSkills=enabledPlans.flatMap{it.prioritySkills}.distinct()
                    val combinedGoal=enabledPlans.joinToString(" + "){it.goal}.ifBlank{athlete.goal}
                    val extraAthlete=athlete.copy(goal=combinedGoal,prioritySkills=combinedSkills)
                    val extra=if(todaySessions.isEmpty())generateWorkout(profile,extraAthlete,sessions,readinessScore) else emptyList()
                    if(todaySessions.isNotEmpty()) {
                        Text(if(todaySessions.size==1)"Allenamento del piano" else todaySessions.size.toString()+" allenamenti dei piani", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        todaySessions.forEach { (owner,scheduled) ->
                            val realized=materializePlannedSession(scheduled,profile,athlete,sessions,readinessScore)
                            Card(Modifier.fillMaxWidth().padding(top=10.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(owner.goal,fontWeight=FontWeight.Bold)
                                    Text(realized.exercises.size.toString()+" esercizi • sessione "+(scheduled.sequenceInWeek+1),color=Color.Gray,fontSize=12.sp)
                                    Button(onClick={onStartPlannedWorkout(scheduled)},enabled=realized.exercises.isNotEmpty(),modifier=Modifier.fillMaxWidth().padding(top=8.dp)){Text("INIZIA")}
                                }
                            }
                        }
                    } else {
                        Text("Nessuna sessione programmata", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        nextSession?.let { (owner,w) -> Text("Prossimo: "+owner.goal+" • "+calendarDate(w.epochDay),color=Color.Gray) }
                        Text("Puoi recuperare oppure aggiungere una sessione extra adattata al carico complessivo dei piani attivi.",fontSize=13.sp,color=Color.Gray,modifier=Modifier.padding(top=8.dp))
                        Button(onClick=onStartAdaptiveExtra,enabled=extra.isNotEmpty(),modifier=Modifier.fillMaxWidth().padding(top=18.dp),shape=RoundedCornerShape(14.dp)){Text("ALLENAMENTO EXTRA")}
                    }
                    OutlinedButton(onClick = { customWorkoutOpen = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(14.dp)) { Text("CREA WORKOUT PERSONALIZZATO") }
                    TextButton(onClick=managePlan,modifier=Modifier.fillMaxWidth()){Text(if(enabledPlans.isEmpty())"CREA PIANO" else "VEDI PIANI")}
                }
            }
        }
        item { Box(Modifier.padding(horizontal=20.dp)) { ReadinessOverview(readinessScore,sessions) } }
        item { Box(Modifier.padding(horizontal=20.dp)) { CoachSummary(coachingContext(sessions,readinessScore,enabledPlans,java.time.LocalDate.now().toEpochDay())) } }
        item { SectionTitle("Il tuo Skill Tree") }
        items(enabledPlans.flatMap{it.prioritySkills}.distinct().ifEmpty{listOf("Front lever","Planche","Handstand")}.take(3)) { skill ->
            val states=SkillCatalog.progress(skill,sessions)
            val percent=ProgressionEngine.skillProgress(states)
            val current=states.firstOrNull{it.status==ProgressionStatus.IN_PROGRESS || it.status==ProgressionStatus.AVAILABLE}
            Card(Modifier.padding(horizontal=20.dp,vertical=5.dp).fillMaxWidth().clickable{treeSkill=skill},shape=RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(skill,fontWeight=FontWeight.Bold,fontSize=18.sp);Text("$percent%",color=Accent,fontWeight=FontWeight.Bold)}
                    LinearProgressIndicator(progress={percent/100f},modifier=Modifier.fillMaxWidth().padding(vertical=10.dp))
                    Text(nextUnlockLabel(states),fontSize=13.sp,fontWeight=FontWeight.SemiBold)
                    Text("${states.count{it.status==ProgressionStatus.MASTERED}} / ${states.size} step consolidati • tocca per i requisiti",fontSize=12.sp,color=Color.Gray)
                }
            }
        }
        item{TextButton(onClick={treePicker=true},modifier=Modifier.padding(horizontal=20.dp)){Text("ESPLORA TUTTE LE SKILL")}}
        item{Box(Modifier.padding(horizontal=20.dp)){HealthConnectCard()}}
        item { SectionTitle("Recupero") }
        item { RecoveryChart(sessions) }
    }
    if(treePicker) AlertDialog(onDismissRequest={treePicker=false},title={Text("Skill Tree")},text={LazyColumn{items(calisthenicsSkills){skill->TextButton(onClick={treeSkill=skill.name;treePicker=false}){Text(skill.name)}}}},confirmButton={TextButton(onClick={treePicker=false}){Text("CHIUDI")}})
    treeSkill?.let{skill->SkillDetailDialog(skill,athlete,{treeSkill=null},{customSkill=skill;treeSkill=null;customWorkoutOpen=true},sessions)}
    if (workoutOpen && restoredWorkout?.runnerKind!="custom") { WorkoutDialog(profile = profile, athlete = athlete, sessions = sessions, store = store, restored = restoredWorkout, plannedWorkout=restoredWorkout?.plannedWorkoutId?.let{id->store.loadTrainingPlans().asSequence().flatMap{it.workouts.asSequence()}.firstOrNull{w->w.id==id}}, onDismiss = { workoutOpen = false }, onSave = {record->val saved=onSessionSaved(record);if(saved)enabledPlans=store.loadEnabledTrainingPlans();saved}) }
    if (customWorkoutOpen || (workoutOpen && restoredWorkout?.runnerKind=="custom")) { CustomWorkoutDialog(profile, athlete, sessions, { customWorkoutOpen=false;workoutOpen=false }, onSessionSaved, initialSkill=customSkill, store=store, restored=restoredWorkout?.takeIf{it.runnerKind=="custom"}) }
}

internal fun calendarStateLabel(state:PlannedWorkoutState):String=when(state){
 PlannedWorkoutState.PLANNED->"Pianificato";PlannedWorkoutState.RESCHEDULED->"Riprogrammato";PlannedWorkoutState.COMPLETED->"Completato";PlannedWorkoutState.PARTIAL->"Parziale";PlannedWorkoutState.SKIPPED->"Saltato";PlannedWorkoutState.ADAPTED->"Adattato"
}
internal fun calendarStateMarker(state:PlannedWorkoutState):String=when(state){
 PlannedWorkoutState.PLANNED->"P";PlannedWorkoutState.RESCHEDULED->"R";PlannedWorkoutState.COMPLETED->"✓";PlannedWorkoutState.PARTIAL->"½";PlannedWorkoutState.SKIPPED->"–";PlannedWorkoutState.ADAPTED->"A"
}
@Composable private fun calendarStateColor(state:PlannedWorkoutState):Color=when(state){
 PlannedWorkoutState.COMPLETED->MaterialTheme.colorScheme.primary
 PlannedWorkoutState.PARTIAL->MaterialTheme.colorScheme.tertiary
 PlannedWorkoutState.SKIPPED->Color.Gray
 PlannedWorkoutState.RESCHEDULED->MaterialTheme.colorScheme.secondary
 else->Accent
}

@Composable
private fun TrainingPlanScreen(store:AppStore,onStartWorkout:(PlannedWorkout)->Unit) {
    val today=java.time.LocalDate.now().toEpochDay()
    var plans by remember { mutableStateOf(store.loadTrainingPlans()) }
    var plan by remember { mutableStateOf(store.loadTrainingPlans().firstOrNull()) }
    var enabledPlanIds by remember { mutableStateOf(store.loadEnabledTrainingPlanIds()) }
    var showPlans by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<TrainingPlan?>(null) }
    val todayDate=java.time.LocalDate.ofEpochDay(today)
    var calendarYear by remember { mutableIntStateOf(todayDate.year) }
    var calendarMonth by remember { mutableIntStateOf(todayDate.monthValue) }
    var selected by remember { mutableStateOf<PlannedWorkout?>(null) }
    var selectedDayWorkouts by remember { mutableStateOf<List<PlannedWorkout>>(emptyList()) }
    var rescheduleOffset by remember { mutableStateOf(1) }
    var editPlan by remember { mutableStateOf(false) }
    var confirmRegeneration by remember { mutableStateOf<PlanConfiguration?>(null) }
    var planSaveError by remember { mutableStateOf(false) }
    fun persistCandidate(candidate:TrainingPlan):Boolean {
        if(!store.saveTrainingPlan(candidate)){ planSaveError=true; return false }
        plan=candidate;plans=store.loadTrainingPlans(); planSaveError=false; return true
    }
    Column(Modifier.fillMaxSize()) {
        Header("Piano","Calendario e programmi")
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={showPlans=true},modifier=Modifier.weight(1f)){Text("I MIEI PIANI")}
            Button(onClick={plan=null;editPlan=true},modifier=Modifier.weight(1f)){Text("+ NUOVO PIANO")}
        }
        if(plan==null) {
            Card(Modifier.padding(20.dp).fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Nessun piano selezionato",fontWeight=FontWeight.Bold,fontSize=20.sp)
                    Text("Crea qui il tuo programma multi-settimana. Il luogo di allenamento verrà scelto quando avvierai ogni sessione.",color=Color.Gray,modifier=Modifier.padding(top=6.dp))
                    Button(onClick={editPlan=true},modifier=Modifier.fillMaxWidth().padding(top=14.dp)){Text("CREA PIANO")}
                }
            }
        } else {
            val monthState=calendarMonthState(plan!!,calendarYear,calendarMonth,today)
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick={val shifted=shiftCalendarMonth(calendarYear,calendarMonth,-1);calendarYear=shifted.first;calendarMonth=shifted.second}){Icon(Icons.Default.ChevronLeft,"Mese precedente")}
                Text(monthState.title,fontWeight=FontWeight.Bold,fontSize=18.sp,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick={val shifted=shiftCalendarMonth(calendarYear,calendarMonth,1);calendarYear=shifted.first;calendarMonth=shifted.second}){Icon(Icons.Default.ChevronRight,"Mese successivo")}
                TextButton(onClick={editPlan=true}){Text("MODIFICA")}
                TextButton(onClick={confirmDelete=plan}){Text("ELIMINA")}
            }
            Row(Modifier.fillMaxWidth().padding(horizontal=12.dp)) {
                listOf("L","M","M","G","V","S","D").forEach{label->Text(label,fontSize=11.sp,fontWeight=FontWeight.Bold,color=Color.Gray,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.weight(1f))}
            }
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=12.dp,vertical=4.dp)) {
                items(monthState.days.chunked(7)) { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            val enabled=day.workouts.isNotEmpty()
                            Card(
                                Modifier.weight(1f).aspectRatio(0.82f).padding(2.dp).then(if(enabled)Modifier.clickable{
                                    if(day.workouts.size==1)selected=day.workouts.single() else selectedDayWorkouts=day.workouts
                                } else Modifier),
                                shape=RoundedCornerShape(10.dp),
                                colors=CardDefaults.cardColors(containerColor=if(day.isToday)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            ) {
                                Column(Modifier.fillMaxSize().padding(5.dp)) {
                                    Text(java.time.LocalDate.ofEpochDay(day.epochDay).dayOfMonth.toString(),fontSize=12.sp,fontWeight=if(day.isToday)FontWeight.Bold else FontWeight.Normal,color=if(day.inMonth)MaterialTheme.colorScheme.onSurface else Color.Gray)
                                    Spacer(Modifier.weight(1f))
                                    day.workouts.take(3).forEach { workout ->
                                        Text(calendarStateMarker(workout.state),fontSize=10.sp,fontWeight=FontWeight.Bold,color=calendarStateColor(workout.state),maxLines=1)
                                    }
                                    if(day.workouts.size>3)Text("+"+(day.workouts.size-3),fontSize=9.sp,color=Color.Gray)
                                }
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        calendarLegendStates.forEach{state->
                            Text(calendarStateMarker(state)+" "+calendarStateLabel(state),fontSize=8.sp,color=calendarStateColor(state),modifier=Modifier.weight(1f),maxLines=1)
                        }
                    }
                }
            }
        }
    }
    if(selectedDayWorkouts.isNotEmpty()) {
        AlertDialog(onDismissRequest={selectedDayWorkouts=emptyList()},title={Text("Sessioni del giorno")},text={
            Column { selectedDayWorkouts.forEach { workout ->
                TextButton(onClick={selected=workout;selectedDayWorkouts=emptyList()},modifier=Modifier.fillMaxWidth()) {
                    Text("Sessione "+(workout.sequenceInWeek+1)+" • "+calendarStateLabel(workout.state),modifier=Modifier.fillMaxWidth())
                }
            }}
        },confirmButton={},dismissButton={TextButton(onClick={selectedDayWorkouts=emptyList()}){Text("CHIUDI")}})
    }
    if(editPlan) {
        val current=plan?.let(PlanConfiguration::fromPlan) ?: PlanConfiguration.defaults()
        var goal by remember { mutableStateOf(current.goal) }
        var days by remember { mutableIntStateOf(current.daysPerWeek) }
        val weekdays=remember { mutableStateListOf<Int>().apply{addAll(current.trainingDays.sorted())} }
        val skills=remember { mutableStateListOf<String>().apply{addAll(current.prioritySkills)} }
        AlertDialog(onDismissRequest={editPlan=false},title={Text(if(plan==null)"Crea piano" else "Modifica programma")},text={LazyColumn(Modifier.heightIn(max=520.dp)){item{
            Text("Obiettivo",fontWeight=FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Forza","Ipertrofia","Calisthenics","Fitness generale").forEach{g->FilterChip(selected=goal==g,onClick={goal=g},label={Text(g,fontSize=10.sp)})}}
            Text("Allenamenti a settimana: $days",fontWeight=FontWeight.SemiBold)
            Row{OutlinedButton(onClick={if(days>1){days--;while(weekdays.size>days)weekdays.removeAt(weekdays.lastIndex)}}){Text("−")};Spacer(Modifier.width(8.dp));Button(onClick={if(days<7)days++}){Text("+")}}
            Text("Giorni",fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=8.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)){listOf("L","M","M","G","V","S","D").forEachIndexed{index,label->val day=index+1;FilterChip(selected=day in weekdays,onClick={if(day in weekdays)weekdays.remove(day) else if(weekdays.size<days)weekdays.add(day)},label={Text(label)})}}
            Text("Skill prioritarie (opzionali)",fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=8.dp));calisthenicsSkills.take(12).forEach{s->FilterChip(selected=s.name in skills,onClick={if(s.name in skills)skills.remove(s.name)else skills.add(s.name)},label={Text(s.name)})}
            Text(if(plan==null)"Il piano viene creato come intent: gli esercizi saranno definiti al momento dell'avvio in base al luogo scelto." else "Le sessioni già eseguite e quelle passate non verranno modificate.",fontSize=12.sp,color=Color.Gray,modifier=Modifier.padding(top=10.dp))
        }}},confirmButton={Button(onClick={
            if(weekdays.size==days){
                val candidate=PlanConfiguration(goal,current.weeks,days,weekdays.toSet(),skills.toList()).validated()
                if(plan==null){
                    val created=generateTrainingPlan("plan-"+today+"-"+java.util.UUID.randomUUID(),today,candidate)
                    if(store.saveTrainingPlan(created) && store.setTrainingPlanEnabled(created.id,true)){plans=store.loadTrainingPlans();enabledPlanIds=store.loadEnabledTrainingPlanIds();plan=created;planSaveError=false;editPlan=false}else planSaveError=true
                } else { confirmRegeneration=candidate;editPlan=false }
            }
        },enabled=weekdays.size==days){Text(if(plan==null)"CREA PIANO" else "RIVEDI MODIFICHE")}},dismissButton={TextButton(onClick={editPlan=false}){Text("ANNULLA")}})
    }
    confirmRegeneration?.let { candidateConfig ->
        val current=plan!!
        val protected=current.workouts.count{it.epochDay<today || it.state !in setOf(PlannedWorkoutState.PLANNED,PlannedWorkoutState.RESCHEDULED)}
        val replaceable=current.workouts.size-protected
        AlertDialog(onDismissRequest={confirmRegeneration=null},title={Text("Rigenerare le sessioni future?")},
            text={Text("Verranno sostituite solo le sessioni future modificabili ($replaceable attuali). Storico, sessioni completate/parziali e dati di esecuzione restano invariati.")},
            confirmButton={Button(onClick={val candidate=regenerateFuturePlan(current,candidateConfig,today);if(persistCandidate(candidate))confirmRegeneration=null}){Text("CONFERMA RIGENERAZIONE")}},
            dismissButton={TextButton(onClick={confirmRegeneration=null}){Text("ANNULLA")}})
    }
    selected?.let { workout ->
        val historical=workout.state==PlannedWorkoutState.COMPLETED || workout.state==PlannedWorkoutState.PARTIAL
        val actions=calendarWorkoutActions(workout)
        val actual=if(historical)store.loadSessions().firstOrNull{it.plannedWorkoutId==workout.id}else null
        AlertDialog(onDismissRequest={selected=null},title={Text(if(historical)"Allenamento eseguito" else "Gestisci sessione")},text={
            LazyColumn(Modifier.heightIn(max=480.dp)) {
                item {
                    Text("Pianificato: "+calendarDate(workout.originalEpochDay))
                    if(workout.epochDay!=workout.originalEpochDay)Text("Riprogrammato: "+calendarDate(workout.epochDay))
                    workout.actualExecutionEpochDay?.let{Text("Eseguito: "+calendarDate(it))}
                    Spacer(Modifier.height(12.dp))
                }
                if(historical){
                    if(actual==null)item{Text("Dettagli della sessione non disponibili nello storico.",color=Color.Gray)}
                    else {
                        item{Text("Durata: "+(actual.durationSec/60)+" min • "+actual.completedSets+"/"+actual.totalSets+" serie",fontWeight=FontWeight.SemiBold)}
                        items(actual.structuredExercises){exercise->
                            Column(Modifier.padding(vertical=6.dp)){
                                Text(exercise.exerciseName,fontWeight=FontWeight.SemiBold)
                                exercise.sets.filter{it.completed}.forEach{set->
                                    val result=listOfNotNull(set.actualReps?.let{"$it rip."},set.actualSeconds?.let{"$it s"},set.actualLoadKg?.let{"$it kg"},set.restUsedSec?.let{"recupero $it s"}).joinToString(" • ")
                                    Text("Serie "+(set.index+1)+": "+result,fontSize=12.sp)
                                }
                            }
                        }
                        if(actual.skippedExercises.isNotEmpty())item{Text("Saltati: "+actual.skippedExercises.joinToString(),fontSize=12.sp)}
                    }
                } else {
                    item {
                        if(planSaveError) Text("Impossibile salvare la modifica. Riprova.",color=Color.Red,fontSize=12.sp,modifier=Modifier.padding(bottom=8.dp))
                        Text(workout.intent?.let{intent->"Obiettivo: "+intent.goal+(if(intent.prioritySkills.isEmpty())"" else "\nSkill: "+intent.prioritySkills.joinToString())} ?: "Sessione priva di intent")
                        Spacer(Modifier.height(12.dp))
                        Text("Riprogramma di:",fontWeight=FontWeight.SemiBold)
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            IconButton(onClick={rescheduleOffset=(rescheduleOffset-1).coerceAtLeast(1)}){Icon(Icons.Default.Remove,"Riduci")}
                            Text("+$rescheduleOffset giorni",fontWeight=FontWeight.Bold)
                            IconButton(onClick={rescheduleOffset=(rescheduleOffset+1).coerceAtMost(14)}){Icon(Icons.Default.Add,"Aumenta")}
                        }
                    }
                }
            }
        },confirmButton={
            if(historical)TextButton(onClick={selected=null}){Text("CHIUDI")}
            else TextButton(onClick={
                val candidate=TrainingPlanEngine.reschedule(plan!!,workout.id,workout.epochDay+rescheduleOffset,minimumGapDays=0)
                if(persistCandidate(candidate)){ selected=null;rescheduleOffset=1 }
            },enabled=actions.canReschedule){Text("RIPROGRAMMA")}
        },dismissButton={
            if(!historical)Row {
                TextButton(onClick={ selected=null; onStartWorkout(workout) },enabled=actions.canStart){Text("INIZIA")}
                TextButton(onClick={
                    val candidate=TrainingPlanEngine.skip(plan!!,workout.id)
                    if(persistCandidate(candidate)) selected=null
                },enabled=actions.canSkip){Text("SALTA")}
                if(actions.canRestoreOriginalDate) TextButton(onClick={
                    val candidate=TrainingPlanEngine.restoreOriginalDate(plan!!,workout.id,minimumGapDays=0)
                    if(persistCandidate(candidate)){selected=null;rescheduleOffset=1}
                }){Text("RIPRISTINA DATA ORIGINALE")}
                TextButton(onClick={selected=null}){Text("ANNULLA")}
            }
        })
    }
    if(showPlans) {
        AlertDialog(onDismissRequest={showPlans=false},title={Text("I miei piani")},text={
            LazyColumn(Modifier.heightIn(max=480.dp)) {
                if(plans.isEmpty())item{Text("Nessun piano salvato.",color=Color.Gray)}
                items(plans){saved->
                    Card(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{plan=saved;showPlans=false}) {
                        Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(saved.goal,fontWeight=FontWeight.Bold)
                                Text(saved.weeks.toString()+" settimane • "+saved.daysPerWeek+" sessioni/settimana",fontSize=12.sp,color=Color.Gray)
                                Text(if(saved.id in enabledPlanIds)"ATTIVO • influenza Coach" else "DISATTIVATO",color=if(saved.id in enabledPlanIds)Accent else Color.Gray,fontSize=11.sp,fontWeight=FontWeight.Bold)
                            }
                            Switch(checked=saved.id in enabledPlanIds,onCheckedChange={enabled->
                                if(store.setTrainingPlanEnabled(saved.id,enabled)){
                                    enabledPlanIds=store.loadEnabledTrainingPlanIds()
                                    // Enabling a plan changes Coach participation only; UI selection is independent.
                                }
                            })
                        }
                    }
                }
            }
        },confirmButton={TextButton(onClick={showPlans=false}){Text("CHIUDI")}})
    }
    confirmDelete?.let { doomed ->
        AlertDialog(onDismissRequest={confirmDelete=null},title={Text("Eliminare questo piano?")},
            text={Text("Le sessioni future di questo piano verranno eliminate. Gli allenamenti già eseguiti resteranno nello storico.")},
            confirmButton={Button(onClick={
                if(store.deleteTrainingPlan(doomed.id)){
                    plans=store.loadTrainingPlans()
                    enabledPlanIds=store.loadEnabledTrainingPlanIds()
                    plan=plans.firstOrNull()
                    confirmDelete=null
                } else planSaveError=true
            }){Text("ELIMINA PIANO")}},
            dismissButton={TextButton(onClick={confirmDelete=null}){Text("ANNULLA")}})
    }

}

@Composable
private fun RecoveryChart(sessions: List<SessionRecord>) {
    val scores = remember(sessions) { recoveryScores(sessions) }
    Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("Stima recupero muscolare", fontWeight = FontWeight.SemiBold)
            Text("Basata su allenamenti completati e tempo trascorso", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
            recoveryGroups.forEach { group ->
                val value = scores[group] ?: 100
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(group); Text("$value%", fontWeight = FontWeight.Bold, color = Accent)
                }
                LinearProgressIndicator(progress = { value / 100f }, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
            }
            Text("Indicatore orientativo: non è una misura fisiologica o medica.", color = Color.Gray, fontSize = 11.sp)
        }
    }
}
@Composable private fun SectionTitle(s: String) { Text(s, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 10.dp)) }
@Composable private fun ExerciseRow(a: String, b: String) { Card(Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.FitnessCenter, null, tint = Accent); Spacer(Modifier.width(14.dp)); Column { Text(a, fontWeight = FontWeight.SemiBold); Text(b, color = Color.Gray, fontSize = 13.sp) } } } }

@Composable
private fun Library(profile: TrainingLocation, athlete: AthleteProfile, sessions: List<SessionRecord>, store:AppStore, onExecuteExercise:(Exercise)->Unit, onSessionSaved: (SessionRecord) -> Boolean) {
    var selectedSkill by remember { mutableStateOf<String?>(null) }
    var skillWorkout by remember { mutableStateOf<String?>(null) }
    var filteredWorkout by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Exercise?>(null) }
    val equipmentFilters = remember { mutableStateListOf<String>() }
    val groupFilters = remember { mutableStateListOf<String>() }
    val skillFilters = remember { mutableStateListOf<String>() }
    var filterDialog by remember { mutableStateOf<String?>(null) }
    val equipmentOptions = exerciseEquipmentOptions(exercises)
    val matchingSkills = calisthenicsSkills.filter { s -> query.isNotBlank() && s.name.contains(query.trim(),true) }
    val shown = exercises.filter { ex ->
        (query.isBlank() || query.trim().split(Regex("\\s+")).all { token -> ex.name.contains(token,true) || ex.group.contains(token,true) || ex.equipment.contains(token,true) || ex.skills.any{it.contains(token,true)} }) &&
        (equipmentFilters.isEmpty() || equipmentFilters.any { eq -> EquipmentCatalog.fromDisplayName(eq)?.let { it in EquipmentCatalog.requirementsFor(ex) } == true }) &&
        (groupFilters.isEmpty() || groupFilters.any { ex.group.contains(it,true) }) &&
        (skillFilters.isEmpty() || skillFilters.any { it in ex.skills })
    }
    Column {
        Header("Libreria esercizi", exercises.size.toString()+" esercizi • "+calisthenicsSkills.size+" skill calisthenics")
        OutlinedTextField(query,{query=it},leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Ricerca istantanea: esercizio, muscolo, attrezzo, skill")},modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth(),shape=RoundedCornerShape(16.dp))
        Row(Modifier.padding(horizontal=20.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)){
            FilterChip(selected=equipmentFilters.isNotEmpty(),onClick={filterDialog="equipment"},label={Text("Attrezzi"+if(equipmentFilters.isEmpty())"" else " ("+equipmentFilters.size+")")})
            FilterChip(selected=groupFilters.isNotEmpty(),onClick={filterDialog="groups"},label={Text("Muscoli"+if(groupFilters.isEmpty())"" else " ("+groupFilters.size+")")})
            FilterChip(selected=skillFilters.isNotEmpty(),onClick={filterDialog="skills"},label={Text("Skill"+if(skillFilters.isEmpty())"" else " ("+skillFilters.size+")")})
        }
        if(equipmentFilters.isNotEmpty()||groupFilters.isNotEmpty()||skillFilters.isNotEmpty()) {
 Row(Modifier.padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically){Text(shown.size.toString()+" risultati",color=Color.Gray,modifier=Modifier.weight(1f));TextButton(onClick={equipmentFilters.clear();groupFilters.clear();skillFilters.clear()}){Text("AZZERA")}}
 Button(onClick={filteredWorkout=true},enabled=shown.isNotEmpty(),modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth()){Text("GENERA WORKOUT DAI FILTRI")}
}
        LazyColumn(contentPadding=PaddingValues(horizontal=20.dp,vertical=6.dp)){
            items(matchingSkills){s->Card(Modifier.fillMaxWidth().padding(bottom=10.dp).clickable{selectedSkill=s.name}){Column(Modifier.padding(16.dp)){Text(s.name,fontSize=18.sp,fontWeight=FontWeight.Bold);Text("Skill calisthenics • "+s.category+" • "+s.level,color=Accent)}}}
            items(shown){ex->Card(Modifier.fillMaxWidth().padding(bottom=10.dp).clickable{selected=ex}){Column(Modifier.padding(16.dp)){Text(ex.name,fontSize=18.sp,fontWeight=FontWeight.Bold);Text(ex.group+" • "+ex.equipment+" • "+ex.level,color=Color.Gray);if(ex.skills.isNotEmpty())Text(ex.role+" → "+ex.skills.joinToString(" • "),color=Accent,fontSize=12.sp,modifier=Modifier.padding(top=6.dp))}}}
            if(shown.isEmpty()) item{Text("Nessun esercizio corrisponde ai filtri selezionati.",color=Color.Gray,modifier=Modifier.padding(12.dp))}
        }
    }
    filterDialog?.takeIf{it!="types"}?.let { type ->
        val title=when(type){"equipment"->"Filtra per attrezzatura";"groups"->"Filtra per gruppi muscolari";else->"Filtra per skill"}
        val opts=when(type){"equipment"->equipmentOptions;"groups"->recoveryGroups;else->calisthenicsSkills.map{it.name}}
        val target=when(type){"equipment"->equipmentFilters;"groups"->groupFilters;else->skillFilters}
        AlertDialog(onDismissRequest={filterDialog=null},title={Text(title)},text={LazyColumn(Modifier.heightIn(max=520.dp)){items(opts){o->Row(Modifier.fillMaxWidth().clickable{if(o in target)target.remove(o) else target.add(o)},verticalAlignment=Alignment.CenterVertically){Checkbox(o in target,{checked->if(checked){if(o !in target)target.add(o)}else target.remove(o)});Column(Modifier.clickable(enabled=type=="skills"){if(type=="skills"){selectedSkill=o;filterDialog=null}}){Text(o);if(type=="skills")calisthenicsSkills.firstOrNull{it.name==o}?.let{Text(it.category+" • "+it.level,color=Color.Gray,fontSize=12.sp)}}}}}},confirmButton={Button(onClick={filterDialog=null}){Text("APPLICA")}},dismissButton={TextButton(onClick={target.clear()}){Text("AZZERA")}})
    }
    selected?.let{ex->ExerciseDetailDialog(ex=ex,sessions=sessions,onDismiss={selected=null},onSkillClick={skill->selected=null;selectedSkill=skill},onExecute={selected=null;onExecuteExercise(ex)})}
    selectedSkill?.let { skill -> SkillDetailDialog(skill, athlete, { selectedSkill=null }, { skillWorkout=skill; selectedSkill=null }, sessions) }
    skillWorkout?.let { skill -> CustomWorkoutDialog(profile, athlete, sessions, { skillWorkout=null }, { record -> val saved=onSessionSaved(record);if(saved)skillWorkout=null;saved }, initialSkill=skill,store=store) }
    if(filteredWorkout) CustomWorkoutDialog(profile,athlete,sessions,{filteredWorkout=false},{record->val saved=onSessionSaved(record);if(saved)filteredWorkout=false;saved},initialEquipment=equipmentFilters.toList(),initialGroups=groupFilters.toList(),initialSkill=skillFilters.firstOrNull()?:"Nessuna",filtersLocked=true,store=store)
}
@Composable
private fun SkillDetailDialog(skillName: String, athlete: AthleteProfile, onDismiss: () -> Unit, onGenerate: () -> Unit, sessions:List<SessionRecord>) {
    val states=SkillCatalog.progress(skillName,sessions)
    val percent=ProgressionEngine.skillProgress(states)
    AlertDialog(onDismissRequest=onDismiss,title={Text(skillName)},text={LazyColumn(Modifier.heightIn(max=560.dp)){
        item{
            Text("$percent% del percorso consolidato",fontWeight=FontWeight.Bold,color=Accent)
            LinearProgressIndicator(progress={percent/100f},modifier=Modifier.fillMaxWidth().padding(vertical=12.dp))
            Text("Gli sblocchi richiedono prestazioni registrate, non solo il livello del profilo.",fontSize=12.sp,color=Color.Gray)
        }
        items(states){state->
            val node=state.node
            val criterion=node.criterion
            Card(Modifier.fillMaxWidth().padding(vertical=5.dp)){
                Column(Modifier.padding(14.dp)){
                    Text(node.exerciseName,fontWeight=FontWeight.Bold)
                    Text(when(state.status){ProgressionStatus.LOCKED->"BLOCCATA";ProgressionStatus.AVAILABLE->"DISPONIBILE";ProgressionStatus.IN_PROGRESS->"IN CORSO";ProgressionStatus.MASTERED->"CONSOLIDATA"},color=if(state.status==ProgressionStatus.LOCKED)Color.Gray else Accent,fontSize=11.sp)
                    if(node.prerequisites.isNotEmpty())Text("Prerequisiti: "+node.prerequisites.map{key->states.firstOrNull{it.node.id==key}?.node?.exerciseName?:key}.joinToString(),fontSize=12.sp)
                    val target=criterion.minSeconds?.let{"$it secondi"}?:criterion.minReps?.let{"$it ripetizioni"}?:"target registrato"
                    Text("${criterion.minSets} serie × $target • RPE ≤ ${criterion.maxRpe?:10}",fontSize=12.sp)
                    Text("${criterion.evidenceClass}: ${criterion.evidenceNote}",fontSize=11.sp,color=Color.Gray)
                    Text("${(state.completion*criterion.requiredSessions).toInt()} / ${criterion.requiredSessions} sessioni valide",fontSize=12.sp,color=Color.Gray)
                    LinearProgressIndicator(progress={state.completion.toFloat()},modifier=Modifier.fillMaxWidth().padding(top=6.dp))
                }
            }
        }
    }},confirmButton={Button(onClick=onGenerate){Text("ALLENA QUESTA SKILL")}},dismissButton={TextButton(onClick=onDismiss){Text("CHIUDI")}})
}

@Composable
private fun Equipment(active: TrainingLocation, customProfiles: List<TrainingLocation>, select: (TrainingLocation) -> Unit, addProfile: (TrainingLocation) -> Unit, editProfile: (TrainingLocation,TrainingLocation)->Unit, deleteProfile:(TrainingLocation)->Unit) {
    var showCreator by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TrainingLocation?>(null) }
    var deleting by remember { mutableStateOf<TrainingLocation?>(null) }
    Column {
        Header("Profili attrezzatura", "Scegli dove ti alleni oggi")
        customProfiles.forEach { p ->
                        Card(Modifier.padding(horizontal=20.dp,vertical=6.dp).fillMaxWidth().clickable{select(p)},colors=CardDefaults.cardColors(containerColor=if(p==active)Accent.copy(alpha=.10f) else Color.White)){
                Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){
                    Text(p.icon,fontSize=32.sp);Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)){Text(p.name,fontSize=19.sp,fontWeight=FontWeight.Bold);Text(p.capabilities.map(EquipmentCatalog::displayName).sortedBy{it.lowercase()}.joinToString(" • "),color=Color.Gray,fontSize=13.sp)}
                    IconButton(onClick={editing=p}){Icon(Icons.Default.Edit,"Modifica")};IconButton(onClick={deleting=p}){Icon(Icons.Default.Delete,"Elimina")}
                    if(p==active)Icon(Icons.Default.CheckCircle,"Attivo",tint=Accent)
                }
            }
        }
        if(customProfiles.isEmpty()) Text("Nessun profilo attrezzatura. Creane uno per generare workout.",color=Accent,modifier=Modifier.padding(horizontal=20.dp));OutlinedButton(onClick={showCreator=true},modifier=Modifier.padding(20.dp).fillMaxWidth()){Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text("NUOVO PROFILO")}
    }
    if(showCreator) TrainingLocationDialog(null,{showCreator=false}){created->addProfile(created);showCreator=false}
    editing?.let{old->TrainingLocationDialog(old,{editing=null}){updated->editProfile(old,updated);editing=null}}
    deleting?.let{p->AlertDialog(onDismissRequest={deleting=null},title={Text("Elimina profilo?")},text={Text("Vuoi eliminare "+p.name+"?")},confirmButton={Button(onClick={deleteProfile(p);deleting=null}){Text("ELIMINA")}},dismissButton={TextButton(onClick={deleting=null}){Text("ANNULLA")}})}
}
@Composable
private fun Progress(athlete: AthleteProfile, sessions: List<SessionRecord>, onAthleteSave: (AthleteProfile) -> Unit, onDeleteSession: (Long) -> Unit, showAthleteProfile:Boolean=true) {
    var pendingDeletion by remember { mutableStateOf<SessionRecord?>(null) }
    var edit by remember { mutableStateOf(false) }
    var selectedSession by remember { mutableStateOf<SessionRecord?>(null) }
    Column {
        Header("Profilo e progressi", "Dati salvati sul dispositivo")
        if(showAthleteProfile) {
        Card(Modifier.padding(horizontal=20.dp).fillMaxWidth().clickable { edit=true }) {
            Column(Modifier.padding(18.dp)) {
                Text(if(athlete.name.isBlank()) "Configura profilo atleta" else athlete.name, fontSize=20.sp, fontWeight=FontWeight.Bold)
                val capability=athlete.toCapabilityProfile()
                Text(if(capability.assessed) "Capacità • Esperienza "+capability.trainingExperience+"/3 • Push "+capability.pushCapacity+"/3 • Pull "+capability.pullCapacity+"/3" else "Capacità: valutazione da completare", color=Color.Gray);if(athlete.heightCm>0&&athlete.weightKg>0){val bmi=athlete.weightKg/((athlete.heightCm/100f)*(athlete.heightCm/100f));Text(String.format(java.util.Locale.ITALY,"%.1f kg • %.0f cm • BMI %.1f",athlete.weightKg,athlete.heightCm,bmi),color=Accent,fontSize=13.sp)}
                Text("TOCCA PER MODIFICARE", color=Accent, fontSize=12.sp, modifier=Modifier.padding(top=8.dp))
            }
        }
        }
        SectionTitle("Storico allenamenti")
        if(sessions.isEmpty()) Text("Completa un allenamento per iniziare lo storico.", color=Color.Gray, modifier=Modifier.padding(horizontal=20.dp))
        else LazyColumn(Modifier.padding(horizontal=20.dp).weight(1f,false)){items(sessions){s->Card(Modifier.fillMaxWidth().padding(bottom=8.dp).clickable{selectedSession=s}){Column(Modifier.padding(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(s.profileName,fontWeight=FontWeight.Bold);Text(s.completedSets.toString()+"/"+s.totalSets+" serie • "+java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",java.util.Locale.ITALY).format(java.util.Date(s.timestamp)),color=Color.Gray,fontSize=12.sp);Text("TOCCA PER DETTAGLI",color=Accent,fontSize=11.sp)};TextButton(onClick={pendingDeletion=s}){Text("ELIMINA")}}}}}}
    }
    pendingDeletion?.let { doomed ->
        AlertDialog(onDismissRequest={pendingDeletion=null},title={Text("Eliminare questo allenamento?")},
            text={Text("La rimozione aggiorna progressi, Skill Tree e Adaptive Coach. L'eventuale sessione collegata torna disponibile nel programma.")},
            confirmButton={TextButton(onClick={onDeleteSession(doomed.timestamp);pendingDeletion=null;selectedSession=null}){Text("ELIMINA")}},
            dismissButton={TextButton(onClick={pendingDeletion=null}){Text("ANNULLA")}})
    }
    if(edit) AthleteDialog(athlete,{edit=false}) { onAthleteSave(it); edit=false }
    selectedSession?.let { s ->
        AlertDialog(onDismissRequest={selectedSession=null},title={Text("Dettaglio allenamento")},text={LazyColumn(Modifier.heightIn(max=500.dp)){
            item{Text(s.profileName,fontWeight=FontWeight.Bold);Text(java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",java.util.Locale.ITALY).format(java.util.Date(s.timestamp)),color=Color.Gray);Text(s.completedSets.toString()+"/"+s.totalSets+" serie completate",color=Accent,modifier=Modifier.padding(vertical=8.dp));if(s.workoutRpe>0)Text("Fatica workout: "+s.workoutRpe+"/10",fontWeight=FontWeight.Bold);if(s.durationSec>0)Text("Durata effettiva: "+String.format("%02d:%02d",s.durationSec/60,s.durationSec%60));Text("Workout programmato / eseguito",fontWeight=FontWeight.Bold)}
            items(historyExerciseDetails(s)){detail->
                Card(Modifier.fillMaxWidth().padding(vertical=3.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Text(detail.name+(detail.completedSets?.let{" • $it"+(detail.totalSets?.let{"/$it"}?:"")+" serie completate"}?:""),fontWeight=FontWeight.Bold)
                        detail.lines.forEach { Text(it,fontSize=13.sp) }
                    }
                }
            }
        }},confirmButton={TextButton(onClick={selectedSession=null}){Text("CHIUDI")}},dismissButton={TextButton(onClick={pendingDeletion=s}){Text("ELIMINA SESSIONE")}})
    }
}

@Composable
private fun AthleteDialog(current:AthleteProfile,onDismiss:()->Unit,onSave:(AthleteProfile)->Unit){
    var name by remember{mutableStateOf(current.name)}
    var age by remember{mutableStateOf(if(current.age>0)current.age.toString() else "")}
    var height by remember{mutableStateOf(if(current.heightCm>0)current.heightCm.toString() else "")}
    var weight by remember{mutableStateOf(if(current.weightKg>0)current.weightKg.toString() else "")}
    var sex by remember{mutableStateOf(current.sex)}
    var assessedExperience by remember{mutableStateOf(current.assessedTrainingExperience)}
    var assessedPush by remember{mutableStateOf(current.assessedPushCapacity)}
    var assessedPull by remember{mutableStateOf(current.assessedPullCapacity)}
    var assessmentOpen by remember{mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Profilo atleta")},text={LazyColumn(Modifier.heightIn(max=520.dp)){
        item{
            OutlinedTextField(name,{name=it},label={Text("Nome")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(age,{age=it.filter{ch->ch.isDigit()}},label={Text("Età")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(height,{height=it.replace(",",".").filter{ch->ch.isDigit()||ch=='.'}},label={Text("Altezza (cm)")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(weight,{weight=it.replace(",",".").filter{ch->ch.isDigit()||ch=='.'}},label={Text("Peso (kg)")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Text("Sesso (opzionale)",fontWeight=FontWeight.Bold)
            Row{listOf("Non specificato","Uomo","Donna").forEach{x->FilterChip(sex==x,{sex=x},{Text(x)},Modifier.padding(end=4.dp))}}
            if((height.toFloatOrNull()?:0f)>0&&(weight.toFloatOrNull()?:0f)>0){val hm=(height.toFloat()/100f);Text("BMI: "+String.format(java.util.Locale.ITALY,"%.1f",weight.toFloat()/(hm*hm)),color=Accent)}
            Text("Capacità atleta",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp))
            if(listOf(assessedExperience,assessedPush,assessedPull).all{it!=null})
                Text("Esperienza "+assessedExperience+"/3 • Push "+assessedPush+"/3 • Pull "+assessedPull+"/3",color=Accent)
            else Text("Valutazione non completata: il generatore resta conservativo.",color=Color.Gray)
            if(assessmentOpen){
                var months by remember{mutableStateOf("")}
                var pushUps by remember{mutableStateOf("")}
                var pullUps by remember{mutableStateOf("")}
                Text("Valutazione capacità",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp))
                Text("Test interno orientativo: esperienza e capacità attuali. Il risultato viene usato per limitare la difficoltà del piano.",color=Color.Gray,fontSize=12.sp)
                OutlinedTextField(months,{months=it.filter(Char::isDigit)},label={Text("Mesi di allenamento")},singleLine=true,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(pushUps,{pushUps=it.filter(Char::isDigit)},label={Text("Push-up consecutivi")},singleLine=true,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(pullUps,{pullUps=it.filter(Char::isDigit)},label={Text("Trazioni consecutive")},singleLine=true,modifier=Modifier.fillMaxWidth())
                Button(onClick={
                    val capability=capabilityFromAssessment(FitnessLevelAssessment(months.toIntOrNull()?:0,pushUps.toIntOrNull()?:0,pullUps.toIntOrNull()?:0))
                    assessedExperience=capability.trainingExperience
                    assessedPush=capability.pushCapacity
                    assessedPull=capability.pullCapacity
                    assessmentOpen=false
                },modifier=Modifier.fillMaxWidth()){Text("CALCOLA CAPACITÀ")}
            } else {
                OutlinedButton(onClick={assessmentOpen=true},modifier=Modifier.fillMaxWidth()){Text("VALUTA CAPACITÀ")}
            }
            Text("Obiettivo, skill e giorni di allenamento si configurano nel programma multi-settimana.",color=Color.Gray,fontSize=12.sp,modifier=Modifier.padding(top=10.dp))
        }
    }},confirmButton={Button(onClick={onSave(current.copy(name=name.trim(),age=age.toIntOrNull()?:0,heightCm=height.toFloatOrNull()?:0f,weightKg=weight.toFloatOrNull()?:0f,sex=sex,assessedTrainingExperience=assessedExperience,assessedPushCapacity=assessedPush,assessedPullCapacity=assessedPull))}){Text("SALVA")}},dismissButton={TextButton(onClick=onDismiss){Text("ANNULLA")}})
}
@Composable
private fun ExerciseExecutionInputs(ex:Exercise, enabled:Boolean, recommendation:LoadRecommendation?=null, targetSet:WorkoutSetState?=null, onComplete:(Int?,Int?,Double?,Double?,Double?)->Unit) {
    val p=prescriptionFor(ex)
    val inputTarget=runnerInputTarget(ex,targetSet)
    var value by remember(ex.name,inputTarget){mutableStateOf(inputTarget.toString())}
    var load by remember(ex.name,recommendation?.loadKg){mutableStateOf(recommendation?.loadKg?.toString()?:"")}
    var assistance by remember(ex.name,recommendation?.assistanceKg){mutableStateOf(recommendation?.assistanceKg?.toString()?:"")}
    Column {
        recommendation?.let { rec -> if(ex.resistanceMode==ResistanceMode.EXTERNAL_LOAD||ex.resistanceMode==ResistanceMode.ASSISTED) Text((if(rec.loadKg!=null)"Carico consigliato: ${rec.loadKg} kg" else if(rec.assistanceKg!=null)"Assistenza consigliata: ${rec.assistanceKg} kg" else "Calibrazione iniziale")+ " • "+rec.reason,color=Accent,fontSize=12.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(bottom=4.dp)) }
        val label=when(ex.measure){ExerciseMeasure.REPS->if(ex.laterality==Laterality.UNILATERAL)"Ripetizioni per lato" else "Ripetizioni";ExerciseMeasure.SECONDS->"Secondi mantenuti";ExerciseMeasure.DURATION->"Secondi eseguiti";ExerciseMeasure.DISTANCE->"Distanza (m)";ExerciseMeasure.INTERVALS->"Secondi lavoro"}
        OutlinedTextField(value,{value=it.filter{ch->ch.isDigit()||ch=='.'}},label={Text(label)},singleLine=true,enabled=enabled,modifier=Modifier.fillMaxWidth())
        if(ex.laterality==Laterality.UNILATERAL||ex.laterality==Laterality.ALTERNATING) Text(if(ex.laterality==Laterality.UNILATERAL)"Valore riferito a ciascun lato" else "Alternare i lati",color=Color.Gray,fontSize=11.sp)
        if(ex.resistanceMode==ResistanceMode.EXTERNAL_LOAD) OutlinedTextField(load,{load=it.filter{ch->ch.isDigit()||ch=='.'}},label={Text("Carico kg")},singleLine=true,enabled=enabled,modifier=Modifier.fillMaxWidth())
        if(ex.resistanceMode==ResistanceMode.ASSISTED) OutlinedTextField(assistance,{assistance=it.filter{ch->ch.isDigit()||ch=='.'}},label={Text("Assistenza kg")},singleLine=true,enabled=enabled,modifier=Modifier.fillMaxWidth())
        if(ex.measure==ExerciseMeasure.INTERVALS) Text("Recupero intervallo: "+(ex.intervalRestSec?:ex.setRestSec)+" sec",color=Accent,fontSize=12.sp)
        Button(onClick={
            val v=value.toDoubleOrNull()?:inputTarget
            onComplete(if(ex.measure==ExerciseMeasure.REPS)v.toInt() else null,if(ex.measure==ExerciseMeasure.SECONDS||ex.measure==ExerciseMeasure.DURATION||ex.measure==ExerciseMeasure.INTERVALS)v.toInt() else null,if(ex.measure==ExerciseMeasure.DISTANCE)v else null,load.toDoubleOrNull(),assistance.toDoubleOrNull())
        },enabled=enabled,modifier=Modifier.fillMaxWidth().padding(top=4.dp)){Text("COMPLETA SERIE")}
    }
}

@Composable
private fun CustomWorkoutDialog(profile: TrainingLocation, athlete: AthleteProfile, sessions: List<SessionRecord>, onDismiss: () -> Unit, onSave: (SessionRecord) -> Boolean, initialSkill: String = "Nessuna", initialEquipment: List<String> = emptyList(), initialGroups: List<String> = emptyList(), filtersLocked:Boolean=false, store:AppStore?=null, restored:WorkoutSessionState?=null) {
    val liveReadiness=LocalHealthConnect.current.readiness
    val readinessScore=liveReadiness
    var minutes by remember { mutableIntStateOf(45) }
    val initialLocation=remember(profile,restored?.trainingLocationId){
        restored?.trainingLocationId?.let{id->(trainingLocations+profile).firstOrNull{it.id==id}} ?: profile
    }
    val locationOptions=remember(profile){(trainingLocations+profile).distinctBy{it.id}}
    var selectedLocation by remember(profile){mutableStateOf(initialLocation)}
    val selectedGroups = remember { mutableStateListOf<String>().apply { addAll(initialGroups) } }
    var mode by remember(initialSkill,initialGroups) { mutableStateOf(if(initialSkill!="Nessuna") "Skill" else "Parti del corpo") }
    val selectedSkills = remember(initialSkill) { mutableStateListOf<String>().apply { if(initialSkill!="Nessuna") add(initialSkill) } }
    var generated by remember { mutableStateOf(restored?.let{restoreExercises(it)} ?: emptyList()) }
    var running by remember { mutableStateOf(restored!=null) }
    var sessionStartedAt by remember { mutableStateOf<Long?>(restored?.startedAtEpochMs) }
    val sessionId = remember { restored?.id ?: "custom-"+System.currentTimeMillis() }
    var paused by remember { mutableStateOf(restored?.pausedAtEpochMs!=null) }
    var elapsedSeconds by remember { mutableIntStateOf(restored?.let{elapsedWorkoutSeconds(it,System.currentTimeMillis())}?:0) }
    var restRemaining by remember { mutableIntStateOf(restored?.pausedRestRemainingSec ?: remainingSeconds(restored?.restEndsAtEpochMs)) }
    var restEndsAt by remember { mutableStateOf<Long?>(restored?.restEndsAtEpochMs) }
    var exerciseRestSeconds by remember { mutableIntStateOf(180) }
    var restKind by remember { mutableStateOf(restored?.restKind?:"Serie") }
    var currentExercise by remember { mutableStateOf<String?>(restored?.exercises?.getOrNull(restored.currentExerciseIndex)?.exerciseName) }
    var generationMessage by remember { mutableStateOf<String?>(null) }
    var confirmFinish by remember { mutableStateOf(false) }
    val completed = remember { mutableStateMapOf<String, Int>().apply{restored?.exercises?.forEach{put(it.exerciseName,it.sets.count{s->s.completed})}} }
    val skipped = remember { mutableStateMapOf<String, Boolean>().apply{restored?.exercises?.filter{it.status==WorkoutExerciseStatus.SKIPPED}?.forEach{put(it.exerciseName,true)}} }
    val expandedCompleted = remember { mutableStateMapOf<String, Boolean>() }
    val pinnedNames = remember { mutableStateListOf<String>().apply{restored?.exercises?.filter{it.pinned}?.forEach{add(it.exerciseName)}} }
    val reps = remember { mutableStateMapOf<String, Int>() }
    val restSeconds = remember { mutableStateMapOf<String, Int>() }
    val executedSets = remember { mutableStateMapOf<String,List<WorkoutSetState>>().apply{restored?.exercises?.forEach{put(it.exerciseName,it.sets)} } }
    val exerciseRpe = remember { mutableStateMapOf<String,Int>().apply{restored?.exercises?.forEach{e->put(e.exerciseName,runnerEffort(e.rpe))}} }
    var workoutRpe by remember { mutableIntStateOf(runnerEffort(restored?.workoutRpe)) }
    val listState = rememberLazyListState()
    var previewGeneration by remember { mutableIntStateOf(0) }

    LaunchedEffect(running,paused,restEndsAt,sessionStartedAt){ while(running&&!paused){ val now=System.currentTimeMillis();restRemaining=remainingSeconds(restEndsAt,now);sessionStartedAt?.let{elapsedSeconds=((now-it)/1000L).toInt()};delay(500) } }
    LaunchedEffect(running,paused,skipped.toMap(),currentExercise,restRemaining,completed.toMap(),executedSets.toMap(),exerciseRpe.toMap(),workoutRpe) {
        if(running&&store!=null){
            val now=System.currentTimeMillis()
            val states=generated.mapIndexed{i,ex->recordedExerciseState(i,ex,executedSets[ex.name],exerciseRpe[ex.name],skipped[ex.name]==true,ex.name in pinnedNames)}
            store.saveActiveWorkout(WorkoutSessionState(sessionId,sessionStartedAt?:now,"Personalizzato",states,generated.indexOfFirst{it.name==currentExercise}.coerceAtLeast(0),if(paused)elapsedSeconds*1000L else 0L,if(paused)now else null,restEndsAt,workoutRpe,runnerKind="custom",restKind=restKind,pausedRestRemainingSec=if(paused)restRemaining else null,trainingLocationId=selectedLocation.id))
        }
    }
    LaunchedEffect(previewGeneration) {
        if(!running) {
            val navigation=previewNavigationAfterGeneration(generated)
            if(navigation.scrollIndex>=0) listState.scrollToItem(navigation.scrollIndex)
        }
    }
    LaunchedEffect(running) { if(running){ if(currentExercise==null)currentExercise=generated.firstOrNull{(completed[it.name]?:0)<(executedSets[it.name]?.size?:prescriptionFor(it).sets)}?.name; val i=generated.indexOfFirst{it.name==currentExercise}.coerceAtLeast(0);listState.scrollToItem(i) } }
    LaunchedEffect(currentExercise) { if(running){ val i=generated.indexOfFirst{it.name==currentExercise}; if(i>=0) listState.animateScrollToItem(i) } }

    val skills = listOf("Nessuna") + calisthenicsSkills.map { it.name }
    fun prescription(ex:Exercise):Pair<Int,Int> = prescriptionFor(ex).value.toInt() to ex.setRestSec
    fun targetSetCount(ex:Exercise):Int = prescriptionFor(ex).sets
    fun build() {
        val count=when{minutes<=25->4;minutes<=40->5;minutes<=60->6;else->8}; val recovery=recoveryScores(sessions)
        val base=customWorkoutCandidatePool(selectedLocation,athlete,sessions,mode,selectedSkills,selectedGroups,equipmentFilters=initialEquipment)
        val ranked=base.sortedWith(compareBy<Exercise>{ex->val rec=recovery.filterKeys{ex.group.contains(it,true)}.values.minOrNull()?:100;if(rec<40)100 else if(rec<60)50 else 0}.thenBy{if(selectedSkills.any{s->s in it.skills})0 else 1}.thenBy{if(it.role=="Forza")0 else 1})
        generated=ranked.take(count)
        generated=applyCoaching(sequenceWorkout(generated),coachingContext(sessions,readinessScore))
        if(mode=="Skill") generated=enforceSkillMasteryPrescription(generated,selectedSkills,sessions)
        completed.clear();expandedCompleted.clear();reps.clear();restSeconds.clear();exerciseRpe.clear()
        generated.forEach{ex->val p=prescription(ex);reps[ex.name]=p.first;restSeconds[ex.name]=p.second;exerciseRpe[ex.name]=5}
        exerciseRestSeconds=generated.maxOfOrNull{it.exerciseRestSec}?:180;running=false;paused=false;elapsedSeconds=0;restRemaining=0
        currentExercise=previewNavigationAfterGeneration(generated).exerciseName
        generationMessage=if(generated.isEmpty())"Nessun esercizio compatibile: prova un altro luogo o amplia muscoli/livello/skill." else "Workout pronto: "+generated.size+" esercizi."
        previewGeneration++
    }
    fun resetPreview(){ generated=emptyList();generationMessage=null;completed.clear();exerciseRpe.clear();restRemaining=0;currentExercise=null }

    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text(if(running)"Workout in corso" else if(generated.isNotEmpty())"Anteprima workout" else "Crea workout personalizzato")},
        text={
            Column(Modifier.heightIn(max=590.dp)){
                if(running){
                    Card(Modifier.fillMaxWidth().padding(bottom=8.dp)){
                        Column(Modifier.padding(10.dp)){
                            Text("Tempo allenamento: "+String.format("%02d:%02d",elapsedSeconds/60,elapsedSeconds%60),fontWeight=FontWeight.Bold)
                            if(paused) Text("IN PAUSA",color=Accent,fontWeight=FontWeight.Bold)
                            if(restRemaining>0) Text("Recupero "+restKind.lowercase()+": "+restRemaining+" sec",color=Accent,fontSize=18.sp,fontWeight=FontWeight.Bold)
                            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                OutlinedButton(onClick={if(paused){paused=false;sessionStartedAt=System.currentTimeMillis()-elapsedSeconds*1000L;if(restRemaining>0)restEndsAt=System.currentTimeMillis()+restRemaining*1000L}else{paused=true;restRemaining=remainingSeconds(restEndsAt);restEndsAt=null}}){Text(if(paused)"RIPRENDI" else "PAUSA")}
                                if(restRemaining>0) TextButton(onClick={restRemaining=0;restEndsAt=null}){Text("SALTA RECUPERO")}
                            }
                        }
                    }
                }
                if(!running && generated.isNotEmpty()){
                    Column(Modifier.fillMaxWidth().padding(bottom=8.dp)){
                        Text("WORKOUT GENERATO",fontWeight=FontWeight.Bold,color=Accent)
                        Text("Durata prevista: $minutes min")
                        Text("Luogo: "+selectedLocation.icon+" "+selectedLocation.name,fontSize=12.sp,color=Color.Gray)
                        Text(if(mode=="Skill")"Skill: "+selectedSkills.joinToString(", ") else "Gruppi: "+selectedGroups.joinToString(", "),fontSize=12.sp,color=Color.Gray)
                        CoachSummary(coachingContext(sessions,readinessScore))
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                            Text("Blocca gli esercizi che vuoi mantenere",fontSize=12.sp,color=Color.Gray,modifier=Modifier.weight(1f))
                            TextButton(onClick={
                                val pool=customWorkoutCandidatePool(selectedLocation,athlete,sessions,mode,selectedSkills,selectedGroups,equipmentFilters=initialEquipment)
                                generated=if(mode=="Skill") regenerateSkillWorkout(generated,pool,pinnedNames.toSet(),coachingContext(sessions,readinessScore),selectedSkills,sessions) else regenerateCoachedWorkout(generated,pool,pinnedNames.toSet(),coachingContext(sessions,readinessScore))
                                reps.clear();restSeconds.clear();generated.forEach{ex->reps[ex.name]=prescriptionFor(ex).value.toInt();restSeconds[ex.name]=ex.setRestSec}
                                generationMessage="Rigenerati solo gli esercizi non bloccati."
                                currentExercise=previewNavigationAfterGeneration(generated).exerciseName
                                previewGeneration++
                            }){Text("RIGENERA NON BLOCCATI")}
                        }
                        Text("ESERCIZI",fontWeight=FontWeight.Bold)
                    }
                }
                LazyColumn(state=listState,modifier=Modifier.weight(1f)){
                    if(!running && generated.isEmpty()){
                        if(filtersLocked){
                            item{
                                Text("1. Luogo di allenamento",fontWeight=FontWeight.Bold)
                                LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(locationOptions){loc->FilterChip(selectedLocation.id==loc.id,{selectedLocation=loc;resetPreview()},{Text(loc.icon+" "+loc.name)})}}
                                Text("Workout basato sui filtri",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp))
                                Text((initialGroups+initialEquipment+selectedSkills).joinToString(" • "),color=Accent)
                                Text("Durata workout",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp))
                                Row{listOf(20,30,45,60,75).forEach{m->FilterChip(minutes==m,{minutes=m},{Text("$m")},Modifier.padding(end=4.dp))}}
                                Button(onClick={build()},modifier=Modifier.fillMaxWidth().padding(vertical=10.dp)){Text("GENERA WORKOUT")}
                                generationMessage?.let{Text(it,color=MaterialTheme.colorScheme.error,fontWeight=FontWeight.Bold)}
                            }
                        } else {
                            item{
                                Text("1. Luogo di allenamento",fontWeight=FontWeight.Bold)
                                Text("Scegli dove svolgerai questo workout. Gli esercizi saranno adattati alle attrezzature realmente disponibili.",color=Color.Gray,fontSize=12.sp)
                                LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(locationOptions){loc->FilterChip(selectedLocation.id==loc.id,{selectedLocation=loc;resetPreview()},{Text(loc.icon+" "+loc.name)})}}
                                val available=exercises.count{ex->locationCompatible(ex,selectedLocation)}
                                Text("$available esercizi compatibili con "+selectedLocation.name,color=Accent,fontSize=12.sp)
                                Text("2. Durata",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp))
                                Row{listOf(20,30,45,60,75).forEach{m->FilterChip(minutes==m,{minutes=m},{Text("$m")},Modifier.padding(end=4.dp))}}
                                Text("3. Cosa vuoi allenare?",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp))
                                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){FilterChip(mode=="Parti del corpo",{mode="Parti del corpo";selectedSkills.clear()},{Text("Parti del corpo")});FilterChip(mode=="Skill",{mode="Skill";selectedGroups.clear()},{Text("Skill calisthenics")})}
                            }
                            if(mode=="Parti del corpo") items(recoveryGroups){g->Row(Modifier.fillMaxWidth().clickable{if(g in selectedGroups)selectedGroups.remove(g)else selectedGroups.add(g)},verticalAlignment=Alignment.CenterVertically){Checkbox(g in selectedGroups,{if(it){if(g !in selectedGroups)selectedGroups.add(g)}else selectedGroups.remove(g)});Text(g)}}
                            if(mode=="Skill") item{skills.filter{it!="Nessuna"}.forEach{s->val linked=exercises.filter{s in it.skills};val muscles=linked.flatMap{ex->recoveryGroups.filter{g->ex.group.contains(g,true)}}.distinct().joinToString(" • ");Row(Modifier.fillMaxWidth().clickable{if(s in selectedSkills)selectedSkills.remove(s)else selectedSkills.add(s)},verticalAlignment=Alignment.CenterVertically){Checkbox(s in selectedSkills,{checked->if(checked){if(s !in selectedSkills)selectedSkills.add(s)}else selectedSkills.remove(s)});Column{Text(s);if(s in selectedSkills)Text("Coinvolge: $muscles",color=Color.Gray,fontSize=11.sp)}}}}
                            item{Button(onClick={build()},enabled=mode!="Skill"||selectedSkills.isNotEmpty(),modifier=Modifier.fillMaxWidth().padding(vertical=10.dp)){Text("GENERA WORKOUT")};generationMessage?.let{Text(it,color=MaterialTheme.colorScheme.error,fontWeight=FontWeight.Bold)}}
                        }
                    }
                    items(generated){ex->
                        val n=completed[ex.name]?:0;val targetSets=targetSetCount(ex);val done=n>=targetSets;val isSkipped=skipped[ex.name]==true;val expanded=!done||(expandedCompleted[ex.name]?:false)
                        Card(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{if(running){if(done)expandedCompleted[ex.name]=!(expandedCompleted[ex.name]?:false) else {skipped[ex.name]=false;currentExercise=ex.name}}}){
                            Column(Modifier.padding(10.dp)){
                                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text((if(done)"✓ " else if(running&&currentExercise==ex.name)"▶ " else "")+ex.name,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));if(!running)TextButton(onClick={if(ex.name in pinnedNames)pinnedNames.remove(ex.name)else pinnedNames.add(ex.name)}){Text(if(ex.name in pinnedNames)"🔒 BLOCCATO" else "🔓 BLOCCA")}}
                                if(done) Text("COMPLETATO • "+targetSets+"/"+targetSets+" serie",color=Accent,fontSize=12.sp)
                                if(running && !done) {
                                    if(isSkipped) Text("SALTATO • tocca RIPRENDI per continuare",color=Color.Gray,fontSize=12.sp)
                                    else if(currentExercise==ex.name) Text("ESERCIZIO ATTIVO • serie "+(n+1).coerceAtMost(targetSets)+"/"+targetSets,color=Accent,fontSize=12.sp,fontWeight=FontWeight.Bold)
                                    TextButton(onClick={
                                        skipped[ex.name]=!isSkipped
                                        currentExercise=if(isSkipped) ex.name else generated.firstOrNull{it.name!=ex.name && skipped[it.name]!=true && (completed[it.name]?:0)<targetSetCount(it)}?.name
                                    }){Text(if(isSkipped)"RIPRENDI" else "SALTA ESERCIZIO")}
                                }
                                if(running&&n>0){Text("RPE: "+(exerciseRpe[ex.name]?.toString()?:"da indicare"),fontSize=12.sp);Slider(value=(exerciseRpe[ex.name]?:5).toFloat(),onValueChange={exerciseRpe[ex.name]=it.toInt()},valueRange=1f..10f,steps=8)}
                                if(expanded){
                                    val p=prescriptionFor(ex)
                                    Text(ex.group+" • "+ex.equipment+" • "+ex.role,color=Color.Gray,fontSize=12.sp)
                                    Text(targetSetCount(ex).toString()+" serie × "+(reps[ex.name]?:p.value.toInt())+" "+p.unit+" • "+(restSeconds[ex.name]?:ex.setRestSec)+"s serie / "+ex.exerciseRestSec+"s esercizio",color=Accent,fontSize=12.sp)
                                    Text("APP_RULE / FALLBACK: target del catalogo adattato dal coach",fontSize=10.sp,color=Color.Gray)
                                    if(running && !done && !isSkipped){
                                        ExerciseExecutionInputs(ex,restRemaining==0&&!paused,loadRecommendationForExercise(ex,sessions,minOf(exerciseRecovery(ex,recoveryScores(sessions)),readinessScore?:100)),executedSets[ex.name]?.firstOrNull{!it.completed}){ar,asec,dist,load,assist->val p=prescriptionFor(ex);val nNow=completed[ex.name]?:0;val planned=plannedExerciseState(0,ex).sets;val requiredSets=targetSetCount(ex);val old=executedSets[ex.name]?:List(requiredSets){idx->planned.getOrNull(idx)?.copy(index=idx)?:planned.last().copy(index=idx,completed=false,actualReps=null,actualSeconds=null,actualDistanceMeters=null,actualLoadKg=null,actualAssistanceKg=null,restUsedSec=null)};if(nNow<old.size){val xs=old.toMutableList();xs[nNow]=xs[nNow].copy(actualReps=ar,actualSeconds=asec,actualDistanceMeters=dist,actualLoadKg=load,actualAssistanceKg=assist,completed=true);executedSets[ex.name]=xs;completed[ex.name]=nNow+1;if(nNow+1>=targetSetCount(ex)){restKind="Esercizio";restRemaining=ex.exerciseRestSec}else{restKind="Serie";restRemaining=runnerSetRestSeconds(ex,restSeconds[ex.name])};restEndsAt=System.currentTimeMillis()+restRemaining*1000L}}

                                        Row(verticalAlignment=Alignment.CenterVertically){Text("Recupero serie: ",fontSize=12.sp,color=Color.Gray);listOf(30,60,90,120,180).forEach{v->FilterChip((restSeconds[ex.name]?:60)==v,{restSeconds[ex.name]=v},{Text(v.toString()+"s")},Modifier.padding(end=3.dp))}}

                                        if(n>0)TextButton(onClick={
                                            val sets=executedSets[ex.name].orEmpty().toMutableList()
                                            val last=sets.indexOfLast{it.completed}
                                            if(last>=0){sets[last]=sets[last].copy(completed=false);executedSets[ex.name]=sets;completed[ex.name]=sets.count{it.completed};restEndsAt=null;restRemaining=0}
                                        }){Text("ANNULLA ULTIMA SERIE")}

                                    }
                                }
                            }
                        }
                    }
                    if(running) item{Text("Fatica complessiva workout: $workoutRpe/10",fontWeight=FontWeight.Bold);Slider(value=workoutRpe.toFloat(),onValueChange={workoutRpe=it.toInt()},valueRange=1f..10f,steps=8)}
                }
            }
        },
        confirmButton={
            if(generated.isNotEmpty()){
                Button(onClick={
                    if(!running){sessionStartedAt=System.currentTimeMillis();running=true;paused=false;currentExercise=generated.firstOrNull{(completed[it.name]?:0)<targetSetCount(it)}?.name}
                    else confirmFinish=true
                }){Text(if(running)"TERMINA E SALVA" else "AVVIA WORKOUT")}
            }
        },
        dismissButton={
            if(!running && generated.isNotEmpty()) TextButton(onClick={resetPreview()}){Text("MODIFICA PARAMETRI")}
            else TextButton(onClick=onDismiss){Text("ANNULLA")}
        }
    )
    if(confirmFinish) AlertDialog(
        onDismissRequest={confirmFinish=false},
        title={Text("Terminare e salvare il workout?")},
        text={Text("Serie completate: ${completed.values.sum()}/${generated.sumOf{targetSetCount(it)}}. Potrai consultare il dettaglio nella cronologia.")},
        confirmButton={Button(onClick={
            val total=generated.sumOf{targetSetCount(it)}
            val saved=onSave(SessionRecord(System.currentTimeMillis(),"Personalizzato"+if(selectedSkills.isNotEmpty())" • "+selectedSkills.joinToString(", ") else "",completed.values.sum(),total,generated.map{it.name},exerciseRpe.toMap(),workoutRpe,completed.toMap(),elapsedSeconds,generated.mapIndexed{i,ex->recordedExerciseState(i,ex,executedSets[ex.name],exerciseRpe[ex.name],skipped[ex.name]==true,ex.name in pinnedNames)},skipped.filterValues{it}.keys.toList()))
            confirmFinish=false
            if(saved)onDismiss()
        }){Text("SALVA WORKOUT")}},
        dismissButton={TextButton(onClick={confirmFinish=false}){Text("CONTINUA ALLENAMENTO")}}
    )
}

@Composable
private fun WorkoutDialog(profile: TrainingLocation, athlete: AthleteProfile, sessions: List<SessionRecord>, store:AppStore, restored:WorkoutSessionState?=null, plannedWorkout:PlannedWorkout?=null, onDismiss: () -> Unit, onSave: (SessionRecord) -> Boolean) {
    val liveReadiness=LocalHealthConnect.current.readiness
    val readinessScore=liveReadiness
    val compatible = remember(profile, athlete, sessions, restored, plannedWorkout, readinessScore) { val restoredSource=restored?.let{restoreExercises(it)}?.takeIf{it.isNotEmpty()}; val source=restoredSource ?: plannedWorkout?.let{materializePlannedSession(it,profile,athlete,sessions,readinessScore).exercises} ?: generateWorkout(profile, athlete, sessions,readinessScore); effectiveAutomaticExercises(source,athlete,sessions,readinessScore,restoredSource!=null || plannedWorkout!=null) }
    val completedSets = remember { mutableStateMapOf<String, Int>().apply { restored?.exercises?.forEach { e -> put(e.exerciseName,e.sets.count{it.completed}) } } }
    val expandedCompleted = remember { mutableStateMapOf<String, Boolean>() }
    val skipped = remember { mutableStateMapOf<String, Boolean>().apply{restored?.exercises?.filter{it.status==WorkoutExerciseStatus.SKIPPED}?.forEach{put(it.exerciseName,true)}} }
    val totalSets = compatible.sumOf{prescriptionFor(it).sets}
    val doneSets = completedSets.values.sum()
    var started by remember { mutableStateOf(restored != null) }
    var confirmFinish by remember { mutableStateOf(false) }
    var sessionStartedAt by remember { mutableStateOf(restored?.startedAtEpochMs) }
    val sessionId = remember { restored?.id ?: "auto-"+System.currentTimeMillis() }
    var paused by remember { mutableStateOf(restored?.pausedAtEpochMs != null) }
    var elapsedSeconds by remember { mutableIntStateOf(restored?.let{elapsedWorkoutSeconds(it,System.currentTimeMillis())}?:0) }
    var restRemaining by remember { mutableIntStateOf(restored?.pausedRestRemainingSec ?: remainingSeconds(restored?.restEndsAtEpochMs)) }
    var restEndsAt by remember { mutableStateOf(restored?.restEndsAtEpochMs) }
    var restKind by remember { mutableStateOf(restored?.restKind?:"Serie") }
    var currentExercise by remember { mutableStateOf<String?>(restored?.exercises?.getOrNull(restored.currentExerciseIndex)?.exerciseName) }
    val executedSets = remember { mutableStateMapOf<String,List<WorkoutSetState>>().apply{restored?.exercises?.forEach{put(it.exerciseName,it.sets)} } }
    val exerciseRpe = remember { mutableStateMapOf<String,Int>().apply {
        compatible.forEach { put(it.name, runnerEffort(restored?.exercises?.firstOrNull { saved -> saved.exerciseName==it.name }?.rpe)) }
    } }
    var workoutRpe by remember { mutableIntStateOf(runnerEffort(restored?.workoutRpe)) }
    val listState = rememberLazyListState()

    LaunchedEffect(started,paused,restEndsAt,sessionStartedAt){while(started&&!paused){val now=System.currentTimeMillis();restRemaining=remainingSeconds(restEndsAt,now);sessionStartedAt?.let{elapsedSeconds=((now-it)/1000L).toInt()};delay(500)}}
    LaunchedEffect(started, paused, skipped.toMap(),exerciseRpe.toMap(),currentExercise, restRemaining, doneSets, executedSets.toMap(), workoutRpe) {
        if(started) {
            val now=System.currentTimeMillis()
            val states=compatible.mapIndexed { index, ex ->
                recordedExerciseState(index,ex,executedSets[ex.name],exerciseRpe[ex.name],skipped[ex.name]==true,false)
            }
            store.saveActiveWorkout(WorkoutSessionState(sessionId,sessionStartedAt?:now,profile.name,states,compatible.indexOfFirst{it.name==currentExercise}.coerceAtLeast(0),if(paused)elapsedSeconds*1000L else 0L,if(paused)now else null,restEndsAt,workoutRpe,runnerKind="automatic",restKind=restKind,plannedWorkoutId=restored?.plannedWorkoutId?:plannedWorkout?.id,pausedRestRemainingSec=if(paused)restRemaining else null,trainingLocationId=restored?.trainingLocationId?:profile.id))
        }
    }
    LaunchedEffect(started){if(started){if(currentExercise==null)currentExercise=compatible.firstOrNull{(completedSets[it.name]?:0)<prescriptionFor(it).sets}?.name;val i=compatible.indexOfFirst{it.name==currentExercise}.coerceAtLeast(0);listState.scrollToItem(i+1)}}
    LaunchedEffect(currentExercise){if(started){val i=compatible.indexOfFirst{it.name==currentExercise};if(i>=0)listState.animateScrollToItem(i+1)}}

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Allenamento • " + profile.name) },
        text = {
            Column(Modifier.heightIn(max=520.dp)){
                if(started){
                    Card(Modifier.fillMaxWidth().padding(bottom=8.dp)){
                        Column(Modifier.padding(10.dp)){
                            Text("Tempo allenamento: "+String.format("%02d:%02d",elapsedSeconds/60,elapsedSeconds%60),fontWeight=FontWeight.Bold)
                            if(paused) Text("IN PAUSA",color=Accent,fontWeight=FontWeight.Bold)
                            if(restRemaining>0) Text("Recupero "+restKind.lowercase()+": "+restRemaining+" sec",color=Accent,fontSize=18.sp,fontWeight=FontWeight.Bold)
                            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                OutlinedButton(onClick={if(paused){paused=false;sessionStartedAt=System.currentTimeMillis()-elapsedSeconds*1000L;if(restRemaining>0)restEndsAt=System.currentTimeMillis()+restRemaining*1000L}else{paused=true;restRemaining=remainingSeconds(restEndsAt);restEndsAt=null}}){Text(if(paused)"RIPRENDI" else "PAUSA")}
                                if(restRemaining>0) TextButton(onClick={restRemaining=0;restEndsAt=null}){Text("SALTA RECUPERO")}
                            }
                        }
                    }
                }
                LazyColumn(state=listState,modifier=Modifier.weight(1f)){
                    if(!started)item{
                        CoachSummary(coachingContext(sessions,readinessScore))
                        val capability=athlete.toCapabilityProfile()
                        Text("Generato per "+athlete.goal+" • "+(if(capability.assessed)"capacità valutata" else "capacità da valutare")+" • "+profile.name,color=Color.Gray)
                        if(sessions.any{it.exerciseNames.isNotEmpty()})Text("Adattato anche agli allenamenti recenti",color=Accent,fontSize=12.sp)
                    }
                    item{LinearProgressIndicator(progress={if(totalSets==0)0f else doneSets.toFloat()/totalSets},modifier=Modifier.fillMaxWidth().padding(vertical=12.dp))}
                    if(compatible.isEmpty())item{Text("Nessun esercizio compatibile con questo profilo.")}
                    items(compatible){ex->
                        val count=completedSets[ex.name]?:0
                        val isSkipped=skipped[ex.name]==true
                        val targetSets=prescriptionFor(ex).sets
                        val done=count>=targetSets
                        val expanded=!done||(expandedCompleted[ex.name]?:false)
                        Card(Modifier.fillMaxWidth().padding(vertical=5.dp).clickable{
                            if(started){
                                if(done)expandedCompleted[ex.name]=!(expandedCompleted[ex.name]?:false)
                                else {skipped[ex.name]=false;currentExercise=ex.name}
                            }
                        }){
                            Column(Modifier.padding(12.dp)){
                                Text((if(done)"✓ " else if(isSkipped)"↷ " else if(started&&currentExercise==ex.name)"▶ " else "")+ex.name,fontWeight=FontWeight.Bold);if(started&&currentExercise==ex.name&&!done&&!isSkipped)Text("ESERCIZIO ATTIVO • serie "+(count+1).coerceAtMost(targetSets)+"/"+targetSets,color=Accent,fontSize=12.sp,fontWeight=FontWeight.Bold);if(isSkipped)Text("SALTATO • tocca RIPRENDI per continuare",color=Color.Gray,fontSize=12.sp)
                                if(done)Text("COMPLETATO • "+targetSets+"/"+targetSets+" serie",color=Accent,fontSize=12.sp)
                                if(expanded){
                                    val p=prescriptionFor(ex)
                                    Text(p.sets.toString()+" serie × "+p.value.toInt()+" "+p.unit+" • "+ex.setRestSec+"s serie / "+ex.exerciseRestSec+"s esercizio",color=Accent,fontSize=12.sp)
                                    if(started&&count>0){Text("Fatica esercizio (RPE): "+(exerciseRpe[ex.name]?:5)+"/10",fontSize=12.sp);Slider(value=(exerciseRpe[ex.name]?:5).toFloat(),onValueChange={exerciseRpe[ex.name]=it.toInt()},valueRange=1f..10f,steps=8)}
                                    Text(ex.equipment+" • "+ex.role,color=Color.Gray,fontSize=12.sp)
                                    if(started&&!done){Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){TextButton(onClick={skipped[ex.name]=!isSkipped;if(!isSkipped){currentExercise=compatible.firstOrNull{it.name!=ex.name&&(completedSets[it.name]?:0)<prescriptionFor(it).sets}?.name}else currentExercise=ex.name}){Text(if(isSkipped)"RIPRENDI" else "SALTA ESERCIZIO")}}}
                                    if(started&&!done&&!isSkipped){
                                        ExerciseExecutionInputs(ex,!paused&&restRemaining==0,loadRecommendationForExercise(ex,sessions,minOf(exerciseRecovery(ex,recoveryScores(sessions)),readinessScore?:100)),executedSets[ex.name]?.firstOrNull{!it.completed}){ar,asec,dist,load,assist->
                                            val p=prescriptionFor(ex)
                                            val old=executedSets[ex.name]?:plannedExerciseState(0,ex).sets
                                            if(count<old.size){
                                                val xs=old.toMutableList()
                                                xs[count]=xs[count].copy(actualReps=ar,actualSeconds=asec,actualDistanceMeters=dist,actualLoadKg=load,actualAssistanceKg=assist,completed=true)
                                                executedSets[ex.name]=xs
                                                completedSets[ex.name]=count+1
                                                if(count+1>=p.sets){
                                                    restKind="Esercizio";restRemaining=ex.exerciseRestSec
                                                    currentExercise=compatible.dropWhile{it.name!=ex.name}.drop(1).firstOrNull{(completedSets[it.name]?:0)<prescriptionFor(it).sets&&skipped[it.name]!=true}?.name
                                                        ?:compatible.firstOrNull{(completedSets[it.name]?:0)<prescriptionFor(it).sets&&skipped[it.name]!=true}?.name
                                                } else {
                                                    restKind="Serie";restRemaining=runnerSetRestSeconds(ex);currentExercise=ex.name
                                                }
                                                restEndsAt=System.currentTimeMillis()+restRemaining*1000L
                                            }
                                        }
                                        if(count>0) TextButton(onClick={
                                            val undone=undoLastCompletedSet(executedSets[ex.name].orEmpty())
                                            executedSets[ex.name]=undone
                                            completedSets[ex.name]=undone.count{it.completed}
                                            restEndsAt=null;restRemaining=0;currentExercise=ex.name
                                        }){Text("ANNULLA ULTIMA SERIE")}
                                    }
                                }
                            }
                        }
                    }
                    if(started)item{Text("Fatica complessiva workout: $workoutRpe/10",fontWeight=FontWeight.Bold);Slider(value=workoutRpe.toFloat(),onValueChange={workoutRpe=it.toInt()},valueRange=1f..10f,steps=8)}
                }
            }
        },
        confirmButton = { Button(onClick={
            if(!started){sessionStartedAt=System.currentTimeMillis();started=true;paused=false;currentExercise=compatible.firstOrNull{(completedSets[it.name]?:0)<prescriptionFor(it).sets}?.name}
            else confirmFinish=true
        }){Text(if(!started)"AVVIA WORKOUT" else if(totalSets>0&&doneSets==totalSets)"TERMINA E SALVA" else "SALVA WORKOUT PARZIALE")} },
        dismissButton = { TextButton(onClick=onDismiss){Text("ANNULLA")} }
    )
    if(confirmFinish) AlertDialog(
        onDismissRequest={confirmFinish=false},
        title={Text("Terminare e salvare il workout?")},
        text={Text("Serie completate: $doneSets/$totalSets. Il programma sarà aggiornato dopo il salvataggio.")},
        confirmButton={Button(onClick={
            val structured=compatible.mapIndexed { index,ex->recordedExerciseState(index,ex,executedSets[ex.name],exerciseRpe[ex.name],skipped[ex.name]==true,false) }
            val saved=onSave(SessionRecord(System.currentTimeMillis(),profile.name,doneSets,totalSets,compatible.map{it.name},exerciseRpe.toMap(),workoutRpe,compatible.associate{it.name to (completedSets[it.name]?:0)},elapsedSeconds,structured,structured.filter{it.status==WorkoutExerciseStatus.SKIPPED}.map{it.exerciseName},plannedWorkoutId=restored?.plannedWorkoutId?:plannedWorkout?.id))
            confirmFinish=false
            if(saved)onDismiss()
        }){Text("SALVA WORKOUT")}},
        dismissButton={TextButton(onClick={confirmFinish=false}){Text("CONTINUA ALLENAMENTO")}}
    )
}

@Composable
private fun ExerciseDetailDialog(ex: Exercise, sessions: List<SessionRecord>, onDismiss: () -> Unit, onSkillClick:(String)->Unit, onExecute:()->Unit) {
    val stats = remember(ex.name,sessions) { ExerciseStatistics.fromHistory(ex.name,sessions) }
    val v2Record=ExerciseDbV2.recordFor(ex)
    val sourceDrivenInstructions = v2Record?.let { record ->
        buildString {
            if(record.setupIt.isNotBlank()) append("Preparazione\n").append(record.setupIt).append("\n\n")
            if(record.executionStepsIt.isNotEmpty()) append("Esecuzione\n• ").append(record.executionStepsIt.joinToString("\n• "))
            else if(record.descriptionIt.isNotBlank()) append(record.descriptionIt)
        }.takeIf { it.isNotBlank() }
    }
    val instructions = sourceDrivenInstructions ?: "Dettagli tecnici non disponibili: esercizio non validato nel catalogo F01."
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ex.name) },
        text = {
            Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState())) {
                Text(ex.group + " • " + ex.equipment + " • " + ex.level, color = Color.Gray)
                Spacer(Modifier.height(14.dp))
                Text("Come eseguirlo", fontWeight = FontWeight.Bold)
                Text(instructions, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(12.dp))
                if(stats.sessions>0){
                    Spacer(Modifier.height(12.dp))
                    Text("I tuoi progressi",fontWeight=FontWeight.Bold)
                    Text("${stats.sessions} sessioni • ${stats.completedSets} serie completate")
                    if(stats.totalReps>0)Text("${stats.totalReps} ripetizioni totali")
                    if(stats.totalSeconds>0)Text("${stats.totalSeconds} secondi totali")
                    stats.maxLoadKg?.let{Text("Carico massimo: ${it} kg")}
                    if(stats.totalLoadVolumeKg>0)Text("Volume con carico: ${stats.totalLoadVolumeKg.toInt()} kg")
                }
                Text("Ruolo: " + ex.role, fontWeight = FontWeight.SemiBold)
                val progressionRelations=ExerciseDbV2.progressionRelations(ex)
                if(progressionRelations.easier.isNotEmpty() || progressionRelations.harder.isNotEmpty()) {
                    Text("Progressione esercizio",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp))
                    if(progressionRelations.easier.isNotEmpty())
                        Text("Più facile: "+progressionRelations.easier.joinToString(" • "){it.nameIt},fontSize=12.sp,color=Color.Gray)
                    else Text("Più facile: nessuna regressione verificata nel catalogo",fontSize=12.sp,color=Color.Gray)
                    if(progressionRelations.harder.isNotEmpty())
                        Text("Più difficile: "+progressionRelations.harder.joinToString(" • "){it.nameIt},fontSize=12.sp,color=Accent)
                    else Text("Più difficile: nessuna progressione verificata nel catalogo",fontSize=12.sp,color=Color.Gray)
                }
                if (ex.skills.isNotEmpty()) {
                    Text("Skill collegate",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp))
                    val links=remember(ex.name,ex.skills,sessions){exerciseSkillLinks(ex,sessions)}
                    links.forEach { link ->
                        TextButton(onClick={onSkillClick(link.skill)}){Text(link.skill+" • "+link.progressPercent+"%")}
                        link.currentNode?.let{Text("Livello: "+it+" • prossimo sblocco: "+link.nextUnlock,fontSize=12.sp,color=Color.Gray)}
                    }
                } else {
                    Text("Esercizio base/accessorio senza skill obbligatoria", color = Color.Gray, modifier = Modifier.padding(top = 6.dp))
                }
            }
        },
        confirmButton = { Button(onClick=onExecute){Text("ESEGUI")} },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CHIUDI") } }
    )
}

@Composable
private fun TrainingLocationDialog(current: TrainingLocation? = null, onDismiss: () -> Unit, onCreate: (TrainingLocation) -> Unit) {
    var name by remember(current) { mutableStateOf(current?.name ?: "") }
    val options = profileEquipmentOptions(exercises)
    val selected = remember(current) { mutableStateListOf<EquipmentId>().apply { addAll(current?.capabilities ?: emptySet()) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if(current==null)"Nuovo profilo attrezzatura" else "Modifica profilo attrezzatura") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome (es. Hotel)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Attrezzatura disponibile", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                }
                items(options) { item ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            EquipmentCatalog.fromDisplayName(item)?.let { id -> if (id in selected) selected.remove(id) else selected.add(id) }
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = EquipmentCatalog.fromDisplayName(item)?.let { it in selected } == true,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    EquipmentCatalog.fromDisplayName(item)?.let { if (it !in selected) selected.add(it) }
                                } else EquipmentCatalog.fromDisplayName(item)?.let { selected.remove(it) }
                            }
                        )
                        Text(item)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(TrainingLocation(current?.id ?: "custom:"+System.currentTimeMillis(), name.trim(), "📍", TrainingLocationKind.CUSTOM, selected.toSet())) },
                enabled = name.isNotBlank() && selected.isNotEmpty()
            ) { Text(if(current==null)"CREA" else "SALVA") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ANNULLA") } }
    )
}


