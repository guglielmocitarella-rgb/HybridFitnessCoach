package com.hybridfitnesscoach.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Xml
import androidx.compose.runtime.mutableStateListOf
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentDimension
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentLevel
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentObservation
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentProvenance
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentRevision
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentSnapshot
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentTarget
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentVerification
import java.io.File
import java.util.UUID
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
    .put("completed", completed).put("restUsedSec", restUsedSec).put("actualRpe",actualRpe).put("actualStartedAt",actualStartedAtEpochMs).put("actualFinishedAt",actualFinishedAtEpochMs)
private fun JSONObject.optNullableInt(key:String):Int? = if(has(key)&&!isNull(key)) storedInt(key) else null
private fun JSONObject.optNullableLong(key:String):Long? = if(has(key)&&!isNull(key)) storedLong(key) else null
private fun JSONObject.optNullableDouble(key:String):Double? = if(has(key)&&!isNull(key)) strictNumber(key).toDouble() else null
private fun workoutSetFromJson(o:JSONObject)=WorkoutSetState(
    o.storedInt("index"),o.optNullableInt("targetReps"),o.optNullableInt("targetSeconds"),
    o.optNullableDouble("targetDistanceMeters"),o.optNullableDouble("targetLoadKg"),o.optNullableDouble("targetAssistanceKg"),
    o.optNullableInt("actualReps"),o.optNullableInt("actualSeconds"),o.optNullableDouble("actualDistanceMeters"),
    o.optNullableDouble("actualLoadKg"),o.optNullableDouble("actualAssistanceKg"),o.storedBoolean("completed"),o.optNullableInt("restUsedSec"),o.optNullableInt("actualRpe"),o.optNullableLong("actualStartedAt"),o.optNullableLong("actualFinishedAt"))
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
internal fun WorkoutSessionState.toJson():JSONObject { val a=JSONArray();exercises.forEach{a.put(it.toJson())};return JSONObject()
    .put("schemaVersion",2).put("id",id).put("startedAtEpochMs",startedAtEpochMs).put("profileName",profileName)
    .put("exercises",a).put("currentExerciseIndex",currentExerciseIndex).put("elapsedBeforePauseMs",elapsedBeforePauseMs)
    .put("pausedAtEpochMs",pausedAtEpochMs).put("restEndsAtEpochMs",restEndsAtEpochMs).put("workoutRpe",workoutRpe).put("finishedAtEpochMs",finishedAtEpochMs)
    .put("runnerKind",runnerKind).put("restKind",restKind).put("modifications",JSONArray().apply{modifications.forEach{put(it)}}).put("plannedWorkoutId",plannedWorkoutId).put("pausedRestRemainingSec",pausedRestRemainingSec).put("trainingLocationId",trainingLocationId).put("workoutOrigin",workoutOrigin).put("restStartedAt",restStartedAtEpochMs)
    .put("timedSet",timedSet?.let{JSONObject().put("exercise",it.exerciseIndex).put("set",it.setIndex).put("starts",it.startsAtEpochMs).put("target",it.targetSeconds).put("load",it.loadKg).put("assistance",it.assistanceKg)}) }
internal fun workoutSessionFromJson(o:JSONObject, recovery:StoreRecovery):WorkoutSessionState { o.requireVersion(2); val a=o.getJSONArray("exercises");return WorkoutSessionState(
    o.getString("id"),o.getLong("startedAtEpochMs"),o.storedString("profileName",""),
    recovery.records(a,"active_workout_v1.exercises") { exercise, path -> workoutExerciseFromJson(exercise,recovery,path) },o.storedInt("currentExerciseIndex"),
    o.storedLong("elapsedBeforePauseMs",0L),o.optNullableLong("pausedAtEpochMs"),o.optNullableLong("restEndsAtEpochMs"),
    o.optNullableInt("workoutRpe"),o.optNullableLong("finishedAtEpochMs"),o.storedString("runnerKind","automatic"),if(o.has("restKind")&&!o.isNull("restKind"))o.getString("restKind") else null,
    (o.storedArray("modifications")?:JSONArray()).let{a->(0 until a.length()).map{a.getString(it)}},o.storedString("plannedWorkoutId").takeIf{it.isNotBlank()},o.optNullableInt("pausedRestRemainingSec"),o.storedString("trainingLocationId").takeIf{it.isNotBlank()},o.storedString("workoutOrigin","ADAPTIVE_EXTRA"),
    if(o.isNull("timedSet"))null else o.getJSONObject("timedSet").let{TimedSetState(it.getInt("exercise"),it.getInt("set"),it.getLong("starts"),it.getInt("target"),it.optNullableDouble("load"),it.optNullableDouble("assistance"))},o.optNullableLong("restStartedAt")) }

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
    private fun long(key: String, fallback: Long = 0L): Long = field(key, fallback) { it.getLong(key, fallback) }
    private fun float(key: String): Float = field(key, 0f) { it.getFloat(key, 0f).also { v -> require(v.isFinite()) } }

    private fun validateStore() {
        loadAthlete()
        loadOnboardingDraft()
        loadActiveTrainingLocationId()
        loadLastUsedTrainingLocationId()
        loadTrainingLocations()
        loadSessions()
        loadActiveWorkout()
        loadTrainingPlans()
        loadEnabledTrainingPlanIds()
        loadProgressionNodes()
        string("weight_history")
        loadV3AssessmentHistory()
    }

    private fun write(edit: SharedPreferences.Editor.() -> Unit): Boolean {
        // Recheck even when the caller never loaded the area it is about to replace.
        validateStore()
        if (issues.isNotEmpty() || prefs == null) return false
        val committed = recovery.read("storage_write", false) { prefs.edit().apply(edit).commit() }
        if (!committed) recovery.problem("storage_write")
        return committed
    }

    internal fun loadOnboardingDraft():OnboardingDraft {
        val raw=string("onboarding_draft") ?: return OnboardingDraft()
        return recovery.read("onboarding_draft",OnboardingDraft()) {
            val o=JSONObject(raw)
            val days=o.getJSONArray("days")
            OnboardingDraft(o.getInt("step"),(0 until days.length()).map{days.getInt(it)}.toSet(),o.getString("planId"),o.getInt("weeks")).validated()
        }
    }
    internal fun saveOnboardingDraft(draft:OnboardingDraft):Boolean {
        val valid=draft.validated()
        return write {putString("onboarding_draft",JSONObject().put("step",valid.step).put("days",JSONArray(valid.trainingDays.sorted())).put("planId",valid.planId).put("weeks",valid.weeks).toString())}
    }

    fun loadAthlete(): AthleteProfile =
        AthleteProfile(
            name = string("athlete_name", "") ?: "",
            goal = string("athlete_current_goal", "Forza + skill") ?: "Forza + skill",
            daysPerWeek = int("athlete_current_days",3).takeIf{it in 1..7} ?: 3,
            prioritySkills = recovery.read("athlete_current_skills",emptyList()) { val a=JSONArray(string("athlete_current_skills","[]"));(0 until a.length()).map{a.getString(it)} },
            age = int("athlete_age"),
            heightCm = float("athlete_height"),
            weightKg = float("athlete_weight"),
            sex = string("athlete_sex", "Non specificato") ?: "Non specificato",
            assessedTrainingExperience = int("athlete_assessed_experience").takeIf{it in 1..3},
            assessedPushCapacity = int("athlete_assessed_push").takeIf{it in 1..3},
            assessedPullCapacity = int("athlete_assessed_pull").takeIf{it in 1..3},
            assessedHorizontalPullCapacity = int("capability_horizontalpull").takeIf{it in 1..3},
            assessedVerticalPushCapacity = int("capability_verticalpush").takeIf{it in 1..3},
            assessedSquatCapacity = int("capability_squat").takeIf{it in 1..3},
            assessedHingeCapacity = int("capability_hinge").takeIf{it in 1..3},
            assessedCoreCapacity = int("capability_core").takeIf{it in 1..3},
            assessedIsometricCapacity = int("capability_isometric").takeIf{it in 1..3},
            assessedSkillCapacity = int("capability_skill").takeIf{it in 1..3}
        )

    fun saveAthlete(p: AthleteProfile):Boolean {
        val history = string("weight_history", "") ?: ""
        val previousWeight=float("athlete_weight")
        val profileRevision = loadAthleteRevision()
        return write {
            putString("athlete_name", p.name)
                .putString("athlete_current_goal",p.goal).putInt("athlete_current_days",p.daysPerWeek.coerceIn(1,7)).putString("athlete_current_skills",JSONArray(p.prioritySkills).toString())
                .putInt("athlete_age", p.age).putFloat("athlete_height", p.heightCm)
                .putFloat("athlete_weight", p.weightKg).putString("athlete_sex", p.sex)
                .putLong("athlete_profile_revision", if (profileRevision == Long.MAX_VALUE) 1L else profileRevision + 1L)
                .apply {
                    p.assessedTrainingExperience?.let{putInt("athlete_assessed_experience",it)} ?: remove("athlete_assessed_experience")
                    p.assessedPushCapacity?.let{putInt("athlete_assessed_push",it)} ?: remove("athlete_assessed_push")
                    p.assessedPullCapacity?.let{putInt("athlete_assessed_pull",it)} ?: remove("athlete_assessed_pull")
                    p.assessedHorizontalPullCapacity?.let{putInt("capability_horizontalpull",it)} ?: remove("capability_horizontalpull")
                    p.assessedVerticalPushCapacity?.let{putInt("capability_verticalpush",it)} ?: remove("capability_verticalpush")
                    p.assessedSquatCapacity?.let{putInt("capability_squat",it)} ?: remove("capability_squat")
                    p.assessedHingeCapacity?.let{putInt("capability_hinge",it)} ?: remove("capability_hinge")
                    p.assessedCoreCapacity?.let{putInt("capability_core",it)} ?: remove("capability_core")
                    p.assessedIsometricCapacity?.let{putInt("capability_isometric",it)} ?: remove("capability_isometric")
                    p.assessedSkillCapacity?.let{putInt("capability_skill",it)} ?: remove("capability_skill")
                }
                .remove("athlete_goal").remove("athlete_days").remove("athlete_priority_skills")
            if (p.weightKg > 0 && (previousWeight!=p.weightKg || history.isBlank())) putString("weight_history", ("${System.currentTimeMillis()}:${p.weightKg};" + history).take(4000))
        }
    }

    fun loadAthleteRevision(): Long = long("athlete_profile_revision").coerceAtLeast(0L)

    /** Append a guided self-report or explicit unknown answer without touching legacy profile fields. */
    fun saveV3Assessment(
        target: V3AssessmentTarget,
        level: V3AssessmentLevel?,
        assessedAtEpochMs: Long?,
        recordedAtEpochMs: Long = System.currentTimeMillis()
    ): Boolean {
        val history = loadV3AssessmentHistory()
        val snapshot = V3AssessmentSnapshot.fromHistory(history, profileRevision = 0L)
        val current = snapshot.current(target)
        val id = UUID.randomUUID().toString()
        val row = if (level == null) {
            V3AssessmentRevision(
                id, target, null, V3AssessmentProvenance.UNKNOWN, V3AssessmentVerification.UNKNOWN,
                null, recordedAtEpochMs, null, null, null, null, current?.id
            )
        } else {
            require(assessedAtEpochMs != null) { "Known self-reported levels require an assessment date" }
            V3AssessmentRevision(
                id, target, level, V3AssessmentProvenance.SELF_REPORTED, V3AssessmentVerification.DECLARED,
                assessedAtEpochMs, recordedAtEpochMs, "guided-self-report", "1", "G1-RUBRIC-0.1-DRAFT",
                null, current?.id
            )
        }
        return appendV3AssessmentRevision(row)
    }

    /** Persists a validated direct or reproducible derived row with its provenance unchanged. */
    fun appendV3AssessmentRevision(row: V3AssessmentRevision): Boolean {
        val history = loadV3AssessmentHistory()
        val snapshot = V3AssessmentSnapshot.fromHistory(history, profileRevision = 0L)
        if (snapshot.hasConflict(row.target) || history.any { it.id == row.id }) return false
        if (row.supersedesId != snapshot.current(row.target)?.id) return false
        val updated = history + row
        V3AssessmentSnapshot.fromHistory(updated, profileRevision = 0L)
        val envelope = JSONObject().put("schemaVersion", 1).put("items", JSONArray().apply {
            updated.forEach { put(it.toJson()) }
        })
        return write { putString("v3_assessment_history", envelope.toString()) }
    }

    /** Append an invalidation event. The prior observed or derived row remains in history. */
    fun invalidateV3Assessment(target: V3AssessmentTarget, reason: String, recordedAtEpochMs: Long = System.currentTimeMillis()): Boolean {
        require(reason.isNotBlank())
        val history = loadV3AssessmentHistory()
        val snapshot = V3AssessmentSnapshot.fromHistory(history, profileRevision = 0L)
        val current = snapshot.current(target) ?: return false
        val row = V3AssessmentRevision(
            UUID.randomUUID().toString(), target, null, V3AssessmentProvenance.UNKNOWN,
            V3AssessmentVerification.INVALIDATED, null, recordedAtEpochMs, null, null, null, null,
            current.id, reason.trim()
        )
        return appendV3AssessmentRevision(row)
    }

    fun loadV3AssessmentHistory(): List<V3AssessmentRevision> {
        val raw = string("v3_assessment_history") ?: return emptyList()
        return recovery.read("v3_assessment_history", emptyList()) {
            val root = JSONObject(raw)
            root.requireVersion(1)
            val items = root.getJSONArray("items")
            val rows = recovery.records(items, "v3_assessment_history.items") { item, _ ->
                V3AssessmentRevision.fromJson(item)
            }
            V3AssessmentSnapshot.fromHistory(rows, profileRevision = 0L)
            rows
        }
    }

    fun loadV3AssessmentSnapshot(profileRevision: Long): V3AssessmentSnapshot =
        V3AssessmentSnapshot.fromHistory(loadV3AssessmentHistory(), profileRevision)

    internal fun loadActiveTrainingLocationId(): String = string("active_training_location", trainingLocations.first().id) ?: trainingLocations.first().id
    internal fun loadLastUsedTrainingLocationId():String? = string("last_used_training_location")
    internal fun saveActiveTrainingLocationId(id: String) = write { putString("active_training_location", id) }

    internal fun loadTrainingLocations(): List<TrainingLocation> {
        val raw = string("training_locations") ?: return trainingLocations
        return recovery.read("training_locations", trainingLocations) {
            recovery.records(JSONArray(raw), "training_locations") { o, _ ->
                val kind=TrainingLocationKind.entries.firstOrNull { it.stableId==o.storedString("kind", TrainingLocationKind.CUSTOM.stableId) } ?: TrainingLocationKind.CUSTOM
                val capabilities=o.planStringList("capabilities").mapNotNull { id -> EquipmentId.entries.firstOrNull { it.name==id } }.toSet()
                TrainingLocation(o.getString("id"),o.getString("name"),o.storedString("icon","📍"),kind,capabilities)
            }.filter { it.capabilities.isNotEmpty() }
        }
    }

    internal fun saveTrainingLocations(items: List<TrainingLocation>):Boolean {
        val array = JSONArray()
        items.forEach { location ->
            array.put(JSONObject()
                .put("id",location.id)
                .put("name",location.name)
                .put("icon",location.icon)
                .put("kind",location.kind.stableId)
                .put("capabilities",JSONArray(location.capabilities.map { it.name }.sorted())))
        }
        return write { putString("training_locations", array.toString()) }
    }

    internal fun startWorkout(state:WorkoutSessionState):Boolean {
        if(loadActiveWorkout()!=null)return false
        val location=state.trainingLocationId
        require(location!=null || state.workoutOrigin==WorkoutOrigin.SINGLE_EXERCISE.name)
        require(state.exercises.isNotEmpty())
        return write {
            putString("active_workout_v1",state.toJson().toString())
            if(location!=null){
                putString("last_used_training_location",location)
                putString("active_training_location",location)
            }
        }
    }
    fun saveActiveWorkout(state: WorkoutSessionState) = write { putString("active_workout_v1", state.toJson().toString()) }
    fun loadActiveWorkout(): WorkoutSessionState? {
        val raw = string("active_workout_v1") ?: return null
        return recovery.read("active_workout_v1", null) { workoutSessionFromJson(JSONObject(raw), recovery) }
    }
    fun clearActiveWorkout() = write { remove("active_workout_v1") }

    private fun intentJson(intent:PlannedSessionIntent):JSONObject = JSONObject()
        .put("goal",intent.goal).put("prioritySkills",JSONArray(intent.prioritySkills))
        .put("focusCategories",JSONArray(intent.focusCategories.map{it.name}.sorted()))
        .put("sequenceInWeek",intent.sequenceInWeek)

    private fun intentFromJson(o:JSONObject):PlannedSessionIntent = PlannedSessionIntent(
        o.storedString("goal","Fitness generale"),o.planStringList("prioritySkills"),
        o.planStringList("focusCategories").map{ExerciseCategory.valueOf(it)}.toSet(),
        o.storedInt("sequenceInWeek",0)
    ).validated()

    private fun trainingPlanJsonObject(plan: TrainingPlan): JSONObject {
        val workouts = JSONArray()
        plan.workouts.forEach { w ->
            val intent=requireNotNull(w.intent) { "Current plan schema requires intent for planned workout ${w.id}" }
            workouts.put(JSONObject().put("id", w.id).put("epochDay", w.epochDay).put("weekIndex", w.weekIndex)
                .put("sequenceInWeek", w.sequenceInWeek)
                .put("state", w.state.name).put("originalEpochDay", w.originalEpochDay)
                .put("adaptationReason", w.adaptationReason).put("actualExecutionEpochDay", w.actualExecutionEpochDay)
                .put("intent",intentJson(intent)))
        }
        return JSONObject().put("id", plan.id).put("startEpochDay", plan.startEpochDay)
            .put("weeks", plan.weeks).put("daysPerWeek", plan.daysPerWeek)
            .put("goal", plan.goal).put("trainingDays", JSONArray(plan.trainingDays.sorted()))
            .put("prioritySkills", JSONArray(plan.prioritySkills)).put("workouts", workouts)
    }

    private fun trainingPlanFromJson(root:JSONObject,path:String):TrainingPlan {
        val workouts = recovery.records(root.getJSONArray("workouts"), "$path.workouts") { o, _ ->
            PlannedWorkout(
                o.getString("id"), o.getLong("epochDay"), o.getInt("weekIndex"), o.getInt("sequenceInWeek"), emptyList(),
                PlannedWorkoutState.valueOf(o.storedString("state", "PLANNED")),
                o.storedLong("originalEpochDay", o.getLong("epochDay")),
                o.storedString("adaptationReason").takeIf { it.isNotBlank() },
                o.optNullableLong("actualExecutionEpochDay"),
                intentFromJson(o.getJSONObject("intent"))
            )
        }
        val trainingDaysArray = root.getJSONArray("trainingDays")
        return TrainingPlan(
            root.getString("id"), root.getLong("startEpochDay"), root.getInt("weeks"), root.getInt("daysPerWeek"), workouts,
            normalizePlanGoal(root.getString("goal")),
            (0 until trainingDaysArray.length()).map { trainingDaysArray.getInt(it) }.toSet(),
            root.planStringList("prioritySkills")
        )
    }

    fun loadTrainingPlans():List<TrainingPlan> {
        val raw=string("training_plans_v095") ?: return emptyList()
        return recovery.read("training_plans_v095", emptyList()) {
            val root=JSONObject(raw);root.requireVersion(1)
            recovery.records(root.getJSONArray("plans"),"training_plans_v095.plans"){o,path->trainingPlanFromJson(o,path)}
        }
    }

    fun saveTrainingPlans(plans:List<TrainingPlan>):Boolean {
        require(plans.map{it.id}.distinct().size==plans.size) { "Plan ids must be unique" }
        val root=JSONObject().put("schemaVersion",1).put("plans",JSONArray().apply{plans.forEach{put(trainingPlanJsonObject(it))}})
        return write { putString("training_plans_v095",root.toString()) }
    }

    fun loadEnabledTrainingPlanIds():Set<String> {
        val raw=string("enabled_training_plan_ids_v095") ?: return emptySet()
        return recovery.read("enabled_training_plan_ids_v095", emptySet()) {
            val a=JSONArray(raw);(0 until a.length()).map{a.getString(it)}.toSet()
        }
    }

    fun saveEnabledTrainingPlanIds(ids:Set<String>):Boolean {
        val existing=loadTrainingPlans().map{it.id}.toSet()
        require(ids.all{it in existing}) { "Enabled plan must exist" }
        return write { putString("enabled_training_plan_ids_v095",JSONArray(ids.sorted()).toString()) }
    }

    fun loadEnabledTrainingPlans():List<TrainingPlan> {
        val enabled=loadEnabledTrainingPlanIds()
        return loadTrainingPlans().filter{it.id in enabled}
    }

    fun saveTrainingPlan(plan:TrainingPlan):Boolean {
        val plans=loadTrainingPlans().toMutableList()
        val index=plans.indexOfFirst{it.id==plan.id}
        if(index>=0)plans[index]=plan else plans.add(plan)
        return saveTrainingPlans(plans)
    }

    fun setTrainingPlanEnabled(id:String,enabled:Boolean):Boolean {
        if(loadTrainingPlans().none{it.id==id})return false
        val ids=loadEnabledTrainingPlanIds().toMutableSet()
        if(enabled)ids.add(id) else ids.remove(id)
        return saveEnabledTrainingPlanIds(ids)
    }

    fun deleteTrainingPlan(id:String):Boolean {
        val remaining=loadTrainingPlans().filterNot{it.id==id}
        if(!saveTrainingPlans(remaining))return false
        val enabled=loadEnabledTrainingPlanIds()-id
        return saveEnabledTrainingPlanIds(enabled)
    }


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
                    o.storedString("plannedWorkoutId").takeIf { it.isNotBlank() },
                    o.storedString("trainingLocationId").takeIf{it.isNotBlank()},o.storedString("workoutOrigin","ADAPTIVE_EXTRA"))
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
                .put("modifications", JSONArray(s.modifications)).put("plannedWorkoutId", s.plannedWorkoutId)
                .put("trainingLocationId",s.trainingLocationId).put("workoutOrigin",s.workoutOrigin))
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
        val plans=loadTrainingPlans()
        val executionDay = java.time.Instant.ofEpochMilli(s.timestamp)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()
        val owner=plans.firstOrNull{plan->plan.workouts.any{it.id==s.plannedWorkoutId}}
        val updatedPlan=owner?.let{PlannedWorkoutReconciler.complete(it,s,executionDay,0).plan}
        val updatedPlans=if(updatedPlan!=null && updatedPlan!=owner) plans.map{if(it.id==updatedPlan.id)updatedPlan else it} else plans
        val plansRoot=JSONObject().put("schemaVersion",1).put("plans",JSONArray().apply{updatedPlans.forEach{put(trainingPlanJsonObject(it))}})
        return write {
            putString("sessions", sessionsJson(listOf(s) + history))
            if(updatedPlans!=plans)putString("training_plans_v095",plansRoot.toString())
            remove("active_workout_v1")
        }
    }

    fun addSession(s: SessionRecord) = saveSessions(listOf(s) + loadSessions())

    fun completePlannedWorkoutForSession(s: SessionRecord, minimumGapDays: Int = 0): ScheduleUpdate? {
        val plan = loadTrainingPlans().firstOrNull{candidate->candidate.workouts.any{it.id==s.plannedWorkoutId}} ?: return null
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
        val plans=loadTrainingPlans()
        val plan=plans.firstOrNull{candidate->candidate.workouts.any{it.id==removed.plannedWorkoutId}}
        val restored = plan?.let { PlannedWorkoutReconciler.afterDeletion(it, removed, remaining) }
        val updatedPlans=if(restored!=null && restored!=plan) plans.map{if(it.id==restored.id)restored else it} else plans
        val plansRoot=JSONObject().put("schemaVersion",1).put("plans",JSONArray().apply{updatedPlans.forEach{put(trainingPlanJsonObject(it))}})
        write {
            putString("sessions", sessionsJson(remaining))
            if(updatedPlans!=plans)putString("training_plans_v095",plansRoot.toString())
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
