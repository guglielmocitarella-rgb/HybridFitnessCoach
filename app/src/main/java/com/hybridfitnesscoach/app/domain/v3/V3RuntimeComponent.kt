package com.hybridfitnesscoach.app.domain.v3

import android.os.Looper
import com.hybridfitnesscoach.app.TrainingLocation
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Runtime state for the isolated V3 load. A failed load never carries an environment. */
internal sealed interface V3RuntimeState {
    data object NotStarted : V3RuntimeState
    data object Loading : V3RuntimeState
    data class Ready(
        val loadResult: V3LoadResult,
        val environment: V3NativeEnvironmentResult
    ) : V3RuntimeState
    data class Failed(val cause: Throwable) : V3RuntimeState
}

/** Sanitized runtime facts; no athlete profile fields or personal data are included. */
internal data class V3RuntimeDiagnostics(
    val state: String,
    val recordCount: Int,
    val schemaVersion: String?,
    val activeLocationId: String?,
    val recognizedEquipmentCount: Int,
    val unmappedNativeEquipmentCount: Int,
    val error: String?,
    val loadAttempts: Int,
    val loadDurationMillis: Long?,
    val loadThreadIsMain: Boolean?
)

/**
 * Loads V3 at most once for this application-owned component. Profile changes only
 * rebuild the native environment from the already validated records.
 */
internal class V3RuntimeComponent(
    private val assetReader: () -> V3LoadResult,
    private val executor: Executor = ForkJoinPool.commonPool(),
    private val diagnosticsEnabled: () -> Boolean = { false }
) {
    private val lock = Any()
    private val mutableState = MutableStateFlow<V3RuntimeState>(V3RuntimeState.NotStarted)
    val state = mutableState.asStateFlow()

    private var started = false
    private var activeLocation: TrainingLocation? = null
    private var loadAttempts = 0
    private var loadDurationMillis: Long? = null
    private var loadThreadIsMain: Boolean? = null

    /** Runtime diagnostics require an explicit debug-build or test gate. */
    fun diagnostics(): V3RuntimeDiagnostics? {
        if (!diagnosticsEnabled()) return null
        synchronized(lock) {
            val current = mutableState.value
            val ready = current as? V3RuntimeState.Ready
            val failed = current as? V3RuntimeState.Failed
            return V3RuntimeDiagnostics(
                state = when (current) {
                    V3RuntimeState.NotStarted -> "NotStarted"
                    V3RuntimeState.Loading -> "Loading"
                    is V3RuntimeState.Ready -> "Ready"
                    is V3RuntimeState.Failed -> "Failed"
                },
                recordCount = ready?.loadResult?.records?.size ?: 0,
                schemaVersion = ready?.loadResult?.records?.firstOrNull()?.version?.schemaVersion,
                activeLocationId = ready?.environment?.locationId ?: activeLocation?.id,
                recognizedEquipmentCount = ready?.environment?.environment?.equipment?.size ?: 0,
                unmappedNativeEquipmentCount = ready?.environment?.unmappedNativeEquipment?.size ?: 0,
                error = failed?.cause?.let { "${it::class.java.simpleName}: ${it.message.orEmpty()}" },
                loadAttempts = loadAttempts,
                loadDurationMillis = loadDurationMillis,
                loadThreadIsMain = loadThreadIsMain
            )
        }
    }

    fun activate(location: TrainingLocation) {
        synchronized(lock) {
            activeLocation = location
            val current = mutableState.value
            if (current is V3RuntimeState.Ready) {
                mutableState.value = current.copy(
                    environment = V3NativeEnvironmentAdapter.fromLocation(location, current.loadResult)
                )
            }
            if (started) return
            started = true
            loadAttempts += 1
            mutableState.value = V3RuntimeState.Loading
            try {
                executor.execute {
                    val startedAtNanos = System.nanoTime()
                    synchronized(lock) {
                        loadThreadIsMain = runCatching { Thread.currentThread() === Looper.getMainLooper()?.thread }.getOrNull()
                    }
                    val result = try {
                        Result.success(assetReader())
                    } catch (failure: Exception) {
                        Result.failure(failure)
                    } finally {
                        synchronized(lock) {
                            loadDurationMillis = (System.nanoTime() - startedAtNanos) / 1_000_000L
                        }
                    }
                    synchronized(lock) {
                        mutableState.value = result.fold(
                            onSuccess = { loaded ->
                                V3RuntimeState.Ready(
                                    loaded,
                                    V3NativeEnvironmentAdapter.fromLocation(activeLocation, loaded)
                                )
                            },
                            onFailure = { failure -> V3RuntimeState.Failed(failure) }
                        )
                    }
                }
            } catch (failure: Exception) {
                mutableState.value = V3RuntimeState.Failed(failure)
            }
        }
    }
}
