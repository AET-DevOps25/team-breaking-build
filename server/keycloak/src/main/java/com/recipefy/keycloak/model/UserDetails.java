package com.recipefy.keycloak.model;

import lombok.Data;

@Data
public class UserDetails {
    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean emailVerified;
}
