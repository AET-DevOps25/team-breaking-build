package com.recipefy.gateway.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security.cors")
data class CorsConfigurationProperties(
    val allowedOrigins: List<String>
)
