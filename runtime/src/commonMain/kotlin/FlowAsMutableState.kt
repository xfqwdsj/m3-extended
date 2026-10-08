package top.ltfan.m3x.compose.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.flow.Flow

/**
 * Collect a [Flow] and expose its latest emission as a delegated mutable property.
 *
 * Reading the delegated property returns the latest value emitted by the receiver [Flow]. Writing
 * to the delegated property does NOT update the internal collected state; instead it invokes the
 * provided [set] callback, allowing you to forward user changes (e.g. to a ViewModel) without
 * creating a feedback loop back into the upstream [Flow].
 *
 * Implementation details:
 * - Uses [remember] + [mutableStateOf] to hold the latest emission.
 * - [rememberUpdatedState] keeps the latest [set] lambda without restarting collection.
 * - [LaunchedEffect] ties collection to the composable lifecycle and re-collects when the receiver
 *   [Flow] reference changes.
 *
 * @param T the concrete flow emission type. Must be a subtype of [R].
 * @param R the (possibly covariant) read type exposed by the returned delegate.
 * @param initial the initial value used until the first emission arrives.
 * @param set callback invoked when the caller assigns a new value to the delegate.
 * @return a [ReadWriteProperty] suitable for use with `by` in Compose stateful scopes.
 * @receiver the upstream [Flow] whose emissions are collected.
 */
@Composable
public fun <T : R, R> Flow<T>.collectAsMutableState(
    initial: R,
    set: (R) -> Unit,
): ReadWriteProperty<Any?, R> {
    val state = remember(initial) { mutableStateOf(initial) }
    val set by rememberUpdatedState(set)

    LaunchedEffect(this, state) {
        collect { state.value = it }
    }

    return remember(set) {
        object : ReadWriteProperty<Any?, R> {
            /** Current value collected from the Flow. */
            override fun getValue(thisRef: Any?, property: KProperty<*>): R = state.value

            /**
             * Forwards assignments to the external [set] callback; does not mutate the state
             * directly.
             */
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: R) {
                set(value)
            }
        }
    }
}
