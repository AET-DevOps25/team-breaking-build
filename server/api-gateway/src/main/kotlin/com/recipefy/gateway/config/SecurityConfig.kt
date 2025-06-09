package com.recipefy.gateway.config
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableConfigurationProperties(CorsConfigurationProperties::class, KeyCloakConfigurationProperties::class)
class SecurityConfig(
    private val corsProperties: CorsConfigurationProperties,
    private val keycloakProperties: KeyCloakConfigurationProperties
) {
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/actuator/**")
                    .permitAll()
                    .anyExchange()
                    .authenticated()
            }.oauth2ResourceServer { oauth2 ->
                oauth2.opaqueToken { opaque ->
                    opaque
                        .introspectionUri(keycloakProperties.introspectUrl)
                        .introspectionClientCredentials(
                            keycloakProperties.clientId,
                            keycloakProperties.clientSecret
                        )
                }
            }.csrf { csrf -> csrf.disable() }
            .cors { cors -> cors.configurationSource(corsConfigurationSource()) }
        return http.build()
    }

    private fun corsConfigurationSource(): CorsConfigurationSource {
        val config =
            CorsConfiguration().apply {
                allowCredentials = true
                allowedOrigins = corsProperties.allowedOrigins
                allowedHeaders = listOf("*")
                allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
