package com.recipefy.recipe.client;

import com.recipefy.recipe.model.request.CopyBranchRequest;
import com.recipefy.recipe.model.request.InitRecipeRequest;

public interface VersionClient {
    void initRecipe(Long recipeId, InitRecipeRequest request);
    void copyRecipe(Long branchId, CopyBranchRequest request);
}
