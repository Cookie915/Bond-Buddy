package com.bond.bondbuddy.navigation

import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.NavController
import com.bond.bondbuddy.viewmodels.UserViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.messaging.FirebaseMessaging

@ExperimentalComposeUiApi
@ExperimentalPermissionsApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalStdlibApi
@Composable
fun GraphFinder(navController: NavController, userViewModel: UserViewModel, userToNavigateTo: String?) {
    val firebaseMessaging = FirebaseMessaging.getInstance()
    val tag = "NavGraphFinder"
    Log.i("NavGraphFinder", "Starting Nav")
    val lifeCycleOwner = LocalLifecycleOwner.current
    userViewModel.user.observe(lifeCycleOwner) {
        if (it != null) {
            val companySubscriptionName = it.companyname.toString().filter { char ->
                !char.isWhitespace()
            }
            if (!it.registered) {
                navController.navigate("registrationGraph")
            } else if (it.owner == true && it.registered) {
                firebaseMessaging.subscribeToTopic(companySubscriptionName)
                firebaseMessaging.unsubscribeFromTopic("LocationUpdates")
                if (userToNavigateTo != null){
                    navController.navigate("AdminPeopleLocationsScreen/{$userToNavigateTo}")
                } else {
                    navController.navigate("adminGraph")
                }
            } else if (it.owner == false && it.registered) {
                firebaseMessaging.subscribeToTopic("LocationUpdates")
                firebaseMessaging.unsubscribeFromTopic(companySubscriptionName)
                navController.navigate("userGraph")
            }
        } else {
            Log.i(tag, "user null")
        }
    }
}
