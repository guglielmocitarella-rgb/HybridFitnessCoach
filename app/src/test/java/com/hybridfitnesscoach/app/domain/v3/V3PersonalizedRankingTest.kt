package com.hybridfitnesscoach.app.domain.v3

import com.hybridfitnesscoach.app.AthleteProfile
import com.hybridfitnesscoach.app.SessionRecord
import com.hybridfitnesscoach.app.SkillNode
import com.hybridfitnesscoach.app.SkillNodeProgress
import com.hybridfitnesscoach.app.ProgressionStatus
import com.hybridfitnesscoach.app.TrainingPlan
import com.hybridfitnesscoach.app.PlannedWorkout
import com.hybridfitnesscoach.app.Exercise
import com.hybridfitnesscoach.app.WorkoutExerciseState
import com.hybridfitnesscoach.app.WorkoutSetState
import org.junit.Assert.*
import org.junit.Test

class V3PersonalizedRankingTest {
    private val now = 1_800_000_000_000L
    private val low = SemanticValue.known(CapabilityLevel.LOW)
    private fun exercise(id: String, category: String = "PUSH") = ExerciseV3(
        CanonicalExerciseId(id), EquipmentRequirement(EquipmentInformationState.KNOWN, EquipmentRequirementMode.NONE),
        ResistanceSpec(SemanticValue.known(ResistanceSource.BODYWEIGHT)),
        CapabilityRequirements(low, low, low, low), ExerciseCategoryV3(category),
        movementPatterns = SemanticValue.unknown(), version = V3Version("3.7")
    )
    private fun context(goal: String = "Forza", sessions: List<SessionRecord> = emptyList(), readiness: Int? = null): V3NativeAthleteContext {
        val known = V3AssessedValue.direct(CapabilityLevel.MODERATE)
        return V3NativeAthleteContextAdapter.fromNative(AthleteProfile(goal = goal), sessions, readiness = readiness)
            .copy(capabilities = V3NativeAthleteCapabilityContract(transversal = V3TransversalAssessments(known, known, known, known)))
    }
    private fun loaded(vararg records: ExerciseV3) = V3LoadResult(records.toList(), V3LoadAudit(0,0,0,0,0,0,0,0,0,emptySet()))
    private fun rank(db: V3LoadResult, athlete: V3NativeAthleteContext = context(), environment: V3SelectionEnvironment = V3SelectionEnvironment(), intent: V3SelectionIntent = V3SelectionIntent()) =
        V3PersonalizedRanking.evaluate(db, environment, athlete, V3RankingConfig(now), intent)

    private fun fullyAssessed() = context().copy(capabilities = V3NativeAthleteCapabilityContract(
        transversal = V3TransversalAssessments(
            V3AssessedValue.direct(CapabilityLevel.VERY_HIGH), V3AssessedValue.direct(CapabilityLevel.VERY_HIGH),
            V3AssessedValue.direct(CapabilityLevel.VERY_HIGH), V3AssessedValue.direct(CapabilityLevel.VERY_HIGH))))

    private fun fullEnvironment(db: V3LoadResult) = V3SelectionEnvironment(
        V3LoadedEquipmentVocabulary.from(db), assistanceAvailable = true, externalLoadAvailable = true)

    @Test fun installedAssetRemainsEmptyEvenWithMaximumExplicitAssessments() {
        val db = V3AssetLoader.loadAuthoritative(java.io.File(requireNotNull(System.getProperty("hfc.v3.asset"))).readText())
        val result = rank(db, fullyAssessed(), fullEnvironment(db))
        assertEquals(1087, result.decisions.size)
        assertTrue(result.orderedEligible.isEmpty())
        assertTrue(result.decisions.all { V3SelectionRejection.CAPABILITY_UNRESOLVED in it.eligibility.reasons })
    }

    @Test fun frozenCandidateWithRealNativeUnknownAssessmentsCannotInventAnEligiblePool() {
        val db = V3RankingRealFixtures.frozenCanonical()
        val native = V3NativeAthleteContextAdapter.fromNative(AthleteProfile(assessedTrainingExperience = 3), emptyList())
        assertTrue(rank(db, native, fullEnvironment(db)).orderedEligible.isEmpty())
        assertEquals(SemanticState.UNKNOWN, native.capabilities.transversal.strength.semantic.state)
    }

    @Test fun frozenCandidateRetainsTerminalEvidenceAndMissingPrescriptionReasonsWithoutScores() {
        val db = V3RankingRealFixtures.frozenCanonical()
        val result = rank(db, fullyAssessed(), fullEnvironment(db))
        val terminalIds = db.records.filter { record -> with(record.capabilities) {
            listOf(strengthRequirement, technicalDifficulty, mobilityRequirement, balanceCoordinationRequirement)
                .any { it.state == SemanticState.EVIDENCE_UNRESOLVED }
        } }.map { it.id }.toSet()
        assertTrue(terminalIds.isNotEmpty())
        result.decisions.filter { it.canonicalId in terminalIds }.forEach {
            assertTrue(V3SelectionRejection.TERMINAL_CANONICAL_STATE in it.eligibility.reasons)
            assertNull(it.score)
            assertTrue(it.contributions.isEmpty())
        }
        val pdIds = db.records.filter { it.capabilities.strengthRequirement.state == SemanticState.PRESCRIPTION_DEPENDENT }.map { it.id }.toSet()
        assertEquals(441, pdIds.size)
        result.decisions.filter { it.canonicalId in pdIds }.forEach {
            assertTrue(V3SelectionRejection.PD_STRENGTH_CONTEXT_INSUFFICIENT in it.eligibility.reasons)
            assertNull(it.score)
        }
    }

    @Test fun knownIncompatibleRequirementCannotBeOffsetByGoalOrReadiness() {
        val high = exercise("too-hard").copy(capabilities = CapabilityRequirements(
            SemanticValue.known(CapabilityLevel.VERY_HIGH), low, low, low))
        val result = rank(loaded(high), context(readiness = 100)).decisions.single()
        assertEquals(setOf(V3SelectionRejection.CAPABILITY_INSUFFICIENT), result.eligibility.reasons)
        assertNull(result.score)
        assertTrue(result.contributions.isEmpty())
    }

    @Test fun frozenPdLoadAndAssistanceRequireExactPrescriptionAndMeasuredEnvelope() {
        val db = V3RankingRealFixtures.frozenCanonical()
        val env = fullEnvironment(db)
        val athlete = fullyAssessed()
        for (source in listOf(ResistanceSource.EXTERNAL_LOAD, ResistanceSource.ASSISTED)) {
            val record = db.records.first { record ->
                record.resistance.source.value == source &&
                    record.capabilities.strengthRequirement.state == SemanticState.PRESCRIPTION_DEPENDENT &&
                    with(record.capabilities) { listOf(technicalDifficulty, mobilityRequirement, balanceCoordinationRequirement)
                        .all { it.state in setOf(SemanticState.KNOWN, SemanticState.NOT_APPLICABLE) } }
            }
            // Explicit contract probe only: these values are not user measurements or a generated prescription.
            val unit = (if (source == ResistanceSource.EXTERNAL_LOAD) record.resistance.loadUnit.value
                else record.resistance.assistanceUnit.value) ?: InputUnit.KILOGRAMS
            val work = V3WorkDimensions(reps = 5.0)
            val prescription = V3PrescriptionContext(record.id, source, work, V3QuantifiedResistance(10.0, unit))
            val envelope = V3AthleteExerciseCapacityEnvelope(listOf(V3CapacityEnvelopePoint(record.id, source, work,
                maximumFeasibleLoad = if (source == ResistanceSource.EXTERNAL_LOAD) V3QuantifiedResistance(10.0, unit) else null,
                minimumRequiredAssistance = if (source == ResistanceSource.ASSISTED) V3QuantifiedResistance(10.0, unit) else null)))
            fun decision(context: V3PdStrengthContext) = rank(db, athlete, env,
                V3SelectionIntent(pdStrengthContext = context)).decisions.single { it.canonicalId == record.id }
            val valid = V3PdStrengthContext(prescription, envelope)
            assertTrue(decision(valid).eligibility.eligible)
            assertNotNull(decision(valid).score)
            val incompatible = prescription.copy(resistance = V3QuantifiedResistance(
                if (source == ResistanceSource.EXTERNAL_LOAD) 11.0 else 9.0, unit))
            assertTrue(V3SelectionRejection.CAPABILITY_INSUFFICIENT in decision(valid.copy(prescription = incompatible)).eligibility.reasons)
            assertTrue(V3SelectionRejection.ATHLETE_CAPABILITY_UNKNOWN in decision(valid.copy(athleteCapacity = null)).eligibility.reasons)
            assertTrue(V3SelectionRejection.PD_STRENGTH_CONTEXT_INVALID in decision(valid.copy(
                prescription = prescription.copy(exerciseId = CanonicalExerciseId("different-id")))).eligibility.reasons)
            assertEquals(SemanticState.PRESCRIPTION_DEPENDENT, record.capabilities.strengthRequirement.state)
        }
    }

    @Test fun availableRecoveryIsExplicitAppCautionAndNotAnInventedMuscleAssessment() {
        val athlete = context().copy(recovery = V3ContextValue.known(20,
            V3ContextProvenance.DERIVED_APP_RULE, "test observed recovery input"))
        val result = rank(loaded(exercise("push"), exercise("mobility", "MOBILITY")), athlete)
        assertEquals("mobility", result.orderedEligible.first().canonicalId.value)
        assertTrue(result.orderedEligible.all { row -> row.contributions.any {
            it.factor == "recovery" && it.provenance.contains("APP_RULE") && it.reason.contains("not measured muscle recovery")
        } })
    }

    @Test fun declaredPriorityNeedsVerifiedExactFamilyRelation() {
        val relation = SkillRelation(SkillTargetType.FAMILY, SkillTargetId("front-lever"),
            SkillRelationType.DIRECT_PROGRESSION, EvidenceState.VERIFIED)
        val athlete = context().copy(prioritySkills = V3ContextValue.known(listOf("front-lever"),
            V3ContextProvenance.DECLARED, "declared priority"))
        val matched = exercise("matched").copy(skillRelations = listOf(relation))
        val other = exercise("other").copy(skillRelations = listOf(relation.copy(targetId = SkillTargetId("front-lever-other"))))
        val result = rank(loaded(matched, other), athlete)
        assertTrue(result.decisions.single { it.canonicalId == matched.id }.contributions.any { it.factor == "priority_skill" })
        assertFalse(result.decisions.single { it.canonicalId == other.id }.contributions.any { it.factor == "priority_skill" })
    }

    @Test fun frozenSkillRelationsCannotClaimVerifiedPersonalizationFromConfidenceMetadata() {
        val db = V3RankingRealFixtures.frozenCanonical()
        val env = fullEnvironment(db)
        val eligibleIds = rank(db, fullyAssessed(), env).orderedEligible.map { it.canonicalId }.toSet()
        val record = db.records.first { it.id in eligibleIds && it.skillRelations.any { r -> r.targetType == SkillTargetType.NODE } }
        val relation = record.skillRelations.first { it.targetType == SkillTargetType.NODE }
        assertEquals(EvidenceState.UNSPECIFIED, relation.evidenceState)
        val athlete = fullyAssessed().copy(skillProgress = V3ContextValue.known(listOf(
            SkillNodeProgress(SkillNode(relation.targetId.value, "Observed native node", "Observed skill"),
                ProgressionStatus.IN_PROGRESS, 0.5)), V3ContextProvenance.DERIVED_APP_RULE, "test native observed progression"))
        val result = rank(db, athlete, env).decisions.single { it.canonicalId == record.id }
        assertTrue(result.eligibility.eligible)
        assertFalse(result.contributions.any { it.factor == "priority_skill" || it.factor == "skill_progress" })
        assertTrue("skill_relation_evidence" in result.unavailableSignals)
    }

    @Test fun deterministicWithStableCanonicalTiesAndExactBreakdown() {
        val db = loaded(exercise("b"), exercise("a"))
        val first = rank(db)
        assertEquals(first, rank(db.copy(records = db.records.reversed())))
        assertEquals(listOf("a", "b"), first.orderedEligible.map { it.canonicalId.value })
        first.orderedEligible.forEach { assertEquals(it.contributions.sumOf { c -> c.points }, it.score) }
    }

    @Test fun incompatibleEquipmentCannotBeRescuedByScore() {
        val bad = exercise("bad").copy(equipment = EquipmentRequirement(EquipmentInformationState.KNOWN,
            EquipmentRequirementMode.REQUIRED, setOf(EquipmentId("barbell"))))
        val result = rank(loaded(bad, exercise("bw")))
        assertEquals(listOf("bw"), result.orderedEligible.map { it.canonicalId.value })
        val rejected = result.decisions.single { it.canonicalId == bad.id }
        assertNull(rejected.score)
        assertTrue(rejected.contributions.isEmpty())
        assertTrue(V3SelectionRejection.EQUIPMENT_UNAVAILABLE in rejected.eligibility.reasons)
    }

    @Test fun unknownCapabilitiesFailClosedWithoutUsingNativeExperience() {
        val native = V3NativeAthleteContextAdapter.fromNative(AthleteProfile(assessedTrainingExperience = 3), emptyList())
        assertTrue(rank(loaded(exercise("a")), native).orderedEligible.isEmpty())
    }

    @Test fun realGoalsChangeOrder() {
        val db = loaded(exercise("a-push"), exercise("z-cardio", "CARDIO"))
        assertEquals("a-push", rank(db, context("Forza")).orderedEligible.first().canonicalId.value)
        assertEquals("z-cardio", rank(db, context("Fitness generale")).orderedEligible.first().canonicalId.value)
    }

    @Test fun absentReadinessAndEmptyHistoryDoNotClaimRecovery() {
        val result = rank(loaded(exercise("a"))).orderedEligible.single()
        assertTrue("readiness" in result.unavailableSignals)
        assertTrue("recovery" in result.unavailableSignals)
        assertFalse(result.contributions.any { it.factor == "readiness" || it.factor == "recovery" || it.factor == "recency" })
    }

    @Test fun lowReadinessUsesExplicitAppRule() {
        val db = loaded(exercise("push"), exercise("mobility", "MOBILITY"))
        val result = rank(db, context(readiness = 20))
        assertEquals("mobility", result.orderedEligible.first().canonicalId.value)
        assertTrue(result.orderedEligible.all { it.contributions.any { c -> c.factor == "readiness" && c.provenance.contains("APP_RULE") } })
    }

    @Test fun completedCanonicalHistoryPenalizesRecencyAndObservedRpe() {
        val session = SessionRecord(now - 1_000, "Casa", 1, 2, structuredExercises = listOf(
            WorkoutExerciseState("Native", exerciseId = "a", sets = listOf(
                WorkoutSetState(0, completed = true, actualLoadKg = 20.0, actualRpe = 9),
                WorkoutSetState(1, completed = false, actualRpe = 10)))))
        val result = rank(loaded(exercise("a"), exercise("b")), context(sessions = listOf(session)))
        assertEquals("b", result.orderedEligible.first().canonicalId.value)
        assertEquals(1, result.historyIdentityAudit.matched)
        val a = result.decisions.single { it.canonicalId.value == "a" }
        assertTrue(a.contributions.any { it.factor == "observed_rpe" && it.points < 0 })
        assertTrue(a.contributions.any { it.factor == "observed_load" && it.points == 0 })
    }

    @Test fun skippedAndUncompletedExercisesDoNotBecomeHistory() {
        val session = SessionRecord(now, "Casa", 0, 1, structuredExercises = listOf(
            WorkoutExerciseState("a", exerciseId = "a", sets = listOf(WorkoutSetState(0)))))
        assertEquals(0, rank(loaded(exercise("a")), context(sessions = listOf(session))).historyIdentityAudit.matched)
    }

    @Test fun identityPrefersStableIdAndRejectsAmbiguousExactNamesAndUnknownIds() {
        val db = loaded(exercise("a"), exercise("b")).copy(identityEvidence = listOf(
            V3IdentityEvidence(CanonicalExerciseId("a"), setOf("native-a"), setOf("Same")),
            V3IdentityEvidence(CanonicalExerciseId("b"), emptySet(), setOf("Same"))))
        val resolver = V3CanonicalIdentityResolver(db)
        assertEquals(CanonicalExerciseId("a"), resolver.resolve("native-a", "Same").canonicalId)
        assertEquals(V3IdentityMatchState.AMBIGUOUS, resolver.resolve(null, "Same").state)
        assertEquals(V3IdentityMatchState.UNRESOLVED, resolver.resolve("unknown-id", "Same").state)
        assertEquals(V3IdentityMatchState.UNRESOLVED, resolver.resolve(null, "Similar Same").state)
    }

    @Test fun unresolvedHistoryCannotPenalizeAnyCanonicalExercise() {
        val session = SessionRecord(now, "Casa", 1, 1, completedByExercise = mapOf("Not mapped" to 1))
        val result = rank(loaded(exercise("a")), context(sessions = listOf(session)))
        assertEquals(1, result.historyIdentityAudit.unresolved)
        assertFalse(result.orderedEligible.single().contributions.any { it.factor == "recency" })
    }

    @Test fun verifiedSkillRelationUsesDeclaredPriorityAndObservedNodeProgress() {
        val relation = SkillRelation(SkillTargetType.NODE, SkillTargetId("front_tuck"),
            SkillRelationType.DIRECT_PROGRESSION, EvidenceState.VERIFIED)
        val a = exercise("a", "SKILL").copy(skillRelations = listOf(relation))
        val athlete = context("Calisthenics").copy(skillProgress = V3ContextValue.known(
            listOf(SkillNodeProgress(SkillNode("front_tuck", "Native", "Front lever"), ProgressionStatus.IN_PROGRESS, 0.5)),
            V3ContextProvenance.DERIVED_APP_RULE, "SkillCatalog.progress"))
        val result = rank(loaded(a), athlete).orderedEligible.single()
        assertTrue(result.contributions.any { it.factor == "skill_progress" && it.points > 0 })
        val unverified = a.copy(skillRelations = listOf(relation.copy(evidenceState = EvidenceState.DOCUMENTED_UNRESOLVED)))
        assertFalse(rank(loaded(unverified), athlete).orderedEligible.single().contributions.any { it.factor == "skill_progress" })
    }

    @Test fun activePlanOverlapUsesStableIdentityAndNotSimilarName() {
        val native = Exercise("Native", "Petto", "Corpo libero", "Principiante", progressionKey = "a")
        val day = java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneOffset.UTC).toLocalDate().toEpochDay()
        val plan = TrainingPlan("plan", day, 1, 1, listOf(PlannedWorkout("workout", day, 0, 0, listOf(native))))
        val athlete = context().copy(activePlans = V3ContextValue.known(listOf(plan), V3ContextProvenance.DECLARED, "plans"))
        val result = rank(loaded(exercise("a"), exercise("b")), athlete)
        assertEquals("b", result.orderedEligible.first().canonicalId.value)
        assertEquals(1, result.planIdentityAudit.matched)
    }

    @Test fun realDatabasePreservesAllDecisionsAndEligibility() {
        val db = V3AssetLoader.loadAuthoritative(java.io.File(requireNotNull(System.getProperty("hfc.v3.asset"))).readText())
        val env = V3SelectionEnvironment(V3LoadedEquipmentVocabulary.from(db), assistanceAvailable = true, externalLoadAvailable = true)
        val athlete = context()
        val result = rank(db, athlete, env)
        assertEquals(1087, result.decisions.size)
        // The unchanged installed 9r-b asset has UNKNOWN native capability fields.
        // This regression must remain fail-closed, not award scores to excluded records.
        assertTrue(result.orderedEligible.isEmpty())
        assertTrue(result.decisions.all { V3SelectionRejection.CAPABILITY_UNRESOLVED in it.eligibility.reasons })
        assertTrue(result.decisions.all { it.score == null && it.contributions.isEmpty() })
        assertEquals(V3SelectionEngine.pool(db.records, athlete.capabilities.toV3AthleteCapabilities(), env, V3SelectionIntent()).eligible.map { it.id }.toSet(),
            result.orderedEligible.map { it.canonicalId }.toSet())
        assertEquals(result, rank(db, athlete, env))
        assertTrue(db.identityEvidence.isNotEmpty())
    }

    @Test fun frozenCanonicalDatabaseProducesNonemptyPersonalizedScoresWithRealNativeHistory() {
        val db = V3RankingRealFixtures.frozenCanonical()
        val known = V3AssessedValue.direct(CapabilityLevel.VERY_HIGH)
        val capabilities = V3NativeAthleteCapabilityContract(transversal = V3TransversalAssessments(known, known, known, known))
        val athlete = context().copy(capabilities = capabilities)
        val env = V3SelectionEnvironment(V3LoadedEquipmentVocabulary.from(db), assistanceAvailable = true, externalLoadAvailable = true)
        val base = rank(db, athlete, env)
        assertTrue(base.orderedEligible.isNotEmpty())
        val chosen = base.orderedEligible.first().canonicalId
        val identity = db.identityEvidence.single { it.canonicalId == chosen }
        val session = SessionRecord(now - 1_000, "Casa", 1, 1, structuredExercises = listOf(
            WorkoutExerciseState(identity.nativeNames.first(), exerciseId = identity.nativeIds.first(),
                sets = listOf(WorkoutSetState(0, completed = true, actualRpe = 9, actualLoadKg = 20.0)))))
        val observed = rank(db, context(sessions = listOf(session)).copy(capabilities = capabilities), env)
        assertEquals(1, observed.historyIdentityAudit.matched)
        assertTrue(observed.decisions.single { it.canonicalId == chosen }.score!! < base.decisions.single { it.canonicalId == chosen }.score!!)
        assertNotEquals(base.orderedEligible, rank(db, context("Fitness generale").copy(capabilities = capabilities), env).orderedEligible)
        assertEquals(base, rank(db, athlete, env))
    }
}
