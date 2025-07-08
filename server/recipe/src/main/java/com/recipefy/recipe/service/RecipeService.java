package com.recipefy.recipe.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.recipefy.recipe.model.dto.RecipeMetadataDTO;
import com.recipefy.recipe.model.dto.RecipeTagDTO;
import com.recipefy.recipe.model.request.CreateRecipeRequest;

public interface RecipeService {
    Page<RecipeMetadataDTO> getAllRecipes(Pageable pageable);
    Page<RecipeMetadataDTO> getAllRecipes(UUID userId, Pageable pageable);
    RecipeMetadataDTO getRecipe(Long recipeId);
    RecipeMetadataDTO createRecipe(CreateRecipeRequest request, UUID userId);
    RecipeMetadataDTO updateRecipe(Long recipeId, RecipeMetadataDTO metadataDTO, UUID userId);
    RecipeMetadataDTO copyRecipe(Long recipeId, UUID userId, Long branchId);
    void deleteRecipe(Long recipeId, UUID userId);
    List<RecipeTagDTO> getAllTags();
}