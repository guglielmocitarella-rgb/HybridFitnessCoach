package com.hybridfitnesscoach.app.domain.v3

import com.hybridfitnesscoach.app.PlannedWorkoutState
import com.hybridfitnesscoach.app.ProgressionStatus
import java.time.Instant
import java.time.ZoneOffset

/** Explicit evaluation time makes recency reproducible; scoring weights are versioned app rules. */
internal data class V3RankingConfig(val referenceTimeMillis: Long) {
    init { require(referenceTimeMillis >= 0) }
}
internal data class V3ScoreContribution(
    val factor: String,
    val points: Int,
    val reason: String,
    val provenance: String
)
internal data class V3RankedExercise(
    val canonicalId: CanonicalExerciseId,
    val eligibility: V3Eligibility,
    val score: Int?,
    val contributions: List<V3ScoreContribution>,
    val unavailableSignals: Set<String>
)
internal data class V3RankingEvaluation(
    val referenceTimeMillis: Long,
    val ruleVersion: String,
    val orderedEligible: List<V3RankedExercise>,
    val decisions: List<V3RankedExercise>,
    val historyIdentityAudit: V3IdentityAudit,
    val planIdentityAudit: V3IdentityAudit
)

/** Pure scoring over the existing fail-closed eligibility engine. No workout is materialized. */
internal object V3PersonalizedRanking {
    private const val DAY = 86_400_000L
    private const val RULE_VERSION = "10B.2-APP-RULE-1"
    private data class Actual(val id: CanonicalExerciseId, val time: Long, val rpe: Int?, val loadKg: Double?)

    fun evaluate(
        load: V3LoadResult,
        environment: V3SelectionEnvironment,
        athlete: V3NativeAthleteContext,
        config: V3RankingConfig,
        intent: V3SelectionIntent = V3SelectionIntent()
    ): V3RankingEvaluation {
        val resolver = V3CanonicalIdentityResolver(load)
        val observations = athlete.recentExercises.semantic.value.orEmpty()
        val historyMatches = observations.map { resolver.resolve(it.nativeExerciseId, it.nativeExerciseName) }
        val recent = observations.zip(historyMatches).filter { (observation, match) ->
            match.state == V3IdentityMatchState.MATCHED && observation.sessionTimestamp in 0..config.referenceTimeMillis
        }.groupBy { requireNotNull(it.second.canonicalId) }
        val actuals = athlete.history.semantic.value.orEmpty().flatMap { session ->
            session.structuredExercises.mapNotNull { native ->
                val completed = native.sets.filter { it.completed }
                if (completed.isEmpty() || session.timestamp !in 0..config.referenceTimeMillis) return@mapNotNull null
                val match = resolver.resolve(native.exerciseId, native.exerciseName)
                val id = match.canonicalId ?: return@mapNotNull null
                Actual(id, session.timestamp,
                    completed.mapNotNull { it.actualRpe?.takeIf { value -> value in 1..10 } }.maxOrNull()
                        ?: native.rpe?.takeIf { it in 1..10 },
                    completed.mapNotNull { it.actualLoadKg?.takeIf { value -> value.isFinite() && value >= 0 } }.maxOrNull())
            }
        }.groupBy { it.id }
        val day = Instant.ofEpochMilli(config.referenceTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
        val planned = athlete.activePlans.semantic.value.orEmpty().flatMap { plan ->
            plan.workouts.filter { it.epochDay in day..(day + 7) && it.state in setOf(
                PlannedWorkoutState.PLANNED, PlannedWorkoutState.RESCHEDULED, PlannedWorkoutState.ADAPTED)
            }.flatMap { it.exercises }
        }
        val planMatches = planned.map { resolver.resolve(it.progressionKey, it.name) }
        val plannedIds = planMatches.mapNotNull { it.canonicalId }.toSet()
        val capabilities = athlete.capabilities.toV3AthleteCapabilities()
        val decisions = load.records.sortedBy { it.id.value }.map { exercise ->
            val eligibility = V3SelectionEngine.evaluate(exercise, capabilities, environment, intent)
            val unavailable = linkedSetOf<String>()
            val contributions = mutableListOf<V3ScoreContribution>()
            fun add(factor: String, points: Int, reason: String, source: String) {
                contributions += V3ScoreContribution(factor, points, reason, "$RULE_VERSION; $source")
            }
            fun <T> source(value: V3ContextValue<T>) = "${value.provenance}: ${value.source}"
            if (capabilities.strength.state != SemanticState.KNOWN) unavailable += "strength"
            if (capabilities.technical.state != SemanticState.KNOWN) unavailable += "technical"
            if (capabilities.mobility.state != SemanticState.KNOWN) unavailable += "mobility"
            if (capabilities.balanceCoordination.state != SemanticState.KNOWN) unavailable += "balance_coordination"
            if (athlete.readiness.semantic.value == null) unavailable += "readiness"
            if (athlete.recovery.semantic.value == null) unavailable += "recovery"
            // The native fatigue input has no validated unit/scale yet; never invent one.
            unavailable += "fatigue_scale"
            if (athlete.skillProgress.semantic.value == null) unavailable += "skill_progress"
            if (exercise.skillRelations.any { it.evidenceState != EvidenceState.VERIFIED })
                unavailable += "skill_relation_evidence"
            if (athlete.activePlans.semantic.value == null) unavailable += "active_plans"
            if (recent[exercise.id].isNullOrEmpty()) unavailable += "exercise_history"
            if (actuals[exercise.id].orEmpty().none { it.loadKg != null }) unavailable += "observed_load"
            if (actuals[exercise.id].orEmpty().none { it.rpe != null }) unavailable += "observed_rpe"
            val goal = athlete.goal.semantic.value
            if (goalPoints(goal, exercise) == null) unavailable += "supported_goal"
            if (!eligibility.eligible) return@map V3RankedExercise(exercise.id, eligibility, null, emptyList(), unavailable)

            goalPoints(goal, exercise)?.let { points ->
                add("goal", points, "Declared goal '$goal': category ${exercise.category.value}", source(athlete.goal))
            }
            val requiredTechnical = exercise.capabilities.technicalDifficulty.value
            val assessedTechnical = capabilities.technical.value
            if (requiredTechnical != null && assessedTechnical != null) {
                val points = (6 - (assessedTechnical.ordinal - requiredTechnical.ordinal) * 2).coerceAtLeast(0)
                add("technical_fit", points, "Requirement $requiredTechnical within assessed $assessedTechnical",
                    "${athlete.capabilities.transversal.technical.provenance}; canonical.capability.technicalDifficulty")
            }
            val history = recent[exercise.id].orEmpty().map { it.first }.distinctBy { it.sessionTimestamp }
            val latest = history.maxOfOrNull { it.sessionTimestamp }
            if (latest != null && config.referenceTimeMillis - latest < 7 * DAY) {
                val age = config.referenceTimeMillis - latest
                val penalty = when { age < DAY -> -24; age < 3 * DAY -> -12; else -> -4 }
                add("recency", penalty, "Executed exercise within ${age / 3_600_000L} hours; no recovery inference",
                    source(athlete.recentExercises) + "; " + recent[exercise.id].orEmpty().first().second.provenance)
                val count = history.count { config.referenceTimeMillis - it.sessionTimestamp < 7 * DAY }
                add("repetition", -minOf(12, count * 3), "$count distinct executed sessions in seven days",
                    source(athlete.recentExercises))
            }
            val recentActuals = actuals[exercise.id].orEmpty().filter { config.referenceTimeMillis - it.time < 3 * DAY }
            val latestRpeTime = recentActuals.filter { it.rpe != null }.maxOfOrNull { it.time }
            val rpe = recentActuals.filter { it.time == latestRpeTime }.mapNotNull { it.rpe }.maxOrNull()
            if (rpe != null) add("observed_rpe", if (rpe >= 8) -8 else 0,
                "Recent completed-set/exercise RPE $rpe; app caution rule", source(athlete.history))
            actuals[exercise.id].orEmpty().filter { it.loadKg != null }.maxByOrNull { it.time }?.let { actual ->
                add("observed_load", 0, "Observed load ${actual.loadKg} kg; no cross-exercise capacity inference", source(athlete.history))
            }
            fun caution(factor: String, value: V3ContextValue<Int>) {
                val score = value.semantic.value
                if (score == null || score !in 0..100) { unavailable += factor; return }
                val points = when {
                    score < 45 -> if (exercise.category.value == "MOBILITY") 12 else -12
                    score < 60 -> if (requiredTechnical != null && requiredTechnical >= CapabilityLevel.HIGH) -8 else -4
                    else -> 0
                }
                add(factor, points, "Available $factor $score/100; explicit caution rule, not measured muscle recovery", source(value))
            }
            caution("readiness", athlete.readiness)
            caution("recovery", athlete.recovery)
            val relations = exercise.skillRelations.filter { it.evidenceState == EvidenceState.VERIFIED }
            val priorities = athlete.prioritySkills.semantic.value.orEmpty().toSet()
            if (relations.any { it.targetType == SkillTargetType.FAMILY && it.targetId.value in priorities }) {
                add("priority_skill", 20, "Verified family relation matches declared priority skill",
                    source(athlete.prioritySkills) + "; canonical.skillRelations VERIFIED")
            }
            val progress = athlete.skillProgress.semantic.value.orEmpty().filter { progress ->
                relations.any { it.targetType == SkillTargetType.NODE && it.targetId.value == progress.node.id }
            }
            if (progress.isNotEmpty()) {
                val points = progress.maxOf { when (it.status) {
                    ProgressionStatus.IN_PROGRESS -> 14
                    ProgressionStatus.AVAILABLE -> 8
                    ProgressionStatus.MASTERED -> -4
                    ProgressionStatus.LOCKED -> -10
                } }
                add("skill_progress", points, "Observed native nodes: " + progress.sortedBy { it.node.id }
                    .joinToString { "${it.node.id} ${it.status}" }, source(athlete.skillProgress) + "; exact canonical NODE relation")
            }
            if (exercise.id in plannedIds) add("plan_overlap", -10,
                "Already present in a pending plan within seven days", source(athlete.activePlans) + "; explicit canonical identity")
            V3RankedExercise(exercise.id, eligibility, contributions.sumOf { it.points }, contributions, unavailable)
        }
        return V3RankingEvaluation(config.referenceTimeMillis, RULE_VERSION,
            decisions.filter { it.eligibility.eligible }.sortedWith(
                compareByDescending<V3RankedExercise> { requireNotNull(it.score) }.thenBy { it.canonicalId.value }),
            decisions, V3IdentityAudit.from(historyMatches), V3IdentityAudit.from(planMatches))
    }

    /** Product priorities, explicitly APP_RULE; neither physiological claims nor prescription. */
    private fun goalPoints(goal: String?, exercise: ExerciseV3): Int? {
        val category = exercise.category.value
        val strength = category in setOf("PUSH", "PULL", "LEGS")
        return when (goal) {
            "Forza", "Forza + skill", "Forza + Skill Calisthenics" -> when { strength -> 20; category == "CORE" -> 8; else -> 0 }
            "Ipertrofia" -> when { strength -> 20 + if (exercise.resistance.source.value == ResistanceSource.EXTERNAL_LOAD) 5 else 0; category == "CORE" -> 8; else -> 0 }
            "Calisthenics" -> when (category) { "SKILL" -> 20; "PUSH", "PULL" -> 12; "CORE" -> 8; else -> 0 }
            "Fitness generale" -> when (category) { "CARDIO" -> 15; "MOBILITY" -> 8; else -> 10 }
            else -> null
        }
    }
}
