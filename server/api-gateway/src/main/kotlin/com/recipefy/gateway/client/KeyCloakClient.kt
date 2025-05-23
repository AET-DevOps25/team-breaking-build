package com.recipefy.gateway.client

import com.recipefy.gateway.config.KeyCloakConfigurationProperties
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
    }

    fun getToken(
        username: String,
        password: String
    ): Mono<TokenResponse> {
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("username", username)
        formData.add("password", password)
        formData.add("grant_type", GRANT_TYPE_PASSWORD)
        formData.add("client_id", keyCloakProperties.clientId)
        formData.add("client_secret", keyCloakProperties.clientSecret)

        return keyCloakClient
            .post()
            .uri(keyCloakProperties.tokenUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(formData)
            .retrieve()
            .bodyToMono<TokenResponse>()
    }
}
