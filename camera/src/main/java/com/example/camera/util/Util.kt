package com.example.camera.util

import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun SmoothImmersiveRotationEffect(context: Context) =
    DisposableEffect(context) {
        context.getActivity()?.window?.let { window ->
            window.attributes = window.attributes.apply {
                rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_SEAMLESS
            }
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            context.getActivity()?.window?.let { window ->
                window.attributes = window.attributes.apply {
                    rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_ROTATE
                }
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

private fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}