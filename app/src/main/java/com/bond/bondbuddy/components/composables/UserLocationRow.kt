package com.bond.bondbuddy.components.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.bond.bondbuddy.models.UserLocation
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.util.*

@Composable
fun UserLocationRow(modifier: Modifier, loc: UserLocation, onClick: () -> Unit) {
    val lat = BigDecimal(loc.latlng!!.latitude).setScale(4, RoundingMode.HALF_EVEN)
    val long = BigDecimal(loc.latlng!!.longitude.toString()).setScale(4, RoundingMode.HALF_EVEN)
    val label = "[$lat, $long]"
    Column {
        Row(
            modifier = modifier.clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = rememberRipple(
                    bounded = true, color = colorResource(id = R.color.SecondaryBlueLight)
                ), onClick = onClick
            ), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_location_blip),
                contentDescription = "Location Icon",
                modifier = Modifier.size(24.dp),
                tint = colorResource(id = R.color.SecondaryBlueLight)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label, fontSize = 14.sp, fontWeight = FontWeight.Light, modifier = Modifier, fontFamily = FontFamily(
                    Font(R.font.inter)
                )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val date = Date(loc.timestamp?.time ?: Date().time)
        val formattedDate = DateFormat.getDateTimeInstance().format(date)
        Row(horizontalArrangement = Arrangement.Center) {
            Text(
                text = formattedDate,
                color = Color.LightGray.copy(alpha = 0.9f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier,
                fontFamily = FontFamily(
                    Font(R.font.inter)
                )
            )
        }
        Row(horizontalArrangement = Arrangement.Center) {
            Divider(
                modifier = Modifier.fillMaxWidth(0.9f),
                color = colorResource(id = R.color.SecondaryBlueLight),
            )
        }
    }
}