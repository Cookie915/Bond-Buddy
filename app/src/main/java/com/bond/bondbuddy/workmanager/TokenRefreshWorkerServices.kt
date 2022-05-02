package com.bond.bondbuddy.workmanager

import android.content.Context
import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.work.*
import com.bond.bondbuddy.fcm.MyFirebaseMessagingService
import com.bond.bondbuddy.models.TokenWithTimeStampID
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.ParseException
import java.util.*
import java.util.concurrent.TimeUnit
@ExperimentalComposeUiApi
@ExperimentalPermissionsApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalStdlibApi
class TokenRefreshWorkerServices {
    class TokenRefreshWorker(@ApplicationContext ctx: Context, workerParams: WorkerParameters) : CoroutineWorker(ctx, workerParams) {

        override suspend fun doWork(): Result {
            val calendar = Calendar.getInstance()
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            try {
                if (day == 1) {
                    FirebaseFirestore.getInstance()
                    FirebaseMessaging.getInstance().token.addOnCompleteListener {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid!!
                        val token = TokenWithTimeStampID(uid, it.result)
                        MyFirebaseMessagingService.updateToken(token)
                    }
                }
            } catch (exc: ParseException) {
                Log.e(tag, "TokenRefreshWorkerParserException ", exc)
            }
            return Result.success()
        }

        companion object {
            const val tag = "TokenRefreshWorker"
            private val constraints: Constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            private val tokenRefreshRequest: PeriodicWorkRequest =
                 PeriodicWorkRequestBuilder<TokenRefreshWorker>(43800, TimeUnit.MINUTES)
                .addTag("TokenRefreshWorker").setConstraints(constraints)
                .build()

            fun enqueueTokenRefreshWork(@ApplicationContext ctx:Context){
                WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                    "TokenRefreshWorker",
                    ExistingPeriodicWorkPolicy.KEEP,
                    tokenRefreshRequest
                )
            }
        }

    }


}