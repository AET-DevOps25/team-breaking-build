package com.recipefy.version.model.request;

import com.recipefy.version.model.dto.RecipeDetailsDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommitToBranchRequest {

    @NotNull(message = "Message is required")
    @NotBlank(message = "Message is required")
    private String message;

    @Valid
    @NotNull(message = "Recipe details are required")
    private RecipeDetailsDTO recipeDetails;
}
