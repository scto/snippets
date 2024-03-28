package com.example.camera.concurrent

import android.content.Context
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun ConcurrentCameraScreen(
    modifier: Modifier = Modifier,
    viewModel: ConcurrentCameraViewModel = viewModel(
        factory = ConcurrentCameraViewModelFactory(LocalContext.current.applicationContext)
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
                .fillMaxWidth(0.5f)
                .aspectRatio(1f)
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clip(CircleShape)
        )
    }
}


class ConcurrentCameraViewModelFactory(private val application: Context) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ConcurrentCameraViewModel(AsyncCameraProvider(application)) as T
}

class ConcurrentCameraViewModel(
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
            val lifecyleOwner = CoroutineLifecycleOwner(coroutineContext)

            val (primarySelector, secondarySelector) =
                cameraProvider.availableConcurrentCameraInfos
                    .firstNotNullOf { possibleConfig ->
                        val frontFacing = possibleConfig
                            .firstOrNull { it.lensFacing == LENS_FACING_FRONT }
                            ?: return@firstNotNullOf null
                        val backFacing = possibleConfig
                            .firstOrNull { it.lensFacing == LENS_FACING_BACK }
                            ?: return@firstNotNullOf null
                        frontFacing.cameraSelector to backFacing.cameraSelector
                    }
            val primary = SingleCameraConfig(
                primarySelector,
                primaryUseCaseGroup,
                lifecyleOwner
            )
            val secondary = SingleCameraConfig(
                secondarySelector,
                secondaryUseCaseGroup,
                lifecyleOwner
            )
            cameraProvider.bindToLifecycle(listOf(primary, secondary))
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
