package com.hybridfitnesscoach.app.domain.v3

import com.hybridfitnesscoach.app.AthleteProfile
import com.hybridfitnesscoach.app.SessionRecord
import com.hybridfitnesscoach.app.SkillCatalog
import com.hybridfitnesscoach.app.SkillNodeProgress
import com.hybridfitnesscoach.app.TrainingPlan

/** Facts and app heuristics are separate from verified capability derivations. */
internal enum class V3ContextProvenance { DECLARED, OBSERVED, DERIVED_APP_RULE }

internal data class V3ContextValue<T>(
    val semantic: SemanticValue<T>,
    val provenance: V3ContextProvenance? = null,
    val source: String? = null
) {
    init {
        require((semantic.state == SemanticState.KNOWN) == (provenance != null))
        require((provenance != null) == !source.isNullOrBlank())
    }

    companion object {
        fun <T> unknown(): V3ContextValue<T> = V3ContextValue(SemanticValue.unknown())
        fun <T> known(value: T, provenance: V3ContextProvenance, source: String): V3ContextValue<T> =
            V3ContextValue(SemanticValue.known(value), provenance, source)
    }
}

/** Native IDs/names are retained as observations, never relabelled as canonical V3 IDs. */
internal data class V3ExecutedExercise(
    val nativeExerciseName: String,
    val nativeExerciseId: String?,
    val sessionTimestamp: Long,
    val completedSets: Int
)

/** Transient ranking input. Existing native snapshots are reused, not persisted again. */
internal data class V3NativeAthleteContext(
    val profile: V3ContextValue<AthleteProfile>,
    val goal: V3ContextValue<String>,
    val prioritySkills: V3ContextValue<List<String>>,
    val trainingExperience: V3AssessedValue<Int>,
    val capabilities: V3NativeAthleteCapabilityContract,
    val history: V3ContextValue<List<SessionRecord>>,
    val recentExercises: V3ContextValue<List<V3ExecutedExercise>>,
    val activePlans: V3ContextValue<List<TrainingPlan>>,
    val skillProgress: V3ContextValue<List<SkillNodeProgress>>,
    val readiness: V3ContextValue<Int>,
    val recovery: V3ContextValue<Int> = V3ContextValue.unknown(),
    val fatigue: V3ContextValue<Int> = V3ContextValue.unknown()
)

internal object V3NativeAthleteContextAdapter {
    /** No clock, guessed mastery, synthetic recovery, or cross-axis capability conversion. */
    fun fromNative(
        profile: AthleteProfile,
        sessions: List<SessionRecord>,
        activePlans: List<TrainingPlan> = emptyList(),
        readiness: Int? = null
    ): V3NativeAthleteContext {
        val history = sessions.toList()
        val recent = history.flatMap { session ->
            if (session.structuredExercises.isNotEmpty()) {
                session.structuredExercises.mapNotNull { exercise ->
                    val completed = exercise.sets.count { it.completed }
                    if (completed == 0) null else V3ExecutedExercise(
                        exercise.exerciseName, exercise.exerciseId, session.timestamp, completed
                    )
                }
            } else {
                // Aggregate completedSets/exerciseNames cannot prove which exercises were done.
                session.completedByExercise.filterValues { it > 0 }.map { (name, count) ->
                    V3ExecutedExercise(name, null, session.timestamp, count)
                }
            }
        }.sortedWith(compareByDescending<V3ExecutedExercise> { it.sessionTimestamp }
            .thenBy { it.nativeExerciseName }.thenBy { it.nativeExerciseId.orEmpty() }.thenBy { it.completedSets })
        val observedSkillSessions = history.mapNotNull { session ->
            val actual = session.structuredExercises.mapNotNull { exercise ->
                val completed = exercise.sets.filter { it.completed }
                if (completed.isEmpty()) null else exercise.copy(sets = completed)
            }
            if (actual.isEmpty()) null else session.copy(structuredExercises = actual)
        }
        val observedNames = observedSkillSessions.flatMap { it.structuredExercises }.map { it.exerciseName }
        val observedProgress = if (observedNames.isEmpty()) emptyList() else
            SkillCatalog.skills().sorted().flatMap { SkillCatalog.progress(it, observedSkillSessions) }
                .filter { progress ->
                    val names = progress.node.exerciseAliases + progress.node.exerciseName
                    observedNames.any { observed -> names.any { it.equals(observed, ignoreCase = true) } }
                }
        val skillProgress: V3ContextValue<List<SkillNodeProgress>> =
            if (observedProgress.isEmpty()) V3ContextValue.unknown() else
            V3ContextValue.known(
                observedProgress,
                V3ContextProvenance.DERIVED_APP_RULE, "SkillCatalog.progress: native mastery criteria"
            )
        return V3NativeAthleteContext(
            profile = V3ContextValue.known(profile, V3ContextProvenance.DECLARED, "AthleteProfile"),
            goal = profile.goal.takeIf { it.isNotBlank() }?.let {
                V3ContextValue.known(it, V3ContextProvenance.DECLARED, "AthleteProfile.goal")
            } ?: V3ContextValue.unknown(),
            prioritySkills = V3ContextValue.known(profile.prioritySkills.toList(),
                V3ContextProvenance.DECLARED, "AthleteProfile.prioritySkills"),
            trainingExperience = profile.assessedTrainingExperience?.takeIf { it in 1..3 }?.let {
                V3AssessedValue.direct(it)
            } ?: V3AssessedValue.unknown(),
            capabilities = V3NativeAthleteProfileAdapter.fromProfile(profile),
            history = V3ContextValue.known(history, V3ContextProvenance.OBSERVED, "SessionRecord actual history"),
            recentExercises = V3ContextValue.known(recent, V3ContextProvenance.OBSERVED,
                "SessionRecord: completed structured sets or explicit per-exercise actual counts"),
            activePlans = V3ContextValue.known(activePlans.toList(), V3ContextProvenance.DECLARED,
                "AppStore.loadEnabledTrainingPlans"),
            skillProgress = skillProgress,
            readiness = readiness?.takeIf { it in 0..100 }?.let {
                V3ContextValue.known(it, V3ContextProvenance.DERIVED_APP_RULE,
                    "HealthConnectState.readiness: DefaultHealthNormalizer + DefaultReadinessProvider")
            } ?: V3ContextValue.unknown()
        )
    }
}
