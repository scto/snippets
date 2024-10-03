package com.example.compose.snippets.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionBarMenu(modifier: Modifier = Modifier) {
    var mDisplayMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(text = "Compose") },
        actions = {
            IconButton(onClick = { mDisplayMenu = !mDisplayMenu }) {
                Icon(Icons.Default.MoreVert, "")
            }
            DropdownMenu(expanded = mDisplayMenu, onDismissRequest = { mDisplayMenu = false }) {
                DropdownMenuItem(text = { Text("Refresh") }, onClick = { /* Handle refresh! */ })
                DropdownMenuItem(text = { Text("Settings") }, onClick = { /* Handle settings! */ })
            }
        }
    )
}

@Preview
@Composable
private fun ActionBarMenuPreview() {
    ActionBarMenu()
}

@Composable
fun OptionsMenu(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopStart)
    ) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Localized description")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Refresh") }, onClick = { /* Handle refresh! */ })
            DropdownMenuItem(text = { Text("Settings") }, onClick = { /* Handle settings! */ })
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Send Feedback") }, onClick = { /* Handle send feedback! */ })
        }
    }
}

@Preview
@Composable
private fun DropDownMenuPreview() {
    OptionsMenu()
}

@Composable
fun PhoneNumberCodePicker(
    countryCodes: Map<String, String>,
    selectedCountry: String,
    modifier: Modifier = Modifier,
    value: String = "",
    onValueChange: (String) -> Unit = {},
    onCountryCodeChange: (String) -> Unit
) {
    val interactionSource by remember { mutableStateOf(MutableInteractionSource()) }
    val numericRegex = Regex("[^0-9]")
    var expandedOptions by remember { mutableStateOf(false) }


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .wrapContentSize(Alignment.TopCenter)
    ) {
        OutlinedTextField(
            modifier = modifier,
            singleLine = true,
            value = value,
            onValueChange = {
                val stripped = numericRegex.replace(it, "")
                val phoneNumber = if (stripped.length >= 10) {
                    stripped.substring(0..9)
                } else {
                    stripped
                }
                onValueChange(phoneNumber)
            },
            leadingIcon = {
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { expandedOptions = !expandedOptions }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .width(64.dp)
                            .padding(start = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = countryCodes[selectedCountry] ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            lineHeight = 8.sp
                        )
                        Text(
                            text = selectedCountry,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            lineHeight = 14.sp
                        )
                    }
                    VerticalDivider(modifier = Modifier.padding(start = 2.dp, end = 8.dp))
                }
            },
            trailingIcon = {
                Icon(imageVector = Icons.Filled.Phone, contentDescription = "Phone icon")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        DropdownMenu(
            expanded = expandedOptions, onDismissRequest = { expandedOptions = false }
        ) {
            countryCodes.forEach { selectionOption ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(selectionOption.key)
                            Text(selectionOption.value)
                        }
                    },
                    onClick = {
                        onCountryCodeChange(selectionOption.key)
                        expandedOptions = !expandedOptions
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun PhoneNumberCodePickerPreview() {
    val countryCodes = mapOf(
        "United States" to "+1",
        "Canada" to "+1",
    )

    var selectedCountry by remember { mutableStateOf("United States") }
    var phoneNumber by remember { mutableStateOf("") }

    PhoneNumberCodePicker(
        countryCodes = countryCodes,
        selectedCountry = selectedCountry,
        value = phoneNumber,
        onValueChange = { phoneNumber = it },
        onCountryCodeChange = { country -> selectedCountry = country }
    )

    LaunchedEffect(selectedCountry, phoneNumber) {
        Log.i("PhoneNumber", "${countryCodes[selectedCountry]} $phoneNumber")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenu(options: List<String>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf(options[0]) }
    // We want to react on tap/press on TextField to show menu
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            readOnly = true,
            value = selectedOptionText,
            onValueChange = { },
            label = { Text("Label") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true) // This is the key that enables the anchor for the dropdown menu
                .clickable { expanded = true }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(text = selectionOption) },
                    onClick = {
                        selectedOptionText = selectionOption
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun ExposedDropdownMenuBoxPreview() {
    val options = listOf("Option 1", "Option 2", "Option 3")
    ExposedDropdownMenu(options = options)
}

