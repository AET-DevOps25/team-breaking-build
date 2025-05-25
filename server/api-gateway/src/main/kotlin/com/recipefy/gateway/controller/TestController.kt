package com.recipefy.gateway.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
class TestController {
    @GetMapping
    fun get(): ResponseEntity<String> = ResponseEntity.ok().build()
}
