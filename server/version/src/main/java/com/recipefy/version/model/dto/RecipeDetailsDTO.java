package com.recipefy.version.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeDetailsDTO {

    @NotNull(message = "Serving size is required")
    @Min(value = 1, message = "Serving size must be at least 1")
    private int servingSize;

    private List<@Valid RecipeImageDTO> images;

    @NotNull(message = "Recipe ingredients are required")
    @Size(min = 1, message = "Recipe must contain at least 1 ingredient")
    private List<@Valid RecipeIngredientDTO> recipeIngredients;

    @NotNull(message = "Recipe steps are required")
    @Size(min = 1, message = "Recipe must contain at least 1 step")
    private List<@Valid RecipeStepDTO> recipeSteps;
}
