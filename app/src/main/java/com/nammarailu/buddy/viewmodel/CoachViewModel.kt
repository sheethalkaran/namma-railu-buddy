package com.nammarailu.buddy.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nammarailu.buddy.data.model.CoachPosition
import com.nammarailu.buddy.data.repository.FirebaseRepository
import com.nammarailu.buddy.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoachUiState(
    val isLoading: Boolean = true,
    val coachPosition: CoachPosition? = null,
    val selectedCoachIndex: Int = -1,
    val error: String? = null
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val repo: FirebaseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val trainId = savedStateHandle.get<String>("trainId") ?: ""

    private val _state = MutableStateFlow(CoachUiState())
    val state: StateFlow<CoachUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getCoachPosition(trainId).collect { result ->
                when (result) {
                    is Result.Loading -> _state.update { it.copy(isLoading = true) }
                    is Result.Success -> _state.update { it.copy(isLoading = false, coachPosition = result.data) }
                    is Result.Error   -> _state.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun selectCoach(index: Int) = _state.update { it.copy(selectedCoachIndex = index) }
}
