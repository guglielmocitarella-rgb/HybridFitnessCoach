package com.hybridfitnesscoach.app.domain.v3

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

/**
 * Loads V3 at most once for this application-owned component. Profile changes only
 * rebuild the native environment from the already validated records.
 */
internal class V3RuntimeComponent(
    private val assetReader: () -> V3LoadResult,
    private val executor: Executor = ForkJoinPool.commonPool()
) {
    private val lock = Any()
    private val mutableState = MutableStateFlow<V3RuntimeState>(V3RuntimeState.NotStarted)
    val state = mutableState.asStateFlow()

    private var started = false
    private var activeLocation: TrainingLocation? = null

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
            mutableState.value = V3RuntimeState.Loading
            try {
                executor.execute {
                    val result = try {
                        Result.success(assetReader())
                    } catch (failure: Exception) {
                        Result.failure(failure)
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
