package com.hybridfitnesscoach.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentDimension
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentDerivation
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentLevel
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentObservation
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentProvenance
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentRevision
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentTarget
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentVerification
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentPolicyDecision
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentPolicyRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class V3AssessmentPersistenceTest {
    @Test fun revisionsRoundTripAcrossAppStoreRecreationAndKeepSupersededRows() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("hybrid_fitness_store", Context.MODE_PRIVATE).edit().clear().commit()
        val target = V3AssessmentTarget(V3AssessmentDimension.STRENGTH)
        val firstStore = AppStore(context)

        assertTrue(firstStore.saveV3Assessment(target, V3AssessmentLevel.LOW, assessedAtEpochMs = 100L))
        val firstId = firstStore.loadV3AssessmentHistory().single().id
        assertTrue(firstStore.saveV3Assessment(target, V3AssessmentLevel.HIGH, assessedAtEpochMs = 200L))

        val restored = AppStore(context).loadV3AssessmentSnapshot(profileRevision = 12L)
        assertEquals(2, restored.history.size)
        assertEquals(firstId, restored.history.first().id)
        assertEquals(V3AssessmentLevel.HIGH, restored.current(target)?.level)
        assertEquals(firstId, restored.current(target)?.supersedesId)
    }

    @Test fun unknownAnswerIsPersistedAsUnknownWithoutAValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("hybrid_fitness_store", Context.MODE_PRIVATE).edit().clear().commit()
        val target = V3AssessmentTarget(V3AssessmentDimension.MOBILITY)

        assertTrue(AppStore(context).saveV3Assessment(target, null, assessedAtEpochMs = null))

        val row = AppStore(context).loadV3AssessmentHistory().single()
        assertNull(row.level)
        assertNull(row.assessedAtEpochMs)
    }

    @Test fun directAndDerivedProvenanceRoundTripWithoutReclassification() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("hybrid_fitness_store", Context.MODE_PRIVATE).edit().clear().commit()
        val store = AppStore(context)
        val directTarget = V3AssessmentTarget(V3AssessmentDimension.HORIZONTAL_PUSH)
        val direct = V3AssessmentRevision(
            "direct-1", directTarget, V3AssessmentLevel.MODERATE, V3AssessmentProvenance.DIRECT_ASSESSMENT,
            V3AssessmentVerification.OBSERVED_VALID, 100L, 100L, "push-protocol", "2", "G1-RUBRIC-0.1-DRAFT",
            V3AssessmentObservation("push_up", "inclined", "bench support", "controlled range, no compensation"), null
        )
        val derivedTarget = V3AssessmentTarget(V3AssessmentDimension.HINGE)
        val derived = V3AssessmentRevision(
            "derived-1", derivedTarget, V3AssessmentLevel.LOW, V3AssessmentProvenance.VERIFIED_DERIVATION,
            V3AssessmentVerification.DERIVED_VALID, 110L, 110L, "rule-hinge", "3", "G1-RUBRIC-0.1-DRAFT",
            null, null, derivation = V3AssessmentDerivation("rule-hinge", "3", mapOf("source" to "assessment-7"), "LOW")
        )

        assertTrue(store.appendV3AssessmentRevision(direct))
        assertTrue(store.appendV3AssessmentRevision(derived))
        val restored = AppStore(context).loadV3AssessmentHistory()

        assertEquals(V3AssessmentProvenance.DIRECT_ASSESSMENT, restored[0].provenance)
        assertEquals(direct.observation, restored[0].observation)
        assertEquals(V3AssessmentProvenance.VERIFIED_DERIVATION, restored[1].provenance)
        assertEquals(derived.derivation, restored[1].derivation)
    }

    @Test fun invalidationAppendsUnknownEventAndDoesNotDeleteEvidenceHistory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("hybrid_fitness_store", Context.MODE_PRIVATE).edit().clear().commit()
        val store = AppStore(context)
        val target = V3AssessmentTarget(V3AssessmentDimension.STRENGTH)
        assertTrue(store.saveV3Assessment(target, V3AssessmentLevel.HIGH, assessedAtEpochMs = 100L))

        assertTrue(store.invalidateV3Assessment(target, "protocol superseded", recordedAtEpochMs = 200L))

        val snapshot = AppStore(context).loadV3AssessmentSnapshot(profileRevision = 0L)
        assertEquals(2, snapshot.history.size)
        assertNull(snapshot.current(target)?.level)
        assertEquals(V3AssessmentVerification.INVALIDATED, snapshot.current(target)?.verification)
        assertEquals(V3AssessmentPolicyDecision.ASSESSMENT_REQUIRED,
            V3AssessmentPolicyRegistry.decision(target, snapshot.current(target)))
    }
}
