package com.recipefy.recipe.model.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeMetadataDTO {
    private Long id;
    private UUID userId;
    private Long forkedFrom;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String title;
    private String description;
    private RecipeImageDTO thumbnail; // nullable, can be null if no image is provided
    private Integer servingSize;

    private List<RecipeTagDTO> tags;
}
