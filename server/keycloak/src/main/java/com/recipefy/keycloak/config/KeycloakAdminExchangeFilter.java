package com.recipefy.keycloak.config;

import com.recipefy.keycloak.model.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class KeycloakAdminExchangeFilter implements ExchangeFilterFunction {
    private final KeycloakProperties keycloakProperties;
    private final WebClient keycloakClient;

    private final AtomicReference<CachedToken> cache = new AtomicReference<>();

    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return getAdminToken()
                .map(token -> ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build()
                )
                .flatMap(next::exchange);
    }

    private Mono<String> getAdminToken() {
        CachedToken current = cache.get();

        // refresh when nonexistent or about to expire (<30 s left)
        if (current == null || Instant.now().isAfter(current.expiresAt.minusSeconds(60))) {
            return getNewToken()
                    .doOnNext(cache::set)
                    .map(t -> t.token);
        }
        return Mono.just(current.token);
    }

    private Mono<CachedToken> getNewToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", keycloakProperties.getCredentials().getAdmin().getClientId());
        form.add("client_secret", keycloakProperties.getCredentials().getAdmin().getClientSecret());

        return keycloakClient.post()
                .uri(keycloakProperties.getKeycloakUri() + "/token")
                .bodyValue(form)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .map(body -> {
                    String access = body.getAccessToken();
                    long expiresIn = body.getExpiresIn();
                    Instant exp = Instant.now().plusSeconds(expiresIn);
                    return new CachedToken(access, exp);
                });
    }

    private record CachedToken(String token, Instant expiresAt) {}
}
