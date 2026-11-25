package top.ltfan.m3x.demo

import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource
import top.ltfan.m3x.demo.generated.resources.MR
import top.ltfan.m3x.demo.generated.resources.demo_amber
import top.ltfan.m3x.demo.generated.resources.demo_blue
import top.ltfan.m3x.demo.generated.resources.demo_mint
import top.ltfan.m3x.resource.Resource
import top.ltfan.m3x.resource.compose.composeResource

@Composable internal fun demoText(resource: StringResource): String = stringResource(resource)

@Composable
internal fun demoText(resource: StringResource, vararg args: Any): String =
    stringResource(resource, *args)

internal fun demoResource(resource: StringResource): Resource<String> = composeResource {
    stringResource(resource)
}

@Composable
internal fun accentLabel(accent: Accent): String =
    demoText(
        when (accent) {
            Accent.Blue -> MR.strings.demo_blue
            Accent.Mint -> MR.strings.demo_mint
            Accent.Amber -> MR.strings.demo_amber
        },
    )
