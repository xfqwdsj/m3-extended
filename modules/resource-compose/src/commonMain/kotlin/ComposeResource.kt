package top.ltfan.m3x.resource.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
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

public fun stringResource(resource: StringResource): ComposeResource<String> = composeResource {
    stringResource(resource)
}

public fun stringResource(resource: StringResource, vararg args: Any): ComposeResource<String> =
    composeResource {
        stringResource(resource, *args)
    }

public fun painterResource(resource: DrawableResource): ComposeResource<Painter> = composeResource {
    painterResource(resource)
}

public fun painterResource(image: ImageVector): ComposeResource<Painter> = composeResource {
    rememberVectorPainter(image)
}

public fun painterResource(image: ImageBitmap): ComposeResource<Painter> = composeResource {
    remember(image) { BitmapPainter(image) }
}
