package com.recipefy.recipe.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Recipe {
    private Long id;
    private RecipeMetadata recipeMetadata;
    private List<RecipeIngredient> recipeIngredients;
    private List<RecipeStep> recipeSteps;
}
