package com.recipefy.version.converter.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.recipefy.version.converter.dto.RecipeDetailsDTOConverter;
import com.recipefy.version.model.mongo.RecipeDetails;
import com.recipefy.version.model.response.ChangeResponse;

public class ChangeResponseConverter {

    public static ChangeResponse apply(RecipeDetails oldRecipeDetails, RecipeDetails currentRecipeDetails, JsonNode changes, boolean firstCommit) {
        ChangeResponse changeResponse = new ChangeResponse();
        changeResponse.setCurrentDetails(RecipeDetailsDTOConverter.toDTO(currentRecipeDetails));
        changeResponse.setOldDetails(RecipeDetailsDTOConverter.toDTO(oldRecipeDetails));
        changeResponse.setChanges(changes);
        changeResponse.setFirstCommit(firstCommit);
        return changeResponse;
    }
}
