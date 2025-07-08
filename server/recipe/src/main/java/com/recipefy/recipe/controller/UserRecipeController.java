package com.recipefy.recipe.controller;

import com.recipefy.recipe.model.dto.RecipeMetadataDTO;
import com.recipefy.recipe.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users/{userId}/recipes")
@RequiredArgsConstructor
public class UserRecipeController {
    private static final Logger logger = LoggerFactory.getLogger(UserRecipeController.class);
    private final RecipeService recipeService;

    @GetMapping
    public ResponseEntity<Page<RecipeMetadataDTO>> getUserRecipes(@PathVariable UUID userId, Pageable pageable) {
        logger.debug("Fetching all recipes for user: {}", userId);
        try {
            Page<RecipeMetadataDTO> recipes = recipeService.getAllRecipes(userId, pageable);
            logger.debug("Found {} recipes for user {}", recipes.getTotalElements(), userId);
            return ResponseEntity.ok(recipes);
        } catch (Exception e) {
            logger.error("Failed to fetch recipes for user: {}", userId, e);
            throw e;
        }
    }
} 