package com.recipefy.gateway.model

data class TokenRequest(
    val username: String,
    val password: String?,
    val grantType: String,
    val refreshToken: String?,
)
