package com.recipefy.recipe.mapper.dto;

import com.recipefy.recipe.model.dto.RecipeMetadataDTO;
import com.recipefy.recipe.model.entity.RecipeMetadata;

public class RecipeMetadataDTOMapper {
    public static RecipeMetadataDTO toDTO(RecipeMetadata recipeMetadata) {
        RecipeMetadataDTO dto = new RecipeMetadataDTO();

        dto.setId(recipeMetadata.getId());
        dto.setUserId(recipeMetadata.getUserId());
        dto.setForkedFrom(recipeMetadata.getForkedFrom());

        dto.setCreatedAt(recipeMetadata.getCreatedAt());
        dto.setUpdatedAt(recipeMetadata.getUpdatedAt());

        dto.setTitle(recipeMetadata.getTitle());
        dto.setDescription(recipeMetadata.getDescription());
        dto.setThumbnail(recipeMetadata.getThumbnail());

        dto.setTags(RecipeTagDTOMapper.toDTO(recipeMetadata.getTags()));

        return dto;
    }
}
