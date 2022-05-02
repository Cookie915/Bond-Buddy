package com.bond.bondbuddy.screens.admin

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.bond.bondbuddy.R
import com.bond.bondbuddy.components.composables.AccountColumn
import com.bond.bondbuddy.components.composables.HelpColumn
import com.bond.bondbuddy.components.composables.SettingsScreenDivider
import com.bond.bondbuddy.components.composables.TopColumn
import com.bond.bondbuddy.viewmodels.CompanyViewModel
import com.bond.bondbuddy.viewmodels.UserViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

@ExperimentalComposeUiApi
@ExperimentalPermissionsApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalStdlibApi
@Composable
fun AdminSettingsScreen(navController: NavController, userViewModel: UserViewModel, companyViewModel: CompanyViewModel) {
    val ctx = LocalContext.current
    val user by userViewModel.user.observeAsState()

    //  Trigger
    var showReauthenticationPrompt by remember {
        mutableStateOf(false)
    }

    Scaffold(bottomBar = {
        AdminBottomBar(navController)
    }) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                TopColumn(onLogoutClicked = {
                    userViewModel.logOut(ctx)
                })
                AccountColumn(onChangePasswordClick = {
                    val email = FirebaseAuth.getInstance().currentUser!!.email!!
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    Toast.makeText(ctx, "Sent Password Rest Email to $email", Toast.LENGTH_SHORT).show()
                }, onDeletedAccountClicked = {
                    showReauthenticationPrompt = true
                })
                SettingsScreenDivider(label = "Help")
                HelpColumn(onPrivacyClicked = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://sites.google.com/view/bondbuddy-privacy-policy/home")
                    }
                    ctx.startActivity(intent)
                }, onBugButtonClicked = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        val deviceModel = android.os.Build.MODEL
                        val apiLevel = android.os.Build.VERSION.SDK_INT
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("Calebcookdev@gmail.com"))
                        putExtra(
                            Intent.EXTRA_SUBJECT, "Bug Report:\n${user?.displayname}\n${user?.id}\n$deviceModel\n$apiLevel"
                        )
                        putExtra(
                            Intent.EXTRA_TEXT, "Please describe the issue providing as many details as possible! \n"
                        )

                    }
                    ctx.startActivity(intent)
                })
                Spacer(modifier = Modifier.height(60.dp))
            }
            // Dialogs
            if (showReauthenticationPrompt) {
                ReauthenticationPrompt(onConfirm = {
                    companyViewModel.deleteCompanyAccount(user!!.companyname.toString())
                }) {
                    showReauthenticationPrompt = false
                }
            }
        }
    }
}

@Composable
fun ReauthenticationPrompt(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var password by remember {
        mutableStateOf("")
    }
    var confirmPassword by remember {
        mutableStateOf("")
    }
    var passwordVisualTransformation: VisualTransformation? by remember {
        mutableStateOf(VisualTransformation.None)
    }
    var confirmVisualTransformation: VisualTransformation? by remember {
        mutableStateOf(VisualTransformation.None)
    }
    var isError by remember {
        mutableStateOf(false)
    }
    val ctx = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember {
        FocusRequester()
    }
    Dialog(onDismissRequest = {
        onDismiss()
    }) {
        Card {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Enter Password",
                        fontFamily = FontFamily(Font(R.font.inter)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = password,
                    onValueChange = {
                        password = it
                    },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            passwordVisualTransformation = if (it.isFocused) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            }
                        },
                    singleLine = true,
                    label = { Text(text = "Password") },
                    visualTransformation = passwordVisualTransformation!!,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }),
                    colors = TextFieldDefaults.textFieldColors(
                        cursorColor = colorResource(id = R.color.SecondaryBlueLight),
                        backgroundColor = Color.Transparent,
                        focusedLabelColor = colorResource(id = R.color.SecondaryBlueLight),
                        focusedIndicatorColor = colorResource(id = R.color.SecondaryBlueLight)
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = confirmPassword,
                          onValueChange = {
                              isError = confirmPassword == password
                              confirmPassword = it
                          },
                          modifier = Modifier.onFocusChanged {
                              confirmVisualTransformation = if (it.isFocused) {
                                  VisualTransformation.None
                              } else {
                                  PasswordVisualTransformation()
                              }
                          },
                          isError = isError,
                          singleLine = true,
                          label = { Text(text = "Confirm Password") },
                          visualTransformation = confirmVisualTransformation!!,
                          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                          keyboardActions = KeyboardActions(onDone = {
                              onConfirm()
                          }),
                          colors = TextFieldDefaults.textFieldColors(
                              cursorColor = colorResource(id = R.color.SecondaryBlueLight),
                              backgroundColor = Color.Transparent,
                              focusedLabelColor = colorResource(id = R.color.SecondaryBlueLight),
                              focusedIndicatorColor = colorResource(id = R.color.SecondaryBlueLight)
                          )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val firebaseAuth = FirebaseAuth.getInstance()
                        val email = firebaseAuth.currentUser!!.email
                        val credential = EmailAuthProvider.getCredential(email.toString(), confirmPassword)
                        firebaseAuth.currentUser!!.reauthenticate(credential).addOnCompleteListener {
                            when (it.isSuccessful) {
                                false -> {
                                    Toast.makeText(ctx, "Failed to authenticate user, Invalid credentials", Toast.LENGTH_SHORT).show()
                                    password = ""
                                    confirmPassword = ""
                                    focusManager.clearFocus(true)
                                }
                                true  -> {
                                    Toast.makeText(ctx, "User re-authenticated", Toast.LENGTH_SHORT).show()
                                    password = ""
                                    confirmPassword = ""
                                    focusManager.clearFocus(true)
                                    onConfirm()
                                }
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorResource(id = R.color.SecondaryBlueLight),
                        contentColor = Color.White,
                        disabledBackgroundColor = Color.LightGray
                    ), enabled = password == confirmPassword && password != ""
                ) {
                    Text(text = "Confirm")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            DisposableEffect(Unit) {
                focusRequester.requestFocus()
                onDispose { }
            }
        }
    }
}
