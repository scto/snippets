package com.example.camera.util

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await

class AsyncCameraProvider(private val application: Context) {
    suspend fun getCameraProvider(): ProcessCameraProvider =
        ProcessCameraProvider.getInstance(application).await()
}