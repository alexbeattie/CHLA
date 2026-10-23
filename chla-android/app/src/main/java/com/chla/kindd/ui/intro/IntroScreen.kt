package com.chla.kindd.ui.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.ui.theme.KiNDDPrimaryGradientCapsule
import com.chla.kindd.ui.theme.KiNDDSpacingTokens
import com.chla.kindd.ui.theme.kinddTopWash

/** The intro reopened outside onboarding, from Settings or About. */
@Composable
fun IntroScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("intro_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(kinddTopWash())
        )
        Column(modifier = Modifier.fillMaxSize()) {
            IntroContent(
                modifier = Modifier
                    .weight(1f)
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = KiNDDSpacingTokens.PageInset, vertical = 24.dp)
            )
            KiNDDPrimaryGradientCapsule(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .testTag("intro_done")
            ) {
                Text(
                    text = stringResource(R.string.intro_done),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
