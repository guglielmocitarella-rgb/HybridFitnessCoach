package com.hybridfitnesscoach.app.domain.v3

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class V3PackagedAssetTest {
    @Test fun canonicalAssetIsPresentAndValid() {
        val asset = File(requireNotNull(System.getProperty("hfc.v3.asset")) { "Missing hfc.v3.asset test property" })
        assertTrue("Generated V3 asset missing: $asset", asset.isFile)
        val loaded = V3AssetLoader.loadAuthoritative(asset.readText(Charsets.UTF_8))
        assertEquals(1087, loaded.records.size)
        assertEquals(1087, loaded.records.map { it.id.value }.toSet().size)
        assertTrue("V3 equipment vocabulary is empty", V3LoadedEquipmentVocabulary.from(loaded).isNotEmpty())
    }
}
