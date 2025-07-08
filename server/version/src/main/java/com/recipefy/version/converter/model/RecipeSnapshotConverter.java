package com.recipefy.version.converter.model;

import com.recipefy.version.model.dto.RecipeDetailsDTO;
import com.recipefy.version.model.dto.RecipeImageDTO;
import com.recipefy.version.model.dto.RecipeIngredientDTO;
import com.recipefy.version.model.dto.RecipeStepDTO;
import com.recipefy.version.model.mongo.RecipeDetails;
import com.recipefy.version.model.mongo.RecipeImage;
import com.recipefy.version.model.mongo.RecipeIngredient;
import com.recipefy.version.model.mongo.RecipeSnapshot;
import com.recipefy.version.model.mongo.RecipeStep;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RecipeSnapshotConverter {

    public static RecipeSnapshot toDocument(Long commitId, Long recipeId, RecipeDetailsDTO recipeDetailsDTO) {
        RecipeSnapshot recipe = new RecipeSnapshot();
        RecipeDetails recipeDetails = new RecipeDetails();

        recipeDetails.setServingSize(recipeDetailsDTO.getServingSize());
        recipeDetails.setImages(mapRecipeImages(recipeDetailsDTO.getImages()));
        recipeDetails.setRecipeIngredients(mapRecipeIngredients(recipeDetailsDTO.getRecipeIngredients()));
        recipeDetails.setRecipeSteps(mapRecipeSteps(recipeDetailsDTO.getRecipeSteps()));

        recipe.setId(commitId.toString());
        recipe.setRecipeId(recipeId);
        recipe.setDetails(recipeDetails);

        return recipe;
    }

    private static List<RecipeImage> mapRecipeImages(List<RecipeImageDTO> recipeImageDTOS) {
        return Optional.ofNullable(recipeImageDTOS)
                .orElse(Collections.emptyList())
                .stream()
                .map(RecipeSnapshotConverter::mapRecipeImage)
                .collect(Collectors.toList());
    }

    private static RecipeImage mapRecipeImage(RecipeImageDTO recipeImageDTO) {
        RecipeImage image = new RecipeImage();
        image.setBase64Image(recipeImageDTO.getBase64Image());
        return image;
    }

    private static List<RecipeIngredient> mapRecipeIngredients(List<RecipeIngredientDTO> recipeIngredientDTOS) {
        return Optional.ofNullable(recipeIngredientDTOS)
                .orElse(Collections.emptyList())
                .stream()
                .map(RecipeSnapshotConverter::mapRecipeIngredient)
                .collect(Collectors.toList());
    }

    private static RecipeIngredient mapRecipeIngredient(RecipeIngredientDTO recipeIngredientDTO) {
        RecipeIngredient ingredient = new RecipeIngredient();
        ingredient.setName(recipeIngredientDTO.getName());
        ingredient.setUnit(recipeIngredientDTO.getUnit());
        ingredient.setAmount(recipeIngredientDTO.getAmount());
        return ingredient;
    }

    private static List<RecipeStep> mapRecipeSteps(List<RecipeStepDTO> recipeStepDTOS) {
        return Optional.ofNullable(recipeStepDTOS)
                .orElse(Collections.emptyList())
                .stream()
                .map(RecipeSnapshotConverter::mapRecipeStep)
                .collect(Collectors.toList());
    }

    private static RecipeStep mapRecipeStep(RecipeStepDTO recipeStepDTO) {
        RecipeStep step = new RecipeStep();
        step.setOrder(recipeStepDTO.getOrder());
        step.setDetails(recipeStepDTO.getDetails());
        step.setImages(mapRecipeImages(recipeStepDTO.getImages()));
        return step;
    }
}
