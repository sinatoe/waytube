package com.waytube.app.common.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.waytube.app.R

@Composable
fun MoreOptionsMenu(
    actions: List<MenuAction>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { isExpanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.cd_more_options)
            )
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            for ((label, iconPainter, onClick) in actions) {
                DropdownMenuItem(
                    text = { Text(text = label) },
                    onClick = {
                        onClick()
                        isExpanded = false
                    },
                    leadingIcon = {
                        Icon(
                            painter = iconPainter,
                            contentDescription = null
                        )
                    },
                )
            }
        }
    }
}
