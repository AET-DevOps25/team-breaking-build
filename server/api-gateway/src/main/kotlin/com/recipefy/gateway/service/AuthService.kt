package com.recipefy.gateway.service

import com.recipefy.gateway.client.KeyCloakClient
import com.recipefy.gateway.model.TokenRequest
import com.recipefy.gateway.model.TokenResponse
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuthService(
    private val keyCloakClient: KeyCloakClient
) {
    fun getToken(tokenRequest: TokenRequest): Mono<TokenResponse> = keyCloakClient.getToken(tokenRequest)
}
