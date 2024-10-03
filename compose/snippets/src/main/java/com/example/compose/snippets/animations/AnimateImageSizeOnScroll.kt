package com.example.compose.snippets.animations

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedImageContent(
    nestedScrollConnection: NestedScrollConnection,
    currentImgSize: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.nestedScroll(nestedScrollConnection)
    ) {
        LazyColumn(
            modifier = Modifier.offset {
                IntOffset(0, currentImgSize.roundToPx())
            }
        ) {
            items(100, key = { it }) {
                Text(
                    text = "Scroll down to see the image size animating...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        Image(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val heightPx = currentImgSize
                        .coerceAtLeast(0.dp)
                    val placeable = measurable.measure(
                        constraints.copy(
                            maxHeight = heightPx.roundToPx()
                        )
                    )
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                },
            painter = ColorPainter(Color.Blue),
            contentDescription = "Animated Image",
        )
    }
}

@Composable
fun AnimatedImageSizeOnScroll(
    modifier: Modifier = Modifier,
    maxImgSize: Dp = 300.dp,
    minImgSize: Dp = 100.dp
) {
    var currentImgSize by remember { mutableStateOf(maxImgSize) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newImgSize = currentImgSize + delta.dp
                val previousImgSize = currentImgSize
                currentImgSize = newImgSize.coerceIn(minImgSize, maxImgSize)
                val consumed = currentImgSize - previousImgSize

                return Offset(0f, consumed.value)
            }
        }
    }
    AnimatedImageContent(nestedScrollConnection, currentImgSize, modifier)
}

@Preview
@Composable
private fun AnimatedImageSizeOnScrollPreview() {
    AnimatedImageSizeOnScroll()
}
