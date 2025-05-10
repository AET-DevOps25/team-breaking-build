package com.recipefy.recipe.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeMetadata {
    private Integer servingSize;
    private String title;
    private String description;
    private RecipeImage recipeImage;
}
