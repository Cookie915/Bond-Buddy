package com.bond.bondbuddy.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
@ExperimentalPermissionsApi
@ExperimentalComposeUiApi
@SuppressLint("CustomSplashScreen")
@ExperimentalStdlibApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
class SplashActivity : AppCompatActivity() {
    val tag = "SplashActivityLogs"
    private val firebaseAuth = FirebaseAuth.getInstance()
    val fireStore = FirebaseFirestore.getInstance()
    private lateinit var mainActivityIntent: Intent
    private lateinit var loginActivityIntent: Intent
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    override fun onResume() {
        super.onResume()
        Log.i(tag, "On Resume")
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onPause() {
        super.onPause()
        Log.i(tag, "On Pause")
        firebaseAuth.removeAuthStateListener(authStateListener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(tag, "OnCreate")
        val notificationType = intent.extras?.get("Notification_Type")
        mainActivityIntent = Intent(this, MainActivity::class.java)
        if (notificationType != null){
            if (notificationType.toString() == "Location_Update"){
                mainActivityIntent.putExtra("Notification_Type", notificationType.toString())
            }
        }
        loginActivityIntent = Intent(this, LoginActivity::class.java)
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                startActivity(loginActivityIntent)
                finishAfterTransition()
            }
            if (firebaseAuth.currentUser != null) {
                startActivity(mainActivityIntent)
                finishAfterTransition()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i(tag, "OnStart")
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        Log.i(tag, "onStop")
        firebaseAuth.removeAuthStateListener(authStateListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(tag, "onDestroy")
    }
}