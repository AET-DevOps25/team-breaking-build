package com.recipefy.gateway.client

import com.recipefy.gateway.config.KeyCloakConfigurationProperties
import com.recipefy.gateway.model.TokenRequest
import com.recipefy.gateway.model.TokenResponse
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Component
@EnableConfigurationProperties(KeyCloakConfigurationProperties::class)
class KeyCloakClient(
    private val keyCloakClient: WebClient,
    private val keyCloakProperties: KeyCloakConfigurationProperties
) {
    companion object {
        private const val GRANT_TYPE_PASSWORD = "password"
        private const val GRANT_TYPE_REFRESH_TOKEN = "refresh_token"
    }

    fun getToken(
        tokenRequest: TokenRequest
    ): Mono<TokenResponse> {
        val formData = LinkedMultiValueMap<String, String>()
        prepareTokenRequest(tokenRequest, formData)
        return keyCloakClient
            .post()
            .uri(keyCloakProperties.tokenUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(formData)
            .retrieve()
            .bodyToMono<TokenResponse>()
    }

    private fun prepareTokenRequest(tokenRequest: TokenRequest, formData: LinkedMultiValueMap<String, String>) {
        when (tokenRequest.grantType) {
            GRANT_TYPE_PASSWORD -> preparePasswordRequest(tokenRequest, formData)
            GRANT_TYPE_REFRESH_TOKEN -> prepareRefreshTokenRequest(tokenRequest, formData)
        }
    }

    private fun preparePasswordRequest(tokenRequest: TokenRequest, formData: LinkedMultiValueMap<String, String>) {
        assert(tokenRequest.password.isNullOrBlank())
        formData.add("username", tokenRequest.username)
        formData.add("password", tokenRequest.password)
        formData.add("grant_type", GRANT_TYPE_PASSWORD)
        formData.add("client_id", keyCloakProperties.clientId)
        formData.add("client_secret", keyCloakProperties.clientSecret)
    }

    private fun prepareRefreshTokenRequest(tokenRequest: TokenRequest, formData: LinkedMultiValueMap<String, String>) {
        assert(tokenRequest.refreshToken.isNullOrBlank())
        formData.add("grant_type", GRANT_TYPE_REFRESH_TOKEN)
        formData.add("client_id", keyCloakProperties.clientId)
        formData.add("client_secret", keyCloakProperties.clientSecret)
        formData.add("refresh_token", tokenRequest.refreshToken)
    }

}
