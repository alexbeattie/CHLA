package com.chla.kindd.ui.map

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.ui.theme.KiNDDShapeTokens

private const val HOW_TO_TIP_PREFS = "kindd_howto"
private const val HAS_SEEN_HOW_TO_TIP = "has_seen_how_to_tip"

@Composable
internal fun HowToTipOverlay(
    profileComplete: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var visible by remember(profileComplete) {
        mutableStateOf(
            profileComplete &&
                !context.getSharedPreferences(HOW_TO_TIP_PREFS, Context.MODE_PRIVATE)
                    .getBoolean(HAS_SEEN_HOW_TO_TIP, false)
        )
    }
    if (!visible) return

    fun dismiss() {
        context.getSharedPreferences(HOW_TO_TIP_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(HAS_SEEN_HOW_TO_TIP, true)
            .apply()
        visible = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f))
            .clickable { dismiss() }
            .testTag("map_howto_tip")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 130.dp, start = 24.dp, end = 24.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(KiNDDShapeTokens.Selection)
                )
                .clickable(enabled = false) {}
                .padding(18.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_howto_tip_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_howto_tip_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { dismiss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("map_howto_tip_dismiss")
            ) {
                Text(stringResource(R.string.onboarding_howto_tip_dismiss))
            }
        }
    }
}
