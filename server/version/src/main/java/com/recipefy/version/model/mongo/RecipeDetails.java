package com.recipefy.version.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeDetails {

    private int servingSize;

    private List<RecipeImage> images;

    private List<RecipeIngredient> recipeIngredients;

    private List<RecipeStep> recipeSteps;
}
