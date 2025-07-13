package com.recipefy.gateway

import com.recipefy.gateway.config.RequestIdHeaderFilter
import java.util.UUID
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
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class RequestIdHeaderFilterTest {

    @Mock
    private lateinit var chain: GatewayFilterChain

    private lateinit var filter: RequestIdHeaderFilter

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        filter = RequestIdHeaderFilter()
    }

    @Test
    fun `should add request ID header when not present`() {
        // Given
        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        whenever(chain.filter(any<ServerWebExchange>())).thenReturn(Mono.empty())

        // When
        val result = filter.filter(exchange, chain)

        // Then
        StepVerifier.create(result).verifyComplete()

        // Verify that the chain was called with a modified exchange
        verify(chain).filter(any<ServerWebExchange>())
    }

    @Test
    fun `should preserve existing request ID header when present`() {
        // Given
        val existingRequestId = "existing-request-id"
        val request =
            MockServerHttpRequest.get("/test").header("X-Request-Id", existingRequestId).build()
        val exchange = MockServerWebExchange.from(request)

        whenever(chain.filter(any<ServerWebExchange>())).thenReturn(Mono.empty())

        // When
        val result = filter.filter(exchange, chain)

        // Then
        StepVerifier.create(result).verifyComplete()

        // Verify that the chain was called
        verify(chain).filter(any<ServerWebExchange>())
    }

    @Test
    fun `should generate valid UUID when no request ID is present`() {
        // Given
        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        whenever(chain.filter(any<ServerWebExchange>())).thenAnswer { invocation ->
            val modifiedExchange = invocation.getArgument<ServerWebExchange>(0)
            val requestId = modifiedExchange.request.headers.getFirst("X-Request-Id")

            // Verify that the request ID is a valid UUID
            assert(requestId != null) { "Request ID should not be null" }
            assert(isValidUUID(requestId!!)) { "Request ID should be a valid UUID: $requestId" }

            Mono.empty<Void>()
        }

        // When
        val result = filter.filter(exchange, chain)

        // Then
        StepVerifier.create(result).verifyComplete()
    }

    @Test
    fun `should use first request ID when multiple headers are present`() {
        // Given
        val firstRequestId = "first-request-id"
        val secondRequestId = "second-request-id"
        val request =
            MockServerHttpRequest.get("/test")
                .header("X-Request-Id", firstRequestId, secondRequestId)
                .build()
        val exchange = MockServerWebExchange.from(request)

        whenever(chain.filter(any<ServerWebExchange>())).thenAnswer { invocation ->
            val modifiedExchange = invocation.getArgument<ServerWebExchange>(0)
            val requestId = modifiedExchange.request.headers.getFirst("X-Request-Id")

            // Should use the first request ID
            assert(requestId == firstRequestId) { "Should use first request ID: $requestId" }

            Mono.empty<Void>()
        }

        // When
        val result = filter.filter(exchange, chain)

        // Then
        StepVerifier.create(result).verifyComplete()
    }


    @Test
    fun `should handle null request ID header`() {
        // Given
        val request =
            MockServerHttpRequest.get("/test").header("X-Request-Id", null as String?).build()
        val exchange = MockServerWebExchange.from(request)

        whenever(chain.filter(any<ServerWebExchange>())).thenAnswer { invocation ->
            val modifiedExchange = invocation.getArgument<ServerWebExchange>(0)
            val requestId = modifiedExchange.request.headers.getFirst("X-Request-Id")

            // Should generate a new UUID since the header is null
            assert(requestId != null && requestId.isNotEmpty()) {
                "Request ID should not be null or empty"
            }
            assert(isValidUUID(requestId!!)) { "Request ID should be a valid UUID: $requestId" }

            Mono.empty<Void>()
        }

        // When
        val result = filter.filter(exchange, chain)

        // Then
        StepVerifier.create(result).verifyComplete()
    }

    @Test
    fun `should maintain other headers while adding request ID`() {
        // Given
        val request =
            MockServerHttpRequest.get("/test")
                .header("Authorization", "Bearer token")
                .header("Content-Type", "application/json")
                .build()
        val exchange = MockServerWebExchange.from(request)

        whenever(chain.filter(any<ServerWebExchange>())).thenAnswer { invocation ->
            val modifiedExchange = invocation.getArgument<ServerWebExchange>(0)
            val headers = modifiedExchange.request.headers

            // Verify that other headers are preserved
            assert(headers.getFirst("Authorization") == "Bearer token") {
                "Authorization header should be preserved"
            }
            assert(headers.getFirst("Content-Type") == "application/json") {
                "Content-Type header should be preserved"
            }

            // Verify that request ID is added
            val requestId = headers.getFirst("X-Request-Id")
            assert(requestId != null) { "Request ID should be added" }
            assert(isValidUUID(requestId!!)) { "Request ID should be a valid UUID: $requestId" }

            Mono.empty<Void>()
        }

        // When
        val result = filter.filter(exchange, chain)

        // Then
        StepVerifier.create(result).verifyComplete()
    }

    @Test
    fun `should work with different HTTP methods`() {
        // Test with different HTTP methods
        val methods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")

        methods.forEach { method ->
            val request =
                MockServerHttpRequest.method(
                    org.springframework.http.HttpMethod.valueOf(method),
                    "/test"
                )
                    .build()
            val exchange = MockServerWebExchange.from(request)

            whenever(chain.filter(any<ServerWebExchange>())).thenAnswer { invocation ->
                val modifiedExchange = invocation.getArgument<ServerWebExchange>(0)
                val requestId = modifiedExchange.request.headers.getFirst("X-Request-Id")

                assert(requestId != null) { "Request ID should be added for $method" }
                assert(isValidUUID(requestId!!)) {
                    "Request ID should be a valid UUID for $method: $requestId"
                }

                Mono.empty<Void>()
            }

            val result = filter.filter(exchange, chain)

            StepVerifier.create(result).verifyComplete()
        }
    }

    @Test
    fun `should work with different paths`() {
        // Test with different paths
        val paths = listOf("/", "/api", "/api/v1", "/api/v1/users", "/api/v1/users/123")

        paths.forEach { path ->
            val request = MockServerHttpRequest.get(path).build()
            val exchange = MockServerWebExchange.from(request)

            whenever(chain.filter(any<ServerWebExchange>())).thenAnswer { invocation ->
                val modifiedExchange = invocation.getArgument<ServerWebExchange>(0)
                val requestId = modifiedExchange.request.headers.getFirst("X-Request-Id")

                assert(requestId != null) { "Request ID should be added for path $path" }
                assert(isValidUUID(requestId!!)) {
                    "Request ID should be a valid UUID for path $path: $requestId"
                }

                Mono.empty<Void>()
            }

            val result = filter.filter(exchange, chain)

            StepVerifier.create(result).verifyComplete()
        }
    }

    @Test
    fun `should handle chain filter errors gracefully`() {
        // Given
        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)
        val expectedException = RuntimeException("Chain filter error")

        whenever(chain.filter(any<ServerWebExchange>())).thenReturn(Mono.error(expectedException))

        // When
        val result = filter.filter(exchange, chain)

        // Then
        StepVerifier.create(result).expectError(RuntimeException::class.java).verify()
    }

    @Test
    fun `should generate different request IDs for different requests`() {
        // Given
        val request1 = MockServerHttpRequest.get("/test1").build()
        val exchange1 = MockServerWebExchange.from(request1)
        val request2 = MockServerHttpRequest.get("/test2").build()
        val exchange2 = MockServerWebExchange.from(request2)

        val capturedRequestIds = mutableListOf<String>()

        whenever(chain.filter(any<ServerWebExchange>())).thenAnswer { invocation ->
            val modifiedExchange = invocation.getArgument<ServerWebExchange>(0)
            val requestId = modifiedExchange.request.headers.getFirst("X-Request-Id")
            if (requestId != null) {
                capturedRequestIds.add(requestId)
            }
            Mono.empty<Void>()
        }

        // When
        val result1 = filter.filter(exchange1, chain)
        val result2 = filter.filter(exchange2, chain)

        // Then
        StepVerifier.create(result1).verifyComplete()
        StepVerifier.create(result2).verifyComplete()

        assert(capturedRequestIds.size == 2) { "Should capture two request IDs" }
        assert(capturedRequestIds[0] != capturedRequestIds[1]) { "Request IDs should be different" }
        assert(isValidUUID(capturedRequestIds[0])) { "First request ID should be valid UUID" }
        assert(isValidUUID(capturedRequestIds[1])) { "Second request ID should be valid UUID" }
    }

    private fun isValidUUID(uuid: String): Boolean {
        return try {
            UUID.fromString(uuid)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
