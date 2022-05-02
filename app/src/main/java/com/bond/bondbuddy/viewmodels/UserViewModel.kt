package com.bond.bondbuddy.viewmodels

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bond.bondbuddy.activities.LoginActivity
import com.bond.bondbuddy.activities.SplashActivity
import com.bond.bondbuddy.components.findActivity
import com.bond.bondbuddy.models.User
import com.bond.bondbuddy.repo.UserRepository
import com.firebase.ui.auth.AuthUI
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import javax.inject.Inject
@ExperimentalPermissionsApi
@ExperimentalComposeUiApi
@ExperimentalStdlibApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@HiltViewModel
class UserViewModel @Inject constructor(
    val userRepo: UserRepository,
) : ViewModel() {
    private val tag = "UserViewModel"
    private lateinit var dataBaseListener: ListenerRegistration

    private var mUser: MutableLiveData<User?> = MutableLiveData(null)
    var user: LiveData<User?> = mUser

    private var mLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = mLoading

    //  Set
    fun setContactEmail(email: String) {
        userRepo.setContactEmail(email)
    }

    fun setIsOwner(boolean: Boolean): Task<Void> {
        return userRepo.setOwner(boolean)
    }

    fun setBoundedState(id: String, state: String): Task<Void> {
        return userRepo.setBoundState(id, state)
    }

    fun setContactNumber(number: String, @ApplicationContext context: Context) {
        val phoneNumberUtil = PhoneNumberUtil.createInstance(context)
        try {
            val newNumber = phoneNumberUtil.parse(number, "US")
            if (phoneNumberUtil.isValidNumber(newNumber)) {
                userRepo.setContactNumber(number)
            } else {
                Toast.makeText(context, "Invalid Number", Toast.LENGTH_SHORT).show()
            }
        } catch (e: NumberParseException) {
            Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
            return
        }
    }

    //  Get
    fun getUsersCompanyOwnerReference(ctx: Context, companyName: String, cb: (DocumentReference) -> Unit){
        userRepo.getUsersCompanyOwnerReference(ctx, companyName, cb)
    }

    //  Delete
        //  Delete User Account and Cache Data to Company, from settings screen
    fun settingsDeleteUserAccount(companyName: String, displayName: String, ctx: Context) {
        val fireStore = FirebaseFirestore.getInstance()
        val id = FirebaseAuth.getInstance().currentUser?.uid!!
        val data = mapOf("companyname" to companyName, "name" to displayName)
        Firebase.functions.getHttpsCallable("notifyUserDeletedAccount").call(data)
        //  Set Cached
        fireStore.collection("users").document(id).set(mapOf("cached" to true), SetOptions.merge())
        fireStore.collection("users").document(id).set(mapOf("active" to false), SetOptions.merge())
        //  Deleted Token
        FirebaseFirestore.getInstance().collection("fcmtokens")
            .whereEqualTo("id", id).get()
            .addOnSuccessListener { query ->
            query.documents.forEach { doc ->
                doc.reference.delete()
            }
        }
        //  Delete FirebaseAuth Info
        Firebase.auth.currentUser!!.delete().addOnSuccessListener {
            AuthUI.getInstance().signOut(ctx).addOnSuccessListener {
                mUser.value = null
                val intent = Intent(ctx.findActivity(), LoginActivity::class.java)
                val mainActivity = ctx.findActivity()
                ctx.startActivity(intent)
                mainActivity?.finish()
            }
        }
    }

    //  Functions
    fun logOut(ctx: Context) {
        AuthUI.getInstance().signOut(ctx).addOnSuccessListener {
            val activity = ctx.findActivity()!!
            FirebaseMessaging.getInstance().unsubscribeFromTopic("LocationUpdates")
            Log.i(tag, activity.componentName.toShortString())
            dataBaseListener.remove()
            mUser.value = null
            val intent = Intent(
                ctx, SplashActivity::class.java
            )
            ctx.startActivity(intent)
        }
    }

        //  listen to user in Firestore database
    @ExperimentalMaterialApi
    @ExperimentalAnimationApi
    @ExperimentalStdlibApi
    fun listenToUser() {
            Log.i(tag, "Called ListenToUser()")
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        mUser.value = null
        if (uid != null){
            dataBaseListener = userRepo.dbUsersRef.document(uid).addSnapshotListener() { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    Log.i(tag, "changing user")
                    mUser.value = snapshot.toObject(User::class.java)
                    mUser.value = mUser.value
                }
            }
        } else {
            mUser.value = null
        }
    }

    fun postLoading(boolean: Boolean) {
        mLoading.postValue(boolean)
    }

    override fun onCleared() {
        super.onCleared()
        dataBaseListener.remove()
        mUser.value = null
        mUser.value = mUser.value
    }
}