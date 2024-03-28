package com.example.camera.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class CoroutineLifecycleOwner(coroutineContext: CoroutineContext) :
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