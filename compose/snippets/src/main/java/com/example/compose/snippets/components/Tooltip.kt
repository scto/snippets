package com.example.compose.snippets.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainTooltipSample(
    modifier: Modifier = Modifier,
    plainTooltipText: String = "Add to favorites"
) {
    TooltipBox(
        modifier = modifier,
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip { Text(plainTooltipText) }
        },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = { /* Icon button's click event */ }) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Localized Description"
            )
        }
    }
}

@Preview
@Composable
private fun PlainTooltipSamplePreview() {
    PlainTooltipSample()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichTooltipSample(
    modifier: Modifier = Modifier,
    richTooltipSubheadText: String = "Rich Tooltip",
    richTooltipText: String = "Rich tooltips supports multiple lines of informational text."
) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    TooltipBox(
        modifier = modifier,
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            RichTooltip(
                title = { Text(richTooltipSubheadText) }
            ) {
                Text(richTooltipText)
            }
        }, state = tooltipState
    ) {
        IconButton(onClick = { /* Icon button's click event */ }) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Localized Description"
            )
        }
    }
}

@Preview
@Composable
private fun RichTooltipSamplePreview() {
    RichTooltipSample()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichTooltipWithCustomCaretSample(
    modifier: Modifier = Modifier,
    richTooltipSubheadText: String = "Custom Rich Tooltip",
    richTooltipText: String = "Rich tooltips supports multiple lines of informational text.",
    richTooltipActionText: String = "Dismiss"
) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        modifier = modifier,
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            RichTooltip(
                title = { Text(richTooltipSubheadText) },
                action = {
                    Row {
                        TextButton(onClick = { scope.launch { tooltipState.dismiss() } }) {
                            Text(richTooltipActionText)
                        }
                        TextButton(onClick = { scope.launch { tooltipState.dismiss() } }) {
                            Text("Learn more")
                        }
                    }
                },
                caretSize = DpSize(32.dp, 16.dp)
            ) {
                Text(richTooltipText)
            }
        },
        state = tooltipState
    ) {
        IconButton(onClick = { /* Icon button's click event */ }) {
            Icon(imageVector = Icons.Filled.Camera, contentDescription = "Localized Description")
        }
    }
}

@Preview
@Composable
private fun RichTooltipWithCustomCaretSamplePreview() {
    RichTooltipWithCustomCaretSample()
}