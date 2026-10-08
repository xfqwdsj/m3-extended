package top.ltfan.m3x.demo

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import top.ltfan.m3x.demo.generated.resources.Res
import top.ltfan.m3x.demo.generated.resources.demo_amber
import top.ltfan.m3x.demo.generated.resources.demo_blue
import top.ltfan.m3x.demo.generated.resources.demo_mint
import top.ltfan.m3x.resource.Resource
import top.ltfan.m3x.resource.compose.composeResource

@Composable internal fun demoText(resource: StringResource): String = stringResource(resource)

internal fun demoResource(resource: StringResource): Resource<String> = composeResource {
    stringResource(resource)
}

@Composable
internal fun accentLabel(accent: Accent): String =
    demoText(
        when (accent) {
            Accent.Blue -> Res.string.demo_blue
            Accent.Mint -> Res.string.demo_mint
            Accent.Amber -> Res.string.demo_amber
        },
    )
