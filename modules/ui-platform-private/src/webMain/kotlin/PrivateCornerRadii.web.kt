package top.ltfan.m3x.compose.ui.privateapi

import androidx.compose.runtime.Composable

@Composable
public actual fun ProvidePrivateCornerRadii(content: @Composable () -> Unit) {
    content()
}
