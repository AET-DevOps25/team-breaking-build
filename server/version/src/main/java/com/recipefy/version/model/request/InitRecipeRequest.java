package com.recipefy.version.model.request;

import com.recipefy.version.model.dto.RecipeDetailsDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitRecipeRequest {

    @Valid
    @NotNull(message = "Recipe details are required")
    private RecipeDetailsDTO recipeDetails;
}
