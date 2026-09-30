package com.hybridfitnesscoach.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Xml
import androidx.compose.runtime.mutableStateListOf
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser

/** Paths only: diagnostics must not include the athlete's stored values. */
internal class StoreRecovery {
    val issues = mutableStateListOf<String>()

    fun problem(path: String) {
        if (path !in issues) issues.add(path)
    }

    fun <T> read(path: String, fallback: T, decode: () -> T): T = try {
        decode()
    } catch (_: Exception) {
        problem(path)
        fallback
    }

    fun <T> records(array: JSONArray, path: String, decode: (JSONObject, String) -> T): List<T> =
        (0 until array.length()).mapNotNull { index ->
            val recordPath = "$path[$index]"
            read<T?>(recordPath, null) { decode(array.getJSONObject(index), recordPath) }
        }
}

private fun JSONObject.storedArray(key: String): JSONArray? =
    if (!has(key)) null else getJSONArray(key)

private fun JSONObject.storedString(key: String, default: String = ""): String =
    if (!has(key)) default else (get(key) as? String ?: error("Invalid string"))

private fun JSONObject.storedInt(key: String, default: Int = 0): Int =
    if (!has(key)) default else strictNumber(key).let {
        require(it.toDouble() == it.toInt().toDouble())
        it.toInt()
    }

private fun JSONObject.storedLong(key: String, default: Long = 0): Long =
    if (!has(key)) default else strictNumber(key).let {
        require(it.toDouble() == it.toLong().toDouble())
        it.toLong()
    }

private fun JSONObject.strictNumber(key: String): Number =
    (get(key) as? Number ?: error("Invalid number")).also { require(it.toDouble().isFinite()) }

private fun JSONObject.storedBoolean(key: String, default: Boolean = false): Boolean =
    if (!has(key)) default else (get(key) as? Boolean ?: error("Invalid boolean"))

private fun JSONObject.requireVersion(vararg supported: Int) {
    require(has("schemaVersion") && storedInt("schemaVersion") in supported) { "Unsupported schema version" }
}

private fun WorkoutSetState.toJson() = JSONObject()
    .put("index", index).put("targetReps", targetReps).put("targetSeconds", targetSeconds)
    .put("targetDistanceMeters", targetDistanceMeters).put("targetLoadKg", targetLoadKg)
    .put("targetAssistanceKg", targetAssistanceKg).put("actualReps", actualReps)
    .put("actualSeconds", actualSeconds).put("actualDistanceMeters", actualDistanceMeters)
    .put("actualLoadKg", actualLoadKg).put("actualAssistanceKg", actualAssistanceKg)
    .put("completed", completed).put("restUsedSec", restUsedSec)
private fun JSONObject.optNullableInt(key:String):Int? = if(has(key)&&!isNull(key)) storedInt(key) else null
private fun JSONObject.optNullableLong(key:String):Long? = if(has(key)&&!isNull(key)) storedLong(key) else null
private fun JSONObject.optNullableDouble(key:String):Double? = if(has(key)&&!isNull(key)) strictNumber(key).toDouble() else null
private fun workoutSetFromJson(o:JSONObject)=WorkoutSetState(
    o.storedInt("index"),o.optNullableInt("targetReps"),o.optNullableInt("targetSeconds"),
    o.optNullableDouble("targetDistanceMeters"),o.optNullableDouble("targetLoadKg"),o.optNullableDouble("targetAssistanceKg"),
    o.optNullableInt("actualReps"),o.optNullableInt("actualSeconds"),o.optNullableDouble("actualDistanceMeters"),
    o.optNullableDouble("actualLoadKg"),o.optNullableDouble("actualAssistanceKg"),o.storedBoolean("completed"),o.optNullableInt("restUsedSec"))
private fun WorkoutExerciseState.toJson():JSONObject { val a=JSONArray();sets.forEach{a.put(it.toJson())};return JSONObject()
    .put("exerciseName",exerciseName).put("measure",measure.name).put("sets",a).put("status",status.name)
    .put("rpe",rpe).put("pinned",pinned).put("setRestSec",setRestSec).put("exerciseRestSec",exerciseRestSec)
    .put("originalOrder",originalOrder).put("currentOrder",currentOrder).put("exerciseId",exerciseId)
    .put("exerciseSnapshot",exerciseSnapshot?.toPlanSnapshotJson()) }
private fun workoutExerciseFromJson(o:JSONObject, recovery:StoreRecovery, path:String):WorkoutExerciseState { val a=o.storedArray("sets")?:JSONArray();return WorkoutExerciseState(
    o.getString("exerciseName"),ExerciseMeasure.valueOf(o.storedString("measure","REPS")),
    recovery.records(a,"$path.sets") { set, _ -> workoutSetFromJson(set) },
    WorkoutExerciseStatus.valueOf(o.storedString("status","PENDING")),
    o.optNullableInt("rpe"),o.storedBoolean("pinned"),o.storedInt("setRestSec",90),o.storedInt("exerciseRestSec",120),o.storedInt("originalOrder"),o.storedInt("currentOrder"),
    o.storedString("exerciseId").takeIf{it.isNotBlank()},
    if(o.has("exerciseSnapshot")&&!o.isNull("exerciseSnapshot")) exerciseFromPlanSnapshot(o.getJSONObject("exerciseSnapshot")) else null) }
private fun WorkoutSessionState.toJson():JSONObject { val a=JSONArray();exercises.forEach{a.put(it.toJson())};return JSONObject()
    .put("schemaVersion",2).put("id",id).put("startedAtEpochMs",startedAtEpochMs).put("profileName",profileName)
    .put("exercises",a).put("currentExerciseIndex",currentExerciseIndex).put("elapsedBeforePauseMs",elapsedBeforePauseMs)
    .put("pausedAtEpochMs",pausedAtEpochMs).put("restEndsAtEpochMs",restEndsAtEpochMs).put("workoutRpe",workoutRpe).put("finishedAtEpochMs",finishedAtEpochMs)
    .put("runnerKind",runnerKind).put("restKind",restKind).put("modifications",JSONArray().apply{modifications.forEach{put(it)}}).put("plannedWorkoutId",plannedWorkoutId).put("pausedRestRemainingSec",pausedRestRemainingSec) }
private fun workoutSessionFromJson(o:JSONObject, recovery:StoreRecovery):WorkoutSessionState { o.requireVersion(2); val a=o.getJSONArray("exercises");return WorkoutSessionState(
    o.getString("id"),o.getLong("startedAtEpochMs"),o.storedString("profileName",""),
    recovery.records(a,"active_workout_v1.exercises") { exercise, path -> workoutExerciseFromJson(exercise,recovery,path) },o.storedInt("currentExerciseIndex"),
    o.storedLong("elapsedBeforePauseMs",0L),o.optNullableLong("pausedAtEpochMs"),o.optNullableLong("restEndsAtEpochMs"),
    o.optNullableInt("workoutRpe"),o.optNullableLong("finishedAtEpochMs"),o.storedString("runnerKind","automatic"),if(o.has("restKind")&&!o.isNull("restKind"))o.getString("restKind") else null,
    (o.storedArray("modifications")?:JSONArray()).let{a->(0 until a.length()).map{a.getString(it)}},o.storedString("plannedWorkoutId").takeIf{it.isNotBlank()},o.optNullableInt("pausedRestRemainingSec")) }

private fun Exercise.toPlanSnapshotJson():JSONObject = JSONObject()
    .put("name",name).put("group",group).put("equipment",equipment).put("level",level)
    .put("skills",JSONArray().apply{skills.forEach{put(it)}}).put("role",role)
    .put("setRestSec",setRestSec).put("exerciseRestSec",exerciseRestSec)
    .put("secondaryGroups",JSONArray().apply{secondaryGroups.forEach{put(it)}})
    .put("movementPattern",movementPattern).put("category",category.name)
    .put("intensity",intensity).put("systemicFatigue",systemicFatigue)
    .put("technicalComplexity",technicalComplexity).put("measure",measure.name)
    .put("laterality",laterality.name).put("resistanceMode",resistanceMode.name)
    .put("progressionKey",progressionKey).put("targetSets",targetSets)
    .put("targetValue",targetValue).put("intervalRestSec",intervalRestSec)
private fun JSONObject.planStringList(key:String):List<String> {
    val a=storedArray(key) ?: JSONArray()
    return (0 until a.length()).map{a.get(it) as? String ?: error("Invalid string")}
}
private fun exerciseFromPlanSnapshot(o:JSONObject):Exercise = Exercise(
    o.getString("name"),o.getString("group"),o.getString("equipment"),o.getString("level"),
    o.planStringList("skills"),o.storedString("role","Base"),o.getInt("setRestSec"),
    o.getInt("exerciseRestSec"),o.planStringList("secondaryGroups"),o.storedString("movementPattern",""),
    ExerciseCategory.valueOf(o.getString("category")),o.getInt("intensity"),
    o.getInt("systemicFatigue"),o.getInt("technicalComplexity"),
    ExerciseMeasure.valueOf(o.getString("measure")),Laterality.valueOf(o.getString("laterality")),
    ResistanceMode.valueOf(o.getString("resistanceMode")),
    if(o.has("progressionKey") && !o.isNull("progressionKey")) o.getString("progressionKey") else null,
    o.getInt("targetSets"),o.optNullableDouble("targetValue"),o.optNullableInt("intervalRestSec"))

/**
 * Current-schema store. Failed reads are never treated as a new installation:
 * readable current records are exposed, but every write is blocked until the
 * original data is repaired. No recovery path writes a partial result.
 */
class AppStore(context: Context) {
    private val recovery = StoreRecovery()
    val issues: List<String> get() = recovery.issues
    private val prefs: SharedPreferences? = recovery.read("preference_file", null) {
        validatePreferenceFile(context)
        context.getSharedPreferences("hybrid_fitness_store", Context.MODE_PRIVATE)
    }

    init { validateStore() }

    private fun <T> field(key: String, fallback: T, read: (SharedPreferences) -> T): T =
        recovery.read(key, fallback) { prefs?.let(read) ?: fallback }

    private fun string(key: String, fallback: String? = null): String? =
        field(key, fallback) { it.getString(key, fallback) }

    private fun int(key: String, fallback: Int = 0): Int = field(key, fallback) { it.getInt(key, fallback) }
    private fun float(key: String): Float = field(key, 0f) { it.getFloat(key, 0f).also { v -> require(v.isFinite()) } }

    private fun validateStore() {
        loadAthlete()
        loadActiveProfileName()
        loadCustomProfiles()
        loadSessions()
        loadActiveWorkout()
        readTrainingPlan()
        loadProgressionNodes()
        string("weight_history")
    }

    private fun write(edit: SharedPreferences.Editor.() -> Unit): Boolean {
        // Recheck even when the caller never loaded the area it is about to replace.
        validateStore()
        if (issues.isNotEmpty() || prefs == null) return false
        val committed = recovery.read("storage_write", false) { prefs.edit().apply(edit).commit() }
        if (!committed) recovery.problem("storage_write")
        return committed
    }

    fun loadAthlete(): AthleteProfile {
        val goal = string("athlete_goal", "Forza") ?: "Forza"
        return AthleteProfile(string("athlete_name", "") ?: "", goal,
            string("athlete_level", "Intermedio") ?: "Intermedio", int("athlete_days", 3),
            int("athlete_age"), float("athlete_height"), float("athlete_weight"),
            string("athlete_sex", "Non specificato") ?: "Non specificato",
            (string("athlete_priority_skills", "") ?: "").split("|").filter { it.isNotBlank() })
    }

    fun saveAthlete(p: AthleteProfile) {
        val history = string("weight_history", "") ?: ""
        write {
            putString("athlete_name", p.name).putString("athlete_goal", p.goal).putString("athlete_level", p.level)
                .putInt("athlete_days", p.daysPerWeek).putInt("athlete_age", p.age)
                .putFloat("athlete_height", p.heightCm).putFloat("athlete_weight", p.weightKg)
                .putString("athlete_sex", p.sex).putString("athlete_priority_skills", p.prioritySkills.joinToString("|"))
            if (p.weightKg > 0) putString("weight_history", ("${System.currentTimeMillis()}:${p.weightKg};" + history).take(4000))
        }
    }

    fun loadActiveProfileName(): String = string("active_equipment", "Palestra") ?: "Palestra"
    fun saveActiveProfileName(name: String) = write { putString("active_equipment", name) }

    fun loadCustomProfiles(): List<EquipmentProfile> {
        val raw = string("custom_profiles") ?: return profiles
        return recovery.read("custom_profiles", emptyList()) {
            recovery.records(JSONArray(raw), "custom_profiles") { o, _ ->
                require(o.has("equipment"))
                EquipmentProfile(o.getString("name"), o.storedString("icon", "📍"), o.planStringList("equipment"))
            }
        }
    }

    fun saveCustomProfiles(items: List<EquipmentProfile>) {
        val array = JSONArray()
        items.forEach { array.put(JSONObject().put("name", it.name).put("icon", it.icon).put("equipment", JSONArray(it.equipment))) }
        write { putString("custom_profiles", array.toString()) }
    }

    fun saveActiveWorkout(state: WorkoutSessionState) = write { putString("active_workout_v1", state.toJson().toString()) }
    fun loadActiveWorkout(): WorkoutSessionState? {
        val raw = string("active_workout_v1") ?: return null
        return recovery.read("active_workout_v1", null) { workoutSessionFromJson(JSONObject(raw), recovery) }
    }
    fun clearActiveWorkout() = write { remove("active_workout_v1") }

    private fun trainingPlanJson(plan: TrainingPlan): String {
        val workouts = JSONArray()
        plan.workouts.forEach { w ->
            val snapshots = JSONArray(); w.exercises.forEach { snapshots.put(it.toPlanSnapshotJson()) }
            workouts.put(JSONObject().put("id", w.id).put("epochDay", w.epochDay).put("weekIndex", w.weekIndex)
                .put("sequenceInWeek", w.sequenceInWeek).put("exerciseNames", JSONArray(w.exercises.map { it.name }))
                .put("exerciseSnapshots", snapshots).put("state", w.state.name).put("originalEpochDay", w.originalEpochDay)
                .put("adaptationReason", w.adaptationReason).put("actualExecutionEpochDay", w.actualExecutionEpochDay))
        }
        return JSONObject().put("schemaVersion", 3).put("id", plan.id).put("startEpochDay", plan.startEpochDay)
            .put("weeks", plan.weeks).put("daysPerWeek", plan.daysPerWeek)
            .put("goal", plan.goal).put("trainingDays", JSONArray(plan.trainingDays.sorted()))
            .put("prioritySkills", JSONArray(plan.prioritySkills)).put("workouts", workouts).toString()
    }

    fun saveTrainingPlan(plan: TrainingPlan) = write { putString("training_plan_v091", trainingPlanJson(plan)) }

    private fun readTrainingPlan(): TrainingPlan? {
        val raw = string("training_plan_v091") ?: return null
        return recovery.read("training_plan_v091", null) {
            val root = JSONObject(raw)
            root.requireVersion(3)
            val workouts = recovery.records(root.getJSONArray("workouts"), "training_plan_v091.workouts") { o, path ->
                val snapshots = o.getJSONArray("exerciseSnapshots")
                val planned = recovery.records(snapshots, "$path.exerciseSnapshots") { snapshot, _ ->
                    exerciseFromPlanSnapshot(snapshot)
                }
                PlannedWorkout(
                    o.getString("id"), o.getLong("epochDay"), o.getInt("weekIndex"), o.getInt("sequenceInWeek"), planned,
                    PlannedWorkoutState.valueOf(o.storedString("state", "PLANNED")),
                    o.storedLong("originalEpochDay", o.getLong("epochDay")),
                    o.storedString("adaptationReason").takeIf { it.isNotBlank() },
                    o.optNullableLong("actualExecutionEpochDay")
                )
            }
            val trainingDaysArray = root.getJSONArray("trainingDays")
            TrainingPlan(
                root.getString("id"), root.getLong("startEpochDay"), root.getInt("weeks"), root.getInt("daysPerWeek"), workouts,
                normalizePlanGoal(root.getString("goal")),
                (0 until trainingDaysArray.length()).map { trainingDaysArray.getInt(it) }.toSet(),
                root.planStringList("prioritySkills")
            )
        }
    }

    fun loadTrainingPlan(): TrainingPlan? = readTrainingPlan()

    fun loadSessions(): List<SessionRecord> {
        val raw = string("sessions") ?: return emptyList()
        return recovery.read("sessions", emptyList()) {
            recovery.records(JSONArray(raw), "sessions") { o, path ->
                val structured = o.storedArray("structuredExercises") ?: JSONArray()
                fun intMap(key: String): Map<String, Int> = if (!o.has(key)) emptyMap() else
                    o.getJSONObject(key).let { map -> map.keys().asSequence().associateWith { map.storedInt(it) } }
                SessionRecord(o.getLong("timestamp"), o.getString("profile"), o.getInt("done"), o.getInt("total"),
                    o.planStringList("exercises"), intMap("exerciseRpe"), o.storedInt("workoutRpe"),
                    intMap("completedByExercise"), o.storedInt("durationSec"),
                    recovery.records(structured, "$path.structuredExercises") { exercise, exercisePath ->
                        workoutExerciseFromJson(exercise, recovery, exercisePath)
                    }, o.planStringList("skippedExercises"), o.planStringList("modifications"),
                    o.storedString("plannedWorkoutId").takeIf { it.isNotBlank() })
            }.sortedByDescending { it.timestamp }
        }
    }

    private fun sessionsJson(all: List<SessionRecord>): String {
        val array = JSONArray()
        all.distinctBy { it.timestamp }.sortedByDescending { it.timestamp }.forEach { s ->
            val structured = JSONArray(); s.structuredExercises.forEach { structured.put(it.toJson()) }
            array.put(JSONObject().put("timestamp", s.timestamp).put("profile", s.profileName).put("done", s.completedSets)
                .put("total", s.totalSets).put("exercises", JSONArray(s.exerciseNames)).put("exerciseRpe", JSONObject(s.exerciseRpe))
                .put("workoutRpe", s.workoutRpe).put("completedByExercise", JSONObject(s.completedByExercise))
                .put("durationSec", s.durationSec).put("structuredExercises", structured).put("skippedExercises", JSONArray(s.skippedExercises))
                .put("modifications", JSONArray(s.modifications)).put("plannedWorkoutId", s.plannedWorkoutId))
        }
        return array.toString()
    }

    fun saveSessions(all: List<SessionRecord>) = write { putString("sessions", sessionsJson(all)) }

    private fun skillKey(skill: String) = "skill_node_" + skill.lowercase().replace(" ", "_")
    fun loadSkillNode(skill: String): String? = string(skillKey(skill))
    fun saveSkillNode(skill: String, nodeId: String) = write { putString(skillKey(skill), nodeId) }
    fun loadProgressionNodes(): Map<String, String> = recovery.read("progression", emptyMap()) {
        prefs?.all.orEmpty().keys.filter { it.startsWith("skill_node_") }.mapNotNull { key ->
            string(key)?.let { key to it }
        }.toMap()
    }

    /** Commit history, linked plan and active-runner removal in one preferences transaction. */
    fun finishWorkout(s: SessionRecord): Boolean {
        val history = loadSessions()
        val plan = loadTrainingPlan()
        val updatedPlan = plan?.let {
            val executionDay = java.time.Instant.ofEpochMilli(s.timestamp)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()
            PlannedWorkoutReconciler.complete(it, s, executionDay, 0).plan
        }
        return write {
            putString("sessions", sessionsJson(listOf(s) + history))
            if (updatedPlan != null && updatedPlan != plan) {
                putString("training_plan_v091", trainingPlanJson(updatedPlan))
            }
            remove("active_workout_v1")
        }
    }

    fun addSession(s: SessionRecord) = saveSessions(listOf(s) + loadSessions())

    fun completePlannedWorkoutForSession(s: SessionRecord, minimumGapDays: Int = 0): ScheduleUpdate? {
        val plan = loadTrainingPlan() ?: return null
        val executionDay = java.time.Instant.ofEpochMilli(s.timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()
        val update = PlannedWorkoutReconciler.complete(plan, s, executionDay, minimumGapDays)
        if (update.plan == plan) return null
        saveTrainingPlan(update.plan)
        return update
    }

    fun deleteSession(timestamp: Long) {
        val sessions = loadSessions()
        val removed = sessions.firstOrNull { it.timestamp == timestamp } ?: return
        val remaining = sessions.filterNot { it.timestamp == timestamp }
        val plan = loadTrainingPlan()
        val restored = plan?.let { PlannedWorkoutReconciler.afterDeletion(it, removed, remaining) }
        write {
            putString("sessions", sessionsJson(remaining))
            if (restored != null && restored != plan) putString("training_plan_v091", trainingPlanJson(restored))
        }
    }
}

/** Android otherwise treats malformed preference XML as an empty map. */
private fun validatePreferenceFile(context: Context) {
    val file = File(context.applicationInfo.dataDir, "shared_prefs/hybrid_fitness_store.xml")
    val backup = File(file.path + ".bak")
    val source = if (backup.exists()) backup else file
    if (!source.exists()) return
    source.inputStream().use { input ->
        val parser = Xml.newPullParser()
        parser.setInput(input, "UTF-8")
        require(parser.nextTag() == XmlPullParser.START_TAG && parser.name == "map")
        val names = mutableSetOf<String>()
        var entryType: String? = null
        var closed = false
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.END_TAG && parser.depth == 1) closed = true
            if (parser.eventType == XmlPullParser.START_TAG) {
                require(!closed)
                if (parser.depth > 2) {
                    require(parser.depth == 3 && entryType == "set" && parser.name == "string")
                    continue
                }
                require(parser.depth == 2)
                entryType = parser.name
                require(names.add(requireNotNull(parser.getAttributeValue(null, "name"))))
                val value = parser.getAttributeValue(null, "value")
                when (parser.name) {
                    "int" -> requireNotNull(value).toInt()
                    "long" -> requireNotNull(value).toLong()
                    "float" -> require(requireNotNull(value).toFloat().isFinite())
                    "boolean" -> require(value == "true" || value == "false")
                    "string", "set" -> Unit
                    else -> error("Unsupported preference entry")
                }
            }
            if (parser.eventType == XmlPullParser.TEXT && !parser.isWhitespace) {
                require((parser.depth == 2 && entryType == "string") || (parser.depth == 3 && entryType == "set"))
            }
        }
        require(closed) { "Truncated preference XML" }
    }
}

