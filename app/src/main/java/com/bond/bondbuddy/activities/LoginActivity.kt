package com.bond.bondbuddy.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import com.bond.bondbuddy.R
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.auth.FirebaseAuth
@ExperimentalComposeUiApi
@ExperimentalPermissionsApi
@ExperimentalStdlibApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
class LoginActivity : ComponentActivity() {
    val tag = "LoginActivityLogs"
    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val auth = FirebaseAuth.getInstance()
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            Log.i(tag, "Result Okay")
            auth.currentUser!!.reload()
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_CLEAR_TASK)
            this.startActivity(intent)
            finishAfterTransition()
        } else {
            if (response == null) {
                Log.i(tag, "Result Null")
                val startMain = Intent(Intent.ACTION_MAIN)
                startMain.addCategory(Intent.CATEGORY_HOME)
                startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                this.startActivity(startMain)
                finish()
            }
            if (response?.error?.errorCode == ErrorCodes.NO_NETWORK) {
                Log.i(tag, "Result No Network")
                Toast.makeText(this, "No Network Connection, Please Try Again Later", Toast.LENGTH_LONG).show()
            }
            if (response?.error?.errorCode == ErrorCodes.UNKNOWN_ERROR) {
                Log.i(tag, "Result Unknown Error: " + response.error!!.localizedMessage)
                val error = response.error!!.errorCode.toString()
                Log.i("FirebaseAuth", "Sign In Error: ${error}")
                Toast.makeText(this, "Error Occurred: $error", Toast.LENGTH_LONG).show()
                val intent = Intent(this, SplashActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_CLEAR_TASK)
                this.startActivity(intent)
                finish()
            }
        }
    }

    private val providers = arrayListOf(
        AuthUI.IdpConfig.GoogleBuilder().build(),
        AuthUI.IdpConfig.EmailBuilder().setRequireName(true).build(),
    )

    @ExperimentalPermissionsApi
    private val signInLauncher =
        registerForActivityResult(FirebaseAuthUIActivityResultContract()) { res ->
            onSignInResult(res)
        }

    private val signInIntent =
        AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers)
            .setTheme(R.style.LoginTheme)
            .setLogo(R.drawable.ic_globeicon_login)
            .setIsSmartLockEnabled(true, true)
            .setTosAndPrivacyPolicyUrls(
                Uri.parse("https://sites.google.com/view/bondbuddy-privacy-policy/home").toString(),
                Uri.parse("https://sites.google.com/view/bondbuddy-privacy-policy/home").toString()
            )
            .build()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        signInLauncher.launch(signInIntent)
    }
}