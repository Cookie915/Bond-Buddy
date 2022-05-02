package com.bond.bondbuddy.components.composables

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.bond.bondbuddy.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState

@ExperimentalPermissionsApi
@Composable
fun MapPermissions(content: @Composable () -> Unit) {
    val shouldShowRational = rememberSaveable{
        mutableStateOf(true)
    }
    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_COARSE_LOCATION)
    PermissionRequired(permissionState = locationPermissionState, permissionNotGrantedContent = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (shouldShowRational.value){
                Card(backgroundColor = Color.LightGray.copy(alpha = 1f), elevation = 16.dp) {
                    Column(modifier = Modifier.padding(16.dp),
                           horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Please allow location permissions")
                        Text(text = "to view the map")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Button(onClick = { shouldShowRational.value = false },
                                   colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(
                                       id = R.color.navitemfocused))) {
                                Text(text = "Deny")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { locationPermissionState.launchPermissionRequest() },
                                   colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(
                                       id = R.color.navitemfocused))) {
                                Text(text = "Okay!")
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                    Text(text = "Please enable location permissions in you're settings to view the map")
                }
            }
        }
    }, permissionNotAvailableContent = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            Text(text = "Please enable location permissions in you're settings to view the map")
        }
    }) {
        content()
    }
}