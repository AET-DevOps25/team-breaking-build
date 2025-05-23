package com.recipefy.gateway.client.webclient

import com.recipefy.gateway.config.KeyCloakConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableConfigurationProperties(KeyCloakConfigurationProperties::class)
class KeyCloakWebClient(
    private val keyCloakConfigurationProperties: KeyCloakConfigurationProperties
) {
    @Bean
    fun keycloakWebClient(): WebClient =
        WebClient
            .builder()
            .baseUrl(keyCloakConfigurationProperties.baseUrl)
            .build()
}
