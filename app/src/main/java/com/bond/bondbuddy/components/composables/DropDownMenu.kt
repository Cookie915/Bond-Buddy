package com.bond.bondbuddy.components.composables

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.bond.bondbuddy.R


@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalStdlibApi
@ExperimentalComposeUiApi
@Composable
fun Menu(
    menuItems: List<String>,
    menuExpandedState: Boolean,
    selectedIndex: Int,
    updateMenuExpandedState: () -> Unit,
    onDismissMenu: () -> Unit,
    onMenuItemClicked: (Int, String) -> Unit,
) {
    Box(modifier = Modifier
        .requiredHeight(24.dp)
        .requiredWidth(74.dp)
        .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp))
        .clickable {
            updateMenuExpandedState()
        }
    ) {
        ConstraintLayout(modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .padding(horizontal = 6.dp)) {
            val (label, icon) = createRefs()
            Text(
                text = menuItems[selectedIndex],
                modifier = Modifier
                    .constrainAs(label) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                    }
                    .offset(x = 12.dp)
            )
            val displayIcon: Painter = painterResource(R.drawable.ic_ico_dropdown_1)
            Icon(
                painter = displayIcon,
                contentDescription = "States Selection Menu",
                modifier = Modifier
                    .size(12.dp, 12.dp)
                    .aspectRatio(1f)
                    .constrainAs(icon) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end)
                    }
            )
            DropdownMenu(
                expanded = menuExpandedState,
                onDismissRequest = { onDismissMenu() }
            ) {
                menuItems.forEachIndexed { index, label ->
                    DropdownMenuItem(
                        onClick = {
                            if (index != 0){
                                val state = menuItems[index]
                                onMenuItemClicked(index, state)
                                onDismissMenu()
                            }
                        }
                    ) {
                        if (label == "State"){
                            Text(text = "Bounding State:")
                        } else {
                            Text(text = label)
                        }
                    }
                }
            }
        }
    }
}

//val (label, icon) = createRefs()
//Text(
//text= menuItems[selectedIndex],
//modifier = Modifier
//.constrainAs(label) {
//    top.linkTo(parent.top)
//    bottom.linkTo(parent.bottom)
//    start.linkTo(parent.start)
//    end.linkTo(icon.start)
//    width = Dimension.fillToConstraints
//}
//)
//
//val displayIcon: Painter = painterResource(R.drawable.ic_ico_dropdown_1)
//
//Icon(
//painter = displayIcon,
//contentDescription = null,
//modifier = Modifier
//.size(20.dp, 20.dp)
//.constrainAs(icon) {
//    end.linkTo(parent.end)
//    top.linkTo(parent.top)
//    bottom.linkTo(parent.bottom)
//},
//tint = MaterialTheme.colors.onSurface
//)
//
//DropdownMenu(
//expanded = menuExpandedState,
//onDismissRequest = { onDismissMenu() },
//modifier = Modifier
//.width(12.dp)
//.background(MaterialTheme.colors.surface)
//) {
//    menuItems.forEachIndexed { index, title ->
//        DropdownMenuItem(
//            onClick = {
//                if (index != 0){
//                    onMenuItemClicked(index)
//                }
//            }) {
//            Text(text = title)
//        }
//    }
//}









