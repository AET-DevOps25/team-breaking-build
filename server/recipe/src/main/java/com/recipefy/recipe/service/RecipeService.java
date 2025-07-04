package com.recipefy.recipe.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.recipefy.recipe.model.dto.RecipeMetadataDTO;
import com.recipefy.recipe.model.dto.RecipeTagDTO;
import com.recipefy.recipe.model.request.CreateRecipeRequest;

public interface RecipeService {
    Page<RecipeMetadataDTO> getAllRecipes(Pageable pageable);
    RecipeMetadataDTO getRecipe(Long recipeId);
    RecipeMetadataDTO createRecipe(CreateRecipeRequest request);
    RecipeMetadataDTO updateRecipe(Long recipeId, RecipeMetadataDTO metadataDTO);
    RecipeMetadataDTO copyRecipe(Long recipeId, Long userId, Long branchId);
    void deleteRecipe(Long recipeId);
    RecipeMetadataDTO updateTags(Long recipeId, List<RecipeTagDTO> tags);
}