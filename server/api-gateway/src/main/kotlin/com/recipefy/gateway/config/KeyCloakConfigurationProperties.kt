package com.recipefy.gateway.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "keycloak")
data class KeyCloakConfigurationProperties(
    val baseUrl: String,
    val realms: String,
    val introspectUrl: String,
    val clientId: String,
    val clientSecret: String
)
