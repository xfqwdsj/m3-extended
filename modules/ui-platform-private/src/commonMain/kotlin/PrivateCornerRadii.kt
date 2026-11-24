package top.ltfan.m3x.compose.ui.privateapi

import androidx.compose.runtime.Composable

/** Installs private platform corner readers for the current composition when supported. */
@Composable public expect fun ProvidePrivateCornerRadii(content: @Composable () -> Unit)
