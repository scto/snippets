package com.example.camera.systemcamera

import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.example.camera.util.SmoothImmersiveRotationEffect

@Composable
fun TakePicture_HighLevel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // As long as this screen is showing, go to immersive mode and don't animate on rotation
    SmoothImmersiveRotationEffect(context)

    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember {
        LifecycleCameraController(context).apply {
            bindToLifecycle(lifecycleOwner)
        }
    }
    PreviewViewWrapper(controller, onTakePicture = { /*TODO*/ }, modifier)
}

// [START android_camera_preview_wrapper]
@Composable
private fun PreviewViewWrapper(
    controller: CameraController,
    onTakePicture: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        AndroidView(
            factory = { PreviewView(it).apply { this.controller = controller } },
            modifier = Modifier.fillMaxSize()
        )
        Button({ onTakePicture() }, Modifier.align(Alignment.BottomCenter)) {
            Text("Take Picture")
        }
    }
}
// [END android_camera_preview_wrapper]
