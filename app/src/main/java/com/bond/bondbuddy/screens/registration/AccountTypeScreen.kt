package com.bond.bondbuddy.screens.registration

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.NavController
import com.bond.bondbuddy.R
import com.bond.bondbuddy.viewmodels.UserViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.skydoves.landscapist.rememberDrawablePainter

@ExperimentalComposeUiApi
@ExperimentalPermissionsApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalStdlibApi
@Composable
fun AccountTypeScreen(navController: NavController, userViewModel: UserViewModel) {
    val context = LocalContext.current
    BackHandler {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(startMain)
    }
    Image(
        painter = rememberDrawablePainter(
            drawable = ResourcesCompat.getDrawable(
                context.resources, R.drawable.bg_gradient, null
            )
        ), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_appicon_foreground),
            contentDescription = "Sign In Logo",
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(1f),
            contentScale = ContentScale.FillBounds,
        )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.fillMaxHeight(0.15f))
                Text(
                    text = "I am a...", modifier = Modifier.padding(top = 25.dp, bottom = 25.dp), color = Color.White, fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(0.7f), onClick = {
                        userViewModel.setIsOwner(false).addOnSuccessListener {
                            navController.navigate("UserCompanyRegister"){
                                launchSingleTop = true
                            }
                        }
                    }, colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent), border = BorderStroke(
                        2.dp, color = Color.White
                    ), shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Client", letterSpacing = 2.sp, fontSize = 26.sp, color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(0.7f), onClick = {
                        userViewModel.setIsOwner(true).addOnSuccessListener {
                            navController.navigate("OwnerCompanyRegister"){
                                launchSingleTop
                            }
                        }
                    }, colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent), border = BorderStroke(
                        2.dp, color = Color.White
                    ), shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Bondsman", letterSpacing = 2.sp, fontSize = 26.sp, color = Color.White
                    )
                }
            }
        }
    }