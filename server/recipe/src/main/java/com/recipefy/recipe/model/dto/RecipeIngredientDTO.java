package com.recipefy.recipe.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Only for VCS Service propagation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredientDTO {
    private String name;
    private String unit;
    private Float amount;
}
