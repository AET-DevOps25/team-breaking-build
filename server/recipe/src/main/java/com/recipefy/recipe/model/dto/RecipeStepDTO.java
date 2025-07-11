package com.recipefy.recipe.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Only for VCS Service propagation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStepDTO {
    private Integer order;
    private String details;
    private List<RecipeImageDTO> recipeImageDTOS; // nullable, can be null or empty
}
