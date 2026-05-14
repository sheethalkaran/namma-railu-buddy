package com.nammarailu.buddy.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nammarailu.buddy.data.model.PlatformUpdate
import com.nammarailu.buddy.data.repository.FirebaseRepository
import com.nammarailu.buddy.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlatformUiState(
    val isLoading: Boolean = true,
    val currentUpdate: PlatformUpdate? = null,
    val selectedPlatform: Int = 0,
    // FIX 2: submitted = true only after THIS user clicks Confirm in this session.
    // It starts false so the grid is always interactive on first open.
    val submitted: Boolean = false,
    // FIX 2: isUpdating = true when user taps "Edit / Change Answer"
    val isUpdating: Boolean = false,
    val error: String? = null,
    val userPoints: Int = 0
)

@HiltViewModel
class PlatformViewModel @Inject constructor(
    private val repo: FirebaseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val trainId   = savedStateHandle.get<String>("trainId")   ?: ""
    private val stationId = savedStateHandle.get<String>("stationId") ?: ""

    private val _state = MutableStateFlow(PlatformUiState())
    val state: StateFlow<PlatformUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getPlatformUpdate(trainId, stationId).collect { result ->
                when (result) {
                    is Result.Loading -> _state.update { it.copy(isLoading = true) }
                    is Result.Success -> {
                        val update = result.data
                        val uid = repo.currentUid
                        
                        val hasVoted = update != null && uid != null && update.userVotes.containsKey(uid)

                        val preSelected = if (hasVoted) {
                            update!!.userVotes[uid] ?: update.platform_number
                        } else if (update != null && _state.value.selectedPlatform == 0) {
                            update.platform_number
                        } else {
                            _state.value.selectedPlatform
                        }

                        _state.update {
                            it.copy(
                                isLoading       = false,
                                currentUpdate   = update,
                                selectedPlatform = preSelected,
                                submitted       = if (hasVoted && !it.submitted) true else it.submitted
                            )
                        }
                    }
                    is Result.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
        viewModelScope.launch {
            repo.getUserProfile()
                .filterIsInstance<Result.Success<com.nammarailu.buddy.data.model.UserProfile?>>()
                .collect { r -> _state.update { it.copy(userPoints = r.data?.points ?: 0) } }
        }
    }

    fun selectPlatform(num: Int) = _state.update { it.copy(selectedPlatform = num) }

    fun submitPing() {
        val num = _state.value.selectedPlatform
        if (num == 0) return
        val existingUpdate = _state.value.currentUpdate
        viewModelScope.launch {
            repo.submitPlatformPing(trainId, stationId, num, existingUpdate)
            // FIX 2: submitted = true, isUpdating = false — shows success state with Edit button
            _state.update { it.copy(submitted = true, isUpdating = false) }
        }
    }

    /**
     * FIX 2: Enter update mode — grid becomes interactive again so user can change their answer.
     * The "Edit" / "Change Answer" button in the success state calls this.
     */
    fun enterUpdateMode() {
        _state.update { it.copy(isUpdating = true) }
    }

    /**
     * FIX 2: Cancel update — go back to showing the confirmed result without re-submitting.
     */
    fun cancelUpdate() {
        _state.update { it.copy(isUpdating = false) }
    }
}