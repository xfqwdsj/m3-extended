package top.ltfan.m3x.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import top.ltfan.backdrop.Backdrop
import top.ltfan.backdrop.BackdropDrawCallback
import top.ltfan.backdrop.BackdropDrawScope
import top.ltfan.backdrop.BackdropEffectDrawCallback
import top.ltfan.backdrop.BackdropEffectScope
import top.ltfan.backdrop.BackdropInsets
import top.ltfan.backdrop.backdrops.LayerBackdrop
import top.ltfan.backdrop.backdrops.layerBackdrop
import top.ltfan.backdrop.backdrops.rememberLayerBackdrop
import top.ltfan.backdrop.drawBackdrop
import top.ltfan.backdrop.effects.blur
import top.ltfan.backdrop.effects.vibrancy
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.interaction.InteractionLighting
import top.ltfan.backdrop.interaction.ProvideInteractionLighting
import top.ltfan.backdrop.shadow.InnerShadow
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.m3x.compose.ui.visual.BackdropBlurEnabled

/**
 * A captured backdrop layer that backdrop surfaces sample.
 *
 * Create one with [rememberBackdropLayer], mark the source subtree with [captureBackdrop] and draw
 * surfaces with [backdropSurface].
 *
 * A handle of a backdrop stack, created with [rememberStackedBackdropLayer], samples the layers
 * under it and records its own layer from the frame it exists.
 */
@Stable
public class BackdropLayerHandle
internal constructor(
    public val layer: LayerBackdrop,
    private val sampled: Backdrop = layer,
    private val sampleSource: Backdrop = layer,
    private val state: StackState? = null,
) {
    /**
     * What the backdrop surfaces of this layer sample: the layer itself, or the layers under it.
     */
    public val backdrop: Backdrop
        get() = sampled

    /** What a layer above samples from this layer: the pixels of this layer. */
    internal val exported: Backdrop
        get() = sampleSource

    /**
     * Whether a backdrop surface sampled this layer, for a layer of a backdrop stack. A layer
     * records while it exists, so this reports the sampling of the stack.
     */
    public val isSampling: Boolean
        get() = state?.sampled?.value == true
}

/**
 * The sampling state of one layer of a backdrop stack.
 *
 * A layer of a stack records from the frame it exists, so a surface above it samples what this
 * frame recorded. Sampling reports usage independently of recording. A surface draws before the
 * node that records the layer below it. Starting recording only after the first sample would leave
 * that layer missing from the backdrop chain for the current frame.
 */
internal class StackState {
    /** Whether a backdrop surface sampled the layer, written in the draw phase. */
    val sampled: MutableState<Boolean> = mutableStateOf(false)

    /** Marks the layer as sampled by a surface. */
    fun markSampled() {
        if (!sampled.value) sampled.value = true
    }
}

/**
 * Creates one layer of a backdrop stack: the content of the layer samples [below] — the page and
 * the layers of the overlays under it — and the layers above sample this layer.
 *
 * Record the content with [captureStackBackdrop] and provide the handle to the content with
 * [BackdropHost], like the handle of a single capture.
 *
 * The returned handle and sampling state survive changes to [below]. The layer reads the latest
 * list while drawing. Opening an overlay or replacing a page handle therefore keeps recording into
 * the same layer. Replacing the handle instead would restart the recording and leave the layer out
 * of the backdrop chain for a frame, which surfaces above it sample as missing content.
 *
 * @param below the handles the content samples, in the order they are drawn. Pass a new list value
 *   when the handles change; treat each published list as immutable.
 * @param tileMode the boundary policy of this layer when a layer above samples it.
 */
@Composable
public fun rememberStackedBackdropLayer(
    below: List<BackdropLayerHandle>,
    tileMode: TileMode = TileMode.Clamp,
): BackdropLayerHandle {
    val layer = rememberLayerBackdrop(tileMode = tileMode)
    // The list is read while drawing, so the handle outlives a change of the layers underneath.
    val currentBelow = rememberUpdatedState(below)
    return remember(layer) {
        val state = StackState()
        BackdropLayerHandle(
            layer = layer,
            sampled = BelowBackdrop { currentBelow.value },
            sampleSource = SampledBackdrop(layer, state),
            state = state,
        )
    }
}

/**
 * Draws the layers under a stacked layer, in the order they are drawn, so a surface that samples
 * the stack sees the same composition the layers have on screen.
 *
 * The layers under this one are sampled directly: a layer above that sampled the composition of a
 * lower layer would draw the layers under it once per level.
 */
private class BelowBackdrop(private val below: () -> List<BackdropLayerHandle>) : Backdrop {
    /**
     * Always true, including while nothing is under the layer: a layer that gains its first source
     * later has to keep the coordinates that source is mapped through.
     */
    override val isCoordinatesDependent: Boolean
        get() = true

    override fun BackdropDrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        below().forEach { handle ->
            with(handle.exported) { drawBackdrop(density, coordinates, layerBlock) }
        }
    }
}

/** Draws a layer and reports that a surface sampled it. */
private class SampledBackdrop(
    private val layer: LayerBackdrop,
    private val state: StackState,
) : Backdrop {
    override val isCoordinatesDependent: Boolean
        get() = true

    override fun BackdropDrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        state.markSampled()
        with(layer) { drawBackdrop(density, coordinates, layerBlock) }
    }
}

/**
 * Captures [background] and the content beneath as a backdrop layer.
 *
 * Passing a [background] bakes the page background into the capture, so the effects and the tinted
 * surface compose over the same opaque backdrop. With the same [tileMode], the handle and its layer
 * remain the same across recompositions at this call site; changing [background] updates the
 * captured color without replacing either object.
 *
 * [tileMode] defines sampling outside the recorded source. [TileMode.Clamp] extends boundary
 * texels, including their alpha; [TileMode.Decal] supplies transparent exterior pixels. Repeated
 * and mirror modes tile the source. Boundary extension requires runtime shader support.
 */
@Composable
public fun rememberBackdropLayer(
    background: Color? = null,
    tileMode: TileMode = TileMode.Clamp,
): BackdropLayerHandle {
    val currentBackground = rememberUpdatedState(background)
    val onDraw: ContentDrawScope.() -> Unit = remember {
        {
            currentBackground.value?.let { drawRect(it) }
            drawContent()
        }
    }
    val layer = rememberLayerBackdrop(tileMode = tileMode, onDraw = onDraw)
    return remember(layer) { BackdropLayerHandle(layer) }
}

/**
 * The nearest enclosing [BackdropLayerHandle], sampled by the backdrop surfaces. Use [BackdropHost]
 * to provide a handle to a subtree; built-in M3X surfaces read this local automatically. For a page
 * source layer, place this provider outside the overlay host so the host can include the page in
 * its automatic layer stack.
 */
public val LocalBackdrop: ProvidableCompositionLocal<BackdropLayerHandle?> =
    staticCompositionLocalOf {
        null
    }

/**
 * Provides [handle] to the surfaces below [content]. This is the common entry point for supplying a
 * backdrop to built-in M3X components.
 *
 * For a page source layer, put this host outside the overlay host. Capture only the page content
 * with [captureBackdrop]; keep glass surfaces outside that capture node so they do not sample
 * themselves. The overlay host then creates the layers for any overlays above the page
 * automatically.
 */
@Composable
public fun BackdropHost(handle: BackdropLayerHandle, content: @Composable () -> Unit) {
    ProvideBackdropAppearanceBatch {
        ProvideInteractionLighting(isolate = false) {
            CompositionLocalProvider(LocalBackdrop provides handle) { content() }
        }
    }
}

/**
 * Records this subtree into [handle]'s layer. Apply this to the page source subtree, including its
 * background, and place glass surfaces as siblings outside the captured subtree. A page handle
 * provided by [BackdropHost] can then be sampled by built-in surfaces and by the overlay host's
 * stack.
 */
public fun Modifier.captureBackdrop(handle: BackdropLayerHandle): Modifier =
    layerBackdrop(handle.layer)

/**
 * Records this subtree into the layer of [handle], for a layer of a backdrop stack created with
 * [rememberStackedBackdropLayer].
 *
 * A layer of a stack records from the frame it exists, so a surface above it samples the content
 * this frame recorded. A surface above draws before the node that records the layer below it, so a
 * layer whose record waited for its first sample would be missing from the backdrop chain for the
 * frame that surface is drawn in.
 */
public fun Modifier.captureStackBackdrop(handle: BackdropLayerHandle): Modifier =
    layerBackdrop(handle.layer)

/**
 * Draws the [handle] layer through [effects], clipped to [shape].
 *
 * Without a [handle] (and without an enclosing [BackdropHost]) this modifier adds no drawing.
 * Callers of this low-level API must draw their own ordinary background when no backdrop is
 * available; built-in M3X components do so.
 */
@Composable
public fun Modifier.backdropSurface(
    handle: BackdropLayerHandle? = LocalBackdrop.current,
    shape: Shape,
    effects: (BackdropEffectScope.() -> Unit)? = null,
    highlight: (() -> Highlight)? = null,
    shadow: (() -> Shadow)? = null,
    innerShadow: (() -> InnerShadow)? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    exportedBackdrop: LayerBackdrop? = null,
    insets: (() -> BackdropInsets)? = null,
    onDrawBehind: BackdropDrawCallback? = null,
    onDrawBackdrop: BackdropEffectDrawCallback = { drawBackdrop, _ -> drawBackdrop() },
    onDrawSurface: BackdropDrawCallback? = null,
    onDrawFront: BackdropDrawCallback? = null,
    interactionLighting: (() -> InteractionLighting)? = null,
): Modifier {
    handle ?: return this
    return drawBackdrop(
        backdrop = handle.backdrop,
        shape = { shape },
        effects = effects,
        insets = insets,
        highlight = highlight,
        shadow = shadow,
        innerShadow = innerShadow,
        layerBlock = layerBlock,
        exportedBackdrop = exportedBackdrop,
        onDrawBehind = onDrawBehind,
        onDrawBackdrop = onDrawBackdrop,
        onDrawSurface = onDrawSurface,
        onDrawFront = onDrawFront,
        interactionLighting = interactionLighting,
    )
}

/** The default values of the backdrop surfaces. */
public object BackdropDefaults {
    /**
     * The default effects of a backdrop surface: a vibrancy tint and, while [LocalBlurEnabled] is
     * true, a blur.
     */
    public val effects: BackdropEffectScope.() -> Unit
        @Composable
        get() = if (BackdropBlurEnabled) BlurredBackdropEffects else PlainBackdropEffects
}

private val BlurredBackdropEffects: BackdropEffectScope.() -> Unit = {
    vibrancy()
    blur(8.dp.toPx())
}

private val PlainBackdropEffects: BackdropEffectScope.() -> Unit = { vibrancy() }
