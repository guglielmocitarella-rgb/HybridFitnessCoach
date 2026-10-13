package com.hybridfitnesscoach.app.domain.v3

import com.hybridfitnesscoach.app.AthleteProfile
import com.hybridfitnesscoach.app.TrainingLocation
import com.hybridfitnesscoach.app.TrainingLocationKind
import com.hybridfitnesscoach.app.EquipmentId as NativeEquipmentId
import java.util.concurrent.Executor
import org.junit.Assert.*
import org.junit.Test

class V3RuntimeRankingTest {
    @Test fun realDatabaseRanksOnWorkerSnapshotsWithoutReloadAndRejectsStaleContext() {
        val pending = mutableListOf<Runnable>()
        var loads = 0
        val runtime = V3RuntimeComponent({
            loads++
            V3RankingRealFixtures.frozenCanonical()
        }, Executor { pending += it })
        val home = TrainingLocation("home", "Casa", "🏠", TrainingLocationKind.HOME, setOf(NativeEquipmentId.BODYWEIGHT))
        val assessed = V3AssessedValue.direct(CapabilityLevel.VERY_HIGH)
        val capabilities = V3NativeAthleteCapabilityContract(transversal = V3TransversalAssessments(assessed, assessed, assessed, assessed))
        val firstAthlete = V3NativeAthleteContextAdapter.fromNative(AthleteProfile(goal = "Forza"), emptyList()).copy(capabilities = capabilities)
        runtime.activate(home, firstAthlete)
        pending.removeAt(0).run()
        val loaded = (runtime.state.value as V3RuntimeState.Ready).loadResult
        val first = runtime.rank(V3RankingConfig(1_800_000_000_000L))
        assertFalse(first.isDone)
        val gym = home.copy(id = "gym", kind = TrainingLocationKind.GYM, capabilities = setOf(NativeEquipmentId.BARBELL))
        val secondAthlete = V3NativeAthleteContextAdapter.fromNative(AthleteProfile(goal = "Fitness generale"), emptyList(), readiness = 20).copy(capabilities = capabilities)
        runtime.activate(gym, secondAthlete)
        pending.removeAt(0).run()
        assertEquals("home", first.get().locationId)
        assertFalse(runtime.isCurrent(first.get()))
        val second = runtime.rank(V3RankingConfig(1_800_000_000_000L))
        pending.removeAt(0).run()
        assertTrue(runtime.isCurrent(second.get()))
        assertEquals("gym", second.get().locationId)
        assertEquals(1087, second.get().evaluation.decisions.size)
        assertTrue(first.get().evaluation.orderedEligible.isNotEmpty())
        assertNotEquals(first.get().evaluation, second.get().evaluation)
        assertSame(loaded, (runtime.state.value as V3RuntimeState.Ready).loadResult)
        assertEquals(1, loads)
        assertEquals(V3PersonalizedRanking.evaluate(loaded,
            (runtime.state.value as V3RuntimeState.Ready).environment.environment,
            secondAthlete, V3RankingConfig(1_800_000_000_000L)), second.get().evaluation)
    }

    @Test fun unavailableRuntimeDoesNotReturnAnEmptySuccessfulRanking() {
        val runtime = V3RuntimeComponent({ error("Must not load implicitly") })
        assertTrue(runtime.rank(V3RankingConfig(0)).isCompletedExceptionally)
        assertSame(V3RuntimeState.NotStarted, runtime.state.value)
    }
}
