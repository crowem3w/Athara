package org.example.core.secure

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.example.core.net.CoreJson

/** Access/refresh tokens, encrypted at rest. Thread-safe; [signedIn] flips to false when tokens are cleared. */
class TokenStore(private val secure: SecureStore) {

    @Serializable
    data class Tokens(val access: String, val refresh: String, val userId: String)

    @Volatile private var cache: Tokens? = load()
    private val _signedIn = MutableStateFlow(cache != null)
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    val accessToken: String? get() = cache?.access
    val refreshToken: String? get() = cache?.refresh
    val userId: String? get() = cache?.userId

    @Synchronized
    fun save(tokens: Tokens) {
        secure.putString(KEY, CoreJson.encodeToString(tokens))
        cache = tokens
        _signedIn.value = true
    }

    @Synchronized
    fun clear() {
        secure.remove(KEY)
        cache = null
        _signedIn.value = false
    }

    private fun load(): Tokens? = secure.getString(KEY)?.let {
        runCatching { CoreJson.decodeFromString<Tokens>(it) }.getOrNull()
    }

    private companion object {
        const val KEY = "tokens"
    }
}
