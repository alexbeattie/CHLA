package com.chla.kindd.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.ui.theme.KiNDDIndigo

@Composable
fun HomeCompactHeader(
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.kindd_logo),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(148.dp)
                .height(42.dp)
                .testTag("home_compact_logo")
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = stringResource(R.string.home_parity_county),
            color = KiNDDIndigo,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(KiNDDIndigo.copy(alpha = 0.10f))
                .padding(horizontal = 10.dp, vertical = 7.dp)
        )

        Spacer(Modifier.width(6.dp))

        androidx.compose.foundation.layout.Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("home_header_overflow")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = stringResource(R.string.home_parity_more_actions),
                    tint = KiNDDIndigo,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_parity_change_preferences)) },
                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onEditProfile()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings_title)) },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onOpenSettings()
                    }
                )
            }
        }
    }
}
