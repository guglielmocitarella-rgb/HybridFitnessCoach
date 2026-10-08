package com.hybridfitnesscoach.app.domain.v3

import com.hybridfitnesscoach.app.EquipmentId
import com.hybridfitnesscoach.app.TrainingLocation
import com.hybridfitnesscoach.app.TrainingLocationKind
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class V3RuntimeComponentTest {
    @Test
    fun activationLoadsOnceOfferedProfilesAndKeepsLoadingDistinct() {
        var loads = 0
        val pending = mutableListOf<Runnable>()
        val component = V3RuntimeComponent(
            assetReader = { loads++; emptyLoadedDatabase() },
            executor = Executor { task -> pending.add(task) }
        )

        component.activate(location("home"))
        assertSame(V3RuntimeState.Loading, component.state.value)
        component.activate(location("gym"))
        assertEquals(0, loads)
        assertEquals(1, pending.size)

        pending.single().run()
        val ready = component.state.value as V3RuntimeState.Ready
        assertEquals("gym", ready.environment.locationId)
        assertEquals(1, loads)
    }

    @Test
    fun changingProfileRebuildsEnvironmentWithoutReloadingDatabase() {
        var loads = 0
        val component = V3RuntimeComponent(
            assetReader = { loads++; emptyLoadedDatabase() },
            executor = Executor { task -> task.run() }
        )

        component.activate(location("home"))
        val loaded = (component.state.value as V3RuntimeState.Ready).loadResult
        component.activate(location("outdoor"))

        val ready = component.state.value as V3RuntimeState.Ready
        assertSame(loaded, ready.loadResult)
        assertEquals("outdoor", ready.environment.locationId)
        assertEquals(1, loads)
    }

    @Test
    fun failedLoadStaysAnErrorAndDoesNotCreateAnEnvironmentOrRetry() {
        var loads = 0
        val failure = V3AssetValidationException("invalid V3 asset")
        val component = V3RuntimeComponent(
            assetReader = { loads++; throw failure },
            executor = Executor { task -> task.run() }
        )

        component.activate(location("home"))
        component.activate(location("gym"))

        val state = component.state.value
        assertTrue(state is V3RuntimeState.Failed)
        assertSame(failure, (state as V3RuntimeState.Failed).cause)
        assertEquals(1, loads)
    }

    @Test
    fun recreatedComponentGetsItsOwnSingleLoad() {
        var loads = 0
        val reader = { loads++; emptyLoadedDatabase() }
        val executor = Executor { task -> task.run() }
        val first = V3RuntimeComponent(reader, executor)
        val recreated = V3RuntimeComponent(reader, executor)

        first.activate(location("home"))
        recreated.activate(location("home"))

        assertEquals(2, loads)
        assertTrue(first.state.value is V3RuntimeState.Ready)
        assertTrue(recreated.state.value is V3RuntimeState.Ready)
    }

    private fun location(id: String) = TrainingLocation(
        id = id,
        name = id,
        icon = "",
        kind = TrainingLocationKind.HOME,
        capabilities = setOf(EquipmentId.BODYWEIGHT)
    )

    private fun emptyLoadedDatabase() = V3LoadResult(
        records = emptyList(),
        audit = V3LoadAudit(0, 0, 0, 0, 0, 0, 0, 0, 0, emptySet())
    )
}
