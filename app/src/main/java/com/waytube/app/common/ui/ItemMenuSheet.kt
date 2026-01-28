package com.waytube.app.common.ui

import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.waytube.app.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemMenuSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onShare: (() -> Unit)? = null,
    onNavigateToChannel: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val items = listOfNotNull(
        onShare?.let {
            Triple(
                it,
                painterResource(R.drawable.ic_share),
                stringResource(R.string.label_share)
            )
        },
        onNavigateToChannel?.let {
            Triple(
                it,
                painterResource(R.drawable.ic_person),
                stringResource(R.string.label_go_to_channel)
            )
        }
    )

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        for ((onClick, iconPainter, label) in items) {
            ListItem(
                leadingContent = {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null
                    )
                },
                headlineContent = {
                    Text(text = label)
                },
                modifier = Modifier.clickable {
                    onClick()
                    scope.launch {
                        sheetState.hide()
                        onDismissRequest()
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        }
    }
}
