package com.recipefy.gateway.controller

import com.recipefy.gateway.model.TokenRequest
import com.recipefy.gateway.model.TokenResponse
import com.recipefy.gateway.service.AuthService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/token")
    fun createToken(
        @RequestBody tokenRequest: TokenRequest
    ): Mono<TokenResponse> {
        return authService.getToken(tokenRequest)
    }
}
