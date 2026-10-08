package com.hybridfitnesscoach.app.domain.v3

import org.junit.Assert.*
import org.junit.Test

/**
 * Step 8B0: independent observable invariants for the actual V3 selector.
 * This is a focused harness component, not the completed massive matrix.
 */
class V3MassiveSelectionHarnessTest {
    private val low = SemanticValue.known(CapabilityLevel.LOW)
    private val athlete = V3AthleteCapabilities(low, low, low, low)
    private fun exercise(id: String) = ExerciseV3(
        CanonicalExerciseId(id),
        EquipmentRequirement(EquipmentInformationState.KNOWN, EquipmentRequirementMode.NONE),
        ResistanceSpec(SemanticValue.known(ResistanceSource.BODYWEIGHT)),
        CapabilityRequirements(low, low, low, low),
        ExerciseCategoryV3("PUSH"),
        movementPatterns = SemanticValue.known(setOf(MovementPatternV3("HORIZONTAL_PUSH"))),
        prescription = ExercisePrescription(listOf(PrescribedValue(PrescriptionDimension.REPS))),
        version = V3Version("3.7")
    )

    @Test fun identicalInputsYieldIdenticalResults() {
        val x = exercise("stable")
        val environment = V3SelectionEnvironment()
        val intent = V3SelectionIntent()
        assertEquals(
            V3SelectionEngine.evaluate(x, athlete, environment, intent),
            V3SelectionEngine.evaluate(x, athlete, environment, intent)
        )
    }

    @Test fun alternatingContextsDoNotLeak() {
        val x = exercise("isolated")
        val a = V3SelectionIntent(categories = setOf(ExerciseCategoryV3("PUSH")))
        val b = V3SelectionIntent(categories = setOf(ExerciseCategoryV3("PULL")))
        val environment = V3SelectionEnvironment()
        val first = V3SelectionEngine.evaluate(x, athlete, environment, a)
        val middle = V3SelectionEngine.evaluate(x, athlete, environment, b)
        val last = V3SelectionEngine.evaluate(x, athlete, environment, a)
        assertEquals(first, last)
        assertTrue(first.eligible)
        assertFalse(middle.eligible)
        assertTrue(V3SelectionRejection.CATEGORY_MISMATCH in middle.reasons)
    }

    @Test fun poolIdsAreUniqueAndSorted() {
        val result = V3SelectionEngine.pool(
            listOf(exercise("z"), exercise("a")),
            athlete, V3SelectionEnvironment(), V3SelectionIntent()
        )
        val ids = result.eligible.map { it.id.value }
        assertEquals(ids.distinct(), ids)
        assertEquals(ids.sorted(), ids)
        assertEquals(V3PoolStatus.VALID_POOL, result.status)
    }

    @Test fun emptyPoolClassificationIsExplicit() {
        val environment = V3SelectionEnvironment()
        val intent = V3SelectionIntent()
        assertEquals(V3PoolStatus.LEGITIMATE_ZERO_POOL,
            V3SelectionEngine.pool(emptyList(), athlete, environment, intent, true).status)
        assertEquals(V3PoolStatus.INVALID_ZERO_POOL,
            V3SelectionEngine.pool(emptyList(), athlete, environment, intent, false).status)
    }
}
