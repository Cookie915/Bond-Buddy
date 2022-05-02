package com.bond.bondbuddy.screens.registration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.NavController
import com.bond.bondbuddy.R
import com.bond.bondbuddy.components.composables.CustomTextField
import com.bond.bondbuddy.models.Response
import com.bond.bondbuddy.util.autocomplete.*
import com.bond.bondbuddy.viewmodels.CompanyViewModel
import com.skydoves.landscapist.rememberDrawablePainter

@Composable
fun UserCompanyRegister(navController: NavController, companyViewModel: CompanyViewModel) {
    val context = LocalContext.current
    val companyListFlow by companyViewModel.companyList.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    Image(
        painter = rememberDrawablePainter(drawable = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_gradient, null)),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_appicon_foreground),
            contentDescription = "Sign In Logo",
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(1f),
            contentScale = ContentScale.FillBounds,
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (showDialog) {
            CompanyIDDialog {
                showDialog = false
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enter Company Name..",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(x = 18.dp)
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            when (companyListFlow) {
                is Response.Success -> {
                    val companies = companyListFlow.data?.asAutoCompleteEntities { name, query ->
                        name.toLowerCase(Locale.current).startsWith(query.toLowerCase(Locale.current))
                    } ?: listOf("None Found").asAutoCompleteEntities { _, _ -> return@asAutoCompleteEntities true }
                    Row(
                        modifier = Modifier, horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompanySearchBar(items = companies, onClick = {
                            companyViewModel.setUserCompany(context, it.value) {
                                navController.navigate("GraphFinder"){
                                    popUpTo("GraphFinder")
                                    launchSingleTop = true
                                }
                            }
                        })
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = {
                            showDialog = true
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_question_mark),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                is Response.Loading -> {
                    Column(
                        Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = colorResource(id = R.color.white), strokeWidth = 2.dp
                        )
                    }
                }
                is Response.Failure -> {
                    Column(
                        Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Failed To Fetch Companies From Server",
                            fontFamily = FontFamily(Font(R.font.inter)),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompanyIDField(modifier: Modifier, updateTextState: (String) -> Unit, onDone: () -> Unit) {
    val focusManager = LocalFocusManager.current
    CustomTextField(
        modifier = modifier,
        placeholder = "Company Name",
        initialValueState = "",
        placeholderColor = Color.White,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus()
            onDone()
        }),
        onValueChangeHelper = {
            updateTextState(it)
        },
        passwordField = false,
    )
}

@Composable
fun CompanyIDDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        modifier = Modifier,
        title = {
            Text(text = "Company Name")
        },
        text = {
            Text(text = "Company name given by bondsman, please contact your bondsman if you are unsure.")
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(text = "Dismiss", fontFamily = FontFamily(Font(R.font.inter)), color = colorResource(id = R.color.SecondaryBlueLight))
            }
        },
        backgroundColor = Color.White,
    )
}

@Composable
fun CompanySearchBar(items: List<ValueAutoCompleteEntity<String>>, onClick: (ValueAutoCompleteEntity<String>) -> Unit) {
    val focusManager = LocalFocusManager.current
    var searchName by remember {
        mutableStateOf("")
    }
    AutoCompleteBox(items = items, itemContent = { CompanyAutoCompleteItem(company = it.value) }) {
        onItemSelected { item ->
            onClick(item)
        }
        //Design Scope, Customize Box Size Shape Border Here
        boxWidthPercentage = 0.65f
        boxMaxHeight = 200.dp
        boxShape = RoundedCornerShape(0.dp, 0.dp, 5.dp, 5.dp)
        boxBorderStroke = BorderStroke(1.dp, colorResource(id = R.color.white))
        textColor = Color.Black
        shouldWrapContentHeight = true

        AutoCompleteSearchBar(modifier = Modifier.fillMaxWidth(0.8f),
                              value = searchName,
                              label = "Company Search",
                              onFocusChanged = {
                                               isSearching = it.hasFocus
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
                                  unfocusedBorderColor = colorResource(id = R.color.white),
                                  focusedBorderColor = colorResource(id = R.color.white),
                                  textColor = colorResource(id = R.color.black),
                                  cursorColor = colorResource(id = R.color.white),
                                  focusedLabelColor = colorResource(id = R.color.black),
                                  trailingIconColor = colorResource(id = R.color.black),
                                  leadingIconColor = colorResource(id = R.color.white)

                              )
        )
    }
}

@Composable
fun CompanyAutoCompleteItem(company: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp), horizontalArrangement = Arrangement.Center
    ) {
        Text(text = company)
    }
}