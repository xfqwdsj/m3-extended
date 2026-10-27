package top.ltfan.m3x.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import top.ltfan.backdrop.BackdropEdge
import top.ltfan.backdrop.BackdropFill
import top.ltfan.backdrop.BackdropRamp
import top.ltfan.backdrop.BackdropRampAnchors
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.backdrop.rememberStackedBackdropLayer
import top.ltfan.m3x.demo.generated.resources.Res
import top.ltfan.m3x.demo.generated.resources.demo_lab_option
import top.ltfan.m3x.demo.generated.resources.demo_lab_sample
import top.ltfan.m3x.material.FilterChip
import top.ltfan.m3x.material.backdrop.backdropScrollEdge
import top.ltfan.m3x.material.backdrop.theme.backdrop
import top.ltfan.m3x.material.theme.extended

/** Independent vertical and horizontal scroll edges over the same page and captured controls. */
@Composable
internal fun ScrollEdgeLabPreview(modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf(0) }
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    val page = rememberBackdropLayer()
    val verticalEdge = rememberBackdropLayer()
    val controls = rememberBackdropLayer()
    val horizontalSource = rememberStackedBackdropLayer(listOf(page, verticalEdge, controls))
    val edgeMaterial = MaterialTheme.extended.backdrop.scrollEdge
    val verticalMaterial =
        edgeMaterial.copy(
            ramp = BackdropRamp(anchors = BackdropRampAnchors.offsets(24.dp, 48.dp)),
        )
    val horizontalMaterial = edgeMaterial.copy(ramp = BackdropRamp())
    BackdropHost(page) {
        Box(modifier) {
            Column(
                Modifier.fillMaxSize()
                    .captureBackdrop(page)
                    .background(demoBackground(PreviewBackground.Artwork))
                    .testTag("lab-scroll-content")
                    .verticalScroll(vertical)
                    .padding(top = 124.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(8) {
                    LabPreviewSamples(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            Box(Modifier.fillMaxWidth()) {
                // The edge draws outside the controls' capture so its fade extends into the page.
                Spacer(
                    Modifier.matchParentSize()
                        .testTag("lab-vertical-edge")
                        .backdropScrollEdge(
                            visible = { vertical.canScrollBackward },
                            material = verticalMaterial,
                            exportedBackdrop = verticalEdge.layer,
                        ),
                )
                Column(Modifier.fillMaxWidth().captureBackdrop(controls)) {
                    Box(
                        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            demoText(Res.string.demo_lab_sample),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth()
                            .height(56.dp)
                            .testTag("lab-scroll-options")
                            .horizontalScroll(horizontal)
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        repeat(8) { index ->
                            FilterChip(
                                selected = selected == index,
                                onClick = { selected = index },
                                label = {
                                    Text("${demoText(Res.string.demo_lab_option)} ${index + 1}")
                                },
                            )
                        }
                    }
                }
                BackdropHost(horizontalSource) {
                    Box(
                        Modifier.align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(56.dp)
                            .clipToBounds(),
                    ) {
                        for ((edge, alignment) in
                            listOf(
                                BackdropEdge.Start to Alignment.CenterStart,
                                BackdropEdge.End to Alignment.CenterEnd,
                            )) {
                            Spacer(
                                Modifier.align(alignment)
                                    .width(24.dp)
                                    .height(56.dp)
                                    .backdropScrollEdge(
                                        edge = edge,
                                        visible = {
                                            if (edge == BackdropEdge.Start)
                                                horizontal.canScrollBackward
                                            else horizontal.canScrollForward
                                        },
                                        material = horizontalMaterial,
                                        fill = BackdropFill.None,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}
