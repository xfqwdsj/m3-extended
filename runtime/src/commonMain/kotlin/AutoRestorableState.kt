package top.ltfan.m3x.compose.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A [MutableState] wrapper that automatically resets its value back to the original value after a
 * specified [delay].
 *
 * When [value] is set, a coroutine is launched in [coroutineScope]; after [delay] the underlying
 * state is restored to the captured initial value.
 *
 * Use for transient UI flags (e.g. ephemeral banners, one‑shot visual effects).
 *
 * Concurrency note: each assignment cancels any previous restore job and schedules a new one.
 * Multiple overlapping writes will only trigger one restore operation based on the last write.
 *
 * @param delegate backing mutable state to wrap
 * @param delay duration to wait before restoring the initial value
 * @param coroutineScope scope in which the restore coroutine is launched
 */
@Stable
public class AutoRestorableState<T>(
    private val delegate: MutableState<T>,
    private val delay: Duration = 1.seconds,
    private val coroutineScope: CoroutineScope,
) : MutableState<T> by delegate {
    private val initialValue = delegate.value
    private var restoreJob: Job? = null

    override var value: T
        get() = delegate.value
        set(newValue) {
            restoreJob?.cancel()

            delegate.value = newValue

            if (newValue != initialValue) {
                restoreJob = coroutineScope.launch {
                    delay(delay)
                    delegate.value = initialValue
                    restoreJob = null
                }
            }
        }

    /**
     * Convenience constructor that creates an internal [MutableState] from [initialValue].
     *
     * @param initialValue value to expose and later restore to
     * @param delay duration to wait before restoring
     * @param coroutineScope scope used for the delayed restore
     */
    public constructor(
        initialValue: T,
        delay: Duration = 1.seconds,
        coroutineScope: CoroutineScope,
    ) : this(
        delegate = mutableStateOf(initialValue),
        delay = delay,
        coroutineScope = coroutineScope,
    )
}

/**
 * Remember an [AutoRestorableState] tied to the current composition which resets itself to
 * [initialValue] after each mutation and the specified [delay].
 *
 * @param initialValue starting (and restore) value
 * @param delay duration to wait before restoring after every write
 */
@Composable
public fun <T> rememberAutoRestorableState(
    initialValue: T,
    delay: Duration = 1.seconds,
): AutoRestorableState<T> {
    val coroutineScope = rememberCoroutineScope()
    return remember(initialValue, delay, coroutineScope) {
        AutoRestorableState(
            initialValue = initialValue,
            delay = delay,
            coroutineScope = coroutineScope,
        )
    }
}
