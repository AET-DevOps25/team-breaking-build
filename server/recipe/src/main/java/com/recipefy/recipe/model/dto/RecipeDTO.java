package com.recipefy.recipe.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeDTO {
    private RecipeMetadataDTO recipeMetadata;
    private List<RecipeIngredientDTO> recipeIngredientDTOS;
    private List<RecipeStepDTO> recipeStepDTOS;
}
