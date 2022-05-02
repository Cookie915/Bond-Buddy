package com.bond.bondbuddy.workmanager

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.location.Geocoder
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.bond.bondbuddy.components.GeoStates
import com.bond.bondbuddy.models.User
import com.bond.bondbuddy.models.UserLocation
import com.bond.bondbuddy.repo.UserRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.intentfilter.androidpermissions.PermissionManager
import com.intentfilter.androidpermissions.models.DeniedPermissions
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*

const val TAG = "LocationWorker"

class LocationWorkerServices {

    //  Used to record a realtime location update from owner of company for a specific user
    class LocationDirectRequestWorker(@ApplicationContext appContext: Context, workerParameters: WorkerParameters) :
        CoroutineWorker(appContext, workerParameters) {
        private val notificationManager = appContext.getSystemService(NotificationManager::class.java)
        val ctx = appContext
        override suspend fun doWork(): Result {
            Log.i(TAG, "Got To Direct Location Worker")
            return try {
                recordLocation(ctx, direct = true)
                Result.success()
            } catch (e: Throwable) {
                Log.e(TAG, "Couldn't record Location", e)
                return Result.failure()
            }
        }

        override suspend fun getForegroundInfo(): ForegroundInfo {
            createNotificationChannel()
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID).setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle("Location Update Requested").build()
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(
                    NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
                )
            } else {
                ForegroundInfo(NOTIFICATION_ID, notification)
            }
        }

        private fun createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                var notificationChannel = notificationManager?.getNotificationChannel(CHANNEL_ID)
                if (notificationChannel == null) {
                    notificationChannel = NotificationChannel(
                        CHANNEL_ID, "Location Worker", NotificationManager.IMPORTANCE_LOW
                    )
                    notificationManager?.createNotificationChannel(notificationChannel)
                }
            }
        }

        companion object {
            private const val NOTIFICATION_ID = 4556
            private const val CHANNEL_ID = "Direct Location Requests"

            private val constraints: Constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

            private val DirectLocationWorkRequest: OneTimeWorkRequest =
                OneTimeWorkRequestBuilder<LocationDirectRequestWorker>().setConstraints(constraints)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()

            fun enqueueDirectLocationWork(@ApplicationContext ctx: Context) {
                WorkManager.getInstance(ctx).also {
                    it.enqueueUniqueWork("LocationDirectWorkRequest", ExistingWorkPolicy.REPLACE, DirectLocationWorkRequest)
                }
            }
        }

    }

    class LocationWorker(@ApplicationContext appContext: Context, workerParams: WorkerParameters) :
        CoroutineWorker(appContext, workerParams) {
        val ctx = appContext
        private val notificationManager = appContext.getSystemService(NotificationManager::class.java)

        override suspend fun doWork(): Result {
            Log.i(TAG, "Got To LocationWorker")
            return try {
                recordLocation(ctx, direct = false)
                Result.success()
            } catch (e: Throwable) {
                Log.e("LocationWorker", "Couldn't record Location", e)
                return Result.failure()
            }
        }

        override suspend fun getForegroundInfo(): ForegroundInfo {
            createNotificationChannel()
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID).setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle("Updating Location").build()
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(
                    NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
                )
            } else {
                ForegroundInfo(NOTIFICATION_ID, notification)
            }
        }

        private fun createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                var notificationChannel = notificationManager?.getNotificationChannel(CHANNEL_ID)
                if (notificationChannel == null) {
                    notificationChannel = NotificationChannel(
                        CHANNEL_ID, "Location Worker", NotificationManager.IMPORTANCE_LOW
                    )
                    notificationManager?.createNotificationChannel(notificationChannel)
                }
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 3562
        private const val CHANNEL_ID = "Location Updates"

        private val constraints: Constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        private val locationWorkRequest: OneTimeWorkRequest = OneTimeWorkRequestBuilder<LocationWorker>().setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()

        fun enqueueLocationWork(@ApplicationContext ctx: Context) {
            WorkManager.getInstance(ctx).also {
                it.enqueueUniqueWork("LocationWorkRequest", ExistingWorkPolicy.REPLACE, locationWorkRequest)
            }
        }

        //check if user is in bounded state, notify company owner if false
        fun isInBoundingState(boundingState: String, latLng: LatLng, ctx: Context): Boolean {
            //  check for Abbreviated states
            var state: String = boundingState
            if (state.length <= 2) {
                state = GeoStates[state].toString()
            }
            return if (state != "null") {
                val geoCoder = Geocoder(ctx)
                val addresses = geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                val currentState = addresses[0].adminArea
                currentState == state
            } else {
                true
            }
        }
    }
}

//Location Functions
private val dbUsersRef = UserRepository().dbUsersRef
private val firebaseAuth = UserRepository().firebaseAuth

@SuppressLint("MissingPermission")
fun recordLocation(ctx: Context, direct: Boolean) {
    val dynamicPermissionManager = PermissionManager.getInstance(ctx)
    if (Build.VERSION.SDK_INT >= 29) {
        val backgroundLocationPermission = listOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        val coarseFineLocationPermission = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        dynamicPermissionManager.checkPermissions(coarseFineLocationPermission, object : PermissionManager.PermissionRequestListener {
            override fun onPermissionGranted() {
                dynamicPermissionManager.checkPermissions(backgroundLocationPermission,
                                                          object : PermissionManager.PermissionRequestListener {
                                                              override fun onPermissionGranted() {
                                                                  grabLocation(ctx, direct)
                                                              }

                                                              override fun onPermissionDenied(p0: DeniedPermissions?) {
                                                                  ctx.mainExecutor.execute {
                                                                      Toast.makeText(
                                                                          ctx,
                                                                          "Please Allow Full Location Access To BondBuddy in Your Settings",
                                                                          Toast.LENGTH_LONG
                                                                      ).show()
                                                                  }
                                                              }
                                                          })
            }

            override fun onPermissionDenied(p0: DeniedPermissions?) {
                ctx.mainExecutor.execute {
                    Toast.makeText(
                        ctx, "Please Allow Full Location Access To BondBuddy in Your Settings", Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    } else {
        val locationPermissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        dynamicPermissionManager.checkPermissions(locationPermissions, object : PermissionManager.PermissionRequestListener {
            override fun onPermissionGranted() {
                grabLocation(ctx, direct)
            }
            override fun onPermissionDenied(p0: DeniedPermissions?) {
                val ctxCompat = ContextCompat.getMainExecutor(ctx)
                ctxCompat.execute {
                    Toast.makeText(
                        ctx, "Please Allow Full Location Access To BondBuddy in Your Settings", Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    }
}

@SuppressLint("MissingPermission")
fun grabLocation(ctx: Context, direct: Boolean) {
    val locationService = LocationServices.getFusedLocationProviderClient(ctx)
    val cancellationToken = CancellationTokenSource().token
    val uid = firebaseAuth.uid
    Log.i(TAG, "Got to recordLocation")
    locationService.getCurrentLocation(
        com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY, cancellationToken
    ).addOnSuccessListener { it ->
        if (it != null && FirebaseAuth.getInstance().currentUser != null) {
            Log.i(TAG, "lat ${it.latitude} long ${it.longitude}")
            val location = UserLocation(GeoPoint(it.latitude, it.longitude))
            dbUsersRef.document(uid!!).get().addOnSuccessListener { user ->
                if (!LocationWorkerServices.isInBoundingState(
                        user["boundingState"].toString(), LatLng(it.latitude, it.longitude), ctx
                    )
                ) {
                    val companyName = user["companyname"].toString()
                    if (companyName != "null") {
                        notifyUserLeftBoundedState(companyName)
                    }
                }
                val currentTime = Date().time
                val lastLocationTime = user.toObject(User::class.java)?.lastlocation?.timestamp?.time ?: currentTime + 3600100
                if (currentTime <= lastLocationTime + 3500000 && !direct){
                    dbUsersRef.document(uid).set(mapOf("active" to true), SetOptions.merge())
                } else {
                    dbUsersRef.document(uid).set(mapOf("active" to true), SetOptions.merge())
                    dbUsersRef.document(uid).set(mapOf("lastlocation" to location), SetOptions.merge())
                    dbUsersRef.document(uid).collection("locations").document().set(location, SetOptions.merge())
                }
            }
        } else {
            Log.i(TAG, "Record Location Returned Null, Check Permissions")
            dbUsersRef.document(uid!!).get().addOnSuccessListener {
                val companyName = it["companyname"].toString()
                if (companyName != "null") {
                    notifyFailedLocationUpdate(companyName)
                }
            }
        }
    }.addOnFailureListener { error ->
        Log.i(TAG, "Record Location failed" + error.localizedMessage)
        val id = firebaseAuth.uid
        dbUsersRef.document(id!!).get().addOnSuccessListener {
            val companyName = it["companyname"].toString()
            if (companyName != "null") {
                notifyFailedLocationUpdate(companyName)
            }
        }
    }
}

// Fetch Location On Request For Specified User

//send Https to callable firebase functions with token to notify of failure
private fun notifyFailedLocationUpdate(companyName: String?): Task<String> {
    val functions = Firebase.functions
    val name = FirebaseAuth.getInstance().currentUser?.displayName
    val data = mapOf("companyname" to companyName, "name" to name)
    return functions.getHttpsCallable("notifyFailedLocationUpdate").call(data).continueWith { task ->
            val result = task.result?.data as String
            result
        }
}

//send Https to callable firebase function with token and name to notify use leaving bounded state
fun notifyUserLeftBoundedState(companyName: String?): Task<String> {
    val functions = Firebase.functions
    val name = FirebaseAuth.getInstance().currentUser?.displayName
    val data = mapOf("companyname" to companyName, "name" to name)
    return functions.getHttpsCallable("notifyUserLeftState").call(data).continueWith { task ->
        val result = task.result?.data as String
        result
    }
}

//  send Https to callable firebase function with token and name to Notify of Uninstalls
//fun notifyUserUninstalled(companyName: String): Task<String> {
//    val functions = Firebase.functions
//    val name = FirebaseAuth.getInstance().currentUser?.displayName
//    val data = mapOf(
//        "topic" to companyName,
//        "name" to name,
//    )
//    return functions.getHttpsCallable("notifyUserUninstalled").call(data).continueWith { task ->
//            val result = task.result?.data as String
//            result
//        }
//}


// </Location Services>
