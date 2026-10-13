package com.hybridfitnesscoach.app.domain.v3

/**
 * Equipment tokens observed in the validated, installed V3 exercise records.
 * No native labels, parallel catalog, or inferred equipment are admitted.
 */
internal object V3LoadedEquipmentVocabulary {
    fun from(load: V3LoadResult): Set<EquipmentId> = fromRecords(load.records)

    fun fromRecords(records: List<ExerciseV3>): Set<EquipmentId> =
        records.asSequence()
            .flatMap { exercise ->
                sequenceOf(
                    exercise.equipment.equipment,
                    exercise.equipment.alternatives,
                    exercise.equipment.optional,
                    exercise.equipment.assistance
                ).flatMap { it.asSequence() }
            }
            .toSortedSet(compareBy(EquipmentId::value))
}
