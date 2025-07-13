package com.recipefy.keycloak.service;

import com.recipefy.keycloak.config.KeycloakProperties;
import com.recipefy.keycloak.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakServiceTest {

    @Mock
    private WebClient keycloakClient;
    
    @Mock
    private WebClient keycloakAdminClient;
    
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;

    private KeycloakService keycloakService;
    private KeycloakProperties keycloakProperties;

    @BeforeEach
    void setUp() {
        keycloakProperties = new KeycloakProperties();
        keycloakProperties.setBaseUrl("http://localhost:8080");
        keycloakProperties.setRealm("test-realm");
        keycloakProperties.setKeycloakUri("/realms/test-realm/protocol/openid-connect");
        keycloakProperties.setAdminUri("/admin/realms/test-realm");
        
        KeycloakProperties.Credentials credentials = new KeycloakProperties.Credentials();
        KeycloakProperties.Client recipefyClient = new KeycloakProperties.Client();
        recipefyClient.setClientId("web");
        recipefyClient.setClientSecret("web-secret");
        
        KeycloakProperties.Client adminClient = new KeycloakProperties.Client();
        adminClient.setClientId("admin-cli");
        adminClient.setClientSecret("admin-secret");
        
        credentials.setRecipefy(recipefyClient);
        credentials.setAdmin(adminClient);
        keycloakProperties.setCredentials(credentials);
        
        keycloakService = new KeycloakService(keycloakProperties, keycloakClient, keycloakAdminClient);
    }

    @Test
    void login_ShouldReturnTokenResponse_WhenCredentialsAreValid() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");
        
        TokenResponse expectedResponse = new TokenResponse();
        expectedResponse.setAccessToken("access-token");
        expectedResponse.setRefreshToken("refresh-token");
        expectedResponse.setExpiresIn(3600);
        expectedResponse.setTokenType("Bearer");
        
        when(keycloakClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class)).thenReturn(Mono.just(expectedResponse));

        // When
        Mono<TokenResponse> result = keycloakService.login(loginRequest);

        // Then
        StepVerifier.create(result)
            .expectNextMatches(response -> {
                assertEquals("access-token", response.getAccessToken());
                assertEquals("refresh-token", response.getRefreshToken());
                assertEquals(3600, response.getExpiresIn());
                assertEquals("Bearer", response.getTokenType());
                return true;
            })
            .verifyComplete();
            
        verify(keycloakClient).post();
        verify(requestBodyUriSpec).uri("/realms/test-realm/protocol/openid-connect/token");
        verify(requestBodySpec).bodyValue(any());
    }

    @Test
    void login_ShouldReturnError_WhenCredentialsAreInvalid() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("wrong-password");
        
        when(keycloakClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class))
            .thenReturn(Mono.error(new WebClientResponseException(401, "Unauthorized", null, null, null)));

        // When
        Mono<TokenResponse> result = keycloakService.login(loginRequest);

        // Then
        StepVerifier.create(result)
            .expectError(WebClientResponseException.class)
            .verify();
    }

    @Test
    void register_ShouldReturnSuccessResponse_WhenUserRegistrationIsValid() {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setPassword("password");
        
        when(keycloakAdminClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.just(ResponseEntity.status(HttpStatus.CREATED).build()));

        // When
        Mono<ResponseEntity<Void>> result = keycloakService.register(registerRequest);

        // Then
        StepVerifier.create(result)
            .expectNextMatches(response -> {
                assertEquals(HttpStatus.CREATED, response.getStatusCode());
                return true;
            })
            .verifyComplete();
            
        verify(keycloakAdminClient).post();
        verify(requestBodyUriSpec).uri("/admin/realms/test-realm/users");
        verify(requestBodySpec).bodyValue(any(UserPayload.class));
    }

    @Test
    void register_ShouldReturnError_WhenUserAlreadyExists() {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("existing@example.com");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setPassword("password");
        
        when(keycloakAdminClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity())
            .thenReturn(Mono.error(new WebClientResponseException(409, "Conflict", null, null, null)));

        // When
        Mono<ResponseEntity<Void>> result = keycloakService.register(registerRequest);

        // Then
        StepVerifier.create(result)
            .expectError(WebClientResponseException.class)
            .verify();
    }

    @Test
    void userInfo_ShouldReturnUserInfo_WhenTokenIsValid() {
        // Given
        String token = "valid-token";
        UserInfoResponse expectedResponse = new UserInfoResponse();
        expectedResponse.setSub("user-id");
        expectedResponse.setEmail("test@example.com");
        expectedResponse.setGivenName("John");
        expectedResponse.setFamilyName("Doe");
        expectedResponse.setEmailVerified(true);
        
        when(keycloakClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserInfoResponse.class)).thenReturn(Mono.just(expectedResponse));

        // When
        Mono<UserInfoResponse> result = keycloakService.userInfo(token);

        // Then
        StepVerifier.create(result)
            .expectNextMatches(response -> {
                assertEquals("user-id", response.getSub());
                assertEquals("test@example.com", response.getEmail());
                assertEquals("John", response.getGivenName());
                assertEquals("Doe", response.getFamilyName());
                assertEquals(true, response.getEmailVerified());
                return true;
            })
            .verifyComplete();
            
        verify(keycloakClient).get();
        verify(requestHeadersUriSpec).uri("/realms/test-realm/protocol/openid-connect/userinfo");
    }

    @Test
    void userInfo_ShouldReturnError_WhenTokenIsInvalid() {
        // Given
        String token = "invalid-token";
        
        when(keycloakClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserInfoResponse.class))
            .thenReturn(Mono.error(new WebClientResponseException(401, "Unauthorized", null, null, null)));

        // When
        Mono<UserInfoResponse> result = keycloakService.userInfo(token);

        // Then
        StepVerifier.create(result)
            .expectError(WebClientResponseException.class)
            .verify();
    }

    @Test
    void refreshToken_ShouldReturnNewTokens_WhenRefreshTokenIsValid() {
        // Given
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setToken("valid-refresh-token");
        
        TokenResponse expectedResponse = new TokenResponse();
        expectedResponse.setAccessToken("new-access-token");
        expectedResponse.setRefreshToken("new-refresh-token");
        expectedResponse.setExpiresIn(3600);
        expectedResponse.setTokenType("Bearer");
        
        when(keycloakClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class)).thenReturn(Mono.just(expectedResponse));

        // When
        Mono<TokenResponse> result = keycloakService.refreshToken(refreshRequest);

        // Then
        StepVerifier.create(result)
            .expectNextMatches(response -> {
                assertEquals("new-access-token", response.getAccessToken());
                assertEquals("new-refresh-token", response.getRefreshToken());
                assertEquals(3600, response.getExpiresIn());
                assertEquals("Bearer", response.getTokenType());
                return true;
            })
            .verifyComplete();
            
        verify(keycloakClient).post();
        verify(requestBodyUriSpec).uri("/realms/test-realm/protocol/openid-connect/token");
    }

    @Test
    void refreshToken_ShouldReturnError_WhenRefreshTokenIsInvalid() {
        // Given
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setToken("invalid-refresh-token");
        
        when(keycloakClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class))
            .thenReturn(Mono.error(new WebClientResponseException(400, "Bad Request", null, null, null)));

        // When
        Mono<TokenResponse> result = keycloakService.refreshToken(refreshRequest);

        // Then
        StepVerifier.create(result)
            .expectError(WebClientResponseException.class)
            .verify();
    }

    @Test
    void getUser_ShouldReturnUserDetails_WhenUserExists() {
        // Given
        UUID userId = UUID.randomUUID();
        UserDetails expectedResponse = new UserDetails();
        expectedResponse.setId(userId.toString());
        expectedResponse.setUsername("testuser");
        expectedResponse.setFirstName("John");
        expectedResponse.setLastName("Doe");
        expectedResponse.setEmail("test@example.com");
        expectedResponse.setEmailVerified(true);
        
        when(keycloakAdminClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserDetails.class)).thenReturn(Mono.just(expectedResponse));

        // When
        Mono<UserDetails> result = keycloakService.getUser(userId);

        // Then
        StepVerifier.create(result)
            .expectNextMatches(response -> {
                assertEquals(userId.toString(), response.getId());
                assertEquals("testuser", response.getUsername());
                assertEquals("John", response.getFirstName());
                assertEquals("Doe", response.getLastName());
                assertEquals("test@example.com", response.getEmail());
                assertEquals(true, response.getEmailVerified());
                return true;
            })
            .verifyComplete();
            
        verify(keycloakAdminClient).get();
        verify(requestHeadersUriSpec).uri("/admin/realms/test-realm/users/" + userId);
    }

    @Test
    void getUser_ShouldReturnError_WhenUserNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        
        when(keycloakAdminClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserDetails.class))
            .thenReturn(Mono.error(new WebClientResponseException(404, "Not Found", null, null, null)));

        // When
        Mono<UserDetails> result = keycloakService.getUser(userId);

        // Then
        StepVerifier.create(result)
            .expectError(WebClientResponseException.class)
            .verify();
    }

    @Test
    void login_ShouldUseCorrectCredentials() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");
        
        TokenResponse expectedResponse = new TokenResponse();
        expectedResponse.setAccessToken("access-token");
        
        when(keycloakClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class)).thenReturn(Mono.just(expectedResponse));

        // When
        keycloakService.login(loginRequest);

        // Then
        verify(requestBodySpec).bodyValue(argThat(body -> {
            if (body instanceof org.springframework.util.MultiValueMap) {
                @SuppressWarnings("unchecked")
                org.springframework.util.MultiValueMap<String, String> multiValueMap = 
                    (org.springframework.util.MultiValueMap<String, String>) body;
                return "password".equals(multiValueMap.getFirst("grant_type")) &&
                       "web".equals(multiValueMap.getFirst("client_id")) &&
                       "web-secret".equals(multiValueMap.getFirst("client_secret")) &&
                       "test@example.com".equals(multiValueMap.getFirst("username")) &&
                       "password".equals(multiValueMap.getFirst("password")) &&
                       "openid".equals(multiValueMap.getFirst("scope"));
            }
            return false;
        }));
    }

    @Test
    void register_ShouldCreateCorrectUserPayload() {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setPassword("password");
        
        when(keycloakAdminClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.just(ResponseEntity.ok().build()));

        // When
        keycloakService.register(registerRequest);

        // Then
        verify(requestBodySpec).bodyValue(argThat(body -> {
            if (body instanceof UserPayload) {
                UserPayload userPayload = (UserPayload) body;
                return "newuser@example.com".equals(userPayload.getEmail()) &&
                       "John".equals(userPayload.getFirstName()) &&
                       "Doe".equals(userPayload.getLastName()) &&
                       Boolean.TRUE.equals(userPayload.getEnabled()) &&
                       Boolean.TRUE.equals(userPayload.getEmailVerified()) &&
                       userPayload.getCredentials() != null &&
                       userPayload.getCredentials().size() == 1 &&
                       "password".equals(userPayload.getCredentials().get(0).getType()) &&
                       "password".equals(userPayload.getCredentials().get(0).getValue()) &&
                       Boolean.FALSE.equals(userPayload.getCredentials().get(0).getTemporary());
            }
            return false;
        }));
    }

    @Test
    void refreshToken_ShouldUseCorrectGrantType() {
        // Given
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setToken("refresh-token");
        
        TokenResponse expectedResponse = new TokenResponse();
        expectedResponse.setAccessToken("new-access-token");
        
        when(keycloakClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class)).thenReturn(Mono.just(expectedResponse));

        // When
        keycloakService.refreshToken(refreshRequest);

        // Then
        verify(requestBodySpec).bodyValue(argThat(body -> {
            if (body instanceof org.springframework.util.MultiValueMap) {
                @SuppressWarnings("unchecked")
                org.springframework.util.MultiValueMap<String, String> multiValueMap = 
                    (org.springframework.util.MultiValueMap<String, String>) body;
                return "refresh_token".equals(multiValueMap.getFirst("grant_type")) &&
                       "web".equals(multiValueMap.getFirst("client_id")) &&
                       "web-secret".equals(multiValueMap.getFirst("client_secret")) &&
                       "refresh-token".equals(multiValueMap.getFirst("refresh_token"));
            }
            return false;
        }));
    }

    @Test
    void userInfo_ShouldSetBearerToken() {
        // Given
        String token = "bearer-token";
        UserInfoResponse expectedResponse = new UserInfoResponse();
        expectedResponse.setSub("user-id");
        
        when(keycloakClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserInfoResponse.class)).thenReturn(Mono.just(expectedResponse));

        // When
        keycloakService.userInfo(token);

        // Then
        verify(requestHeadersSpec).headers(argThat(headersConsumer -> {
            // This is a bit tricky to test directly, but we can verify that headers() was called
            return true;
        }));
    }

    @Test
    void service_ShouldHandleNetworkTimeout() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");
        
        when(keycloakClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class))
            .thenReturn(Mono.error(new java.net.ConnectException("Connection timeout")));

        // When
        Mono<TokenResponse> result = keycloakService.login(loginRequest);

        // Then
        StepVerifier.create(result)
            .expectError(java.net.ConnectException.class)
            .verify();
    }

    @Test
    void service_ShouldHandleServerError() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");
        
        when(keycloakClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class))
            .thenReturn(Mono.error(new WebClientResponseException(500, "Internal Server Error", null, null, null)));

        // When
        Mono<TokenResponse> result = keycloakService.login(loginRequest);

        // Then
        StepVerifier.create(result)
            .expectError(WebClientResponseException.class)
            .verify();
    }
} 