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

package com.example.camera.systemcamera

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File

// [START android_camera_systemcamera_take_picture]
@Composable
fun TakePicture_WithCameraApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uri = remember { getImageUri(context) }
    var captureSuccess by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { captureSuccess = it }

    Box(modifier, contentAlignment = Alignment.Center) {
        if (captureSuccess) {
            AsyncImage(uri, contentDescription = null)
        } else {
            Button({ launcher.launch(uri) }) {
                Text("Take picture")
            }
        }
    }
}
// [END android_camera_systemcamera_take_picture]

// [START android_camera_systemcamera_record_video]
@Composable
fun RecordVideo_WithCameraApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uri = remember { getVideoUri(context) }
    var captureSuccess by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { captureSuccess = it }

    Box(modifier, contentAlignment = Alignment.Center) {
        if (captureSuccess) {
            Text("Successfully saved video")
        } else {
            Button({ launcher.launch(uri) }) {
                Text("Record video")
            }
        }
    }
}
// [END android_camera_systemcamera_record_video]

private fun getImageUri(context: Context): Uri =
    FileProvider.getUriForFile(
        context,
        context.applicationContext.packageName + ".provider",
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "testImage.jpg"
        )
    )

private fun getVideoUri(context: Context): Uri =
    FileProvider.getUriForFile(
        context,
        context.applicationContext.packageName + ".provider",
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "testVideo.MP4"
        )
    )
