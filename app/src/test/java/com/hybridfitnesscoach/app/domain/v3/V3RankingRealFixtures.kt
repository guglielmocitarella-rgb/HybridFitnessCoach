package com.hybridfitnesscoach.app.domain.v3

import java.io.File
import java.security.MessageDigest

/** Test-only frozen positive fixture. Never substitutes the packaged runtime authority. */
internal object V3RankingRealFixtures {
    fun frozenCanonical(): V3LoadResult {
        var directory: File? = File(".").canonicalFile
        var fixture: File? = null
        while (directory != null) {
            val candidate = File(directory, "exercise-db-canonical/exercise-db-v3-final-candidate.json")
            if (candidate.isFile) { fixture = candidate; break }
            directory = directory.parentFile
        }
        val bytes = requireNotNull(fixture) { "Frozen canonical integration fixture missing" }.readBytes()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        check(hash == "0a48dd6b6a3f128d28d168004976aeb989361fde6b4aba8cff6c3ac0e196911a") {
            "Frozen candidate SHA-256 mismatch: $hash"
        }
        return V3AssetLoader.loadAuthoritative(bytes.toString(Charsets.UTF_8))
    }
}
