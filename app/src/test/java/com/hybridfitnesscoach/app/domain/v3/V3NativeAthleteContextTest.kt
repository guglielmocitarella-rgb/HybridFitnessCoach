package com.hybridfitnesscoach.app.domain.v3

import com.hybridfitnesscoach.app.*
import java.util.concurrent.Executor
import org.junit.Assert.*
import org.junit.Test

class V3NativeAthleteContextTest {
    private val profile = AthleteProfile(name = "Atleta", goal = "Forza", assessedTrainingExperience = 2,
        assessedPushCapacity = 3, prioritySkills = listOf("Handstand"))
    private val location = TrainingLocation("home", "Casa", "", TrainingLocationKind.HOME,
        setOf(com.hybridfitnesscoach.app.EquipmentId.BODYWEIGHT))
    private fun session(timestamp: Long = 10L) = SessionRecord(timestamp, "Casa", 1, 2,
        structuredExercises = listOf(
            WorkoutExerciseState("Push-up", sets = listOf(WorkoutSetState(0, actualReps = 8, completed = true)), exerciseId = "native-push"),
            WorkoutExerciseState("Skipped", status = WorkoutExerciseStatus.SKIPPED,
                sets = listOf(WorkoutSetState(0, targetReps = 10)))
        ))
    private fun context(history: List<SessionRecord> = emptyList()) =
        V3NativeAthleteContextAdapter.fromNative(profile, history)

    @Test fun realProfileGoalsAndExperienceRemainDeclaredOnNativeScale() {
        val context = context()
        assertEquals(profile, context.profile.semantic.value)
        assertEquals("Forza", context.goal.semantic.value)
        assertEquals(listOf("Handstand"), context.prioritySkills.semantic.value)
        assertEquals(V3ContextProvenance.DECLARED, context.goal.provenance)
        assertEquals(2, context.trainingExperience.semantic.value)
        assertEquals(V3AssessmentProvenance.DIRECT_ASSESSMENT, context.trainingExperience.provenance)
    }

    @Test fun assessedCapabilitiesReuseContractAndKeepUnknownAxesUnknown() {
        val context = context()
        assertEquals(V3AssessedValue.direct(3), context.capabilities.movement.assessment(V3MovementDimension.HORIZONTAL_PUSH))
        assertEquals(SemanticState.UNKNOWN, context.capabilities.movement.assessment(V3MovementDimension.VERTICAL_PULL).semantic.state)
        assertEquals(SemanticState.UNKNOWN, context.capabilities.toV3AthleteCapabilities().strength.state)
        assertNull(context.capabilities.transversal.strength.provenance)
    }

    @Test fun emptyHistoryDoesNotInventReadinessRecoveryFatigueOrSkillMastery() {
        val context = context()
        assertEquals(emptyList<SessionRecord>(), context.history.semantic.value)
        assertEquals(emptyList<V3ExecutedExercise>(), context.recentExercises.semantic.value)
        listOf(context.readiness, context.recovery, context.fatigue).forEach {
            assertEquals(SemanticState.UNKNOWN, it.semantic.state)
            assertNull(it.provenance)
        }
        assertEquals(SemanticState.UNKNOWN, context.skillProgress.semantic.state)
    }

    @Test fun onlyActualSetsBecomeObservedRecentExercisesWithoutCanonicalIdGuessing() {
        val context = context(listOf(session()))
        assertEquals(listOf(V3ExecutedExercise("Push-up", "native-push", 10L, 1)), context.recentExercises.semantic.value)
        assertEquals(V3ContextProvenance.OBSERVED, context.recentExercises.provenance)
        assertEquals(listOf(session()), context.history.semantic.value)
        assertEquals(SemanticState.UNKNOWN, context.skillProgress.semantic.state)
    }

    @Test fun unstructuredHistoryUsesOnlyPositivePerExerciseActualCounts() {
        val history = listOf(SessionRecord(20, "Casa", 2, 4,
            exerciseNames = listOf("Done", "Planned"), completedByExercise = mapOf("Done" to 2, "Skipped" to 0)))
        assertEquals(listOf(V3ExecutedExercise("Done", null, 20, 2)), context(history).recentExercises.semantic.value)
        assertEquals(SemanticState.UNKNOWN, context(history).skillProgress.semantic.state)
    }

    @Test fun nativeReadinessIsAnAppRuleAndMissingOrInvalidReadinessStaysUnknown() {
        val context = V3NativeAthleteContextAdapter.fromNative(profile, emptyList(), readiness = 65)
        assertEquals(65, context.readiness.semantic.value)
        assertEquals(V3ContextProvenance.DERIVED_APP_RULE, context.readiness.provenance)
        assertEquals(SemanticState.UNKNOWN, V3NativeAthleteContextAdapter.fromNative(profile, emptyList(), readiness = 101).readiness.semantic.state)
        assertEquals(SemanticState.UNKNOWN, context.recovery.semantic.state)
    }

    @Test fun enabledPlansAreCarriedWithoutChoosingAnArtificialSingleActivePlan() {
        val plans = listOf(TrainingPlan("a", 0, 2, 3, emptyList(), goal = "Forza"),
            TrainingPlan("b", 0, 2, 2, emptyList(), goal = "Skill"))
        val context = V3NativeAthleteContextAdapter.fromNative(profile, emptyList(), plans)
        assertEquals(plans, context.activePlans.semantic.value)
        assertEquals(V3ContextProvenance.DECLARED, context.activePlans.provenance)
    }

    @Test fun skillProgressUsesExistingNativeRulesOnlyForObservedSkillNodes() {
        val actual = SessionRecord(30, "Casa", 3, 3, structuredExercises = listOf(
            WorkoutExerciseState("Wall Handstand", sets = (0..2).map {
                WorkoutSetState(it, actualSeconds = 30, completed = true)
            }, rpe = 6)))
        val progress = context(listOf(actual)).skillProgress
        assertEquals(V3ContextProvenance.DERIVED_APP_RULE, progress.provenance)
        assertEquals(listOf("handstand_wall"), progress.semantic.value!!.map { it.node.id })
        assertEquals(SkillCatalog.progress("Handstand", listOf(actual)).first(), progress.semantic.value!!.single())
        assertEquals(SemanticState.UNKNOWN, context(listOf(actual)).capabilities.toV3AthleteCapabilities().technical.state)
    }

    @Test fun blankGoalAndUnknownFactsCannotClaimProvenance() {
        val context = V3NativeAthleteContextAdapter.fromNative(profile.copy(goal = ""), emptyList())
        assertEquals(SemanticState.UNKNOWN, context.goal.semantic.state)
        assertNull(context.goal.provenance)
        try {
            V3ContextValue(SemanticValue.unknown<Int>(), V3ContextProvenance.OBSERVED, "invalid")
            fail("Unknown values cannot claim observed provenance")
        } catch (expected: IllegalArgumentException) {
            // Contract rejects invented provenance.
        }
    }

    @Test fun determinismUsesSuppliedFactsAndStableRecentOrdering() {
        val history = listOf(session(10), session(20))
        assertEquals(context(history), context(history))
        assertEquals(context(history).recentExercises, context(history.reversed()).recentExercises)
        assertEquals(listOf(20L, 10L), context(history).recentExercises.semantic.value!!.map { it.sessionTimestamp })
    }

    @Test fun invalidOrAbsentTrainingExperienceDoesNotBecomeAPositiveLevel() {
        for (experience in listOf(null, 0, 4)) {
            val context = V3NativeAthleteContextAdapter.fromNative(profile.copy(assessedTrainingExperience = experience), emptyList())
            assertEquals(SemanticState.UNKNOWN, context.trainingExperience.semantic.state)
        }
    }

    @Test fun athleteHistoryPlanAndEnvironmentUpdatesKeepTheSameLoadedDatabase() {
        var loads = 0
        val runtime = V3RuntimeComponent({ loads++; emptyDatabase() }, Executor { it.run() })
        runtime.activate(location, context())
        val loaded = (runtime.state.value as V3RuntimeState.Ready).loadResult
        val changedProfile = V3NativeAthleteContextAdapter.fromNative(profile.copy(goal = "Skill"), emptyList())
        runtime.activate(location, changedProfile)
        assertEquals(changedProfile, (runtime.state.value as V3RuntimeState.Ready).athleteContext)
        val changedHistory = context(listOf(session()))
        runtime.activate(location, changedHistory)
        assertEquals(changedHistory, (runtime.state.value as V3RuntimeState.Ready).athleteContext)
        val plan = TrainingPlan("active", 0, 1, 1, emptyList())
        val changedPlan = V3NativeAthleteContextAdapter.fromNative(profile, listOf(session()), listOf(plan))
        runtime.activate(location.copy(id = "park"), changedPlan)
        val ready = runtime.state.value as V3RuntimeState.Ready
        assertEquals(changedPlan, ready.athleteContext)
        assertEquals("park", ready.environment.locationId)
        assertSame(loaded, ready.loadResult)
        assertEquals(1, loads)
    }

    @Test fun updatesDuringLoadingPublishTheLatestAthleteAndHistoryWithOneTask() {
        val pending = mutableListOf<Runnable>()
        var loads = 0
        val runtime = V3RuntimeComponent({ loads++; emptyDatabase() }, Executor { pending.add(it) })
        runtime.activate(location, context())
        val latest = context(listOf(session()))
        runtime.activate(location.copy(id = "park"), latest)
        assertEquals(1, pending.size)
        pending.single().run()
        assertEquals(latest, (runtime.state.value as V3RuntimeState.Ready).athleteContext)
        assertEquals(1, loads)
    }

    @Test fun locationOnlyUpdateRetainsAthleteContext() {
        val runtime = V3RuntimeComponent({ emptyDatabase() }, Executor { it.run() })
        val context = context()
        runtime.activate(location, context)
        runtime.activate(location.copy(id = "park"))
        assertEquals(context, (runtime.state.value as V3RuntimeState.Ready).athleteContext)
    }

    @Test fun bodyweightIsImplicitForNoneAndDoesNotInventVocabularyOrBypassRequiredEquipment() {
        val environment = V3NativeEnvironmentAdapter.fromLocation(location, emptySet())
        assertTrue(environment.environment.equipment.isEmpty())
        assertTrue(com.hybridfitnesscoach.app.EquipmentId.BODYWEIGHT in environment.unmappedNativeEquipment)
        val known = SemanticValue.known(CapabilityLevel.LOW)
        val athlete = V3AthleteCapabilities(known, known, known, known)
        val exercise = ExerciseV3(CanonicalExerciseId("none"),
            EquipmentRequirement(EquipmentInformationState.KNOWN, EquipmentRequirementMode.NONE),
            ResistanceSpec(SemanticValue.known(ResistanceSource.BODYWEIGHT)),
            CapabilityRequirements(known, known, known, known), ExerciseCategoryV3("push"),
            movementPatterns = SemanticValue.known(emptySet()), version = V3Version("3.7"))
        assertTrue(V3SelectionEngine.evaluate(exercise, athlete, environment.environment, V3SelectionIntent()).eligible)
        val requiredBar = exercise.copy(equipment = EquipmentRequirement(EquipmentInformationState.KNOWN,
            EquipmentRequirementMode.REQUIRED, setOf(EquipmentId("pull_up_bar"))))
        assertTrue(V3SelectionRejection.EQUIPMENT_UNAVAILABLE in V3SelectionEngine.evaluate(requiredBar,
            athlete, environment.environment, V3SelectionIntent()).reasons)
    }

    private fun emptyDatabase() = V3LoadResult(emptyList(), V3LoadAudit(0, 0, 0, 0, 0, 0, 0, 0, 0, emptySet()))
}
