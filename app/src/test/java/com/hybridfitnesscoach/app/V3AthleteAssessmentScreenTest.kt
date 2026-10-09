package com.hybridfitnesscoach.app

import android.app.Application
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentDimension
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentLevel
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentProvenance
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentTarget
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentVerification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class V3AthleteAssessmentScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = RuntimeEnvironment.getApplication()

    @Before fun clearStore() {
        context.getSharedPreferences("hybrid_fitness_store", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun launch(store: AppStore, onClose: () -> Unit = {}) {
        compose.setContent { MaterialTheme { V3AthleteAssessmentScreen(store, onClose, {}) } }
    }

    private val assessmentDimensions = listOf(
        V3AssessmentDimension.STRENGTH, V3AssessmentDimension.TECHNICAL,
        V3AssessmentDimension.MOBILITY, V3AssessmentDimension.BALANCE_COORDINATION,
        V3AssessmentDimension.HORIZONTAL_PUSH, V3AssessmentDimension.VERTICAL_PUSH,
        V3AssessmentDimension.HORIZONTAL_PULL, V3AssessmentDimension.VERTICAL_PULL,
        V3AssessmentDimension.SQUAT, V3AssessmentDimension.HINGE,
        V3AssessmentDimension.CORE, V3AssessmentDimension.ISOMETRIC
    )

    private fun advanceToSkill(store: AppStore, startPage: Int = 0) {
        assessmentDimensions.drop(startPage).forEach { dimension ->
            val current = store.loadV3AssessmentSnapshot(0L).current(V3AssessmentTarget(dimension))
            if (current != null && current.level != null && current.verification != V3AssessmentVerification.INVALIDATED) {
                compose.onNodeWithTag("v3-assessment-save").performScrollTo().performClick()
            } else {
                compose.onNodeWithTag("v3-assessment-unknown-option").performScrollTo().performClick()
            }
            compose.waitForIdle()
        }
    }

    private fun chooseFamily(name: String) {
        compose.onNodeWithTag("v3-skill-family-selector").performScrollTo().performClick()
        compose.onNodeWithText(name, useUnmergedTree = true).performClick()
        compose.waitForIdle()
    }

    @Test fun skipCompletesWithoutSelectingANodeOrCreatingASkillAssessment() {
        val store = AppStore(context)
        var returnedToProfile = false
        launch(store) { returnedToProfile = true }

        advanceToSkill(store)
        compose.onNodeWithTag("v3-assessment-skip-skill").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Valutazione completata").assertIsDisplayed()
        assertEquals(12, store.loadV3AssessmentHistory().size)
        assertTrue(store.loadV3AssessmentHistory().none { it.target.dimension == V3AssessmentDimension.SKILL })
        compose.onNodeWithText("Nessuna skill valutata: la sezione è stata saltata per ora", substring = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Sconosciuta · Non so / non l'ho provata", substring = true)
            .performScrollTo().assertIsDisplayed()

        compose.onNodeWithTag("v3-assessment-finish").performScrollTo().performClick()
        assertTrue(returnedToProfile)
    }

    @Test fun nonSoIsAnExplicitAnswerAndAdvancesTheGuidedFlow() {
        val store = AppStore(context)
        launch(store)

        compose.onNodeWithTag("v3-assessment-unknown-option").performScrollTo().performClick()

        assertEquals(1, store.loadV3AssessmentHistory().size)
        val row = store.loadV3AssessmentSnapshot(0L)
            .current(V3AssessmentTarget(V3AssessmentDimension.STRENGTH))
        assertNull(row?.level)
        assertEquals(V3AssessmentVerification.UNKNOWN, row?.verification)
        compose.onNodeWithText("Competenza tecnica").performScrollTo().assertIsDisplayed()
    }

    @Test fun levelChoiceIsStoredAsDeclaredAndAdvances() {
        val store = AppStore(context)
        launch(store)

        compose.onNodeWithTag("v3-assessment-level-MODERATE").performScrollTo().performClick()
        compose.onNodeWithTag("v3-assessment-save").performScrollTo().performClick()

        val row = store.loadV3AssessmentSnapshot(0L).current(V3AssessmentTarget(V3AssessmentDimension.STRENGTH))
        assertEquals(V3AssessmentLevel.MODERATE, row?.level)
        assertEquals(V3AssessmentProvenance.SELF_REPORTED, row?.provenance)
        compose.onNodeWithText("Competenza tecnica").performScrollTo().assertIsDisplayed()
    }

    @Test fun unansweredCapabilityCanBeSkippedAndSummaryDistinguishesItFromUnknown() {
        val store = AppStore(context)
        launch(store)

        compose.onNodeWithTag("v3-assessment-skip-dimension").performScrollTo().performClick()
        assertNull(store.loadV3AssessmentSnapshot(0L).current(V3AssessmentTarget(V3AssessmentDimension.STRENGTH)))
        compose.onNodeWithText("Competenza tecnica").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("v3-assessment-unknown-option").performScrollTo().performClick()
        advanceToSkill(store, startPage = 2)
        compose.onNodeWithTag("v3-assessment-skip-skill").performScrollTo().performClick()

        compose.onNodeWithText("Forza: Non valutata", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Competenza tecnica: Sconosciuta", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun familySelectionFiltersProgressionsAndMissingDescriptionsAreExplicit() {
        val store = AppStore(context)
        launch(store)
        advanceToSkill(store)

        chooseFamily("Front lever")
        compose.onNodeWithTag("v3-skill-progression-option-0").performScrollTo().performClick()

        compose.onNodeWithText("Tuck front lever").assertIsDisplayed()
        compose.onNodeWithText("Dip assistite").assertDoesNotExist()
        compose.onNodeWithText("Descrizione tecnica dettagliata non presente nel catalogo", substring = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Il catalogo non riporta prerequisiti per questa progressione.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("front_tuck").assertDoesNotExist()
    }

    @Test fun selectedSkillUnknownIsSavedForItsOriginalNodeIdAndIsNotASectionSkip() {
        val store = AppStore(context)
        launch(store)
        advanceToSkill(store)

        chooseFamily("Front lever")
        compose.onNodeWithTag("v3-skill-progression-option-0").performScrollTo().performClick()
        compose.onNodeWithTag("v3-assessment-unknown-option").performScrollTo().performClick()

        val skillTarget = V3AssessmentTarget.skill("front_tuck")
        val row = store.loadV3AssessmentSnapshot(0L).current(skillTarget)
        assertNull(row?.level)
        assertEquals(V3AssessmentVerification.UNKNOWN, row?.verification)
        assertEquals(V3AssessmentProvenance.UNKNOWN, row?.provenance)
        compose.onNodeWithText("Front lever · Tuck front lever: Sconosciuta", substring = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("La sezione è stata saltata per ora").assertDoesNotExist()
    }

    @Test fun skippingDoesNotDeletePreviouslySavedSkillOrOtherAssessments() {
        val store = AppStore(context)
        assertTrue(store.saveV3Assessment(V3AssessmentTarget.skill("dip_2"), V3AssessmentLevel.MODERATE, 100L))
        assertTrue(store.saveV3Assessment(V3AssessmentTarget(V3AssessmentDimension.MOBILITY), V3AssessmentLevel.LOW, 200L))
        val originalSkillRow = store.loadV3AssessmentSnapshot(0L).current(V3AssessmentTarget.skill("dip_2"))
        launch(store)

        advanceToSkill(store)
        compose.onNodeWithTag("v3-assessment-skip-skill").performScrollTo().performClick()

        val snapshot = AppStore(context).loadV3AssessmentSnapshot(0L)
        assertEquals(originalSkillRow?.id, snapshot.current(V3AssessmentTarget.skill("dip_2"))?.id)
        assertEquals(V3AssessmentLevel.LOW, snapshot.current(V3AssessmentTarget(V3AssessmentDimension.MOBILITY))?.level)
        assertEquals(1, snapshot.currentRows().count { it.target.dimension == V3AssessmentDimension.SKILL })
        compose.onNodeWithText("Hai saltato per ora nuove risposte skill", substring = true)
            .performScrollTo().assertIsDisplayed()
    }

    @Test fun savedAnswersSurviveBackNavigationCanBeEditedAndReloaded() {
        val store = AppStore(context)
        assertTrue(store.saveV3Assessment(V3AssessmentTarget(V3AssessmentDimension.MOBILITY), V3AssessmentLevel.LOW, 50L))
        var assessmentOpen by mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                if (assessmentOpen) {
                    V3AthleteAssessmentScreen(store, onClose = { assessmentOpen = false }, onChanged = {})
                } else {
                    TextButton(onClick = { assessmentOpen = true }) { Text("Riapri valutazione") }
                }
            }
        }

        compose.onNodeWithTag("v3-assessment-level-MODERATE").performScrollTo().performClick()
        compose.onNodeWithTag("v3-assessment-save").performScrollTo().performClick()
        compose.onNodeWithText("Competenza tecnica").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Indietro").performScrollTo().performClick()
        compose.onNodeWithTag("v3-assessment-level-MODERATE").performScrollTo().assertIsSelected()

        compose.onNodeWithTag("v3-assessment-save").performScrollTo().performClick()
        advanceToSkill(store, startPage = 1)
        compose.onNodeWithTag("v3-assessment-skip-skill").performScrollTo().performClick()
        compose.onNodeWithTag("v3-assessment-modify").performScrollTo().performClick()
        compose.onNodeWithText("Forza").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("v3-assessment-level-MODERATE").performScrollTo().assertIsSelected()

        compose.onNodeWithTag("v3-assessment-level-HIGH").performScrollTo().performClick()
        compose.onNodeWithTag("v3-assessment-save").performScrollTo().performClick()
        assertEquals(V3AssessmentLevel.HIGH,
            AppStore(context).loadV3AssessmentSnapshot(0L).current(V3AssessmentTarget(V3AssessmentDimension.STRENGTH))?.level)
        assertEquals(V3AssessmentLevel.LOW,
            AppStore(context).loadV3AssessmentSnapshot(0L).current(V3AssessmentTarget(V3AssessmentDimension.MOBILITY))?.level)

        advanceToSkill(store, startPage = 1)
        compose.onNodeWithTag("v3-assessment-skip-skill").performScrollTo().performClick()
        compose.onNodeWithTag("v3-assessment-finish").performScrollTo().performClick()
        compose.onNodeWithText("Riapri valutazione").performClick()
        compose.onNodeWithTag("v3-assessment-level-HIGH").performScrollTo().assertIsSelected()
    }

    @Test fun fiveInternalLevelsKeepTheirContractValues() {
        assertEquals(
            listOf("VERY_LOW", "LOW", "MODERATE", "HIGH", "VERY_HIGH"),
            V3AssessmentLevel.entries.map { it.name }
        )
    }
}
