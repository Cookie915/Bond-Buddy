package com.bond.bondbuddy.screens.admin

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bond.bondbuddy.R
import com.bond.bondbuddy.components.composables.LocationsMap
import com.bond.bondbuddy.components.composables.MapSearchBar
import com.bond.bondbuddy.components.composables.UserLocationRow
import com.bond.bondbuddy.components.toLatLng
import com.bond.bondbuddy.models.Response
import com.bond.bondbuddy.models.User
import com.bond.bondbuddy.models.UserLocation
import com.bond.bondbuddy.util.autocomplete.ValueAutoCompleteEntity
import com.bond.bondbuddy.util.autocomplete.asAutoCompleteEntities
import com.bond.bondbuddy.util.rememberMapViewWithLifecycle
import com.bond.bondbuddy.viewmodels.CompanyViewModel
import com.bond.bondbuddy.viewmodels.MapViewModel
import com.bond.bondbuddy.viewmodels.MapViewModel.Companion.addLocationsAndPan
import com.bond.bondbuddy.viewmodels.MapViewModel.Companion.animateCameraDefaultPosition
import com.bond.bondbuddy.viewmodels.MapViewModel.Companion.moveCameraDefaultPosition
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.maps.MapView
import java.text.DateFormat
import java.util.*
import androidx.compose.animation.slideInVertically as slideInVertically1

@ExperimentalComposeUiApi
@ExperimentalPermissionsApi
@Composable
fun AdminPeopleLocationsScreen(
    navController: NavController, userToNavigateTo: String?, companyViewModel: CompanyViewModel
) {
    val ctx: Context = LocalContext.current
    val focusManger = LocalFocusManager.current
    val mapViewModel: MapViewModel = viewModel()

    //  State
    var selectedUser: User? by remember {
        mutableStateOf(null)
    }
    var selectedUserLocations by remember { mutableStateOf<(List<UserLocation>)?>(null) }
    var userToNavigateToState by remember {
        mutableStateOf(userToNavigateTo)
    }
    var autofillNames: List<ValueAutoCompleteEntity<User>> by remember {
        mutableStateOf(listOf())
    }
    val companyUsers by companyViewModel.companyMembers.collectAsState()
    val lazyColumnState = rememberLazyListState()
    val mapView: MapView = rememberMapViewWithLifecycle()

    //  Layout
    Scaffold(bottomBar = { AdminBottomBar(navController = navController) }) {
        when (companyUsers) {
            is Response.Loading         -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center), color = colorResource(id = R.color.SecondaryBlue), 2.dp
                    )
                }
            }
            is Response.Success     -> {
                autofillNames = companyUsers.data!!.asAutoCompleteEntities { user, query ->
                    return@asAutoCompleteEntities user.displayname!!.toLowerCase(Locale.current).startsWith(
                        query.toLowerCase(Locale.current)
                    )
                }
                BackHandler {
                    focusManger.clearFocus(true)
                    if (selectedUser != null) {
                        mapViewModel.showLastLocationMarkers(companyUsers.data, ctx)
                        mapViewModel.animateCameraDefaultPosition()
                        selectedUserLocations = null
                        selectedUser = null
                        userToNavigateToState = null
                    } else {
                        navController.popBackStack()
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    LocationsMap(
                        mapView = mapView,
                        mapViewModel,
                        companyViewModel,
                        userSelectedCb = { user ->
                            Log.i("AdminPeopleScreen", "UserSelectedDB")
                            selectedUser = user
                            companyViewModel.getUserLocationList(selectedUser!!).get().addOnSuccessListener { locations ->
                                val tempList: MutableList<UserLocation> = mutableListOf()
                                locations.forEach { loc ->
                                    val location = loc.toObject(UserLocation::class.java)
                                    tempList.add(location)
                                }
                                tempList.toList()
                                tempList.sortWith { o1, o2 -> o1!!.timestamp!!.compareTo(o2!!.timestamp) }
                                tempList.reverse()
                                selectedUserLocations = tempList
                            }.continueWith {
                                mapViewModel.addLocationsAndPan(ctx, selectedUserLocations!!)
                            }
                        },
                    ) {
                        mapViewModel.moveCameraDefaultPosition()
                        mapViewModel.showLastLocationMarkers(companyUsers.data, ctx)
                        if (userToNavigateToState != null) {
                            selectedUser = getUserForMap(
                                companyViewModel, mapViewModel, ctx, companyUsers.data, userToNavigateToState
                            ) { userLocations ->
                                selectedUserLocations = userLocations
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = 16.dp), horizontalArrangement = Arrangement.Center
                    ) {
                        MapSearchBar(items = autofillNames) {
                            focusManger.clearFocus()
                            selectedUser = getUserForMap(
                                companyViewModel, mapViewModel, ctx, companyUsers.data, it.value.displayname
                            ) { userLocations ->
                                selectedUserLocations = userLocations
                            }
                        }
                    }
                    AnimatedVisibility(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        visible = selectedUser != null,
                        enter = fadeIn() + slideInVertically1(),
                        exit = fadeOut() + slideOutVertically()

                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .fillMaxHeight(0.5f)
                                .padding(bottom = 64.dp), elevation = 24.dp
                        ) {
                            if (!selectedUserLocations.isNullOrEmpty()) {
                                val locationsMap = mutableMapOf<String, MutableList<UserLocation>>()
                                selectedUserLocations!!.forEach {
                                    val formattedDate = DateFormat.getDateInstance().format(it.timestamp!!)
                                    if (locationsMap[formattedDate].isNullOrEmpty()){
                                        locationsMap[formattedDate] = mutableListOf()
                                    }
                                    locationsMap[formattedDate]?.add(it)
                                }
                                val locationsList = locationsMap.toList()
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth(1.0f)
                                        .fillMaxHeight(1f),
                                    contentPadding = PaddingValues(
                                        horizontal = 16.dp, vertical = 16.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    state = lazyColumnState
                                ) {
                                    item {
                                        Text(
                                            text = selectedUser?.displayname ?: "",
                                            modifier = Modifier.padding(8.dp),
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily(Font(R.font.inter)),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    items(locationsList){ locationsByDay ->
                                        UserLocationDay(day = locationsByDay.first, locationsByDay.second){
                                            mapViewModel.animateZoomInCamera(it.latlng!!.toLatLng(), 14f)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is Response.Failure -> {
                Toast.makeText(LocalContext.current, "Failed To Load Company Members: ${companyUsers.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun UserLocationDay(day: String, locationList: MutableList<UserLocation>, onUserLocationClicked: (location: UserLocation) -> Unit) {
    var showUserLocations by remember {
        mutableStateOf(false)
    }
    val arrowAngle by animateFloatAsState(targetValue = if (!showUserLocations) 0f else -90f)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = rememberRipple(
                    bounded = true, color = colorResource(id = R.color.SecondaryBlueLight)
                ), onClick = {
                    showUserLocations = !showUserLocations
                }), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_calendar),
                contentDescription = "Location Icon",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
            Text(
                text = day, fontSize = 14.sp, fontWeight = FontWeight.Light, modifier = Modifier, fontFamily = FontFamily(
                    Font(R.font.inter)
                )
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_ico_dropdown_1),
                contentDescription = "Location Icon",
                modifier = Modifier.rotate(arrowAngle),
                tint = Color.LightGray
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        AnimatedVisibility(visible = showUserLocations) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(1.0f)
                    .height(160.dp),
                contentPadding = PaddingValues(
                    horizontal = 16.dp, vertical = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start,
                state = rememberLazyListState()
            ){
                items(locationList) { location ->
                    UserLocationRow(modifier = Modifier, loc = location) {
                        onUserLocationClicked(location)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.Center) {
            Divider(
                modifier = Modifier.fillMaxWidth(1f),
                color = colorResource(id = R.color.SecondaryBlueLight),
            )
        }
    }
}

fun getUserForMap(
    companyViewModel: CompanyViewModel,
    mapViewModel: MapViewModel,
    ctx: Context,
    users: List<User>?,
    userToNavigateTo: String?,
    onUserLocationsLoaded: (List<UserLocation>) -> Unit
): User? {
    var selectedUserLocations: List<UserLocation> = listOf()
    val user = users?.find { user ->
        user.displayname == userToNavigateTo
    }
    if (user != null) {
        companyViewModel.getUserLocationList(user).get().addOnSuccessListener { locations ->
                val tempList: MutableList<UserLocation> = mutableListOf()
                locations.forEach { loc ->
                    val location = loc.toObject(UserLocation::class.java)
                    tempList.add(location)
                }
                tempList.toList()
                tempList.sortWith { o1, o2 -> o1!!.timestamp!!.compareTo(o2!!.timestamp) }
                tempList.reverse()
                selectedUserLocations = tempList
            }.continueWith {
                onUserLocationsLoaded(selectedUserLocations)
                mapViewModel.addLocationsAndPan(ctx, selectedUserLocations)
            }
        return user
    } else {
        mapViewModel.showLastLocationMarkers(user, ctx)
        return null
    }
}









