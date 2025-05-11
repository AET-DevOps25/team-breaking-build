package com.recipefy.recipe.mapper.dto;

import com.recipefy.recipe.model.dto.RecipeTagDTO;
import com.recipefy.recipe.model.entity.RecipeTag;

import java.util.List;
import java.util.Set;

public class RecipeTagDTOMapper {
    public static List<RecipeTagDTO> toDTO(Set<RecipeTag> tags) {
        return tags.stream().map(RecipeTagDTOMapper::toSingleDTO).toList();
    }

    public static RecipeTagDTO toSingleDTO(RecipeTag recipeTag) {
        RecipeTagDTO dto = new RecipeTagDTO();

        dto.setId(recipeTag.getId());
        dto.setName(recipeTag.getName());

        return dto;
    }
}
