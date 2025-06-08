package com.recipefy.recipe.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import com.recipefy.recipe.model.dto.RecipeMetadataDTO;
import com.recipefy.recipe.model.dto.RecipeTagDTO;
import com.recipefy.recipe.model.request.InitRecipeRequest;
import com.recipefy.recipe.service.RecipeService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeController {
    private final RecipeService recipeService;

    @GetMapping
    public ResponseEntity<Page<RecipeMetadataDTO>> getAllRecipes(Pageable pageable) {
        return ResponseEntity.ok(recipeService.getAllRecipes(pageable));
    }

    @GetMapping("/{recipeId}")
    public ResponseEntity<RecipeMetadataDTO> getRecipe(@PathVariable Long recipeId) {
        try {
            return ResponseEntity.ok(recipeService.getRecipe(recipeId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<RecipeMetadataDTO> createRecipe(
            @Valid @RequestBody RecipeMetadataDTO metadataDTO,
            @Valid @RequestBody InitRecipeRequest request) {
        return ResponseEntity.ok(recipeService.createRecipe(metadataDTO, request));
    }

    @PutMapping("/{recipeId}")
    public ResponseEntity<RecipeMetadataDTO> updateRecipe(
            @PathVariable Long recipeId,
            @Valid @RequestBody RecipeMetadataDTO metadataDTO) {
        try {
            return ResponseEntity.ok(recipeService.updateRecipe(recipeId, metadataDTO));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{recipeId}/copy")
    public ResponseEntity<RecipeMetadataDTO> copyRecipe(
            @PathVariable Long recipeId,
            @RequestParam Long userId,
            @RequestParam Long branchId) {
        return ResponseEntity.ok(recipeService.copyRecipe(recipeId, userId, branchId));
    }

    @DeleteMapping("/{recipeId}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long recipeId) {
        recipeService.deleteRecipe(recipeId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{recipeId}/tags")
    public ResponseEntity<RecipeMetadataDTO> updateTags(
            @PathVariable Long recipeId,
            @Valid @RequestBody List<RecipeTagDTO> tags) {
        try {
            return ResponseEntity.ok(recipeService.updateTags(recipeId, tags));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
