package com.hybridfitnesscoach.app

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.hybridfitnesscoach.app.domain.v3.*
import java.util.concurrent.Executor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** JVM Compose clicks/layout, plus real AppStore round trips; not a physical-device test. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class EquipmentEditorRegressionTest {
    @get:Rule val compose = createComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()
    @Before fun clearStore() {
        context.getSharedPreferences("hybrid_fitness_store", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun homeEditorRoundTrip() = roundTrip(trainingLocations.single { it.kind == TrainingLocationKind.HOME })
    @Test fun parkEditorRoundTrip() = roundTrip(trainingLocations.single { it.kind == TrainingLocationKind.OUTDOOR })
    @Test fun gymEditorRoundTrip() = roundTrip(trainingLocations.single { it.kind == TrainingLocationKind.GYM })
    @Test fun customEditorRoundTrip() = roundTrip(
        TrainingLocation("custom:regression", "Hotel", "📍", TrainingLocationKind.CUSTOM,
            setOf(EquipmentId.BODYWEIGHT, EquipmentId.PULL_UP_BAR))
    )

    private fun roundTrip(original: TrainingLocation) {
        val store = AppStore(context)
        val other = TrainingLocation("custom:other", "Altro luogo", "📍", TrainingLocationKind.CUSTOM,
            setOf(EquipmentId.BODYWEIGHT))
        assertTrue(store.saveTrainingLocations(listOf(original, other)))
        assertTrue(store.saveActiveTrainingLocationId(original.id))
        val profiles = mutableStateListOf<TrainingLocation>().apply { addAll(store.loadTrainingLocations()) }
        val active = mutableStateOf(original)
        var saves = 0
        var loads = 0
        val component = V3RuntimeComponent(
            assetReader = {
                loads++
                V3AssetLoader.loadAuthoritative(java.io.File(requireNotNull(System.getProperty("hfc.v3.asset"))).readText())
            },
            executor = Executor { it.run() },
            diagnosticsEnabled = { true }
        )
        compose.setContent {
            MaterialTheme {
                // Same bounded Column placement used by ProfileHub: the editor must overlay
                // Equipment's list rather than becoming another child below that list.
                Column(Modifier.fillMaxSize()) {
                    LaunchedEffect(active.value) { component.activate(active.value) }
                    Equipment(active.value, profiles,
                        select = { active.value = it },
                        addProfile = { fail("Edit must not create a profile") },
                        editProfile = { old, updated ->
                            assertEquals(original.id, old.id)
                            assertEquals(old.id, updated.id)
                            val index = profiles.indexOfFirst { it.id == old.id }
                            assertTrue(index >= 0)
                            profiles[index] = updated
                            assertTrue(store.saveTrainingLocations(profiles))
                            if (active.value.id == old.id) {
                                active.value = updated
                                assertTrue(store.saveActiveTrainingLocationId(updated.id))
                            }
                            saves++
                        },
                        deleteProfile = { fail("Edit must not delete a profile") })
                }
            }
        }
        lateinit var loadedBefore: V3LoadResult
        compose.runOnIdle { loadedBefore = (component.state.value as V3RuntimeState.Ready).loadResult }
        fun open() {
            compose.onAllNodesWithContentDescription("Modifica")[0].performClick()
            compose.onNodeWithText("Modifica profilo attrezzatura").assertIsDisplayed()
            compose.onNodeWithText("SALVA").assertIsDisplayed()
        }
        fun equipment(id: EquipmentId): SemanticsNodeInteraction {
            val tag = "equipment-option-" + id.token
            compose.onNodeWithTag("equipment-options").performScrollToNode(hasTestTag(tag))
            return compose.onNodeWithTag(tag, useUnmergedTree = true)
        }

        open()
        equipment(EquipmentId.BODYWEIGHT).assertIsOn().performClick().assertIsOff()
        compose.onNodeWithText("ANNULLA").performClick()
        compose.onNodeWithText("Modifica profilo attrezzatura").assertDoesNotExist()
        assertEquals(0, saves)
        assertEquals(listOf(original, other), AppStore(context).loadTrainingLocations())
        open()
        equipment(EquipmentId.BODYWEIGHT).assertIsOn()
        val added = profileEquipmentOptions(exercises).mapNotNull(EquipmentCatalog::fromDisplayName)
            .first { it !in original.capabilities }
        equipment(added).assertIsOff().performClick().assertIsOn()
        equipment(EquipmentId.BODYWEIGHT).performClick().assertIsOff()
        compose.onNodeWithText("SALVA").performClick()
        compose.onNodeWithText("Modifica profilo attrezzatura").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, saves)
            assertEquals(original.id, active.value.id)
            assertEquals(original.capabilities - EquipmentId.BODYWEIGHT + added, active.value.capabilities)
            val recreated = AppStore(context)
            val restored = recreated.loadTrainingLocations()
            assertEquals(2, restored.size)
            assertEquals(2, restored.map { it.id }.distinct().size)
            assertEquals(other, restored.single { it.id == other.id })
            assertEquals(active.value, restored.single { it.id == original.id })
            assertEquals(original.id, recreated.loadActiveTrainingLocationId())
            // V2 uses the edited native capabilities immediately.
            val bodyweight = Exercise("BW", "Core", "Corpo libero", "Principiante")
            val addedExercise = Exercise("Added", "Core", added.labelIt, "Principiante")
            assertFalse(locationCompatible(bodyweight, active.value))
            assertTrue(locationCompatible(addedExercise, active.value))
            val ready = component.state.value as V3RuntimeState.Ready
            assertEquals(original.id, ready.environment.locationId)
            val expected = V3NativeEnvironmentAdapter.fromLocation(active.value, loadedBefore)
            assertEquals(expected, ready.environment)
            assertSame(loadedBefore, ready.loadResult)
            assertFalse(EquipmentId.BODYWEIGHT in ready.environment.unmappedNativeEquipment)
            assertEquals(1, loads)
        }
        open()
        equipment(EquipmentId.BODYWEIGHT).assertIsOff()
        equipment(added).assertIsOn()
        compose.onNodeWithText("ANNULLA").performClick()
        assertEquals(1, saves)
    }
}
