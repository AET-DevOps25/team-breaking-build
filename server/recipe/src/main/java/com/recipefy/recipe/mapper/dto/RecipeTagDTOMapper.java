package com.recipefy.recipe.mapper.dto;

import com.recipefy.recipe.model.dto.RecipeTagDTO;
import com.recipefy.recipe.model.entity.RecipeTag;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class RecipeTagDTOMapper {
    public static List<RecipeTagDTO> toDTO(Set<RecipeTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        // Create a defensive copy to prevent ConcurrentModificationException
        Set<RecipeTag> tagsCopy = new HashSet<>(tags);
        return tagsCopy.stream().map(RecipeTagDTOMapper::toDTO).toList();
    }

    public static RecipeTagDTO toDTO(RecipeTag recipeTag) {
        if (recipeTag == null) {
            return null;
        }
        
        RecipeTagDTO dto = new RecipeTagDTO();
        dto.setId(recipeTag.getId());
        dto.setName(recipeTag.getName());
        return dto;
    }

    public static Set<RecipeTag> toEntity(List<RecipeTagDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return new HashSet<>();
        }
        return dtos.stream()
                .filter(dto -> dto != null) // Filter out null DTOs
                .map(RecipeTagDTOMapper::toEntity)
                .collect(Collectors.toSet());
    }

    public static RecipeTag toEntity(RecipeTagDTO dto) {
        if (dto == null) {
            return null;
        }
        
        RecipeTag tag = new RecipeTag();
        tag.setId(dto.getId());
        tag.setName(dto.getName());
        return tag;
    }
}
