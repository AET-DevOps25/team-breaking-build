package com.recipefy.recipe.client;

import java.util.UUID;

import com.recipefy.recipe.model.dto.BranchDTO;
import com.recipefy.recipe.model.request.CopyBranchRequest;
import com.recipefy.recipe.model.request.InitRecipeRequest;

public interface VersionClient {
    BranchDTO initRecipe(Long recipeId, InitRecipeRequest request, UUID userId);
    BranchDTO copyRecipe(Long branchId, CopyBranchRequest request, UUID userId);
}
