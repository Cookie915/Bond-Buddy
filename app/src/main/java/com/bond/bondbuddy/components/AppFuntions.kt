package com.bond.bondbuddy.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.view.Surface
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.StorageReference
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

operator fun <User> MutableLiveData<MutableSet<User>>.plusAssign(user: User) {
    val value = this.value ?: mutableSetOf()
    value.add(user)
    this.value = value
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

val Context.executor: Executor
    get() = ContextCompat.getMainExecutor(this)

//  get Context's Camera Provider
suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener({
                               continuation.resume(future.get())
                           }, executor)
    }
}

fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    var rotation = image.imageInfo.rotationDegrees
    when(rotation){
        Surface.ROTATION_90 -> {
            rotation += 270
        }
        Surface.ROTATION_180 -> {
            rotation += 180
        }
        Surface.ROTATION_270 -> {
            rotation += 90
        }
    }
    val matrix = Matrix()
    matrix.postRotate(rotation.toFloat())
    val buffer: ByteBuffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = orientation
        onDispose {
            // restore original orientation when view disappears
            activity.requestedOrientation = originalOrientation
        }
    }
}

@Composable
fun animateHorizontalAlignmentAsState(
    targetBiasValue: Float
): State<BiasAlignment.Horizontal> {
    val bias by animateFloatAsState(targetBiasValue, animationSpec = tween(
        durationMillis = 50,
        easing = LinearEasing
    )
    )
    return derivedStateOf { BiasAlignment.Horizontal(bias) }
}

fun GeoPoint.toLatLng(): LatLng {
    return LatLng(latitude, longitude)
}

//  polls storage certain number of times with 1s delay to wait for value
fun pollStorage(storageRef: StorageReference, times: Int): String? {
    var repeatCount = times
    while (repeatCount > 0) {
        Thread.sleep(1000)
        try {
            val urlTask = storageRef.downloadUrl
            Tasks.await(urlTask)
            if (urlTask.exception == null && urlTask.result != null) {
                return urlTask.result.toString()
            }
        } catch (ex: Exception) {
            repeatCount -= 1
            Log.e("PollStorage", "exception: ${ex.localizedMessage}")
        }
    }
    return null
}



