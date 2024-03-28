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

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.camera.Screen.CONCURRENT_CAMERA_PREVIEW
import com.example.camera.Screen.HOME
import com.example.camera.Screen.PREVIEW_BASIC
import com.example.camera.Screen.PREVIEW_VIEWFINDER
import com.example.camera.Screen.RECORD_VIDEO_WITH_CAMERA_APP
import com.example.camera.Screen.TAKE_PICTURE_HIGH_LEVEL
import com.example.camera.Screen.TAKE_PICTURE_WITH_CAMERA_APP
import com.example.camera.concurrent.ConcurrentCameraScreen
import com.example.camera.systemcamera.RecordVideo_WithCameraApp
import com.example.camera.systemcamera.TakePicture_HighLevel
import com.example.camera.systemcamera.TakePicture_WithCameraApp
import com.example.camera.ui.theme.SnippetsTheme
import com.example.camera.viewfinder.LowLevelPreview
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState


class CameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            SnippetsTheme {
                MyApp(Modifier.fillMaxSize())
            }
        }
    }
}

enum class Screen {
    HOME,
    TAKE_PICTURE_WITH_CAMERA_APP,
    RECORD_VIDEO_WITH_CAMERA_APP,
    TAKE_PICTURE_HIGH_LEVEL,
    PREVIEW_BASIC,
    PREVIEW_VIEWFINDER,
    CONCURRENT_CAMERA_PREVIEW
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MyApp(modifier: Modifier = Modifier) {
    // For the snippets to run correctly, we need to request the CAMERA permission.
    // DO NOT COPY THIS CODE, it's not following best practices!
    val permissionState =
        rememberPermissionState(permission = Manifest.permission.CAMERA)

    if(permissionState.status.isGranted) {
        var activeScreen by rememberSaveable { mutableStateOf(HOME) }
        Box(modifier, contentAlignment = Alignment.Center) {
            when (activeScreen) {
                HOME -> Home(onScreenChange = { activeScreen = it })
                TAKE_PICTURE_WITH_CAMERA_APP -> TakePicture_WithCameraApp()
                RECORD_VIDEO_WITH_CAMERA_APP -> RecordVideo_WithCameraApp()
                TAKE_PICTURE_HIGH_LEVEL -> TakePicture_HighLevel()
                PREVIEW_BASIC -> LowLevelPreview(useViewfinderLib = false)
                PREVIEW_VIEWFINDER -> LowLevelPreview(useViewfinderLib = true)
                CONCURRENT_CAMERA_PREVIEW -> ConcurrentCameraScreen()
            }
        }
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            Button(onClick = {
                permissionState.launchPermissionRequest()
            }
            ) {
                Text("Request camera permission")
            }
        }
    }

}

@Composable
fun Home(
    onScreenChange: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier
                .padding(innerPadding)
                .fillMaxSize()
                .wrapContentSize()) {
            Button({ onScreenChange(TAKE_PICTURE_WITH_CAMERA_APP) }) {
                Text("Take Picture with Camera App")
            }
            Button({ onScreenChange(RECORD_VIDEO_WITH_CAMERA_APP) }) {
                Text("Record Video with Camera App")
            }
            Button({ onScreenChange(TAKE_PICTURE_HIGH_LEVEL) }) {
                Text("Take Picture with High Level API")
            }
            Button({ onScreenChange(PREVIEW_BASIC) }) {
                Text("Preview without viewfinder lib")
            }
            Button({ onScreenChange(PREVIEW_VIEWFINDER) }) {
                Text("Preview with viewfinder lib")
            }
            Button({ onScreenChange(CONCURRENT_CAMERA_PREVIEW) }) {
                Text("Concurrent camera preview")
            }
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
