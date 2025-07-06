package com.recipefy.recipe.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recipefy.recipe.annotation.LogContext;
import com.recipefy.recipe.exception.UnauthorizedException;
import com.recipefy.recipe.model.dto.RecipeMetadataDTO;
import com.recipefy.recipe.model.dto.RecipeTagDTO;
import com.recipefy.recipe.model.request.CreateRecipeRequest;
import com.recipefy.recipe.service.RecipeService;
import com.recipefy.recipe.util.HeaderUtil;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.UUID;

@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeController {
    
    private static final Logger logger = LoggerFactory.getLogger(RecipeController.class);
    private final RecipeService recipeService;

    @GetMapping
    @LogContext(extractUserIdFromHeader = true)
    public ResponseEntity<Page<RecipeMetadataDTO>> getAllRecipes(Pageable pageable) {
        UUID userId = HeaderUtil.extractRequiredUserIdFromHeader();
        logger.debug("Fetching all recipes for user: {}", userId);
        try {
            Page<RecipeMetadataDTO> recipes = recipeService.getAllRecipes(pageable);
            logger.debug("Found {} recipes", recipes.getTotalElements());
            return ResponseEntity.ok(recipes);
        } catch (Exception e) {
            logger.error("Failed to fetch recipes", e);
            throw e;
        }
    }

    @GetMapping("/{recipeId}")
    @LogContext(extractRecipeIdFromPath = true, extractUserIdFromHeader = true)
    public ResponseEntity<RecipeMetadataDTO> getRecipe(@PathVariable Long recipeId) {
        UUID userId = HeaderUtil.extractRequiredUserIdFromHeader();
        logger.debug("Fetching recipe: {} for user: {}", recipeId, userId);
        try {
            RecipeMetadataDTO recipe = recipeService.getRecipe(recipeId);
            logger.debug("Successfully retrieved recipe: {}", recipeId);
            return ResponseEntity.ok(recipe);
        } catch (EntityNotFoundException e) {
            logger.warn("Recipe not found: {} for user: {}", recipeId, userId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to fetch recipe: {}", recipeId, e);
            throw e;
        }
    }

    @PostMapping
    @LogContext(extractUserIdFromHeader = true)
    public ResponseEntity<RecipeMetadataDTO> createRecipe(
            @Valid @RequestBody CreateRecipeRequest request) {
        UUID userId = HeaderUtil.extractRequiredUserIdFromHeader();
        logger.info("Creating recipe with title: {} for user: {}", request.getMetadata().getTitle(), userId);
        try {
            RecipeMetadataDTO recipe = recipeService.createRecipe(request, userId);
            logger.info("Successfully created recipe with ID: {} for user: {}", recipe.getId(), userId);
            return ResponseEntity.ok(recipe);
        } catch (Exception e) {
            logger.error("Failed to create recipe for user: {}", userId, e);
            throw e;
        }
    }

    @PutMapping("/{recipeId}")
    @LogContext(extractRecipeIdFromPath = true, extractUserIdFromHeader = true)
    public ResponseEntity<RecipeMetadataDTO> updateRecipe(
            @PathVariable Long recipeId,
            @Valid @RequestBody RecipeMetadataDTO metadataDTO) {
        UUID userId = HeaderUtil.extractRequiredUserIdFromHeader();
        logger.info("Updating recipe: {} with title: {} for user: {}", recipeId, metadataDTO.getTitle(), userId);
        try {
            RecipeMetadataDTO recipe = recipeService.updateRecipe(recipeId, metadataDTO, userId);
            logger.info("Successfully updated recipe: {} for user: {}", recipeId, userId);
            return ResponseEntity.ok(recipe);
        } catch (EntityNotFoundException e) {
            logger.warn("Recipe not found for update: {} for user: {}", recipeId, userId);
            return ResponseEntity.notFound().build();
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized access attempt to update recipe: {} by user: {}", recipeId, userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Failed to update recipe: {} for user: {}", recipeId, userId, e);
            throw e;
        }
    }

    @PostMapping("/{recipeId}/copy")
    @LogContext(extractRecipeIdFromPath = true, extractUserIdFromHeader = true)
    public ResponseEntity<RecipeMetadataDTO> copyRecipe(
            @PathVariable Long recipeId,
            @RequestParam Long branchId) {
        UUID userId = HeaderUtil.extractRequiredUserIdFromHeader();
        logger.info("Copying recipe: {} to branch: {} for user: {}", recipeId, branchId, userId);
        try {
            RecipeMetadataDTO copiedRecipe = recipeService.copyRecipe(recipeId, userId, branchId);
            logger.info("Successfully copied recipe: {} to new recipe ID: {} for user: {}", recipeId, copiedRecipe.getId(), userId);
            return ResponseEntity.ok(copiedRecipe);
        } catch (Exception e) {
            logger.error("Failed to copy recipe: {} for user: {}", recipeId, userId, e);
            throw e;
        }
    }

    @DeleteMapping("/{recipeId}")
    @LogContext(extractRecipeIdFromPath = true, extractUserIdFromHeader = true)
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long recipeId) {
        UUID userId = HeaderUtil.extractRequiredUserIdFromHeader();
        logger.info("Deleting recipe: {} for user: {}", recipeId, userId);
        try {
            recipeService.deleteRecipe(recipeId, userId);
            logger.info("Successfully deleted recipe: {} for user: {}", recipeId, userId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            logger.warn("Recipe not found for deletion: {} by user: {}", recipeId, userId);
            return ResponseEntity.notFound().build();
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized access attempt to delete recipe: {} by user: {}", recipeId, userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Failed to delete recipe: {} for user: {}", recipeId, userId, e);
            throw e;
        }
    }

    @PutMapping("/{recipeId}/tags")
    @LogContext(extractRecipeIdFromPath = true, extractUserIdFromHeader = true)
    public ResponseEntity<RecipeMetadataDTO> updateTags(
            @PathVariable Long recipeId,
            @Valid @RequestBody List<RecipeTagDTO> tags) {
        UUID userId = HeaderUtil.extractRequiredUserIdFromHeader();
        logger.info("Updating tags for recipe: {} with {} tags for user: {}", recipeId, tags.size(), userId);
        try {
            RecipeMetadataDTO recipe = recipeService.updateTags(recipeId, tags, userId);
            logger.info("Successfully updated tags for recipe: {} for user: {}", recipeId, userId);
            return ResponseEntity.ok(recipe);
        } catch (EntityNotFoundException e) {
            logger.warn("Recipe not found for tag update: {} for user: {}", recipeId, userId);
            return ResponseEntity.notFound().build();
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized access attempt to update tags for recipe: {} by user: {}", recipeId, userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Failed to update tags for recipe: {} for user: {}", recipeId, userId, e);
            throw e;
        }
    }
}
