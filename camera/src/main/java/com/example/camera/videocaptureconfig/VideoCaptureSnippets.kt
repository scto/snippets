package com.example.camera.videocaptureconfig

import android.content.Context
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.DynamicRange
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.camera.util.AsyncCameraProvider
import com.example.camera.util.CoroutineLifecycleOwner
import com.example.camera.util.SmoothImmersiveRotationEffect
import com.example.camera.viewfinder.Viewfinder
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun VideoCaptureScreen(
    modifier: Modifier = Modifier,
    viewModel: VideoCaptureViewModel = viewModel(
        factory = VideoCaptureViewModelFactory(LocalContext.current.applicationContext)
    )
) {
    SmoothImmersiveRotationEffect(LocalContext.current)
    LifecycleStartEffect(viewModel) {
        viewModel.startCamera()
        onStopOrDispose {
            viewModel.stopCamera()
        }
    }

    val surfaceRequestFront by viewModel.surfaceRequestFront.collectAsStateWithLifecycle()
    val surfaceRequestBack by viewModel.surfaceRequestBack.collectAsStateWithLifecycle()

    // TODO: Fix issue with top SurfaceView not adhering to size
    Box(modifier.fillMaxSize()) {
        Viewfinder(surfaceRequestFront, Modifier.fillMaxSize())
        Viewfinder(
            surfaceRequestBack,
            Modifier
                .sizeIn(maxWidth = 200.dp, maxHeight = 400.dp)
                .align(Alignment.BottomEnd)
                .padding(32.dp)
        )
    }
}


class VideoCaptureViewModelFactory(private val application: Context) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        VideoCaptureViewModel(AsyncCameraProvider(application)) as T
}

class VideoCaptureViewModel(
    asyncCameraProvider: AsyncCameraProvider
) : ViewModel() {
    private val _surfaceRequestFront = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequestFront: StateFlow<SurfaceRequest?> = _surfaceRequestFront
    private val _surfaceRequestBack = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequestBack: StateFlow<SurfaceRequest?> = _surfaceRequestBack

    private val primaryUseCaseGroup = UseCaseGroup.Builder().addUseCase(
        Preview.Builder().build().apply {
            setSurfaceProvider {
                _surfaceRequestFront.value = it
            }
        }
    ).build()
    private val secondaryUseCaseGroup = UseCaseGroup.Builder().addUseCase(
        Preview.Builder().build().apply {
            setSurfaceProvider {
                _surfaceRequestBack.value = it
            }
        }
    ).build()
    private lateinit var cameraProvider: ProcessCameraProvider
    private var runningCameraJob: Job? = null

    private var initializationDeferred: Deferred<Unit> = viewModelScope.async {
        cameraProvider = asyncCameraProvider.getCameraProvider()
    }

    fun startCamera() {
        stopCamera()
        runningCameraJob = viewModelScope.launch {
            initializationDeferred.await()

            val (cameraSelector, possibleHdrEncoding) = try {
                cameraProvider.availableCameraInfos
                    .firstNotNullOf { cameraInfo ->
                        val supportedEncoding = Recorder
                            .getVideoCapabilities(cameraInfo)
                            .supportedDynamicRanges
                            .filterNot { it == DynamicRange.SDR }
                            .firstOrNull() ?: return@firstNotNullOf null

                        cameraInfo.cameraSelector to supportedEncoding
                    }
            } catch (e: NoSuchElementException) {
                DEFAULT_BACK_CAMERA to null
            }
            // Create a Recorder with Quality.HIGHEST, which will select the highest
            // resolution compatible with the chosen DynamicRange.
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .setAspectRatio(AspectRatio.RATIO_16_9)
                .build()
            val videoCapture = VideoCapture.Builder(recorder).apply {
                possibleHdrEncoding?.let(::setDynamicRange)
                setMirrorMode(MirrorMode.MIRROR_MODE_ON)
            }.build()

            val lifecycleOwner = CoroutineLifecycleOwner(coroutineContext)
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                UseCaseGroup.Builder().addUseCase(videoCapture).build()
            )
            awaitCancellation()
        }
    }

    fun stopCamera() {
        runningCameraJob?.apply {
            if (isActive) {
                cancel()
            }
        }
    }
}