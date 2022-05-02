package com.bond.bondbuddy.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.bond.bondbuddy.models.TokenWithTimeStampID
import com.bond.bondbuddy.models.User
import com.bond.bondbuddy.navigation.GraphFinder
import com.bond.bondbuddy.navigation.adminGraph
import com.bond.bondbuddy.navigation.registerGraph
import com.bond.bondbuddy.navigation.userGraph
import com.bond.bondbuddy.repo.UserRepository
import com.bond.bondbuddy.viewmodels.CompanyViewModel
import com.bond.bondbuddy.viewmodels.UserViewModel
import com.bond.bondbuddy.workmanager.TokenRefreshWorkerServices
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint



@ExperimentalComposeUiApi
@ExperimentalPermissionsApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalStdlibApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val googleApiAvailability = GoogleApiAvailability.getInstance()
    private var userRepo: UserRepository = UserRepository()
    private lateinit var emailAuthStateListener: FirebaseAuth.AuthStateListener
    lateinit var userViewModel: UserViewModel
    lateinit var companyViewModel: CompanyViewModel
    private val firebaseUser = FirebaseAuth.getInstance().currentUser
    val tag = "MainActivityLogs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        emailAuthStateListener = FirebaseAuth.AuthStateListener { Auth ->
            if (Auth.currentUser != null && !Auth.currentUser!!.isEmailVerified){
                val verifyEmailIntent = Intent(this, EmailVerifyActivity::class.java)
                startActivity(verifyEmailIntent)
            }
        }
        firebaseUser?.reload()?.addOnSuccessListener {
            userRepo.dbUsersRef.document(firebaseUser.uid).get().addOnSuccessListener { snapshot ->
                //  document doesn't exist, should be first time logging in, initialize user in database
                if (snapshot?.exists() == false) {
                    val displayName = firebaseUser.displayName
                    val contactEmail = firebaseUser.email
                    val uid = firebaseUser.uid
                    val number = firebaseUser.phoneNumber
                    FirebaseMessaging.getInstance().token.addOnSuccessListener {
                        val token = it
                        val user =
                            User(displayname = displayName, id = uid, contactemail = contactEmail, fcmToken = token, contactnumber = number)
                        userRepo.dbUsersRef.document(uid).set(user, SetOptions.merge())
                        val tokenWithTimestamp = TokenWithTimeStampID(uid, token, Timestamp.now().toDate())
                        userRepo.database.collection("fcmtokens").add(tokenWithTimestamp)
                    }
                }
            }
        }
        //monthly worker to refresh fcm tokens serverside
        TokenRefreshWorkerServices.TokenRefreshWorker.enqueueTokenRefreshWork(applicationContext)
        //check for google play services
        if (googleApiAvailability.isGooglePlayServicesAvailable(applicationContext) != ConnectionResult.SUCCESS) {
            googleApiAvailability.makeGooglePlayServicesAvailable(this)
        }
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        companyViewModel = ViewModelProvider(this)[CompanyViewModel::class.java]
        setContent {
            Log.i(tag, "Setting Content")
            var userToNavigateTo: String? by remember {
                mutableStateOf(null)
            }
            //  Toast If User Is Refreshing Active Status
            val incomingIntent = intent
            if (incomingIntent != null) {
                val notificationType = incomingIntent.extras?.get("Notification_Type")
                if (notificationType == "Location_Update") {
                    Toast.makeText(LocalContext.current, "Active Status Refreshed!", Toast.LENGTH_SHORT).show()
                }
                if (notificationType == "Mark_Inactive") {
                    val userName = incomingIntent.extras!!["User_Name"].toString()
                    userToNavigateTo = userName
                }
                if (notificationType == "Bounds_Check") {
                    val userName = incomingIntent.extras!!["User_Name"].toString()
                    userToNavigateTo = userName
                }
            }
            val navController = rememberAnimatedNavController()
            AnimatedNavHost(
                navController = navController,
                startDestination = "GraphFinder",
                ){
                adminGraph(navController = navController, userViewModel = userViewModel, companyViewModel = companyViewModel)
                registerGraph(navController = navController, userViewModel = userViewModel, companyViewModel = companyViewModel)
                userGraph(navController, userViewModel)
                composable("GraphFinder"){
                   GraphFinder(navController = navController, userViewModel = userViewModel, userToNavigateTo = userToNavigateTo)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        FirebaseAuth.getInstance().addAuthStateListener(emailAuthStateListener)
        userViewModel.listenToUser()
        if (googleApiAvailability.isGooglePlayServicesAvailable(applicationContext) != ConnectionResult.SUCCESS) {
            googleApiAvailability.makeGooglePlayServicesAvailable(this)
        }
        if (!::userViewModel.isInitialized) {
            userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        }
        if (!::companyViewModel.isInitialized){
            companyViewModel = ViewModelProvider(this)[CompanyViewModel::class.java]
        }
    }

    override fun onPause() {
        super.onPause()
        FirebaseAuth.getInstance().removeAuthStateListener(emailAuthStateListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        this.viewModelStore.clear()
    }
}

// TODO
//  alpha launch
//  create play store feature graphic





