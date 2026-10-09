package com.hybridfitnesscoach.app.domain.v3

/** Explicit identity links retained from the validated asset, never guessed from aliases. */
data class V3IdentityEvidence(
    val canonicalId: CanonicalExerciseId,
    val nativeIds: Set<String>,
    val nativeNames: Set<String>,
    val source: String = "canonical asset: legacy.id / legacy.runtimeName"
)

enum class V3IdentityMatchState { MATCHED, AMBIGUOUS, UNRESOLVED }
data class V3IdentityMatch(
    val state: V3IdentityMatchState,
    val canonicalId: CanonicalExerciseId? = null,
    val provenance: String,
    val reason: String
)
data class V3IdentityAudit(val matched: Int, val ambiguous: Int, val unresolved: Int) {
    companion object {
        fun from(matches: List<V3IdentityMatch>) = V3IdentityAudit(
            matches.count { it.state == V3IdentityMatchState.MATCHED },
            matches.count { it.state == V3IdentityMatchState.AMBIGUOUS },
            matches.count { it.state == V3IdentityMatchState.UNRESOLVED }
        )
    }
}

/** IDs win; an unknown nonempty ID never falls back to a potentially unrelated name. */
class V3CanonicalIdentityResolver(load: V3LoadResult) {
    private val knownIds = load.records.filter { it.identityState == SemanticState.KNOWN }.map { it.id }.toSet()
    private val evidence = load.identityEvidence.filter { it.canonicalId in knownIds }
    private val ids = buildMap<String, Set<CanonicalExerciseId>> {
        knownIds.forEach { put(it.value, setOf(it)) }
        evidence.forEach { link -> link.nativeIds.forEach { id ->
            put(id, get(id).orEmpty() + link.canonicalId)
        } }
    }
    private val names = buildMap<String, Set<CanonicalExerciseId>> {
        evidence.forEach { link -> link.nativeNames.filter(String::isNotBlank).forEach { name ->
            put(name, get(name).orEmpty() + link.canonicalId)
        } }
    }

    fun resolve(nativeId: String?, nativeName: String): V3IdentityMatch {
        val stableId = nativeId?.takeIf(String::isNotBlank)
        val candidates = if (stableId != null) ids[stableId].orEmpty() else names[nativeName].orEmpty()
        val source = if (stableId != null) "canonical ID / explicit legacy.id" else "explicit legacy.runtimeName (exact, unique)"
        return when (candidates.size) {
            1 -> V3IdentityMatch(V3IdentityMatchState.MATCHED, candidates.single(), source, "Explicit identity match")
            0 -> V3IdentityMatch(V3IdentityMatchState.UNRESOLVED, provenance = source, reason = "No verified identity link")
            else -> V3IdentityMatch(V3IdentityMatchState.AMBIGUOUS, provenance = source, reason = "Multiple canonical identities; history withheld")
        }
    }
}
