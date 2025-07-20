# Keycloak Integration Service

A Spring Boot WebFlux service that provides authentication and user management capabilities by integrating with Keycloak, serving as a bridge between the Recipefy application and the Keycloak authentication server.

## Overview

The Keycloak service acts as an authentication proxy, providing simplified REST endpoints for user authentication, registration, token management, and user information retrieval. It abstracts the complexity of direct Keycloak integration and provides a consistent API for the frontend and other microservices.

### Why This Service is Needed

This service is essential for security reasons - it prevents client-side secret exposure. Instead of having the frontend directly communicate with Keycloak (which would require exposing client secrets in the browser), this service acts as a secure intermediary. The client secrets and admin credentials are kept server-side, ensuring that sensitive authentication information is never exposed to the client application.

## Key Features

### 🔐 Authentication Management
- **User Login**: OAuth2 password grant authentication flow
- **User Registration**: Create new user accounts
- **Token Management**: Handle access and refresh tokens
- **User Information**: Retrieve authenticated user details

### 🔄 Token Operations
- **Access Token Generation**: Issue JWT tokens for authentication
- **Token Refresh**: Refresh expired access tokens using refresh tokens
- **Token Introspection**: Validate and retrieve token information
- **Session Management**: Handle user session lifecycle

### 👤 User Management
- **User Creation**: Register new users with credentials
- **Profile Management**: Retrieve and update user profiles
- **Email Verification**: Handle email verification workflows
- **User Lookup**: Find users by ID and other identifiers

### 🌐 Integration Features
- **Admin API Access**: Use Keycloak admin APIs with service account
- **Realm Management**: Configure and manage authentication realms
- **Client Configuration**: Handle OAuth2 client settings
- **Error Handling**: Comprehensive error mapping and responses

## Technology Stack

- **Framework**: Spring Boot 3.5.0 with WebFlux (reactive)
- **Language**: Java 21
- **Security**: OAuth2 and OpenID Connect
- **HTTP Client**: WebClient for reactive HTTP calls
- **Validation**: Spring Boot Validation with custom annotations
- **Build Tool**: Gradle
- **Testing**: Spring Boot Test with Reactor Test

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client App    │    │  Keycloak SPI   │    │   Keycloak      │
│  (Frontend)     │    │   Service       │    │    Server       │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │   Login     │─┼────┼→│    Auth     │─┼────┼→│   Token     │ │
│ │  Component  │ │    │ │ Controller  │ │    │ │  Endpoint   │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │ Registration│─┼────┼→│ Keycloak    │─┼────┼→│   Admin     │ │
│ │    Form     │ │    │ │  Service    │ │    │ │    API      │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                        │
                                                        ▼
                                               ┌─────────────────┐
                                               │   User Store    │
                                               │  (Database)     │
                                               └─────────────────┘
```

## Project Structure

```
server/keycloak/
├── src/
│   ├── main/
│   │   ├── java/com/recipefy/keycloak/
│   │   │   ├── KeycloakApplication.java        # Main application
│   │   │   ├── controller/                     # REST controllers
│   │   │   │   └── AuthController.java         # Authentication endpoints
│   │   │   ├── service/                        # Business logic
│   │   │   │   └── KeycloakService.java        # Keycloak integration
│   │   │   ├── config/                         # Configuration classes
│   │   │   │   ├── KeycloakConfig.java         # Keycloak client config
│   │   │   │   ├── KeycloakAdminConfig.java    # Admin client config
│   │   │   │   ├── KeycloakProperties.java     # Configuration properties
│   │   │   │   ├── KeycloakAdminExchangeFilter.java # Admin auth filter
│   │   │   │   ├── SwaggerConfig.java          # API documentation
│   │   │   │   └── WebConfig.java              # CORS configuration
│   │   │   └── model/                          # Data models
│   │   │       ├── LoginRequest.java           # Request DTOs
│   │   │       ├── RegisterRequest.java
│   │   │       ├── TokenResponse.java          # Response DTOs
│   │   │       ├── UserDetails.java
│   │   │       └── UserPayload.java            # Keycloak API models
│   │   └── resources/
│   │       ├── application.yml                 # Application configuration
│   │       └── logback-spring.xml             # Logging configuration
│   └── test/                                  # Unit and integration tests
├── build.gradle                               # Build configuration
├── Dockerfile                                 # Container image
└── settings.gradle                            # Gradle settings
```

## API Endpoints

### Authentication Endpoints

#### User Login
```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

Response:
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
  "expires_in": 3600,
  "token_type": "Bearer"
}
```

#### User Registration
```http
POST /auth/register
Content-Type: application/json

{
  "email": "newuser@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "password": "password123"
}
```

#### Get User Information
```http
GET /auth/userinfo
Authorization: Bearer <access_token>

Response:
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "given_name": "John",
  "family_name": "Doe",
  "email_verified": true
}
```

#### Refresh Token
```http
POST /auth/refresh
Content-Type: application/json

{
  "token": "refresh_token_here"
}
```

#### Get User Details
```http
GET /auth/users/{userId}
Authorization: Bearer <admin_token>

Response:
{
  "id": "user-uuid",
  "username": "john.doe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "emailVerified": true
}
```

## Configuration

### Application Properties

```yaml
keycloak:
  baseUrl: https://keycloak.team-breaking-build.student.k8s.aet.cit.tum.de
  realm: recipefy
  adminUri: "/admin/realms/${keycloak.realm}"
  keycloakUri: "/realms/${keycloak.realm}/protocol/openid-connect"
  credentials:
    recipefy:
      clientId: web
      clientSecret: web-secret
    admin:
      clientId: admin-cli
      clientSecret: admin-secret
```

### Environment Variables

```bash
# Keycloak Server Configuration
KEYCLOAK_BASE_URL=https://your-keycloak-server.com
KEYCLOAK_REALM=recipefy

# Client Credentials
KEYCLOAK_CREDENTIALS_RECIPEFY_CLIENT_ID=web
KEYCLOAK_CREDENTIALS_RECIPEFY_CLIENT_SECRET=your-client-secret

# Admin Credentials
KEYCLOAK_CREDENTIALS_ADMIN_CLIENT_ID=admin-cli
KEYCLOAK_CREDENTIALS_ADMIN_CLIENT_SECRET=your-admin-secret
```

## Development Setup

### Prerequisites

- Java 21 or higher
- Gradle 8.x
- Running Keycloak instance
- Configured Keycloak realm and clients

### Running Locally

```bash
# Start Keycloak (if using Docker)
docker run -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest start-dev

# Run the service
./gradlew bootRun
```

### Keycloak Setup

The Keycloak realm configuration is automatically imported on startup using the realm export file. You can run Keycloak with the realm import using Docker:

```bash
# Run Keycloak with automatic realm import
docker run -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -v ./config/realm-export.json:/opt/keycloak/data/import/realm.json \
  quay.io/keycloak/keycloak:26.2.4 start --import-realm --verbose
```

The realm export file (`config/realm-export.json`) includes:
- **Realm Configuration**: Complete `recipefy` realm settings and policies
- **Client Definitions**: Pre-configured OAuth2 clients (`web` for frontend, `admin-cli` for admin operations)
- **User Roles**: Default roles and permissions
- **Authentication Flows**: Custom authentication and authorization flows
- **Theme Settings**: Custom login and account management themes

This approach ensures consistent realm configuration across different environments and eliminates the need for manual setup or API calls. All secrets are predefined and configured as environment variables in the export JSON, so there is no risk of secret leakage in the configuration files.

## Service Implementation

### KeycloakService Class

The main service class handles all Keycloak interactions:

```java
@Service
@RequiredArgsConstructor
public class KeycloakService {
    private final KeycloakProperties properties;
    private final WebClient keycloakClient;
    private final WebClient keycloakAdminClient;

    public Mono<TokenResponse> login(LoginRequest request) {
        // OAuth2 password grant flow
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", properties.getCredentials().getRecipefy().getClientId());
        body.add("username", request.getEmail());
        body.add("password", request.getPassword());
        
        return keycloakClient.post()
                .uri(properties.getKeycloakUri() + "/token")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(TokenResponse.class);
    }
}
```

### Admin Client Configuration

The service uses a service account for admin operations:

```java
@Component
public class KeycloakAdminExchangeFilter implements ExchangeFilterFunction {
    private final AtomicReference<CachedToken> cache = new AtomicReference<>();

    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return getAdminToken()
                .map(token -> ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build())
                .flatMap(next::exchange);
    }

    private Mono<String> getAdminToken() {
        // Get cached token or refresh if expired
        CachedToken current = cache.get();
        if (current == null || Instant.now().isAfter(current.expiresAt.minusSeconds(60))) {
            return getNewToken().doOnNext(cache::set).map(t -> t.token);
        }
        return Mono.just(current.token);
    }
}
```

## Data Models

### Request Models

```java
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}

public class RegisterRequest {
    @NotBlank @Email
    private String email;
    
    @NotBlank
    private String firstName;
    
    @NotBlank
    private String lastName;
    
    @NotBlank @Size(min = 6)
    private String password;
}
```

### Response Models

```java
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private long refreshExpiresIn;
    private String tokenType;
    private String scope;
}

public class UserDetails {
    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean emailVerified;
}
```

## Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class KeycloakServiceTest {
    
    @Mock
    private WebClient keycloakClient;
    
    @InjectMocks
    private KeycloakService keycloakService;
    
    @Test
    void login_ShouldReturnTokenResponse_WhenCredentialsAreValid() {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "password");
        TokenResponse expectedResponse = new TokenResponse();
        
        // When & Then
        StepVerifier.create(keycloakService.login(request))
                .expectNext(expectedResponse)
                .verifyComplete();
    }
}
```

### Integration Tests

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport

# Run specific test
./gradlew test --tests "*KeycloakServiceTest"
```

## Build and Deployment

### Gradle Build

```bash
# Build the application
./gradlew build

# Create JAR file
./gradlew bootJar

# Check code quality
./gradlew check
```

### Docker Build

```bash
# Build Docker image
docker build -t recipefy-keycloak-service .

# Run container
docker run -p 8080:8080 \
  -e KEYCLOAK_BASE_URL=http://keycloak:8080 \
  -e KEYCLOAK_REALM=recipefy \
  recipefy-keycloak-service
```

### Multi-stage Dockerfile

```dockerfile
FROM openjdk:21-jdk-slim-bullseye AS build
WORKDIR /src
COPY . .
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /src/build/libs/keycloak.jar /app/keycloak.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "keycloak.jar"]
```

## Monitoring and Health Checks

### Health Endpoints

```bash
# Service health
curl http://localhost:8080/actuator/health

# Detailed health with dependencies
curl http://localhost:8080/actuator/health/readiness
```

### Prometheus Metrics

Available at `/actuator/prometheus`:

- HTTP request metrics
- Keycloak API call metrics
- Token operation counters
- Error rate tracking

### Logging Configuration

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
                <stackTrace/>
            </providers>
        </encoder>
    </appender>
</springProfile>
```

## Security Considerations

### Token Security

- Short-lived access tokens (15-30 minutes)
- Secure refresh token storage
- Token introspection for validation
- Automatic token rotation

### Password Security

- Minimum password requirements
- Secure password hashing (handled by Keycloak)
- Password policy enforcement
- Account lockout protection

### API Security

- CORS configuration for allowed origins
- Input validation and sanitization
- Rate limiting (via API Gateway)
- Request/response logging

## Performance Optimization

### Connection Pooling

```java
@Bean
public WebClient keycloakClient() {
    return WebClient.builder()
            .baseUrl(keycloakProperties.getBaseUrl())
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create().responseTimeout(Duration.ofSeconds(10))
            ))
            .build();
}
```

### Caching Strategy

- Admin token caching with automatic refresh
- User information caching (short-term)
- Connection pool optimization
- Reactive stream optimization

## Troubleshooting

### Common Issues

1. **Keycloak Connection Failed**
   ```bash
   # Check Keycloak availability
   curl http://keycloak:8080/auth/realms/recipefy/.well-known/openid_configuration
   
   # Verify network connectivity
   telnet keycloak 8080
   ```

2. **Authentication Failures**
   ```bash
   # Check client configuration
   # Verify realm settings
   # Confirm user credentials
   ```

3. **Token Issues**
   ```bash
   # Verify token expiration
   # Check refresh token validity
   # Confirm client secret
   ```

### Debug Logging

Enable debug logs for troubleshooting:

```yaml
logging:
  level:
    com.recipefy.keycloak: DEBUG
    org.springframework.web.reactive: DEBUG
    org.springframework.security: DEBUG
```

### Health Diagnostics

```bash
# Check service health
curl http://localhost:8080/actuator/health

# Test authentication endpoint
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'
```

## Integration with Other Services

### API Gateway Integration

The service is accessed through the API Gateway:

```yaml
# Gateway routing configuration
spring:
  cloud:
    gateway:
      routes:
        - id: keycloak-service
          uri: http://keycloak-service:8080
          predicates:
            - Path=/auth/**
```

### Frontend Integration

```javascript
// Login function
const login = async (email, password) => {
  const response = await fetch('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  
  if (response.ok) {
    const tokens = await response.json();
    localStorage.setItem('accessToken', tokens.access_token);
    localStorage.setItem('refreshToken', tokens.refresh_token);
  }
};
```
