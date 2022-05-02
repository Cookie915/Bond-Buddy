package com.bond.bondbuddy.components.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bond.bondbuddy.R


@Composable
fun CustomTextField(modifier: Modifier = Modifier,
                    paddingLeadingIconEnd: Dp = 0.dp,
                    paddingTrailingIconStart: Dp = 0.dp,
                    placeholder: String,
                    placeholderColor: Color,
                    keyboardOptions: KeyboardOptions,
                    keyboardActions: KeyboardActions,
                    onValueChangeHelper: (String) -> Unit,
                    passwordField: Boolean,
                    initialValueState: String? = null) {
    var valueState by rememberSaveable {
        mutableStateOf("$initialValueState")
    }
    var showPassword by remember {
        mutableStateOf(false)
    }
    Row(
        modifier = modifier
            .border(BorderStroke(2.dp, color = Color.White),
                    shape = RoundedCornerShape(8.dp))
            .background(Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier
            .padding(start = paddingLeadingIconEnd, end = paddingTrailingIconStart)
            .weight(1f)) {
            TextField(value = valueState,
                      onValueChange = {
                          valueState = it; onValueChangeHelper(it)
                      },
                      colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent,
                                                                 focusedIndicatorColor = Color.Transparent,
                                                                 unfocusedIndicatorColor = Color.Transparent,
                                                                 disabledIndicatorColor = Color.Transparent,
                                                                 textColor = placeholderColor,
                                                                 cursorColor = Color.White),
                      singleLine = true,
                      keyboardOptions = keyboardOptions,
                      keyboardActions = keyboardActions,
                      visualTransformation = if (!showPassword && passwordField) PasswordVisualTransformation(
                          '*') else VisualTransformation.None)
            if (valueState.isEmpty()) {
                Text(text = placeholder,
                     modifier.align(Alignment.Center),
                     maxLines = 1,
                     color = placeholderColor.copy(0.6f))
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            if (passwordField) {
                IconButton(onClick = {
                    showPassword = !showPassword
                }, modifier = Modifier.offset(x = 10.dp)) {
                    Icon(painter = if (showPassword) painterResource(id = R.drawable.ic_visibility)
                    else painterResource(id = R.drawable.ic_visibilityoff),
                         contentDescription = "Show/Hide Password",
                         modifier = Modifier.padding(14.dp),
                         tint = placeholderColor)
                }
            }
            IconButton(
                onClick = {
                    valueState = ""
                },
            ) {
                Icon(imageVector = Icons.Filled.Clear,
                     contentDescription = "Clear Text",
                     tint = placeholderColor)
            }

        }
    }
}


@Preview
@Composable
fun CustomTextFieldPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        CustomTextField(placeholder = "TestField",
                        keyboardOptions = KeyboardOptions(),
                        keyboardActions = KeyboardActions { },
                        modifier = Modifier.fillMaxWidth(0.75f),
                        onValueChangeHelper = {},
                        passwordField = true,
                        placeholderColor = Color.White)

    }
}