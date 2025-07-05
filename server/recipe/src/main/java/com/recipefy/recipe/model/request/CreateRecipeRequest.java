package com.recipefy.recipe.model.request;

import com.recipefy.recipe.model.dto.RecipeMetadataDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecipeRequest {
    @Valid
    @NotNull(message = "Recipe metadata is required")
    private RecipeMetadataDTO metadata;
    
    @Valid
    @NotNull(message = "Init recipe request is required")
    private InitRecipeRequest initRequest;
} 
