/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.camera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.camera.Screen.HOME
import com.example.camera.Screen.RECORD_VIDEO_WITH_CAMERA_APP
import com.example.camera.Screen.TAKE_PICTURE_WITH_CAMERA_APP
import com.example.camera.systemcamera.RecordVideo_WithCameraApp
import com.example.camera.systemcamera.TakePicture_WithCameraApp
import com.example.camera.ui.theme.SnippetsTheme

class CameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnippetsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MyApp(
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

enum class Screen { HOME, TAKE_PICTURE_WITH_CAMERA_APP, RECORD_VIDEO_WITH_CAMERA_APP }
@Composable
fun MyApp(modifier: Modifier = Modifier) {
    var activeScreen by remember { mutableStateOf(HOME) }
    Box(modifier, contentAlignment = Alignment.Center) {
        when (activeScreen) {
            HOME -> Home(onScreenChange = { activeScreen = it })
            TAKE_PICTURE_WITH_CAMERA_APP -> TakePicture_WithCameraApp()
            RECORD_VIDEO_WITH_CAMERA_APP -> RecordVideo_WithCameraApp()
        }
    }
}

@Composable
fun Home(
    onScreenChange: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Button({ onScreenChange(TAKE_PICTURE_WITH_CAMERA_APP) }) {
            Text("Take Picture with Camera App")
        }
        Button({ onScreenChange(RECORD_VIDEO_WITH_CAMERA_APP) }) {
            Text("Record Video with Camera App")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MyAppPreview() {
    SnippetsTheme {
        MyApp()
    }
}
