package social.ahtara.websites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import social.ahtara.websites.data.WebsiteDao
import social.ahtara.websites.data.WebsiteDatabase
import social.ahtara.websites.data.WebsiteEntity

internal const val MAX_WEBSITES_PER_OWNER = 10

internal class WebsiteFeedsViewModel(
    private val dao: WebsiteDao,
    private val ownerId: String,
) : ViewModel() {

    val websites: StateFlow<List<WebsiteEntity>> =
        dao.websites(ownerId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    
    fun add(
        name: String, url: String, category: String?, latitude: Double, longitude: Double,
        onResult: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            if (dao.count(ownerId) >= MAX_WEBSITES_PER_OWNER) {
                onResult(false)
                return@launch
            }
            dao.insert(
                WebsiteEntity(
                    ownerId = ownerId, name = name.trim(), url = url, category = category,
                    latitude = latitude, longitude = longitude, createdAt = System.currentTimeMillis(),
                )
            )
            onResult(true)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { dao.delete(id, ownerId) }
    }

    companion object {
        fun factory(ownerId: String) = viewModelFactory {
            initializer {
                WebsiteFeedsViewModel(WebsiteDatabase.get(this[APPLICATION_KEY]!!).dao(), ownerId)
            }
        }
    }
}


internal fun normalizeWebsiteUrl(input: String): String? {
    val text = input.trim()
    if (text.isEmpty()) return null
    val withScheme = if ("://" in text) text else "https://$text"
    val uri = runCatching { java.net.URI(withScheme) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase()
    if ((scheme != "http" && scheme != "https") || uri.host.isNullOrBlank()) return null
    return withScheme
}