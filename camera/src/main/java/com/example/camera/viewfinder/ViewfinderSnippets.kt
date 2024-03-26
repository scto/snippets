package com.example.camera.viewfinder

import android.app.Application
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.viewfinder.compose.Viewfinder
import androidx.camera.viewfinder.surface.ImplementationMode
import androidx.camera.viewfinder.surface.TransformationInfo
import androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.await
import com.example.camera.util.SmoothImmersiveRotationEffect
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

@Composable
fun LowLevelPreview(
    useViewfinderLib: Boolean,
    modifier: Modifier = Modifier
) {
    // As long as this screen is showing, go to immersive mode and don't animate on rotation
    SmoothImmersiveRotationEffect(LocalContext.current)
    if(useViewfinderLib) {
        WithViewfinderSnippet(modifier)
    } else {
        WithoutViewfinder(modifier)
    }
}


// DO NOT COPY - Streaming the preview directly to a Surface will result in wrong aspect ratios.
@Composable
fun WithoutViewfinder(
    modifier: Modifier = Modifier,
    viewModel: ViewfinderViewModel = viewModel()
) {
    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()
    surfaceRequest?.let { request ->
        AndroidExternalSurface(modifier.fillMaxSize()) {
            onSurface { surface, _, _ ->
                suspendCancellableCoroutine {
                    request.provideSurface(
                        surface,
                        Runnable::run
                    ) { result: SurfaceRequest.Result? ->
                        it.resume(requireNotNull(result) {
                            "Expected non-null result from SurfaceRequest, but received null."
                        }.surface)
                    }
                }
                Unit
            }
        }
    }
}

@Composable
fun WithViewfinderSnippet(
    modifier: Modifier = Modifier,
    viewModel: ViewfinderViewModel = viewModel()
) {
    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()
    val currentSurfaceRequest = surfaceRequest
    if (currentSurfaceRequest != null) {
        val viewfinderArgs by produceState<ViewfinderArgs?>(
            initialValue = null,
            surfaceRequest
        ) {
            val viewfinderSurfaceRequest =
                ViewfinderSurfaceRequest
                    .Builder(currentSurfaceRequest.resolution)
                    .build()

            currentSurfaceRequest.addRequestCancellationListener(Runnable::run) {
                viewfinderSurfaceRequest.markSurfaceSafeToRelease()
            }

            // Launch undispatched so we always reach the try/finally in this coroutine
            launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    val surface = viewfinderSurfaceRequest.getSurface()
                    currentSurfaceRequest.provideSurface(surface, Runnable::run) {
                        viewfinderSurfaceRequest.markSurfaceSafeToRelease()
                    }
                } finally {
                    // If we haven't provided the surface, such as if we're cancelled
                    // while suspending on getSurface(), this call will succeed. Otherwise
                    // it will be a no-op.
                    currentSurfaceRequest.willNotProvideSurface()
                }
            }

            val transformationInfos = MutableStateFlow<SurfaceRequest.TransformationInfo?>(null)
            currentSurfaceRequest.setTransformationInfoListener(Runnable::run) {
                transformationInfos.value = it
            }

            // The ImplementationMode that will be used for all TransformationInfo updates.
            // This is locked in once we have updated ViewfinderArgs and won't change until
            // this produceState block is cancelled and restarted
            var snapshotImplementationMode: ImplementationMode? = null

            snapshotFlow { ImplementationMode.PERFORMANCE }
                .combine(transformationInfos.filterNotNull()) { implMode, transformInfo ->
                    Pair(implMode, transformInfo)
                }.takeWhile { (implMode, _) ->
                    val shouldAbort =
                        snapshotImplementationMode != null && implMode != snapshotImplementationMode
                    if (shouldAbort) {
                        // Abort flow and invalidate SurfaceRequest so a new one will be sent
                        currentSurfaceRequest.invalidate()
                    }
                    !shouldAbort
                }.collectLatest { (implMode, transformInfo) ->
                    // We'll only ever get here with a single non-null implMode,
                    // so setting it every time is ok
                    snapshotImplementationMode = implMode
                    value = ViewfinderArgs(
                        viewfinderSurfaceRequest,
                        implMode,
                        TransformationInfo(
                            transformInfo.rotationDegrees,
                            transformInfo.cropRect.left,
                            transformInfo.cropRect.right,
                            transformInfo.cropRect.top,
                            transformInfo.cropRect.bottom,
                            transformInfo.isMirroring
                        )
                    )
                }
        }
        viewfinderArgs?.let { args ->
            Viewfinder(
                surfaceRequest = args.viewfinderSurfaceRequest,
                implementationMode = args.implementationMode,
                transformationInfo = args.transformationInfo,
                modifier = modifier.fillMaxSize()
            )
        }
    }
}

private data class ViewfinderArgs(
    val viewfinderSurfaceRequest: ViewfinderSurfaceRequest,
    val implementationMode: ImplementationMode,
    val transformationInfo: TransformationInfo
)

class ViewfinderViewModel(application: Application) : AndroidViewModel(application) {
    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest
    private val previewUseCase = Preview.Builder().build().apply {
        setSurfaceProvider {
            _surfaceRequest.value = it
        }
    }
    private val useCaseGroup = UseCaseGroup.Builder().addUseCase(previewUseCase).build()
    private lateinit var cameraProvider: ProcessCameraProvider

    init {
        viewModelScope.launch {
            cameraProvider = ProcessCameraProvider.getInstance(application).await()
            cameraProvider.bindToLifecycle(
                CoroutineLifecycleOwner(coroutineContext),
                CameraSelector.DEFAULT_BACK_CAMERA,
                useCaseGroup
            )
            awaitCancellation()
        }
    }
}
private class CoroutineLifecycleOwner(coroutineContext: CoroutineContext) :
    LifecycleOwner {
    private val lifecycleRegistry: LifecycleRegistry =
        LifecycleRegistry(this).apply {
            currentState = Lifecycle.State.INITIALIZED
        }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    init {
        if (coroutineContext[Job]?.isActive == true) {
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            coroutineContext[Job]?.invokeOnCompletion {
                lifecycleRegistry.apply {
                    currentState = Lifecycle.State.STARTED
                    currentState = Lifecycle.State.CREATED
                    currentState = Lifecycle.State.DESTROYED
                }
            }
        } else {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
    }
}