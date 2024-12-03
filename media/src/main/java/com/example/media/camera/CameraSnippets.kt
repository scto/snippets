package com.example.media.camera

import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.window.layout.FoldingFeature
import com.google.accompanist.adaptive.calculateDisplayFeatures
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.roundToInt

// [START android_media_camera_preview_viewmodel]
class CameraPreviewViewModel(private val appContext: Context) : ViewModel() {
    // used to set up a link between the Camera and your UI.
    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest
    private var surfaceMeteringPointFactory: SurfaceOrientedMeteringPointFactory? = null

    private val cameraPreviewUseCase = Preview.Builder().build().apply {
        setSurfaceProvider {
            _surfaceRequest.value = it
            surfaceMeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                it.resolution.width.toFloat(),
                it.resolution.height.toFloat()
            )
        }
    }

    suspend fun runCamera(lifecycleOwner: LifecycleOwner) {
        val processCameraProvider = ProcessCameraProvider.awaitInstance(appContext)
        processCameraProvider.bindToLifecycle(
            lifecycleOwner, DEFAULT_BACK_CAMERA, cameraPreviewUseCase
        )

        try { awaitCancellation() } finally { processCameraProvider.unbindAll() }
    }

    fun onTapToFocus(offset: Offset) {
        viewModelScope.launch {
            val point = surfaceMeteringPointFactory?.createPoint(offset.x, offset.y)
            val cameraControl = cameraPreviewUseCase.camera?.cameraControl
            if (point != null && cameraControl != null) {
                val meteringAction = FocusMeteringAction.Builder(point).build()
                cameraControl.startFocusAndMetering(meteringAction)
            }
        }
    }
}
// [END android_media_camera_preview_viewmodel]

// [START android_media_camera_preview_screen]
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPreviewScreen(
    viewModel: CameraPreviewViewModel,
    modifier: Modifier = Modifier
) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    if (cameraPermissionState.status.isGranted) {
        CameraPreviewContent(viewModel, modifier)
    } else {
        Column(modifier.safeContentPadding()) {
            val textToShow = if (cameraPermissionState.status.shouldShowRationale) {
                // If the user has denied the permission but the rationale can be shown,
                // then gently explain why the app requires this permission
                "The camera is important for this app. Please grant the permission."
            } else {
                // If it's the first time the user lands on this feature, or the user
                // doesn't want to be asked again for this permission, explain that the
                // permission is required
                "Camera permission required for this feature to be available. " +
                    "Please grant the permission"
            }
            Text(textToShow)
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Request permission")
            }
        }
    }
}
// [END android_media_camera_preview_screen]

// [START android_media_camera_preview_content]
@Composable
fun CameraPreviewContent(
    viewModel: CameraPreviewViewModel,
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()

    LaunchedEffect(lifecycleOwner) { viewModel.runCamera(lifecycleOwner) }

    CameraPreviewContent(
        surfaceRequest = surfaceRequest,
        onTapToFocus = viewModel::onTapToFocus,
        modifier = modifier
    )
}
// [END android_media_camera_preview_content]

@Composable
fun CameraPreviewContent(
    surfaceRequest: SurfaceRequest?,
    onTapToFocus: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayFeatures = LocalActivity.current?.let { calculateDisplayFeatures(it) }
    val isTableTopPosture = displayFeatures
        ?.filterIsInstance<FoldingFeature>()
        ?.any { isTableTopPosture(it) } ?: false


    MyAnimatedTwoPane(
        isDualPane = isTableTopPosture,
        mainContent = {
            surfaceRequest?.let {
                MainContent(it, onTapToFocus, Modifier.fillMaxSize())
            }
        },
        supportingContent = {
            Box(Modifier.fillMaxSize()) {
                Button(onClick = { /*TODO*/ }, Modifier.align(Alignment.Center)) {
                    Text("Take picture")
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun MainContent(
    surfaceRequest: SurfaceRequest,
    onTapToFocus: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAutofocusBox by remember { mutableStateOf(false) }
    var showAutofocusCoords by remember { mutableStateOf<Offset?>(null) }
    LaunchedEffect(showAutofocusCoords) {
        if (showAutofocusCoords == null) return@LaunchedEffect
        showAutofocusBox = true
        delay(1000)
        showAutofocusBox = false
    }
    Box(modifier) {
        val coordinateTransformer = remember { MutableCoordinateTransformer() }
        CameraXViewfinder(
            surfaceRequest = surfaceRequest,
            coordinateTransformer = coordinateTransformer,
            modifier = Modifier
                .pointerInput(coordinateTransformer, onTapToFocus) {
                    detectTapGestures { offset ->
                        with(coordinateTransformer) {
                            onTapToFocus(offset.transform())
                            showAutofocusCoords = offset
                        }
                    }
                }
        )
        AnimatedVisibility(
            visible = showAutofocusBox,
            enter = fadeIn(tween()), exit = fadeOut(tween()),
            modifier = Modifier
                .offset { showAutofocusCoords?.round() ?: IntOffset.Zero }
                .offset((-24).dp, (-24).dp)
        ) {
            // You have all Compose functionality at your hands here, and can draw / animate
            // whatever you like!
            Spacer(
                Modifier
                    .border(2.dp, Color.White, CircleShape)
                    .size(48.dp)
            )
        }
    }
}


@OptIn(ExperimentalContracts::class)
fun isTableTopPosture(foldFeature: FoldingFeature?): Boolean {
    contract { returns(true) implies (foldFeature != null) }
    return foldFeature?.state == FoldingFeature.State.HALF_OPENED &&
        foldFeature.orientation == FoldingFeature.Orientation.HORIZONTAL
}

@Composable
fun MyAnimatedTwoPane(
    isDualPane: Boolean,
    mainContent: @Composable () -> Unit,
    supportingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedFraction by animateFloatAsState(
        // this is a simplification, you'd want to use the fold bounds
        if (isDualPane) 0.5f else 1f
    )
    Layout(
        modifier = modifier,
        contents = listOf(mainContent, supportingContent)
    ) { (mainMeasurables, supportingMeasurables), constraints ->
        val desiredMainHeight = (constraints.maxHeight * animatedFraction).roundToInt()
        val desiredSupportingHeight = (constraints.maxHeight * (1f - animatedFraction)).roundToInt()

        val mainConstraints = constraints.copy(maxHeight = desiredMainHeight)
        val supportingConstraints = constraints.copy(maxHeight = desiredSupportingHeight)

        val mainPlaceables = mainMeasurables.map { it.measure(mainConstraints) }
        val supportingPlaceables = supportingMeasurables.map { it.measure(supportingConstraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            mainPlaceables.forEach { it.place(0, 0) }
            val mainHeight = mainPlaceables.sumOf { it.height }
            supportingPlaceables.forEach { it.place(0, mainHeight) }
        }
    }
}
