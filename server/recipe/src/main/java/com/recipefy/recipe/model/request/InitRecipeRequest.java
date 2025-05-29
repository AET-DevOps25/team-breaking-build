package com.recipefy.recipe.model.request;

import com.recipefy.recipe.model.dto.RecipeDetailsDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitRecipeRequest {
    @NotNull(message = "User id is required")
    private Long userId;

    @Valid
    @NotNull(message = "Recipe details are required")
    private RecipeDetailsDTO recipeDetails;
}