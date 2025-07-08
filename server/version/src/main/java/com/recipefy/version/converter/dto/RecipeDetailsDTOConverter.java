package com.recipefy.version.converter.dto;

import com.recipefy.version.model.dto.RecipeDetailsDTO;
import com.recipefy.version.model.dto.RecipeImageDTO;
import com.recipefy.version.model.dto.RecipeIngredientDTO;
import com.recipefy.version.model.dto.RecipeStepDTO;
import com.recipefy.version.model.mongo.RecipeDetails;
import com.recipefy.version.model.mongo.RecipeImage;
import com.recipefy.version.model.mongo.RecipeIngredient;
import com.recipefy.version.model.mongo.RecipeStep;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RecipeDetailsDTOConverter {

    public static RecipeDetailsDTO toDTO(RecipeDetails recipeDetails) {
        RecipeDetailsDTO recipeDetailsDTO = new RecipeDetailsDTO();

        recipeDetailsDTO.setServingSize(recipeDetails.getServingSize());
        recipeDetailsDTO.setImages(mapRecipeImages(recipeDetails.getImages()));
        recipeDetailsDTO.setRecipeIngredients(mapRecipeIngredients(recipeDetails.getRecipeIngredients()));
        recipeDetailsDTO.setRecipeSteps(mapRecipeSteps(recipeDetails.getRecipeSteps()));

        return recipeDetailsDTO;
    }


    private static List<RecipeImageDTO> mapRecipeImages(List<RecipeImage> recipeImages) {
        return Optional.ofNullable(recipeImages)
                .orElse(Collections.emptyList())
                .stream()
                .map(RecipeDetailsDTOConverter::mapRecipeImage)
                .collect(Collectors.toList());
    }

    private static RecipeImageDTO mapRecipeImage(RecipeImage recipeImage) {
        RecipeImageDTO image = new RecipeImageDTO();
        image.setBase64Image(recipeImage.getBase64Image());
        return image;
    }

    private static List<RecipeIngredientDTO> mapRecipeIngredients(List<RecipeIngredient> recipeIngredients) {
        return Optional.ofNullable(recipeIngredients)
                .orElse(Collections.emptyList())
                .stream()
                .map(RecipeDetailsDTOConverter::mapRecipeIngredient)
                .collect(Collectors.toList());
    }

    private static RecipeIngredientDTO mapRecipeIngredient(RecipeIngredient recipeIngredient) {
        RecipeIngredientDTO ingredient = new RecipeIngredientDTO();
        ingredient.setName(recipeIngredient.getName());
        ingredient.setUnit(recipeIngredient.getUnit());
        ingredient.setAmount(recipeIngredient.getAmount());
        return ingredient;
    }

    private static List<RecipeStepDTO> mapRecipeSteps(List<RecipeStep> recipeSteps) {
        return Optional.ofNullable(recipeSteps)
                .orElse(Collections.emptyList())
                .stream()
                .map(RecipeDetailsDTOConverter::mapRecipeStep)
                .collect(Collectors.toList());
    }

    private static RecipeStepDTO mapRecipeStep(RecipeStep recipeStep) {
        RecipeStepDTO step = new RecipeStepDTO();
        step.setOrder(recipeStep.getOrder());
        step.setDetails(recipeStep.getDetails());
        step.setImages(mapRecipeImages(recipeStep.getImages()));
        return step;
    }
}
