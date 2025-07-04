package com.recipefy.recipe.client;

import com.recipefy.recipe.model.dto.RecipeDetailsDTO;
import com.recipefy.recipe.model.dto.RecipeMetadataDTO;

public interface GenAIClient {
    void indexRecipe(RecipeMetadataDTO metadata, RecipeDetailsDTO details);
    void deleteRecipe(String recipeId);
} 