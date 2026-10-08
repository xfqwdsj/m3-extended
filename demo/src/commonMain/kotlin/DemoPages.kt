package top.ltfan.m3x.demo

import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.animation.AnimatedVisibilityWithBlur
import top.ltfan.m3x.compose.animation.core.withDefaultVisibilityThreshold
import top.ltfan.m3x.compose.foundation.GroupedLazyColumn
import top.ltfan.m3x.compose.foundation.GroupedLazyRow
import top.ltfan.m3x.compose.ui.layout.CornerRadii
import top.ltfan.m3x.datastore.MutableStore
import top.ltfan.m3x.demo.generated.resources.Res
import top.ltfan.m3x.demo.generated.resources.demo_a_glass_snackbar
import top.ltfan.m3x.demo.generated.resources.demo_a_stable_foreground_and_automatic_overlay_sampling_nested_modal_panels_have_independent_interaction_lighting
import top.ltfan.m3x.demo.generated.resources.demo_alert
import top.ltfan.m3x.demo.generated.resources.demo_animate
import top.ltfan.m3x.demo.generated.resources.demo_animated_chip
import top.ltfan.m3x.demo.generated.resources.demo_blurred_entrance_and_exit
import top.ltfan.m3x.demo.generated.resources.demo_bottom_sheet
import top.ltfan.m3x.demo.generated.resources.demo_cancel
import top.ltfan.m3x.demo.generated.resources.demo_card
import top.ltfan.m3x.demo.generated.resources.demo_close
import top.ltfan.m3x.demo.generated.resources.demo_close_floating_actions
import top.ltfan.m3x.demo.generated.resources.demo_close_sheet
import top.ltfan.m3x.demo.generated.resources.demo_confirm
import top.ltfan.m3x.demo.generated.resources.demo_container_radius
import top.ltfan.m3x.demo.generated.resources.demo_container_shape
import top.ltfan.m3x.demo.generated.resources.demo_control
import top.ltfan.m3x.demo.generated.resources.demo_controls_and_resources
import top.ltfan.m3x.demo.generated.resources.demo_corner_policy
import top.ltfan.m3x.demo.generated.resources.demo_dialog
import top.ltfan.m3x.demo.generated.resources.demo_disabled_action
import top.ltfan.m3x.demo.generated.resources.demo_dismiss
import top.ltfan.m3x.demo.generated.resources.demo_display_corners
import top.ltfan.m3x.demo.generated.resources.demo_done
import top.ltfan.m3x.demo.generated.resources.demo_drag_between_partial_expanded_and_hidden_states_or_dismiss_with_back
import top.ltfan.m3x.demo.generated.resources.demo_draggable_glass_sheet
import top.ltfan.m3x.demo.generated.resources.demo_explore_surfaces_grouped_content_and_responsive_controls_change_the_app_s_appearance_in_settings
import top.ltfan.m3x.demo.generated.resources.demo_first_action
import top.ltfan.m3x.demo.generated.resources.demo_floating_layers
import top.ltfan.m3x.demo.generated.resources.demo_full_uses_size
import top.ltfan.m3x.demo.generated.resources.demo_glass_rendering_material_colors_and_accessible_dismissal
import top.ltfan.m3x.demo.generated.resources.demo_grouped_horizontal_cards
import top.ltfan.m3x.demo.generated.resources.demo_grouped_item
import top.ltfan.m3x.demo.generated.resources.demo_grouped_menu
import top.ltfan.m3x.demo.generated.resources.demo_independent_groups
import top.ltfan.m3x.demo.generated.resources.demo_interactions
import top.ltfan.m3x.demo.generated.resources.demo_keyboard_and_pointer_tooltip
import top.ltfan.m3x.demo.generated.resources.demo_l
import top.ltfan.m3x.demo.generated.resources.demo_li
import top.ltfan.m3x.demo.generated.resources.demo_live_resource
import top.ltfan.m3x.demo.generated.resources.demo_m
import top.ltfan.m3x.demo.generated.resources.demo_material_alert
import top.ltfan.m3x.demo.generated.resources.demo_material_with_light
import top.ltfan.m3x.demo.generated.resources.demo_menus_and_transient_layers
import top.ltfan.m3x.demo.generated.resources.demo_modal_panels
import top.ltfan.m3x.demo.generated.resources.demo_nested_modal
import top.ltfan.m3x.demo.generated.resources.demo_no_editor_registered_for
import top.ltfan.m3x.demo.generated.resources.demo_no_radius_metadata_is_attached_to_this_shape_preset
import top.ltfan.m3x.demo.generated.resources.demo_none
import top.ltfan.m3x.demo.generated.resources.demo_open_a_dialog
import top.ltfan.m3x.demo.generated.resources.demo_open_another_panel
import top.ltfan.m3x.demo.generated.resources.demo_open_floating_actions
import top.ltfan.m3x.demo.generated.resources.demo_panel
import top.ltfan.m3x.demo.generated.resources.demo_panel_material
import top.ltfan.m3x.demo.generated.resources.demo_persistent_glass_tooltip
import top.ltfan.m3x.demo.generated.resources.demo_s
import top.ltfan.m3x.demo.generated.resources.demo_scale
import top.ltfan.m3x.demo.generated.resources.demo_scrolls_beneath_the_progressive_glass_bar
import top.ltfan.m3x.demo.generated.resources.demo_section
import top.ltfan.m3x.demo.generated.resources.demo_show_alert
import top.ltfan.m3x.demo.generated.resources.demo_show_panel
import top.ltfan.m3x.demo.generated.resources.demo_snackbar
import top.ltfan.m3x.demo.generated.resources.demo_the_lower_panel_remains_protected_from_input
import top.ltfan.m3x.demo.generated.resources.demo_tooltip
import top.ltfan.m3x.demo.generated.resources.demo_try_nested_panels_light_isolation_outside_dismissal_escape_and_keyboard_focus
import top.ltfan.m3x.demo.generated.resources.demo_unknown
import top.ltfan.m3x.demo.generated.resources.demo_window_corners
import top.ltfan.m3x.demo.generated.resources.demo_window_presentation
import top.ltfan.m3x.demo.generated.resources.demo_xl
import top.ltfan.m3x.demo.generated.resources.demo_xli
import top.ltfan.m3x.demo.generated.resources.demo_xs
import top.ltfan.m3x.demo.generated.resources.demo_xxl
import top.ltfan.m3x.material.Button
import top.ltfan.m3x.material.FilterChip
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.OutlinedButton
import top.ltfan.m3x.material.TextButton
import top.ltfan.m3x.material.backdrop.BackdropSurface
import top.ltfan.m3x.material.card
import top.ltfan.m3x.material.cardItem
import top.ltfan.m3x.material.header
import top.ltfan.m3x.material.item
import top.ltfan.m3x.material.overlay.IconButtonWithTooltip
import top.ltfan.m3x.material.overlay.MenuFloatingActionButton
import top.ltfan.m3x.material.overlay.OverlayAlertDialog
import top.ltfan.m3x.material.overlay.OverlayBottomSheet
import top.ltfan.m3x.material.overlay.OverlayDialog
import top.ltfan.m3x.material.overlay.OverlayMenu
import top.ltfan.m3x.material.overlay.OverlayMenuDefaults
import top.ltfan.m3x.material.overlay.OverlayMenuGroup
import top.ltfan.m3x.material.overlay.OverlayMenuItem
import top.ltfan.m3x.material.overlay.OverlayPlainTooltip
import top.ltfan.m3x.material.overlay.OverlaySnackbarHost
import top.ltfan.m3x.material.overlay.OverlayTooltipBox
import top.ltfan.m3x.material.overlay.rememberOverlayTooltipState
import top.ltfan.m3x.material.theme.extended
import top.ltfan.m3x.settingspage.CompositeSettingsTree
import top.ltfan.m3x.settingspage.SettingsSource
import top.ltfan.m3x.settingspage.SliderCommitMode
import top.ltfan.m3x.settingspage.material.CardSettings

/** Body text under a card header, kept in one place so every page shares the same rhythm. */
@Composable
private fun SupportingText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun cornerRadiiText(corners: CornerRadii?, unknown: String): String =
    "TL ${corners?.topLeft ?: unknown} · TR ${corners?.topRight ?: unknown} · BR ${corners?.bottomRight ?: unknown} · BL ${corners?.bottomLeft ?: unknown}"

@Composable
internal fun ComponentsPage() {
    var count by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(true) }
    DemoPageHost {
        GroupedLazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = DemoContentPadding,
            spacing = 16.dp,
        ) {
            cardItem(modifier = { Modifier.fillMaxWidth() }) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        demoText(Res.string.demo_material_with_light),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    SupportingText(
                        demoText(
                            Res.string
                                .demo_explore_surfaces_grouped_content_and_responsive_controls_change_the_app_s_appearance_in_settings,
                        ),
                    )
                }
            }
            cardItem(modifier = { Modifier.fillMaxWidth() }) {
                val radii = MaterialTheme.extended.shapes.cornerRadii
                val display = radii?.displayCorners
                val window = radii?.windowCorners
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        demoText(Res.string.demo_container_shape),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (radii == null)
                            demoText(
                                Res.string.demo_no_radius_metadata_is_attached_to_this_shape_preset,
                            )
                        else
                            "${demoText(Res.string.demo_display_corners)}: ${cornerRadiiText(display, demoText(Res.string.demo_unknown))}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "${demoText(Res.string.demo_window_corners)}: ${cornerRadiiText(window, demoText(Res.string.demo_unknown))}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "${demoText(Res.string.demo_corner_policy)}: ${radii?.policy ?: demoText(Res.string.demo_unknown)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "${demoText(Res.string.demo_window_presentation)}: ${radii?.windowPresentation ?: demoText(Res.string.demo_unknown)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (radii != null) {
                        Text(
                            "${demoText(Res.string.demo_container_radius)} ${radii.containerRadius} · ${demoText(Res.string.demo_scale)} ${radii.extraLargeIncreased.value / 32f} · ${radii.source}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "${demoText(Res.string.demo_xs)} ${radii.extraSmall} · ${demoText(Res.string.demo_s)} ${radii.small} · ${demoText(Res.string.demo_m)} ${radii.medium} · ${demoText(Res.string.demo_l)} ${radii.large} · ${demoText(Res.string.demo_li)} ${radii.largeIncreased} · ${demoText(Res.string.demo_xl)} ${radii.extraLarge} · ${demoText(Res.string.demo_xli)} ${radii.extraLargeIncreased} · ${demoText(Res.string.demo_xxl)} ${radii.extraExtraLarge} · ${demoText(Res.string.demo_none)} ${radii.none} · ${demoText(Res.string.demo_full_uses_size)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            card {
                header { Text(demoText(Res.string.demo_controls_and_resources)) }
                item {
                    // The gaps after the first row are padding on the following element rather than
                    // an arrangement: a collapsing row keeps the arrangement's gap until disposal.
                    Column(
                        Modifier.fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                    ) {
                        Text(
                            "${demoText(Res.string.demo_live_resource)} · $count ${demoText(Res.string.demo_interactions)}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        FlowRow(
                            Modifier.padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            itemVerticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilterChip(
                                count % 2 == 1,
                                { count++ },
                                demoText(Res.string.demo_animated_chip),
                            )
                            IconButtonWithTooltip(
                                rememberVectorPainter(Icons.Default.Info),
                                demoText(Res.string.demo_keyboard_and_pointer_tooltip),
                                onClick = { count++ },
                            )
                            Button(onClick = { visible = !visible }) {
                                Text(demoText(Res.string.demo_animate))
                            }
                        }
                        AnimatedVisibilityWithBlur(
                            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                            visible = visible,
                            enter =
                                expandVertically(
                                    animationSpec =
                                        MaterialTheme.motionScheme
                                            .defaultSpatialSpec<IntSize>()
                                            .withDefaultVisibilityThreshold(
                                                IntSize.VisibilityThreshold,
                                            ),
                                    clip = false,
                                ),
                            exit =
                                shrinkVertically(
                                    animationSpec =
                                        MaterialTheme.motionScheme
                                            .fastSpatialSpec<IntSize>()
                                            .withDefaultVisibilityThreshold(
                                                IntSize.VisibilityThreshold,
                                            ),
                                    clip = false,
                                ),
                            maxRadius = MaterialTheme.extended.enterExitBlurRadius,
                        ) {
                            Text(
                                demoText(Res.string.demo_blurred_entrance_and_exit),
                                Modifier.fillMaxWidth().padding(top = 16.dp),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
            cardItem(modifier = { Modifier.fillMaxWidth() }) {
                GlassPreview(Modifier.fillMaxWidth().height(240.dp)) {
                    Row(
                        Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        listOf(MaterialSurfaceRole.Control, MaterialSurfaceRole.Panel).forEach {
                            role ->
                            BackdropSurface(
                                shape = MaterialTheme.extended.shapes.full,
                                role = role,
                            ) {
                                TextButton(onClick = { count++ }) {
                                    Text(
                                        if (role == MaterialSurfaceRole.Control)
                                            demoText(Res.string.demo_control)
                                        else demoText(Res.string.demo_panel),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            card {
                header { Text(demoText(Res.string.demo_grouped_horizontal_cards)) }
                item {
                    GroupedLazyRow(
                        Modifier.fillMaxWidth().height(128.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        spacing = 12.dp,
                    ) {
                        repeat(8) { index ->
                            cardItem(
                                key = "horizontal-$index",
                                modifier = { Modifier.fillMaxHeight() },
                            ) {
                                Column(
                                    Modifier.padding(20.dp).widthIn(min = 140.dp),
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text("${demoText(Res.string.demo_card)} ${index + 1}")
                                    SupportingText(demoText(Res.string.demo_independent_groups))
                                }
                            }
                        }
                    }
                }
            }
            repeat(4) { section ->
                card(useStickyHeader = true) {
                    header { Text("${demoText(Res.string.demo_section)} ${section + 1}") }
                    repeat(4) { row ->
                        item(key = "$section-$row") {
                            Column(
                                Modifier.fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text("${demoText(Res.string.demo_grouped_item)} ${row + 1}")
                                SupportingText(
                                    demoText(
                                        Res.string.demo_scrolls_beneath_the_progressive_glass_bar,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The glass in this preview samples only its local background, excluding its own pixels. */
@Composable
internal fun GlassPreview(
    modifier: Modifier = Modifier,
    hdrBackground: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val backdrop = rememberBackdropLayer()
    BackdropHost(backdrop) {
        Box(modifier) {
            Box(
                Modifier.fillMaxSize()
                    .captureBackdrop(backdrop)
                    .background(demoBackground(PreviewBackground.Artwork)),
            ) {
                if (hdrBackground)
                    Canvas(Modifier.fillMaxSize()) {
                        val left = size.width * 0.08f
                        val top = size.height * 0.15f
                        val right = size.width * 0.56f
                        val bottom = size.height * 0.72f
                        clipRect(left, top, right, bottom) {
                            drawRect(
                                Color(
                                    4f,
                                    1.4f,
                                    0.25f,
                                    colorSpace = ColorSpaces.LinearExtendedSrgb,
                                ),
                                topLeft = Offset(left, top),
                                size = Size(right - left, bottom - top),
                            )
                            val stripeWidth = 5.dp.toPx()
                            var x = left - (bottom - top)
                            while (x < right) {
                                drawLine(
                                    Color(1f, 1f, 1f, colorSpace = ColorSpaces.LinearExtendedSrgb),
                                    start = Offset(x, top),
                                    end = Offset(x + bottom - top, bottom),
                                    strokeWidth = stripeWidth,
                                )
                                x += stripeWidth * 3f
                            }
                        }
                    }
                Column(Modifier.padding(16.dp)) {
                    repeat(20) {
                        Text(
                            "CONTENT · COLOR · TYPOGRAPHY",
                            color =
                                if (it % 2 == 0) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
internal fun OverlaysPage() {
    var dialog by remember { mutableStateOf(false) }
    var alert by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    var nested by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var fab by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val tooltip = rememberOverlayTooltipState(isPersistent = true)
    val snackbarMessage = demoText(Res.string.demo_a_glass_snackbar)
    val dismissLabel = demoText(Res.string.demo_dismiss)
    DemoPageHost {
        GroupedLazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = DemoContentPadding,
            spacing = 16.dp,
        ) {
            cardItem(modifier = { Modifier.fillMaxWidth() }) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        demoText(Res.string.demo_floating_layers),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    SupportingText(
                        demoText(
                            Res.string
                                .demo_try_nested_panels_light_isolation_outside_dismissal_escape_and_keyboard_focus,
                        ),
                    )
                }
            }
            card {
                header { Text(demoText(Res.string.demo_modal_panels)) }
                item {
                    FlowRow(
                        Modifier.fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 2,
                    ) {
                        Button(
                            onClick = { dialog = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(demoText(Res.string.demo_dialog))
                        }
                        Button(
                            onClick = { alert = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(demoText(Res.string.demo_alert))
                        }
                        Button(
                            onClick = { sheet = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(demoText(Res.string.demo_bottom_sheet))
                        }
                    }
                }
            }
            card {
                header { Text(demoText(Res.string.demo_menus_and_transient_layers)) }
                item {
                    FlowRow(
                        Modifier.fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 2,
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { menu = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(demoText(Res.string.demo_grouped_menu))
                            }
                            OverlayMenu(
                                expanded = { menu },
                                onDismissRequest = { menu = false },
                            ) {
                                OverlayMenuGroup(shapes = OverlayMenuDefaults.groupShape(0, 2)) {
                                    OverlayMenuItem(
                                        { Text(demoText(Res.string.demo_first_action)) },
                                        { menu = false },
                                    )
                                    OverlayMenuItem(
                                        { Text(demoText(Res.string.demo_disabled_action)) },
                                        {},
                                        enabled = false,
                                    )
                                }
                                OverlayMenuGroup(shapes = OverlayMenuDefaults.groupShape(1, 2)) {
                                    OverlayMenuItem(
                                        { Text(demoText(Res.string.demo_open_a_dialog)) },
                                        {
                                            menu = false
                                            dialog = true
                                        },
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    snackbar.showSnackbar(
                                        snackbarMessage,
                                        actionLabel = dismissLabel,
                                        withDismissAction = true,
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(demoText(Res.string.demo_snackbar))
                        }
                        OverlayTooltipBox(
                            tooltip = {
                                OverlayPlainTooltip {
                                    Text(demoText(Res.string.demo_persistent_glass_tooltip))
                                }
                            },
                            state = tooltip,
                            modifier = Modifier.weight(1f),
                        ) {
                            OutlinedButton(
                                onClick = { scope.launch { tooltip.show() } },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(demoText(Res.string.demo_tooltip))
                            }
                        }
                    }
                }
            }
            cardItem(modifier = { Modifier.fillMaxWidth() }) {
                GlassPreview(Modifier.fillMaxWidth().height(260.dp)) {
                    MenuFloatingActionButton(
                        expanded = { fab },
                        onExpandedChange = { fab = it },
                        modifier = Modifier.align(Alignment.Center),
                        buttonContent = { expanded ->
                            Icon(
                                if (expanded) Icons.Default.Close else Icons.Default.Add,
                                if (expanded) demoText(Res.string.demo_close_floating_actions)
                                else demoText(Res.string.demo_open_floating_actions),
                            )
                        },
                        menuContent = {
                            OverlayMenuItem(
                                { Text(demoText(Res.string.demo_show_panel)) },
                                {
                                    fab = false
                                    dialog = true
                                },
                            )
                            OverlayMenuItem(
                                { Text(demoText(Res.string.demo_show_alert)) },
                                {
                                    fab = false
                                    alert = true
                                },
                            )
                        },
                    )
                }
            }
        }
    }
    OverlayDialog(
        onDismissRequest = { dialog = false },
        visible = { dialog },
        sharedClipShape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                demoText(Res.string.demo_panel_material),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                demoText(
                    Res.string
                        .demo_a_stable_foreground_and_automatic_overlay_sampling_nested_modal_panels_have_independent_interaction_lighting,
                ),
            )
            Button(onClick = { nested = true }) {
                Text(demoText(Res.string.demo_open_another_panel))
            }
            TextButton(onClick = { dialog = false }) {
                Text(demoText(Res.string.demo_close))
            }
            OverlayAlertDialog(
                onDismissRequest = { nested = false },
                visible = { nested },
                confirmButton = {
                    TextButton(onClick = { nested = false }) {
                        Text(demoText(Res.string.demo_done))
                    }
                },
                title = { Text(demoText(Res.string.demo_nested_modal)) },
                text = {
                    Text(demoText(Res.string.demo_the_lower_panel_remains_protected_from_input))
                },
            )
        }
    }
    OverlayAlertDialog(
        onDismissRequest = { alert = false },
        visible = { alert },
        confirmButton = {
            TextButton(onClick = { alert = false }) {
                Text(demoText(Res.string.demo_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { alert = false }) {
                Text(demoText(Res.string.demo_cancel))
            }
        },
        title = { Text(demoText(Res.string.demo_material_alert)) },
        text = {
            Text(demoText(Res.string.demo_glass_rendering_material_colors_and_accessible_dismissal))
        },
    )
    OverlayBottomSheet(onDismissRequest = { sheet = false }, visible = { sheet }) {
        Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                demoText(Res.string.demo_draggable_glass_sheet),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                demoText(
                    Res.string
                        .demo_drag_between_partial_expanded_and_hidden_states_or_dismiss_with_back,
                ),
            )
            TextButton(onClick = { sheet = false }) {
                Text(demoText(Res.string.demo_close_sheet))
            }
        }
    }
    OverlaySnackbarHost(snackbar, Modifier.padding(bottom = 112.dp))
}

@Composable
internal fun SettingsPage(store: MutableStore<DemoPreferences>) {
    val describer = remember(store) { store.settingsDescriber() }
    CardSettings(
        describer,
        modifier = Modifier.fillMaxSize(),
        sliderCommitMode =
            if (store.value.appearance.liveSliders) SliderCommitMode.WhileDragging
            else SliderCommitMode.OnRelease,
        contentPadding = DemoContentPadding,
    )
}

@Composable
internal fun SettingsCatalogPage(store: MutableStore<FeatureSettings>, manual: ManualSettingsTree) {
    val generated = remember(store) { store.settingsDescriber() }
    val composite =
        remember(generated, manual) {
            CompositeSettingsTree(
                listOf(
                    SettingsSource("generated", generated, generated),
                    SettingsSource("runtime", manual),
                ),
            )
        }
    CardSettings(
        composite,
        modifier = Modifier.fillMaxSize().testTag("settings-catalog"),
        contentPadding = DemoContentPadding,
        customEditor = { item, draft, change ->
            if (item.rendererId == "accent")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Accent.entries.forEach { accent ->
                        FilterChip(draft == accent, { change(accent) }, accentLabel(accent))
                    }
                }
            else Text("${demoText(Res.string.demo_no_editor_registered_for)} ${item.rendererId}")
        },
    )
}
