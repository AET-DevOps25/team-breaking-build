package com.recipefy.keycloak.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserInfoResponse {
    private String sub;
    private String name;
    private String givenName;
    private String familyName;
    private String preferredUsername;
    private String email;
    private Boolean emailVerified;
}
