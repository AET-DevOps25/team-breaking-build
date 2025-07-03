package com.recipefy.version.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredient {

    private String name;

    private String unit;

    private Float amount;
}
