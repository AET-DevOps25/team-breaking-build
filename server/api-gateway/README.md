# API Gateway Service

A Spring Boot API Gateway built with Kotlin and Spring Cloud Gateway, serving as the central entry point for the Recipefy microservices architecture. It provides request routing, authentication, CORS handling, and observability features.

## Overview

The API Gateway acts as a reverse proxy and single entry point for all client requests, routing them to appropriate backend microservices. It handles cross-cutting concerns like authentication, authorization, request/response transformation, and monitoring.

## Key Features

### 🌐 Request Routing
- **Intelligent Routing**: Route requests to appropriate microservices based on path patterns
- **Load Balancing**: Distribute requests across multiple instances
- **Dynamic Routing**: Runtime route configuration and updates
- **Path Rewriting**: Transform request paths before forwarding

### 🔐 Security & Authentication
- **OAuth2 Resource Server**: Token-based authentication via Keycloak
- **JWT Token Validation**: Verify and introspect OAuth2 tokens
- **User Context Propagation**: Forward user information to downstream services
- **Request ID Tracking**: Unique request identification for tracing

### 🌍 Cross-Origin Resource Sharing (CORS)
- **Configurable CORS**: Support for multiple frontend origins
- **Preflight Handling**: Automatic OPTIONS request handling
- **Flexible Configuration**: Environment-specific CORS policies

### 📊 Observability & Monitoring
- **Prometheus Metrics**: Request counts, latencies, and error rates
- **Health Checks**: Service health and dependency status
- **Request Tracing**: Distributed tracing with request IDs
- **Structured Logging**: JSON-formatted logs for analysis

## Technology Stack

- **Framework**: Spring Boot 3.4.6 with WebFlux (reactive)
- **Language**: Kotlin 1.9.25
- **Gateway**: Spring Cloud Gateway
- **Security**: Spring Security with OAuth2 Resource Server
- **Build Tool**: Gradle with Kotlin DSL
- **Monitoring**: Micrometer with Prometheus
- **Testing**: Spring Boot Test with Mockito

## Architecture

```
                    ┌─────────────────┐
                    │   Client App    │
                    │  (React/Mobile) │
                    └─────────┬───────┘
                              │ HTTPS/HTTP
                              ▼
                    ┌─────────────────┐
                    │  API Gateway    │
                    │ (Spring Cloud)  │
                    │                 │
┌─────────────────┐ │ ┌─────────────┐ │ ┌─────────────────┐
│   Keycloak      │◄┼─┤ Auth Filter │ │ │                 │
│ (Auth Server)   │ │ └─────────────┘ │ │                 │
└─────────────────┘ │                 │ │                 │
                    │ ┌─────────────┐ │ │ Downstream      │
                    │ │CORS Filter  │ │ │ Services:       │
                    │ └─────────────┘ │ │                 │
                    │                 │ │ • Recipe        │
                    │ ┌─────────────┐ │ │ • Version       │
                    │ │Request ID   │ │ │ • GenAI         │
                    │ │Extraction   │ │ │                 │
                    │ └─────────────┘ │ │                 │
                    │ ┌─────────────┐ │ │                 │
                    │ │User ID      │ │ │                 │
                    │ │Extraction   │ │ │                 │
                    │ └─────────────┘ │ │                 │
                    └─────────┬───────┘ └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │   Monitoring    │
                    │ (Prometheus)    │
                    └─────────────────┘
```

## Project Structure

```
server/api-gateway/
├── src/
│   ├── main/
│   │   ├── kotlin/com/recipefy/gateway/
│   │   │   ├── ApiGatewayApplication.kt      # Main application
│   │   │   └── config/                       # Configuration classes
│   │   │       ├── SecurityConfig.kt         # Security configuration
│   │   │       ├── CorsConfigurationProperties.kt
│   │   │       ├── KeyCloakConfigurationProperties.kt
│   │   │       ├── RequestIdHeaderFilter.kt  # Request tracking
│   │   │       └── UserIdHeaderFilter.kt     # User context
│   │   └── resources/
│   │       ├── application.yml               # Application configuration
│   │       └── logback-spring.xml           # Logging configuration
│   └── test/                                # Unit and integration tests
├── build.gradle.kts                         # Build configuration
├── Dockerfile                               # Container image
└── settings.gradle.kts                      # Gradle settings
```

## Routing Configuration

### Current Routes

The gateway routes requests based on path patterns:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: version-control-system
          uri: http://version:8080
          predicates:
            - Path=/vcs/**
            
        - id: recipes
          uri: http://recipe:8080
          predicates:
            - Path=/recipes/**
            
        - id: genai
          uri: http://genai:8080
          predicates:
            - Path=/genai/**
            
        - id: user-recipes
          uri: http://recipe:8080
          predicates:
            - Path=/users/*/recipes/**
```

### Dynamic Routing

Routes can be configured via environment variables:

```bash
SPRING_CLOUD_GATEWAY_ROUTES_0_ID=recipes
SPRING_CLOUD_GATEWAY_ROUTES_0_URI=http://recipe-service:8080
SPRING_CLOUD_GATEWAY_ROUTES_0_PREDICATES_0=Path=/recipes/**
```

## Security Configuration

### OAuth2 Resource Server

The gateway validates JWT tokens using Keycloak's introspection endpoint:

```kotlin
oauth2ResourceServer { oauth2 ->
    oauth2.opaqueToken { opaque ->
        opaque
            .introspectionUri(keycloakProperties.introspectUrl)
            .introspectionClientCredentials(
                keycloakProperties.clientId,
                keycloakProperties.clientSecret
            )
    }
}
```

### Request Headers

The gateway automatically adds headers for downstream services:

- `X-Request-Id`: Unique request identifier
- `X-User-Id`: Authenticated user ID from JWT token
- `Authorization`: Forwarded Bearer token

## Development Setup

### Prerequisites

- Java 21 or higher
- Gradle 8.x
- Docker (for dependencies)
- Access to Keycloak instance

### Running Locally

```bash
# Start dependencies (if using Docker Compose)
docker-compose up -d keycloak postgres

# Run the gateway
./gradlew bootRun

# Or with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The gateway will be available at [http://localhost:8080](http://localhost:8080).

### Environment Configuration

#### Required Environment Variables

```bash
# Keycloak Configuration
KEYCLOAK_CREDENTIALS_RECIPEFY_CLIENT_ID=your-client-id
KEYCLOAK_CREDENTIALS_RECIPEFY_CLIENT_SECRET=your-client-secret

# CORS Configuration (optional)
APP_SECURITY_CORS_ALLOWED_ORIGINS_0=http://localhost:3000
```

#### Development Profile

```yaml
# application-dev.yml
keycloak:
  base-url: http://localhost:8080
  realms: recipefy

logging:
  level:
    com.recipefy.gateway: DEBUG
    org.springframework.cloud.gateway: DEBUG
```

## Build and Deployment

### Gradle Build

```bash
# Build the application
./gradlew build

# Run tests
./gradlew test

# Check code quality
./gradlew check

# Generate JAR
./gradlew bootJar
```

### Docker Build

```bash
# Build Docker image
docker build -t recipefy-api-gateway .

# Run container
docker run -p 8080:8080 \
  -e KEYCLOAK_CREDENTIALS_RECIPEFY_CLIENT_ID=client-id \
  -e KEYCLOAK_CREDENTIALS_RECIPEFY_CLIENT_SECRET=client-secret \
  recipefy-api-gateway
```

### Multi-stage Dockerfile

The service uses a multi-stage build:

1. **Build Stage**: Gradle build with full JDK
2. **Runtime Stage**: Optimized JRE runtime

## Testing

### Test Categories

- **Unit Tests**: Component testing with mocking
- **Integration Tests**: Full Spring context testing
- **Security Tests**: Authentication and authorization flows

### Running Tests

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport

# Run specific test class
./gradlew test --tests "*SecurityConfigTest"
```

### Test Configuration

```kotlin
@SpringBootTest
@ActiveProfiles("test")
class ApiGatewayApplicationTests {
    
    @Test
    fun contextLoads() {
        // Verify Spring context loads correctly
    }
}
```

## Configuration Properties

### CORS Configuration

```kotlin
@ConfigurationProperties(prefix = "app.security.cors")
data class CorsConfigurationProperties(
    val allowedOrigins: List<String>
)
```

### Keycloak Configuration

```kotlin
@ConfigurationProperties(prefix = "keycloak")
data class KeyCloakConfigurationProperties(
    val baseUrl: String,
    val realms: String,
    val introspectUrl: String,
    val clientId: String,
    val clientSecret: String
)
```

## Monitoring and Observability

### Health Checks

The gateway exposes health endpoints:

```bash
# Application health
curl http://localhost:8080/actuator/health

# Detailed health information
curl http://localhost:8080/actuator/health/readiness
```

### Prometheus Metrics

Available at `/actuator/prometheus`:

- HTTP request metrics (`http_server_requests`)
- JVM metrics (`jvm_*`)
- Gateway route metrics (`gateway_*`)
- Custom business metrics

### Request Tracing

Every request gets a unique `X-Request-Id` header for tracing:

```kotlin
class RequestIdHeaderFilter : GlobalFilter {
    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val id = exchange.request.headers["X-Request-Id"]?.firstOrNull() 
            ?: UUID.randomUUID().toString()
        
        val mutatedExchange = exchange.mutate()
            .request(exchange.request.mutate().header("X-Request-Id", id).build())
            .build()
            
        return chain.filter(mutatedExchange)
    }
}
```

### Structured Logging

JSON-formatted logs in production:

```xml
<!-- logback-spring.xml -->
<springProfile name="prod">
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
            </providers>
        </encoder>
    </appender>
</springProfile>
```

## Performance Considerations

### Reactive Architecture

Built on Spring WebFlux for non-blocking I/O:

- High concurrency with fewer threads
- Backpressure handling
- Efficient resource utilization

### Connection Pooling

Configurable connection pools for downstream services:

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          max-connections: 100
          max-idle-time: 30s
```

### Circuit Breaker

Integration with Spring Cloud Circuit Breaker:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: recipe-service
          filters:
            - CircuitBreaker=recipe-cb
```

## Security Best Practices

### Token Validation

- OAuth2 token introspection with Keycloak
- Token caching for performance
- Automatic token refresh handling

### CORS Policy

Restrictive CORS configuration:

```kotlin
cors { cors ->
    cors.configurationSource(corsConfigurationSource())
}

private fun corsConfigurationSource(): CorsConfigurationSource {
    val config = CorsConfiguration().apply {
        allowCredentials = true
        allowedOrigins = corsProperties.allowedOrigins
        allowedHeaders = listOf("*")
        allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
    }
    
    return UrlBasedCorsConfigurationSource().apply {
        registerCorsConfiguration("/**", config)
    }
}
```

### Request Size Limits

```yaml
spring:
  webflux:
    multipart:
      max-in-memory-size: 10MB
      max-disk-usage-per-part: 100MB
```

## Troubleshooting

### Common Issues

1. **Authentication Failures**
   ```bash
   # Check Keycloak connectivity
   curl http://keycloak:8080/realms/recipefy/.well-known/openid_configuration
   
   # Verify client credentials
   echo $KEYCLOAK_CREDENTIALS_RECIPEFY_CLIENT_ID
   ```

2. **Routing Issues**
   ```bash
   # Check route configuration
   curl http://localhost:8080/actuator/gateway/routes
   
   # Test specific route
   curl -H "Authorization: Bearer <token>" http://localhost:8080/recipes
   ```

3. **CORS Problems**
   ```bash
   # Verify CORS headers
   curl -H "Origin: http://localhost:3000" \
        -H "Access-Control-Request-Method: POST" \
        -X OPTIONS http://localhost:8080/recipes
   ```

### Debug Mode

Enable debug logging:

```yaml
logging:
  level:
    com.recipefy.gateway: DEBUG
    org.springframework.cloud.gateway: DEBUG
    org.springframework.security: DEBUG
```

### Health Diagnostics

```bash
# Check all health indicators
curl http://localhost:8080/actuator/health

# Gateway-specific health
curl http://localhost:8080/actuator/gateway/routes
```

## Integration Testing

### WebTestClient

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTest {
    
    @Autowired
    lateinit var webTestClient: WebTestClient
    
    @Test
    fun `should route to recipe service`() {
        webTestClient
            .get()
            .uri("/recipes")
            .header("Authorization", "Bearer valid-token")
            .exchange()
            .expectStatus().isOk
    }
}
```

### MockWebServer

```kotlin
@Test
fun `should handle downstream service unavailable`() {
    // Mock downstream service responses
    mockWebServer.enqueue(MockResponse().setResponseCode(503))
    
    webTestClient
        .get()
        .uri("/recipes")
        .exchange()
        .expectStatus().is5xxServerError
}
```

## Deployment Configurations

### Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
spec:
  replicas: 3
  selector:
    matchLabels:
      app: api-gateway
  template:
    spec:
      containers:
      - name: api-gateway
        image: recipefy-api-gateway:latest
        ports:
        - containerPort: 8080
        env:
        - name: KEYCLOAK_BASE_URL
          value: "http://keycloak:8080"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
```

### Docker Compose

```yaml
services:
  api-gateway:
    build: ./server/api-gateway
    ports:
      - "8080:8080"
    environment:
      - KEYCLOAK_BASE_URL=http://keycloak:8080
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      - keycloak
      - recipe
      - version
      - genai
```
