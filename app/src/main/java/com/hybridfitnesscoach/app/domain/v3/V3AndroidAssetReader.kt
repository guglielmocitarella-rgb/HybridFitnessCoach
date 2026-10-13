package com.hybridfitnesscoach.app.domain.v3

import android.content.res.AssetManager
import java.io.IOException

/** Loads the packaged canonical V3 asset; never falls back to V2. */
object V3AndroidAssetReader {
    const val ASSET_PATH = "exercise-db-v3/exercises.json"

    fun load(assetManager: AssetManager): V3LoadResult {
        val json = try {
            assetManager.open(ASSET_PATH).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: IOException) {
            throw V3AssetValidationException("V3 Android asset missing or unreadable at $ASSET_PATH: ${e.message}")
        }
        return V3AssetLoader.loadAuthoritative(json)
    }
}
