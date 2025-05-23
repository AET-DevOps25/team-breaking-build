package com.recipefy.gateway.service

import com.recipefy.gateway.client.KeyCloakClient
import com.recipefy.gateway.model.TokenResponse
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuthService(
    private val keyCloakClient: KeyCloakClient
) {
    fun getToken(
        username: String,
        password: String
    ): Mono<TokenResponse> = keyCloakClient.getToken(username, password)
}
