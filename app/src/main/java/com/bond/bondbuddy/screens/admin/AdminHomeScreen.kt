package com.bond.bondbuddy.screens.admin

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bond.bondbuddy.R
import com.bond.bondbuddy.components.animateHorizontalAlignmentAsState
import com.bond.bondbuddy.components.composables.*
import com.bond.bondbuddy.models.Response
import com.bond.bondbuddy.models.User
import com.bond.bondbuddy.navigation.NavScreen
import com.bond.bondbuddy.viewmodels.CompanyViewModel
import com.bond.bondbuddy.viewmodels.UserViewModel
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class States {
    ACTIVE, INACTIVE, CACHED
}

@ExperimentalAnimationApi
@ExperimentalPermissionsApi
@ExperimentalStdlibApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun AdminHomeScreen(navController: NavController, userViewModel: UserViewModel, companyViewModel: CompanyViewModel) {
    val context = LocalContext.current
    //  State
    val user by userViewModel.user.observeAsState()
    val companyMembers by companyViewModel.companyMembers.collectAsState()
    var inactiveMembers by remember {
        mutableStateOf(listOf<User>())
    }
    var activeMembers by remember {
        mutableStateOf(listOf<User>())
    }
    var cachedUsers by remember {
        mutableStateOf(listOf<User>())
    }
    var cameraFocusedUser: User? by remember {
        mutableStateOf(null)
    }
    var userToRemove: User? by remember {
        mutableStateOf(null)
    }
    //  Triggers
    var showPhoneTextField by remember {
        mutableStateOf(false)
    }
    var showEmailTextField by remember {
        mutableStateOf(false)
    }
    var showCamera by remember {
        mutableStateOf(false)
    }
    var showInactiveUserDialog by remember {
        mutableStateOf(false)
    }
    var showRemoveUserDialog by remember {
        mutableStateOf(false)
    }
    var showLoading by remember {
        mutableStateOf(false)
    }
    //  State Management
    BackHandler(
        enabled = showEmailTextField || showPhoneTextField || showCamera || cameraFocusedUser != null || showRemoveUserDialog
    ) {
        showPhoneTextField = false
        showEmailTextField = false
        showCamera = false
        cameraFocusedUser = null
        showRemoveUserDialog = false
        userToRemove = null
    }
    BackHandler {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(startMain)
    }
    val swipeState = rememberSwipeableState(0)
    var horizontalBias by remember {
        when (swipeState.currentValue) {
            0    -> mutableStateOf(-1f)
            1    -> mutableStateOf(0f)
            else -> {
                mutableStateOf(1f)
            }
        }
    }
    LaunchedEffect(key1 = swipeState.isAnimationRunning) {
        horizontalBias = when (swipeState.targetValue) {
            0    -> {
                -1f
            }
            1    -> {
                0f
            }
            else -> {
                1f
            }
        }
    }
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val cameraOffset by animateFloatAsState(
        if (showCamera) 0.0f else screenHeight.toFloat(), TweenSpec(
            600, delay = 25, easing = FastOutSlowInEasing
        )
    )
    // Layout
    Scaffold(bottomBar = { AdminBottomBar(navController = navController) }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            //  Dialogs
            if (showInactiveUserDialog) {
                InactiveUsersDialog { showInactiveUserDialog = false }
            }
            if (showRemoveUserDialog && userToRemove != null) {
                RemoveUserDialog(companyViewModel = companyViewModel, userToRemove = userToRemove!!, onDismiss = { showRemoveUserDialog = false })
            }
            Column(
                modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BondBuddy",
                    modifier = Modifier.padding(top = 22.dp, bottom = 38.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(
                        Font(R.font.inter)
                    )
                )
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
                    placeHolder = painterResource(id = R.drawable.ic_pfp_placeholder_withplus),
                    error = painterResource(id = R.drawable.ic_pfp_placeholder)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = user?.displayname ?: "",
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
                    PhoneContactInfo(modifier = Modifier, number = user?.contactnumber ?: "", callback = {
                        showPhoneTextField = !showPhoneTextField
                    })
                }
                if (showEmailTextField) {
                    EmailContactTextField(modifier = Modifier.fillMaxWidth(0.85f), userViewModel = userViewModel, onDone = {
                        showEmailTextField = !showEmailTextField
                    })
                } else {
                    EmailContactInfo(modifier = Modifier, email = user?.contactemail ?: "", callback = {
                        showEmailTextField = !showEmailTextField
                    })
                }
                Spacer(modifier = Modifier.height(8.dp))
                CompanyDivider(companyName = user?.companyname ?: "")
                ActiveInactiveDivider(horizontalBias) { showInactiveUserDialog = true }
                BoxWithConstraints {
                    val constrainScope = this
                    val maxWidth = with(LocalDensity.current) {
                        constrainScope.maxWidth.toPx()
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .swipeable(
                                state = swipeState, anchors = mapOf(
                                    0f to States.ACTIVE.ordinal,
                                    -maxWidth to States.INACTIVE.ordinal,
                                    (-maxWidth * 2) to States.CACHED.ordinal
                                ), orientation = Orientation.Horizontal, reverseDirection = false
                            )
                    ) {
                        //  Company Members
                        when (companyMembers) {
                            is Response.Loading -> {
                                Box(modifier = Modifier
                                    .fillMaxSize()
                                    .offset {
                                        IntOffset(x = swipeState.offset.value.roundToInt(), y = 0)
                                    }, contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier, color = colorResource(id = R.color.SecondaryBlueLight), strokeWidth = 3.dp
                                    )
                                }
                            }
                            is Response.Failure -> {
                                Box(modifier = Modifier
                                    .fillMaxSize()
                                    .offset {
                                        IntOffset(x = swipeState.offset.value.roundToInt(), y = 0)
                                    }, contentAlignment = Alignment.Center) {
                                    Text(text = "Unable to fetch company members")
                                }
                            }
                            is Response.Success -> {
                                val activeUsers = companyMembers.data!!.filter { member ->
                                    member.active &&
                                    !member.cached
                                }
                                val inactiveUsers = companyMembers.data!!.filter { member ->
                                    !member.active &&
                                    !member.cached
                                }
                                inactiveMembers = inactiveUsers
                                activeMembers = activeUsers
                                cachedUsers = companyMembers.data!!.filter { members ->
                                    members.cached
                                }
                                //  Active Members
                                CompanyMemberLazyColumn(modifier = Modifier.offset {
                                    IntOffset(x = swipeState.offset.value.roundToInt(), y = 0)
                                }, userViewModel = userViewModel, users = activeMembers, onGlobeClick = { user ->
                                    navController.navigate("${NavScreen.AdminPeopleLocationsScreen.route}/{${user.displayname}}")
                                }, onProfileClick = { user ->
                                    cameraFocusedUser = user
                                    showCamera = true
                                }, onRefreshClick = { user ->
                                    //  Payload for Https Callable
                                    val data = mapOf(
                                        "id" to user.id,
                                        "companyName" to user.companyname,
                                    )
                                    showLoading = true
                                    Firebase.functions.getHttpsCallable("directLocationUpdate").call(
                                        data
                                    ).addOnCompleteListener {
                                        showLoading = false
                                        Toast.makeText(context, "Attempting to update location..", Toast.LENGTH_SHORT).show()
                                    }
                                }, onLongPress = { user ->
                                    userToRemove = user
                                    showRemoveUserDialog = true
                                }, emptyMsg = "No Active Users")
                                //  Inactive Members
                                CompanyMemberLazyColumn(modifier = Modifier.offset {
                                    IntOffset(x = (swipeState.offset.value + maxWidth).toInt(), y = 0)
                                }, userViewModel = userViewModel, users = inactiveMembers, onGlobeClick = { user ->
                                    navController.navigate("${NavScreen.AdminPeopleLocationsScreen.route}/{${user.displayname}}")
                                }, onRefreshClick = {
                                    Toast.makeText(
                                        context, "Unable To Update Inactive User Profiles", Toast.LENGTH_SHORT
                                    ).show()
                                }, onProfileClick = { user ->
                                    cameraFocusedUser = user
                                    showCamera = true
                                }, onLongPress = { user ->
                                    userToRemove = user
                                    showRemoveUserDialog = true
                                }, emptyMsg = "No Inactive Users")
                                // Cached Users Column
                                CompanyMemberLazyColumn(modifier = Modifier.offset {
                                    IntOffset(x = (swipeState.offset.value + maxWidth * 2).toInt(), y = 0)
                                }, userViewModel = userViewModel, users = cachedUsers, onGlobeClick = { user ->
                                    navController.navigate("${NavScreen.AdminPeopleLocationsScreen.route}/{${user.displayname}}")
                                }, onProfileClick = {
                                    Toast.makeText(
                                        context, "Unable To Update Cached User Profiles", Toast.LENGTH_SHORT
                                    ).show()
                                }, onRefreshClick = {
                                    Toast.makeText(
                                        context, "Unable To Update Cached User Profiles", Toast.LENGTH_SHORT
                                    ).show()
                                }, onLongPress = { user ->
                                    userToRemove = user
                                    showRemoveUserDialog = true
                                }, emptyMsg = "No Cached Users"
                                )
                            }
                        }
                    }
                }
            }
            if (showLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center), color = colorResource(id = R.color.SecondaryBlueLight), strokeWidth = 3.dp
                )
            }
            if (cameraFocusedUser != null) {
                CameraCapture(modifier = Modifier.offset(y = cameraOffset.dp), userViewModel = userViewModel, user = cameraFocusedUser ?: User(), onDone = {
                    showCamera = false
                }, onCancel = {
                    showCamera = false
                })
            }
        }
    }
}

@Composable
fun RemoveUserDialog(
    companyViewModel: CompanyViewModel,
    userToRemove: User,
    onDismiss: () -> Unit,
) {
    Log.i("UserToDelete", "${userToRemove.id} ${userToRemove.displayname}")
    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier.fillMaxWidth(), elevation = 4.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Delete User?", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily(
                                Font(R.font.inter)
                            )
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            text = "Warning: Removing a user deletes all user data and removes them from the company, the data isn't recoverable.",
                            fontSize = 14.sp,
                            fontFamily = FontFamily(
                                Font(R.font.inter)
                            ),
                            fontWeight = FontWeight.Light,
                            color = Color.Gray
                        )
                    }
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            //  Dismiss
                            TextButton(
                                onClick = {
                                    onDismiss()
                                },
                            ) {
                                Text(
                                    text = "Dismiss",
                                    fontFamily = FontFamily(Font(R.font.inter)),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                    color = colorResource(id = R.color.SecondaryBlueLight)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            //  Confirm
                            TextButton(
                                onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        companyViewModel.removeCompanyMember(userToRemove.id!!, userToRemove.companyname.toString())
                                    }
                                    onDismiss()
                                },
                            ) {
                                Text(
                                    text = "Confirm",
                                    fontFamily = FontFamily(Font(R.font.inter)),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                    color = colorResource(id = R.color.SecondaryBlueLight)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun InactiveUsersDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f), elevation = 4.dp
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                Column {
                    Text(
                        text = "Inactive Users", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily(
                            Font(R.font.inter)
                        )
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        text = stringResource(R.string.InactiveUserDialogText),
                        fontSize = 14.sp,
                        fontFamily = FontFamily(
                            Font(R.font.inter)
                        ),
                        fontWeight = FontWeight.Light,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@ExperimentalPermissionsApi
@ExperimentalStdlibApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun CompanyMemberLazyColumn(
    modifier: Modifier,
    userViewModel: UserViewModel,
    users: List<User>,
    onGlobeClick: (User) -> Unit,
    onProfileClick: (User) -> Unit,
    onRefreshClick: (User) -> Unit,
    onLongPress: (User) -> Unit,
    emptyMsg: String
) {
    val listState = rememberLazyListState()
    if (users.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = emptyMsg)
        }
    } else {
        LazyColumn(
            modifier = modifier,
            state = listState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(users) {
                PersonCard(userViewModel = userViewModel, user = it, faded = !it.active || it.cached, onProfileClick = { user ->
                    onProfileClick(user)
                }, onGlobeClick = { user ->
                    onGlobeClick(user)
                }, onRefreshClick = { user ->
                    onRefreshClick(user)
                }, onLongPress = { user ->
                    onLongPress(user)
                })
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun CompanyDivider(companyName: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(colorResource(id = R.color.DividerGray))
            .border(1.dp, color = colorResource(id = R.color.DividerStrokeGray)),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$companyName :", modifier = Modifier.offset(x = 15.dp), fontFamily = FontFamily(
                Font(R.font.inter)
            ), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorResource(id = R.color.UnfocusedIconGray)
        )
    }
}

@Composable
fun ActiveInactiveDivider(horizontalBias: Float, onIconPress: () -> Unit) {
    val tabAlignment by animateHorizontalAlignmentAsState(horizontalBias)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(colorResource(id = R.color.SecondaryBlueLight))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active",
                    fontFamily = FontFamily(
                        Font(R.font.inter)
                    ),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (horizontalBias == (-1).toFloat()) Color.White else Color.White.copy(alpha = 0.4f)
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inactive",
                    fontFamily = FontFamily(
                        Font(R.font.inter)
                    ),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (horizontalBias == 0.toFloat()) Color.White else Color.White.copy(alpha = 0.4f)
                )
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Cached",
                    fontFamily = FontFamily(
                        Font(R.font.inter)
                    ),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (horizontalBias == 1.toFloat()) Color.White else Color.White.copy(alpha = 0.4f)
                )
                Row(horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { onIconPress() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_question_mark),
                            contentDescription = "Help",
                            modifier = Modifier.size(16.dp),
                            tint = colorResource(id = R.color.white)
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxSize(), horizontalAlignment = tabAlignment, verticalArrangement = Arrangement.Bottom
        ) {
            Divider(
                modifier = Modifier.fillMaxWidth(0.33f), color = Color.White, thickness = 4.dp
            )
        }
    }
}

@Composable
fun AdminBottomBar(navController: NavController) {
    LaunchedEffect(key1 = Unit){
        navController.backQueue.forEach {
            Log.i("tester", "${it.destination.route}")
        }
        Log.i("tester", "Backstack Size ${navController.backQueue.size}")
    }
    BottomNavigation(
        modifier = Modifier.requiredHeight(56.dp), backgroundColor = Color.White, elevation = 24.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val navScreens: List<NavScreen> = NavScreen.AdminScreenList
        navScreens.forEach { NavScreen ->
            BottomNavigationItem(
                selected = currentDestination?.hierarchy?.any {it.route == NavScreen.route} == true,
                onClick = {
                    Log.i("tester", "Backstack Size ${navController.backQueue.size}")
                    navController.navigate(NavScreen.route)
                    {
                        Log.i("tester", "${navController.graph.startDestinationRoute}")
                        popUpTo(navController.graph.startDestinationId){
                            saveState = true
                        }
                        restoreState = true
                        launchSingleTop = true
                    }
                },
                icon = {
                    Icon(
                                modifier = Modifier.requiredSize(24.dp),
                        painter = painterResource(id = NavScreen.icon),
                        contentDescription = NavScreen.contentDisc,
                    )
                },
                modifier = Modifier,
                enabled = currentDestination?.hierarchy?.any {it.route == NavScreen.route} == false,
                selectedContentColor = colorResource(id = R.color.SecondaryBlueLight),
                unselectedContentColor = colorResource(id = R.color.UnfocusedIconGray)
            )
        }
    }
}