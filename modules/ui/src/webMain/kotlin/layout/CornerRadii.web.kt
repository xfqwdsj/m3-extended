package top.ltfan.m3x.compose.ui.layout

import androidx.compose.runtime.Composable

@Composable internal actual fun readPublicDisplayCornerRadii(): CornerRadii? = null

@Composable internal actual fun readPublicWindowCornerRadii(): CornerRadii? = null

@Composable
public actual fun rememberWindowPresentation(): WindowPresentation = WindowPresentation.Unknown
