package com.hybridfitnesscoach.app

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
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
import org.json.JSONArray
import org.json.JSONObject

private val Accent = Color(0xFF3568E8)
private val Bg = Color(0xFFF7F8FA)

data class EquipmentProfile(val name: String, val icon: String, val equipment: List<String>)
private fun recommendedSetRest(role:String,name:String,equipment:String):Int {
    val n=name.lowercase(); val e=equipment.lowercase()
    val isSkill=n.contains("lever")||n.contains("planche")||n.contains("handstand")||n.contains("muscle-up")||n.contains("flag")||n.contains("one arm")
    val isHeavyCompound=n.contains("squat")||n.contains("deadlift")||n.contains("stacco")||n.contains("panca")||n.contains("bench")||n.contains("press")||n.contains("row")||n.contains("rematore")||n.contains("trazioni")||n.contains("pull-up")||n.contains("dip")
    val isIsolation=n.contains("curl")||n.contains("extension")||n.contains("alzate")||n.contains("raise")||n.contains("fly")||n.contains("croci")||n.contains("calf")||n.contains("pushdown")
    return when {
        isSkill && (role=="Progressione"||role=="Forza") -> 180
        role=="Forza" && isHeavyCompound -> 180
        role=="Forza" -> 150
        role=="Progressione" -> 150
        role=="Prerequisito" && isHeavyCompound -> 120
        isIsolation -> 75
        e.contains("macchina")||e.contains("cavi") -> 90
        else -> 90
    }
}
private fun recommendedExerciseRest(role:String,name:String,equipment:String):Int {
    val set=recommendedSetRest(role,name,equipment)
    return when { set>=180->240; set>=150->180; set>=120->150; else->120 }
}
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
    val currentOrder: Int = originalOrder
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
    val modifications: List<String> = emptyList()
)
private fun WorkoutSetState.toJson() = JSONObject()
    .put("index", index).put("targetReps", targetReps).put("targetSeconds", targetSeconds)
    .put("targetDistanceMeters", targetDistanceMeters).put("targetLoadKg", targetLoadKg)
    .put("targetAssistanceKg", targetAssistanceKg).put("actualReps", actualReps)
    .put("actualSeconds", actualSeconds).put("actualDistanceMeters", actualDistanceMeters)
    .put("actualLoadKg", actualLoadKg).put("actualAssistanceKg", actualAssistanceKg)
    .put("completed", completed).put("restUsedSec", restUsedSec)
private fun JSONObject.optNullableInt(key:String):Int? = if(has(key)&&!isNull(key)) getInt(key) else null
private fun JSONObject.optNullableLong(key:String):Long? = if(has(key)&&!isNull(key)) getLong(key) else null
private fun JSONObject.optNullableDouble(key:String):Double? = if(has(key)&&!isNull(key)) getDouble(key) else null
private fun workoutSetFromJson(o:JSONObject)=WorkoutSetState(
    o.optInt("index"),o.optNullableInt("targetReps"),o.optNullableInt("targetSeconds"),
    o.optNullableDouble("targetDistanceMeters"),o.optNullableDouble("targetLoadKg"),o.optNullableDouble("targetAssistanceKg"),
    o.optNullableInt("actualReps"),o.optNullableInt("actualSeconds"),o.optNullableDouble("actualDistanceMeters"),
    o.optNullableDouble("actualLoadKg"),o.optNullableDouble("actualAssistanceKg"),o.optBoolean("completed"),o.optNullableInt("restUsedSec"))
private fun WorkoutExerciseState.toJson():JSONObject { val a=JSONArray();sets.forEach{a.put(it.toJson())};return JSONObject()
    .put("exerciseName",exerciseName).put("measure",measure.name).put("sets",a).put("status",status.name)
    .put("rpe",rpe).put("pinned",pinned).put("setRestSec",setRestSec).put("exerciseRestSec",exerciseRestSec)
    .put("originalOrder",originalOrder).put("currentOrder",currentOrder) }
private fun workoutExerciseFromJson(o:JSONObject):WorkoutExerciseState { val a=o.optJSONArray("sets")?:JSONArray();return WorkoutExerciseState(
    o.getString("exerciseName"),runCatching{ExerciseMeasure.valueOf(o.optString("measure","REPS"))}.getOrDefault(ExerciseMeasure.REPS),
    (0 until a.length()).map{workoutSetFromJson(a.getJSONObject(it))},
    runCatching{WorkoutExerciseStatus.valueOf(o.optString("status","PENDING"))}.getOrDefault(WorkoutExerciseStatus.PENDING),
    o.optNullableInt("rpe"),o.optBoolean("pinned"),o.optInt("setRestSec",90),o.optInt("exerciseRestSec",120),o.optInt("originalOrder"),o.optInt("currentOrder")) }
private fun WorkoutSessionState.toJson():JSONObject { val a=JSONArray();exercises.forEach{a.put(it.toJson())};return JSONObject()
    .put("schemaVersion",1).put("id",id).put("startedAtEpochMs",startedAtEpochMs).put("profileName",profileName)
    .put("exercises",a).put("currentExerciseIndex",currentExerciseIndex).put("elapsedBeforePauseMs",elapsedBeforePauseMs)
    .put("pausedAtEpochMs",pausedAtEpochMs).put("restEndsAtEpochMs",restEndsAtEpochMs).put("workoutRpe",workoutRpe).put("finishedAtEpochMs",finishedAtEpochMs)
    .put("runnerKind",runnerKind).put("restKind",restKind).put("modifications",JSONArray().apply{modifications.forEach{put(it)}}) }
private fun workoutSessionFromJson(o:JSONObject):WorkoutSessionState { val a=o.optJSONArray("exercises")?:JSONArray();return WorkoutSessionState(
    o.getString("id"),o.getLong("startedAtEpochMs"),o.optString("profileName",""),
    (0 until a.length()).map{workoutExerciseFromJson(a.getJSONObject(it))},o.optInt("currentExerciseIndex"),
    o.optLong("elapsedBeforePauseMs",0L),o.optNullableLong("pausedAtEpochMs"),o.optNullableLong("restEndsAtEpochMs"),
    o.optNullableInt("workoutRpe"),o.optNullableLong("finishedAtEpochMs"),o.optString("runnerKind","automatic"),o.optString("restKind",null),
    (o.optJSONArray("modifications")?:JSONArray()).let{a->(0 until a.length()).map{a.getString(it)}}) }

data class AthleteProfile(val name: String = "", val goal: String = "Forza + skill", val level: String = "Intermedio", val daysPerWeek: Int = 3, val age:Int=0, val heightCm:Float=0f, val weightKg:Float=0f, val sex:String="Non specificato", val prioritySkills:List<String> = emptyList())
data class SessionRecord(val timestamp: Long, val profileName: String, val completedSets: Int, val totalSets: Int, val exerciseNames: List<String> = emptyList(), val exerciseRpe: Map<String,Int> = emptyMap(), val workoutRpe:Int=0, val completedByExercise:Map<String,Int> = emptyMap(), val durationSec:Int=0, val structuredExercises:List<WorkoutExerciseState> = emptyList(), val skippedExercises:List<String> = emptyList(), val modifications:List<String> = emptyList(), val plannedWorkoutId:String?=null)
data class CalisthenicsSkill(val name:String, val category:String, val level:String)

private class AppStore(context: Context) {
    private val prefs = context.getSharedPreferences("hybrid_fitness_store", Context.MODE_PRIVATE)
    fun loadAthlete():AthleteProfile { val storedGoal=prefs.getString("athlete_goal","Forza") ?: "Forza"; val goal=if(storedGoal=="Forza + skill"||storedGoal=="Forza + Skill Calisthenics") "Calisthenics" else storedGoal; return AthleteProfile(prefs.getString("athlete_name","") ?: "", goal, prefs.getString("athlete_level","Intermedio") ?: "Intermedio", prefs.getInt("athlete_days",3),prefs.getInt("athlete_age",0),prefs.getFloat("athlete_height",0f),prefs.getFloat("athlete_weight",0f),prefs.getString("athlete_sex","Non specificato")?:"Non specificato",(prefs.getString("athlete_priority_skills","")?:"").split("|").filter{it.isNotBlank()}) }
    fun saveAthlete(p: AthleteProfile) { prefs.edit().putString("athlete_name",p.name).putString("athlete_goal",p.goal).putString("athlete_level",p.level).putInt("athlete_days",p.daysPerWeek).putInt("athlete_age",p.age).putFloat("athlete_height",p.heightCm).putFloat("athlete_weight",p.weightKg).putString("athlete_sex",p.sex).putString("athlete_priority_skills",p.prioritySkills.joinToString("|")).apply(); if(p.weightKg>0){val h=prefs.getString("weight_history","")?:"";prefs.edit().putString("weight_history",(System.currentTimeMillis().toString()+":"+p.weightKg+";"+h).take(4000)).apply()} }
    fun loadActiveProfileName() = prefs.getString("active_equipment","Palestra") ?: "Palestra"
    fun saveActiveProfileName(name:String) { prefs.edit().putString("active_equipment",name).apply() }
    fun loadCustomProfiles(): List<EquipmentProfile> = try {
        if(!prefs.getBoolean("equipment_profiles_migrated",false)){
            val a=JSONArray();profiles.forEach{p->val e=JSONArray();p.equipment.forEach{e.put(it)};a.put(JSONObject().put("name",p.name).put("icon",p.icon).put("equipment",e))}
            prefs.edit().putString("custom_profiles",a.toString()).putBoolean("equipment_profiles_migrated",true).apply()
        }
        val a=JSONArray(prefs.getString("custom_profiles","[]") ?: "[]")
        (0 until a.length()).map { i -> val o=a.getJSONObject(i); val e=o.getJSONArray("equipment"); EquipmentProfile(o.getString("name"),o.optString("icon","📍"),(0 until e.length()).map { e.getString(it) }) }
    } catch(_:Exception){ emptyList() }
    fun saveCustomProfiles(items:List<EquipmentProfile>) { val a=JSONArray(); items.forEach { p -> val e=JSONArray(); p.equipment.forEach{e.put(it)}; a.put(JSONObject().put("name",p.name).put("icon",p.icon).put("equipment",e)) }; prefs.edit().putString("custom_profiles",a.toString()).apply() }
    fun saveActiveWorkout(state:WorkoutSessionState) { prefs.edit().putString("active_workout_v1",state.toJson().toString()).commit() }
    fun loadActiveWorkout():WorkoutSessionState? = try { prefs.getString("active_workout_v1",null)?.let{workoutSessionFromJson(JSONObject(it))} } catch(_:Exception){ null }
    fun clearActiveWorkout() { prefs.edit().remove("active_workout_v1").commit() }
    fun saveTrainingPlan(plan:TrainingPlan) {
        val workouts=JSONArray()
        plan.workouts.forEach { w ->
            val names=JSONArray(); w.exercises.forEach { names.put(it.name) }
            workouts.put(JSONObject().put("id",w.id).put("epochDay",w.epochDay).put("weekIndex",w.weekIndex)
                .put("sequenceInWeek",w.sequenceInWeek).put("exerciseNames",names).put("state",w.state.name)
                .put("originalEpochDay",w.originalEpochDay).put("adaptationReason",w.adaptationReason)
                .put("actualExecutionEpochDay",w.actualExecutionEpochDay))
        }
        val root=JSONObject().put("schemaVersion",1).put("id",plan.id).put("startEpochDay",plan.startEpochDay)
            .put("weeks",plan.weeks).put("daysPerWeek",plan.daysPerWeek).put("workouts",workouts)
        prefs.edit().putString("training_plan_v091",root.toString()).commit()
    }
    fun loadTrainingPlan():TrainingPlan? = try {
        val root=JSONObject(prefs.getString("training_plan_v091",null) ?: return null)
        val workouts=root.optJSONArray("workouts") ?: JSONArray()
        TrainingPlan(root.getString("id"),root.getLong("startEpochDay"),root.getInt("weeks"),root.getInt("daysPerWeek"),
            (0 until workouts.length()).map { i ->
                val o=workouts.getJSONObject(i); val names=o.optJSONArray("exerciseNames") ?: JSONArray()
                val planned=(0 until names.length()).mapNotNull { n -> exercises.firstOrNull { it.name==names.getString(n) } }
                PlannedWorkout(o.getString("id"),o.getLong("epochDay"),o.getInt("weekIndex"),o.getInt("sequenceInWeek"),planned,
                    runCatching{PlannedWorkoutState.valueOf(o.optString("state","PLANNED"))}.getOrDefault(PlannedWorkoutState.PLANNED),
                    o.optLong("originalEpochDay",o.getLong("epochDay")),o.optString("adaptationReason").takeIf{it.isNotBlank()},
                    o.optNullableLong("actualExecutionEpochDay"))
            })
    } catch(_:Exception) { null }
    fun loadSessions():List<SessionRecord> = try { val a=JSONArray(prefs.getString("sessions","[]") ?: "[]"); (0 until a.length()).map { i -> val o=a.getJSONObject(i); val sx=o.optJSONArray("structuredExercises")?:JSONArray(); val sk=o.optJSONArray("skippedExercises")?:JSONArray(); val mo=o.optJSONArray("modifications")?:JSONArray(); SessionRecord(o.getLong("timestamp"),o.getString("profile"),o.getInt("done"),o.getInt("total"), if(o.has("exercises")) { val e=o.getJSONArray("exercises"); (0 until e.length()).map { e.getString(it) } } else emptyList(), if(o.has("exerciseRpe")){val r=o.getJSONObject("exerciseRpe");r.keys().asSequence().associateWith{r.getInt(it)}}else emptyMap(),o.optInt("workoutRpe",0),if(o.has("completedByExercise")){val r=o.getJSONObject("completedByExercise");r.keys().asSequence().associateWith{r.getInt(it)}}else emptyMap(),o.optInt("durationSec",0),(0 until sx.length()).map{workoutExerciseFromJson(sx.getJSONObject(it))},(0 until sk.length()).map{sk.getString(it)},(0 until mo.length()).map{mo.getString(it)},o.optString("plannedWorkoutId").takeIf{it.isNotBlank()}) }.sortedByDescending{it.timestamp} } catch(_:Exception){ emptyList() }
    fun saveSessions(all:List<SessionRecord>){ val a=JSONArray(); all.distinctBy{it.timestamp}.sortedByDescending{it.timestamp}.forEach{x-> val e=JSONArray(); x.exerciseNames.forEach{e.put(it)};val sx=JSONArray();x.structuredExercises.forEach{sx.put(it.toJson())};val sk=JSONArray();x.skippedExercises.forEach{sk.put(it)};val mo=JSONArray();x.modifications.forEach{mo.put(it)}; a.put(JSONObject().put("timestamp",x.timestamp).put("profile",x.profileName).put("done",x.completedSets).put("total",x.totalSets).put("exercises",e).put("exerciseRpe",JSONObject(x.exerciseRpe.mapValues{it.value})).put("workoutRpe",x.workoutRpe).put("completedByExercise",JSONObject(x.completedByExercise.mapValues{it.value})).put("durationSec",x.durationSec).put("structuredExercises",sx).put("skippedExercises",sk).put("modifications",mo).put("plannedWorkoutId",x.plannedWorkoutId))}; prefs.edit().putString("sessions",a.toString()).apply() }
    fun loadSkillNode(skill:String):String? = prefs.getString("skill_node_"+skill.lowercase().replace(" ","_"),null)
    fun saveSkillNode(skill:String,nodeId:String) { prefs.edit().putString("skill_node_"+skill.lowercase().replace(" ","_"),nodeId).apply() }
    fun addSession(s:SessionRecord){
        val all=listOf(s)+loadSessions()
        saveSessions(all)
    }
    fun completePlannedWorkoutForSession(s:SessionRecord, minimumGapDays:Int=2):ScheduleUpdate? {
        val plan=loadTrainingPlan() ?: return null
        val executionDay=java.time.Instant.ofEpochMilli(s.timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()
        val update=PlannedWorkoutReconciler.complete(plan,s,executionDay,minimumGapDays)
        if(update.plan==plan) return null
        saveTrainingPlan(update.plan)
        return update
    }

    fun deleteSession(timestamp:Long){ saveSessions(loadSessions().filterNot{it.timestamp==timestamp}) }
}

internal val profiles = listOf(
    EquipmentProfile("Palestra", "🏋️", listOf("Bilanciere", "Manubri", "Cavi", "Macchine", "Sbarra", "Parallele", "Panca", "Kettlebell", "Leg press", "Ab Wheel", "Foam Roller")),
    EquipmentProfile("Casa", "🏠", listOf("Corpo libero", "Sbarra", "Elastici", "Ab Wheel", "Foam Roller")),
    EquipmentProfile("Parco", "🌳", listOf("Sbarra", "Parallele", "Corpo libero"))
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
    val planned=recent.sumOf { session -> session.structuredExercises.filter{it.exerciseName in relevantNames}.sumOf{it.sets.size}.takeIf{it>0} ?: relevantNames.sumOf{name->session.completedByExercise[name]?:0} }
    val completed=recent.sumOf { session -> session.structuredExercises.filter{it.exerciseName in relevantNames}.sumOf{ex->ex.sets.count{it.completed}}.takeIf{it>0} ?: relevantNames.sumOf{name->session.completedByExercise[name]?:0} }
    val success=if(planned>0) completed.toDouble()/planned else 0.0
    val rpes=recent.flatMap{session->session.exerciseRpe.filterKeys{it in relevantNames}.values}.filter{it>0}
    val avgRpe=if(rpes.isEmpty()) 7.0 else rpes.average()
    val recoveryMap=recoveryScores(sessions)
    val groups=exercises.filter{it.name in relevantNames}.flatMap{listOf(it.group)+it.secondaryGroups}.filter{it in recoveryGroups}
    val recovery=groups.mapNotNull{recoveryMap[it]}.minOrNull() ?: 100
    var streak=0
    for(session in relevant){
        val plannedSession=session.structuredExercises.filter{it.exerciseName in relevantNames}.sumOf{it.sets.size}
        val doneSession=session.structuredExercises.filter{it.exerciseName in relevantNames}.sumOf{ex->ex.sets.count{it.completed}}
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

internal val exercises: List<Exercise> = (coreExerciseCatalog + extendedExerciseCatalog)
    .distinctBy { it.name.trim().lowercase() }

internal data class ExercisePrescription(val sets:Int,val value:Double,val unit:String)
internal fun prescriptionFor(ex:Exercise):ExercisePrescription {
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
    return WorkoutExerciseState(ex.name,ex.measure,sets,WorkoutExerciseStatus.PENDING,null,false,ex.setRestSec,ex.exerciseRestSec,index,index)
}
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

internal fun equipmentCompatible(ex: Exercise, profile: EquipmentProfile): Boolean {
    fun normalize(value:String)=value.trim().lowercase().let{if(it=="macchina")"macchine" else it}
    val available=profile.equipment.map(::normalize).toSet()+"corpo libero"
    val required=ex.equipment.split("/").map(::normalize)
    // Explicit bodyweight variants are alternatives; other compound labels require every resource.
    return if("corpo libero" in required)true else required.all{it in available}
}

private fun levelScore(level: String) = when(level) { "Principiante" -> 1; "Intermedio" -> 2; else -> 3 }

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

internal fun generateWorkout(profile: EquipmentProfile, athlete: AthleteProfile, sessions: List<SessionRecord>, readinessScore:Int?=null): List<Exercise> {
    val context=coachingContext(sessions,readinessScore)
    val maxLevel = levelScore(athlete.level)
    val eligible=skillEligibleExercises(sessions)
    val available = exercises.filter { it.name in eligible && equipmentCompatible(it, profile) && levelScore(it.level) <= maxLevel }
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
            repeatPenalty + musclePenalty + skillBonus
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HybridFitnessApp() }
    }
}

enum class Tab { HOME, PLAN, LIBRARY, EQUIPMENT, PROGRESS }

@Composable
fun HybridFitnessApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { AppStore(context) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    val customProfiles = remember { mutableStateListOf<EquipmentProfile>().apply { addAll(store.loadCustomProfiles()) } }
    var activeProfile by remember { mutableStateOf(customProfiles.firstOrNull { it.name == store.loadActiveProfileName() } ?: customProfiles.firstOrNull() ?: EquipmentProfile("Nessun profilo","📍",emptyList())) }
    var athlete by remember { mutableStateOf(store.loadAthlete()) }
    var sessions by remember { mutableStateOf(store.loadSessions()) }
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
                        Triple(Tab.LIBRARY, Icons.Default.Search, "Esercizi"),
                        Triple(Tab.EQUIPMENT, Icons.Default.FitnessCenter, "Attrezzatura"),
                        Triple(Tab.PROGRESS, Icons.Default.TrendingUp, "Progressi")
                    ).forEach { (item, icon, label) ->
                        NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(icon, label) }, label = { Text(label) })
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (tab) {
                    Tab.HOME -> Home(activeProfile, athlete, sessions, store, { tab = Tab.EQUIPMENT }) { record -> store.addSession(record); store.completePlannedWorkoutForSession(record); sessions = store.loadSessions() }
                    Tab.PLAN -> TrainingPlanScreen(store)
                    Tab.LIBRARY -> Library(activeProfile, athlete, sessions, store) { record -> store.addSession(record); store.completePlannedWorkoutForSession(record); sessions = store.loadSessions() }
                    Tab.EQUIPMENT -> Equipment(activeProfile, customProfiles,
 { selected -> activeProfile=selected; store.saveActiveProfileName(selected.name) },
 { created -> customProfiles.add(created); store.saveCustomProfiles(customProfiles); activeProfile=created; store.saveActiveProfileName(created.name) },
 { old, updated -> val i=customProfiles.indexOfFirst{it.name==old.name}; if(i>=0){customProfiles[i]=updated;store.saveCustomProfiles(customProfiles);if(activeProfile.name==old.name){activeProfile=updated;store.saveActiveProfileName(updated.name)}} },
 { doomed -> customProfiles.removeAll{it.name==doomed.name};store.saveCustomProfiles(customProfiles);if(activeProfile.name==doomed.name){activeProfile=customProfiles.firstOrNull()?:EquipmentProfile("Nessun profilo","📍",emptyList());store.saveActiveProfileName(activeProfile.name)} }
)
                    Tab.PROGRESS -> Progress(athlete, sessions, { updated -> athlete = updated; store.saveAthlete(updated) }, { timestamp -> store.deleteSession(timestamp); sessions = store.loadSessions() })
                }
            }
        }
    }
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
private fun Home(profile: EquipmentProfile, athlete: AthleteProfile, sessions: List<SessionRecord>, store: AppStore, changeProfile: () -> Unit, onSessionSaved: (SessionRecord) -> Unit) {
    val readinessScore=LocalHealthConnect.current.readiness
    val restoredWorkout = remember { store.loadActiveWorkout() }
    var workoutOpen by remember { mutableStateOf(restoredWorkout != null) }
    var customWorkoutOpen by remember { mutableStateOf(false) }
    var treeSkill by remember { mutableStateOf<String?>(null) }
    var treePicker by remember { mutableStateOf(false) }
    var customSkill by remember { mutableStateOf("Nessuna") }
    var trainingPlan by remember { mutableStateOf(store.loadTrainingPlan()) }
    fun ensurePlan():TrainingPlan {
        trainingPlan?.let { return it }
        val today=java.time.LocalDate.now().toEpochDay()
        val created=TrainingPlanEngine.scheduleImmediately("plan-"+today,today,8,athlete.daysPerWeek.coerceIn(1,7)) { _,_ ->
            generateWorkout(profile,athlete,sessions,readinessScore)
        }
        store.saveTrainingPlan(created); trainingPlan=created; return created
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header(if (athlete.name.isBlank()) "Buongiorno 👋" else "Ciao, " + athlete.name + " 👋", athlete.goal + " • " + athlete.daysPerWeek + " giorni/settimana") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.icon, fontSize = 30.sp); Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text("Ti alleni in", color = Color.Gray); Text(profile.name, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                        TextButton(onClick = changeProfile) { Text("CAMBIA") }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    Text("Allenamento suggerito", fontWeight = FontWeight.SemiBold)
                    val suggested = generateWorkout(profile,athlete,sessions,readinessScore)
                    Text(when(athlete.goal){"Calisthenics"->"Calisthenics"+athlete.prioritySkills.firstOrNull()?.let{" • "+it}.orEmpty();"Ipertrofia"->"Ipertrofia";"Forza"->"Forza";else->"Fitness generale"}, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    Text(suggested.size.toString()+" esercizi  •  "+athlete.goal, color = Color.Gray)
                    Button(onClick = { workoutOpen = true }, modifier = Modifier.fillMaxWidth().padding(top = 18.dp), shape = RoundedCornerShape(14.dp)) { Text("INIZIA ALLENAMENTO") }
                    OutlinedButton(onClick = { customWorkoutOpen = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(14.dp)) { Text("CREA WORKOUT PERSONALIZZATO") }
                }
            }
        }
        item {
            val plan=trainingPlan
            Card(Modifier.padding(horizontal=20.dp,vertical=10.dp).fillMaxWidth(),shape=RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Programma multi-settimana",fontWeight=FontWeight.Bold,fontSize=18.sp)
                    if(plan==null) {
                        Text("Crea un piano di 8 settimane dal profilo atleta e dal generatore adattivo corrente.",fontSize=13.sp,color=Color.Gray)
                        Button(onClick={ensurePlan()},modifier=Modifier.fillMaxWidth().padding(top=10.dp)){Text("CREA PROGRAMMA")}
                    } else {
                        val today=java.time.LocalDate.now().toEpochDay()
                        val next=plan.workouts.filter{it.epochDay>=today && it.state!=PlannedWorkoutState.SKIPPED && it.state!=PlannedWorkoutState.COMPLETED}.minByOrNull{it.epochDay}
                        Text(plan.weeks.toString()+" settimane • "+plan.daysPerWeek+" sessioni/settimana",fontSize=13.sp,color=Color.Gray)
                        Text(next?.let{"Prossima sessione: "+java.time.LocalDate.ofEpochDay(it.epochDay)+" • "+it.exercises.size+" esercizi"}?:"Nessuna sessione futura",modifier=Modifier.padding(top=6.dp))
                    }
                }
            }
        }
        item { Box(Modifier.padding(horizontal=20.dp)) { ReadinessOverview(readinessScore,sessions) } }
        item { Box(Modifier.padding(horizontal=20.dp)) { CoachSummary(coachingContext(sessions,readinessScore)) } }
        item { SectionTitle("Il tuo Skill Tree") }
        items(athlete.prioritySkills.ifEmpty{listOf("Front lever","Planche","Handstand")}.take(3)) { skill ->
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
    if (workoutOpen && restoredWorkout?.runnerKind!="custom") { WorkoutDialog(profile = profile, athlete = athlete, sessions = sessions, store = store, restored = restoredWorkout, onDismiss = { workoutOpen = false }, onSave = onSessionSaved) }
    if (customWorkoutOpen || (workoutOpen && restoredWorkout?.runnerKind=="custom")) { CustomWorkoutDialog(profile, athlete, sessions, { customWorkoutOpen=false;workoutOpen=false }, onSessionSaved, initialSkill=customSkill, store=store, restored=restoredWorkout?.takeIf{it.runnerKind=="custom"}) }
}

@Composable
private fun TrainingPlanScreen(store:AppStore) {
    val today=java.time.LocalDate.now().toEpochDay()
    var plan by remember { mutableStateOf(store.loadTrainingPlan()) }
    var showPast by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<PlannedWorkout?>(null) }
    var rescheduleOffset by remember { mutableStateOf(1) }
    Column(Modifier.fillMaxSize()) {
        Header("Programma","Calendario allenamenti • passato e futuro")
        if(plan==null) {
            Card(Modifier.padding(20.dp).fillMaxWidth()){Text("Crea il programma dalla schermata Oggi.",modifier=Modifier.padding(20.dp))}
        } else {
            Row(Modifier.padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {
                FilterChip(selected=!showPast,onClick={showPast=false},label={Text("Futuri")})
                Spacer(Modifier.width(8.dp))
                FilterChip(selected=showPast,onClick={showPast=true},label={Text("Passati")})
            }
            val visible=plan!!.workouts.filter{if(showPast) it.epochDay<today || it.state==PlannedWorkoutState.COMPLETED || it.state==PlannedWorkoutState.SKIPPED else it.epochDay>=today && it.state!=PlannedWorkoutState.COMPLETED && it.state!=PlannedWorkoutState.SKIPPED}.sortedBy{it.epochDay}
            LazyColumn(contentPadding=PaddingValues(bottom=24.dp)) {
                items(visible,key={it.id}) { workout ->
                    Card(Modifier.padding(horizontal=20.dp,vertical=5.dp).fillMaxWidth().clickable{selected=workout},shape=RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                                Text(java.time.LocalDate.ofEpochDay(workout.epochDay).toString(),fontWeight=FontWeight.Bold)
                                Text(workout.state.name,fontSize=12.sp,color=Accent,fontWeight=FontWeight.SemiBold)
                            }
                            Text("Settimana "+(workout.weekIndex+1)+" • Sessione "+(workout.sequenceInWeek+1),fontSize=13.sp,color=Color.Gray)
                            Text(workout.exercises.joinToString(" • "){it.name},fontSize=12.sp,modifier=Modifier.padding(top=5.dp))
                            if(workout.epochDay!=workout.originalEpochDay) Text("Data originale: "+java.time.LocalDate.ofEpochDay(workout.originalEpochDay),fontSize=11.sp,color=Color.Gray)
                            workout.adaptationReason?.let{Text(it,fontSize=11.sp,color=Color.Gray)}
                            if(workout.state==PlannedWorkoutState.PLANNED || workout.state==PlannedWorkoutState.RESCHEDULED) Text("Tocca per saltare o riprogrammare",fontSize=11.sp,color=Accent,modifier=Modifier.padding(top=6.dp))
                        }
                    }
                }
                if(visible.isEmpty()) item{Text("Nessuna sessione in questa vista.",modifier=Modifier.padding(20.dp),color=Color.Gray)}
            }
        }
    }
    selected?.let { workout ->
        AlertDialog(onDismissRequest={selected=null},title={Text("Gestisci sessione")},text={
            Column {
                Text(java.time.LocalDate.ofEpochDay(workout.epochDay).toString()+" • "+workout.exercises.size+" esercizi")
                Spacer(Modifier.height(12.dp))
                Text("Riprogramma di:",fontWeight=FontWeight.SemiBold)
                Row(verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick={rescheduleOffset=(rescheduleOffset-1).coerceAtLeast(1)}){Icon(Icons.Default.Remove,"Riduci")}
                    Text("+$rescheduleOffset giorni",fontWeight=FontWeight.Bold)
                    IconButton(onClick={rescheduleOffset=(rescheduleOffset+1).coerceAtMost(14)}){Icon(Icons.Default.Add,"Aumenta")}
                }
            }
        },confirmButton={
            TextButton(onClick={
                plan=TrainingPlanEngine.reschedule(plan!!,workout.id,workout.epochDay+rescheduleOffset,minimumGapDays=2)
                store.saveTrainingPlan(plan!!); selected=null;rescheduleOffset=1
            }){Text("RIPROGRAMMA")}
        },dismissButton={
            Row {
                TextButton(onClick={
                    plan=TrainingPlanEngine.skip(plan!!,workout.id)
                    store.saveTrainingPlan(plan!!);selected=null
                }){Text("SALTA")}
                TextButton(onClick={selected=null}){Text("ANNULLA")}
            }
        })
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
private fun Library(profile: EquipmentProfile, athlete: AthleteProfile, sessions: List<SessionRecord>, store:AppStore, onSessionSaved: (SessionRecord) -> Unit) {
    var selectedSkill by remember { mutableStateOf<String?>(null) }
    var skillWorkout by remember { mutableStateOf<String?>(null) }
    var filteredWorkout by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Exercise?>(null) }
    val equipmentFilters = remember { mutableStateListOf<String>() }
    val groupFilters = remember { mutableStateListOf<String>() }
    val skillFilters = remember { mutableStateListOf<String>() }
    val measureFilters = remember { mutableStateListOf<ExerciseMeasure>() }
    var filterDialog by remember { mutableStateOf<String?>(null) }
    val equipmentOptions = listOf("Corpo libero","Sbarra","Parallele","Anelli","TRX","Bilanciere","Manubri","Panca","Cavi","Macchine","Elastici","Kettlebell","Leg press","Ab Wheel","Foam Roller")
    val matchingSkills = calisthenicsSkills.filter { s -> query.isNotBlank() && s.name.contains(query.trim(),true) }
    val shown = exercises.filter { ex ->
        (query.isBlank() || query.trim().split(Regex("\\s+")).all { token -> ex.name.contains(token,true) || ex.group.contains(token,true) || ex.equipment.contains(token,true) || ex.skills.any{it.contains(token,true)} }) &&
        (equipmentFilters.isEmpty() || equipmentFilters.any { eq -> ex.equipment.contains(eq,true) || (eq=="Macchine" && ex.equipment.contains("Macchina",true)) }) &&
        (groupFilters.isEmpty() || groupFilters.any { ex.group.contains(it,true) }) &&
        (skillFilters.isEmpty() || skillFilters.any { it in ex.skills }) &&
        (measureFilters.isEmpty() || ex.measure in measureFilters)
    }
    Column {
        Header("Libreria esercizi", exercises.size.toString()+" esercizi • "+calisthenicsSkills.size+" skill calisthenics")
        OutlinedTextField(query,{query=it},leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Ricerca istantanea: esercizio, muscolo, attrezzo, skill")},modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth(),shape=RoundedCornerShape(16.dp))
        Row(Modifier.padding(horizontal=20.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)){
            FilterChip(selected=equipmentFilters.isNotEmpty(),onClick={filterDialog="equipment"},label={Text("Attrezzi"+if(equipmentFilters.isEmpty())"" else " ("+equipmentFilters.size+")")})
            FilterChip(selected=groupFilters.isNotEmpty(),onClick={filterDialog="groups"},label={Text("Muscoli"+if(groupFilters.isEmpty())"" else " ("+groupFilters.size+")")})
            FilterChip(selected=skillFilters.isNotEmpty(),onClick={filterDialog="skills"},label={Text("Skill"+if(skillFilters.isEmpty())"" else " ("+skillFilters.size+")")})
            FilterChip(selected=measureFilters.isNotEmpty(),onClick={filterDialog="measure"},label={Text("Misurazione"+if(measureFilters.isEmpty())"" else " ("+measureFilters.size+")")})
        }
        if(equipmentFilters.isNotEmpty()||groupFilters.isNotEmpty()||skillFilters.isNotEmpty()||measureFilters.isNotEmpty()) {
 Row(Modifier.padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically){Text(shown.size.toString()+" risultati",color=Color.Gray,modifier=Modifier.weight(1f));TextButton(onClick={equipmentFilters.clear();groupFilters.clear();skillFilters.clear();measureFilters.clear()}){Text("AZZERA")}}
 Button(onClick={filteredWorkout=true},enabled=shown.isNotEmpty(),modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth()){Text("GENERA WORKOUT DAI FILTRI")}
}
        LazyColumn(contentPadding=PaddingValues(horizontal=20.dp,vertical=6.dp)){
            items(matchingSkills){s->Card(Modifier.fillMaxWidth().padding(bottom=10.dp).clickable{selectedSkill=s.name}){Column(Modifier.padding(16.dp)){Text(s.name,fontSize=18.sp,fontWeight=FontWeight.Bold);Text("Skill calisthenics • "+s.category+" • "+s.level,color=Accent)}}}
            items(shown){ex->Card(Modifier.fillMaxWidth().padding(bottom=10.dp).clickable{selected=ex}){Column(Modifier.padding(16.dp)){Text(ex.name,fontSize=18.sp,fontWeight=FontWeight.Bold);Text(ex.group+" • "+ex.equipment+" • "+ex.level,color=Color.Gray);if(ex.skills.isNotEmpty())Text(ex.role+" → "+ex.skills.joinToString(" • "),color=Accent,fontSize=12.sp,modifier=Modifier.padding(top=6.dp))}}}
            if(shown.isEmpty()) item{Text("Nessun esercizio corrisponde ai filtri selezionati.",color=Color.Gray,modifier=Modifier.padding(12.dp))}
        }
    }
    if(filterDialog=="measure") {
        AlertDialog(onDismissRequest={filterDialog=null},title={Text("Filtra per misurazione")},text={Column{ExerciseMeasure.entries.forEach{m->Row(Modifier.fillMaxWidth().clickable{if(m in measureFilters)measureFilters.remove(m)else measureFilters.add(m)},verticalAlignment=Alignment.CenterVertically){Checkbox(m in measureFilters,{checked->if(checked){if(m !in measureFilters)measureFilters.add(m)}else measureFilters.remove(m)});Text(m.name)}}}},confirmButton={Button(onClick={filterDialog=null}){Text("APPLICA")}},dismissButton={TextButton(onClick={measureFilters.clear()}){Text("AZZERA")}})
    }
    filterDialog?.takeIf{it!="measure" && it!="types"}?.let { type ->
        val title=when(type){"equipment"->"Filtra per attrezzatura";"groups"->"Filtra per gruppi muscolari";else->"Filtra per skill"}
        val opts=when(type){"equipment"->equipmentOptions;"groups"->recoveryGroups;else->calisthenicsSkills.map{it.name}}
        val target=when(type){"equipment"->equipmentFilters;"groups"->groupFilters;else->skillFilters}
        AlertDialog(onDismissRequest={filterDialog=null},title={Text(title)},text={LazyColumn(Modifier.heightIn(max=520.dp)){items(opts){o->Row(Modifier.fillMaxWidth().clickable{if(o in target)target.remove(o) else target.add(o)},verticalAlignment=Alignment.CenterVertically){Checkbox(o in target,{checked->if(checked){if(o !in target)target.add(o)}else target.remove(o)});Column(Modifier.clickable(enabled=type=="skills"){if(type=="skills"){selectedSkill=o;filterDialog=null}}){Text(o);if(type=="skills")calisthenicsSkills.firstOrNull{it.name==o}?.let{Text(it.category+" • "+it.level,color=Color.Gray,fontSize=12.sp)}}}}}},confirmButton={Button(onClick={filterDialog=null}){Text("APPLICA")}},dismissButton={TextButton(onClick={target.clear()}){Text("AZZERA")}})
    }
    selected?.let{ex->ExerciseDetailDialog(ex=ex,sessions=sessions,onDismiss={selected=null})}
    selectedSkill?.let { skill -> SkillDetailDialog(skill, athlete, { selectedSkill=null }, { skillWorkout=skill; selectedSkill=null }, sessions) }
    skillWorkout?.let { skill -> CustomWorkoutDialog(profile, athlete, sessions, { skillWorkout=null }, { record -> onSessionSaved(record); skillWorkout=null }, initialSkill=skill,store=store) }
    if(filteredWorkout) CustomWorkoutDialog(profile,athlete,sessions,{filteredWorkout=false},{record->onSessionSaved(record);filteredWorkout=false},initialEquipment=equipmentFilters.toList(),initialGroups=groupFilters.toList(),initialSkill=skillFilters.firstOrNull()?:"Nessuna",filtersLocked=true,store=store)
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
                    Text("${(state.completion*criterion.requiredSessions).toInt()} / ${criterion.requiredSessions} sessioni valide",fontSize=12.sp,color=Color.Gray)
                    LinearProgressIndicator(progress={state.completion.toFloat()},modifier=Modifier.fillMaxWidth().padding(top=6.dp))
                }
            }
        }
    }},confirmButton={Button(onClick=onGenerate){Text("ALLENA QUESTA SKILL")}},dismissButton={TextButton(onClick=onDismiss){Text("CHIUDI")}})
}

@Composable
private fun Equipment(active: EquipmentProfile, customProfiles: List<EquipmentProfile>, select: (EquipmentProfile) -> Unit, addProfile: (EquipmentProfile) -> Unit, editProfile: (EquipmentProfile,EquipmentProfile)->Unit, deleteProfile:(EquipmentProfile)->Unit) {
    var showCreator by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EquipmentProfile?>(null) }
    var deleting by remember { mutableStateOf<EquipmentProfile?>(null) }
    Column {
        Header("Profili attrezzatura", "Scegli dove ti alleni oggi")
        customProfiles.forEach { p ->
                        Card(Modifier.padding(horizontal=20.dp,vertical=6.dp).fillMaxWidth().clickable{select(p)},colors=CardDefaults.cardColors(containerColor=if(p==active)Accent.copy(alpha=.10f) else Color.White)){
                Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){
                    Text(p.icon,fontSize=32.sp);Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)){Text(p.name,fontSize=19.sp,fontWeight=FontWeight.Bold);Text(p.equipment.joinToString(" • "),color=Color.Gray,fontSize=13.sp)}
                    IconButton(onClick={editing=p}){Icon(Icons.Default.Edit,"Modifica")};IconButton(onClick={deleting=p}){Icon(Icons.Default.Delete,"Elimina")}
                    if(p==active)Icon(Icons.Default.CheckCircle,"Attivo",tint=Accent)
                }
            }
        }
        if(customProfiles.isEmpty()) Text("Nessun profilo attrezzatura. Creane uno per generare workout.",color=Accent,modifier=Modifier.padding(horizontal=20.dp));OutlinedButton(onClick={showCreator=true},modifier=Modifier.padding(20.dp).fillMaxWidth()){Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text("NUOVO PROFILO")}
    }
    if(showCreator) EquipmentProfileDialog(null,{showCreator=false}){created->addProfile(created);showCreator=false}
    editing?.let{old->EquipmentProfileDialog(old,{editing=null}){updated->editProfile(old,updated);editing=null}}
    deleting?.let{p->AlertDialog(onDismissRequest={deleting=null},title={Text("Elimina profilo?")},text={Text("Vuoi eliminare "+p.name+"?")},confirmButton={Button(onClick={deleteProfile(p);deleting=null}){Text("ELIMINA")}},dismissButton={TextButton(onClick={deleting=null}){Text("ANNULLA")}})}
}
@Composable
private fun Progress(athlete: AthleteProfile, sessions: List<SessionRecord>, onAthleteSave: (AthleteProfile) -> Unit, onDeleteSession: (Long) -> Unit) {
    var edit by remember { mutableStateOf(false) }
    var selectedSession by remember { mutableStateOf<SessionRecord?>(null) }
    Column {
        Header("Profilo e progressi", "Dati salvati sul dispositivo")
        Card(Modifier.padding(horizontal=20.dp).fillMaxWidth().clickable { edit=true }) {
            Column(Modifier.padding(18.dp)) {
                Text(if(athlete.name.isBlank()) "Configura profilo atleta" else athlete.name, fontSize=20.sp, fontWeight=FontWeight.Bold)
                Text(athlete.goal + " • " + athlete.level + " • " + athlete.daysPerWeek + " giorni/settimana", color=Color.Gray);if(athlete.heightCm>0&&athlete.weightKg>0){val bmi=athlete.weightKg/((athlete.heightCm/100f)*(athlete.heightCm/100f));Text(String.format(java.util.Locale.ITALY,"%.1f kg • %.0f cm • BMI %.1f",athlete.weightKg,athlete.heightCm,bmi),color=Accent,fontSize=13.sp)}
                Text("TOCCA PER MODIFICARE", color=Accent, fontSize=12.sp, modifier=Modifier.padding(top=8.dp))
            }
        }
        SectionTitle("Storico allenamenti")
        if(sessions.isEmpty()) Text("Completa un allenamento per iniziare lo storico.", color=Color.Gray, modifier=Modifier.padding(horizontal=20.dp))
        else LazyColumn(Modifier.padding(horizontal=20.dp).weight(1f,false)){items(sessions){s->Card(Modifier.fillMaxWidth().padding(bottom=8.dp).clickable{selectedSession=s}){Column(Modifier.padding(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(s.profileName,fontWeight=FontWeight.Bold);Text(s.completedSets.toString()+"/"+s.totalSets+" serie • "+java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",java.util.Locale.ITALY).format(java.util.Date(s.timestamp)),color=Color.Gray,fontSize=12.sp);Text("TOCCA PER DETTAGLI",color=Accent,fontSize=11.sp)};TextButton(onClick={onDeleteSession(s.timestamp)}){Text("ELIMINA")}}}}}}
    }
    if(edit) AthleteDialog(athlete,{edit=false}) { onAthleteSave(it); edit=false }
    selectedSession?.let { s ->
        AlertDialog(onDismissRequest={selectedSession=null},title={Text("Dettaglio allenamento")},text={LazyColumn(Modifier.heightIn(max=500.dp)){
            item{Text(s.profileName,fontWeight=FontWeight.Bold);Text(java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",java.util.Locale.ITALY).format(java.util.Date(s.timestamp)),color=Color.Gray);Text(s.completedSets.toString()+"/"+s.totalSets+" serie completate",color=Accent,modifier=Modifier.padding(vertical=8.dp));if(s.workoutRpe>0)Text("Fatica workout: "+s.workoutRpe+"/10",fontWeight=FontWeight.Bold);if(s.durationSec>0)Text("Durata effettiva: "+String.format("%02d:%02d",s.durationSec/60,s.durationSec%60));Text("Workout programmato / eseguito",fontWeight=FontWeight.Bold)}
            items(s.exerciseNames){name->val ex=exercises.firstOrNull{it.name==name};val done=s.completedByExercise[name]?:if(s.completedByExercise.isEmpty())0 else 0;Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){Column(Modifier.padding(10.dp)){Text((if(done>=3)"✓ " else if(done>0)"◐ " else "○ ")+name+"  "+done+"/3",fontWeight=FontWeight.Bold);if(ex!=null){Text(ex.group+" • "+ex.equipment+" • "+ex.role,color=Color.Gray,fontSize=12.sp);val p=prescriptionFor(ex);Text("Prescrizione: "+p.sets+" × "+p.value.toInt()+" "+p.unit,color=Accent,fontSize=12.sp);s.exerciseRpe[name]?.let{Text("Fatica percepita: "+it+"/10",fontWeight=FontWeight.Bold,fontSize=12.sp)}}}}}
        }},confirmButton={TextButton(onClick={selectedSession=null}){Text("CHIUDI")}},dismissButton={TextButton(onClick={onDeleteSession(s.timestamp);selectedSession=null}){Text("ELIMINA SESSIONE")}})
    }
}

@Composable
private fun AthleteDialog(current:AthleteProfile,onDismiss:()->Unit,onSave:(AthleteProfile)->Unit){
    var name by remember{mutableStateOf(current.name)}; var goal by remember{mutableStateOf(current.goal)}; var level by remember{mutableStateOf(current.level)}; var days by remember{mutableIntStateOf(current.daysPerWeek)};var age by remember{mutableStateOf(if(current.age>0)current.age.toString() else "")};var height by remember{mutableStateOf(if(current.heightCm>0)current.heightCm.toString() else "")};var weight by remember{mutableStateOf(if(current.weightKg>0)current.weightKg.toString() else "")};var sex by remember{mutableStateOf(current.sex)};val prioritySkills=remember{mutableStateListOf<String>().apply{addAll(current.prioritySkills)}}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Profilo atleta")},text={LazyColumn(Modifier.heightIn(max=520.dp)){
        item{OutlinedTextField(name,{name=it},label={Text("Nome")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(age,{age=it.filter{ch->ch.isDigit()}},label={Text("Età")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(height,{height=it.replace(",",".").filter{ch->ch.isDigit()||ch=='.'}},label={Text("Altezza (cm)")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(weight,{weight=it.replace(",",".").filter{ch->ch.isDigit()||ch=='.'}},label={Text("Peso (kg)")},singleLine=true,modifier=Modifier.fillMaxWidth());Text("Sesso (opzionale)",fontWeight=FontWeight.Bold);Row{listOf("Non specificato","Uomo","Donna").forEach{x->FilterChip(sex==x,{sex=x},{Text(x)},Modifier.padding(end=4.dp))}};if((height.toFloatOrNull()?:0f)>0&&(weight.toFloatOrNull()?:0f)>0){val hm=(height.toFloat()/100f);Text("BMI: "+String.format(java.util.Locale.ITALY,"%.1f",weight.toFloat()/(hm*hm)),color=Accent)}; Text("Obiettivo",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp))}
        items(listOf("Forza","Ipertrofia","Calisthenics","Fitness generale")){x->Row(Modifier.fillMaxWidth().clickable{goal=x},verticalAlignment=Alignment.CenterVertically){RadioButton(goal==x,{goal=x});Text(x)}}
        item{if(goal=="Calisthenics"){Text("Skill da sviluppare (opzionali)",fontWeight=FontWeight.Bold);Text("Le skill selezionate hanno priorità nella generazione del workout.",color=Color.Gray,fontSize=11.sp);calisthenicsSkills.forEach{s->Row(Modifier.fillMaxWidth().clickable{if(s.name in prioritySkills)prioritySkills.remove(s.name)else prioritySkills.add(s.name)},verticalAlignment=Alignment.CenterVertically){Checkbox(s.name in prioritySkills,{if(it){if(s.name !in prioritySkills)prioritySkills.add(s.name)}else prioritySkills.remove(s.name)});Text(s.name)}}};Text("Livello",fontWeight=FontWeight.Bold); listOf("Principiante","Intermedio","Avanzato").forEach{x->Row(Modifier.fillMaxWidth().clickable{level=x},verticalAlignment=Alignment.CenterVertically){RadioButton(level==x,{level=x});Text(x)}}; Text("Allenamenti a settimana: "+days,fontWeight=FontWeight.Bold); Row(verticalAlignment=Alignment.CenterVertically){OutlinedButton(onClick={if(days>1)days--}){Text("−")};Spacer(Modifier.width(12.dp));Text(days.toString(),fontSize=20.sp);Spacer(Modifier.width(12.dp));Button(onClick={if(days<7)days++}){Text("+")}}}
    }},confirmButton={Button(onClick={onSave(AthleteProfile(name.trim(),goal,level,days,age.toIntOrNull()?:0,height.toFloatOrNull()?:0f,weight.toFloatOrNull()?:0f,sex,prioritySkills.toList()))}){Text("SALVA")}},dismissButton={TextButton(onClick=onDismiss){Text("ANNULLA")}})
}

@Composable
private fun ExerciseExecutionInputs(ex:Exercise, enabled:Boolean, recommendation:LoadRecommendation?=null, onComplete:(Int?,Int?,Double?,Double?,Double?)->Unit) {
    val p=prescriptionFor(ex)
    var value by remember(ex.name){mutableStateOf(p.value.toString())}
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
            val v=value.toDoubleOrNull()?:p.value
            onComplete(if(ex.measure==ExerciseMeasure.REPS)v.toInt() else null,if(ex.measure==ExerciseMeasure.SECONDS||ex.measure==ExerciseMeasure.DURATION||ex.measure==ExerciseMeasure.INTERVALS)v.toInt() else null,if(ex.measure==ExerciseMeasure.DISTANCE)v else null,load.toDoubleOrNull(),assistance.toDoubleOrNull())
        },enabled=enabled,modifier=Modifier.fillMaxWidth().padding(top=4.dp)){Text("COMPLETA SERIE")}
    }
}

@Composable
private fun CustomWorkoutDialog(profile: EquipmentProfile, athlete: AthleteProfile, sessions: List<SessionRecord>, onDismiss: () -> Unit, onSave: (SessionRecord) -> Unit, initialSkill: String = "Nessuna", initialEquipment: List<String> = emptyList(), initialGroups: List<String> = emptyList(), filtersLocked:Boolean=false, store:AppStore?=null, restored:WorkoutSessionState?=null) {
    val liveReadiness=LocalHealthConnect.current.readiness
    val readinessScore=remember{liveReadiness}
    var minutes by remember { mutableIntStateOf(45) }
    val selectedEquipment = remember { mutableStateListOf<String>().apply { addAll(if(initialEquipment.isNotEmpty()) initialEquipment else profile.equipment) } }
    val selectedGroups = remember { mutableStateListOf<String>().apply { addAll(initialGroups) } }
    var mode by remember(initialSkill,initialGroups) { mutableStateOf(if(initialSkill!="Nessuna") "Skill" else "Parti del corpo") }
    val selectedSkills = remember(initialSkill) { mutableStateListOf<String>().apply { if(initialSkill!="Nessuna") add(initialSkill) } }
    var generated by remember { mutableStateOf(restored?.let{restoreExercises(it)} ?: emptyList()) }
    var running by remember { mutableStateOf(restored!=null) }
    var sessionStartedAt by remember { mutableStateOf<Long?>(restored?.startedAtEpochMs) }
    val sessionId = remember { restored?.id ?: "custom-"+System.currentTimeMillis() }
    var paused by remember { mutableStateOf(restored?.pausedAtEpochMs!=null) }
    var elapsedSeconds by remember { mutableIntStateOf(restored?.let{((System.currentTimeMillis()-it.startedAtEpochMs)/1000L).toInt()}?:0) }
    var restRemaining by remember { mutableIntStateOf(remainingSeconds(restored?.restEndsAtEpochMs)) }
    var restEndsAt by remember { mutableStateOf<Long?>(restored?.restEndsAtEpochMs) }
    var exerciseRestSeconds by remember { mutableIntStateOf(180) }
    var restKind by remember { mutableStateOf(restored?.restKind?:"Serie") }
    var currentExercise by remember { mutableStateOf<String?>(restored?.exercises?.getOrNull(restored.currentExerciseIndex)?.exerciseName) }
    var generationMessage by remember { mutableStateOf<String?>(null) }
    val completed = remember { mutableStateMapOf<String, Int>().apply{restored?.exercises?.forEach{put(it.exerciseName,it.sets.count{s->s.completed})}} }
    val skipped = remember { mutableStateMapOf<String, Boolean>().apply{restored?.exercises?.filter{it.status==WorkoutExerciseStatus.SKIPPED}?.forEach{put(it.exerciseName,true)}} }
    val expandedCompleted = remember { mutableStateMapOf<String, Boolean>() }
    val pinnedNames = remember { mutableStateListOf<String>().apply{restored?.exercises?.filter{it.pinned}?.forEach{add(it.exerciseName)}} }
    val reps = remember { mutableStateMapOf<String, Int>() }
    val restSeconds = remember { mutableStateMapOf<String, Int>() }
    val executedSets = remember { mutableStateMapOf<String,List<WorkoutSetState>>().apply{restored?.exercises?.forEach{put(it.exerciseName,it.sets)} } }
    val exerciseRpe = remember { mutableStateMapOf<String,Int>().apply{restored?.exercises?.forEach{e->e.rpe?.let{put(e.exerciseName,it)}}} }
    var workoutRpe by remember { mutableIntStateOf(restored?.workoutRpe?:5) }
    val listState = rememberLazyListState()

    LaunchedEffect(running,paused,restEndsAt,sessionStartedAt){ while(running&&!paused){ val now=System.currentTimeMillis();restRemaining=remainingSeconds(restEndsAt,now);sessionStartedAt?.let{elapsedSeconds=((now-it)/1000L).toInt()};delay(500) } }
    LaunchedEffect(running,paused,skipped.toMap(),currentExercise,restRemaining,completed.toMap(),executedSets.toMap(),exerciseRpe.toMap(),workoutRpe) {
        if(running&&store!=null){
            val now=System.currentTimeMillis()
            val states=generated.mapIndexed{i,ex->recordedExerciseState(i,ex,executedSets[ex.name],exerciseRpe[ex.name],skipped[ex.name]==true,ex.name in pinnedNames)}
            store.saveActiveWorkout(WorkoutSessionState(sessionId,sessionStartedAt?:now,"Personalizzato",states,generated.indexOfFirst{it.name==currentExercise}.coerceAtLeast(0),elapsedSeconds*1000L,if(paused)now else null,restEndsAt,workoutRpe,runnerKind="custom",restKind=restKind))
        }
    }
    LaunchedEffect(running) { if(running){ if(currentExercise==null)currentExercise=generated.firstOrNull{(completed[it.name]?:0)<prescriptionFor(it).sets}?.name; val i=generated.indexOfFirst{it.name==currentExercise}.coerceAtLeast(0);listState.scrollToItem(i) } }
    LaunchedEffect(currentExercise) { if(running){ val i=generated.indexOfFirst{it.name==currentExercise}; if(i>=0) listState.animateScrollToItem(i) } }

    val skills = listOf("Nessuna") + calisthenicsSkills.map { it.name }
    val equipmentOptions = listOf("Corpo libero","Sbarra","Parallele","Bilanciere","Manubri","Cavi","Macchine","Elastici","Panca","Kettlebell","Leg press","Anelli","TRX","Ab Wheel","Foam Roller")
    fun prescription(ex:Exercise):Pair<Int,Int> { val p=prescriptionFor(ex); return p.value.toInt() to ex.setRestSec }
    fun build() {
        val count=when{minutes<=25->4;minutes<=40->5;minutes<=60->6;else->8}; val recovery=recoveryScores(sessions)
        val eligible=skillEligibleExercises(sessions)
        val base=exercises.filter{ex->ex.name in eligible && levelScore(ex.level)<=levelScore(athlete.level)&&equipmentCompatible(ex,EquipmentProfile("Custom","",selectedEquipment.toList()))&&(mode!="Parti del corpo"||selectedGroups.isEmpty()||selectedGroups.any{ex.group.contains(it,true)})}
        val ranked=base.sortedWith(compareBy<Exercise>{ex->val rec=recovery.filterKeys{ex.group.contains(it,true)}.values.minOrNull()?:100;if(rec<40)100 else if(rec<60)50 else 0}.thenBy{if(selectedSkills.any{s->s in it.skills})0 else 1}.thenBy{if(it.role=="Forza")0 else 1})
        generated=if(mode=="Skill"){
            val skillWork=ranked.filter{ex->selectedSkills.any{it in ex.skills}}
            (skillWork+ranked.filter{it !in skillWork && (it.role=="Forza"||it.role=="Prerequisito"||it.role=="Accessorio")}).distinct().take(count)
        } else ranked.take(count)
        generated=applyCoaching(sequenceWorkout(generated),coachingContext(sessions,readinessScore))
        completed.clear();expandedCompleted.clear();reps.clear();restSeconds.clear();exerciseRpe.clear()
        generated.forEach{ex->val p=prescription(ex);reps[ex.name]=p.first;restSeconds[ex.name]=p.second}
        exerciseRestSeconds=generated.maxOfOrNull{it.exerciseRestSec}?:180;running=false;paused=false;elapsedSeconds=0;restRemaining=0
        currentExercise=generated.firstOrNull()?.name
        generationMessage=if(generated.isEmpty())"Nessun esercizio compatibile: prova ad ampliare attrezzatura, muscoli o livello/skill." else "Workout pronto: "+generated.size+" esercizi."
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
                                OutlinedButton(onClick={if(paused){paused=false;if(restRemaining>0)restEndsAt=System.currentTimeMillis()+restRemaining*1000L}else{paused=true;restRemaining=remainingSeconds(restEndsAt);restEndsAt=null}}){Text(if(paused)"RIPRENDI" else "PAUSA")}
                                if(restRemaining>0) TextButton(onClick={restRemaining=0;restEndsAt=null}){Text("SALTA RECUPERO")}
                            }
                        }
                    }
                }
                LazyColumn(state=listState,modifier=Modifier.weight(1f)){
                    if(!running && generated.isEmpty()){
                        if(filtersLocked){
                            item{
                                Text("Workout basato sui filtri",fontWeight=FontWeight.Bold)
                                Text((initialGroups+initialEquipment+selectedSkills).joinToString(" • "),color=Accent)
                                Text("Durata workout",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp))
                                Row{listOf(20,30,45,60,75).forEach{m->FilterChip(minutes==m,{minutes=m},{Text("$m")},Modifier.padding(end=4.dp))}}
                                Button(onClick={build()},modifier=Modifier.fillMaxWidth().padding(vertical=10.dp)){Text("GENERA WORKOUT")}
                                generationMessage?.let{Text(it,color=MaterialTheme.colorScheme.error,fontWeight=FontWeight.Bold)}
                            }
                        } else {
                            item{Text("1. Attrezzatura disponibile",fontWeight=FontWeight.Bold);Text("Profilo attivo: "+profile.name+" • modifica valida solo per questo workout",color=Color.Gray,fontSize=12.sp)}
                            items(equipmentOptions){e->Row(Modifier.fillMaxWidth().clickable{if(e in selectedEquipment)selectedEquipment.remove(e)else selectedEquipment.add(e)},verticalAlignment=Alignment.CenterVertically){Checkbox(e in selectedEquipment,{if(it){if(e !in selectedEquipment)selectedEquipment.add(e)}else selectedEquipment.remove(e)});Text(e)}}
                            item{
                                val available=exercises.count{ex->equipmentCompatible(ex,EquipmentProfile("Custom","",selectedEquipment.toList()))}
                                Text("$available esercizi compatibili con l'attrezzatura",color=Accent,fontSize=12.sp)
                                Text("2. Durata",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp))
                                Row{listOf(20,30,45,60,75).forEach{m->FilterChip(minutes==m,{minutes=m},{Text("$m")},Modifier.padding(end=4.dp))}}
                                Text("3. Cosa vuoi allenare?",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp))
                                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){FilterChip(mode=="Parti del corpo",{mode="Parti del corpo";selectedSkills.clear()},{Text("Parti del corpo")});FilterChip(mode=="Skill",{mode="Skill";selectedGroups.clear()},{Text("Skill calisthenics")})}
                            }
                            if(mode=="Parti del corpo") items(recoveryGroups){g->Row(Modifier.fillMaxWidth().clickable{if(g in selectedGroups)selectedGroups.remove(g)else selectedGroups.add(g)},verticalAlignment=Alignment.CenterVertically){Checkbox(g in selectedGroups,{if(it){if(g !in selectedGroups)selectedGroups.add(g)}else selectedGroups.remove(g)});Text(g)}}
                            if(mode=="Skill") item{skills.filter{it!="Nessuna"}.forEach{s->val linked=exercises.filter{s in it.skills};val muscles=linked.flatMap{ex->recoveryGroups.filter{g->ex.group.contains(g,true)}}.distinct().joinToString(" • ");Row(Modifier.fillMaxWidth().clickable{if(s in selectedSkills)selectedSkills.remove(s)else selectedSkills.add(s)},verticalAlignment=Alignment.CenterVertically){Checkbox(s in selectedSkills,{checked->if(checked){if(s !in selectedSkills)selectedSkills.add(s)}else selectedSkills.remove(s)});Column{Text(s);if(s in selectedSkills)Text("Coinvolge: $muscles",color=Color.Gray,fontSize=11.sp)}}}}
                            item{Button(onClick={build()},enabled=selectedEquipment.isNotEmpty()&&(mode!="Skill"||selectedSkills.isNotEmpty()),modifier=Modifier.fillMaxWidth().padding(vertical=10.dp)){Text("GENERA WORKOUT")};generationMessage?.let{Text(it,color=MaterialTheme.colorScheme.error,fontWeight=FontWeight.Bold)}}
                        }
                    }
                    if(!running && generated.isNotEmpty()){
                        item{
                            Text("WORKOUT GENERATO",fontWeight=FontWeight.Bold,color=Accent)
                            Text("Durata prevista: $minutes min")
                            Text("Attrezzatura: "+selectedEquipment.joinToString(", "),fontSize=12.sp,color=Color.Gray)
                            Text(if(mode=="Skill")"Skill: "+selectedSkills.joinToString(", ") else "Gruppi: "+selectedGroups.joinToString(", "),fontSize=12.sp,color=Color.Gray)
                            Text("ESERCIZI",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=10.dp))
                        }
                    }
                    if(!running && generated.isNotEmpty())item{
                        CoachSummary(coachingContext(sessions,readinessScore))
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                            Text("Blocca gli esercizi che vuoi mantenere",fontSize=12.sp,color=Color.Gray,modifier=Modifier.weight(1f))
                            TextButton(onClick={
                                val eligible=skillEligibleExercises(sessions)
                                val pool=exercises.filter{ex->ex.name in eligible&&levelScore(ex.level)<=levelScore(athlete.level)&&equipmentCompatible(ex,EquipmentProfile("Custom","",selectedEquipment.toList()))&&(mode!="Parti del corpo"||selectedGroups.isEmpty()||selectedGroups.any{ex.group.contains(it,true)})&&(mode!="Skill"||selectedSkills.any{it in ex.skills})}
                                generated=regenerateCoachedWorkout(generated,pool,pinnedNames.toSet(),coachingContext(sessions,readinessScore))
                                reps.clear();restSeconds.clear();generated.forEach{ex->reps[ex.name]=prescriptionFor(ex).value.toInt();restSeconds[ex.name]=ex.setRestSec}
                                generationMessage="Rigenerati solo gli esercizi non bloccati."
                            }){Text("RIGENERA NON BLOCCATI")}
                        }
                    }
                    items(generated){ex->
                        val n=completed[ex.name]?:0;val targetSets=prescriptionFor(ex).sets;val done=n>=targetSets;val isSkipped=skipped[ex.name]==true;val expanded=!done||(expandedCompleted[ex.name]?:false)
                        Card(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{if(running){if(done)expandedCompleted[ex.name]=!(expandedCompleted[ex.name]?:false) else {skipped[ex.name]=false;currentExercise=ex.name}}}){
                            Column(Modifier.padding(10.dp)){
                                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text((if(done)"✓ " else if(running&&currentExercise==ex.name)"▶ " else "")+ex.name,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));if(!running)TextButton(onClick={if(ex.name in pinnedNames)pinnedNames.remove(ex.name)else pinnedNames.add(ex.name)}){Text(if(ex.name in pinnedNames)"🔒 BLOCCATO" else "🔓 BLOCCA")}}
                                if(done) Text("COMPLETATO • "+targetSets+"/"+targetSets+" serie",color=Accent,fontSize=12.sp)
                                if(running&&n>0){Text("RPE: "+(exerciseRpe[ex.name]?.toString()?:"da indicare"),fontSize=12.sp);Slider(value=(exerciseRpe[ex.name]?:5).toFloat(),onValueChange={exerciseRpe[ex.name]=it.toInt()},valueRange=1f..10f,steps=8)}
                                if(expanded){
                                    val p=prescriptionFor(ex)
                                    Text(ex.group+" • "+ex.equipment+" • "+ex.role,color=Color.Gray,fontSize=12.sp)
                                    Text(p.sets.toString()+" serie × "+(reps[ex.name]?:p.value.toInt())+" "+p.unit+" • "+ex.setRestSec+"s serie / "+ex.exerciseRestSec+"s esercizio",color=Accent,fontSize=12.sp)
                                    if(running && !done && !isSkipped){
                                        ExerciseExecutionInputs(ex,restRemaining==0&&!paused,loadRecommendationForExercise(ex,sessions,minOf(exerciseRecovery(ex,recoveryScores(sessions)),readinessScore?:100))){ar,asec,dist,load,assist->val p=prescriptionFor(ex);val nNow=completed[ex.name]?:0;val old=executedSets[ex.name]?:plannedExerciseState(0,ex).sets;if(nNow<old.size){val xs=old.toMutableList();xs[nNow]=xs[nNow].copy(actualReps=ar,actualSeconds=asec,actualDistanceMeters=dist,actualLoadKg=load,actualAssistanceKg=assist,completed=true);executedSets[ex.name]=xs;completed[ex.name]=nNow+1;if(nNow+1>=p.sets){restKind="Esercizio";restRemaining=ex.exerciseRestSec}else{restKind="Serie";restRemaining=ex.intervalRestSec?:restSeconds[ex.name]?:ex.setRestSec};restEndsAt=System.currentTimeMillis()+restRemaining*1000L}}

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
                    if(!running){sessionStartedAt=System.currentTimeMillis();running=true;paused=false;currentExercise=generated.firstOrNull{(completed[it.name]?:0)<prescriptionFor(it).sets}?.name}
                    else {val total=generated.sumOf{prescriptionFor(it).sets};onSave(SessionRecord(System.currentTimeMillis(),"Personalizzato"+if(selectedSkills.isNotEmpty())" • "+selectedSkills.joinToString(", ") else "",completed.values.sum(),total,generated.map{it.name},exerciseRpe.toMap(),workoutRpe,completed.toMap(),elapsedSeconds,generated.mapIndexed{i,ex->recordedExerciseState(i,ex,executedSets[ex.name],exerciseRpe[ex.name],skipped[ex.name]==true,ex.name in pinnedNames)},skipped.filterValues{it}.keys.toList()));store?.clearActiveWorkout();onDismiss()}
                }){Text(if(running)"TERMINA E SALVA" else "AVVIA WORKOUT")}
            }
        },
        dismissButton={
            if(!running && generated.isNotEmpty()) TextButton(onClick={resetPreview()}){Text("MODIFICA PARAMETRI")}
            else TextButton(onClick=onDismiss){Text("ANNULLA")}
        }
    )
}

@Composable
private fun WorkoutDialog(profile: EquipmentProfile, athlete: AthleteProfile, sessions: List<SessionRecord>, store:AppStore, restored:WorkoutSessionState?=null, onDismiss: () -> Unit, onSave: (SessionRecord) -> Unit) {
    val liveReadiness=LocalHealthConnect.current.readiness
    val readinessScore=remember{liveReadiness}
    val compatible = remember(profile, athlete, sessions, restored) { restored?.let{restoreExercises(it)}?.takeIf{it.isNotEmpty()} ?: generateWorkout(profile, athlete, sessions,readinessScore) }
    val completedSets = remember { mutableStateMapOf<String, Int>().apply { restored?.exercises?.forEach { e -> put(e.exerciseName,e.sets.count{it.completed}) } } }
    val expandedCompleted = remember { mutableStateMapOf<String, Boolean>() }
    val skipped = remember { mutableStateMapOf<String, Boolean>().apply{restored?.exercises?.filter{it.status==WorkoutExerciseStatus.SKIPPED}?.forEach{put(it.exerciseName,true)}} }
    val totalSets = compatible.sumOf{prescriptionFor(it).sets}
    val doneSets = completedSets.values.sum()
    var started by remember { mutableStateOf(restored != null) }
    var sessionStartedAt by remember { mutableStateOf(restored?.startedAtEpochMs) }
    val sessionId = remember { restored?.id ?: "auto-"+System.currentTimeMillis() }
    var paused by remember { mutableStateOf(restored?.pausedAtEpochMs != null) }
    var elapsedSeconds by remember { mutableIntStateOf(restored?.let{((System.currentTimeMillis()-it.startedAtEpochMs)/1000).toInt().coerceAtLeast(0)}?:0) }
    var restRemaining by remember { mutableIntStateOf(remainingSeconds(restored?.restEndsAtEpochMs)) }
    var restEndsAt by remember { mutableStateOf(restored?.restEndsAtEpochMs) }
    var restKind by remember { mutableStateOf(restored?.restKind?:"Serie") }
    var currentExercise by remember { mutableStateOf<String?>(restored?.exercises?.getOrNull(restored.currentExerciseIndex)?.exerciseName) }
    val executedSets = remember { mutableStateMapOf<String,List<WorkoutSetState>>().apply{restored?.exercises?.forEach{put(it.exerciseName,it.sets)} } }
    val exerciseRpe = remember { mutableStateMapOf<String,Int>().apply { restored?.exercises?.forEach{e->e.rpe?.let{put(e.exerciseName,it)}} } }
    var workoutRpe by remember { mutableIntStateOf(restored?.workoutRpe?:5) }
    val listState = rememberLazyListState()

    LaunchedEffect(started,paused,restEndsAt,sessionStartedAt){while(started&&!paused){val now=System.currentTimeMillis();restRemaining=remainingSeconds(restEndsAt,now);sessionStartedAt?.let{elapsedSeconds=((now-it)/1000L).toInt()};delay(500)}}
    LaunchedEffect(started, paused, skipped.toMap(),exerciseRpe.toMap(),currentExercise, restRemaining, doneSets, executedSets.toMap(), workoutRpe) {
        if(started) {
            val now=System.currentTimeMillis()
            val states=compatible.mapIndexed { index, ex ->
                recordedExerciseState(index,ex,executedSets[ex.name],exerciseRpe[ex.name],skipped[ex.name]==true,false)
            }
            store.saveActiveWorkout(WorkoutSessionState(sessionId,sessionStartedAt?:now,profile.name,states,compatible.indexOfFirst{it.name==currentExercise}.coerceAtLeast(0),elapsedSeconds*1000L,if(paused)now else null,restEndsAt,workoutRpe,runnerKind="automatic",restKind=restKind))
        }
    }
    LaunchedEffect(started){if(started){if(currentExercise==null)currentExercise=compatible.firstOrNull{(completedSets[it.name]?:0)<prescriptionFor(it).sets}?.name;val i=compatible.indexOfFirst{it.name==currentExercise}.coerceAtLeast(0);listState.scrollToItem(i)}}
    LaunchedEffect(currentExercise){if(started){val i=compatible.indexOfFirst{it.name==currentExercise};if(i>=0)listState.animateScrollToItem(i)}}

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
                                OutlinedButton(onClick={if(paused){paused=false;if(restRemaining>0)restEndsAt=System.currentTimeMillis()+restRemaining*1000L}else{paused=true;restRemaining=remainingSeconds(restEndsAt);restEndsAt=null}}){Text(if(paused)"RIPRENDI" else "PAUSA")}
                                if(restRemaining>0) TextButton(onClick={restRemaining=0;restEndsAt=null}){Text("SALTA RECUPERO")}
                            }
                        }
                    }
                }
                LazyColumn(state=listState,modifier=Modifier.weight(1f)){
                    if(!started)item{
                        CoachSummary(coachingContext(sessions,readinessScore))
                        Text("Generato per "+athlete.goal+" • "+athlete.level+" • "+profile.name,color=Color.Gray)
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
                                        ExerciseExecutionInputs(ex,!paused&&restRemaining==0,loadRecommendationForExercise(ex,sessions,minOf(exerciseRecovery(ex,recoveryScores(sessions)),readinessScore?:100))){ar,asec,dist,load,assist->
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
                                                    restKind="Serie";restRemaining=ex.intervalRestSec?:ex.setRestSec;currentExercise=ex.name
                                                }
                                                restEndsAt=System.currentTimeMillis()+restRemaining*1000L
                                            }
                                        }
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
            else{val structured=compatible.mapIndexed { index,ex->recordedExerciseState(index,ex,executedSets[ex.name],exerciseRpe[ex.name],skipped[ex.name]==true,false) };onSave(SessionRecord(System.currentTimeMillis(),profile.name,doneSets,totalSets,compatible.map{it.name},exerciseRpe.toMap(),workoutRpe,compatible.associate{it.name to (completedSets[it.name]?:0)},elapsedSeconds,structured,structured.filter{it.status==WorkoutExerciseStatus.SKIPPED}.map{it.exerciseName}));store.clearActiveWorkout();onDismiss()}
        }){Text(if(!started)"AVVIA WORKOUT" else if(totalSets>0&&doneSets==totalSets)"TERMINA E SALVA" else "TERMINA E SALVA")} },
        dismissButton = { TextButton(onClick=onDismiss){Text("ANNULLA")} }
    )
}

@Composable
private fun ExerciseDetailDialog(ex: Exercise, sessions: List<SessionRecord>, onDismiss: () -> Unit) {
    val stats = remember(ex.name,sessions) { ExerciseStatistics.fromHistory(ex.name,sessions) }
    val instructions = when (ex.name) {
        "Corsa intervallata 60/60" -> "Alterna 60 secondi di corsa sostenuta e 60 secondi di recupero. Ripeti per il numero di intervalli previsto, mantenendo un ritmo controllabile e costante."
        "Trazioni prone" -> "Parti in sospensione completa, mantieni il tronco stabile e porta il petto verso la sbarra senza slancio. Torna in basso in controllo."
        "Lat machine" -> "Mantieni il torace alto e tira la barra verso la parte alta del petto. Evita di trasformare il movimento in uno slancio del busto."
        "Panca piana" -> "Mantieni scapole addotte e piedi stabili. Scendi in controllo verso il petto e spingi mantenendo polsi e gomiti stabili."
        "Push-up" -> "Mantieni testa, tronco e bacino allineati. Scendi in controllo e spingi senza perdere la tensione del core."
        "Dip alle parallele" -> "Mantieni le spalle stabili, scendi in controllo e risali senza rimbalzo. Interrompi se avverti dolore alla spalla."
        "Squat" -> "Mantieni il piede stabile e il tronco compatto. Scendi in controllo e risali coordinando ginocchia e anche."
        "Bulgarian split squat" -> "Mantieni il bacino stabile, scendi verticalmente e spingi soprattutto con la gamba anteriore."
        "Pike push-up" -> "Mantieni il bacino alto e porta la testa tra le mani in controllo. Spingi mantenendo le spalle attive."
        "Hollow body hold" -> "Mantieni la zona lombare aderente al suolo e il bacino in retroversione. Riduci la leva se perdi la posizione."
        else -> when {
            ex.role=="Progressione" -> "Progressione tecnica per "+ex.skills.joinToString().ifBlank{"la skill selezionata"}+". Mantieni una variante che consenta controllo completo e interrompi la serie prima di perdere la posizione."
            ex.role=="Forza" -> "Usa un carico che consenta tutte le ripetizioni previste con tecnica stabile. Mantieni discesa controllata, assetto solido e evita ripetizioni forzate."
            ex.role=="Prerequisito" -> "Concentrati sulla qualità del movimento e sul controllo. Questo esercizio costruisce i requisiti necessari per "+ex.skills.joinToString().ifBlank{"le progressioni successive"}+"."
            else -> "Esegui con escursione controllata e assetto stabile. Regola carico o variante per completare le ripetizioni previste lasciando margine tecnico."
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ex.name) },
        text = {
            Column {
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
                if (ex.skills.isNotEmpty()) {
                    Text("Collegamenti: " + ex.skills.joinToString(" • "), color = Accent, modifier = Modifier.padding(top = 6.dp))
                } else {
                    Text("Esercizio base/accessorio senza skill obbligatoria", color = Color.Gray, modifier = Modifier.padding(top = 6.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CHIUDI") } }
    )
}

@Composable
private fun EquipmentProfileDialog(current: EquipmentProfile? = null, onDismiss: () -> Unit, onCreate: (EquipmentProfile) -> Unit) {
    var name by remember(current) { mutableStateOf(current?.name ?: "") }
    val options = listOf("Corpo libero", "Sbarra", "Parallele", "Bilanciere", "Manubri", "Cavi", "Macchine", "Elastici", "Panca", "Kettlebell", "Leg press", "Anelli", "TRX", "Ab Wheel", "Foam Roller")
    val selected = remember(current) { mutableStateListOf<String>().apply { addAll(current?.equipment ?: emptyList()) } }

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
                            if (item in selected) selected.remove(item) else selected.add(item)
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item in selected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (item !in selected) selected.add(item)
                                } else selected.remove(item)
                            }
                        )
                        Text(item)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(EquipmentProfile(name.trim(), "📍", selected.toList())) },
                enabled = name.isNotBlank() && selected.isNotEmpty()
            ) { Text(if(current==null)"CREA" else "SALVA") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ANNULLA") } }
    )
}


