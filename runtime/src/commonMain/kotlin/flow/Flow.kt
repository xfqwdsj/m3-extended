package top.ltfan.m3x.compose.runtime.flow

import androidx.compose.runtime.mutableStateOf
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

public inline fun <T : R, R> StateFlow<T>.collectAsMutableState(
    coroutineScope: CoroutineScope,
    crossinline set: (R) -> Unit,
): ReadWriteProperty<Any?, R> {
    val state = mutableStateOf(value)
    coroutineScope.launch { collect { state.value = it } }
    return object : ReadWriteProperty<Any?, R> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = state.value

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: R) {
            set(value)
        }
    }
}
