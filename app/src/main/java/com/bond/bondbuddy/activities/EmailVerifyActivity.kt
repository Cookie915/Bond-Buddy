package com.bond.bondbuddy.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bond.bondbuddy.R
import com.bond.bondbuddy.screens.admin.ReauthenticationPrompt
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
@ExperimentalStdlibApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalPermissionsApi
@ExperimentalComposeUiApi
class EmailVerifyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentUser = FirebaseAuth.getInstance().currentUser!!
        setContent {
            BackHandler {}
            VerifyEmail(emailToSendTo = currentUser.email!!) {
                FirebaseAuth.getInstance().currentUser!!.reload().addOnSuccessListener {
                    finishAfterTransition()
                }
            }
        }
    }
}

@Composable
fun VerifyEmail(emailToSendTo: String, onEmailVerified: () -> Unit) {
    val ctx = LocalContext.current
    var email by remember {
        mutableStateOf(emailToSendTo)
    }
    var showVerifyEmail by remember {
        mutableStateOf(true)
    }
    var showChangeEmailPrompt by remember {
        mutableStateOf(false)
    }
    var showEmailTextField by remember {
        mutableStateOf(false)
    }
    var emailTextFieldState by remember {
        mutableStateOf("")
    }
    LaunchedEffect(key1 = showChangeEmailPrompt, key2 = showEmailTextField){
        showVerifyEmail = !(showChangeEmailPrompt || showEmailTextField)
    }
    BackHandler {
        if (showChangeEmailPrompt || showEmailTextField) {
            showChangeEmailPrompt = false
            showEmailTextField = false
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(id = R.color.SecondaryBlueLight)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = showVerifyEmail) {
            Card(shape = RoundedCornerShape(20.dp), elevation = 16.dp) {
                Column(
                    modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Please Verify You're Email to Continue",
                        fontWeight = FontWeight.Bold,
                    )
                    EmailSpacer()
                    Text(text = email)
                    EmailSpacer()
                    OutlinedButton(modifier = Modifier.fillMaxWidth(0.8f), onClick = {
                        FirebaseAuth.getInstance().currentUser!!.sendEmailVerification()
                        Toast.makeText(
                            ctx, "Verification email sent," + " please verify Email and check again", Toast.LENGTH_LONG
                        ).show()
                    }, border = BorderStroke(1.dp, colorResource(id = R.color.SecondaryBlueLight))) {
                        Text(text = "Send verification email", color = colorResource(id = R.color.SecondaryBlue))
                    }
                    EmailSpacer()
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(0.8f), onClick = {
                            showChangeEmailPrompt = true
                        }, border = BorderStroke(1.dp, color = colorResource(id = R.color.SecondaryBlueLight))
                    ) {
                        Text(text = "Reset Email", color = colorResource(id = R.color.SecondaryBlue))
                    }
                }
            }
        }
        AnimatedVisibility(visible = showChangeEmailPrompt) {
            ReauthenticationPrompt(onConfirm = {
                showEmailTextField = true
                showChangeEmailPrompt = false
            }) {
                showChangeEmailPrompt = false
            }
        }
        AnimatedVisibility(visible = showEmailTextField) {
            Card(modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.2f)) {
                Column(modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        value = emailTextFieldState,
                        onValueChange = {
                            emailTextFieldState = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = colorResource(id = R.color.SecondaryBlue),
                            cursorColor = colorResource(id = R.color.SecondaryBlueLight)
                        ),
                        singleLine = true,
                        placeholder = { Text(text = "New email")},
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colorResource(id = R.color.SecondaryBlue),
                            ),
                            border = BorderStroke(1.dp, colorResource(id = R.color.SecondaryBlue)),
                            onClick = {
                            FirebaseAuth.getInstance().currentUser?.updateEmail(emailTextFieldState)?.addOnCompleteListener {
                                if (it.exception != null) {
                                    Toast.makeText(ctx, "Failed to update: ${it.exception!!.localizedMessage}", Toast.LENGTH_SHORT).show()
                                } else {
                                    email = FirebaseAuth.getInstance().currentUser?.email!!
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(FirebaseAuth.getInstance().currentUser!!.uid).set(mapOf("contactemail" to email), SetOptions.merge())
                                    showChangeEmailPrompt = false
                                    showEmailTextField = false
                                }
                            }
                        },
                        ) {
                            Text(text = "Confirm", fontFamily = FontFamily(Font(R.font.inter)), color = colorResource(id = R.color.SecondaryBlueLight))
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            FirebaseAuth.getInstance().currentUser!!.reload()
            val currentUser = FirebaseAuth.getInstance().currentUser!!
            if (currentUser.isEmailVerified) {
                onEmailVerified()
            }
            delay(2500)
        }
    }
}

@Composable
private fun EmailSpacer() {
    Spacer(modifier = Modifier.height(24.dp))
}