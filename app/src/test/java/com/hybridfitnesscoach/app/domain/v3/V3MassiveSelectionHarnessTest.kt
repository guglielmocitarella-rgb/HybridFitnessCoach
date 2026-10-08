package com.hybridfitnesscoach.app.domain.v3

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class V3MassiveSelectionHarnessTest {
    private val high = SemanticValue.known(CapabilityLevel.VERY_HIGH)
    private val athlete = V3AthleteCapabilities(high, high, high, high)

    @Test fun generatedMatrixExecutesThroughNativeEngineAndIndependentOracle() {
        val root = repositoryRoot()
        val candidateFile = File(root, "exercise-db-canonical/exercise-db-v3-final-candidate.json")
        val manifestFile = File(root, "build/step8/scenario-manifest.json")
        assertTrue("frozen candidate missing", candidateFile.isFile)
        assertTrue("generated manifest missing", manifestFile.isFile)
        val candidate = V3AssetLoader.loadAuthoritative(candidateFile.readText())
        val manifest = JSONObject(manifestFile.readText())
        assertEquals("V3-STEP8-MATRIX-1", manifest.getString("schema"))
        assertEquals(1087, candidate.records.size)
        assertEquals(441, manifest.getInt("pdCount"))
        assertEquals(436, manifest.getInt("externalLoadPdCount"))
        assertEquals(5, manifest.getInt("assistedPdCount"))
        assertEquals(48, manifest.getInt("terminalCount"))
        val byId = candidate.records.associateBy { it.id.value }
        val scenarios = manifest.getJSONArray("scenarios")
        assertEquals(manifest.getInt("plannedCount"), scenarios.length())
        var executedPd = 0
        var executedTerminal = 0
        val coveredPd = linkedSetOf<String>()
        val coveredTerminal = linkedSetOf<String>()
        for (i in 0 until scenarios.length()) {
            val row = scenarios.getJSONObject(i)
            val id = row.getString("canonicalExerciseId")
            val exercise = byId[id] ?: fail("scenario references unknown canonical ID: " + id)
            val axis = row.getString("axis")
            val context = parseContext(row.optJSONObject("context"))
            val environment = fullyEquipped(exercise)
            val intent = V3SelectionIntent(pdStrengthContext = context)
            val decision = V3SelectionEngine.evaluate(exercise, athlete, environment, intent)
            val expectedState = row.getString("expectedState")
            val expectedReason = row.getString("expectedReason")
            if (axis == "PD") {
                assertEquals("PD state for " + row.getString("scenarioId"), expectedState,
                    V3PdStrengthResolver.evaluate(exercise, context).state.name)
                assertEquals("PD reason for " + row.getString("scenarioId"), expectedReason,
                    V3PdStrengthResolver.evaluate(exercise, context).reason.name)
                assertEngineReflectsPdOutcome(row.getString("scenarioId"), expectedState, decision)
                executedPd++
                coveredPd.add(id)
            } else {
                val dimension = row.getString("dimension")
                val semanticKey = row.getString("semanticKey")
                assertEquals("terminal source membership for " + semanticKey,
                    SemanticState.EVIDENCE_UNRESOLVED, capabilityState(exercise, dimension))
                val terminalIntent = V3SelectionIntent(pdStrengthContext = terminalProbe(exercise))
                val terminalDecision = V3SelectionEngine.evaluate(exercise, athlete, environment, terminalIntent)
                assertTrue("terminal rejection missing for " + semanticKey,
                    V3SelectionRejection.TERMINAL_CANONICAL_STATE in terminalDecision.reasons)
                assertFalse("terminal semantic key became eligible: " + semanticKey, terminalDecision.eligible)
                if (dimension == "strengthRequirement") {
                    assertEquals("strength terminal resolver for " + semanticKey,
                        "TERMINAL_CANONICAL_STATE",
                        V3PdStrengthResolver.evaluate(exercise, terminalProbe(exercise)).state.name)
                }
                assertEquals("TERMINAL_CANONICAL_STATE", expectedState)
                assertEquals("TERMINAL_CANONICAL_STATE", expectedReason)
                executedTerminal++
                assertTrue("duplicate terminal key " + semanticKey, coveredTerminal.add(semanticKey))
            }
        }
        assertEquals("every PD canonical record traversed", 441, coveredPd.size)
        assertEquals("every terminal semantic key traversed", 48, coveredTerminal.size)
        assertEquals(manifest.getInt("plannedPdScenarios"), executedPd)
        assertEquals(manifest.getInt("plannedTerminalScenarios"), executedTerminal)

        val equipmentCounts = verifyEquipment(byId.values.toList())
        val skillCounts = verifySkills(byId.values.toList())
        val report = JSONObject()
            .put("candidateSha256", manifest.getString("candidateSha256"))
            .put("planned", scenarios.length() + equipmentCounts.first + skillCounts.first)
            .put("executed", executedPd + executedTerminal + equipmentCounts.second + skillCounts.second)
            .put("pdPlanned", manifest.getInt("plannedPdScenarios"))
            .put("pdExecuted", executedPd)
            .put("pdCanonicalCovered", coveredPd.size)
            .put("terminalPlanned", manifest.getInt("plannedTerminalScenarios"))
            .put("terminalExecuted", executedTerminal)
            .put("terminalCanonicalCovered", coveredTerminal.size)
            .put("equipmentPlanned", equipmentCounts.first)
            .put("equipmentExecuted", equipmentCounts.second)
            .put("skillPlanned", skillCounts.first)
            .put("skillExecuted", skillCounts.second)
            .put("progressionRelationsCovered", skillCounts.third)
        val out = File(root, "build/step8/execution-report.json")
        out.parentFile?.mkdirs()
        out.writeText(report.toString(2) + "\n")
        assertEquals("planned must equal executed", report.getInt("planned"), report.getInt("executed"))
    }

    private fun assertEngineReflectsPdOutcome(id: String, expected: String, decision: V3Eligibility) {
        when (expected) {
            "EXERCISE_REQUIREMENT_UNRESOLVED" -> {
                assertTrue(id, V3SelectionRejection.PD_STRENGTH_CONTEXT_INSUFFICIENT in decision.reasons ||
                    V3SelectionRejection.CAPACITY_ENVELOPE_MISMATCH in decision.reasons)
            }
            "ATHLETE_CAPABILITY_UNKNOWN" ->
                assertTrue(id, V3SelectionRejection.ATHLETE_CAPABILITY_UNKNOWN in decision.reasons)
            "INVALID_PRESCRIPTION_CONTEXT" ->
                assertTrue(id, V3SelectionRejection.PD_STRENGTH_CONTEXT_INVALID in decision.reasons)
            "INELIGIBLE" ->
                assertTrue(id, V3SelectionRejection.CAPABILITY_INSUFFICIENT in decision.reasons)
            "ELIGIBLE" -> {
                assertFalse(id, V3SelectionRejection.PD_STRENGTH_CONTEXT_INSUFFICIENT in decision.reasons)
                assertFalse(id, V3SelectionRejection.PD_STRENGTH_CONTEXT_INVALID in decision.reasons)
                assertFalse(id, V3SelectionRejection.CAPACITY_ENVELOPE_MISMATCH in decision.reasons)
                assertFalse(id, V3SelectionRejection.ATHLETE_CAPABILITY_UNKNOWN in decision.reasons)
            }
            else -> fail("unexpected PD expected state " + expected)
        }
    }

    private fun capabilityState(x: ExerciseV3, dimension: String): SemanticState = when (dimension) {
        "strengthRequirement" -> x.capabilities.strengthRequirement.state
        "technicalDifficulty" -> x.capabilities.technicalDifficulty.state
        "mobilityRequirement" -> x.capabilities.mobilityRequirement.state
        "balanceCoordinationRequirement" -> x.capabilities.balanceCoordinationRequirement.state
        else -> throw AssertionError("unknown terminal capability dimension " + dimension)
    }

    private fun parseContext(o: JSONObject?): V3PdStrengthContext? {
        if (o == null) return null
        val pObj = o.optJSONObject("prescription")
        val prescription = if (pObj == null) null else {
            val resistance = pObj.optJSONObject("resistance")
            V3PrescriptionContext(
                CanonicalExerciseId(pObj.getString("exerciseId")),
                ResistanceSource.valueOf(pObj.getString("resistanceSource")),
                parseWork(pObj.getJSONObject("work")),
                resistance?.let { quantified(it) }
            )
        }
        val eObj = o.optJSONObject("athleteCapacity")
        val envelope = if (eObj == null) null else {
            val points = eObj.getJSONArray("points")
            V3AthleteExerciseCapacityEnvelope((0 until points.length()).map { i ->
                val point = points.getJSONObject(i)
                V3CapacityEnvelopePoint(
                    CanonicalExerciseId(point.getString("exerciseId")),
                    ResistanceSource.valueOf(point.getString("resistanceSource")),
                    parseWork(point.getJSONObject("work")),
                    point.optJSONObject("maximumFeasibleLoad")?.let { quantified(it) },
                    point.optJSONObject("minimumRequiredAssistance")?.let { quantified(it) }
                )
            })
        }
        return V3PdStrengthContext(prescription, envelope)
    }

    private fun quantified(o: JSONObject) =
        V3QuantifiedResistance(o.getDouble("value"), InputUnit.valueOf(o.getString("unit")))

    private fun parseWork(o: JSONObject) = V3WorkDimensions(
        number(o, "reps"), number(o, "durationSeconds"), number(o, "distanceMeters"),
        number(o, "intervals"), string(o, "variation"), string(o, "tempo"),
        string(o, "rangeOfMotion"), string(o, "laterality")
    )

    private fun number(o: JSONObject, key: String): Double? =
        if (!o.has(key) || o.isNull(key)) null else o.getDouble(key)

    private fun string(o: JSONObject, key: String): String? =
        if (!o.has(key) || o.isNull(key)) null else o.getString(key)

    private fun terminalProbe(x: ExerciseV3): V3PdStrengthContext {
        val source = when (x.resistance.source.value) {
            ResistanceSource.ASSISTED -> ResistanceSource.ASSISTED
            else -> ResistanceSource.EXTERNAL_LOAD
        }
        val unit = if (source == ResistanceSource.ASSISTED)
            x.resistance.assistanceUnit.value ?: InputUnit.KILOGRAMS
        else x.resistance.loadUnit.value ?: InputUnit.KILOGRAMS
        val work = V3WorkDimensions()
        val p = V3PrescriptionContext(x.id, source, work, V3QuantifiedResistance(1.0, unit))
        val point = if (source == ResistanceSource.ASSISTED)
            V3CapacityEnvelopePoint(x.id, source, work,
                minimumRequiredAssistance = V3QuantifiedResistance(1.0, unit))
        else V3CapacityEnvelopePoint(x.id, source, work,
                maximumFeasibleLoad = V3QuantifiedResistance(1.0, unit))
        return V3PdStrengthContext(p, V3AthleteExerciseCapacityEnvelope(listOf(point)))
    }

    private fun fullyEquipped(x: ExerciseV3) = V3SelectionEnvironment(
        equipment = x.equipment.equipment + x.equipment.alternatives + x.equipment.optional + x.equipment.assistance,
        assistanceAvailable = true, externalLoadAvailable = true
    )

    /** Returns planned, executed. The oracle below is independent of engine output. */
    private fun verifyEquipment(exercises: List<ExerciseV3>): Pair<Int, Int> {
        var planned = 0
        var executed = 0
        for (x in exercises) {
            val r = x.equipment
            val unknown = r.informationState != EquipmentInformationState.KNOWN ||
                r.sourceState != SemanticState.KNOWN || r.mode == EquipmentRequirementMode.UNKNOWN
            val all = fullyEquipped(x)
            val availableUnavailable = !unknown && r.mode == EquipmentRequirementMode.EXTERNAL_LOAD && !r.externalLoadSupport
            checkEquipment(x, all, unresolved = unknown, unavailable = availableUnavailable)
            planned++; executed++
            val none = V3SelectionEnvironment()
            val noneUnavailable = if (unknown) false else when (r.mode) {
                EquipmentRequirementMode.NONE, EquipmentRequirementMode.OPTIONAL -> false
                EquipmentRequirementMode.REQUIRED, EquipmentRequirementMode.ONE_OF,
                EquipmentRequirementMode.ASSISTED_BY, EquipmentRequirementMode.EXTERNAL_LOAD -> true
                EquipmentRequirementMode.UNKNOWN -> false
            }
            checkEquipment(x, none, unresolved = unknown, unavailable = noneUnavailable)
            planned++; executed++
            if (!unknown && r.mode == EquipmentRequirementMode.REQUIRED && r.equipment.size > 1) {
                val partial = V3SelectionEnvironment(equipment = r.equipment.dropLast(1).toSet(),
                    assistanceAvailable = true, externalLoadAvailable = true)
                checkEquipment(x, partial, unresolved = false, unavailable = true)
                planned++; executed++
            }
        }
        return planned to executed
    }

    private fun checkEquipment(x: ExerciseV3, env: V3SelectionEnvironment,
                               unresolved: Boolean, unavailable: Boolean) {
        val actual = V3SelectionEngine.evaluate(x, athlete, env, V3SelectionIntent()).reasons
        assertEquals("equipment unresolved for " + x.id.value, unresolved,
            V3SelectionRejection.EQUIPMENT_UNRESOLVED in actual)
        assertEquals("equipment availability for " + x.id.value, unavailable,
            V3SelectionRejection.EQUIPMENT_UNAVAILABLE in actual)
    }

    /** Returns planned, executed, direct progression/regression relations covered. */
    private fun verifySkills(exercises: List<ExerciseV3>): Triple<Int, Int, Int> {
        var planned = 0
        var executed = 0
        var progression = 0
        for (x in exercises) {
            for (relation in x.skillRelations) {
                val intent = if (relation.targetType == SkillTargetType.FAMILY)
                    V3SelectionIntent(skillFamilyIds = setOf(relation.targetId))
                else V3SelectionIntent(skillNodeIds = setOf(relation.targetId))
                val reasons = V3SelectionEngine.evaluate(x, athlete, fullyEquipped(x), intent).reasons
                assertFalse("skill relation mismatch for " + x.id.value + "/" + relation.targetId.value,
                    V3SelectionRejection.SKILL_MISMATCH in reasons)
                planned++; executed++
                if (relation.relationType == SkillRelationType.DIRECT_PROGRESSION ||
                    relation.relationType == SkillRelationType.DIRECT_REGRESSION) progression++
            }
            if (x.skillRelations.isNotEmpty()) {
                val miss = SkillTargetId("__step8_nonmatching_target__")
                val intent = V3SelectionIntent(skillFamilyIds = setOf(miss), skillNodeIds = setOf(miss))
                val reasons = V3SelectionEngine.evaluate(x, athlete, fullyEquipped(x), intent).reasons
                assertTrue("skill non-match not rejected for " + x.id.value,
                    V3SelectionRejection.SKILL_MISMATCH in reasons)
                planned++; executed++
            }
        }
        return Triple(planned, executed, progression)
    }

    private fun repositoryRoot(): File {
        var current: File? = File(".").canonicalFile
        while (current != null) {
            if (File(current, "exercise-db-canonical/exercise-db-v3-final-candidate.json").isFile) return current
            current = current.parentFile
        }
        throw AssertionError("repository root not found from test working directory")
    }

    private fun fixture(id: String) = ExerciseV3(
        CanonicalExerciseId(id),
        EquipmentRequirement(EquipmentInformationState.KNOWN, EquipmentRequirementMode.NONE),
        ResistanceSpec(SemanticValue.known(ResistanceSource.BODYWEIGHT)),
        CapabilityRequirements(high, high, high, high),
        ExerciseCategoryV3("PUSH"),
        movementPatterns = SemanticValue.known(setOf(MovementPatternV3("HORIZONTAL_PUSH"))),
        prescription = ExercisePrescription(listOf(PrescribedValue(PrescriptionDimension.REPS))),
        version = V3Version("3.7")
    )

    @Test fun identicalInputsYieldIdenticalResults() {
        val x = fixture("stable")
        val env = V3SelectionEnvironment()
        val intent = V3SelectionIntent()
        assertEquals(V3SelectionEngine.evaluate(x, athlete, env, intent),
            V3SelectionEngine.evaluate(x, athlete, env, intent))
    }

    @Test fun alternatingContextsDoNotLeak() {
        val x = fixture("isolated")
        val a = V3SelectionIntent(categories = setOf(ExerciseCategoryV3("PUSH")))
        val b = V3SelectionIntent(categories = setOf(ExerciseCategoryV3("PULL")))
        val env = V3SelectionEnvironment()
        val first = V3SelectionEngine.evaluate(x, athlete, env, a)
        val middle = V3SelectionEngine.evaluate(x, athlete, env, b)
        val last = V3SelectionEngine.evaluate(x, athlete, env, a)
        assertEquals(first, last)
        assertTrue(first.eligible)
        assertFalse(middle.eligible)
        assertTrue(V3SelectionRejection.CATEGORY_MISMATCH in middle.reasons)
    }

    @Test fun poolOrderAndEmptyPoolClassificationAreExplicit() {
        val a = fixture("a")
        val z = fixture("z")
        val pool = V3SelectionEngine.pool(listOf(z, a), athlete, V3SelectionEnvironment(), V3SelectionIntent())
        assertEquals(listOf("a", "z"), pool.eligible.map { it.id.value })
        assertEquals(V3PoolStatus.VALID_POOL, pool.status)
        assertEquals(V3PoolStatus.LEGITIMATE_ZERO_POOL,
            V3SelectionEngine.pool(emptyList(), athlete, V3SelectionEnvironment(), V3SelectionIntent(), true).status)
        assertEquals(V3PoolStatus.INVALID_ZERO_POOL,
            V3SelectionEngine.pool(emptyList(), athlete, V3SelectionEnvironment(), V3SelectionIntent(), false).status)
    }
}
