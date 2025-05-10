package com.recipefy.recipe.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStep {
    private Integer order;
    private String details;
    private List<RecipeImage> recipeImages;
}
