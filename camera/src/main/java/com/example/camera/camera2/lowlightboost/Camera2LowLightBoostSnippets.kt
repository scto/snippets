//@file:Suppress("UNUSED_VARIABLE", "ControlFlowWithEmptyBody")
//
//package com.example.camera.camera2.lowlightboost
//
//import android.hardware.camera2.CameraCaptureSession
//import android.hardware.camera2.CameraCaptureSession.CaptureCallback
//import android.hardware.camera2.CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES
//import android.hardware.camera2.CameraDevice
//import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
//import android.hardware.camera2.CameraManager
//import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
//import android.hardware.camera2.CameraMetadata.CONTROL_LOW_LIGHT_BOOST_STATE_ACTIVE
//import android.hardware.camera2.CaptureRequest
//import android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE
//import android.hardware.camera2.CaptureResult
//import android.hardware.camera2.CaptureResult.CONTROL_LOW_LIGHT_BOOST_STATE
//import android.hardware.camera2.TotalCaptureResult
//import android.os.Build
//import android.os.Handler
//
//
//// [START android_camera_camera2_lowlightboost_check_support]
//private fun CameraManager.isLowLightBoostAvailable(
//  cameraId: String
//): Boolean {
//  val aeModes = getCameraCharacteristics(cameraId)
//    .get(CONTROL_AE_AVAILABLE_MODES)?.toList().orEmpty()
//
//  return Build.VERSION.SDK_INT >= 35 &&
//      aeModes.contains(
//        CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
//      )
//}
//// [END android_camera_camera2_lowlightboost_check_support]
//
//private fun enableLowLightBoostSnippet(
//  camera: CameraDevice,
//  cameraManager: CameraManager,
//  session: CameraCaptureSession,
//  cameraHandler: Handler,
//  cameraId: String
//) {
//  // [START android_camera_camera2_lowlightboost_enable]
//  val request = camera.createCaptureRequest(TEMPLATE_PREVIEW)
//    .apply {
//      if (cameraManager.isLowLightBoostAvailable(cameraId)) {
//        set(
//          CONTROL_AE_MODE,
//          CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
//        )
//      }
//    }.build()
//  // other capture request params
//
//  session.setRepeatingRequest(
//    request,
//    object : CaptureCallback() {
//      @Override
//      override fun onCaptureCompleted(
//        session: CameraCaptureSession,
//        request: CaptureRequest, result: TotalCaptureResult
//      ) {
//        // verify Low Light Boost AE mode set successfully
//        if (Build.VERSION.SDK_INT >= 35) {
//          val successfullyEnabled = result.get(CaptureResult.CONTROL_AE_MODE) ==
//              CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
//        }
//      }
//    },
//    cameraHandler
//  )
//  // [END android_camera_camera2_lowlightboost_enable]
//}
//
//val callback = object : CaptureCallback() {
//  @Override
//  override fun onCaptureCompleted(
//    session: CameraCaptureSession,
//    request: CaptureRequest, result: TotalCaptureResult
//  ) {
//    if (Build.VERSION.SDK_INT >= 35) {
//      // check if Low Light Boost is active or inactive
//      if (
//        Build.VERSION.SDK_INT >= 35 &&
//        result.get(CONTROL_LOW_LIGHT_BOOST_STATE) ==
//        CONTROL_LOW_LIGHT_BOOST_STATE_ACTIVE
//      ) {
//        // Low Light Boost state is active
//      } else {
//        // Low Light Boost state is inactive or not available
//      }
//    }
//  }
//}
//
//private fun checkLowLightBoostSnippet(
//  camera: CameraDevice,
//  session: CameraCaptureSession,
//  cameraHandler: Handler
//) {
//  val request = camera.createCaptureRequest(TEMPLATE_PREVIEW).build()
//  // [START android_camera_camera2_lowlightboost_status]
//  session.setRepeatingRequest(
//    request, object : CaptureCallback() {
//      @Override
//      override fun onCaptureCompleted(
//        session: CameraCaptureSession,
//        request: CaptureRequest, result: TotalCaptureResult
//      ) {
//        if (
//          Build.VERSION.SDK_INT >= 35 &&
//          result.get(CONTROL_LOW_LIGHT_BOOST_STATE) ==
//          CONTROL_LOW_LIGHT_BOOST_STATE_ACTIVE
//        ) { /* State is active */ }
//        else { /* State is inactive / not available */ }
//      }
//    }, cameraHandler)
//  // [END android_camera_camera2_lowlightboost_status]
//}
//
