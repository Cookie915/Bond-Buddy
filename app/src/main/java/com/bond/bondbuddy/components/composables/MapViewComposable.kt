package com.bond.bondbuddy.components.composables

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bond.bondbuddy.R
import com.bond.bondbuddy.models.User
import com.bond.bondbuddy.util.autocomplete.AutoCompleteBox
import com.bond.bondbuddy.util.autocomplete.AutoCompleteSearchBar
import com.bond.bondbuddy.util.autocomplete.UserAutoCompleteItem
import com.bond.bondbuddy.util.autocomplete.ValueAutoCompleteEntity
import com.bond.bondbuddy.viewmodels.CompanyViewModel
import com.bond.bondbuddy.viewmodels.MapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.maps.MapView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@ExperimentalPermissionsApi
@Composable
fun LocationsMap(
    mapView: MapView,
    mapViewModel: MapViewModel,
    companyViewModel: CompanyViewModel,
    userSelectedCb: (User) -> Unit,
    onMapInit: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val users by companyViewModel.companyMembers.collectAsState()
    MapPermissions {
        AndroidView(factory = {
            mapView
        }) { map ->
            coroutineScope.launch(Dispatchers.Main) {
                mapViewModel.initializeMap(
                    ctx,
                    map,
                    onUserInfoClick = { marker ->
                        Log.i("AdminPeopleScreen" ,"userInfoClickFrom Map")
                        val user = users.data?.find { user ->
                            user.id == marker.tag
                        }
                        if (user != null) {
                            userSelectedCb(user)
                        }
                    },
                ) {
                    onMapInit()
                }
            }
        }
    }
}

@Composable
fun MapSearchBar(items: List<ValueAutoCompleteEntity<User>>, onClick: (ValueAutoCompleteEntity<User>) -> Unit) {
    val focusManager = LocalFocusManager.current
    var searchName by remember {
        mutableStateOf("")
    }
    AutoCompleteBox(items = items, itemContent = { UserAutoCompleteItem(user = it.value) }) {
        onItemSelected { item ->
            onClick(item)
        }
        //Design Scope, Customize Box Size Shape Border Here
        boxWidthPercentage = 0.65f
        boxMaxHeight = 250.dp
        boxShape = RoundedCornerShape(0.dp, 0.dp, 5.dp, 5.dp)
        boxBorderStroke = BorderStroke(2.dp, colorResource(id = R.color.SecondaryBlueLight))
        textColor = Color.Black

        AutoCompleteSearchBar(modifier = Modifier.fillMaxWidth(0.8f),
                              value = searchName,
                              label = "Search Users",
                              onFocusChanged = { focusState ->
                                  isSearching = focusState.isFocused
                              },
                              onValueChanged = { query ->
                                  searchName = query
                                  filter(searchName)
                              },
                              onClearClick = {
                                  searchName = ""
                                  focusManager.clearFocus()
                              },
                              onDoneActionClick = {
                                  focusManager.clearFocus()
                              },
                              colors = TextFieldDefaults.outlinedTextFieldColors(
                                  unfocusedBorderColor = colorResource(id = R.color.SecondaryBlueLight),
                                  focusedBorderColor = colorResource(id = R.color.SecondaryBlue),
                                  textColor = colorResource(id = R.color.black),
                                  cursorColor = colorResource(id = R.color.UnfocusedIconGray),
                                  focusedLabelColor = colorResource(id = R.color.black),
                                  trailingIconColor = colorResource(id = R.color.black),
                                  leadingIconColor = colorResource(id = R.color.black)
                              ))
    }
}