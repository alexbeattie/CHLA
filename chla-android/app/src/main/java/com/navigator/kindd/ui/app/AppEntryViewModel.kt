package com.navigator.kindd.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navigator.kindd.data.profile.UserProfile
import com.navigator.kindd.data.profile.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AppEntryViewModel @Inject constructor(
    repository: UserProfileRepository
) : ViewModel() {

    val state: StateFlow<AppEntryState> = repository.profile
        .map<UserProfile, AppEntryState> { profile ->
            if (profile.isComplete) {
                AppEntryState.Ready(profile)
            } else {
                AppEntryState.NeedsOnboarding(profile)
            }
        }
        .catch {
            emit(AppEntryState.NeedsOnboarding(UserProfile()))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppEntryState.Loading
        )
}
