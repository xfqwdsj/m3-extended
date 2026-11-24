package top.ltfan.m3x.material.overlay

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import top.ltfan.m3x.material.overlay.generated.resources.Res
import top.ltfan.m3x.material.overlay.generated.resources.m3x_bottom_sheet_close
import top.ltfan.m3x.material.overlay.generated.resources.m3x_bottom_sheet_collapse_description
import top.ltfan.m3x.material.overlay.generated.resources.m3x_bottom_sheet_dismiss_description
import top.ltfan.m3x.material.overlay.generated.resources.m3x_bottom_sheet_drag_handle_description
import top.ltfan.m3x.material.overlay.generated.resources.m3x_bottom_sheet_expand_description
import top.ltfan.m3x.material.overlay.generated.resources.m3x_bottom_sheet_pane_title
import top.ltfan.m3x.material.overlay.generated.resources.m3x_dialog_dismiss
import top.ltfan.m3x.material.overlay.generated.resources.m3x_dialog_pane_title
import top.ltfan.m3x.material.overlay.generated.resources.m3x_menu_dismiss
import top.ltfan.m3x.material.overlay.generated.resources.m3x_snackbar_dismiss
import top.ltfan.m3x.material.overlay.generated.resources.m3x_snackbar_pane_title
import top.ltfan.m3x.material.overlay.generated.resources.m3x_tooltip_dismiss
import top.ltfan.m3x.material.overlay.generated.resources.m3x_tooltip_long_press_label
import top.ltfan.m3x.material.overlay.generated.resources.m3x_tooltip_pane_description

/** The localized strings the overlay components announce to accessibility services. */
internal object OverlayStrings {
    @Composable fun dialog(): String = stringResource(Res.string.m3x_dialog_pane_title)

    @Composable fun dismissDialog(): String = stringResource(Res.string.m3x_dialog_dismiss)

    @Composable fun dismissMenu(): String = stringResource(Res.string.m3x_menu_dismiss)

    @Composable fun closeSheet(): String = stringResource(Res.string.m3x_bottom_sheet_close)

    @Composable
    fun bottomSheetPaneTitle(): String = stringResource(Res.string.m3x_bottom_sheet_pane_title)

    @Composable
    fun bottomSheetDragHandleDescription(): String =
        stringResource(Res.string.m3x_bottom_sheet_drag_handle_description)

    @Composable
    fun bottomSheetCollapseDescription(): String =
        stringResource(Res.string.m3x_bottom_sheet_collapse_description)

    @Composable
    fun bottomSheetDismissDescription(): String =
        stringResource(Res.string.m3x_bottom_sheet_dismiss_description)

    @Composable
    fun bottomSheetExpandDescription(): String =
        stringResource(Res.string.m3x_bottom_sheet_expand_description)

    @Composable
    fun tooltipLongPressLabel(): String = stringResource(Res.string.m3x_tooltip_long_press_label)

    @Composable
    fun tooltipPaneDescription(): String = stringResource(Res.string.m3x_tooltip_pane_description)

    @Composable fun dismissTooltip(): String = stringResource(Res.string.m3x_tooltip_dismiss)

    @Composable fun snackbarDismiss(): String = stringResource(Res.string.m3x_snackbar_dismiss)

    @Composable fun snackbarPaneTitle(): String = stringResource(Res.string.m3x_snackbar_pane_title)
}
