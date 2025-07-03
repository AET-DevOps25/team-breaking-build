package com.recipefy.version.model.dto;

import com.recipefy.version.model.mongo.RecipeImage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStepDTO {

    @NotNull(message = "Recipe step order must be provided")
    @Min(value = 1, message = "Recipe step order must be at least 1")
    private int order;

    @NotNull(message = "Recipe step details must be provided")
    @NotBlank(message = "Recipe step details must not be empty")
    private String details;

    private List<@Valid RecipeImageDTO> images;
}
