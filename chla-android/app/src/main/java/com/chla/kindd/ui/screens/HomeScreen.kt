package com.chla.kindd.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chla.kindd.R
import com.chla.kindd.data.discovery.TherapyType
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.ui.chat.ChatLaunchPrompt
import com.chla.kindd.ui.home.HomeEvent
import com.chla.kindd.ui.home.HomeLookupState
import com.chla.kindd.ui.home.HomeMessage
import com.chla.kindd.ui.home.HomeUiState
import com.chla.kindd.ui.home.HomeViewModel

@Composable
fun HomeScreen(
    profile: UserProfile,
    onNavigateToMap: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onNavigateToRegionalCenters: () -> Unit,
    onNavigateToChat: (ChatLaunchPrompt?) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel, profile) {
        viewModel.onReadyProfileChanged(profile)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                HomeEvent.NavigateToMap -> onNavigateToMap()
                HomeEvent.NavigateToList -> onNavigateToProviders()
                HomeEvent.NavigateToRegionalCenters -> onNavigateToRegionalCenters()
                is HomeEvent.NavigateToChat -> onNavigateToChat(event.prompt)
                is HomeEvent.Dial -> context.startActivity(
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${event.digits}"))
                )
            }
        }
    }

    HomeContent(
        profile = profile,
        uiState = uiState,
        onZipChanged = viewModel::onZipChanged,
        onSubmitZip = {
            viewModel.submitZip(
                expectedProfile = profile,
                displayedZip = uiState.displayedZip(profile)
            )
        },
        onNavigateToMap = viewModel::openMap,
        onNavigateToList = viewModel::openList,
        onNavigateToRegionalCenters = viewModel::openRegionalCenters,
        onNavigateToChat = viewModel::openChat,
        onOpenChat = { onNavigateToChat(null) },
        onTherapySelected = viewModel::selectTherapy,
        onCall = { digits -> viewModel.callCenter(profile, digits) }
    )
}

@Composable
fun HomeContent(
    profile: UserProfile,
    uiState: HomeUiState,
    onZipChanged: (String) -> Unit,
    onSubmitZip: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToRegionalCenters: () -> Unit,
    onNavigateToChat: (ChatLaunchPrompt) -> Unit,
    onOpenChat: () -> Unit,
    onTherapySelected: (TherapyType) -> Unit,
    onCall: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_brand),
                    modifier = Modifier
                        .testTag("home_title")
                        .semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.home_intro),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        if (profile.regionalCenter == null) {
            ZipLookupCard(profile, uiState, onZipChanged, onSubmitZip)
        } else {
            RegionalCenterCard(profile, uiState, onNavigateToRegionalCenters, onCall)
        }

        HomeSection(title = stringResource(R.string.home_discover_services)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onNavigateToMap,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("home_map_action")
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_map))
                }
                OutlinedButton(
                    onClick = onNavigateToList,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("home_list_action")
                ) {
                    Icon(Icons.Default.List, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_list))
                }
            }
        }

        HomeSection(title = stringResource(R.string.home_therapy_shortcuts)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TherapyRow(
                    TherapyType.ABA to R.string.aba_therapy,
                    TherapyType.SPEECH to R.string.home_therapy_speech,
                    onTherapySelected = onTherapySelected
                )
                TherapyRow(
                    TherapyType.OCCUPATIONAL to R.string.home_therapy_occupational,
                    TherapyType.PHYSICAL to R.string.home_therapy_physical,
                    onTherapySelected = onTherapySelected
                )
            }
        }

        HomeActionCard(
            icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
            title = stringResource(R.string.regional_centers),
            body = stringResource(R.string.home_regional_centers_body),
            action = stringResource(R.string.home_explore),
            onClick = onNavigateToRegionalCenters
        )

        HomeActionCard(
            icon = { Icon(Icons.Default.Chat, contentDescription = null) },
            title = stringResource(R.string.home_ask_kindd),
            body = stringResource(R.string.home_ask_kindd_body),
            action = stringResource(R.string.home_ask_kindd),
            onClick = onOpenChat
        )

        JourneyCard(profile, uiState, onNavigateToChat, onCall)
    }
}

@Composable
private fun ZipLookupCard(
    profile: UserProfile,
    uiState: HomeUiState,
    onZipChanged: (String) -> Unit,
    onSubmitZip: () -> Unit
) {
    Card(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.home_who_serves),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(stringResource(R.string.home_zip_explanation))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.displayedZip(profile),
                    onValueChange = onZipChanged,
                    label = { Text(stringResource(R.string.onboarding_zip_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = { onSubmitZip() }),
                    modifier = Modifier.weight(1f).testTag("home_zip_input")
                )
                Button(
                    onClick = onSubmitZip,
                    enabled = uiState.displayedZip(profile).matches(Regex("[0-9]{5}")) &&
                        uiState.lookupState != HomeLookupState.LOADING,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    if (uiState.lookupState == HomeLookupState.LOADING) {
                        val loadingDescription = stringResource(R.string.loading)
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(20.dp)
                                .testTag("home_zip_lookup_loading")
                                .semantics {
                                    contentDescription = loadingDescription
                                    liveRegion = LiveRegionMode.Polite
                                }
                        )
                    } else {
                        Text(stringResource(R.string.home_find))
                    }
                }
            }
            uiState.message?.let { message ->
                Text(
                    text = stringResource(
                        when (message) {
                            HomeMessage.INVALID_ZIP -> R.string.home_invalid_zip
                            HomeMessage.NO_MATCH -> R.string.home_no_match
                            HomeMessage.LOOKUP_UNAVAILABLE -> R.string.home_lookup_unavailable
                        }
                    ),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .testTag("home_zip_lookup_message")
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }
    }
}

@Composable
private fun RegionalCenterCard(
    profile: UserProfile,
    uiState: HomeUiState,
    onDetails: () -> Unit,
    onCall: (String) -> Unit
) {
    val identity = profile.regionalCenter ?: return
    val hydratedCenter = uiState.centerDetailsFor(profile)
    Card(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.home_your_regional_center),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium
            )
            Text(stringResource(R.string.home_matched), color = MaterialTheme.colorScheme.primary)
            Text(identity.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(identity.shortName, style = MaterialTheme.typography.labelLarge)
            hydratedCenter?.formattedPhone?.takeIf(String::isNotBlank)?.let { Text(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.dialDigitsFor(profile)?.let { digits ->
                    OutlinedButton(
                        onClick = { onCall(digits) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.home_call_now))
                    }
                }
                Button(onClick = onDetails, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.home_details))
                }
            }
        }
    }
}

@Composable
private fun HomeSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

@Composable
private fun TherapyRow(
    first: Pair<TherapyType, Int>,
    second: Pair<TherapyType, Int>,
    onTherapySelected: (TherapyType) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(first, second).forEach { (therapy, label) ->
            OutlinedButton(
                onClick = { onTherapySelected(therapy) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
private fun HomeActionCard(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    action: String,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(body)
            OutlinedButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(action)
            }
        }
    }
}

@Composable
private fun JourneyCard(
    profile: UserProfile,
    uiState: HomeUiState,
    onChat: (ChatLaunchPrompt) -> Unit,
    onCall: (String) -> Unit
) {
    val prompt = profile.journeyStage.toLaunchPrompt() ?: return
    val (titleRes, actionRes) = when (profile.journeyStage) {
        JourneyStage.JUST_DIAGNOSED ->
            R.string.home_journey_just_diagnosed_title to R.string.home_journey_just_diagnosed_action
        JourneyStage.WAITING_FOR_INTAKE ->
            R.string.home_journey_waiting_title to R.string.home_journey_waiting_action
        JourneyStage.RECEIVING_SERVICES ->
            R.string.home_journey_receiving_title to R.string.home_journey_receiving_action
        JourneyStage.EXPLORING, null -> return
    }
    Card(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.home_your_next_step), style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(titleRes),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onChat(prompt) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(actionRes))
                }
                if (profile.journeyStage == JourneyStage.JUST_DIAGNOSED) {
                    uiState.dialDigitsFor(profile)?.let { digits ->
                        OutlinedButton(
                            onClick = { onCall(digits) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text(stringResource(R.string.home_call_now)) }
                    }
                }
            }
        }
    }
}

private fun JourneyStage?.toLaunchPrompt(): ChatLaunchPrompt? = when (this) {
    JourneyStage.JUST_DIAGNOSED -> ChatLaunchPrompt.JUST_DIAGNOSED
    JourneyStage.WAITING_FOR_INTAKE -> ChatLaunchPrompt.WAITING_INTAKE
    JourneyStage.RECEIVING_SERVICES -> ChatLaunchPrompt.RECEIVING_SERVICES
    JourneyStage.EXPLORING, null -> null
}
