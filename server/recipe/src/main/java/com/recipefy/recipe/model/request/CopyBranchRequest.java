package com.recipefy.recipe.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopyBranchRequest {
    @NotNull(message = "Recipe id is required")
    private Long recipeId;
}
