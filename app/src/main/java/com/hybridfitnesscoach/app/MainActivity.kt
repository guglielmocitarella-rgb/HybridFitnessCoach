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
data class SessionRecord(val timestamp: Long, val profileName: String, val completedSets: Int, val totalSets: Int, val exerciseNames: List<String> = emptyList(), val exerciseRpe: Map<String,Int> = emptyMap(), val workoutRpe:Int=0, val completedByExercise:Map<String,Int> = emptyMap(), val durationSec:Int=0, val structuredExercises:List<WorkoutExerciseState> = emptyList(), val skippedExercises:List<String> = emptyList(), val modifications:List<String> = emptyList())
data class CalisthenicsSkill(val name:String, val category:String, val level:String)

private class AppStore(context: Context) {
    private val prefs = context.getSharedPreferences("hybrid_fitness_store", Context.MODE_PRIVATE)
    fun loadAthlete() = AthleteProfile(prefs.getString("athlete_name","") ?: "", prefs.getString("athlete_goal","Forza + skill") ?: "Forza + skill", prefs.getString("athlete_level","Intermedio") ?: "Intermedio", prefs.getInt("athlete_days",3),prefs.getInt("athlete_age",0),prefs.getFloat("athlete_height",0f),prefs.getFloat("athlete_weight",0f),prefs.getString("athlete_sex","Non specificato")?:"Non specificato",(prefs.getString("athlete_priority_skills","")?:"").split("|").filter{it.isNotBlank()})
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
    fun loadSessions():List<SessionRecord> = try { val a=JSONArray(prefs.getString("sessions","[]") ?: "[]"); (0 until a.length()).map { i -> val o=a.getJSONObject(i); val sx=o.optJSONArray("structuredExercises")?:JSONArray(); val sk=o.optJSONArray("skippedExercises")?:JSONArray(); val mo=o.optJSONArray("modifications")?:JSONArray(); SessionRecord(o.getLong("timestamp"),o.getString("profile"),o.getInt("done"),o.getInt("total"), if(o.has("exercises")) { val e=o.getJSONArray("exercises"); (0 until e.length()).map { e.getString(it) } } else emptyList(), if(o.has("exerciseRpe")){val r=o.getJSONObject("exerciseRpe");r.keys().asSequence().associateWith{r.getInt(it)}}else emptyMap(),o.optInt("workoutRpe",0),if(o.has("completedByExercise")){val r=o.getJSONObject("completedByExercise");r.keys().asSequence().associateWith{r.getInt(it)}}else emptyMap(),o.optInt("durationSec",0),(0 until sx.length()).map{workoutExerciseFromJson(sx.getJSONObject(it))},(0 until sk.length()).map{sk.getString(it)},(0 until mo.length()).map{mo.getString(it)}) }.sortedByDescending{it.timestamp} } catch(_:Exception){ emptyList() }
    fun saveSessions(all:List<SessionRecord>){ val a=JSONArray(); all.take(50).forEach{x-> val e=JSONArray(); x.exerciseNames.forEach{e.put(it)};val sx=JSONArray();x.structuredExercises.forEach{sx.put(it.toJson())};val sk=JSONArray();x.skippedExercises.forEach{sk.put(it)};val mo=JSONArray();x.modifications.forEach{mo.put(it)}; a.put(JSONObject().put("timestamp",x.timestamp).put("profile",x.profileName).put("done",x.completedSets).put("total",x.totalSets).put("exercises",e).put("exerciseRpe",JSONObject(x.exerciseRpe.mapValues{it.value})).put("workoutRpe",x.workoutRpe).put("completedByExercise",JSONObject(x.completedByExercise.mapValues{it.value})).put("durationSec",x.durationSec).put("structuredExercises",sx).put("skippedExercises",sk).put("modifications",mo))}; prefs.edit().putString("sessions",a.toString()).apply() }
    fun addSession(s:SessionRecord){ saveSessions(listOf(s)+loadSessions()) }
    fun deleteSession(timestamp:Long){ saveSessions(loadSessions().filterNot{it.timestamp==timestamp}) }
}

internal val profiles = listOf(
    EquipmentProfile("Palestra", "🏋️", listOf("Bilanciere", "Manubri", "Cavi", "Macchine", "Sbarra", "Parallele", "Panca", "Kettlebell", "Leg press")),
    EquipmentProfile("Casa", "🏠", listOf("Corpo libero", "Sbarra", "Elastici")),
    EquipmentProfile("Parco", "🌳", listOf("Sbarra", "Parallele", "Corpo libero"))
)

private val calisthenicsSkills = listOf(
 CalisthenicsSkill("Trazioni","Pull","Principiante"),CalisthenicsSkill("Dip","Push","Principiante"),CalisthenicsSkill("L-sit","Core","Intermedio"),CalisthenicsSkill("Pistol squat","Gambe","Intermedio"),CalisthenicsSkill("Handstand","Verticale","Intermedio"),CalisthenicsSkill("Muscle-up","Pull/Push","Avanzato"),CalisthenicsSkill("Front lever","Pull/Core","Avanzato"),CalisthenicsSkill("Back lever","Pull/Core","Avanzato"),CalisthenicsSkill("Planche","Push/Core","Avanzato"),CalisthenicsSkill("Handstand push-up","Verticale/Push","Avanzato"),CalisthenicsSkill("Human flag","Pull/Push/Core","Avanzato"),CalisthenicsSkill("Dragon flag","Core","Avanzato"),CalisthenicsSkill("V-sit","Core","Avanzato"),CalisthenicsSkill("One arm pull-up","Pull","Avanzato"),CalisthenicsSkill("One arm push-up","Push","Avanzato"),CalisthenicsSkill("Shrimp squat","Gambe","Intermedio")
)
internal val exercises = listOf(
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
    Exercise("Ab wheel rollout", "Core", "Corpo libero", "Intermedio", listOf("Front lever"), "Accessorio"),
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
    Exercise("Corsa continua", "Cardio", "Corpo libero", "Principiante", role="Base", category=ExerciseCategory.CARDIO, measure=ExerciseMeasure.DURATION, resistanceMode=ResistanceMode.BODYWEIGHT, targetSets=1, targetValue=600.0),
    Exercise("Corsa distanza", "Cardio", "Corpo libero", "Principiante", role="Base", category=ExerciseCategory.CARDIO, measure=ExerciseMeasure.DISTANCE, resistanceMode=ResistanceMode.BODYWEIGHT, targetSets=1, targetValue=1000.0),
    Exercise("Intervalli 60/60", "Cardio", "Corpo libero", "Intermedio", role="Base", category=ExerciseCategory.CARDIO, measure=ExerciseMeasure.INTERVALS, resistanceMode=ResistanceMode.BODYWEIGHT, targetSets=5, targetValue=60.0, intervalRestSec=60)

)

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
private fun plannedExerciseState(index:Int,ex:Exercise):WorkoutExerciseState {
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
internal fun remainingSeconds(endEpochMs:Long?, now:Long=System.currentTimeMillis()):Int =
    if(endEpochMs==null) 0 else kotlin.math.ceil((endEpochMs-now).coerceAtLeast(0L)/1000.0).toInt()

internal fun equipmentCompatible(ex: Exercise, profile: EquipmentProfile): Boolean =
    profile.equipment.any { eq ->
        ex.equipment.contains(eq, ignoreCase = true) ||
        (eq == "Corpo libero" && ex.equipment.contains("corpo libero", ignoreCase = true)) ||
        (eq == "Macchine" && ex.equipment.contains("Macchina", ignoreCase = true))
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
                val actualSets=if(session.completedByExercise.isNotEmpty()) (session.completedByExercise[ex.name]?:0).toDouble() else setsPerExercise
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

internal fun generateWorkout(profile: EquipmentProfile, athlete: AthleteProfile, sessions: List<SessionRecord>): List<Exercise> {
    val maxLevel = levelScore(athlete.level)
    val available = exercises.filter { equipmentCompatible(it, profile) && levelScore(it.level) <= maxLevel }
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
    preferredRoles.forEach { role -> freshnessSorted.filter { it.role == role }.forEach { if (it !in result && result.size < 6) result.add(it) } }
    freshnessSorted.forEach { if (it !in result && result.size < 6) result.add(it) }
    return result
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HybridFitnessApp() }
    }
}

enum class Tab { HOME, LIBRARY, EQUIPMENT, PROGRESS }

@Composable
fun HybridFitnessApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { AppStore(context) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    val customProfiles = remember { mutableStateListOf<EquipmentProfile>().apply { addAll(store.loadCustomProfiles()) } }
    var activeProfile by remember { mutableStateOf(customProfiles.firstOrNull { it.name == store.loadActiveProfileName() } ?: customProfiles.firstOrNull() ?: EquipmentProfile("Nessun profilo","📍",emptyList())) }
    var athlete by remember { mutableStateOf(store.loadAthlete()) }
    var sessions by remember { mutableStateOf(store.loadSessions()) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Accent, background = Bg, surface = Color.White)) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    listOf(
                        Triple(Tab.HOME, Icons.Default.Home, "Oggi"),
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
                    Tab.HOME -> Home(activeProfile, athlete, sessions, store, { tab = Tab.EQUIPMENT }) { record -> store.addSession(record); sessions = store.loadSessions() }
                    Tab.LIBRARY -> Library(activeProfile, athlete, sessions) { record -> store.addSession(record); sessions = store.loadSessions() }
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

@Composable private fun Header(title: String, subtitle: String? = null) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        subtitle?.let { Text(it, color = Color.Gray, modifier = Modifier.padding(top = 4.dp)) }
    }
}

@Composable
private fun Home(profile: EquipmentProfile, athlete: AthleteProfile, sessions: List<SessionRecord>, store: AppStore, changeProfile: () -> Unit, onSessionSaved: (SessionRecord) -> Unit) {
    val restoredWorkout = remember { store.loadActiveWorkout() }
    var workoutOpen by remember { mutableStateOf(restoredWorkout != null) }
    var customWorkoutOpen by remember { mutableStateOf(false) }
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
                    Text("Upper body ibrido", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    Text("55 min  •  6 esercizi  •  Forza + skill", color = Color.Gray)
                    Button(onClick = { workoutOpen = true }, modifier = Modifier.fillMaxWidth().padding(top = 18.dp), shape = RoundedCornerShape(14.dp)) { Text("INIZIA ALLENAMENTO") }
                    OutlinedButton(onClick = { customWorkoutOpen = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(14.dp)) { Text("CREA WORKOUT PERSONALIZZATO") }
                }
            }
        }
        item { SectionTitle("Recupero") }
        item { RecoveryChart(sessions) }
    }
    if (workoutOpen && restoredWorkout?.runnerKind!="custom") { WorkoutDialog(profile = profile, athlete = athlete, sessions = sessions, store = store, restored = restoredWorkout, onDismiss = { workoutOpen = false }, onSave = onSessionSaved) }
    if (customWorkoutOpen || (workoutOpen && restoredWorkout?.runnerKind=="custom")) { CustomWorkoutDialog(profile, athlete, sessions, { customWorkoutOpen=false;workoutOpen=false }, onSessionSaved, store=store, restored=restoredWorkout?.takeIf{it.runnerKind=="custom"}) }
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
private fun Library(profile: EquipmentProfile, athlete: AthleteProfile, sessions: List<SessionRecord>, onSessionSaved: (SessionRecord) -> Unit) {
    var selectedSkill by remember { mutableStateOf<String?>(null) }
    var skillWorkout by remember { mutableStateOf<String?>(null) }
    var filteredWorkout by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Exercise?>(null) }
    val equipmentFilters = remember { mutableStateListOf<String>() }
    val groupFilters = remember { mutableStateListOf<String>() }
    val skillFilters = remember { mutableStateListOf<String>() }
    val typeFilters = remember { mutableStateListOf<String>() }
    val measureFilters = remember { mutableStateListOf<ExerciseMeasure>() }
    var filterDialog by remember { mutableStateOf<String?>(null) }
    val equipmentOptions = listOf("Corpo libero","Sbarra","Parallele","Anelli","TRX","Bilanciere","Manubri","Panca","Cavi","Macchine","Elastici","Kettlebell","Leg press")
    val matchingSkills = calisthenicsSkills.filter { s -> query.isNotBlank() && s.name.contains(query.trim(),true) }
    val shown = exercises.filter { ex ->
        (query.isBlank() || query.trim().split(Regex("\\s+")).all { token -> ex.name.contains(token,true) || ex.group.contains(token,true) || ex.equipment.contains(token,true) || ex.skills.any{it.contains(token,true)} }) &&
        (equipmentFilters.isEmpty() || equipmentFilters.any { eq -> ex.equipment.contains(eq,true) || (eq=="Macchine" && ex.equipment.contains("Macchina",true)) }) &&
        (groupFilters.isEmpty() || groupFilters.any { ex.group.contains(it,true) }) &&
        (skillFilters.isEmpty() || skillFilters.any { it in ex.skills }) &&
        (typeFilters.isEmpty() || typeFilters.any { t -> ex.measure.name.equals(t,true) || ex.category.name.equals(t,true) }) &&
        (measureFilters.isEmpty() || ex.measure in measureFilters)
    }
    Column {
        Header("Libreria esercizi", exercises.size.toString()+" esercizi • "+calisthenicsSkills.size+" skill calisthenics")
        OutlinedTextField(query,{query=it},leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Ricerca istantanea: esercizio, muscolo, attrezzo, skill")},modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth(),shape=RoundedCornerShape(16.dp))
        Row(Modifier.padding(horizontal=20.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)){
            FilterChip(selected=equipmentFilters.isNotEmpty(),onClick={filterDialog="equipment"},label={Text("Attrezzi"+if(equipmentFilters.isEmpty())"" else " ("+equipmentFilters.size+")")})
            FilterChip(selected=groupFilters.isNotEmpty(),onClick={filterDialog="groups"},label={Text("Muscoli"+if(groupFilters.isEmpty())"" else " ("+groupFilters.size+")")})
            FilterChip(selected=skillFilters.isNotEmpty(),onClick={filterDialog="skills"},label={Text("Skill"+if(skillFilters.isEmpty())"" else " ("+skillFilters.size+")")})
            FilterChip(selected=typeFilters.isNotEmpty(),onClick={filterDialog="types"},label={Text("Tipo"+if(typeFilters.isEmpty())"" else " ("+typeFilters.size+")")})
            FilterChip(selected=measureFilters.isNotEmpty(),onClick={filterDialog="measure"},label={Text("Tipo"+if(measureFilters.isEmpty())"" else " ("+measureFilters.size+")")})
        }
        if(equipmentFilters.isNotEmpty()||groupFilters.isNotEmpty()||skillFilters.isNotEmpty()||typeFilters.isNotEmpty()||measureFilters.isNotEmpty()) {
 Row(Modifier.padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically){Text(shown.size.toString()+" risultati",color=Color.Gray,modifier=Modifier.weight(1f));TextButton(onClick={equipmentFilters.clear();groupFilters.clear();skillFilters.clear();typeFilters.clear();measureFilters.clear()}){Text("AZZERA")}}
 Button(onClick={filteredWorkout=true},enabled=shown.isNotEmpty(),modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth()){Text("GENERA WORKOUT DAI FILTRI")}
}
        LazyColumn(contentPadding=PaddingValues(horizontal=20.dp,vertical=6.dp)){
            items(matchingSkills){s->Card(Modifier.fillMaxWidth().padding(bottom=10.dp).clickable{selectedSkill=s.name}){Column(Modifier.padding(16.dp)){Text(s.name,fontSize=18.sp,fontWeight=FontWeight.Bold);Text("Skill calisthenics • "+s.category+" • "+s.level,color=Accent)}}}
            items(shown){ex->Card(Modifier.fillMaxWidth().padding(bottom=10.dp).clickable{selected=ex}){Column(Modifier.padding(16.dp)){Text(ex.name,fontSize=18.sp,fontWeight=FontWeight.Bold);Text(ex.group+" • "+ex.equipment+" • "+ex.level,color=Color.Gray);if(ex.skills.isNotEmpty())Text(ex.role+" → "+ex.skills.joinToString(" • "),color=Accent,fontSize=12.sp,modifier=Modifier.padding(top=6.dp))}}}
            if(shown.isEmpty()) item{Text("Nessun esercizio corrisponde ai filtri selezionati.",color=Color.Gray,modifier=Modifier.padding(12.dp))}
        }
    }
    if(filterDialog=="measure") {
        AlertDialog(onDismissRequest={filterDialog=null},title={Text("Filtra per tipo esercizio")},text={Column{ExerciseMeasure.entries.forEach{m->Row(Modifier.fillMaxWidth().clickable{if(m in measureFilters)measureFilters.remove(m)else measureFilters.add(m)},verticalAlignment=Alignment.CenterVertically){Checkbox(m in measureFilters,{checked->if(checked){if(m !in measureFilters)measureFilters.add(m)}else measureFilters.remove(m)});Text(m.name)}}}},confirmButton={Button(onClick={filterDialog=null}){Text("APPLICA")}},dismissButton={TextButton(onClick={measureFilters.clear()}){Text("AZZERA")}})
    }
    filterDialog?.takeIf{it!="measure"}?.let { type ->
        val title=when(type){"equipment"->"Filtra per attrezzatura";"groups"->"Filtra per gruppi muscolari";"types"->"Filtra per tipologia";else->"Filtra per skill"}
        val opts=when(type){"equipment"->equipmentOptions;"groups"->recoveryGroups;"types"->ExerciseMeasure.entries.map{it.name}+ExerciseCategory.entries.filter{it!=ExerciseCategory.OTHER}.map{it.name};else->calisthenicsSkills.map{it.name}}
        val target=when(type){"equipment"->equipmentFilters;"groups"->groupFilters;"types"->typeFilters;else->skillFilters}
        AlertDialog(onDismissRequest={filterDialog=null},title={Text(title)},text={LazyColumn(Modifier.heightIn(max=520.dp)){items(opts){o->Row(Modifier.fillMaxWidth().clickable{if(o in target)target.remove(o) else target.add(o)},verticalAlignment=Alignment.CenterVertically){Checkbox(o in target,{checked->if(checked){if(o !in target)target.add(o)}else target.remove(o)});Column(Modifier.clickable(enabled=type=="skills"){if(type=="skills"){selectedSkill=o;filterDialog=null}}){Text(o);if(type=="skills")calisthenicsSkills.firstOrNull{it.name==o}?.let{Text(it.category+" • "+it.level,color=Color.Gray,fontSize=12.sp)}}}}}},confirmButton={Button(onClick={filterDialog=null}){Text("APPLICA")}},dismissButton={TextButton(onClick={target.clear()}){Text("AZZERA")}})
    }
    selected?.let{ex->ExerciseDetailDialog(ex=ex,onDismiss={selected=null})}
    selectedSkill?.let { skill -> SkillDetailDialog(skill, athlete, { selectedSkill=null }, { skillWorkout=skill; selectedSkill=null }) }
    skillWorkout?.let { skill -> CustomWorkoutDialog(profile, athlete, sessions, { skillWorkout=null }, { record -> onSessionSaved(record); skillWorkout=null }, initialSkill=skill) }
    if(filteredWorkout) CustomWorkoutDialog(profile,athlete,sessions,{filteredWorkout=false},{record->onSessionSaved(record);filteredWorkout=false},initialEquipment=equipmentFilters.toList(),initialGroups=groupFilters.toList(),initialSkill=skillFilters.firstOrNull()?:"Nessuna",filtersLocked=true)
}
@Composable
private fun SkillDetailDialog(skillName: String, athlete: AthleteProfile, onDismiss: () -> Unit, onGenerate: () -> Unit) {
    val skill = calisthenicsSkills.firstOrNull { it.name == skillName }
    val linked = exercises.filter { skillName in it.skills }
    val roleOrder = mapOf("Prerequisito" to 0, "Progressione" to 1, "Forza" to 2, "Accessorio" to 3)
    val ordered = linked.sortedWith(compareBy<Exercise> { roleOrder[it.role] ?: 9 }.thenBy { levelScore(it.level) })
    val accessible = ordered.filter { levelScore(it.level) <= levelScore(athlete.level) }
    val current = accessible.lastOrNull()
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text(skillName)},
        text={LazyColumn(Modifier.heightIn(max=560.dp)){
            item {
                Text((skill?.category ?: "Calisthenics")+" • "+(skill?.level ?: ""),color=Color.Gray)
                Text("Livello atleta: "+athlete.level,modifier=Modifier.padding(top=6.dp))
                if(current!=null) Text("Livello attuale suggerito: "+current.name,color=Accent,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=8.dp))
                Text("Percorso",fontWeight=FontWeight.Bold,fontSize=18.sp,modifier=Modifier.padding(top=6.dp,bottom=6.dp))
            }
            items(ordered){ex->
                val unlocked=levelScore(ex.level)<=levelScore(athlete.level)
                Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){
                    Column(Modifier.padding(12.dp)){
                        Text(ex.name,fontWeight=FontWeight.Bold)
                        Text(ex.role+" • "+ex.level+" • "+ex.equipment,color=if(unlocked) Accent else Color.Gray,fontSize=12.sp)
                        Text(if(unlocked)"Adatto al tuo livello" else "Da sbloccare con il livello successivo",fontSize=11.sp,color=Color.Gray)
                    }
                }
            }
            if(ordered.isEmpty()) item{Text("Progressione in preparazione.",color=Color.Gray)}
        }},
        confirmButton={Button(onClick=onGenerate,enabled=linked.isNotEmpty()){Text("GENERA WORKOUT")}},
        dismissButton={TextButton(onClick=onDismiss){Text("CHIUDI")}}
    )
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
        items(listOf("Forza + Skill Calisthenics","Ipertrofia","Calisthenics","Fitness generale")){x->Row(Modifier.fillMaxWidth().clickable{goal=x},verticalAlignment=Alignment.CenterVertically){RadioButton(goal==x,{goal=x});Text(x)}}
        item{if(goal=="Forza + Skill Calisthenics"){Text("Skill prioritarie (opzionali)",fontWeight=FontWeight.Bold);Text("Se non ne selezioni nessuna, il profilo viene trattato come Forza.",color=Color.Gray,fontSize=11.sp);calisthenicsSkills.forEach{s->Row(Modifier.fillMaxWidth().clickable{if(s.name in prioritySkills)prioritySkills.remove(s.name)else prioritySkills.add(s.name)},verticalAlignment=Alignment.CenterVertically){Checkbox(s.name in prioritySkills,{if(it){if(s.name !in prioritySkills)prioritySkills.add(s.name)}else prioritySkills.remove(s.name)});Text(s.name)}}};Text("Livello",fontWeight=FontWeight.Bold); listOf("Principiante","Intermedio","Avanzato").forEach{x->Row(Modifier.fillMaxWidth().clickable{level=x},verticalAlignment=Alignment.CenterVertically){RadioButton(level==x,{level=x});Text(x)}}; Text("Allenamenti a settimana: "+days,fontWeight=FontWeight.Bold); Row(verticalAlignment=Alignment.CenterVertically){OutlinedButton(onClick={if(days>1)days--}){Text("−")};Spacer(Modifier.width(12.dp));Text(days.toString(),fontSize=20.sp);Spacer(Modifier.width(12.dp));Button(onClick={if(days<7)days++}){Text("+")}}}
    }},confirmButton={Button(onClick={onSave(AthleteProfile(name.trim(),goal,level,days,age.toIntOrNull()?:0,height.toFloatOrNull()?:0f,weight.toFloatOrNull()?:0f,sex,prioritySkills.toList()))}){Text("SALVA")}},dismissButton={TextButton(onClick=onDismiss){Text("ANNULLA")}})
}

@Composable
private fun ExerciseExecutionInputs(ex:Exercise, enabled:Boolean, onComplete:(Int?,Int?,Double?,Double?,Double?)->Unit) {
    val p=prescriptionFor(ex)
    var value by remember(ex.name){mutableStateOf(p.value.toString())}
    var load by remember(ex.name){mutableStateOf("")}
    var assistance by remember(ex.name){mutableStateOf("")}
    Column {
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
    var minutes by remember { mutableIntStateOf(45) }
    val selectedEquipment = remember { mutableStateListOf<String>().apply { addAll(if(initialEquipment.isNotEmpty()) initialEquipment else profile.equipment) } }
    val selectedGroups = remember { mutableStateListOf<String>().apply { addAll(initialGroups) } }
    var mode by remember(initialSkill,initialGroups) { mutableStateOf(if(initialSkill!="Nessuna") "Skill" else "Parti del corpo") }
    val selectedSkills = remember(initialSkill) { mutableStateListOf<String>().apply { if(initialSkill!="Nessuna") add(initialSkill) } }
    var generated by remember { mutableStateOf(restored?.exercises?.mapNotNull{s->exercises.firstOrNull{it.name==s.exerciseName}} ?: emptyList()) }
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
    val reps = remember { mutableStateMapOf<String, Int>() }
    val restSeconds = remember { mutableStateMapOf<String, Int>() }
    val executedSets = remember { mutableStateMapOf<String,List<WorkoutSetState>>().apply{restored?.exercises?.forEach{put(it.exerciseName,it.sets)} } }
    val exerciseRpe = remember { mutableStateMapOf<String,Int>().apply{restored?.exercises?.forEach{e->e.rpe?.let{put(e.exerciseName,it)}}} }
    var workoutRpe by remember { mutableIntStateOf(restored?.workoutRpe?:5) }
    val listState = rememberLazyListState()

    LaunchedEffect(running,paused,restEndsAt,sessionStartedAt){ while(running&&!paused){ val now=System.currentTimeMillis();restRemaining=remainingSeconds(restEndsAt,now);sessionStartedAt?.let{elapsedSeconds=((now-it)/1000L).toInt()};delay(500) } }
    LaunchedEffect(running,currentExercise,restRemaining,completed.toMap(),exerciseRpe.toMap(),workoutRpe) {
        if(running&&store!=null){
            val now=System.currentTimeMillis()
            val states=generated.mapIndexed{i,ex->completedExerciseState(i,ex,completed[ex.name]?:0,exerciseRpe[ex.name]).let{s->val actual=executedSets[ex.name];val withActual=if(actual!=null)s.copy(sets=actual)else s;if(skipped[ex.name]==true)withActual.copy(status=WorkoutExerciseStatus.SKIPPED)else withActual}}
            store.saveActiveWorkout(WorkoutSessionState(sessionId,sessionStartedAt?:now,"Personalizzato",states,generated.indexOfFirst{it.name==currentExercise}.coerceAtLeast(0),elapsedSeconds*1000L,if(paused)now else null,restEndsAt,workoutRpe,runnerKind="custom",restKind=restKind))
        }
    }
    LaunchedEffect(running) { if(running){ if(currentExercise==null)currentExercise=generated.firstOrNull{(completed[it.name]?:0)<prescriptionFor(it).sets}?.name; val i=generated.indexOfFirst{it.name==currentExercise}.coerceAtLeast(0);listState.scrollToItem(i) } }
    LaunchedEffect(currentExercise) { if(running){ val i=generated.indexOfFirst{it.name==currentExercise}; if(i>=0) listState.animateScrollToItem(i) } }

    val skills = listOf("Nessuna") + calisthenicsSkills.map { it.name }
    val equipmentOptions = listOf("Corpo libero","Sbarra","Parallele","Bilanciere","Manubri","Cavi","Macchine","Elastici","Panca","Kettlebell","Leg press","Anelli","TRX")
    fun prescription(ex:Exercise):Pair<Int,Int> { val p=prescriptionFor(ex); return p.value.toInt() to ex.setRestSec }
    fun build() {
        val count=when{minutes<=25->4;minutes<=40->5;minutes<=60->6;else->8}; val recovery=recoveryScores(sessions)
        val base=exercises.filter{ex->levelScore(ex.level)<=levelScore(athlete.level)&&selectedEquipment.any{eq->ex.equipment.contains(eq,true)||(eq=="Macchine"&&ex.equipment.contains("Macchina",true))}&&(mode!="Parti del corpo"||selectedGroups.isEmpty()||selectedGroups.any{ex.group.contains(it,true)})}
        val ranked=base.sortedWith(compareBy<Exercise>{ex->val rec=recovery.filterKeys{ex.group.contains(it,true)}.values.minOrNull()?:100;if(rec<40)100 else if(rec<60)50 else 0}.thenBy{if(selectedSkills.any{s->s in it.skills})0 else 1}.thenBy{if(it.role=="Forza")0 else 1})
        generated=if(mode=="Skill"){
            val skillWork=ranked.filter{ex->selectedSkills.any{it in ex.skills}}
            (skillWork+ranked.filter{it !in skillWork && (it.role=="Forza"||it.role=="Prerequisito"||it.role=="Accessorio")}).distinct().take(count)
        } else ranked.take(count)
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
                                val available=exercises.count{ex->selectedEquipment.any{eq->ex.equipment.contains(eq,true)||(eq=="Macchine"&&ex.equipment.contains("Macchina",true))}}
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
                    items(generated){ex->
                        val n=completed[ex.name]?:0;val targetSets=prescriptionFor(ex).sets;val done=n>=targetSets;val isSkipped=skipped[ex.name]==true;val expanded=!done||(expandedCompleted[ex.name]?:false)
                        Card(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{if(running){if(done)expandedCompleted[ex.name]=!(expandedCompleted[ex.name]?:false) else {skipped[ex.name]=false;currentExercise=ex.name}}}){
                            Column(Modifier.padding(10.dp)){
                                Text((if(done)"✓ " else if(running&&currentExercise==ex.name)"▶ " else "")+ex.name,fontWeight=FontWeight.Bold)
                                if(done) Text("COMPLETATO • "+targetSets+"/"+targetSets+" serie",color=Accent,fontSize=12.sp)
                                if(expanded){
                                    val p=prescriptionFor(ex)
                                    Text(ex.group+" • "+ex.equipment+" • "+ex.role,color=Color.Gray,fontSize=12.sp)
                                    Text(p.sets.toString()+" serie × "+(reps[ex.name]?:p.value.toInt())+" "+p.unit+" • "+ex.setRestSec+"s serie / "+ex.exerciseRestSec+"s esercizio",color=Accent,fontSize=12.sp)
                                    if(running && !done && !isSkipped){
                                        ExerciseExecutionInputs(ex,restRemaining==0&&!paused){ar,asec,dist,load,assist->val p=prescriptionFor(ex);val nNow=completed[ex.name]?:0;val old=executedSets[ex.name]?:plannedExerciseState(0,ex).sets;if(nNow<old.size){val xs=old.toMutableList();xs[nNow]=xs[nNow].copy(actualReps=ar,actualSeconds=asec,actualDistanceMeters=dist,actualLoadKg=load,actualAssistanceKg=assist,completed=true);executedSets[ex.name]=xs;completed[ex.name]=nNow+1;if(nNow+1>=p.sets){restKind="Esercizio";restRemaining=ex.exerciseRestSec}else{restKind="Serie";restRemaining=ex.intervalRestSec?:restSeconds[ex.name]?:ex.setRestSec};restEndsAt=System.currentTimeMillis()+restRemaining*1000L}}

                                        Row(verticalAlignment=Alignment.CenterVertically){Text("Recupero serie: ",fontSize=12.sp,color=Color.Gray);listOf(30,60,90,120,180).forEach{v->FilterChip((restSeconds[ex.name]?:60)==v,{restSeconds[ex.name]=v},{Text(v.toString()+"s")},Modifier.padding(end=3.dp))}}
                                        if(n>0){Text("Fatica esercizio (RPE): "+(exerciseRpe[ex.name]?:5)+"/10",fontSize=12.sp);Slider(value=(exerciseRpe[ex.name]?:5).toFloat(),onValueChange={exerciseRpe[ex.name]=it.toInt()},valueRange=1f..10f,steps=8)}
                                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                                            Text("$n/$targetSets",modifier=Modifier.weight(1f),fontWeight=FontWeight.Bold)
                                            OutlinedButton(onClick={if(n>0)completed[ex.name]=n-1}){Text("−")};Spacer(Modifier.width(6.dp))
                                            Button(onClick={
                                                if(n<targetSets&&restRemaining==0&&!paused){
                                                    val next=n+1;completed[ex.name]=next
                                                    if(next>=targetSets){restKind="Esercizio";restRemaining=exerciseRestSeconds;restEndsAt=System.currentTimeMillis()+exerciseRestSeconds*1000L;currentExercise=generated.dropWhile{it.name!=ex.name}.drop(1).firstOrNull{(completed[it.name]?:0)<prescriptionFor(it).sets}?.name?:generated.firstOrNull{(completed[it.name]?:0)<prescriptionFor(it).sets}?.name}
                                                    else {restKind="Serie";restRemaining=restSeconds[ex.name]?:60;restEndsAt=System.currentTimeMillis()+restRemaining*1000L;currentExercise=ex.name}
                                                }
                                            },enabled=restRemaining==0&&!paused){Text("+ SERIE")}
                                        }
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
                    else {val total=generated.size*3;onSave(SessionRecord(System.currentTimeMillis(),"Personalizzato"+if(selectedSkills.isNotEmpty())" • "+selectedSkills.joinToString(", ") else "",completed.values.sum(),total,generated.map{it.name},exerciseRpe.toMap(),workoutRpe,completed.toMap(),elapsedSeconds,generated.mapIndexed{i,ex->completedExerciseState(i,ex,completed[ex.name]?:0,exerciseRpe[ex.name]).let{s->val actual=executedSets[ex.name];val withActual=if(actual!=null)s.copy(sets=actual)else s;if(skipped[ex.name]==true)withActual.copy(status=WorkoutExerciseStatus.SKIPPED)else withActual}},skipped.filterValues{it}.keys.toList()));store?.clearActiveWorkout();onDismiss()}
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
    val compatible = remember(profile, athlete, sessions, restored) { restored?.exercises?.mapNotNull { s -> exercises.firstOrNull { it.name==s.exerciseName } }?.takeIf{it.isNotEmpty()} ?: generateWorkout(profile, athlete, sessions) }
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
    LaunchedEffect(started, paused, currentExercise, restRemaining, doneSets, workoutRpe) {
        if(started) {
            val now=System.currentTimeMillis()
            val states=compatible.mapIndexed { index, ex ->
                val p=prescriptionFor(ex); val done=completedSets[ex.name]?:0
                WorkoutExerciseState(ex.name,ex.measure,executedSets[ex.name] ?: (0 until p.sets).map { s -> WorkoutSetState(s,targetReps=if(ex.measure==ExerciseMeasure.REPS)p.value.toInt() else null,targetSeconds=if(ex.measure==ExerciseMeasure.SECONDS||ex.measure==ExerciseMeasure.DURATION||ex.measure==ExerciseMeasure.INTERVALS)p.value.toInt() else null,targetDistanceMeters=if(ex.measure==ExerciseMeasure.DISTANCE)p.value else null,completed=s<done) },when{skipped[ex.name]==true->WorkoutExerciseStatus.SKIPPED;done>=p.sets->WorkoutExerciseStatus.COMPLETED;done>0->WorkoutExerciseStatus.PARTIAL;else->WorkoutExerciseStatus.PENDING},exerciseRpe[ex.name],false,ex.setRestSec,ex.exerciseRestSec,index,index)
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
                                Text((if(done)"✓ " else if(started&&currentExercise==ex.name)"▶ " else "")+ex.name,fontWeight=FontWeight.Bold)
                                if(done)Text("COMPLETATO • "+targetSets+"/"+targetSets+" serie",color=Accent,fontSize=12.sp)
                                if(expanded){
                                    val p=prescriptionFor(ex)
                                    Text(p.sets.toString()+" serie × "+p.value.toInt()+" "+p.unit+" • "+ex.setRestSec+"s serie / "+ex.exerciseRestSec+"s esercizio",color=Accent,fontSize=12.sp)
                                    if(started&&count>0){Text("Fatica esercizio (RPE): "+(exerciseRpe[ex.name]?:5)+"/10",fontSize=12.sp);Slider(value=(exerciseRpe[ex.name]?:5).toFloat(),onValueChange={exerciseRpe[ex.name]=it.toInt()},valueRange=1f..10f,steps=8)}
                                    Text(ex.equipment+" • "+ex.role,color=Color.Gray,fontSize=12.sp)
                                    if(started&&!done&&!isSkipped){ ExerciseExecutionInputs(ex,!paused&&restRemaining==0){ar,asec,dist,load,assist->val p=prescriptionFor(ex);val old=executedSets[ex.name]?:plannedExerciseState(0,ex).sets;if(count<old.size){val xs=old.toMutableList();xs[count]=xs[count].copy(actualReps=ar,actualSeconds=asec,actualDistanceMeters=dist,actualLoadKg=load,actualAssistanceKg=assist,completed=true);executedSets[ex.name]=xs;completedSets[ex.name]=count+1;if(count+1>=p.sets){restKind="Esercizio";restRemaining=ex.exerciseRestSec}else{restKind="Serie";restRemaining=ex.intervalRestSec?:ex.setRestSec};restEndsAt=System.currentTimeMillis()+restRemaining*1000L}}; Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically){
                                        Text("$count/$targetSets",color=Accent,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                                        OutlinedButton(onClick={if(count>0)completedSets[ex.name]=count-1}){Text("−")}
                                        Spacer(Modifier.width(8.dp))
                                        Button(onClick={
                                            if(!paused&&restRemaining==0&&count<targetSets){
                                                val next=count+1
                                                completedSets[ex.name]=next
                                                if(next>=targetSets){
                                                    restKind="Esercizio";restRemaining=ex.exerciseRestSec;restEndsAt=System.currentTimeMillis()+ex.exerciseRestSec*1000L
                                                    currentExercise=compatible.dropWhile{it.name!=ex.name}.drop(1).firstOrNull{(completedSets[it.name]?:0)<prescriptionFor(it).sets}?.name?:compatible.firstOrNull{(completedSets[it.name]?:0)<prescriptionFor(it).sets}?.name
                                                }else{
                                                    restKind="Serie";restRemaining=ex.setRestSec;restEndsAt=System.currentTimeMillis()+ex.setRestSec*1000L;currentExercise=ex.name
                                                }
                                            }
                                        },enabled=!paused&&restRemaining==0){Text("+ SERIE")}
                                    }}
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
            else{val structured=compatible.mapIndexed { index,ex->completedExerciseState(index,ex,completedSets[ex.name]?:0,exerciseRpe[ex.name]).let{s->executedSets[ex.name]?.let{s.copy(sets=it)}?:s} };onSave(SessionRecord(System.currentTimeMillis(),profile.name,doneSets,totalSets,compatible.map{it.name},exerciseRpe.toMap(),workoutRpe,compatible.associate{it.name to (completedSets[it.name]?:0)},elapsedSeconds,structured,structured.filter{it.status==WorkoutExerciseStatus.SKIPPED}.map{it.exerciseName}));store.clearActiveWorkout();onDismiss()}
        }){Text(if(!started)"AVVIA WORKOUT" else if(totalSets>0&&doneSets==totalSets)"TERMINA E SALVA" else "SALVA E CHIUDI")} },
        dismissButton = { TextButton(onClick=onDismiss){Text("ANNULLA")} }
    )
}

@Composable
private fun ExerciseDetailDialog(ex: Exercise, onDismiss: () -> Unit) {
    val instructions = when (ex.name) {
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
    val options = listOf("Corpo libero", "Sbarra", "Parallele", "Bilanciere", "Manubri", "Cavi", "Macchine", "Elastici", "Panca", "Kettlebell", "Leg press", "Anelli", "TRX")
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
