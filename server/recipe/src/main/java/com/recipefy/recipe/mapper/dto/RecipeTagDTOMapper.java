package com.recipefy.recipe.mapper.dto;

import com.recipefy.recipe.model.dto.RecipeTagDTO;
import com.recipefy.recipe.model.entity.RecipeTag;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RecipeTagDTOMapper {
    public static List<RecipeTagDTO> toDTO(Set<RecipeTag> tags) {
        return tags.stream().map(RecipeTagDTOMapper::toDTO).toList();
    }

    public static RecipeTagDTO toDTO(RecipeTag recipeTag) {
        RecipeTagDTO dto = new RecipeTagDTO();

        dto.setId(recipeTag.getId());
        dto.setName(recipeTag.getName());

        return dto;
    }

    public static Set<RecipeTag> toEntity(List<RecipeTagDTO> dtos) {
        return dtos.stream().map(RecipeTagDTOMapper::toEntity).collect(Collectors.toSet());
    }

    public static RecipeTag toEntity(RecipeTagDTO dto) {
        RecipeTag tag = new RecipeTag();

        tag.setId(dto.getId());
        tag.setName(dto.getName());

        return tag;
    }
}
