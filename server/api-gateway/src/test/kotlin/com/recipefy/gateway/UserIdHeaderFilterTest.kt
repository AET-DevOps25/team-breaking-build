package com.recipefy.gateway

import com.recipefy.gateway.config.UserIdHeaderFilter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.UUID

class UserIdHeaderFilterTest {
    @Mock
    private lateinit var chain: GatewayFilterChain

    @Mock
    private lateinit var securityContext: SecurityContext

    @Mock
    private lateinit var authentication: BearerTokenAuthentication

    @Mock
    private lateinit var principal: OAuth2AuthenticatedPrincipal

    private lateinit var filter: UserIdHeaderFilter

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        filter = UserIdHeaderFilter()
    }

    @Test
    fun `should add user ID header when authenticated`() {
        // Given
        val userId = UUID.randomUUID().toString()
        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        whenever(securityContext.authentication).thenReturn(authentication)
        whenever(authentication.principal).thenReturn(principal)
        whenever(principal.getAttribute<String>("sub")).thenReturn(userId)
        whenever(chain.filter(any<ServerWebExchange>())).thenAnswer { invocation ->
            val modifiedExchange = invocation.getArgument<ServerWebExchange>(0)
            val userIdHeader = modifiedExchange.request.headers.getFirst("X-User-Id")

            assert(userIdHeader == userId) {
                "User ID header should be set to $userId but was $userIdHeader"
            }

            Mono.empty<Void>()
        }

        // When
        val result =
            filter
                .filter(exchange, chain)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(securityContext)
                    )
                )

        // Then
        StepVerifier.create(result).verifyComplete()

        verify(chain).filter(any<ServerWebExchange>())
    }

    @Test
    fun `should handle null user ID gracefully`() {
        // Given
        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        whenever(securityContext.authentication).thenReturn(authentication)
        whenever(authentication.principal).thenReturn(principal)
        whenever(principal.getAttribute<String>("sub")).thenReturn(null)
        whenever(chain.filter(any<ServerWebExchange>())).thenAnswer { invocation ->
            val modifiedExchange = invocation.getArgument<ServerWebExchange>(0)
            val userIdHeader = modifiedExchange.request.headers.getFirst("X-User-Id")

            assert(userIdHeader == null) { "User ID header should be null when user ID is null" }

            Mono.empty<Void>()
        }

        // When
        val result =
            filter
                .filter(exchange, chain)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(securityContext)
                    )
                )

        // Then
        StepVerifier.create(result).verifyComplete()
    }

    @Test
    fun `should handle empty user ID gracefully`() {
        // Given
        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        whenever(securityContext.authentication).thenReturn(authentication)
        whenever(authentication.principal).thenReturn(principal)
        whenever(principal.getAttribute<String>("sub")).thenReturn("")
        whenever(chain.filter(any<ServerWebExchange>())).thenAnswer { invocation ->
            val modifiedExchange = invocation.getArgument<ServerWebExchange>(0)
            val userIdHeader = modifiedExchange.request.headers.getFirst("X-User-Id")

            assert(userIdHeader == "") { "User ID header should be empty when user ID is empty" }

            Mono.empty<Void>()
        }

        // When
        val result =
            filter
                .filter(exchange, chain)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(securityContext)
                    )
                )

        // Then
        StepVerifier.create(result).verifyComplete()
    }

    @Test
    fun `should preserve existing headers while adding user ID`() {
        // Given
        val userId = UUID.randomUUID().toString()
        val request =
            MockServerHttpRequest
                .get("/test")
                .header("Authorization", "Bearer token")
                .header("Content-Type", "application/json")
                .build()
        val exchange = MockServerWebExchange.from(request)

        whenever(securityContext.authentication).thenReturn(authentication)
        whenever(authentication.principal).thenReturn(principal)
        whenever(principal.getAttribute<String>("sub")).thenReturn(userId)
        whenever(chain.filter(any<ServerWebExchange>())).thenAnswer { invocation ->
            val modifiedExchange = invocation.getArgument<ServerWebExchange>(0)
            val headers = modifiedExchange.request.headers

            // Verify that existing headers are preserved
            assert(headers.getFirst("Authorization") == "Bearer token") {
                "Authorization header should be preserved"
            }
            assert(headers.getFirst("Content-Type") == "application/json") {
                "Content-Type header should be preserved"
            }

            // Verify that user ID header is added
            assert(headers.getFirst("X-User-Id") == userId) { "User ID header should be added" }

            Mono.empty<Void>()
        }

        // When
        val result =
            filter
                .filter(exchange, chain)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(securityContext)
                    )
                )

        // Then
        StepVerifier.create(result).verifyComplete()
    }

    @Test
    fun `should work with different HTTP methods`() {
        // Given
        val userId = UUID.randomUUID().toString()
        val methods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")

        whenever(securityContext.authentication).thenReturn(authentication)
        whenever(authentication.principal).thenReturn(principal)
        whenever(principal.getAttribute<String>("sub")).thenReturn(userId)

        methods.forEach { method ->
            val request =
                MockServerHttpRequest
                    .method(
                        org.springframework.http.HttpMethod
                            .valueOf(method),
                        "/test"
                    ).build()
            val exchange = MockServerWebExchange.from(request)

            whenever(chain.filter(any<ServerWebExchange>())).thenAnswer { invocation ->
                val modifiedExchange = invocation.getArgument<ServerWebExchange>(0)
                val userIdHeader = modifiedExchange.request.headers.getFirst("X-User-Id")

                assert(userIdHeader == userId) { "User ID header should be set for $method" }

                Mono.empty<Void>()
            }

            val result =
                filter
                    .filter(exchange, chain)
                    .contextWrite(
                        ReactiveSecurityContextHolder.withSecurityContext(
                            Mono.just(securityContext)
                        )
                    )

            StepVerifier.create(result).verifyComplete()
        }
    }

    @Test
    fun `should work with different paths`() {
        // Given
        val userId = UUID.randomUUID().toString()
        val paths = listOf("/", "/api", "/api/v1", "/api/v1/users", "/api/v1/users/123")

        whenever(securityContext.authentication).thenReturn(authentication)
        whenever(authentication.principal).thenReturn(principal)
        whenever(principal.getAttribute<String>("sub")).thenReturn(userId)

        paths.forEach { path ->
            val request = MockServerHttpRequest.get(path).build()
            val exchange = MockServerWebExchange.from(request)

            whenever(chain.filter(any<ServerWebExchange>())).thenAnswer { invocation ->
                val modifiedExchange = invocation.getArgument<ServerWebExchange>(0)
                val userIdHeader = modifiedExchange.request.headers.getFirst("X-User-Id")

                assert(userIdHeader == userId) { "User ID header should be set for path $path" }

                Mono.empty<Void>()
            }

            val result =
                filter
                    .filter(exchange, chain)
                    .contextWrite(
                        ReactiveSecurityContextHolder.withSecurityContext(
                            Mono.just(securityContext)
                        )
                    )

            StepVerifier.create(result).verifyComplete()
        }
    }

    @Test
    fun `should handle chain filter errors gracefully`() {
        // Given
        val userId = UUID.randomUUID().toString()
        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)
        val expectedException = RuntimeException("Chain filter error")

        whenever(securityContext.authentication).thenReturn(authentication)
        whenever(authentication.principal).thenReturn(principal)
        whenever(principal.getAttribute<String>("sub")).thenReturn(userId)
        whenever(chain.filter(any<ServerWebExchange>())).thenReturn(Mono.error(expectedException))

        // When
        val result =
            filter
                .filter(exchange, chain)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(securityContext)
                    )
                )

        // Then
        StepVerifier.create(result).expectError(RuntimeException::class.java).verify()
    }

    @Test
    fun `should handle UUID format user IDs`() {
        // Given
        val userId = UUID.randomUUID().toString()
        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        whenever(securityContext.authentication).thenReturn(authentication)
        whenever(authentication.principal).thenReturn(principal)
        whenever(principal.getAttribute<String>("sub")).thenReturn(userId)
        whenever(chain.filter(any<ServerWebExchange>())).thenAnswer { invocation ->
            val modifiedExchange = invocation.getArgument<ServerWebExchange>(0)
            val userIdHeader = modifiedExchange.request.headers.getFirst("X-User-Id")

            assert(userIdHeader == userId) { "User ID header should match UUID format" }

            // Verify it's a valid UUID
            try {
                UUID.fromString(userIdHeader!!)
            } catch (e: IllegalArgumentException) {
                assert(false) { "User ID should be a valid UUID format" }
            }

            Mono.empty<Void>()
        }

        // When
        val result =
            filter
                .filter(exchange, chain)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(securityContext)
                    )
                )

        // Then
        StepVerifier.create(result).verifyComplete()
    }

    @Test
    fun `should handle non-UUID format user IDs`() {
        // Given
        val userId = "user123"
        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        whenever(securityContext.authentication).thenReturn(authentication)
        whenever(authentication.principal).thenReturn(principal)
        whenever(principal.getAttribute<String>("sub")).thenReturn(userId)
        whenever(chain.filter(any<ServerWebExchange>())).thenAnswer { invocation ->
            val modifiedExchange = invocation.getArgument<ServerWebExchange>(0)
            val userIdHeader = modifiedExchange.request.headers.getFirst("X-User-Id")

            assert(userIdHeader == userId) { "User ID header should match non-UUID format" }

            Mono.empty<Void>()
        }

        // When
        val result =
            filter
                .filter(exchange, chain)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(securityContext)
                    )
                )

        // Then
        StepVerifier.create(result).verifyComplete()
    }

    @Test
    fun `should handle special characters in user ID`() {
        // Given
        val userId = "user@domain.com"
        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        whenever(securityContext.authentication).thenReturn(authentication)
        whenever(authentication.principal).thenReturn(principal)
        whenever(principal.getAttribute<String>("sub")).thenReturn(userId)
        whenever(chain.filter(any<ServerWebExchange>())).thenAnswer { invocation ->
            val modifiedExchange = invocation.getArgument<ServerWebExchange>(0)
            val userIdHeader = modifiedExchange.request.headers.getFirst("X-User-Id")

            assert(userIdHeader == userId) { "User ID header should handle special characters" }

            Mono.empty<Void>()
        }

        // When
        val result =
            filter
                .filter(exchange, chain)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(securityContext)
                    )
                )

        // Then
        StepVerifier.create(result).verifyComplete()
    }

    @Test
    fun `should handle long user IDs`() {
        // Given
        val userId = "a".repeat(1000) // Very long user ID
        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        whenever(securityContext.authentication).thenReturn(authentication)
        whenever(authentication.principal).thenReturn(principal)
        whenever(principal.getAttribute<String>("sub")).thenReturn(userId)
        whenever(chain.filter(any<ServerWebExchange>())).thenAnswer { invocation ->
            val modifiedExchange = invocation.getArgument<ServerWebExchange>(0)
            val userIdHeader = modifiedExchange.request.headers.getFirst("X-User-Id")

            assert(userIdHeader == userId) { "User ID header should handle long user IDs" }
            assert(userIdHeader!!.length == 1000) { "User ID should maintain its length" }

            Mono.empty<Void>()
        }

        // When
        val result =
            filter
                .filter(exchange, chain)
                .contextWrite(
                    ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(securityContext)
                    )
                )

        // Then
        StepVerifier.create(result).verifyComplete()
    }

    @Test
    fun `should not fail when security context is empty`() {
        // Given
        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        whenever(chain.filter(any<ServerWebExchange>())).thenReturn(Mono.empty())

        // When - No security context provided
        val result = filter.filter(exchange, chain)

        // Then - Should not fail, but user ID header won't be added
        StepVerifier.create(result).verifyComplete()
    }
}
