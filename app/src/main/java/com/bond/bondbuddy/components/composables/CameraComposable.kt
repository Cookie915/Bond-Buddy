package com.bond.bondbuddy.components.composables

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.Surface.ROTATION_0
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bond.bondbuddy.R
import com.bond.bondbuddy.components.*
import com.bond.bondbuddy.models.User
import com.bond.bondbuddy.viewmodels.UserViewModel
import com.bond.bondbuddy.workmanager.UploadImageWorkerServices
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor
import kotlin.coroutines.suspendCoroutine

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalStdlibApi
@ExperimentalPermissionsApi
@Composable
fun CameraCapture(
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
    user: User,
    onDone: (() -> Unit),
    onCancel: () -> Unit
) {
    LaunchedEffect(key1 = Unit){
        Log.i("UserViewModel", "From Camera Capture ${userViewModel.hashCode()}")
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var previewUseCase by remember {
        mutableStateOf<UseCase>(
            Preview.Builder().setTargetRotation(ROTATION_0).build()
        )
    }
    val captureUseCase by remember {
        mutableStateOf(
            ImageCapture.Builder().setCaptureMode(
                CAPTURE_MODE_MAXIMIZE_QUALITY
            ).setTargetRotation(ROTATION_0).build()
        )
    }
    var showCameraPreview by remember {
        mutableStateOf(true)
    }
    var capturedImagePreview: Bitmap? by remember {
        mutableStateOf(null)
    }
    lateinit var cameraProvider: ProcessCameraProvider
    val timestamp = System.currentTimeMillis()
    val uploadUserName = user.displayname?.filterNot {
        it.isWhitespace()
    }
    val uploadName: String = (uploadUserName + "$timestamp")
    val fireStorageRef = FirebaseStorage.getInstance().getReference("ProfilePics")
    val imageUploadLocationRef = fireStorageRef.child(uploadName)
    val resizedImageUploadLocationRef = fireStorageRef.child("${uploadName}_400x400")
    val baos = ByteArrayOutputStream()
    var doNotShowRational by rememberSaveable { mutableStateOf(false) }
    val cameraPermissionState = rememberPermissionState(permission = android.Manifest.permission.CAMERA)
        PermissionRequired(permissionState = cameraPermissionState, permissionNotGrantedContent = {
        if (doNotShowRational) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(140.dp),
                    backgroundColor = colorResource(id = R.color.SecondaryBlueLight),
                    elevation = 24.dp
                ) {
                    Text(text = "Camera permission needed to upload profile picture, "
                            + "please grant permission in phone settings to use this feature.", modifier = Modifier.padding(12.dp), color = colorResource(
                        id = R.color.white
                    ))
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(140.dp),
                    backgroundColor = colorResource(id = R.color.SecondaryBlueLight),
                    elevation = 24.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(text = "Camera permission needed to upload profile picture, "
                                + "please grant permission use this feature.",
                             color = Color.White)
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Button(
                                onClick = { doNotShowRational = true }, colors = ButtonDefaults.buttonColors(
                                    backgroundColor = colorResource(id = R.color.white)
                                )
                            ) {
                                Text(text = "Deny", color = colorResource(id = R.color.SecondaryBlue))
                            }
                            Spacer(modifier = Modifier.width(24.dp))
                            Button(
                                onClick = { cameraPermissionState.launchPermissionRequest() }, colors = ButtonDefaults.buttonColors(
                                    backgroundColor = colorResource(id = R.color.white)
                                )
                            ) {
                                Text(text = "Ok!", color = colorResource(id = R.color.SecondaryBlue))
                            }
                        }
                    }
                }
            }
        }
    }, permissionNotAvailableContent = {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ){
            Card(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(140.dp),
            backgroundColor = colorResource(id = R.color.SecondaryBlueLight),
            elevation = 24.dp
        ) {
            Text(
                text = "Camera permission has been revoked, "
                        + "please grant permission in phone settings to use this feature.", color = Color.White, modifier = Modifier.padding(12.dp)
            )
        }
        }
    }) {
            LaunchedEffect(showCameraPreview) {
                cameraProvider = context.getCameraProvider()
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, previewUseCase, captureUseCase
                    )
                    if (showCameraPreview){
                        capturedImagePreview = null
                    }
                } catch (ex: Exception) {
                    Log.e("CameraCapture", "Failed to bind Use Cases", ex)
                }
            }
            LockScreenOrientation(orientation = Orientation.Horizontal.ordinal)
            Box(modifier = modifier) {
                if (!showCameraPreview && capturedImagePreview != null) {
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        bitmap = capturedImagePreview!!.asImageBitmap(),
                        contentDescription = "Captured Image Preview",
                        contentScale = ContentScale.Crop
                    )
                } else {
                    CameraPreview(modifier = Modifier.fillMaxSize(), onUseCase = {
                        previewUseCase = it
                    })
                }
                //  button to close camera Preview
                IconButton(
                    onClick = {
                        capturedImagePreview = null
                        showCameraPreview = true
                        onCancel()
                    }, modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                        .offset(x = (-12).dp, y = 12.dp)
                ) {
                    Icon(
                        modifier = Modifier,
                        painter = painterResource(id = R.drawable.ic_cancel),
                        contentDescription = "Cancel",
                        tint = colorResource(id = R.color.SecondaryBlueLight)
                    )
                }
                when (showCameraPreview) {
                    //  Camera Preview Layout
                    true  -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconButton(modifier = Modifier
                                .size(56.dp)
                                .offset(y = ((-16).dp)), onClick = {
                                coroutineScope.launch {
                                    captureUseCase.captureUserPicture(
                                        context.executor
                                    ) { bitmap ->
                                        capturedImagePreview = bitmap
                                        showCameraPreview = false
                                    }
                                }
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_camera_button),
                                    contentDescription = "Take Photo",
                                    tint = colorResource(id = R.color.SecondaryBlueLight)
                                )
                            }
                        }
                    }
                    false -> {
                        //  Captured Image Preview Layout
                        ConfirmPictureButtons(modifier = Modifier
                            .width(IntrinsicSize.Min)
                            .align(Alignment.BottomCenter)
                            .offset(y = (-10).dp),
                                              cbConfirm = {
                                                  capturedImagePreview!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                                                  val data = baos.toByteArray()
                                                  CoroutineScope(Dispatchers.Main).launch {
                                                      imageUploadLocationRef.putBytes(data).addOnSuccessListener {
                                                          coroutineScope.launch(Dispatchers.IO) {
                                                              val userCopy = user.copy()
                                                              val uri: String? = pollStorage(resizedImageUploadLocationRef, 10)
                                                              when (uri.isNullOrBlank()) {
                                                                  true  -> {
                                                                      Log.e("CameraCapture", "Failed To Retrieve Uri")
                                                                      Toast.makeText(
                                                                          context, "Failed To Update Profile", Toast.LENGTH_SHORT
                                                                      ).show()
                                                                  }
                                                                  false -> {
                                                                      imageUpload(userCopy.profilepicurl, uri, userCopy.id.toString(), context)
//                                                                      appRepo.replaceProfilePicture(userCopy, uri).addOnSuccessListener {
//                                                                          Log.i("CameraCapture", "Successfully Updated Profile Picture")
//                                                                          Toast.makeText(context, "Upload Success", Toast.LENGTH_SHORT).show()
//                                                                      }.addOnFailureListener {
//                                                                          Log.e("CameraCapture", "Failed to ReplaceProfilePicture", it)
//                                                                      }
                                                                  }
                                                              }
                                                          }
                                                      }.addOnFailureListener {
                                                          Log.e("CameraCapture", "Error Uploading Picture Database", it)
                                                          Toast.makeText(
                                                              context, "Error Uploading Picture To Database", Toast.LENGTH_SHORT
                                                          ).show()
                                                          userViewModel.postLoading(false)
                                                      }
                                                  }
                                                  Toast.makeText(context, "Uploading...", Toast.LENGTH_SHORT).show()
                                                  capturedImagePreview = null
                                                  showCameraPreview = false
                                                  onDone()
                                              },
                                              cbRetry = {
                                                  capturedImagePreview = null
                                                  showCameraPreview = true
                                              })
                    }
                }
            }
    }
}

@ExperimentalStdlibApi
private fun imageUpload(originalUri: String, newUri:String, userID: String, ctx: Context ){
    val workManager = WorkManager.getInstance(ctx)
    val data = Data.Builder().apply {
        putString("originalUri", originalUri)
        putString("newUri",newUri)
        putString("userId", userID)
    }
    val imageUploadRequest: OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<UploadImageWorkerServices.ImageUploadWorker>()
            .addTag("ImageUploadWorker")
            .setInputData(data.build())
            .setConstraints(UploadImageWorkerServices.constraints)
            .build()
    workManager.enqueue(imageUploadRequest)
}

@Composable
fun ConfirmPictureButtons(modifier: Modifier, cbRetry: () -> Unit, cbConfirm: () -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(7.5.dp),
        backgroundColor = colorResource(id = R.color.SecondaryBlueLight).copy(alpha = 0.8f),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 2.dp, horizontal = 12.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f), onClick = { cbRetry() }, colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(id = R.color.SecondaryBlue)
                )
            ) {
                Text(text = "Retry", color = Color.White)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                modifier = Modifier.weight(1f), onClick = { cbConfirm() }, colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(id = R.color.SecondaryBlue)
                )
            ) {
                Text(text = "Ok!", color = Color.White)
            }
        }
    }
}

suspend fun ImageCapture.captureUserPicture(
    executor: Executor, cb: (bitmap: Bitmap) -> Unit
) {
    //  save image here
    return suspendCoroutine {
        takePicture(executor, object : OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)
                Log.i("CameraCapture", "Image Captured")
                image.use {
                    val bitmap: Bitmap = imageProxyToBitmap(image)
                    //  flip bitmap horizontally
                    val matrix = Matrix()
                    matrix.setScale(-1f, 1f)
                    matrix.postTranslate(bitmap.width.toFloat(), 0f)
                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    cb(rotatedBitmap)
                }
            }
        })
    }
}

@ExperimentalPermissionsApi
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    onUseCase: (UseCase) -> Unit = {}
) {
    AndroidView(modifier = modifier, factory = { ctx ->
        val previewView = PreviewView(ctx).apply {
            this.scaleType = scaleType
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        onUseCase(Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        })
        previewView
    })
}















