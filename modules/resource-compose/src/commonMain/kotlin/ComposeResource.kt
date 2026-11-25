package top.ltfan.m3x.resource.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import top.ltfan.m3x.resource.Resource

/** A resource whose value needs composition. Other UI toolkits can provide their own subtype. */
public interface ComposeResource<out T> : Resource<T> {
    @Composable public fun resolve(): T
}

private class ComposeValue<T>(private val provider: @Composable () -> T) : ComposeResource<T> {
    @Composable override fun resolve(): T = provider()
}

public fun <T> composeResource(provider: @Composable () -> T): ComposeResource<T> =
    ComposeValue(provider)

/** Resolves both context-free and Compose resources in a composition. */
@Composable
public fun <T> Resource<T>.resolve(): T =
    when (this) {
        is Resource.Global -> value
        is ComposeResource -> resolve()
        else -> error("No Compose adapter for ${this::class.simpleName}")
    }

public fun painterResource(image: ImageVector): ComposeResource<Painter> = composeResource {
    rememberVectorPainter(image)
}

public fun painterResource(image: ImageBitmap): ComposeResource<Painter> = composeResource {
    remember(image) { BitmapPainter(image) }
}
