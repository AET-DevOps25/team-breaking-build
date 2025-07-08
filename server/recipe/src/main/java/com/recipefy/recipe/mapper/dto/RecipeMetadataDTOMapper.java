package com.recipefy.recipe.mapper.dto;

import com.recipefy.recipe.model.dto.RecipeImageDTO;
import com.recipefy.recipe.model.dto.RecipeMetadataDTO;
import com.recipefy.recipe.model.entity.RecipeMetadata;
import java.util.UUID;

public class RecipeMetadataDTOMapper {
    public static RecipeMetadataDTO toDTO(RecipeMetadata recipeMetadata) {
        if (recipeMetadata == null) {
            return null;
        }
        
        RecipeMetadataDTO dto = new RecipeMetadataDTO();

        dto.setId(recipeMetadata.getId());
        dto.setForkedFrom(recipeMetadata.getForkedFrom());

        dto.setCreatedAt(recipeMetadata.getCreatedAt());
        dto.setUpdatedAt(recipeMetadata.getUpdatedAt());

        dto.setTitle(recipeMetadata.getTitle());
        dto.setDescription(recipeMetadata.getDescription());
        
        // Convert byte[] to RecipeImageDTO
        if (recipeMetadata.getThumbnail() != null) {
            RecipeImageDTO thumbnailDTO = new RecipeImageDTO();
            thumbnailDTO.setBase64String(recipeMetadata.getThumbnail());
            dto.setThumbnail(thumbnailDTO);
        } else {
            dto.setThumbnail(null);
        }
        
        dto.setServingSize(recipeMetadata.getServingSize());

        // Safe conversion of tags with defensive programming in RecipeTagDTOMapper
        dto.setTags(RecipeTagDTOMapper.toDTO(recipeMetadata.getTags()));

        return dto;
    }

    public static RecipeMetadata toEntity(RecipeMetadataDTO dto, UUID userId) {
        if (dto == null) {
            return null;
        }
        
        RecipeMetadata metadata = new RecipeMetadata();

        metadata.setId(dto.getId());
        metadata.setUserId(userId);
        metadata.setForkedFrom(dto.getForkedFrom());

        metadata.setCreatedAt(dto.getCreatedAt());
        metadata.setUpdatedAt(dto.getUpdatedAt());

        metadata.setTitle(dto.getTitle());
        metadata.setDescription(dto.getDescription());
        
        // Convert RecipeImageDTO to byte[]
        if (dto.getThumbnail() != null) {
            metadata.setThumbnail(dto.getThumbnail().getBase64String());
        } else {
            metadata.setThumbnail(null);
        }
        
        metadata.setServingSize(dto.getServingSize());

        // Safe conversion of tags with defensive programming in RecipeTagDTOMapper
        metadata.setTags(RecipeTagDTOMapper.toEntity(dto.getTags()));

        return metadata;
    }
}
