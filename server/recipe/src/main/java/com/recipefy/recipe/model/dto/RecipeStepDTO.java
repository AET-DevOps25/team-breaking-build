package com.recipefy.recipe.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStepDTO {
    private Integer order;
    private String details;
    private List<RecipeImageDTO> recipeImageDTOS;
}
