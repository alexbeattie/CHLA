package com.navigator.kindd.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
internal data class KiNDDTopAppBarColorContract(
    val containerColor: Color,
    val titleContentColor: Color,
    val subtitleContentColor: Color,
    val actionIconContentColor: Color,
    val navigationIconContentColor: Color
)

@Composable
internal fun kinddTopAppBarColorContract(): KiNDDTopAppBarColorContract {
    val colorScheme = MaterialTheme.colorScheme
    return KiNDDTopAppBarColorContract(
        containerColor = colorScheme.primaryContainer,
        titleContentColor = colorScheme.onPrimaryContainer,
        subtitleContentColor = colorScheme.onPrimaryContainer,
        actionIconContentColor = colorScheme.onPrimaryContainer,
        navigationIconContentColor = colorScheme.onPrimaryContainer
    )
}
