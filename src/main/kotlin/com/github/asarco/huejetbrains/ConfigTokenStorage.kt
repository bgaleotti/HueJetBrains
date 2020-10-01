package com.github.asarco.huejetbrains

import com.github.asarco.huejetbrains.components.HuePersistentState
import inkapplications.shade.auth.TokenStorage
import org.jetbrains.rpc.LOG

class ConfigTokenStorage(private val state: HuePersistentState.HueState) : TokenStorage {

    override suspend fun getToken(): String? {
        return state.hueToken
    }

    override suspend fun setToken(token: String?) {
        if (token != null) {
            LOG.warn("Saved token $token")
            state.hueToken = token
        } else {
            LOG.warn("No token to save")
        }
    }
}
