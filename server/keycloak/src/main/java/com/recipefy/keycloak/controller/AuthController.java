package com.recipefy.keycloak.controller;

import com.recipefy.keycloak.model.*;
import com.recipefy.keycloak.service.KeycloakService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakService keycloakService;

    @PostMapping("/login")
    public Mono<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return keycloakService.login(request);
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<Void>> register(@Valid @RequestBody RegisterRequest request) {
        return keycloakService.register(request);
    }

    @GetMapping("/userinfo")
    public Mono<UserInfoResponse> userInfo(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        if (!authorization.startsWith("Bearer ")) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Bearer token"));
        }
        String token = authorization.substring(7);
        return keycloakService.userInfo(token);
    }

    @PostMapping("/refresh")
    public Mono<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return keycloakService.refreshToken(request);
    }
}
