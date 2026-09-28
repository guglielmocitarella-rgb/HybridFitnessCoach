package com.hybridfitnesscoach.app

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class AppStoreRecoveryTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val prefs get() = context.getSharedPreferences("hybrid_fitness_store", Context.MODE_PRIVATE)

    @Before fun clearStore() { prefs.edit().clear().commit() }

    private fun session(timestamp: Long) = SessionRecord(
        timestamp, "Casa", 1, 2, listOf("Push-up"),
        structuredExercises = listOf(WorkoutExerciseState("Push-up", sets = listOf(
            WorkoutSetState(0, targetReps = 10, actualReps = 7, actualLoadKg = 5.0, completed = true),
            WorkoutSetState(1, targetReps = 10)
        ), status = WorkoutExerciseStatus.PARTIAL)), plannedWorkoutId = "planned-1"
    )

    @Test fun deletingLinkedSessionPersistsHistoryAndPlanTogether() {
        val store = AppStore(context)
        val completed = PlannedWorkout("planned-1", 10, 0, 0, listOf(exercises.first()),
            PlannedWorkoutState.COMPLETED, 10, actualExecutionEpochDay = 10)
        store.saveTrainingPlan(TrainingPlan("p", 10, 1, 1, listOf(completed)))
        store.saveSessions(listOf(session(1)))

        store.deleteSession(1)

        assertTrue(AppStore(context).loadSessions().isEmpty())
        val restored = AppStore(context).loadTrainingPlan()!!.workouts.single()
        assertEquals(PlannedWorkoutState.PLANNED, restored.state)
        assertNull(restored.actualExecutionEpochDay)
    }

    @Test fun oneBadHistoryRecordDoesNotHideValidCompletedSets() {
        val store = AppStore(context)
        store.saveSessions(listOf(session(1), session(2)))
        val records = JSONArray(prefs.getString("sessions", null)).put(JSONObject().put("timestamp", 3))
        prefs.edit().putString("sessions", records.toString()).commit()
        assertEquals(listOf(session(2), session(1)), store.loadSessions())
    }

    @Test fun addingSessionCannotOverwriteTruncatedHistory() {
        val raw = "[{\"timestamp\":1,"
        prefs.edit().putString("sessions", raw).commit()
        val store = AppStore(context)
        runCatching { store.addSession(session(2)) }
        assertEquals(raw, prefs.getString("sessions", null))
    }

    @Test fun missingEquipmentMigrationFlagDoesNotOverwriteExistingProfiles() {
        val store = AppStore(context)
        val custom = listOf(EquipmentProfile("My equipment", "X", listOf("Ab Wheel")))
        store.saveCustomProfiles(custom)
        assertEquals(custom, store.loadCustomProfiles())
    }

    @Test fun newInstallationAndCurrentHistoryRoundTrip() {
        val store = AppStore(context)
        assertTrue(store.loadSessions().isEmpty())
        assertNull(store.loadActiveWorkout())
        assertNull(store.loadTrainingPlan())
        assertTrue(store.issues.isEmpty())
        store.saveSessions(listOf(session(42)))
        assertEquals(listOf(session(42)), AppStore(context).loadSessions())
    }

    @Test fun legacyV09SessionWithoutOptionalFieldsStillLoads() {
        prefs.edit().putString("sessions", """[{"timestamp":12,"profile":"Casa","done":2,"total":3}]""").commit()
        val store = AppStore(context)
        val expected = SessionRecord(12, "Casa", 2, 3)
        assertEquals(listOf(expected), store.loadSessions())
        store.saveSessions(store.loadSessions())
        assertEquals(listOf(expected), AppStore(context).loadSessions())
    }

    @Test fun unsupportedPlanVersionCannotBeLoadedOrOverwritten() {
        val raw = """{"schemaVersion":999,"id":"future","startEpochDay":1,"weeks":1,"daysPerWeek":1,"workouts":[]}"""
        prefs.edit().putString("training_plan_v091", raw).commit()
        val store = AppStore(context)
        assertNull(store.loadTrainingPlan())
        runCatching { store.saveTrainingPlan(TrainingPlan("new", 1, 1, 1, emptyList())) }
        assertEquals(raw, prefs.getString("training_plan_v091", null))
    }

    @Test fun malformedHistoryDoesNotPreventReadingAthleteAndProgression() {
        prefs.edit().putString("athlete_name", "Ada").putString("skill_node_front_lever", "front_tuck")
            .putString("sessions", "not json").commit()
        val store = AppStore(context)
        assertEquals("Ada", store.loadAthlete().name)
        assertEquals("front_tuck", store.loadSkillNode("Front lever"))
        assertTrue(store.loadSessions().isEmpty())
        runCatching { store.saveSessions(emptyList()) }
        assertEquals("not json", prefs.getString("sessions", null))
    }

    @Test fun legacyPlanMigrationPreservesCompletedStateAndDatesAcrossLoads() {
        val name = exercises.first().name
        val workout = JSONObject().put("id", "w1").put("epochDay", 15).put("originalEpochDay", 10)
            .put("actualExecutionEpochDay", 16).put("weekIndex", 0).put("sequenceInWeek", 0)
            .put("state", "COMPLETED").put("exerciseNames", JSONArray().put(name))
        val root = JSONObject().put("id", "p1").put("startEpochDay", 10).put("weeks", 1)
            .put("daysPerWeek", 1).put("workouts", JSONArray().put(workout))
        prefs.edit().putString("training_plan_v091", root.toString()).commit()
        val first = AppStore(context).loadTrainingPlan()!!
        assertEquals(PlannedWorkoutState.COMPLETED, first.workouts.single().state)
        assertEquals(10L, first.workouts.single().originalEpochDay)
        assertEquals(16L, first.workouts.single().actualExecutionEpochDay)
        assertEquals(2, JSONObject(prefs.getString("training_plan_v091", null)!!).getInt("schemaVersion"))
        repeat(3) {
            val store = AppStore(context)
            assertEquals(first, store.loadTrainingPlan())
            store.saveTrainingPlan(first)
        }
    }

    @Test fun badSetDoesNotHideOtherActualSetsOrOtherSessions() {
        val store = AppStore(context)
        store.saveSessions(listOf(session(1), session(2)))
        val records = JSONArray(prefs.getString("sessions", null))
        records.getJSONObject(0).getJSONArray("structuredExercises").getJSONObject(0)
            .getJSONArray("sets").getJSONObject(1).put("actualReps", "broken")
        prefs.edit().putString("sessions", records.toString()).commit()
        val loaded = store.loadSessions()
        assertEquals(listOf(2L, 1L), loaded.map { it.timestamp })
        assertEquals(listOf(7), loaded.first().structuredExercises.single().sets.map { it.actualReps })
        assertEquals(session(1), loaded.last())
    }

    @Test fun unsupportedActiveWorkoutVersionRemainsIntact() {
        val raw = """{"schemaVersion":999,"id":"w","startedAtEpochMs":1,"exercises":[]}"""
        prefs.edit().putString("active_workout_v1", raw).commit()
        val store = AppStore(context)
        assertNull(store.loadActiveWorkout())
        runCatching { store.clearActiveWorkout() }
        assertEquals(raw, prefs.getString("active_workout_v1", null))
    }

    @Test fun malformedAthleteFieldDoesNotHideValidProfileFields() {
        prefs.edit().putString("athlete_name", "Ada").putString("athlete_days", "broken").commit()
        assertEquals("Ada", AppStore(context).loadAthlete().name)
        assertEquals("broken", prefs.getString("athlete_days", null))
    }

    @Test fun wrongTypedOptionalHistoryArrayCannotSilentlyBecomeEmpty() {
        val store = AppStore(context)
        store.saveSessions(listOf(session(1)))
        val records = JSONArray(prefs.getString("sessions", null))
        records.getJSONObject(0).put("structuredExercises", "broken")
        val raw = records.toString()
        prefs.edit().putString("sessions", raw).commit()
        runCatching { store.saveSessions(store.loadSessions()) }
        assertEquals(raw, prefs.getString("sessions", null))
    }

    @Test fun recoveryModeBlocksEveryMutationAcrossStoreRecreation() {
        val store = AppStore(context)
        store.saveSessions(listOf(session(1)))
        val corrupt = JSONArray(prefs.getString("sessions", null)).put(JSONObject())
        prefs.edit().putString("sessions", corrupt.toString()).commit()
        val before = prefs.all.toMap()
        repeat(2) {
            val recovered = AppStore(context)
            assertTrue(recovered.issues.isNotEmpty())
            recovered.saveAthlete(AthleteProfile(name = "Replacement"))
            recovered.saveActiveProfileName("Replacement")
            recovered.saveCustomProfiles(emptyList())
            recovered.saveSkillNode("Front lever", "front_full")
            recovered.saveActiveWorkout(WorkoutSessionState("new", 1, "Casa", emptyList()))
            recovered.clearActiveWorkout()
            recovered.saveTrainingPlan(TrainingPlan("new", 1, 1, 1, emptyList()))
            recovered.saveSessions(emptyList())
            recovered.addSession(session(2))
            recovered.deleteSession(1)
            assertEquals(before, prefs.all)
            assertEquals(listOf(session(1)), recovered.loadSessions())
        }
    }

    @Test fun oneBadPlannedWorkoutPreservesCompletedWorkoutAndOriginalBytes() {
        val completed = PlannedWorkout("w1", 15, 0, 0, listOf(exercises.first()),
            PlannedWorkoutState.COMPLETED, 10, actualExecutionEpochDay = 16)
        val store = AppStore(context)
        store.saveTrainingPlan(TrainingPlan("p", 10, 1, 1, listOf(completed)))
        val root = JSONObject(prefs.getString("training_plan_v091", null)!!)
        root.getJSONArray("workouts").put(JSONObject().put("id", "broken"))
        val raw = root.toString()
        prefs.edit().putString("training_plan_v091", raw).commit()
        val recovered = AppStore(context)
        assertEquals(listOf(completed), recovered.loadTrainingPlan()!!.workouts)
        assertTrue(recovered.issues.isNotEmpty())
        recovered.saveTrainingPlan(recovered.loadTrainingPlan()!!)
        assertEquals(raw, prefs.getString("training_plan_v091", null))
    }

    @Test fun legacyPlanWithMissingExerciseIsNeverMigrated() {
        val root = JSONObject().put("id", "p").put("startEpochDay", 1).put("weeks", 1).put("daysPerWeek", 1)
            .put("workouts", JSONArray().put(JSONObject().put("id", "w").put("epochDay", 1)
                .put("weekIndex", 0).put("sequenceInWeek", 0).put("exerciseNames", JSONArray().put("Missing legacy exercise"))))
        val raw = root.toString()
        prefs.edit().putString("training_plan_v091", raw).commit()
        val store = AppStore(context)
        assertNotNull(store.loadTrainingPlan())
        assertTrue(store.issues.isNotEmpty())
        assertEquals(raw, prefs.getString("training_plan_v091", null))
    }

    @Test fun activeWorkoutReloadPreservesRecoveryOverridesAndRunningRest() {
        val exercise = WorkoutExerciseState("A", setRestSec = 75, exerciseRestSec = 150,
            sets = listOf(WorkoutSetState(0, targetReps = 8, completed = true, restUsedSec = 70)))
        val active = WorkoutSessionState("w", 1000, "P", listOf(exercise),
            restEndsAtEpochMs = 20_000, restKind = "Serie")
        val store = AppStore(context)
        store.saveActiveWorkout(active)

        val restored = AppStore(context).loadActiveWorkout()!!
        assertEquals(75, restored.exercises.single().setRestSec)
        assertEquals(150, restored.exercises.single().exerciseRestSec)
        assertEquals(70, restored.exercises.single().sets.single().restUsedSec)
        assertEquals(20_000L, restored.restEndsAtEpochMs)
        assertEquals("Serie", restored.restKind)
    }

    @Test fun currentPlanAndActiveWorkoutRoundTripPreserveEveryField() {
        val workout = PlannedWorkout("w1", 15, 1, 2, listOf(exercises.first().copy(targetValue = 7.0)),
            PlannedWorkoutState.PARTIAL, 10, "Adapted", 16)
        val plan = TrainingPlan("p", 10, 52, 3, listOf(workout))
        val active = WorkoutSessionState("active", 123, "Casa", session(1).structuredExercises,
            0, 1000, 234, 567, 8, 890, "custom", "set", listOf("Changed"), "w1")
        val store = AppStore(context)
        store.saveTrainingPlan(plan)
        store.saveActiveWorkout(active)
        val reloaded = AppStore(context)
        assertEquals(plan, reloaded.loadTrainingPlan())
        assertEquals(active, reloaded.loadActiveWorkout())
        assertTrue(reloaded.issues.isEmpty())
    }

    @Test fun malformedPreferenceXmlIsNotANewInstallation() {
        val file = File(context.applicationInfo.dataDir, "shared_prefs/hybrid_fitness_store.xml")
        file.parentFile!!.mkdirs()
        val raw = "<map><string name=\"sessions\">truncated"
        file.writeText(raw)
        try {
            val store = AppStore(context)
            assertTrue(store.issues.contains("preference_file"))
            store.saveSessions(listOf(session(1)))
            assertEquals(raw, file.readText())
        } finally { file.delete() }
    }

    @Test fun invalidPreferenceXmlValueIsNotANewInstallation() {
        val file = File(context.applicationInfo.dataDir, "shared_prefs/hybrid_fitness_store.xml")
        file.parentFile!!.mkdirs()
        val raw = "<map><int name=\"athlete_days\" value=\"broken\"/></map>"
        file.writeText(raw)
        try {
            val store = AppStore(context)
            assertTrue(store.issues.contains("preference_file"))
            store.saveAthlete(AthleteProfile())
            assertEquals(raw, file.readText())
        } finally { file.delete() }
    }

    @Test fun malformedEquipmentRecoversValidProfileAndProtectsOriginal() {
        val raw = """[{"name":"Home","equipment":["Ab Wheel","Foam Roller"]},{"name":"broken","equipment":false}]"""
        prefs.edit().putString("custom_profiles", raw).commit()
        val store = AppStore(context)
        assertEquals(listOf("Home"), store.loadCustomProfiles().map { it.name })
        assertTrue(store.issues.isNotEmpty())
        store.saveCustomProfiles(store.loadCustomProfiles())
        assertEquals(raw, prefs.getString("custom_profiles", null))
    }

    @Test fun malformedNestedXmlIsNotANewInstallation() {
        val file = File(context.applicationInfo.dataDir, "shared_prefs/hybrid_fitness_store.xml")
        file.parentFile!!.mkdirs()
        val raw = "<map><string name=\"sessions\"><invalid/></string></map>"
        file.writeText(raw)
        try {
            val store = AppStore(context)
            assertTrue(store.issues.contains("preference_file"))
            store.saveSessions(listOf(session(1)))
            assertEquals(raw, file.readText())
        } finally { file.delete() }
    }
}
