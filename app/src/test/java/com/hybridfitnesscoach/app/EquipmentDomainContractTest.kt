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

    @Test fun emptyCanonicalEquipmentRemainsUnknownAndFailsClosed() {
        val row=ExerciseV2Record("unknown-test","Unknown equipment","Attrezzatura sconosciuta",emptyList(),"CALISTHENICS","PUSH",emptyList(),null,"REPS",listOf("s"),"HIGH")
        ExerciseDbV2.install(listOf(row))
        val ex=ExerciseDbV2.toLegacyExercise(row)
        assertEquals("Non specificato",ex.equipment)
        assertTrue(EquipmentCatalog.requirementsFor(ex).isEmpty())
        assertFalse(equipmentCompatible(ex,EquipmentProfile("Park","",emptyList())))
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
