package com.recipefy.recipe.service;

import com.recipefy.recipe.model.dto.RecipeMetadataDTO;
import com.recipefy.recipe.model.dto.RecipeTagDTO;
import com.recipefy.recipe.model.request.InitRecipeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RecipeService {
    Page<RecipeMetadataDTO> getAllRecipes(Pageable pageable);
    RecipeMetadataDTO getRecipe(Long recipeId);
    RecipeMetadataDTO createRecipe(RecipeMetadataDTO metadataDTO, InitRecipeRequest request);
    RecipeMetadataDTO updateRecipe(Long recipeId, RecipeMetadataDTO metadataDTO);
    RecipeMetadataDTO copyRecipe(Long recipeId, Long userId, Long branchId);
    void deleteRecipe(Long recipeId);
    RecipeMetadataDTO updateTags(Long recipeId, List<RecipeTagDTO> tags);
}