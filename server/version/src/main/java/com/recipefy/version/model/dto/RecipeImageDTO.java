package com.recipefy.version.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeImageDTO {

    @NotNull(message = "Recipe image url must be provided")
    @NotBlank(message = "Recipe image url must not be empty")
    private String url;
}
