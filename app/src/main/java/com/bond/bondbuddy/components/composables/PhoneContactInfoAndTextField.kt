package com.bond.bondbuddy.components.composables


import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bond.bondbuddy.R
import com.bond.bondbuddy.util.NumberVisualTransformation
import com.bond.bondbuddy.viewmodels.UserViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.michaelrocks.libphonenumber.android.Phonenumber

@Composable
fun PhoneContactInfo(modifier: Modifier, number: String?, callback: () -> Unit) {
    val phoneNumberUtil = PhoneNumberUtil.createInstance(LocalContext.current)
    var phoneNumber: Phonenumber.PhoneNumber = Phonenumber.PhoneNumber()
    if (!number.isNullOrBlank()) {
        try {
            phoneNumber = phoneNumberUtil.parse(number, "US")
        } catch (e: NumberParseException) {
            Toast.makeText(LocalContext.current, e.localizedMessage, Toast.LENGTH_SHORT).show()
        } catch (e: NumberFormatException) {
            Toast.makeText(LocalContext.current, e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(6.dp))
        if (number.isNullOrBlank()){
            Text(text = "No Number Found", fontFamily = FontFamily(Font(R.font.inter)), fontSize = 14.sp, color = colorResource(id = R.color.UnfocusedIconGray))
        } else {
            Text(
                text = phoneNumberUtil.format(
                    phoneNumber,
                    PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
                ), fontFamily = FontFamily(
                    Font(R.font.inter)
                ), color = colorResource(id = R.color.UnfocusedIconGray),
                fontSize = 14.sp
            )
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
                tint = Color.Unspecified
            )
        }
    }
}

@ExperimentalComposeUiApi
@ExperimentalPermissionsApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalStdlibApi
@Composable
fun PhoneContactTextField(modifier: Modifier, userViewModel: UserViewModel, onDone: () -> Unit) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val phoneNumberUtil = PhoneNumberUtil.createInstance(context)
    val textState = remember {
        mutableStateOf(
            TextFieldValue(
                text = "", selection = TextRange(2)
            )
        )
    }
    val maxChar = 10
    Row(modifier.height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        TextField(value = textState.value,
                  onValueChange = {
                      if (it.text.length <= maxChar) {
                          textState.value = it
                      }
                  },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(
                      keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done
                  ),
                  keyboardActions = KeyboardActions(onDone = {
                      try {
                          val phonenumber = phoneNumberUtil.parse(textState.value.text, "US")
                          if (phoneNumberUtil.isValidNumber(phonenumber)) {
                              userViewModel.setContactNumber(textState.value.text, context)
                              focusManager.clearFocus()
                              onDone()
                          } else {
                              Toast.makeText(context, "Invalid Number", Toast.LENGTH_SHORT).show()
                              textState.value = TextFieldValue(
                                  "", selection = TextRange.Zero
                              )
                          }
                      } catch (e: NumberParseException) {
                          Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                      }
                  }),
                  visualTransformation = NumberVisualTransformation(),
                  colors = TextFieldDefaults.textFieldColors(
                      focusedIndicatorColor = colorResource(
                          id = R.color.SecondaryBlue
                      ),
                      backgroundColor = Color.Transparent,
                      cursorColor = colorResource(id = R.color.SecondaryBlueLight)
                  ),
                  placeholder = {
                      Text(text = "+1 *** - *** - ****")
                  },
        )
    }
}