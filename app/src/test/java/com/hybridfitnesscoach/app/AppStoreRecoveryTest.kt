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

    @Test fun rc2RescheduleAndPartialDeletionKeepSummaryConsistentAfterReload() {
        val store = AppStore(context)
        val original = TrainingPlanEngine.scheduleImmediately("rc2", 1000, 2, 3) { _, _ -> listOf(exercises.first()) }
        val moved = TrainingPlanEngine.reschedule(original, original.workouts.first().id, 1008)
        store.saveTrainingPlan(moved)
        assertEquals(moved, AppStore(context).loadTrainingPlan())
        val record = session(1).copy(plannedWorkoutId = moved.workouts.first().id)
        store.saveSessions(listOf(record))
        store.saveTrainingPlan(PlannedWorkoutReconciler.complete(moved, record, 1008, 0).plan)
        store.deleteSession(1)
        val reloaded = AppStore(context).loadTrainingPlan()!!
        assertEquals(6, coachingContext(AppStore(context).loadSessions(), null, reloaded, 1009).upcomingSessions)
        assertEquals(moved.workouts.map { it.originalEpochDay }, reloaded.workouts.map { it.originalEpochDay })
        val restored = TrainingPlanEngine.reschedule(reloaded, reloaded.workouts.first().id, 1000)
        store.saveTrainingPlan(restored)
        assertEquals(1000L, AppStore(context).loadTrainingPlan()!!.workouts.first().epochDay)
        assertTrue(restored.workouts.zipWithNext().all { (a,b) -> a.epochDay < b.epochDay })
    }

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


    @Test fun deletingOneOfDuplicateLinkedSessionsKeepsPlanCompletedAcrossReload() {
        val store = AppStore(context)
        val completed = PlannedWorkout("planned-1", 10, 0, 0, listOf(exercises.first()),
            PlannedWorkoutState.COMPLETED, 10, actualExecutionEpochDay = 10)
        store.saveTrainingPlan(TrainingPlan("p", 10, 1, 1, listOf(completed)))
        store.saveSessions(listOf(session(1), session(2)))

        store.deleteSession(1)

        val reloaded = AppStore(context)
        assertEquals(listOf(2L), reloaded.loadSessions().map { it.timestamp })
        val retained = reloaded.loadTrainingPlan()!!.workouts.single()
        assertEquals(PlannedWorkoutState.COMPLETED, retained.state)
        assertEquals(10L, retained.actualExecutionEpochDay)
    }

    @Test fun deletingUnlinkedSessionCannotChangeCompletedPlan() {
        val store = AppStore(context)
        val completed = PlannedWorkout("planned-1", 10, 0, 0, listOf(exercises.first()),
            PlannedWorkoutState.COMPLETED, 10, actualExecutionEpochDay = 10)
        val plan = TrainingPlan("p", 10, 1, 1, listOf(completed))
        store.saveTrainingPlan(plan)
        store.saveSessions(listOf(session(1).copy(plannedWorkoutId = null)))

        store.deleteSession(1)

        val reloaded = AppStore(context)
        assertTrue(reloaded.loadSessions().isEmpty())
        assertEquals(plan, reloaded.loadTrainingPlan())
    }

    @Test fun activeWorkoutRoundTripPreservesTrainingLocation() {
        val store = AppStore(context)
        val active = WorkoutSessionState("location", 1000, "Casa", listOf(WorkoutExerciseState("Push-up")), trainingLocationId = "builtin:home")
        store.saveActiveWorkout(active)
        assertEquals("builtin:home", AppStore(context).loadActiveWorkout()?.trainingLocationId)
    }

    @Test fun finishingWorkoutAtomicallySavesHistoryPlanAndClearsActiveState() {
        val store = AppStore(context)
        val planned = PlannedWorkout("planned-1", 10, 0, 0, listOf(exercises.first()))
        store.saveTrainingPlan(TrainingPlan("p", 10, 1, 1, listOf(planned)))
        val active = WorkoutSessionState("active", 1000, "Casa",
            listOf(WorkoutExerciseState("Push-up")), plannedWorkoutId = planned.id)
        store.saveActiveWorkout(active)

        assertTrue(store.finishWorkout(session(1)))

        val reloaded = AppStore(context)
        assertEquals(listOf(session(1)), reloaded.loadSessions())
        assertNull(reloaded.loadActiveWorkout())
        assertEquals(PlannedWorkoutState.PARTIAL, reloaded.loadTrainingPlan()!!.workouts.single().state)
    }

    @Test fun failedFinishPreservesActiveWorkoutAndOriginalHistory() {
        val store = AppStore(context)
        val active = WorkoutSessionState("active", 1000, "Casa", listOf(WorkoutExerciseState("Push-up")))
        store.saveActiveWorkout(active)
        val brokenHistory = "[{\"timestamp\":1,"
        prefs.edit().putString("sessions", brokenHistory).commit()
        val reloaded = AppStore(context)

        assertFalse(reloaded.finishWorkout(session(2)))

        assertEquals(brokenHistory, prefs.getString("sessions", null))
        assertEquals(active, AppStore(context).loadActiveWorkout())
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

    @Test fun currentEquipmentProfilesRoundTripWithoutMigrationState() {
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

    @Test fun assessedAthleteCapabilitiesRoundTrip() {
        val store = AppStore(context)
        store.saveAthlete(AthleteProfile(
            name = "Capability",
            level = "Intermedio",
            assessedTrainingExperience = 3,
            assessedPushCapacity = 1,
            assessedPullCapacity = 2
        ))
        val loaded = AppStore(context).loadAthlete()
        assertEquals(3, loaded.assessedTrainingExperience)
        assertEquals(1, loaded.assessedPushCapacity)
        assertEquals(2, loaded.assessedPullCapacity)
        assertTrue(loaded.toCapabilityProfile().assessed)

    }

    @Test fun athletePersistenceOwnsOnlyStableAthleteFields() {
        val store = AppStore(context)
        store.saveAthlete(AthleteProfile(
            name = "Ada",
            goal = "Calisthenics",
            level = "Avanzato",
            daysPerWeek = 6,
            age = 35,
            heightCm = 170f,
            weightKg = 65f,
            sex = "Donna",
            prioritySkills = listOf("Front lever")
        ))
        assertFalse(prefs.contains("athlete_goal"))
        assertFalse(prefs.contains("athlete_days"))
        assertFalse(prefs.contains("athlete_priority_skills"))
        val loaded = store.loadAthlete()
        assertEquals("Ada", loaded.name)
        assertEquals("Avanzato", loaded.level)
        assertEquals(35, loaded.age)
        assertEquals(170f, loaded.heightCm)
        assertEquals(65f, loaded.weightKg)
        assertEquals("Donna", loaded.sex)
        assertEquals(AthleteProfile().goal, loaded.goal)
        assertEquals(AthleteProfile().daysPerWeek, loaded.daysPerWeek)
        assertTrue(loaded.prioritySkills.isEmpty())
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


    @Test fun pausedRestRemainingSurvivesProcessRecreationWithoutAdvancing() {
        val store = AppStore(context)
        val paused = WorkoutSessionState(
            id = "paused-rest",
            startedAtEpochMs = 1_000,
            profileName = "Casa",
            exercises = listOf(WorkoutExerciseState("Push-up")),
            elapsedBeforePauseMs = 42_000,
            pausedAtEpochMs = 50_000,
            restEndsAtEpochMs = null,
            runnerKind = "automatic",
            restKind = "Serie",
            pausedRestRemainingSec = 73
        )
        store.saveActiveWorkout(paused)

        val restored = AppStore(context).loadActiveWorkout()!!

        assertEquals(42, elapsedWorkoutSeconds(restored, 500_000))
        assertEquals(73, restored.pausedRestRemainingSec)
        assertNull(restored.restEndsAtEpochMs)
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
    @Test fun currentPlanExerciseSnapshotIsIndependentFromCurrentCatalogDefinition() {
        val original=exercises.first().copy(setRestSec=137,exerciseRestSec=241,targetSets=4,targetValue=11.0,role="Snapshot role")
        val plan=TrainingPlan("snapshot",10,1,1,listOf(PlannedWorkout("w",10,0,0,listOf(original))))
        val store=AppStore(context)
        store.saveTrainingPlan(plan)
        val raw=prefs.getString("training_plan_v091",null)!!
        assertTrue(raw.contains("\"exerciseSnapshots\""))
        val loaded=AppStore(context).loadTrainingPlan()!!.workouts.single().exercises.single()
        assertEquals(original,loaded)
        val catalogSameName=exercises.first{it.name==original.name}
        assertEquals(137,loaded.setRestSec)
        assertEquals(241,loaded.exerciseRestSec)
        assertEquals(4,loaded.targetSets)
        assertEquals(11.0,loaded.targetValue!!,0.001)
        assertEquals("Snapshot role",loaded.role)
        assertNotEquals(catalogSameName.copy(setRestSec=137,exerciseRestSec=241,targetSets=4,targetValue=11.0,role="Snapshot role"),catalogSameName)
    }

    @Test fun currentActiveWorkoutPersistsStableIdAndImmutableExerciseSnapshot() {
        val catalog=exercises.first()
        val state=WorkoutSessionState("upgrade",1000,"Casa",listOf(plannedExerciseState(0,catalog)))
        val store=AppStore(context); store.saveActiveWorkout(state)
        val raw=prefs.getString("active_workout_v1",null)!!
        assertTrue(raw.contains("\"schemaVersion\":2"))
        val restored=AppStore(context).loadActiveWorkout()!!.exercises.single()
        assertEquals(catalog.progressionKey,restored.exerciseId)
        assertEquals(catalog,restored.exerciseSnapshot)
    }

    @Test fun runningExerciseRecoverySurvivesProcessRecreationFromAbsoluteDeadline() {
        val now = 100_000L
        val active = WorkoutSessionState(
            id = "exercise-rest",
            startedAtEpochMs = 1_000,
            profileName = "Palestra",
            exercises = listOf(WorkoutExerciseState("Squat", setRestSec = 90, exerciseRestSec = 180)),
            restEndsAtEpochMs = now + 180_000L,
            restKind = "Esercizio"
        )
        AppStore(context).saveActiveWorkout(active)

        val restored = AppStore(context).loadActiveWorkout()!!

        assertEquals("Esercizio", restored.restKind)
        assertEquals(180, remainingSeconds(restored.restEndsAtEpochMs, now))
        assertEquals(150, remainingSeconds(restored.restEndsAtEpochMs, now + 30_000L))
        assertEquals(0, remainingSeconds(restored.restEndsAtEpochMs, now + 181_000L))
    }

    @Test fun activeRunnerReloadPreservesCurrentExerciseAndStructuredPerformance() {
        val first = WorkoutExerciseState("Bench", sets = listOf(
            WorkoutSetState(0, targetReps = 8, actualReps = 8, actualLoadKg = 80.0, completed = true, restUsedSec = 120),
            WorkoutSetState(1, targetReps = 8)
        ), status = WorkoutExerciseStatus.PARTIAL, rpe = 5)
        val second = WorkoutExerciseState("Row", sets = listOf(WorkoutSetState(0, targetReps = 10)), rpe = 5)
        val active = WorkoutSessionState(
            id = "runner-position",
            startedAtEpochMs = 1_000,
            profileName = "Palestra",
            exercises = listOf(first, second),
            currentExerciseIndex = 1,
            workoutRpe = 5,
            runnerKind = "automatic"
        )
        AppStore(context).saveActiveWorkout(active)

        val restored = AppStore(context).loadActiveWorkout()!!

        assertEquals(1, restored.currentExerciseIndex)
        assertEquals("Row", restored.exercises[restored.currentExerciseIndex].exerciseName)
        assertEquals(8, restored.exercises.first().sets.first().actualReps)
        assertEquals(80.0, restored.exercises.first().sets.first().actualLoadKg!!, 0.001)
        assertEquals(120, restored.exercises.first().sets.first().restUsedSec)
        assertEquals(5, restored.exercises.first().rpe)
        assertEquals(5, restored.workoutRpe)
    }

    @Test fun schemaFourPlanRoundTripPreservesIntentAndLegacySnapshots() {
        val ex=exercises.first()
        val intent=PlannedSessionIntent("Forza",listOf("Front lever"),setOf(ExerciseCategory.PULL),0).validated()
        val plan=TrainingPlan("intent-v4",10,1,1,listOf(PlannedWorkout("w",10,0,0,listOf(ex),intent=intent)))
        val store=AppStore(context)
        assertTrue(store.saveTrainingPlan(plan))
        val raw=JSONObject(prefs.getString("training_plan_v091",null)!!)
        assertEquals(4,raw.getInt("schemaVersion"))
        assertTrue(raw.getJSONArray("workouts").getJSONObject(0).has("exerciseSnapshots"))
        assertTrue(raw.getJSONArray("workouts").getJSONObject(0).has("intent"))
        assertEquals(plan,AppStore(context).loadTrainingPlan())
    }

    @Test fun schemaThreePlanWithoutIntentStillLoadsForBackwardCompatibility() {
        val ex=exercises.first()
        val plan=TrainingPlan("legacy-v3",10,1,1,listOf(PlannedWorkout("w",10,0,0,listOf(ex))))
        val store=AppStore(context)
        assertTrue(store.saveTrainingPlan(plan))
        val root=JSONObject(prefs.getString("training_plan_v091",null)!!)
        root.put("schemaVersion",3)
        root.getJSONArray("workouts").getJSONObject(0).remove("intent")
        prefs.edit().putString("training_plan_v091",root.toString()).commit()
        val loaded=AppStore(context).loadTrainingPlan()!!
        assertEquals(plan,loaded)
        assertNull(loaded.workouts.single().intent)
    }

}
