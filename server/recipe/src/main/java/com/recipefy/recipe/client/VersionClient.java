package com.recipefy.recipe.client;

import com.recipefy.recipe.model.dto.BranchDTO;
import com.recipefy.recipe.model.request.CopyBranchRequest;
import com.recipefy.recipe.model.request.InitRecipeRequest;
import java.util.UUID;

public interface VersionClient {
    BranchDTO initRecipe(Long recipeId, InitRecipeRequest request, UUID userId);
    BranchDTO copyRecipe(Long branchId, CopyBranchRequest request);
}
