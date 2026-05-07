package com.waytube.app.common.ui.element

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.waytube.app.R

@Composable
fun RetryButton(onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_refresh),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )

        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))

        Text(text = stringResource(R.string.label_retry))
    }
}
