package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class EquipmentDomainContractTest {
    @Test fun everyCurrentDbEquipmentTokenHasCanonicalCapability() {
        val records=ExerciseDbV2.installedRecords()
        if(records.isEmpty()) return
        val unknown=records.flatMap { it.equipment }.distinct().filter { EquipmentCatalog.fromToken(it)==null }
        assertTrue("Unknown equipment tokens: $unknown",unknown.isEmpty())
    }

    @Test fun localizedMachineLabelsDoNotDriveCompatibility() {
        val row=ExerciseV2Record("machine-test","Machine test","Test macchina",emptyList(),"STRENGTH","LEG",listOf("machine"),null,"REPS",listOf("s"),"HIGH")
        ExerciseDbV2.install(listOf(row))
        val ex=ExerciseDbV2.toLegacyExercise(row)
        assertEquals(setOf(EquipmentId.MACHINE),EquipmentCatalog.requirementsFor(ex))
        assertTrue(equipmentCompatible(ex,EquipmentProfile("Gym","",listOf("Macchine"))))
        assertFalse(equipmentCompatible(ex,EquipmentProfile("Home","",listOf("Corpo libero"))))
    }

    @Test fun emptyCanonicalEquipmentMeansBodyweightCapability() {
        val row=ExerciseV2Record("bw-test","Bodyweight test","Test corpo libero",emptyList(),"CALISTHENICS","PUSH",emptyList(),null,"REPS",listOf("s"),"HIGH")
        ExerciseDbV2.install(listOf(row))
        val ex=ExerciseDbV2.toLegacyExercise(row)
        assertEquals(setOf(EquipmentId.BODYWEIGHT),EquipmentCatalog.requirementsFor(ex))
        assertTrue(equipmentCompatible(ex,EquipmentProfile("Park","",emptyList())))
    }

    @Test fun equipmentOptionsAreDeduplicatedAndAlphabetical() {
        val a=Exercise("A","Petto","Macchine","Intermedio")
        val b=Exercise("B","Petto","Macchina / Manubri","Intermedio")
        val options=exerciseEquipmentOptions(listOf(a,b))
        assertEquals(options.sortedBy { it.lowercase() },options)
        assertEquals(1,options.count { it=="Macchina" })
        assertTrue("Manubri" in options)
    }
}
