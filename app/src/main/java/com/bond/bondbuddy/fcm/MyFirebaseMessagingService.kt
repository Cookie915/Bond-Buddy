package com.bond.bondbuddy.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.app.NotificationCompat
import com.bond.bondbuddy.R
import com.bond.bondbuddy.activities.MainActivity
import com.bond.bondbuddy.models.TokenWithTimeStampID
import com.bond.bondbuddy.workmanager.LocationWorkerServices
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage



@ExperimentalPermissionsApi
@ExperimentalComposeUiApi
@ExperimentalStdlibApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val id = FirebaseAuth.getInstance().currentUser?.uid
        id?.let {
            Log.i(TAG,"New Token Registered for ${FirebaseAuth.getInstance().currentUser?.displayName} $token")
            val tokenWithTimeStamp = TokenWithTimeStampID(id, token)
            updateToken(tokenWithTimeStamp)
        }
    }

    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val id = FirebaseAuth.getInstance().uid!!
            val timestamp = com.google.firebase.Timestamp.now()
            val tokenWithTimestamp = TokenWithTimeStampID(id, token,timestamp.toDate())
            updateToken(tokenWithTimestamp)
        }
        Log.d(TAG,"message received")
        val messageCategory = p0.data["category"].toString()
        Log.d(TAG, messageCategory)
        when(messageCategory){
            MessageCategories.LocationUpdate.type -> {
                val title = p0.data["title"].toString()
                val body = p0.data["body"].toString()
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.putExtra("Notification_Type", "Location_Update")
                val pendingIntent = PendingIntent.getActivity(this,0, intent,PendingIntent.FLAG_ONE_SHOT + PendingIntent.FLAG_IMMUTABLE)
                val channelID = "Location Update"
                val builder = NotificationCompat.Builder(this, channelID)
                    .setSmallIcon(R.drawable.ic_location_notification)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(channelID, "Location Updates", NotificationManager.IMPORTANCE_DEFAULT)
                    notificationManager.createNotificationChannel(channel)
                }
                notificationManager.notify(0,builder.build())
                LocationWorkerServices.enqueueLocationWork(applicationContext)
            }

            MessageCategories.RequestedLocation.type -> {
                Log.d(TAG," RequestedLocation")
                val title = p0.data["title"].toString()
                val body = p0.data["body"].toString()
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val pendingIntent = PendingIntent.getActivity(this,0, intent,PendingIntent.FLAG_ONE_SHOT + PendingIntent.FLAG_IMMUTABLE)
                val channelID = "Location Update"
                val builder = NotificationCompat.Builder(this, channelID)
                    .setSmallIcon(R.drawable.ic_location_notification)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(channelID, "Location Updates", NotificationManager.IMPORTANCE_DEFAULT)
                    notificationManager.createNotificationChannel(channel)
                }
                notificationManager.notify(0,builder.build())
                    LocationWorkerServices.LocationDirectRequestWorker.enqueueDirectLocationWork(applicationContext)
            }

            MessageCategories.BoundsCheck.type -> {
                Log.d(TAG,"Performed Bounds Check")
                val title = p0.data["title"].toString()
                val body = p0.data["body"].toString()
                val username = p0.data["username"]
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.putExtra("Notification_Type", "Bounds_Check")
                intent.putExtra("User_Name", username)
                val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT + PendingIntent.FLAG_IMMUTABLE )
                val channelID = "Bounds Check"
                val builder = NotificationCompat.Builder(this, channelID)
                    .setSmallIcon(R.drawable.ic_location_notification)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(channelID, "User Outside State", NotificationManager.IMPORTANCE_DEFAULT)
                    notificationManager.createNotificationChannel(channel)
                }
                notificationManager.notify(0,builder.build())
            }

            MessageCategories.MarkUserInactive.type -> {
                Log.d(TAG, "MarkUserInactive")
                val title = p0.data["title"]
                val body = p0.data["body"]
                val userName = p0.data["username"]
                val intent = Intent(this, MainActivity::class.java).apply {
                    this.putExtra("Notification_Type", "Mark_Inactive")
                    this.putExtra("User_Name", userName)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT + PendingIntent.FLAG_IMMUTABLE )
                val channelID = "Inactive Users"
                val builder = NotificationCompat.Builder(this, channelID)
                    .setSmallIcon(R.drawable.ic_location_notification)
                    .setContentTitle(title.toString())
                    .setContentText(body.toString())
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(channelID, "User Went Inactive", NotificationManager.IMPORTANCE_DEFAULT)
                    notificationManager.createNotificationChannel(channel)
                }
                notificationManager.notify(0,builder.build())
            }
        }
    }
    companion object {
        private const val TAG = "FCM"

        enum class MessageCategories(val type: String){
            LocationUpdate("locationupdate"),
            BoundsCheck("boundscheck"), RequestedLocation("requestedlocation"),
            MarkUserInactive("markinactive")
        }

        private fun updateCompanyToken(token: String, companyName: String){
            FirebaseFirestore.getInstance().collection("companies").document(companyName).set(mapOf("token" to token), SetOptions.merge())
        }

        fun updateToken(token: TokenWithTimeStampID) {
            val fireStore = FirebaseFirestore.getInstance()
            val fcmCollection = fireStore.collection("fcmtokens")
            fireStore.collection("users").document(token.id).get().addOnSuccessListener {
                if (it["owner"] == true) {
                    updateCompanyToken(token.token, it["companyname"].toString())
                }
                it.reference.set(mapOf("token" to token.token), SetOptions.merge())
            }
            fcmCollection.whereEqualTo("token", token.token).get().addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { documentSnapshot ->
                    documentSnapshot.reference.set(token, SetOptions.merge())
                }
            }
        }
    }
}


