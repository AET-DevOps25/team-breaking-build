package com.recipefy.version.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeImageDTO {

    @NotNull(message = "Recipe image data must be provided")
    private byte[] base64Image;
}
