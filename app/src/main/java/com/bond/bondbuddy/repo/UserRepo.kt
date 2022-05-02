package com.bond.bondbuddy.repo

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import javax.inject.Inject

class UserRepository @Inject constructor() {
    val firebaseAuth = FirebaseAuth.getInstance()
    val database: FirebaseFirestore = FirebaseFirestore.getInstance()
    val dbUsersRef = database.collection("users")
    private val dbCompanyRef = database.collection("companies")
    private val tag = "AppRepo"

    //  Set
    fun setOwner(boolean: Boolean): Task<Void> {
        return dbUsersRef.document(firebaseAuth.currentUser!!.uid).update("owner", boolean)
    }

    fun setRegistered() {
        dbUsersRef.document(firebaseAuth.currentUser!!.uid).set(mapOf("registered" to true), SetOptions.merge())
    }

    fun setContactEmail(email: String) {
        dbUsersRef.document(firebaseAuth.currentUser!!.uid).set(mapOf("contactemail" to email), SetOptions.merge())
    }

    fun setContactNumber(number: String) {
        dbUsersRef.document(firebaseAuth.currentUser!!.uid).set(mapOf("contactnumber" to number), SetOptions.merge())
    }

    fun setBoundState(id: String, state: String): Task<Void> {
        return dbUsersRef.document(id).set(mapOf("boundingState" to state), SetOptions.merge())
    }

//    suspend fun setUserDisplayName(name: String) = withContext(Dispatchers.IO) {
//        val firebaseUser = firebaseAuth.currentUser
//        val firebaseUID = firebaseUser?.uid.toString()
//        dbUsersRef.document(firebaseUID).set(mapOf("displayname" to name), SetOptions.merge())
//        firebaseUser?.updateProfile(userProfileChangeRequest {
//            this.displayName = name
//        })
//    }
        //  returns a task to remove old profile picture from fire storage and replace it with newUrl
    @ExperimentalStdlibApi
    fun replaceProfilePicture(oldUri: String, newUri: String, userID: String): Task<Void> {
        //  delete old profile pic from storage
        if (oldUri.isNotBlank()) {
            FirebaseStorage.getInstance().getReferenceFromUrl(oldUri).delete()
                .addOnCompleteListener {
                    when(it.isSuccessful){
                        false -> {
                            Log.i(tag, "Failed To Delete Old Pfp", it.exception)
                        }
                        true -> {
                            Log.i(tag, "Deleted Old Pfp")
                        }
                    }
                }
        }
        val updatePfpTask: Task<Void> = FirebaseFirestore.getInstance().collection("users")
            .document(userID).set(
                mapOf(
                    "profilepicurl" to newUri
                ),
                SetOptions.merge()
            ).addOnCompleteListener {
                when(it.isSuccessful){
                    false -> {
                        Log.i(tag, "Failed To Delete Update Pfp Url", it.exception)
                    }
                    true -> {
                        Log.i(tag, "Updated Pfp Url")
                    }
                }
            }
        return updatePfpTask
    }

    //  Get
    fun getUsersCompanyOwnerReference(ctx: Context, companyName: String, cb: (DocumentReference) -> Unit) {
        dbCompanyRef.document(companyName).get().addOnCompleteListener { companyTask ->
            when (companyTask.isSuccessful) {
                false -> {
                    Toast.makeText(ctx, "Couldn't reach owner", Toast.LENGTH_SHORT).show()
                }
                true -> {
                    val companyOwnerID = companyTask.result["owner"]
                    if (companyOwnerID == null){
                        Toast.makeText(ctx, "Couldn't reach owner", Toast.LENGTH_SHORT).show()
                    } else {
                        cb(dbUsersRef.document(companyOwnerID.toString()))
                    }
                }
            }
        }
    }

    //  Functions
        //  polls storage certain number of times with 1s delay to wait for value
    fun pollStorage(storageRef: StorageReference, times: Int): String? {
        var repeatCount = times
        while (repeatCount > 0) {
            Thread.sleep(1000)
            try {
                val urlTask = storageRef.downloadUrl
                Tasks.await(urlTask)
                if (urlTask.exception == null && urlTask.result != null) {
                    Log.i("AppRepo", "poll storage url ${urlTask.result}")
                    return urlTask.result.toString()
                }
            } catch (ex: Exception) {
                repeatCount -= 1
                Log.i("AppRepo", "exception: ${ex.localizedMessage}")
            }
        }
        Log.e(tag, "Poll Storage Returned Null")
        return null
    }
}