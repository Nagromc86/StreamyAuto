package com.streamyauto.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamyauto.StreamyApp
import com.streamyauto.data.model.Account
import com.streamyauto.data.model.Channel
import com.streamyauto.data.model.Favorite
import com.streamyauto.data.xtream.XtreamCategory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ce qui est en cours de lecture sur le téléphone. */
data class PlayItem(val name: String, val url: String, val icon: String?)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as StreamyApp).repository

    val accounts: StateFlow<List<Account>> =
        repo.accounts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val favorites: StateFlow<List<Favorite>> =
        repo.favorites.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var selectedAccount by mutableStateOf<Account?>(null)
        private set
    var categories by mutableStateOf<List<XtreamCategory>>(emptyList())
        private set
    var openedCategory by mutableStateOf<XtreamCategory?>(null)
        private set
    var channels by mutableStateOf<List<Channel>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var playing by mutableStateOf<PlayItem?>(null)
        private set

    init {
        // Sélectionne automatiquement le premier compte disponible
        viewModelScope.launch {
            accounts.collect { list ->
                val current = selectedAccount
                if (current == null || list.none { it.id == current.id }) {
                    val first = list.firstOrNull()
                    if (first != null) selectAccount(first) else clearBrowse()
                }
            }
        }
    }

    // ---------- Comptes ----------

    suspend fun checkAccount(account: Account): String? = repo.checkAccount(account)

    fun saveAccount(account: Account, onDone: () -> Unit) {
        viewModelScope.launch {
            val id = repo.saveAccount(account)
            if (selectedAccount == null) selectAccount(account.copy(id = id))
            onDone()
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repo.deleteAccount(account)
            if (selectedAccount?.id == account.id) {
                selectedAccount = null
                clearBrowse()
            }
        }
    }

    // ---------- Navigation chaînes ----------

    fun selectAccount(account: Account) {
        selectedAccount = account
        openedCategory = null
        channels = emptyList()
        load { categories = listOf(ALL_CATEGORY) + repo.categories(account) }
    }

    fun openCategory(category: XtreamCategory) {
        val account = selectedAccount ?: return
        openedCategory = category
        channels = emptyList()
        val id = if (category === ALL_CATEGORY) null else category.category_id
        load { channels = repo.channels(account, id) }
    }

    fun closeCategory() {
        openedCategory = null
        channels = emptyList()
        error = null
    }

    fun retry() {
        val account = selectedAccount ?: return
        val category = openedCategory
        if (category != null) openCategory(category) else selectAccount(account)
    }

    private fun clearBrowse() {
        categories = emptyList()
        channels = emptyList()
        openedCategory = null
    }

    private fun load(block: suspend () -> Unit) {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                block()
            } catch (e: Exception) {
                error = "Erreur de chargement : ${e.message ?: e.javaClass.simpleName}"
            } finally {
                loading = false
            }
        }
    }

    // ---------- Favoris ----------

    fun isFavorite(channel: Channel, favs: List<Favorite>): Boolean =
        favs.any { it.accountId == channel.accountId && it.streamId == channel.streamId }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch { repo.toggleFavorite(channel) }
    }

    fun removeFavorite(favorite: Favorite) {
        viewModelScope.launch { repo.removeFavorite(favorite) }
    }

    fun moveFavorite(favorite: Favorite, delta: Int) {
        viewModelScope.launch { repo.moveFavorite(favorite, delta) }
    }

    // ---------- Lecture ----------

    fun play(item: PlayItem) {
        playing = item
    }

    fun stopPlaying() {
        playing = null
    }

    companion object {
        val ALL_CATEGORY = XtreamCategory(category_id = "__all__", category_name = "Toutes les chaînes")
    }
}
