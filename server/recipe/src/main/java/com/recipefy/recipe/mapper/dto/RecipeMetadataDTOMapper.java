package com.recipefy.recipe.mapper.dto;

import com.recipefy.recipe.model.dto.RecipeImageDTO;
import com.recipefy.recipe.model.dto.RecipeMetadataDTO;
import com.recipefy.recipe.model.entity.RecipeMetadata;
import java.util.UUID;

public class RecipeMetadataDTOMapper {
    public static RecipeMetadataDTO toDTO(RecipeMetadata recipeMetadata) {
        RecipeMetadataDTO dto = new RecipeMetadataDTO();

        dto.setId(recipeMetadata.getId());
        dto.setForkedFrom(recipeMetadata.getForkedFrom());

        dto.setCreatedAt(recipeMetadata.getCreatedAt());
        dto.setUpdatedAt(recipeMetadata.getUpdatedAt());

        dto.setTitle(recipeMetadata.getTitle());
        dto.setDescription(recipeMetadata.getDescription());
        dto.setThumbnail(new RecipeImageDTO(recipeMetadata.getThumbnail()));
        dto.setServingSize(recipeMetadata.getServingSize());

        dto.setTags(RecipeTagDTOMapper.toDTO(recipeMetadata.getTags()));

        return dto;
    }

    public static RecipeMetadata toEntity(RecipeMetadataDTO dto, UUID userId) {
        RecipeMetadata metadata = new RecipeMetadata();

        metadata.setId(dto.getId());
        metadata.setUserId(userId);
        metadata.setForkedFrom(dto.getForkedFrom());

        metadata.setCreatedAt(dto.getCreatedAt());
        metadata.setUpdatedAt(dto.getUpdatedAt());

        metadata.setTitle(dto.getTitle());
        metadata.setDescription(dto.getDescription());
        metadata.setThumbnail(dto.getThumbnail().getUrl());
        metadata.setServingSize(dto.getServingSize());

        metadata.setTags(RecipeTagDTOMapper.toEntity(dto.getTags()));

        return metadata;
    }
}
