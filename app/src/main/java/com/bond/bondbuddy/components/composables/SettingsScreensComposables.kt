package com.bond.bondbuddy.components.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bond.bondbuddy.R

@Composable
fun SettingsScreenDivider(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(37.dp)
            .background(colorResource(id = R.color.DividerGray))
            .border(width = 1.dp, color = colorResource(id = R.color.DividerStrokeGray))
    ) {
        Text(
            text = label, color = colorResource(id = R.color.DividerLabelGray).copy(alpha = 0.4f), modifier = Modifier
                .align(
                    Alignment.CenterStart
                )
                .padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun SettingsRow(icon: Int, label: String) {
    Box(
        modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = "$label Icon",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(15.dp))
            Text(
                text = label, fontFamily = FontFamily(
                    Font(R.font.inter)
                ), fontWeight = FontWeight.Medium, fontSize = 15.sp
            )
            Row(
                Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ico_dropdown),
                    contentDescription = "Arrow",
                    modifier = Modifier.size(9.dp),
                    tint = colorResource(
                        id = R.color.UnfocusedIconGray
                    )
                )
            }
        }
    }
}

//  Settings text, Logout Button, Account Box
@Composable
fun TopColumn(onLogoutClicked: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
        ) {
            Text(
                text = "Settings",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp),
                color = Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(
                    Font(R.font.inter)
                )
            )
            TextButton(modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp), onClick = { onLogoutClicked() }) {
                Text(
                    text = "Logout",
                    color = colorResource(id = R.color.LogOutButtonRed),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(
                        Font(R.font.inter)
                    )
                )
            }
        }
        SettingsScreenDivider(label = "Account")
    }
}

@Composable
fun ContactColumn(onCallClicked: () -> Unit, onEmailClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .height(120.dp)
            .padding(12.dp), verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable {
                    onCallClicked()
                }, verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsRow(icon = R.drawable.ic_phoneiconblack, label = "Call Bondsman")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable {
                onEmailClicked()
            }, verticalAlignment = Alignment.CenterVertically) {
            SettingsRow(icon = R.drawable.ic_emailiconblack, label = "Email Bondsman")
        }
    }
}

//  Privacy Policy, TOS, Report A Bug
@Composable
fun HelpColumn(
    onPrivacyClicked: () -> Unit, onBugButtonClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .height(120.dp)
            .padding(12.dp), verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable {
                    onPrivacyClicked()
                }, verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsRow(icon = R.drawable.ic_shield_with_lock, label = "Privacy Policy")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable {
                    onBugButtonClicked()
                }, verticalAlignment = Alignment.CenterVertically) {
            SettingsRow(icon = R.drawable.ic_warning, label = "Report A Bug")
        }
    }
}

//  Account Column
@Composable
fun AccountColumn(
    onChangePasswordClick: () -> Unit, onDeletedAccountClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .height(120.dp)
            .padding(12.dp), verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable {
                    onChangePasswordClick()
                }, verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsRow(icon = R.drawable.ic_key, label = "Reset Password")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable {
                onDeletedAccountClicked()
            }, verticalAlignment = Alignment.CenterVertically) {
            SettingsRow(icon = R.drawable.ic_trash, label = "Delete Account")
        }
    }
}