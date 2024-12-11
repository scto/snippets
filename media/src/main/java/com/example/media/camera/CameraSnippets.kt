package com.example.media.camera

import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import androidx.activity.compose.LocalActivity
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// [START android_media_camera_preview_viewmodel]
class CameraPreviewViewModel(private val appContext: Context) : ViewModel() {
    // used to set up a link between the Camera and your UI.
    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest
    private var surfaceMeteringPointFactory: SurfaceOrientedMeteringPointFactory? = null
    private val _faces = MutableStateFlow(listOf<Rect>())
    val faces: StateFlow<List<Rect>> = _faces.asStateFlow()

    private val cameraPreviewUseCase = Preview.Builder()
        .apply {
            Camera2Interop.Extender(this)
                .setCaptureRequestOption(
                    CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                    CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL
                )
                .setSessionCaptureCallback(object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                        result.get(CaptureResult.STATISTICS_FACES)
                            ?.map { face -> face.bounds }
                            ?.toList()
                            ?.let { faces -> _faces.update { faces } }
                    }
                })
        }
        .build().apply {
            setSurfaceProvider { newSurfaceRequest ->
                _surfaceRequest.update { newSurfaceRequest }
                surfaceMeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                    newSurfaceRequest.resolution.width.toFloat(),
                    newSurfaceRequest.resolution.height.toFloat()
                )
            }
        }

    suspend fun bindToCamera(lifecycleOwner: LifecycleOwner) {
        val processCameraProvider = ProcessCameraProvider.awaitInstance(appContext)
        processCameraProvider.bindToLifecycle(
            lifecycleOwner, DEFAULT_FRONT_CAMERA, cameraPreviewUseCase
        )

        // Cancellation signals we're done with the camera
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
        Column(
            modifier = modifier.fillMaxSize().wrapContentSize().widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val textToShow = if (cameraPermissionState.status.shouldShowRationale) {
                // If the user has denied the permission but the rationale can be shown,
                // then gently explain why the app requires this permission
                "Whoops! Looks like we need your camera to work our magic!" +
                    "Don't worry, we just wanna see your pretty face (and maybe some cats). " +
                    "Grant us permission and let's get this party started!"
            } else {
                // If it's the first time the user lands on this feature, or the user
                // doesn't want to be asked again for this permission, explain that the
                // permission is required
                "Hi there! We need your camera to work our magic! ✨\n" +
                    "Grant us permission and let's get this party started! \uD83C\uDF89"
            }
            Text(textToShow, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Unleash the Camera!")
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
    val faces by viewModel.faces.collectAsStateWithLifecycle()

    LaunchedEffect(lifecycleOwner) { viewModel.bindToCamera(lifecycleOwner) }

    CameraPreviewContent(
        surfaceRequest = surfaceRequest,
        faces = faces,
        onTapToFocus = viewModel::onTapToFocus,
        modifier = modifier
    )
}
// [END android_media_camera_preview_content]

private val colors = listOf(Color(0xFF09D8E6), Color(0xFFE6C709), Color(0xFFE60991))

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CameraPreviewContent(
    surfaceRequest: SurfaceRequest?,
    faces: List<Rect>,
    onTapToFocus: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayFeatures = LocalActivity.current?.let { calculateDisplayFeatures(it) }
    val isTableTopPosture = displayFeatures
        ?.filterIsInstance<FoldingFeature>()
        ?.any { isTableTopPosture(it) } ?: false

    var chosenColorIndex by remember { mutableStateOf(0) }

    LookaheadScope {
        Column(modifier.safeContentPadding()) {
            surfaceRequest?.let {
                MainContent(
                    it, colors[chosenColorIndex], faces, onTapToFocus,
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(if (isTableTopPosture) 0.5f else 1f)
                        .animateBounds(this@LookaheadScope)
                )
            }
            AnimatedVisibility(isTableTopPosture, enter = fadeIn() + slideInVertically { it }) {
                SupportingContent(
                    colors, chosenColorIndex, { chosenColorIndex = it }
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    surfaceRequest: SurfaceRequest,
    chosenColor: Color,
    faces: List<Rect>,
    onTapToFocus: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedColor by animateColorAsState(chosenColor)
    var showAutofocusBox by remember { mutableStateOf(false) }
    var showAutofocusCoords by remember { mutableStateOf<Offset?>(null) }
    LaunchedEffect(showAutofocusCoords) {
        if (showAutofocusCoords == null) return@LaunchedEffect
        showAutofocusBox = true
        delay(1000)
        showAutofocusBox = false
    }
    Box(
        modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(
                8.dp, animatedColor,
                RoundedCornerShape(24.dp)
            )
    ) {
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

        var transformationInfo by remember { mutableStateOf<SurfaceRequest.TransformationInfo?>(null) }
        DisposableEffect(surfaceRequest) {
            surfaceRequest.setTransformationInfoListener(Runnable::run) {
                transformationInfo = it
            }
            onDispose { surfaceRequest.clearTransformationInfoListener() }
        }

        val faceRects = faces.map { face ->
            val bufferToCompose = Matrix().apply {
                setFrom(coordinateTransformer.transformMatrix)
                invert()
            }
            val sensorToBuffer = Matrix().apply {
                transformationInfo?.let {
                    setFrom(it.sensorToBufferTransform)
                }
            }
            val faceRectOnBuffer = sensorToBuffer.map(face.toComposeRect())
            bufferToCompose.map(faceRectOnBuffer)
        }

        AnimatedVisibility(faceRects.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            Canvas(Modifier.fillMaxSize()) {
                // Fill the whole space with the color
                drawRect(animatedColor)
                // Then extract each face and make it transparent
                faceRects.forEach { faceRect ->
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color.Black, Color.Transparent),
                            center = faceRect.center,
                            radius =  faceRect.minDimension,
                        ),
                        blendMode = BlendMode.DstOut
                    )
                }
            }
        }

    }
}

@Composable
fun SupportingContent(
    colors: List<Color>,
    chosenColorIndex: Int,
    onColorChosen: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEachIndexed { i, color ->
            Spacer(
                Modifier.size(72.dp).clip(CircleShape)
                    .clickable { onColorChosen(i) }
                    .background(color)
                    .then(
                        if (i == chosenColorIndex)
                            Modifier.border(2.dp, Color.Black, CircleShape)
                        else Modifier
                    ))
        }
    }
}

@OptIn(ExperimentalContracts::class)
fun isTableTopPosture(foldFeature: FoldingFeature?): Boolean {
    contract { returns(true) implies (foldFeature != null) }
    return foldFeature?.state == FoldingFeature.State.HALF_OPENED &&
        foldFeature.orientation == FoldingFeature.Orientation.HORIZONTAL
}
