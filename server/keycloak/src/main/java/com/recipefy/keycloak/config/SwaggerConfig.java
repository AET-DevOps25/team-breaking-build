package com.recipefy.keycloak.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Keycloak Authentication API for Recipefy")
                        .version("1.0.0")
                        .description("API documentation for Keycloak Service")
                        .contact(new Contact()
                                .name("Team Breaking Build")
                                .url("https://www.github.com/AET-DevOps25/team-breaking-build"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org"))
                );
    }
} 