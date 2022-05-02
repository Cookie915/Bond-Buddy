package com.bond.bondbuddy.components.composables

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bond.bondbuddy.R
import com.bond.bondbuddy.components.COLLAPSE_ANIMATION_DURATION
import com.bond.bondbuddy.components.EXPAND_ANIMATION_DURATION
import com.bond.bondbuddy.components.FADE_IN_ANIMATION_DURATION
import com.bond.bondbuddy.components.FADE_OUT_ANIMATION_DURATION
import com.bond.bondbuddy.models.User
import com.bond.bondbuddy.viewmodels.UserViewModel
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.firestore.FirebaseFirestore
import com.skydoves.landscapist.CircularReveal
import com.skydoves.landscapist.glide.GlideImage
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil


@ExperimentalPermissionsApi
@ExperimentalStdlibApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun PersonCard(
    userViewModel:UserViewModel,
    user: User,
    faded: Boolean,
    onProfileClick: (User) -> Unit,
    onGlobeClick: (User) -> Unit,
    onRefreshClick: (User) -> Unit,
    onLongPress: (User) -> Unit
) {
    val context = LocalContext.current
    val firstName = user.displayname?.substringBefore(" ") ?: ""
    val lastName = user.displayname?.substringAfter(" ") ?: ""
    val contactNumber = user.contactnumber ?: ""
    val states = stringArrayResource(id = R.array.state_list).toList()
    var menuExpandedState by remember {
        mutableStateOf(false)
    }
    var menuSelectedIndex by remember {
        mutableStateOf(0)
    }
    var currentExpandedState by remember {
        mutableStateOf(false)
    }
    val enterFadeIn = remember {
        fadeIn(
            animationSpec = TweenSpec(
                durationMillis = FADE_IN_ANIMATION_DURATION, easing = FastOutLinearInEasing
            )
        )
    }
    val enterExpand = remember {
        expandVertically(animationSpec = tween(EXPAND_ANIMATION_DURATION))
    }
    val exitFadeOut = remember {
        fadeOut(
            animationSpec = TweenSpec(
                durationMillis = FADE_OUT_ANIMATION_DURATION, easing = LinearOutSlowInEasing
            )
        )
    }
    val exitCollapse = remember {
        shrinkVertically(animationSpec = tween(COLLAPSE_ANIMATION_DURATION))
    }
    val transition = updateTransition(currentExpandedState, label = "")
    val arrowRotation by transition.animateFloat(label = "Arrow Rotation") {
        if (!it) 0f else 90f
    }
    var uri by remember {
        mutableStateOf(user.profilepicurl)
    }

    DisposableEffect(key1 = Unit) {
        val listener = FirebaseFirestore.getInstance().collection("users").document(user.id!!).addSnapshotListener { snapshot, _ ->
                uri = snapshot?.get("profilepicurl").toString()
            }
        this.onDispose {
            listener.remove()
        }
    }
    LaunchedEffect(key1 = Unit){
        states.forEachIndexed { index, stateString ->
            if (stateString == user.boundingState ){
                menuSelectedIndex = index
            }
        }
    }
    Card(modifier = Modifier
        .fillMaxWidth(1f)
        .clickable(onClick = {
            currentExpandedState = !currentExpandedState
        })
        .pointerInput(Unit) {
            detectTapGestures(onLongPress = { onLongPress(user) })
        }, elevation = 0.dp
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(2.dp))
                IconButton(
                    onClick = {
                        onProfileClick(user)
                    }, modifier = Modifier.size(44.dp)
                ) {
                    val colorMatrix = ColorMatrix()
                    colorMatrix.setToSaturation(0.0f)
                    GlideImage(
                        imageModel = if (user.profilepicurl.isBlank()) R.drawable.ic_pfp_placeholder_withplus else uri,
                        requestOptions = {
                            RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).circleCrop()
                        },
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.ic_pfp_placeholder_withplus),
                        placeHolder = painterResource(id = R.drawable.ic_pfp_placeholder_withplus),
                        circularReveal = CircularReveal(),
                        alpha = if (faded) 0.7f else 1.0f,
                        colorFilter = if (faded) ColorFilter.colorMatrix(colorMatrix) else null
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
                Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$firstName $lastName",
                        modifier = Modifier,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily(
                            Font(R.font.inter)
                        ),
                        color = Color.Black
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = (8).dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        currentExpandedState = !currentExpandedState
                    }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ico_dropdown),
                            contentDescription = "Detail Button",
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .rotate(arrowRotation)
                                .requiredSize(12.dp)
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = currentExpandedState, enter = enterExpand + enterFadeIn, exit = exitCollapse + exitFadeOut
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color.White)
                ) {
                    Column(Modifier.padding(0.dp)) {
                        Spacer(modifier = Modifier.height(15.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(modifier = Modifier.size(32.dp), onClick = {
                                val callIntent = Intent(Intent.ACTION_DIAL)
                                callIntent.data = Uri.parse("tel:" + "1${contactNumber}")
                                context.startActivity(callIntent)
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_phone),
                                    contentDescription = "Contact Number",
                                    tint = Color.Unspecified
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            val phoneNumberUtil = PhoneNumberUtil.createInstance(LocalContext.current)
                            if (contactNumber.isBlank()) {
                                Text(
                                    text = "Number Not Provided", color = Color.Black
                                )
                            } else {
                                val phoneNumber = phoneNumberUtil.parse(contactNumber, "US")
                                Text(
                                    text = phoneNumberUtil.format(
                                        phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
                                    ) ?: "", color = Color.Black
                                )
                            }
                            if (user.active){
                                Row(modifier = Modifier.fillMaxWidth().padding(end = 6.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                Menu(
                                    menuItems = stringArrayResource(id = R.array.state_list).toList(),
                                    menuExpandedState = menuExpandedState,
                                    selectedIndex = menuSelectedIndex,
                                    updateMenuExpandedState = { menuExpandedState = !menuExpandedState},
                                    onDismissMenu = { menuExpandedState = !menuExpandedState },
                                    onMenuItemClicked = { index, state ->
                                        menuSelectedIndex = index
                                        userViewModel.setBoundedState(user.id!!, state).addOnSuccessListener {
                                            Toast.makeText(context, "User bound state set to $state", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(modifier = Modifier.size(32.dp), onClick = {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf(user.contactemail))
                                }
                                context.startActivity(emailIntent)
                            }) {
                                Icon(

                                    painter = painterResource(id = R.drawable.ic_email),
                                    contentDescription = "Email",
                                    tint = Color.Unspecified,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = user.contactemail ?: "Email Not Provided", color = Color.Black
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onRefreshClick(user) }, modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = R.drawable.ic_refreshlocation
                                        ), contentDescription = "Refresh User Location", tint = Color.Black, modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(0.dp))
                                IconButton(
                                    onClick = { onGlobeClick(user) }, modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_globe_grid),
                                        contentDescription = "Go To Location",
                                        tint = Color.Black,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Divider(
                modifier = Modifier.fillMaxWidth(1f), color = colorResource(id = R.color.ExtraLightDividerGray), thickness = 2.dp
            )
        }
    }
}