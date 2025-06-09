package com.recipefy.keycloak.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakAdminConfig {
    private final KeycloakProperties keycloakProperties;
    private final KeycloakAdminExchangeFilter adminExchangeFilter;

    @Bean
    public WebClient keycloakAdminClient() {
        return WebClient.builder()
                .baseUrl(keycloakProperties.getBaseUrl())
                .filter(adminExchangeFilter)
                .build();
    }
}
