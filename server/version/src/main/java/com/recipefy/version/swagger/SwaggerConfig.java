package com.recipefy.version.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Version Control API for Recipefy")
                        .version("1.0.0")
                        .description("API documentation for Your App")
                        .contact(new Contact()
                                .name("Team Breaking Build")
                                .url("https://www.github.com/AET-DevOps25/team-breaking-build"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org"))
                );
    }
}
