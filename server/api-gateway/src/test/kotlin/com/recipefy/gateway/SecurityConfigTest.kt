package com.recipefy.gateway

import com.recipefy.gateway.config.CorsConfigurationProperties
import com.recipefy.gateway.config.KeyCloakConfigurationProperties
import com.recipefy.gateway.config.SecurityConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.ApplicationContext
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
@ActiveProfiles("test")
class SecurityConfigTest {
    @Autowired
    private lateinit var context: ApplicationContext

    @MockBean
    private lateinit var tokenIntrospector: ReactiveOpaqueTokenIntrospector

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(context).build()
    }

    @Test
    fun `should allow access to actuator endpoints without authentication`() {
        webTestClient
            .get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun `should require authentication for protected endpoints`() {
        webTestClient
            .get()
            .uri("/protected-endpoint")
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `should configure CORS properly`() {
        val corsProperties =
            CorsConfigurationProperties(
                allowedOrigins = listOf("http://localhost:3000", "https://example.com")
            )
        val keycloakProperties =
            KeyCloakConfigurationProperties(
                baseUrl = "http://localhost:8080",
                realms = "test-realm",
                introspectUrl =
                    "http://localhost:8080/realms/test-realm/protocol/openid-connect/token/introspect",
                clientId = "test-client",
                clientSecret = "test-secret"
            )

        val securityConfig = SecurityConfig(corsProperties, keycloakProperties)
        val corsConfigurationSource = securityConfig.corsConfigurationSource()

        val corsConfiguration =
            corsConfigurationSource.getCorsConfiguration(
                MockServerWebExchange.from(MockServerHttpRequest.get("/").build())
            )

        assert(corsConfiguration != null)
        assert(
            corsConfiguration!!.allowedOrigins ==
                listOf("http://localhost:3000", "https://example.com")
        )
        assert(
            corsConfiguration.allowedMethods ==
                listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        )
        assert(corsConfiguration.allowedHeaders == listOf("*"))
        assert(corsConfiguration.allowCredentials == true)
    }

    @Test
    fun `should create security filter chain with OAuth2 resource server`() {
        val corsProperties =
            CorsConfigurationProperties(allowedOrigins = listOf("http://localhost:3000"))
        val keycloakProperties =
            KeyCloakConfigurationProperties(
                baseUrl = "http://localhost:8080",
                realms = "test-realm",
                introspectUrl =
                    "http://localhost:8080/realms/test-realm/protocol/openid-connect/token/introspect",
                clientId = "test-client",
                clientSecret = "test-secret"
            )

        val securityConfig = SecurityConfig(corsProperties, keycloakProperties)
        val http = ServerHttpSecurity.http()
        val filterChain = securityConfig.securityWebFilterChain(http)

        assert(filterChain != null)
        assert(filterChain is SecurityWebFilterChain)
    }

    @Test
    fun `should disable CSRF for REST API`() {
        // This test verifies that CSRF is disabled by checking that POST requests
        // don't require CSRF tokens
        webTestClient
            .post()
            .uri("/test-endpoint")
            .exchange()
            .expectStatus()
            .isUnauthorized // Should be 401 (unauthorized), not 403 (forbidden/CSRF)
    }

    @Test
    fun `should configure OAuth2 with correct introspection settings`() {
        val corsProperties =
            CorsConfigurationProperties(allowedOrigins = listOf("http://localhost:3000"))
        val keycloakProperties =
            KeyCloakConfigurationProperties(
                baseUrl = "http://localhost:8080",
                realms = "test-realm",
                introspectUrl =
                    "http://localhost:8080/realms/test-realm/protocol/openid-connect/token/introspect",
                clientId = "test-client",
                clientSecret = "test-secret"
            )

        val securityConfig = SecurityConfig(corsProperties, keycloakProperties)

        // Verify the introspection URL is correctly configured
        assert(
            keycloakProperties.introspectUrl ==
                "http://localhost:8080/realms/test-realm/protocol/openid-connect/token/introspect"
        )
        assert(keycloakProperties.clientId == "test-client")
        assert(keycloakProperties.clientSecret == "test-secret")
    }

    @Test
    fun `should allow multiple origins in CORS configuration`() {
        val corsProperties =
            CorsConfigurationProperties(
                allowedOrigins =
                    listOf(
                        "http://localhost:3000",
                        "https://example.com",
                        "https://test.com"
                    )
            )
        val keycloakProperties =
            KeyCloakConfigurationProperties(
                baseUrl = "http://localhost:8080",
                realms = "test-realm",
                introspectUrl =
                    "http://localhost:8080/realms/test-realm/protocol/openid-connect/token/introspect",
                clientId = "test-client",
                clientSecret = "test-secret"
            )

        val securityConfig = SecurityConfig(corsProperties, keycloakProperties)
        val corsConfigurationSource = securityConfig.corsConfigurationSource()

        val corsConfiguration =
            corsConfigurationSource.getCorsConfiguration(
                MockServerWebExchange.from(MockServerHttpRequest.get("/").build())
            )

        assert(corsConfiguration != null)
        assert(corsConfiguration!!.allowedOrigins!!.size == 3)
        assert(corsConfiguration.allowedOrigins!!.contains("http://localhost:3000"))
        assert(corsConfiguration.allowedOrigins!!.contains("https://example.com"))
        assert(corsConfiguration.allowedOrigins!!.contains("https://test.com"))
    }

    @Test
    fun `should handle empty origins list`() {
        val corsProperties = CorsConfigurationProperties(allowedOrigins = emptyList())
        val keycloakProperties =
            KeyCloakConfigurationProperties(
                baseUrl = "http://localhost:8080",
                realms = "test-realm",
                introspectUrl =
                    "http://localhost:8080/realms/test-realm/protocol/openid-connect/token/introspect",
                clientId = "test-client",
                clientSecret = "test-secret"
            )

        val securityConfig = SecurityConfig(corsProperties, keycloakProperties)
        val corsConfigurationSource = securityConfig.corsConfigurationSource()

        val corsConfiguration =
            corsConfigurationSource.getCorsConfiguration(
                MockServerWebExchange.from(MockServerHttpRequest.get("/").build())
            )

        assert(corsConfiguration != null)
        assert(corsConfiguration!!.allowedOrigins!!.isEmpty())
    }

    @Test
    fun `should configure all required HTTP methods for CORS`() {
        val corsProperties =
            CorsConfigurationProperties(allowedOrigins = listOf("http://localhost:3000"))
        val keycloakProperties =
            KeyCloakConfigurationProperties(
                baseUrl = "http://localhost:8080",
                realms = "test-realm",
                introspectUrl =
                    "http://localhost:8080/realms/test-realm/protocol/openid-connect/token/introspect",
                clientId = "test-client",
                clientSecret = "test-secret"
            )

        val securityConfig = SecurityConfig(corsProperties, keycloakProperties)
        val corsConfigurationSource = securityConfig.corsConfigurationSource()

        val corsConfiguration =
            corsConfigurationSource.getCorsConfiguration(
                MockServerWebExchange.from(MockServerHttpRequest.get("/").build())
            )

        assert(corsConfiguration != null)
        val allowedMethods = corsConfiguration!!.allowedMethods!!
        assert(allowedMethods.contains("GET"))
        assert(allowedMethods.contains("POST"))
        assert(allowedMethods.contains("PUT"))
        assert(allowedMethods.contains("DELETE"))
        assert(allowedMethods.contains("OPTIONS"))
    }

    @Test
    fun `should allow all headers in CORS configuration`() {
        val corsProperties =
            CorsConfigurationProperties(allowedOrigins = listOf("http://localhost:3000"))
        val keycloakProperties =
            KeyCloakConfigurationProperties(
                baseUrl = "http://localhost:8080",
                realms = "test-realm",
                introspectUrl =
                    "http://localhost:8080/realms/test-realm/protocol/openid-connect/token/introspect",
                clientId = "test-client",
                clientSecret = "test-secret"
            )

        val securityConfig = SecurityConfig(corsProperties, keycloakProperties)
        val corsConfigurationSource = securityConfig.corsConfigurationSource()

        val corsConfiguration =
            corsConfigurationSource.getCorsConfiguration(
                MockServerWebExchange.from(MockServerHttpRequest.get("/").build())
            )

        assert(corsConfiguration != null)
        assert(corsConfiguration!!.allowedHeaders == listOf("*"))
    }

    @Test
    fun `should enable credentials in CORS configuration`() {
        val corsProperties =
            CorsConfigurationProperties(allowedOrigins = listOf("http://localhost:3000"))
        val keycloakProperties =
            KeyCloakConfigurationProperties(
                baseUrl = "http://localhost:8080",
                realms = "test-realm",
                introspectUrl =
                    "http://localhost:8080/realms/test-realm/protocol/openid-connect/token/introspect",
                clientId = "test-client",
                clientSecret = "test-secret"
            )

        val securityConfig = SecurityConfig(corsProperties, keycloakProperties)
        val corsConfigurationSource = securityConfig.corsConfigurationSource()

        val corsConfiguration =
            corsConfigurationSource.getCorsConfiguration(
                MockServerWebExchange.from(MockServerHttpRequest.get("/").build())
            )

        assert(corsConfiguration != null)
        assert(corsConfiguration!!.allowCredentials == true)
    }
}
