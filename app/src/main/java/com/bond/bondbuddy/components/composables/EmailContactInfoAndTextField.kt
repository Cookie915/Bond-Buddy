package com.bond.bondbuddy.components.composables

import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bond.bondbuddy.R
import com.bond.bondbuddy.components.isValidEmail
import com.bond.bondbuddy.viewmodels.UserViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi

@Composable
fun EmailContactInfo(modifier: Modifier, email: String?, callback: () -> Unit) {
    Row(modifier.height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        if (email.isNullOrBlank()) {
            Text(text = "No Email Found",
                 fontFamily = FontFamily(
                     Font(R.font.inter)
                 ),
                 fontSize = 15.sp
            )
        } else {
            Text(text = email,
                 fontFamily = FontFamily(
                     Font(R.font.inter)
                 ),
                 fontSize = 14.sp,
                 color = colorResource(id = R.color.UnfocusedIconGray))
        }
        IconButton(
            modifier = Modifier
                .size(48.dp),
            onClick = {
                callback()
            },
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(id = R.drawable.ic_edit_3),
                contentDescription = "Edit Contact Number",
                tint = Color.Unspecified)
        }
    }
}


@ExperimentalPermissionsApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalStdlibApi
@ExperimentalComposeUiApi
@Composable
fun EmailContactTextField(modifier: Modifier, userViewModel: UserViewModel, onDone: () -> Unit) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var textState by remember {
        mutableStateOf("")
    }
    focusManager.moveFocus(FocusDirection.In)
    Row(modifier.height(56.dp)) {
        TextField(value = textState,
                  onValueChange = {
                      textState = it
                  },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email,
                                                    imeAction = ImeAction.Done),
                  keyboardActions = KeyboardActions(onDone = {
                      if (textState.isValidEmail()) {
                          focusManager.clearFocus()
                          userViewModel.setContactEmail(textState)
                          onDone()
                      } else {
                          Toast.makeText(context, "Invalid Email", Toast.LENGTH_SHORT).show()
                      }
                  }),
                  placeholder = {
                      Text(text = "example@domain.com", fontFamily = FontFamily(Font(R.font.inter)))
                  },
                  colors = TextFieldDefaults.textFieldColors(
                      focusedIndicatorColor = colorResource(
                          id = R.color.SecondaryBlue
                      ),
                      backgroundColor = Color.Transparent,
                      cursorColor = colorResource(id = R.color.SecondaryBlueLight)
                  )
        )
    }
}