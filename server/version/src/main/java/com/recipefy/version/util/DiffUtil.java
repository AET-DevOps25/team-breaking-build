package com.recipefy.version.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.diff.JsonDiff;
import com.recipefy.version.model.mongo.RecipeDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiffUtil {

    private static final Logger logger = LoggerFactory.getLogger(DiffUtil.class);

    public static JsonNode compareRecipes(RecipeDetails oldRecipe, RecipeDetails currentRecipe) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode oldRecipeJson = objectMapper.valueToTree(oldRecipe);
            JsonNode currentRecipeJson = objectMapper.valueToTree(currentRecipe);
            JsonNode diff = JsonDiff.asJson(oldRecipeJson, currentRecipeJson);
            logger.debug("Successfully generated diff with {} operations", diff.size());
            return diff;
        } catch (Exception e) {
            logger.error("Failed to compare recipes", e);
            throw e;
        }
    }
}
