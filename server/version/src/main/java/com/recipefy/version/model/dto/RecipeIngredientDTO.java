package com.recipefy.version.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredientDTO {

    @NotNull(message = "Ingredient name must be provided")
    @NotBlank(message = "Ingredient name must not be empty")
    private String name;

    @NotNull(message = "Ingredient unit must be provided")
    @NotBlank(message = "Ingredient unit must not be empty")
    private String unit;

    @NotNull
    @DecimalMin(value = "0", inclusive = false, message = "Ingredient amount must be greater than 0")
    private Float amount;
}
