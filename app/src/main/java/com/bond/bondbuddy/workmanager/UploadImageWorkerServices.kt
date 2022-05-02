package com.bond.bondbuddy.workmanager

import android.content.Context
import android.widget.Toast
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.bond.bondbuddy.repo.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext

@ExperimentalStdlibApi
class UploadImageWorkerServices {
    class ImageUploadWorker(@ApplicationContext ctx: Context, workerParams: WorkerParameters): CoroutineWorker(ctx, workerParams){
        override suspend fun doWork(): Result {
            return try {
                val originalUri = this.inputData.getString("originalUri")
                val newUri = this.inputData.getString("newUri")
                val userId = this.inputData.getString("userId")
                val appRepo = UserRepository()
                if (originalUri != null && userId != null && newUri != null) {
                    appRepo.replaceProfilePicture(originalUri, newUri, userId).addOnCompleteListener {
                        when(it.isSuccessful){
                            false -> {
                                Toast.makeText(this.applicationContext, "Upload Failed", Toast.LENGTH_SHORT).show()
                            }
                            true -> {
                                Toast.makeText(this.applicationContext, "Upload Success", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                return  Result.success()
            }catch (e: Error) {
                Result.failure()
            }
        }
    }

    companion object {
        val constraints: Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}