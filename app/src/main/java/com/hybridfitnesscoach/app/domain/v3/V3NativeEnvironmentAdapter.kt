package com.hybridfitnesscoach.app.domain.v3

import com.hybridfitnesscoach.app.EquipmentId as NativeEquipmentId
import com.hybridfitnesscoach.app.TrainingLocation

/** A three-valued availability signal. UNKNOWN is never promoted to true. */
enum class V3Availability { AVAILABLE, UNAVAILABLE, UNKNOWN }

data class V3NativeEnvironmentResult(
    val locationId: String?,
    val environment: V3SelectionEnvironment,
    val unmappedNativeEquipment: Set<NativeEquipmentId>,
    val equipmentKnowledge: V3Availability,
    val assistanceKnowledge: V3Availability = V3Availability.UNKNOWN,
    val externalLoadKnowledge: V3Availability = V3Availability.UNKNOWN
)

/**
 * Exact stable-token bridge; no localized labels or equipment substitutions.
 * A location's capabilities are an explicit inventory, not proof that a helper,
 * assisted machine, or load-adjustment facility is usable.
 */
object V3NativeEnvironmentAdapter {
    private val exactTokens: Map<NativeEquipmentId, EquipmentId> = mapOf(
        NativeEquipmentId.BODYWEIGHT to EquipmentId("bodyweight"),
        NativeEquipmentId.AB_WHEEL to EquipmentId("ab_wheel"),
        NativeEquipmentId.BARBELL to EquipmentId("barbell"),
        NativeEquipmentId.BATTLE_ROPES to EquipmentId("battle_ropes"),
        NativeEquipmentId.BENCH to EquipmentId("bench"),
        NativeEquipmentId.BOSU to EquipmentId("bosu"),
        NativeEquipmentId.BOX to EquipmentId("box"),
        NativeEquipmentId.CABLE to EquipmentId("cable"),
        NativeEquipmentId.DIP_BARS to EquipmentId("dip_bars"),
        NativeEquipmentId.DUMBBELL to EquipmentId("dumbbell"),
        NativeEquipmentId.EZ_BAR to EquipmentId("ez_bar"),
        NativeEquipmentId.FOAM_ROLLER to EquipmentId("foam_roller"),
        NativeEquipmentId.KETTLEBELL to EquipmentId("kettlebell"),
        NativeEquipmentId.LEG_PRESS to EquipmentId("leg_press"),
        NativeEquipmentId.MACHINE to EquipmentId("machine"),
        NativeEquipmentId.MEDICINE_BALL to EquipmentId("medicine_ball"),
        NativeEquipmentId.PARALLETTES to EquipmentId("parallettes"),
        NativeEquipmentId.PULL_UP_BAR to EquipmentId("pull_up_bar"),
        NativeEquipmentId.RESISTANCE_BAND to EquipmentId("resistance_band"),
        NativeEquipmentId.RINGS to EquipmentId("rings"),
        NativeEquipmentId.ROPE to EquipmentId("rope"),
        NativeEquipmentId.STABILITY_BALL to EquipmentId("stability_ball"),
        NativeEquipmentId.TOWEL to EquipmentId("towel")
    )

    /**
     * Only tokens explicitly present in the supplied V3 vocabulary are admitted.
     * The caller must obtain that vocabulary from the installed V3 data, not
     * infer it from native equipment labels.
     */
    fun fromLocation(
        location: TrainingLocation?,
        v3EquipmentVocabulary: Set<EquipmentId>
    ): V3NativeEnvironmentResult {
        if (location == null) return V3NativeEnvironmentResult(
            null, V3SelectionEnvironment(), emptySet(), V3Availability.UNKNOWN
        )
        val recognized = location.capabilities.mapNotNull { native ->
            exactTokens[native]?.takeIf { it.value == native.token && it in v3EquipmentVocabulary }
        }.toSortedSet(compareBy { it.value })
        val unmapped = location.capabilities.filterTo(linkedSetOf()) { native ->
            exactTokens[native]?.takeIf { it.value == native.token && it in v3EquipmentVocabulary } == null
        }
        return V3NativeEnvironmentResult(
            location.id,
            V3SelectionEnvironment(
                equipment = recognized,
                assistanceAvailable = false,
                externalLoadAvailable = false
            ),
            unmapped,
            if (unmapped.isEmpty()) V3Availability.AVAILABLE else V3Availability.UNKNOWN
        )
    }
}
