package com.bond.bondbuddy.navigation

import android.Manifest
import android.os.Build
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.bond.bondbuddy.R
import com.bond.bondbuddy.screens.admin.AdminHomeScreen
import com.bond.bondbuddy.screens.admin.AdminPeopleLocationsScreen
import com.bond.bondbuddy.screens.admin.AdminSettingsScreen
import com.bond.bondbuddy.screens.registration.AccountTypeScreen
import com.bond.bondbuddy.screens.registration.OwnerCompanyRegister
import com.bond.bondbuddy.screens.registration.UserCompanyRegister
import com.bond.bondbuddy.screens.user.UserHomeScreen
import com.bond.bondbuddy.viewmodels.CompanyViewModel
import com.bond.bondbuddy.viewmodels.UserViewModel
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.navigation
import com.google.accompanist.permissions.*


@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalStdlibApi
@ExperimentalPermissionsApi
@ExperimentalComposeUiApi
fun NavGraphBuilder.adminGraph(
    navController: NavController,
    userViewModel: UserViewModel,
    companyViewModel: CompanyViewModel
) {
    navigation(
        "AdminHomeScreen", "adminGraph"
    ) {
        composable("AdminHomeScreen") {
            val companyName = userViewModel.user.value?.companyname
            if (companyName != null){
                companyViewModel.initializeCompanyMembers(companyName)
            }
            AdminHomeScreen(navController = navController, userViewModel = userViewModel, companyViewModel)
        }
        composable("AdminSettingsScreen") {
            AdminSettingsScreen(navController = navController, userViewModel =  userViewModel, companyViewModel = companyViewModel)
        }
        composable(
            route = "AdminPeopleLocationsScreen/{UserToNavigateTo}",
            arguments = listOf(navArgument("UserToNavigateTo"){ NavType.StringType})
        ) { backStackEntry ->
            backStackEntry.arguments?.getString("UserToNavigateTo").let { user ->
                val userName = user!!.filterNot { char ->
                    if (char == '{' || char == '}') return@filterNot true else false
                }
                AdminPeopleLocationsScreen(
                    navController = navController,  userToNavigateTo = userName, companyViewModel = companyViewModel
                )
            }
        }
        composable("AdminPeopleLocationsScreen"){
            AdminPeopleLocationsScreen(
                navController = navController,  userToNavigateTo = null, companyViewModel = companyViewModel
            )
        }
    }
}

@ExperimentalPermissionsApi
@ExperimentalComposeUiApi
@ExperimentalStdlibApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
fun NavGraphBuilder.registerGraph(
    navController: NavController,
    userViewModel: UserViewModel,
    companyViewModel: CompanyViewModel
) {
    navigation(
        "AccountTypeScreen", "registrationGraph"
    ) {
        composable("AccountTypeScreen") {
            AccountTypeScreen(
                navController = navController, userViewModel = userViewModel
            )
        }
        composable("OwnerCompanyRegister") {
            OwnerCompanyRegister(
                navController = navController,
                companyViewModel
            )
        }
        composable("UserCompanyRegister") {
            UserCompanyRegister(
                navController = navController,
                companyViewModel
            )
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalStdlibApi
@ExperimentalComposeUiApi
@ExperimentalPermissionsApi
fun NavGraphBuilder.userGraph(
        navController: NavController,
        userViewModel: UserViewModel,
){
    navigation("UserHomeScreen", "userGraph"){
        composable("UserHomeScreen"){
            val doNotShowRational = rememberSaveable {
                mutableStateOf(false)
            }
            val permissionState = rememberMultiplePermissionsState(permissions = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            PermissionsRequired(
                multiplePermissionsState = permissionState,
                permissionsNotAvailableContent = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Enable \"Allow all the time\" for location access in your phones settings to continue",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                },
                permissionsNotGrantedContent = {
                    if (doNotShowRational.value) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Enable location permissions in your phones settings to continue",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AlertDialog(
                                onDismissRequest = {
                                },
                                buttons = {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(end = 4.dp), horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = {
                                            doNotShowRational.value = true
                                        }) {
                                            Text(text = "Deny", color = colorResource(id = R.color.SecondaryBlueLight))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        TextButton(onClick = {
                                            permissionState.launchMultiplePermissionRequest()
                                        }) {
                                            Text(text = "Confirm", color = colorResource(id = R.color.SecondaryBlueLight))
                                        }
                                    }
                                },
                                modifier = Modifier,
                                title = { Text(text = "Permission Required") },
                                text = {
                                    Text(text = stringResource(id = R.string.LocationPermissionsText))
                                },
                            )
                        }
                    }
                },
            ) {
                if (Build.VERSION.SDK_INT > 29) {
                    val permState = rememberPermissionState(permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    PermissionRequired(
                        permissionState = permState,
                        permissionNotGrantedContent = {
                            if (doNotShowRational.value) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = stringResource(id = R.string.LocationPermissionsText),
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            } else {
                                AlertDialog(onDismissRequest = {
                                },
                                            buttons = {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(end = 4.dp),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    TextButton(onClick = {
                                                        doNotShowRational.value = true
                                                    }) {
                                                        Text(text = "Deny", color = colorResource(id = R.color.SecondaryBlueLight))
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    TextButton(onClick = {
                                                        permState.launchPermissionRequest()
                                                    }) {
                                                        Text(text = "Confirm", color = colorResource(id = R.color.SecondaryBlueLight))
                                                    }
                                                }
                                            },
                                            modifier = Modifier,
                                            title = { Text(text = "Permission Required") },
                                            text = { Text(text = "Please Select \"Allow all the time\" for location access in your phone's settings to continue") })
                            }
                        }, permissionNotAvailableContent = {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "Please Select \"Allow all the time\" for location access in your phone's settings to continue")
                            }
                        }) {
                        UserHomeScreen(
                            modifier = Modifier.fillMaxSize(),
                            navController = navController,
                            userViewModel = userViewModel
                        )
                    }
        }
    }
}}}