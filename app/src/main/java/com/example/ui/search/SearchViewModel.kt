package com.example.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Song
import com.example.domain.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val query: String, val songs: List<Song>) : SearchUiState
    data class Empty(val query: String) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel(
    private val repository: MusicRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Recent search history (maximum 10 searches, auto-trimmed in database)
    val searchHistory: StateFlow<List<String>> = repository.getSearchHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        // If the user clears the input while in success/error/empty state, return to Idle (history)
        if (newQuery.isBlank() && _uiState.value !is SearchUiState.Idle) {
            _uiState.value = SearchUiState.Idle
        }
    }

    /**
     * Executes the song search ONLY when the user explicitly triggers it
     * by pressing Enter (IME Search action), tapping Search icon, or clicking a history item.
     * Searches are only saved to history at this exact moment.
     */
    fun performSearch(queryToSearch: String? = null) {
        val targetQuery = (queryToSearch ?: _query.value).trim()
        if (targetQuery.isBlank()) return

        _query.value = targetQuery
        _uiState.value = SearchUiState.Loading

        viewModelScope.launch {
            // Save to search history (max 10, oldest deleted automatically)
            repository.saveSearchQuery(targetQuery)

            val result = repository.searchSongs(targetQuery, limit = 40)
            result.onSuccess { songs ->
                if (songs.isEmpty()) {
                    _uiState.value = SearchUiState.Empty(targetQuery)
                } else {
                    _uiState.value = SearchUiState.Success(targetQuery, songs)
                }
            }.onFailure { error ->
                _uiState.value = SearchUiState.Error(
                    error.localizedMessage ?: "Failed to find songs. Please check your network and try again."
                )
            }
        }
    }

    fun onHistoryItemClicked(historyQuery: String) {
        _query.value = historyQuery
        performSearch(historyQuery)
    }

    fun onDeleteHistoryItem(historyQuery: String) {
        viewModelScope.launch {
            repository.deleteSearchQuery(historyQuery)
        }
    }

    fun onClearAllHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    fun clearQuery() {
        _query.value = ""
        _uiState.value = SearchUiState.Idle
    }

     fun playSongWithRadio(
        song: Song,
        playerManager: PlayerManager,
        fallbackPlaylist: List<Song>,
        playlistName: String
    ) {
        playerManager.playSong(song = song, playlist = fallbackPlaylist, playlistName = playlistName)
        viewModelScope.launch {
            repository.getRadioQueue(song.id).onSuccess { radioSongs ->
                if (radioSongs.isNotEmpty()) {
                    val fullQueue = listOf(song) + radioSongs.filterNot { it.id == song.id }
                    playerManager.replaceUpcomingQueue(currentSong = song, newQueue = fullQueue)
                }
            }
        }
     }
     
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val newLiked = repository.toggleFavorite(song)
            val currentState = _uiState.value
            if (currentState is SearchUiState.Success) {
                val updatedSongs = currentState.songs.map {
                    if (it.id == song.id) it.copy(isLiked = newLiked) else it
                }
                _uiState.value = currentState.copy(songs = updatedSongs)
            }
        }
    }
}
