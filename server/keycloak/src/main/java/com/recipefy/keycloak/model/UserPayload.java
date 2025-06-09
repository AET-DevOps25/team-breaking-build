package com.recipefy.keycloak.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserPayload {
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private Boolean enabled;
    private Boolean emailVerified;
    private List<Credential> credentials;

    @Data
    @Builder
    public static class Credential {
        private String type;
        private String value;
        private Boolean temporary;
    }
}
