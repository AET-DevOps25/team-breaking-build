package com.recipefy.keycloak.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {
    private String baseUrl;
    private String realm;
    private String adminUri;
    private String keycloakUri;
    private Credentials credentials;

    @Data
    public static class Credentials {
        private Client recipefy;
        private Client admin;
    }

    @Data
    public static class Client {
        private String clientId;
        private String clientSecret;
    }
}
