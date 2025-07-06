package com.recipefy.keycloak.service;

import com.recipefy.keycloak.config.KeycloakProperties;
import com.recipefy.keycloak.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakService {
    private final KeycloakProperties properties;
    private final WebClient keycloakClient;
    private final WebClient keycloakAdminClient;

    public Mono<TokenResponse> login(LoginRequest request) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", properties.getCredentials().getRecipefy().getClientId());
        body.add("client_secret", properties.getCredentials().getRecipefy().getClientSecret());
        body.add("username", request.getEmail());
        body.add("password", request.getPassword());
        body.add("scope", "openid");

        return keycloakClient.post()
                .uri(properties.getKeycloakUri() + "/token")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(TokenResponse.class);
    }

    public Mono<ResponseEntity<Void>> register(RegisterRequest request) {
        UserPayload userPayload = UserPayload.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enabled(true)
                .emailVerified(true)
                .credentials(List.of(
                        UserPayload.Credential.builder()
                                .type("password")
                                .value(request.getPassword())
                                .temporary(false)
                                .build()
                ))
                .build();

        return keycloakAdminClient.post()
                .uri(properties.getAdminUri() + "/users")
                .bodyValue(userPayload)
                .retrieve()
                .toBodilessEntity();
    }

    public Mono<UserInfoResponse> userInfo(String token) {
        return keycloakClient.get()
                .uri(properties.getKeycloakUri() + "/userinfo")
                .headers(header-> header.setBearerAuth(token))
                .retrieve()
                .bodyToMono(UserInfoResponse.class);
    }

    public Mono<TokenResponse> refreshToken(RefreshTokenRequest request) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", properties.getCredentials().getRecipefy().getClientId());
        body.add("client_secret", properties.getCredentials().getRecipefy().getClientSecret());
        body.add("refresh_token", request.getToken());

        return keycloakClient.post()
                .uri(properties.getKeycloakUri() + "/token")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(TokenResponse.class);
    }
}
