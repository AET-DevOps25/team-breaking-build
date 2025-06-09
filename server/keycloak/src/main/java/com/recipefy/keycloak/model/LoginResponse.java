package com.recipefy.keycloak.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private long refreshExpiresIn;
    private String tokenType;
    private String scope;
}
