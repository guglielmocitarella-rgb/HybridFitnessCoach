package com.hybridfitnesscoach.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import org.json.JSONObject
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
    val restUsedSec: Int? = null,
    val actualRpe: Int? = null,
    val actualStartedAtEpochMs: Long? = null,
    val actualFinishedAtEpochMs: Long? = null
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
    val trainingLocationId: String? = null,
    val workoutOrigin: String = "ADAPTIVE_EXTRA",
    val timedSet: TimedSetState? = null,
    val restStartedAtEpochMs: Long? = null
)
data class AthleteProfile(val name: String = "", val goal: String = "Forza + skill", val daysPerWeek: Int = 3, val age:Int=0, val heightCm:Float=0f, val weightKg:Float=0f, val sex:String="Non specificato", val prioritySkills:List<String> = emptyList(), val assessedTrainingExperience:Int?=null, val assessedPushCapacity:Int?=null, val assessedPullCapacity:Int?=null,val assessedHorizontalPullCapacity:Int?=null,val assessedVerticalPushCapacity:Int?=null,val assessedSquatCapacity:Int?=null,val assessedHingeCapacity:Int?=null,val assessedCoreCapacity:Int?=null,val assessedIsometricCapacity:Int?=null,val assessedSkillCapacity:Int?=null)
data class SessionRecord(val timestamp: Long, val profileName: String, val completedSets: Int, val totalSets: Int, val exerciseNames: List<String> = emptyList(), val exerciseRpe: Map<String,Int> = emptyMap(), val workoutRpe:Int=0, val completedByExercise:Map<String,Int> = emptyMap(), val durationSec:Int=0, val structuredExercises:List<WorkoutExerciseState> = emptyList(), val skippedExercises:List<String> = emptyList(), val modifications:List<String> = emptyList(), val plannedWorkoutId:String?=null,val trainingLocationId:String?=null,val workoutOrigin:String="ADAPTIVE_EXTRA")
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
    val recent=sessions.sortedByDescending{it.timestamp}.asSequence().flatMap{it.structuredExercises.asSequence()}.firstOrNull{it.exerciseName==ex.name && it.sets.any{set->set.completed}}
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

internal val calisthenicsSkills = listOf(
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
            val contributions=if(session.structuredExercises.isNotEmpty())session.structuredExercises.mapNotNull { saved ->
                val ex=saved.exerciseSnapshot ?: exercises.firstOrNull{it.name==saved.exerciseName}
                ex?.let{it to saved.sets.count{set->set.completed}.toDouble()}
            } else session.exerciseNames.distinct().mapNotNull { name ->
                exercises.firstOrNull{it.name==name}?.let{it to (session.completedByExercise[name]?.toDouble() ?: setsPerExercise)}
            }
            contributions.forEach { (ex,actualSets) ->
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

private fun stringListSaver()=Saver<androidx.compose.runtime.snapshots.SnapshotStateList<String>,ArrayList<String>>(
    save={ArrayList(it)},restore={mutableStateListOf<String>().apply{addAll(it)}})
@Composable
fun HybridFitnessApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { AppStore(context) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    val customProfiles = remember { mutableStateListOf<TrainingLocation>().apply { addAll(store.loadTrainingLocations()) } }
    var activeProfile by remember { mutableStateOf(customProfiles.firstOrNull { it.id == store.loadActiveTrainingLocationId() } ?: customProfiles.firstOrNull() ?: trainingLocations.first()) }
    val v3Runtime = remember(context.applicationContext) {
        (context.applicationContext as HybridFitnessCoachApplication).v3RuntimeComponent
    }
    var athlete by remember { mutableStateOf(store.loadAthlete()) }
    var sessions by remember { mutableStateOf(store.loadSessions()) }
    var v3ActivePlans by remember { mutableStateOf(store.loadEnabledTrainingPlans()) }
    LaunchedEffect(sessions, tab) {
        v3ActivePlans = store.loadEnabledTrainingPlans()
    }
    var onboarding by remember { mutableStateOf(store.loadOnboardingDraft().step in 1..5 || (store.loadOnboardingDraft().step==0 && athlete.name.isBlank() && sessions.isEmpty() && store.loadTrainingPlans().isEmpty())) }
    var summary by remember { mutableStateOf<SessionRecord?>(null) }
    fun refreshProfile(){athlete=store.loadAthlete();customProfiles.clear();customProfiles.addAll(store.loadTrainingLocations());activeProfile=customProfiles.firstOrNull{it.id==store.loadActiveTrainingLocationId()} ?: trainingLocations.first()}
    var workoutLaunch by rememberSaveable(stateSaver=Saver<WorkoutLaunchFlow?,String>(
        save={it?.toLaunchJson().orEmpty()},restore={it.takeIf{value->value.isNotEmpty()}?.let(::workoutLaunchFromJson)}
    )) { mutableStateOf<WorkoutLaunchFlow?>(null) }
    var suspendedWorkout by remember { mutableStateOf(store.loadActiveWorkout()?.takeIf{workoutLaunch==null}) }
    LaunchedEffect(Unit) {
        val active=store.loadActiveWorkout()
        if(active!=null && workoutLaunch!=null)workoutLaunch=resumeWorkoutLaunch(active)
    }
    val health=rememberHealthConnectState()
    LaunchedEffect(activeProfile, athlete, sessions, v3ActivePlans, health.readiness) {
        val athleteSnapshot = athlete
        val historySnapshot = sessions
        val planSnapshot = v3ActivePlans
        val locationSnapshot = activeProfile
        val readinessSnapshot = health.readiness
        val athleteContext = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            com.hybridfitnesscoach.app.domain.v3.V3NativeAthleteContextAdapter.fromNative(
                athleteSnapshot, historySnapshot, planSnapshot, readinessSnapshot
            )
        }
        v3Runtime.activate(locationSnapshot, athleteContext)
    }
    fun requestWorkout(intent:WorkoutLaunchIntent) {
        val active=store.loadActiveWorkout()
        workoutLaunch=if(active!=null)resumeWorkoutLaunch(active) else if(intent.origin==WorkoutOrigin.SINGLE_EXERCISE){
            val chosen=requireNotNull(exercises.firstOrNull{it.name==intent.sourceExerciseId})
            val source=singleExerciseWorkoutState(chosen,"Singolo esercizio",athlete=athlete,sessions=sessions,readinessScore=health.readiness)
            materializeWorkoutLaunch(beginWorkoutLaunch(intent),freezeWorkoutTargets(source,sessions,health.readiness))
        } else beginWorkoutLaunch(intent)
        suspendedWorkout=null
    }
    fun saveFinishedWorkout(record: SessionRecord): Boolean {
        val saved = store.finishWorkout(record)
        if (saved) { sessions = store.loadSessions();suspendedWorkout=null;summary=sessions.firstOrNull{it.timestamp==record.timestamp} ?: record }
        else android.widget.Toast.makeText(context, "Salvataggio non riuscito: allenamento conservato per il recupero.", android.widget.Toast.LENGTH_LONG).show()
        return saved
    }
    if (store.issues.isNotEmpty()) {
        StoreRecoveryScreen(store)
        return
    }
    CompositionLocalProvider(LocalHealthConnect provides health) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Accent, background = Bg, surface = Color.White)) {
        Scaffold(
            containerColor = Bg,
            floatingActionButton={if(suspendedWorkout!=null)ExtendedFloatingActionButton(
                onClick={store.loadActiveWorkout()?.let{workoutLaunch=resumeWorkoutLaunch(it);suspendedWorkout=null}},
                text={Text("RIPRENDI ALLENAMENTO")},icon={Icon(Icons.Default.PlayArrow,null)}
            )},
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
                val customOrigins=setOf(WorkoutOrigin.CUSTOM,WorkoutOrigin.SKILL,WorkoutOrigin.LIBRARY)
                val builderFlow=workoutLaunch?.takeIf{it.stage==WorkoutLaunchStage.MATERIALIZE && it.intent.origin in customOrigins}
                val launchScreen=workoutLaunch?.takeIf{it.stage!=WorkoutLaunchStage.RUNNER && it!=builderFlow}
                if(launchScreen!=null){
                    val locationOptions=(trainingLocations+customProfiles+activeProfile).distinctBy{it.id}
                    val allPlans=store.loadTrainingPlans()
                    val enabledPlans=store.loadEnabledTrainingPlans()
                    val extraAthlete=athlete.copy(
                        goal=enabledPlans.joinToString(" + "){it.goal}.ifBlank{athlete.goal},
                        prioritySkills=enabledPlans.flatMap{it.prioritySkills}.distinct()
                    )
                    WorkoutLaunchScreen(
                        flow=launchScreen,locations=locationOptions,
                        preferredLocationId=store.loadLastUsedTrainingLocationId().orEmpty(),readinessScore=health.readiness,
                        athlete=athlete,sessions=sessions,onFlowChanged={workoutLaunch=it},
                        onChooseLocation={place->
                            val selected=selectWorkoutLocation(launchScreen,place.id)
                            if(launchScreen.intent.origin in customOrigins) workoutLaunch=selected else {
                            val now=System.currentTimeMillis()
                            val result=runCatching { when(launchScreen.intent.origin){
                                WorkoutOrigin.PLAN -> {
                                    val planned=requireNotNull(allPlans.asSequence().flatMap{it.workouts.asSequence()}
                                        .firstOrNull{it.id==launchScreen.intent.plannedWorkoutId})
                                    require(calendarWorkoutActions(planned).canStart){"Sessione già eseguita o saltata"}
                                    val realization=materializePlannedSession(planned,place,athlete,sessions,health.readiness)
                                    WorkoutSessionState(
                                        id="plan-"+planned.id+"-"+now,startedAtEpochMs=now,profileName=place.name,
                                        exercises=realization.exercises.mapIndexed(::plannedExerciseState),
                                        plannedWorkoutId=realization.plannedWorkoutId,trainingLocationId=place.id
                                    )
                                }
                                WorkoutOrigin.ADAPTIVE_EXTRA -> {
                                    // APP_RULE: avoid exact exercises in startable sessions today/tomorrow; use the existing ranking penalty.
                                    val today=java.time.LocalDate.now().toEpochDay()
                                    val upcoming=enabledPlans.flatMap{it.workouts}.filter{it.epochDay in today..today+1 && calendarWorkoutActions(it).canStart}
                                        .flatMap{materializePlannedSession(it,place,athlete,sessions,health.readiness).exercises}.map{it.name}.toSet()
                                    val generated=generateWorkout(place,extraAthlete,sessions,health.readiness,upcoming)
                                    val effective=effectiveAutomaticExercises(generated,athlete,sessions,health.readiness)
                                    WorkoutSessionState(
                                        id="extra-"+now,startedAtEpochMs=now,profileName=place.name,
                                        exercises=effective.mapIndexed(::plannedExerciseState).map{it.copy(rpe=5)},
                                        trainingLocationId=place.id
                                    )
                                }
                                else -> error("Workout origin has no materializer yet: "+launchScreen.intent.origin)
                            }
                            }
                            workoutLaunch=result.fold(
                                onSuccess={materialized->materializeWorkoutLaunch(selected,freezeWorkoutTargets(materialized,sessions,health.readiness))},
                                onFailure={selected.copy(stage=WorkoutLaunchStage.CHOOSE_LOCATION,errorMessage="Impossibile preparare il workout in questo luogo. Scegli un altro luogo o modifica i filtri.")}
                            )
                            }
                        },
                        onStart=start@{
                            val active=store.loadActiveWorkout()
                            if(active!=null){workoutLaunch=resumeWorkoutLaunch(active);return@start}
                            val place=locationOptions.firstOrNull{it.id==launchScreen.locationId}
                            val candidate=startWorkoutRunner(launchScreen)
                            val state=requireNotNull(candidate.workout).copy(startedAtEpochMs=System.currentTimeMillis())
                            if(store.startWorkout(state)) {
                                if(place!=null)activeProfile=place
                                workoutLaunch=candidate.copy(workout=state)
                            } else android.widget.Toast.makeText(context,"Avvio non salvato: riprova.",android.widget.Toast.LENGTH_LONG).show()
                        },
                        onCancel={workoutLaunch=if(launchScreen.stage==WorkoutLaunchStage.PREVIEW && launchScreen.intent.origin!=WorkoutOrigin.SINGLE_EXERCISE)beginWorkoutLaunch(launchScreen.intent)else null}
                    )
                } else if(builderFlow!=null) {
                    val location=(trainingLocations+customProfiles+activeProfile).firstOrNull{it.id==builderFlow.locationId}
                    if(location==null) workoutLaunch=null else CustomWorkoutBuilderScreen(
                        location,athlete,sessions,builderFlow.intent,
                        onDismiss={workoutLaunch=null},
                        onMaterialize={generated,configuredIntent->
                            val current=workoutLaunch
                            if(current!=null && current.stage==WorkoutLaunchStage.MATERIALIZE) {
                                val state=freezeWorkoutTargets(generatedWorkoutSessionState(configuredIntent,location.id,generated,System.currentTimeMillis()),sessions,health.readiness)
                                workoutLaunch=materializeWorkoutLaunch(current.copy(intent=configuredIntent),state)
                            }
                        }
                    )
                } else {
                    when (tab) {
                        Tab.HOME -> Home(
                            athlete=athlete,sessions=sessions,store=store,
                            managePlan={tab=Tab.PLAN},
                            onStartPlannedWorkout={workout->requestWorkout(WorkoutLaunchIntent(WorkoutOrigin.PLAN,plannedWorkoutId=workout.id))},
                            onStartAdaptiveExtra={requestWorkout(WorkoutLaunchIntent(WorkoutOrigin.ADAPTIVE_EXTRA))},
                            onStartCustomWorkout={intent->requestWorkout(intent)}
                        )
                        Tab.PLAN -> TrainingPlanScreen(store,sessions,onPlansChanged={v3ActivePlans=store.loadEnabledTrainingPlans()}) { workout -> requestWorkout(WorkoutLaunchIntent(WorkoutOrigin.PLAN,plannedWorkoutId=workout.id)) }
                        Tab.PROGRESS -> Progress(athlete,sessions,{updated->athlete=updated;store.saveAthlete(updated)},{timestamp->store.deleteSession(timestamp);sessions=store.loadSessions()},showAthleteProfile=false)
                        Tab.PROFILE -> ProfileHub(
                            v3Runtime=v3Runtime, debugDiagnostics=(context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0,
                            active=activeProfile,athlete=athlete,sessions=sessions,store=store,customProfiles=customProfiles,
                            onAthleteSave={updated->if(store.saveAthlete(updated))athlete=updated else android.widget.Toast.makeText(context,"Profilo non salvato: riprova.",android.widget.Toast.LENGTH_LONG).show()},
                            onOnboarding={if(store.loadOnboardingDraft().step==6)store.saveOnboardingDraft(OnboardingDraft());onboarding=true},
                            selectLocation={selected->activeProfile=selected;store.saveActiveTrainingLocationId(selected.id)},
                            addLocation={created->customProfiles.add(created);store.saveTrainingLocations(customProfiles);activeProfile=created;store.saveActiveTrainingLocationId(created.id)},
                            editLocation={old,updated->val i=customProfiles.indexOfFirst{it.id==old.id};if(i>=0){customProfiles[i]=updated;store.saveTrainingLocations(customProfiles);if(activeProfile.id==old.id){activeProfile=updated;store.saveActiveTrainingLocationId(updated.id)}}},
                            deleteLocation={doomed->customProfiles.removeAll{it.id==doomed.id};store.saveTrainingLocations(customProfiles);if(activeProfile.id==doomed.id){activeProfile=customProfiles.firstOrNull()?:trainingLocations.first();store.saveActiveTrainingLocationId(activeProfile.id)}},
                            onExecuteExercise={exercise->requestWorkout(WorkoutLaunchIntent(WorkoutOrigin.SINGLE_EXERCISE,sourceExerciseId=exercise.name))},
                            onStartWorkout={intent->requestWorkout(intent)}
                        )
                    }
                }
            }
        }
        workoutLaunch?.takeIf{it.stage==WorkoutLaunchStage.RUNNER}?.let { flow ->
            key(flow.workout?.id) { UniversalWorkoutScreen(
                store=store,restored=store.loadActiveWorkout()?.takeIf{it.id==flow.workout?.id}?:requireNotNull(flow.workout),
                onDismiss={workoutLaunch=null;suspendedWorkout=store.loadActiveWorkout()},
                onSave={record->val saved=saveFinishedWorkout(record);if(saved)workoutLaunch=null;saved}
            ) }
        }
        summary?.let{record->FlowScreen(onDismissRequest={summary=null},title={Text("Allenamento salvato")},text={LazyColumn(Modifier.fillMaxSize()){
            item{Text("${record.completedSets}/${record.totalSets} serie • ${record.durationSec/60} min • fatica ${record.workoutRpe}/10")}
            items(historyExerciseDetails(record)){detail->Text(detail.name,style=MaterialTheme.typography.titleMedium);detail.lines.forEach{Text(it)}}
        }},confirmButton={Button(onClick={summary=null;tab=Tab.PROGRESS}){Text("VEDI PROGRESSI")}},dismissButton={TextButton(onClick={summary=null}){Text("TORNA A OGGI")}})}
        if(onboarding)OnboardingScreen(store,onExit={onboarding=false;refreshProfile()},onComplete={onboarding=false;refreshProfile();tab=Tab.PLAN})

    }
}
}

@Composable
private fun ProfileHub(
    v3Runtime:com.hybridfitnesscoach.app.domain.v3.V3RuntimeComponent, debugDiagnostics:Boolean,
    active:TrainingLocation, athlete:AthleteProfile, sessions:List<SessionRecord>, store:AppStore,
    customProfiles:List<TrainingLocation>, onAthleteSave:(AthleteProfile)->Unit,
    selectLocation:(TrainingLocation)->Unit, addLocation:(TrainingLocation)->Unit,
    editLocation:(TrainingLocation,TrainingLocation)->Unit, deleteLocation:(TrainingLocation)->Unit,
    onExecuteExercise:(Exercise)->Unit, onStartWorkout:(WorkoutLaunchIntent)->Unit, onOnboarding:()->Unit
) {
    Box(Modifier.fillMaxSize()) {

    var section by remember { mutableStateOf("Profilo") }
    var editAthlete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Header("Profilo","Atleta, luoghi e libreria")
        LazyRow(Modifier.padding(horizontal=20.dp,vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            items(listOf("Profilo","Luoghi","Esercizi") + if(debugDiagnostics) listOf("Diagnostica V3") else emptyList()) { item ->
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
                            Text("Capacità: ${CapabilityDimension.entries.count{capability.levelFor(it)!=null}}/10 aree valutate",color=Color.Gray)
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
                item { OutlinedButton(onClick=onOnboarding,modifier=Modifier.fillMaxWidth()){Text("CONFIGURAZIONE GUIDATA")}}
            }
            "Luoghi" -> Equipment(active,customProfiles,selectLocation,addLocation,editLocation,deleteLocation)
            "Diagnostica V3" -> if(debugDiagnostics) V3DiagnosticsScreen(v3Runtime,active)
            else -> Library(active,athlete,sessions,onExecuteExercise,onStartWorkout)
        }
    }
    if(editAthlete) AthleteProfileScreen(athlete,{editAthlete=false}){updated->onAthleteSave(updated);editAthlete=false}

    }
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
private fun Home(athlete: AthleteProfile, sessions: List<SessionRecord>, store: AppStore, managePlan: () -> Unit, onStartPlannedWorkout:(PlannedWorkout)->Unit, onStartAdaptiveExtra:()->Unit, onStartCustomWorkout:(WorkoutLaunchIntent)->Unit) {
    Box(Modifier.fillMaxSize()) {

    val readinessScore=LocalHealthConnect.current.readiness
    var treeSkill by remember { mutableStateOf<String?>(null) }
    var treePicker by remember { mutableStateOf(false) }
    val enabledPlans=remember(sessions){store.loadEnabledTrainingPlans()}
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            val planLabel=if(enabledPlans.isEmpty())"Nessun programma attivo" else enabledPlans.size.toString()+" piani attivi • "+enabledPlans.sumOf{it.daysPerWeek}+" sessioni/settimana"
            Header(if (athlete.name.isBlank()) "Buongiorno 👋" else "Ciao, " + athlete.name + " 👋", planLabel)
        }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Oggi", fontWeight = FontWeight.SemiBold)
                    val today=java.time.LocalDate.now().toEpochDay()
                    val todaySessions=todayPlannedWorkouts(enabledPlans,today)
                    val nextSession=enabledPlans.flatMap{plan->plan.workouts.filter{w->w.epochDay>today && calendarWorkoutActions(w).canStart}.map{w->plan to w}}
                        .minByOrNull{it.second.epochDay}
                    if(todaySessions.isNotEmpty()) {
                        Text(if(todaySessions.size==1)"Allenamento del piano" else todaySessions.size.toString()+" allenamenti dei piani", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        todaySessions.forEach { (owner,scheduled) ->
                            Card(Modifier.fillMaxWidth().padding(top=10.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(owner.goal,fontWeight=FontWeight.Bold)
                                    Text("Sessione "+(scheduled.sequenceInWeek+1)+" • settimana "+(scheduled.weekIndex+1),color=Color.Gray,fontSize=12.sp)
                                    Button(onClick={onStartPlannedWorkout(scheduled)},enabled=calendarWorkoutActions(scheduled).canStart,modifier=Modifier.fillMaxWidth().padding(top=8.dp)){Text("INIZIA")}
                                }
                            }
                        }
                    } else {
                        Text("Nessuna sessione programmata", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        nextSession?.let { (owner,w) -> Text("Prossimo: "+owner.goal+" • "+calendarDate(w.epochDay),color=Color.Gray) }
                        Text("Puoi recuperare oppure aggiungere una sessione extra adattata al carico complessivo dei piani attivi.",fontSize=13.sp,color=Color.Gray,modifier=Modifier.padding(top=8.dp))
                    }
                    Button(onClick=onStartAdaptiveExtra,modifier=Modifier.fillMaxWidth().padding(top=12.dp)){Text("ALLENAMENTO EXTRA")}
                    Text("La proposta considera recupero, carico recente e programmi attivi; scegli il luogo all’avvio.",fontSize=12.sp,color=Color.Gray)
                    OutlinedButton(onClick = { onStartCustomWorkout(WorkoutLaunchIntent(WorkoutOrigin.CUSTOM)) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(14.dp)) { Text("CREA WORKOUT PERSONALIZZATO") }
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
        item { SectionTitle("Recupero") }
        item { RecoveryChart(sessions) }
    }
    if(treePicker) FlowScreen(onDismissRequest={treePicker=false},title={Text("Skill Tree")},text={LazyColumn{items(calisthenicsSkills){skill->TextButton(onClick={treeSkill=skill.name;treePicker=false}){Text(skill.name)}}}},confirmButton={TextButton(onClick={treePicker=false}){Text("CHIUDI")}})
    treeSkill?.let{skill->SkillDetailScreen(skill,athlete,{treeSkill=null},{onStartCustomWorkout(WorkoutLaunchIntent(WorkoutOrigin.SKILL,focusSkills=listOf(skill)));treeSkill=null},sessions)}


    }
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
private fun TrainingPlanScreen(store:AppStore,sessions:List<SessionRecord>,onPlansChanged:()->Unit,onStartWorkout:(PlannedWorkout)->Unit) {
    Box(Modifier.fillMaxSize()) {

    val today=java.time.LocalDate.now().toEpochDay()
    var plans by remember { mutableStateOf(store.loadTrainingPlans()) }
    var plan by remember { mutableStateOf(store.loadTrainingPlans().firstOrNull()) }
    var enabledPlanIds by remember { mutableStateOf(store.loadEnabledTrainingPlanIds()) }
    LaunchedEffect(plans, enabledPlanIds) {
        onPlansChanged()
    }
    LaunchedEffect(sessions) {
        plans=store.loadTrainingPlans()
        plan=plan?.let{selected->plans.firstOrNull{it.id==selected.id}}
        enabledPlanIds=store.loadEnabledTrainingPlanIds()
    }
    var showPlans by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<TrainingPlan?>(null) }
    val todayDate=java.time.LocalDate.ofEpochDay(today)
    var calendarYear by remember { mutableIntStateOf(todayDate.year) }
    var calendarMonth by remember { mutableIntStateOf(todayDate.monthValue) }
    var weekMode by remember { mutableStateOf(false) }
    var weekAnchor by remember { mutableLongStateOf(today) }
    var selected by remember { mutableStateOf<PlannedWorkout?>(null) }
    var selectedDayWorkouts by remember { mutableStateOf<List<PlannedWorkout>>(emptyList()) }
    var rescheduleOffset by remember { mutableIntStateOf(1) }
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
            val displayedDays=if(weekMode)calendarWeekState(plan!!,weekAnchor,today)else monthState.days
            Row(Modifier.padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(!weekMode,{weekMode=false},{Text("Mese")})
                FilterChip(weekMode,{weekMode=true},{Text("Settimana")})
                TextButton(onClick={showPlans=true}){Text("GESTISCI PIANO")}
            }
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick={if(weekMode)weekAnchor-=7 else {val shifted=shiftCalendarMonth(calendarYear,calendarMonth,-1);calendarYear=shifted.first;calendarMonth=shifted.second}}){Icon(Icons.Default.ChevronLeft,"Mese precedente")}
                Text(if(weekMode)calendarDate(displayedDays.first().epochDay)+" – "+calendarDate(displayedDays.last().epochDay)else monthState.title,fontWeight=FontWeight.Bold,fontSize=18.sp,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick={if(weekMode)weekAnchor+=7 else {val shifted=shiftCalendarMonth(calendarYear,calendarMonth,1);calendarYear=shifted.first;calendarMonth=shifted.second}}){Icon(Icons.Default.ChevronRight,"Mese successivo")}
            }
            Row(Modifier.fillMaxWidth().padding(horizontal=12.dp)) {
                listOf("L","M","M","G","V","S","D").forEach{label->Text(label,fontSize=11.sp,fontWeight=FontWeight.Bold,color=Color.Gray,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.weight(1f))}
            }
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=12.dp,vertical=4.dp)) {
                items(displayedDays.chunked(7)) { week ->
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
        var weeks by remember { mutableIntStateOf(current.weeks) }
        var days by remember { mutableIntStateOf(current.daysPerWeek) }
        val weekdays=remember { mutableStateListOf<Int>().apply{addAll(current.trainingDays.sorted())} }
        val skills=remember { mutableStateListOf<String>().apply{addAll(current.prioritySkills)} }
        FlowScreen(onDismissRequest={editPlan=false},title={Text(if(plan==null)"Crea piano" else "Modifica programma")},text={LazyColumn(Modifier.fillMaxSize()){item{
            Text("Obiettivo",fontWeight=FontWeight.SemiBold)
            listOf("Forza","Ipertrofia","Calisthenics","Fitness generale").chunked(2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){row.forEach{g->FilterChip(goal==g,{goal=g;if(g!="Calisthenics")skills.clear()},{Text(g)},Modifier.weight(1f))}}}
            Text("Durata: $weeks settimane")
            Slider(weeks.toFloat(),{weeks=it.toInt()},valueRange=1f..52f,steps=50)
            Text("Allenamenti a settimana: $days",fontWeight=FontWeight.SemiBold)
            Row{OutlinedButton(onClick={if(days>1){days--;while(weekdays.size>days)weekdays.removeAt(weekdays.lastIndex)}}){Text("−")};Spacer(Modifier.width(8.dp));Button(onClick={if(days<7)days++}){Text("+")}}
            Text("Giorni",fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=8.dp))
            WeekdayChoices(weekdays.toSet(),{chosen->if(chosen.size<=days){weekdays.clear();weekdays.addAll(chosen.sorted())}})
            if(goal=="Calisthenics") {
                Text("Skill prioritarie (opzionali)",fontWeight=FontWeight.SemiBold)
                calisthenicsSkills.forEach{skill->FilterChip(skill.name in skills,{if(skill.name in skills)skills.remove(skill.name)else skills.add(skill.name)},{Text(skill.name)})}
            }
            Text(if(plan==null)"Il piano viene creato come intent: gli esercizi saranno definiti al momento dell'avvio in base al luogo scelto." else "Le sessioni già eseguite e quelle passate non verranno modificate.",fontSize=12.sp,color=Color.Gray,modifier=Modifier.padding(top=10.dp))
        }}},confirmButton={Button(onClick={
            if(weekdays.size==days){
                val candidate=PlanConfiguration(goal,weeks,days,weekdays.toSet(),if(goal=="Calisthenics")skills.toList()else emptyList()).validated()
                confirmRegeneration=candidate;editPlan=false
            }
        },enabled=weekdays.size==days){Text(if(plan==null)"CREA PIANO" else "RIVEDI MODIFICHE")}},dismissButton={TextButton(onClick={editPlan=false}){Text("ANNULLA")}})
    }
    confirmRegeneration?.let { candidateConfig ->
        val current=plan
        val protected=current?.workouts?.count{it.epochDay<today || it.state !in setOf(PlannedWorkoutState.PLANNED,PlannedWorkoutState.RESCHEDULED)} ?: 0
        AlertDialog(onDismissRequest={confirmRegeneration=null},title={Text(if(current==null)"Conferma nuovo piano"else "Conferma programma")},
            text={Text("${candidateConfig.goal} • ${candidateConfig.weeks} settimane • ${candidateConfig.daysPerWeek} giorni/settimana\nGiorni: ${candidateConfig.trainingDays.sorted().joinToString()}\nSkill: ${candidateConfig.prioritySkills.joinToString().ifBlank{"nessuna"}}\n$protected sessioni storiche o protette restano invariate.")},
            confirmButton={Button(onClick={
                val candidate=if(current==null)generateTrainingPlan("plan-"+java.util.UUID.randomUUID(),today,candidateConfig)else regenerateFuturePlan(current,candidateConfig,today)
                if(persistCandidate(candidate) && (current!=null || store.setTrainingPlanEnabled(candidate.id,true))){enabledPlanIds=store.loadEnabledTrainingPlanIds();confirmRegeneration=null}
            }){Text("CONFERMA")}},dismissButton={TextButton(onClick={confirmRegeneration=null}){Text("ANNULLA")}})
    }
    selected?.let { workout ->
        val historical=workout.state==PlannedWorkoutState.COMPLETED || workout.state==PlannedWorkoutState.PARTIAL
        val actions=calendarWorkoutActions(workout)
        val actual=if(historical)store.loadSessions().firstOrNull{it.plannedWorkoutId==workout.id}else null
        FlowScreen(onDismissRequest={selected=null},title={Text(if(historical)"Allenamento eseguito" else "Gestisci sessione")},text={
            LazyColumn(Modifier.fillMaxSize()) {
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
                        items(historyExerciseDetails(actual)){detail->Column(Modifier.padding(vertical=6.dp)){Text(detail.name,fontWeight=FontWeight.SemiBold);detail.lines.forEach{Text(it,fontSize=12.sp)}}}
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
            if(!historical)Column {
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
        FlowScreen(onDismissRequest={showPlans=false},title={Text("I miei piani")},text={
            LazyColumn(Modifier.fillMaxSize()) {
                if(plans.isEmpty())item{Text("Nessun piano salvato.",color=Color.Gray)}
                items(plans){saved->
                    Card(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{plan=saved;showPlans=false}) {
                        Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(saved.goal,fontWeight=FontWeight.Bold)
                                Text(saved.weeks.toString()+" settimane • "+saved.daysPerWeek+" sessioni/settimana",fontSize=12.sp,color=Color.Gray)
                                Text(if(saved.id in enabledPlanIds)"ATTIVO • influenza Coach" else "DISATTIVATO",color=if(saved.id in enabledPlanIds)Accent else Color.Gray,fontSize=11.sp,fontWeight=FontWeight.Bold)
                            }
                            IconButton(onClick={plan=saved;showPlans=false;editPlan=true}){Icon(Icons.Default.Edit,"Modifica piano")}
                            IconButton(onClick={confirmDelete=saved}){Icon(Icons.Default.Delete,"Elimina piano")}
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
private fun Library(profile: TrainingLocation, athlete: AthleteProfile, sessions: List<SessionRecord>, onExecuteExercise:(Exercise)->Unit, onStartWorkout:(WorkoutLaunchIntent)->Unit) {
    Box(Modifier.fillMaxSize()) {

    var selectedSkill by remember { mutableStateOf<String?>(null) }
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
 Button(onClick={onStartWorkout(WorkoutLaunchIntent(WorkoutOrigin.LIBRARY,focusSkills=skillFilters.toList(),equipmentFilters=equipmentFilters.toList(),groupFilters=groupFilters.toList(),lockFilters=true))},enabled=shown.isNotEmpty(),modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth()){Text("GENERA WORKOUT DAI FILTRI")}
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
        FlowScreen(onDismissRequest={filterDialog=null},title={Text(title)},text={LazyColumn(Modifier.fillMaxSize()){items(opts){o->Row(Modifier.fillMaxWidth().clickable{if(o in target)target.remove(o) else target.add(o)},verticalAlignment=Alignment.CenterVertically){Checkbox(o in target,{checked->if(checked){if(o !in target)target.add(o)}else target.remove(o)});Column(Modifier.clickable(enabled=type=="skills"){if(type=="skills"){selectedSkill=o;filterDialog=null}}){Text(o);if(type=="skills")calisthenicsSkills.firstOrNull{it.name==o}?.let{Text(it.category+" • "+it.level,color=Color.Gray,fontSize=12.sp)}}}}}},confirmButton={Button(onClick={filterDialog=null}){Text("APPLICA")}},dismissButton={TextButton(onClick={target.clear()}){Text("AZZERA")}})
    }
    selected?.let{ex->ExerciseDetailScreen(ex=ex,sessions=sessions,onDismiss={selected=null},onSkillClick={skill->selected=null;selectedSkill=skill},onExecute={selected=null;onExecuteExercise(ex)})}
    selectedSkill?.let { skill -> SkillDetailScreen(skill, athlete, { selectedSkill=null }, { onStartWorkout(WorkoutLaunchIntent(WorkoutOrigin.SKILL,focusSkills=listOf(skill)));selectedSkill=null }, sessions) }

    }
}
@Composable
private fun SkillDetailScreen(skillName: String, athlete: AthleteProfile, onDismiss: () -> Unit, onGenerate: () -> Unit, sessions:List<SessionRecord>) {
    val states=SkillCatalog.progress(skillName,sessions)
    val percent=ProgressionEngine.skillProgress(states)
    FlowScreen(onDismissRequest=onDismiss,title={Text(skillName)},text={LazyColumn(Modifier.fillMaxSize()){
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
    if(showCreator) TrainingLocationScreen(null,{showCreator=false}){created->addProfile(created);showCreator=false}
    editing?.let{old->TrainingLocationScreen(old,{editing=null}){updated->editProfile(old,updated);editing=null}}
    deleting?.let{p->AlertDialog(onDismissRequest={deleting=null},title={Text("Elimina profilo?")},text={Text("Vuoi eliminare "+p.name+"?")},confirmButton={Button(onClick={deleteProfile(p);deleting=null}){Text("ELIMINA")}},dismissButton={TextButton(onClick={deleting=null}){Text("ANNULLA")}})}
}
@Composable
private fun Progress(athlete: AthleteProfile, sessions: List<SessionRecord>, onAthleteSave: (AthleteProfile) -> Unit, onDeleteSession: (Long) -> Unit, showAthleteProfile:Boolean=true) {
    Box(Modifier.fillMaxSize()) {

    var pendingDeletion by remember { mutableStateOf<SessionRecord?>(null) }
    var edit by remember { mutableStateOf(false) }
    var selectedSession by remember { mutableStateOf<SessionRecord?>(null) }
    val trends=remember(sessions){progressSummary(sessions,java.time.LocalDate.now().toEpochDay())}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        item{Header("Progressi","Prestazioni effettive e continuità")}
        if(showAthleteProfile)item{OutlinedButton(onClick={edit=true}){Text("MODIFICA PROFILO")}}
        item{
            Text("Continuità • ultime quattro settimane",style=MaterialTheme.typography.titleMedium)
            Text("${trends.weeks.count{it.sessions>0}}/4 settimane con attività registrata")
            trends.weeks.forEach{week->Text("Dal ${calendarDate(week.startEpochDay)}: ${week.sessions} sessioni • ${week.completedSets} serie • ${week.volumeKg.toInt()} kg di volume")}
            Text("Il volume è ripetizioni × carico effettivo. Le settimane correnti possono essere incomplete.",color=Color.Gray,fontSize=12.sp)
        }
        item{Text("Ultima prestazione / precedente",style=MaterialTheme.typography.titleMedium)}
        if(trends.exercises.isEmpty())item{Text("Registra serie eseguite per vedere le tendenze.")}
        items(trends.exercises){trend->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){
            Text(trend.name,fontWeight=FontWeight.Bold)
            trend.latestReps?.let{Text("Ripetizioni migliori: $it / ${trend.previousReps?:"—"}")}
            trend.latestLoad?.let{Text("Carico massimo: $it / ${trend.previousLoad?:"—"} kg")}
            trend.latestSeconds?.let{Text("Durata migliore: $it / ${trend.previousSeconds?:"—"} s")}
            Text("Confronta a parità di tecnica e condizioni; i valori possono provenire da serie diverse.",fontSize=11.sp,color=Color.Gray)
        }}}
        item{Text("Skill",style=MaterialTheme.typography.titleMedium)}
        val practiced=sessions.flatMap{it.structuredExercises}.filter{e->e.sets.any{it.completed}}.flatMap{it.exerciseSnapshot?.skills.orEmpty()}.distinct()
        items(practiced){skill->val states=SkillCatalog.progress(skill,sessions);Text(skill+" • "+ProgressionEngine.skillProgress(states)+"% consolidato")}
        item{Text("Storico allenamenti",style=MaterialTheme.typography.titleMedium)}
        if(sessions.isEmpty())item{Text("Completa un allenamento per iniziare lo storico.",color=Color.Gray)}
        items(sessions){session->Card(Modifier.fillMaxWidth().clickable{selectedSession=session}){Column(Modifier.padding(12.dp)){
            Text(session.profileName,fontWeight=FontWeight.Bold)
            Text("${session.completedSets}/${session.totalSets} serie • "+java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",java.util.Locale.ITALY).format(java.util.Date(session.timestamp)))
            TextButton(onClick={pendingDeletion=session}){Text("ELIMINA")}
        }}}
    }
    pendingDeletion?.let { doomed ->
        AlertDialog(onDismissRequest={pendingDeletion=null},title={Text("Eliminare questo allenamento?")},
            text={Text("La rimozione aggiorna progressi, Skill Tree e Adaptive Coach. L'eventuale sessione collegata torna disponibile nel programma.")},
            confirmButton={TextButton(onClick={onDeleteSession(doomed.timestamp);pendingDeletion=null;selectedSession=null}){Text("ELIMINA")}},
            dismissButton={TextButton(onClick={pendingDeletion=null}){Text("ANNULLA")}})
    }
    if(edit) AthleteProfileScreen(athlete,{edit=false}) { onAthleteSave(it); edit=false }
    selectedSession?.let { s ->
        FlowScreen(onDismissRequest={selectedSession=null},title={Text("Dettaglio allenamento")},text={LazyColumn(Modifier.fillMaxSize()){
            item{Text(s.profileName,fontWeight=FontWeight.Bold);Text(java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",java.util.Locale.ITALY).format(java.util.Date(s.timestamp)),color=Color.Gray);Text(s.completedSets.toString()+"/"+s.totalSets+" serie completate",color=Accent,modifier=Modifier.padding(vertical=8.dp));if(s.workoutRpe>0)Text("Fatica workout: "+s.workoutRpe+"/10",fontWeight=FontWeight.Bold);if(s.durationSec>0)Text("Durata effettiva: "+String.format(java.util.Locale.ITALY,"%02d:%02d",s.durationSec/60,s.durationSec%60));Text("Workout programmato / eseguito",fontWeight=FontWeight.Bold)}
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
}

@Composable
internal fun AthleteProfileScreen(current:AthleteProfile,onDismiss:()->Unit,onSave:(AthleteProfile)->Unit){
    var profile by rememberSaveable(stateSaver=athleteDraftSaver()){mutableStateOf(current)}
    var age by rememberSaveable{mutableStateOf(current.age.takeIf{it>0}?.toString().orEmpty())}
    var height by rememberSaveable{mutableStateOf(current.heightCm.takeIf{it>0}?.toString().orEmpty())}
    var weight by rememberSaveable{mutableStateOf(current.weightKg.takeIf{it>0}?.toString().orEmpty())}
    FlowScreen(onDismissRequest=onDismiss,title={Text("Profilo atleta")},text={LazyColumn(Modifier.fillMaxSize()){item{
        OutlinedTextField(profile.name,{profile=profile.copy(name=it)},label={Text("Nome")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(age,{age=it.filter(Char::isDigit)},label={Text("Età (opzionale)")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(height,{height=it.replace(",",".")},label={Text("Altezza cm (opzionale)")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(weight,{weight=it.replace(",",".")},label={Text("Peso kg (opzionale)")},singleLine=true,modifier=Modifier.fillMaxWidth())
        listOf("Non specificato","Uomo","Donna").forEach{x->FilterChip(profile.sex==x,{profile=profile.copy(sex=x)},{Text(x)})}
        Text("Obiettivo degli Extra",fontWeight=FontWeight.Bold)
        listOf("Forza","Ipertrofia","Calisthenics","Fitness generale").forEach{x->FilterChip(profile.goal==x,{profile=profile.copy(goal=x)},{Text(x)})}
        Text("Frequenza desiderata: ${profile.daysPerWeek} giorni")
        Slider(profile.daysPerWeek.toFloat(),{profile=profile.copy(daysPerWeek=it.toInt())},valueRange=1f..7f,steps=5)
        Text("I programmi mantengono la propria configurazione.",color=Color.Gray)
        CapabilityEditor(profile,{profile=it})
    }}},confirmButton={Button(onClick={onSave(profile.copy(name=profile.name.trim(),age=(age.toIntOrNull()?:0).coerceAtLeast(0),heightCm=height.toFloatOrNull()?.takeIf{it.isFinite() && it>=0} ?: 0f,weightKg=weight.toFloatOrNull()?.takeIf{it.isFinite() && it>=0} ?: 0f))}){Text("SALVA")}},dismissButton={TextButton(onClick=onDismiss){Text("ANNULLA")}})
}
@Composable
private fun ExerciseExecutionInputs(ex:Exercise, enabled:Boolean, targetSet:WorkoutSetState, onComplete:(Int?,Int?,Double?,Double?,Double?)->Unit) {
    val inputTarget=runnerInputTarget(ex,targetSet)
    var value by rememberSaveable(ex.name,targetSet.index,inputTarget){mutableStateOf(inputTarget.toString())}
    var load by rememberSaveable(ex.name,targetSet.targetLoadKg){mutableStateOf("")}
    var assistance by rememberSaveable(ex.name,targetSet.targetAssistanceKg){mutableStateOf("")}
    Column {
        targetSet.targetLoadKg?.let{Text("Carico previsto: $it kg",color=Accent,fontSize=12.sp)}
        targetSet.targetAssistanceKg?.let{Text("Assistenza prevista: $it kg",color=Accent,fontSize=12.sp)}
        val label=when(ex.measure){ExerciseMeasure.REPS->if(ex.laterality==Laterality.UNILATERAL)"Ripetizioni per lato" else "Ripetizioni";ExerciseMeasure.SECONDS->"Secondi mantenuti";ExerciseMeasure.DURATION->"Secondi eseguiti";ExerciseMeasure.DISTANCE->"Distanza (m)";ExerciseMeasure.INTERVALS->"Secondi lavoro"}
        OutlinedTextField(value,{value=it.filter{ch->ch.isDigit()||ch=='.'}},label={Text(label)},singleLine=true,enabled=enabled,modifier=Modifier.fillMaxWidth())
        if(ex.laterality==Laterality.UNILATERAL||ex.laterality==Laterality.ALTERNATING) Text(if(ex.laterality==Laterality.UNILATERAL)"Valore riferito a ciascun lato" else "Alternare i lati",color=Color.Gray,fontSize=11.sp)
        if(ex.resistanceMode==ResistanceMode.EXTERNAL_LOAD) OutlinedTextField(load,{load=it.filter{ch->ch.isDigit()||ch=='.'}},label={Text("Carico usato (kg)")},singleLine=true,enabled=enabled,modifier=Modifier.fillMaxWidth())
        if(ex.resistanceMode==ResistanceMode.ASSISTED) OutlinedTextField(assistance,{assistance=it.filter{ch->ch.isDigit()||ch=='.'}},label={Text("Assistenza usata (kg)")},singleLine=true,enabled=enabled,modifier=Modifier.fillMaxWidth())
        if(ex.measure==ExerciseMeasure.INTERVALS) Text("Recupero intervallo: "+(ex.intervalRestSec?:ex.setRestSec)+" sec",color=Accent,fontSize=12.sp)
        Button(onClick={
            val v=value.toDoubleOrNull()?:return@Button
            onComplete(if(ex.measure==ExerciseMeasure.REPS)v.toInt() else null,if(ex.measure==ExerciseMeasure.SECONDS||ex.measure==ExerciseMeasure.DURATION||ex.measure==ExerciseMeasure.INTERVALS)v.toInt() else null,if(ex.measure==ExerciseMeasure.DISTANCE)v else null,load.toDoubleOrNull(),assistance.toDoubleOrNull())
        },enabled=enabled,modifier=Modifier.fillMaxWidth().padding(top=4.dp)){Text("COMPLETA SERIE")}
    }
}

@Composable
private fun CustomWorkoutBuilderScreen(location:TrainingLocation,athlete:AthleteProfile,sessions:List<SessionRecord>,intent:WorkoutLaunchIntent,onDismiss:()->Unit,onMaterialize:(List<Exercise>,WorkoutLaunchIntent)->Unit) {
    val readiness=LocalHealthConnect.current.readiness
    var minutes by rememberSaveable(intent){mutableIntStateOf(intent.durationMinutes)}
    var mode by rememberSaveable(intent){mutableStateOf(if(intent.focusSkills.isNotEmpty())"Skill"else "Parti del corpo")}
    val groups=rememberSaveable(intent,saver=stringListSaver()){mutableStateListOf<String>().apply{addAll(intent.groupFilters)}}
    val skills=rememberSaveable(intent,saver=stringListSaver()){mutableStateListOf<String>().apply{addAll(intent.focusSkills)}}
    var error by remember{mutableStateOf<String?>(null)}
    val locked=intent.origin==WorkoutOrigin.SKILL || intent.lockFilters
    FlowScreen(onDismissRequest=onDismiss,title={Text("Configura allenamento")},text={
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text("Luogo: ${location.name}")
                Text("$minutes minuti • durata stimata",style=MaterialTheme.typography.titleMedium)
                Slider(minutes.toFloat(),{minutes=(it/5).toInt()*5},valueRange=10f..120f,steps=21)
                if(!locked)Row {
                    FilterChip(mode=="Parti del corpo",{mode="Parti del corpo";skills.clear()},{Text("Muscoli")})
                    FilterChip(mode=="Skill",{mode="Skill";groups.clear()},{Text("Skill")})
                }
                error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            }
            if(locked)item{Text("Focus: "+(groups+skills+intent.equipmentFilters).joinToString())}
            else items(if(mode=="Skill")calisthenicsSkills.map{it.name}else recoveryGroups){label->
                val selected=if(mode=="Skill")skills else groups
                Row(Modifier.fillMaxWidth().clickable{if(label in selected)selected.remove(label)else selected.add(label)},verticalAlignment=Alignment.CenterVertically){
                    Checkbox(label in selected,{checked->if(checked){if(label !in selected)selected.add(label)}else selected.remove(label)})
                    Text(label)
                }
            }
        }
    },confirmButton={Button(onClick={
        val candidates=customWorkoutCandidatePool(location,athlete,sessions,mode,skills,groups,equipmentFilters=intent.equipmentFilters)
        var generated=applyCoaching(sequenceWorkout(candidates),coachingContext(sessions,readiness))
        if(mode=="Skill")generated=enforceSkillMasteryPrescription(generated,skills,sessions)
        generated=fitCustomWorkoutDuration(generated,minutes)
        if(generated.isEmpty())error="Nessun esercizio compatibile. Modifica il focus o il luogo."
        else onMaterialize(generated,intent.copy(focusSkills=skills.toList(),groupFilters=groups.toList(),durationMinutes=minutes))
    },enabled=mode!="Skill"||skills.isNotEmpty(),modifier=Modifier.fillMaxWidth()){Text("GENERA ANTEPRIMA")}},
        dismissButton={TextButton(onClick=onDismiss){Text("ANNULLA")}})
}

@Composable
private fun UniversalWorkoutScreen(store:AppStore, restored:WorkoutSessionState, onDismiss:()->Unit, onSave:(SessionRecord)->Boolean) {
    Box(Modifier.fillMaxSize()) {

    var state by remember(restored.id) { mutableStateOf(restored.copy(exercises=restored.exercises.map{it.copy(rpe=runnerEffort(it.rpe))})) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var confirmFinish by remember { mutableStateOf(false) }
    var technique by remember { mutableStateOf<Exercise?>(null) }
    val context=androidx.compose.ui.platform.LocalContext.current
    val listState=rememberLazyListState()
    fun update(next:WorkoutSessionState) {
        if(store.saveActiveWorkout(next)) state=next
        else android.widget.Toast.makeText(context,"Stato non salvato: riprova prima di uscire.",android.widget.Toast.LENGTH_LONG).show()
    }
    fun suspendRunner() {
        val paused=WorkoutRunnerEngine.pause(state,System.currentTimeMillis())
        if(store.saveActiveWorkout(paused)){state=paused;onDismiss()}
    }
    val paused=state.pausedAtEpochMs!=null
    val remaining=state.pausedRestRemainingSec ?: remainingSeconds(state.restEndsAtEpochMs,now)
    val doneSets=state.exercises.sumOf{e->e.sets.count{it.completed}}
    val totalSets=state.exercises.sumOf{it.sets.size}
    LaunchedEffect(paused) { while(!paused){now=System.currentTimeMillis();val advanced=TimedRunner.tick(state,now);if(advanced!=state)update(advanced);delay(250)} }
    LaunchedEffect(state.currentExerciseIndex) {
        if(state.exercises.isNotEmpty())listState.animateScrollToItem(state.currentExerciseIndex+1)
    }
    FlowScreen(onDismissRequest=::suspendRunner,title={Text("Allenamento • "+state.profileName)},text={
        Column(Modifier.fillMaxSize()) {
            Text("Tempo allenamento: "+elapsedWorkoutSeconds(state,now).let{String.format(java.util.Locale.ITALY,"%02d:%02d",it/60,it%60)},fontWeight=FontWeight.Bold)
            if(paused)Text("IN PAUSA",color=Accent)
            if(remaining>0)Text("Recupero ${state.restKind?.lowercase().orEmpty()}: $remaining sec",color=Accent,fontSize=18.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick={val time=System.currentTimeMillis();update(if(paused)WorkoutRunnerEngine.resume(state,time) else WorkoutRunnerEngine.pause(state,time))}){Text(if(paused)"RIPRENDI" else "PAUSA")}
                if(remaining>0)TextButton(onClick={val time=System.currentTimeMillis();update(skipRunnerRest(state,time))}){Text("SALTA RECUPERO")}
            }
            Row {
                TextButton(onClick={update(WorkoutRunnerEngine.select(state,state.currentExerciseIndex-1))},enabled=state.currentExerciseIndex>0){Text("PRECEDENTE")}
                TextButton(onClick={update(WorkoutRunnerEngine.select(state,state.currentExerciseIndex+1))},enabled=state.currentExerciseIndex<state.exercises.lastIndex){Text("SUCCESSIVO")}
            }
            LazyColumn(state=listState,modifier=Modifier.weight(1f)) {
                item { LinearProgressIndicator(progress={if(totalSets==0)0f else doneSets.toFloat()/totalSets},modifier=Modifier.fillMaxWidth().padding(vertical=12.dp)) }
                itemsIndexed(state.exercises,key={index,_->index}) { index,saved ->
                    val ex=requireNotNull(saved.exerciseSnapshot).copy(measure=saved.measure,
                        setRestSec=saved.setRestSec,exerciseRestSec=saved.exerciseRestSec,
                        intervalRestSec=if(saved.measure==ExerciseMeasure.INTERVALS)saved.setRestSec else saved.exerciseSnapshot.intervalRestSec)
                    val count=saved.sets.count{it.completed}
                    val skipped=saved.status==WorkoutExerciseStatus.SKIPPED
                    val done=count==saved.sets.size
                    val current=index==state.currentExerciseIndex
                    val target=saved.sets.firstOrNull{!it.completed} ?: saved.sets.lastOrNull()
                    Card(Modifier.fillMaxWidth().padding(vertical=5.dp).clickable{update(WorkoutRunnerEngine.select(state,index))}) {
                        Column(Modifier.padding(12.dp)) {
                            Text((if(done)"✓ " else if(skipped)"↷ " else if(current)"▶ " else "")+saved.exerciseName,fontWeight=FontWeight.Bold,fontSize=if(current)24.sp else 16.sp)
                            Text(if(done)"COMPLETATO • $count/${saved.sets.size} serie" else "Serie ${(count+1).coerceAtMost(saved.sets.size)}/${saved.sets.size}",color=Accent,fontSize=12.sp)
                            if(current) {
                                Text("${saved.sets.size} serie • "+when(saved.measure){
                                    ExerciseMeasure.REPS->"${target?.targetReps} rip"
                                    ExerciseMeasure.DISTANCE->"${target?.targetDistanceMeters} m"
                                    else->"${target?.targetSeconds} sec"
                                }+" • ${saved.setRestSec}s serie / ${saved.exerciseRestSec}s esercizio",color=Accent,fontSize=12.sp)
                                TextButton(onClick={technique=ex}){Text("COME ESEGUIRLO")}
                                Text("RPE della prossima serie: ${runnerEffort(saved.rpe)}/10",fontSize=12.sp)
                                Slider(value=runnerEffort(saved.rpe).toFloat(),onValueChange={value->update(state.copy(exercises=state.exercises.mapIndexed{i,e->if(i==index)e.copy(rpe=value.toInt())else e}))},valueRange=1f..10f,steps=8)
                                if(!done)TextButton(onClick={val stopped=if(state.timedSet?.exerciseIndex==index)TimedRunner.stop(state,System.currentTimeMillis())else state;update(if(skipped)WorkoutRunnerEngine.unskip(stopped,index)else WorkoutRunnerEngine.skip(stopped,index))}){Text(if(skipped)"RIPRENDI ESERCIZIO" else "SALTA ESERCIZIO")}
                                if(!done&&!skipped) {
                                    key(state.id,index) {
                                        if(saved.measure in setOf(ExerciseMeasure.SECONDS,ExerciseMeasure.DURATION,ExerciseMeasure.INTERVALS))TimedExecutionControls(state,index,now,::update) else
                                        ExerciseExecutionInputs(ex,!paused&&remaining==0&&state.timedSet==null,targetSet=requireNotNull(target)) { reps,seconds,distance,load,assistance ->
                                            val setIndex=saved.sets.indexOfFirst{!it.completed}
                                            if(setIndex>=0){
                                                update(completeRunnerSetWithRest(state,index,setIndex,System.currentTimeMillis(),reps,seconds,distance,load,assistance))
                                            }
                                        }
                                    }
                                }
                                if(state.workoutOrigin==WorkoutOrigin.SINGLE_EXERCISE.name)OutlinedButton(onClick={val time=System.currentTimeMillis();update(TimedRunner.start(WorkoutRunnerEngine.addSet(state,index),time,afterRest=remaining>0))},enabled=state.timedSet==null){Text("AGGIUNGI SERIE")}
                                if(count>0)TextButton(onClick={update(WorkoutRunnerEngine.undo(state,index))},enabled=state.timedSet==null){Text("ANNULLA ULTIMA SERIE")}
                            }
                        }
                    }
                }
                item {
                    Text("Fatica complessiva workout: ${runnerEffort(state.workoutRpe)}/10",fontWeight=FontWeight.Bold)
                    Slider(value=runnerEffort(state.workoutRpe).toFloat(),onValueChange={update(state.copy(workoutRpe=it.toInt()))},valueRange=1f..10f,steps=8)
                }
            }
        }
    },confirmButton={Button(onClick={confirmFinish=true},enabled=totalSets>0){Text(if(doneSets==totalSets)"TERMINA E SALVA"else "SALVA WORKOUT PARZIALE")}},
        dismissButton={TextButton(onClick=::suspendRunner){Text("SOSPENDI")}})
    if(confirmFinish)AlertDialog(onDismissRequest={confirmFinish=false},title={Text("Terminare e salvare il workout?")},
        text={Text("Serie completate: $doneSets/$totalSets.")},
        confirmButton={Button(onClick={val time=System.currentTimeMillis();val stopped=TimedRunner.stop(state,time).copy(timedSet=null);update(stopped);if(onSave(runnerSessionRecord(stopped,time))){confirmFinish=false;onDismiss()}}){Text("SALVA WORKOUT")}},
        dismissButton={TextButton(onClick={confirmFinish=false}){Text("CONTINUA ALLENAMENTO")}})

    technique?.let{ex->ExerciseDetailScreen(ex,store.loadSessions(),{technique=null},{},{technique=null},actionLabel="TORNA ALL'ALLENAMENTO")}

    }
}

@Composable
internal fun ExerciseDetailScreen(ex: Exercise, sessions: List<SessionRecord>, onDismiss: () -> Unit, onSkillClick:(String)->Unit, onExecute:()->Unit, actionLabel:String="ESEGUI") {
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
    FlowScreen(
        onDismissRequest = onDismiss,
        title = { Text(ex.name) },
        text = {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Text(ex.group + " • " + ex.equipment + " • " + ex.level, color = Color.Gray)
                Spacer(Modifier.height(14.dp))
                Text("Come eseguirlo", fontWeight = FontWeight.Bold)
                Text(when(v2Record?.executionEvidence){"SPECIFIC_SOURCE"->"Indicazioni specifiche con fonte nel catalogo";"FAMILY_GUIDANCE"->"Indicazioni generali della famiglia di movimento";else->"Indicazioni tecniche non verificate"},color=Color.Gray,fontSize=12.sp)
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
                    Text("Livello attuale: "+ex.name,fontWeight=FontWeight.Bold)
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
        confirmButton = { Button(onClick=onExecute){Text(actionLabel)} },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CHIUDI") } }
    )
}

@Composable
internal fun TrainingLocationScreen(current: TrainingLocation? = null, onDismiss: () -> Unit, onCreate: (TrainingLocation) -> Unit) {
    var name by remember(current) { mutableStateOf(current?.name ?: "") }
    val options = profileEquipmentOptions(exercises)
    val selected = remember(current) { mutableStateListOf<EquipmentId>().apply { addAll(current?.capabilities ?: emptySet()) } }

    FlowScreen(
        onDismissRequest = onDismiss,
        title = { Text(if(current==null)"Nuovo profilo attrezzatura" else "Modifica profilo attrezzatura") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
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


/** Debug-only, read-only view of the Application-owned V3 runtime. */
@Composable
private fun V3DiagnosticsScreen(
    component: com.hybridfitnesscoach.app.domain.v3.V3RuntimeComponent,
    active: TrainingLocation
) {
    val state by component.state.collectAsState()
    // Reading diagnostics does not start loading; the existing Application component owns it.
    val diagnostics = component.diagnostics()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Diagnostica V3 — solo debug", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        if (diagnostics == null) {
            item { Text("Diagnostica non disponibile in questa build.") }
        } else {
            item { Text("Stato: ${diagnostics.state}", fontWeight = FontWeight.Bold) }
            item { Text("Record caricati: ${diagnostics.recordCount}") }
            item { Text("Schema: ${diagnostics.schemaVersion ?: "Non disponibile"}") }
            item { Text("Tentativi di caricamento: ${diagnostics.loadAttempts}") }
            item { Text("Durata: ${diagnostics.loadDurationMillis?.let { "${it} ms" } ?: "Non disponibile"}") }
            item { Text("Caricamento sul thread UI: ${diagnostics.loadThreadIsMain?.toString() ?: "Non disponibile"}") }
            item { Text("Luogo selezionato: ${active.name}") }
            item { Text("Luogo runtime: ${diagnostics.activeLocationId ?: "Non disponibile"}") }
            item { Text("Attrezzature riconosciute: ${diagnostics.recognizedEquipmentCount}") }
            item { Text("Attrezzature native non mappate: ${diagnostics.unmappedNativeEquipmentCount}") }
            item { Text("Errore: ${diagnostics.error ?: "Nessuno"}") }
            if (state is com.hybridfitnesscoach.app.domain.v3.V3RuntimeState.Ready) {
                val ready = state as com.hybridfitnesscoach.app.domain.v3.V3RuntimeState.Ready
                item {
                    Text(
                        "Attrezzature V3: " + ready.environment.environment.equipment.joinToString { it.value }
                    )
                }
                item {
                    Text(
                        "Attrezzature native non mappate: " +
                            ready.environment.unmappedNativeEquipment.joinToString()
                    )
                }
            }
        }
    }
}

