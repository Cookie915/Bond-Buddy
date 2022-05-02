package com.bond.bondbuddy.screens.user

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bond.bondbuddy.R
import com.bond.bondbuddy.components.composables.*
import com.bond.bondbuddy.models.User
import com.bond.bondbuddy.navigation.NavScreen
import com.bond.bondbuddy.screens.admin.ReauthenticationPrompt
import com.bond.bondbuddy.viewmodels.UserViewModel
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.auth.FirebaseAuth
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalStdlibApi
@ExperimentalPermissionsApi
@ExperimentalComposeUiApi
@Composable
fun UserHomeScreen(
    modifier: Modifier, navController: NavController, userViewModel: UserViewModel
) {
    val ctx = LocalContext.current
    //  State
    val user by userViewModel.user.observeAsState()
    var cameraFocusedUser: User? by remember {
        mutableStateOf(null)
    }
    val scrollState = rememberScrollState()

    //  Triggers
    var showPhoneTextField by remember {
        mutableStateOf(false)
    }
    var showEmailTextField by remember {
        mutableStateOf(false)
    }
    var showCamera: Boolean by remember {
        mutableStateOf(false)
    }
    var showReauthenticationPrompt by remember {
        mutableStateOf(false)
    }

    //  State Management
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val cameraOffset by animateFloatAsState(
        if (showCamera) 0.0f else screenHeight.toFloat(), TweenSpec(
            600, delay = 0, easing = FastOutSlowInEasing
        )
    )

    BackHandler(true) {
        if (showPhoneTextField || showEmailTextField || showCamera) {
            if (showPhoneTextField) showPhoneTextField = false
            if (showEmailTextField) showEmailTextField = false
            if (showCamera) showCamera = false
        } else {
            val startMain = Intent(Intent.ACTION_MAIN)
            startMain.addCategory(Intent.CATEGORY_HOME)
            startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            ctx.startActivity(startMain)
        }

    }
    Scaffold(bottomBar = { UserBottomBar(navController = navController) }) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "BondBuddy",
                        modifier = Modifier
                            .padding(start = 24.dp, top = 22.dp, bottom = 38.dp)
                            .align(Alignment.CenterStart),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(
                            Font(R.font.inter)
                        )
                    )
                    TextButton(modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp, top = 22.dp, bottom = 38.dp),
                               onClick = { userViewModel.logOut(ctx) }) {
                        Text(
                            text = "Logout",
                            color = colorResource(id = R.color.LogOutButtonRed),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily(
                                Font(R.font.inter)
                            )
                        )
                    }
                }
                GlideImage(
                    imageModel = if (user?.profilepicurl.isNullOrBlank()) R.drawable.ic_pfp_placeholder_withplus else user?.profilepicurl,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .clickable {
                            cameraFocusedUser = user
                            showCamera = true
                        },
                    requestOptions = {
                        RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    },
                    placeHolder = painterResource(id = R.drawable.ic_pfp_placeholder_withplus)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = user?.displayname.toString(),
                    modifier = Modifier,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(
                        Font(R.font.inter)
                    )
                )
                if (showPhoneTextField) {
                    PhoneContactTextField(modifier = Modifier.fillMaxWidth(0.6f), userViewModel = userViewModel, onDone = {
                        showPhoneTextField = !showPhoneTextField
                    })
                } else {
                    PhoneContactInfo(modifier = Modifier, number = user?.contactnumber, callback = {
                        showPhoneTextField = !showPhoneTextField
                    })
                }
                if (showEmailTextField) {
                    EmailContactTextField(modifier = Modifier.fillMaxWidth(0.85f), userViewModel = userViewModel, onDone = {
                        showEmailTextField = !showEmailTextField
                    })
                } else {
                    EmailContactInfo(modifier = Modifier, email = user?.contactemail, callback = {
                        showEmailTextField = !showEmailTextField
                    })
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier.verticalScroll(scrollState)
                ) {
                    SettingsScreenDivider(label = " Contact ${user?.companyname}")
                    ContactColumn(onCallClicked = {
                        userViewModel.getUsersCompanyOwnerReference(ctx, user?.companyname.toString()) { docRef ->
                            docRef.get().addOnCompleteListener { ownerDataTask ->
                                when (ownerDataTask.isSuccessful) {
                                    false -> {
                                        Toast.makeText(ctx, "Couldn't reach owner", Toast.LENGTH_SHORT).show()
                                    }
                                    true  -> {
                                        val contactNumber = ownerDataTask.result["contactnumber"]
                                        if (contactNumber != null) {
                                            val callIntent = Intent(Intent.ACTION_DIAL)
                                            callIntent.data = Uri.parse("tel:" + "1${contactNumber}")
                                            ctx.startActivity(callIntent)
                                        } else {
                                            Toast.makeText(ctx, "No contact number available", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    }, onEmailClicked = {
                        userViewModel.getUsersCompanyOwnerReference(ctx, user?.companyname.toString()) { docRef ->
                            docRef.get().addOnCompleteListener { ownerDataTask ->
                                when (ownerDataTask.isSuccessful) {
                                    false -> {
                                        Toast.makeText(ctx, "Couldn't reach owner", Toast.LENGTH_SHORT).show()
                                    }
                                    true  -> {
                                        val contactEmail = ownerDataTask.result["contactemail"]
                                        if (contactEmail != null) {
                                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:")
                                                putExtra(
                                                    Intent.EXTRA_EMAIL, arrayOf(contactEmail.toString())
                                                )
                                                putExtra(
                                                    Intent.EXTRA_SUBJECT, "User Inquiry: ${user?.displayname.toString()}"
                                                )
                                            }
                                            ctx.startActivity(emailIntent)
                                        } else {
                                            Toast.makeText(ctx, "No contact email available", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    })
                    SettingsScreenDivider(label = "Account")
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
                            data = Uri.parse("https://sites.google.com/view/bondbuddy-privacy-policy/home#h.qll7ljh0f6l1")
                        }
                        ctx.startActivity(intent)
                    }, onBugButtonClicked = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("Calebcookdev@gmail.com"))
                            putExtra(
                                Intent.EXTRA_SUBJECT, "Bug Report:\n${user?.displayname}\n${user?.id}"
                            )
                            putExtra(
                                Intent.EXTRA_TEXT, "Please Provide Device Model and Describe the Issue:\n"
                            )
                        }
                        ctx.startActivity(intent)
                    })
                }
            }
            if (showReauthenticationPrompt) {
                ReauthenticationPrompt(onConfirm = {
                    CoroutineScope(Dispatchers.IO).launch {
                        userViewModel.settingsDeleteUserAccount(
                            user!!.companyname.toString(), user!!.displayname.toString(), ctx
                        )
                    }
                }) {
                    showReauthenticationPrompt = false
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .height(LocalConfiguration.current.screenHeightDp.dp)
            .width(LocalConfiguration.current.screenWidthDp.dp)
            .offset(y = cameraOffset.dp)
            .background(Color.Black), contentAlignment = Alignment.Center
    ) {
        if (showCamera) {
            CameraCapture(
                modifier = Modifier.offset(y = cameraOffset.dp),
                userViewModel = userViewModel,
                user = cameraFocusedUser ?: User(),
                onDone = {
                    showCamera = false
                },
                onCancel = {
                    showCamera = false
                })
        }
    }
}

@Composable
fun UserBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val navScreens: List<NavScreen> = NavScreen.UserScreenList
    //  color of focused nav icon
    val navIconColor = colorResource(id = R.color.UnfocusedIconGray).copy(alpha = 0.5f)
    BottomNavigation(
        modifier = Modifier.requiredHeight(56.dp), backgroundColor = Color.White, elevation = 24.dp
    ) {
        navScreens.forEach { NavScreen ->
            val selected = navBackStackEntry?.destination?.route == NavScreen.route
            BottomNavigationItem(
                selected = selected,
                onClick = {
                    if (NavScreen.route != navBackStackEntry?.destination?.route) {
                        navController.navigate(NavScreen.route) {
                            popUpTo(navController.graph.findStartDestination().route.toString()) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    var tint by remember {
                        mutableStateOf(navIconColor)
                    }
                    Box(
                        modifier = Modifier
                            .requiredWidthIn(min = 56.dp, max = 144.dp)
                            .fillMaxHeight(), contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                modifier = Modifier.requiredSize(24.dp),
                                painter = painterResource(id = NavScreen.icon),
                                contentDescription = NavScreen.contentDisc,
                                tint = tint
                            )
                            if (selected) {
                                tint = colorResource(id = R.color.SecondaryBlue)
                                Text(text = NavScreen.label, fontSize = 10.sp, color = tint)
                            }
                        }
                    }
                },
            )
        }
    }
}