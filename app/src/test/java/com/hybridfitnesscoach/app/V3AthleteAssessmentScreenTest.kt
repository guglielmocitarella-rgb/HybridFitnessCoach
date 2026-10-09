package com.hybridfitnesscoach.app

import android.app.Application
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentDimension
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentTarget
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentLevel
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentProvenance
import org.junit.Assert.assertNull
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

    @Test fun nonSoIsAnExplicitAnswerAndAdvancesTheGuidedFlow() {
        val store = AppStore(context)
        compose.setContent { MaterialTheme { V3AthleteAssessmentScreen(store, {}, {}) } }

        compose.onNodeWithTag("v3-assessment-unknown-option").performScrollTo().performClick()
        compose.waitForIdle()
        org.junit.Assert.assertEquals(1, store.loadV3AssessmentHistory().size)
        assertNull(store.loadV3AssessmentSnapshot(0L).current(V3AssessmentTarget(V3AssessmentDimension.STRENGTH))?.level)
        compose.onNodeWithText("Competenza tecnica").performScrollTo().assertIsDisplayed()
    }

    @Test fun levelChoiceIsStoredAsDeclaredAndAdvances() {
        val store = AppStore(context)
        compose.setContent { MaterialTheme { V3AthleteAssessmentScreen(store, {}, {}) } }

        compose.onNodeWithTag("v3-assessment-level-MODERATE").performScrollTo().performClick()
        compose.onNodeWithText("Salva e continua").performScrollTo().performClick()

        val row = store.loadV3AssessmentSnapshot(0L).current(V3AssessmentTarget(V3AssessmentDimension.STRENGTH))
        org.junit.Assert.assertEquals(V3AssessmentLevel.MODERATE, row?.level)
        org.junit.Assert.assertEquals(V3AssessmentProvenance.SELF_REPORTED, row?.provenance)
        compose.onNodeWithText("Competenza tecnica").performScrollTo().assertIsDisplayed()
    }
}
