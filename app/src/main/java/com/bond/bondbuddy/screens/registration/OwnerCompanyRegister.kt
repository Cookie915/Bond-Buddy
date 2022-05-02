package com.bond.bondbuddy.screens.registration

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.NavController
import com.bond.bondbuddy.R
import com.bond.bondbuddy.viewmodels.CompanyViewModel
import com.skydoves.landscapist.rememberDrawablePainter

@Composable
fun OwnerCompanyRegister(navController: NavController, companyViewModel: CompanyViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    var textFieldState by remember { mutableStateOf("") }
    val context = LocalContext.current
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
                .fillMaxWidth(0.7f)
                .fillMaxHeight(0.4f)
                .aspectRatio(1f),
            contentScale = ContentScale.FillBounds,
        )
        Spacer(modifier = Modifier.height(20.dp))
        if (showDialog) {
            AlertDialog(onDismissRequest = {
                showDialog = false
            }, buttons = {
                Row(modifier = Modifier
                    .fillMaxWidth(.95f)
                    .padding(8.dp), Arrangement.End) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = colorResource(id = R.color.SecondaryBlue),
                            contentColor = Color.White
                        ),
                        onClick = {
                        showDialog = false
                    }

                    ) {
                        Text(text = "Ok")
                    }
                }
            }, modifier = Modifier, title = {
                Text(text = "Company Name")
            }, text = {
                Column {
                    Text(text = "Name of company, users search this name to join.")
                }
            }, shape = RoundedCornerShape(16.dp))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Name your company..", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(x = 18.dp)
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                modifier = Modifier, horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically
            ) {
                CompanyIDField(modifier = Modifier.fillMaxWidth(0.8f), updateTextState = { textFieldState = it }, onDone = {
                    companyViewModel.createCompany(textFieldState, context) {
                        navController.navigate(("GraphFinder")){
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
                        painter = painterResource(id = R.drawable.ic_question_mark), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
