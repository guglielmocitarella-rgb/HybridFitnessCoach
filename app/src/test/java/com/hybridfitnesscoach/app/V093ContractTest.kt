package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class V093ContractTest {
    @Test fun libraryEquipmentOptionsAreCatalogDrivenDeduplicatedAndAlphabetical() {
        val source=listOf(
            Exercise("A","Petto","Sbarra / Anelli","Base"),
            Exercise("B","Schiena","anelli / Manubri","Base"),
            Exercise("C","Gambe","Bilanciere","Base")
        )
        assertEquals(listOf("Anelli","Bilanciere","Manubri","Sbarra"),exerciseEquipmentOptions(source))
    }

    @Test fun profileEquipmentOptionsAddsBodyweightAndStaysAlphabetical() {
        val source=listOf(
            Exercise("A","Petto","Sbarra","Base"),
            Exercise("B","Schiena","Bilanciere","Base")
        )
        assertEquals(listOf("Bilanciere","Corpo libero","Sbarra"),profileEquipmentOptions(source))
    }

    @Test fun pinnedRegenerationKeepsPinnedExercisesInTheirOriginalSlots() {
        val original=listOf(
            Exercise("A","Petto","Nessuna","Base"),
            Exercise("B","Schiena","Nessuna","Base"),
            Exercise("C","Gambe","Nessuna","Base")
        )
        val replacements=listOf(
            Exercise("X","Spalle","Nessuna","Base"),
            Exercise("Y","Core","Nessuna","Base")
        )
        val regenerated=regenerateUnpinned(original,replacements,setOf("B"))
        assertEquals("B",regenerated[1].name)
        assertEquals(3,regenerated.size)
        assertEquals(1,regenerated.count{it.name=="B"})
    }
}
