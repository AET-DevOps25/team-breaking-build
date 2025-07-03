package com.recipefy.version.converter.response;

import com.recipefy.version.converter.dto.CommitDTOConverter;
import com.recipefy.version.converter.dto.RecipeDetailsDTOConverter;
import com.recipefy.version.model.mongo.RecipeSnapshot;
import com.recipefy.version.model.postgres.Commit;
import com.recipefy.version.model.response.CommitDetailsResponse;

public class CommitDetailsResponseConverter {

    public static CommitDetailsResponse apply(Commit commit, RecipeSnapshot recipeSnapshot) {
        CommitDetailsResponse commitDetailsResponse = new CommitDetailsResponse();
        commitDetailsResponse.setCommitMetadata(CommitDTOConverter.toDTO(commit));
        commitDetailsResponse.setRecipeDetails(RecipeDetailsDTOConverter.toDTO(recipeSnapshot.getDetails()));
        return commitDetailsResponse;
    }
}
