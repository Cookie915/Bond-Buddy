package com.bond.bondbuddy.util.autocomplete

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bond.bondbuddy.R
import com.bond.bondbuddy.models.User


@Composable
fun <T : AutoCompleteEntity> AutoCompleteBox(items: List<T>, itemContent: @Composable (T) -> Unit, content: @Composable AutoCompleteScope<T>.() -> Unit) {
    val autoCompleteState = remember {
        AutoCompleteState(startItems = items)
    }
    Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        autoCompleteState.content()
        AnimatedVisibility(visible = autoCompleteState.isSearching) {
            LazyColumn(modifier = Modifier.autoComplete(autoCompleteState), horizontalAlignment = Alignment.CenterHorizontally) {
                items(autoCompleteState.filteredItems) { item ->
                    Box(modifier = Modifier.clickable {
                        autoCompleteState.selectItem(item)
                    }) {
                        //autocomplete form invoked with item here
                        itemContent(item)
                    }
                }
            }
        }
    }
}

//how individual user autocomplete forms should look
@Composable
fun UserAutoCompleteItem(user: User) {
    val first: String?
    var last: String? = null
    val firstLast = user.displayname!!.split(" ")
    if (firstLast.size > 1){
        first = firstLast[0]
        last = firstLast[1]
    } else {
        first = firstLast[0]
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp), horizontalArrangement = Arrangement.Center
    ) {
        Text(text = first)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = last ?: "")
    }
}


//The Search Bar for AutoCompleteBox
@Composable
fun AutoCompleteSearchBar(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    colors: TextFieldColors,
    onDoneActionClick: () -> Unit = {},
    onClearClick: () -> Unit = {},
    onFocusChanged: (FocusState) -> Unit = {},
    onValueChanged: (String) -> Unit
) {
    OutlinedTextField(
        modifier = modifier.onFocusChanged {
                onFocusChanged(it)
            },
        value = value,
        onValueChange = { query ->
            onValueChanged(query)
        },
        label = {
            Text(text = label)
        },
        textStyle = MaterialTheme.typography.subtitle1,
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { onClearClick() }) {
                Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear")
            }
        },
        keyboardActions = KeyboardActions(onDone = { onDoneActionClick() }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Text),
        shape = RoundedCornerShape(25.dp),
        leadingIcon = {
            Icon(
                modifier = Modifier
                    .requiredSize(24.dp)
                    .aspectRatio(1f)
                    .offset(x = 6.dp),
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = "Search Icon",
                tint = colorResource(id = R.color.black)
            )
        },
        colors = colors
    )
}




