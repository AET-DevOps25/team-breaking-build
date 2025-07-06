package com.recipefy.recipe.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeMetadataDTO {
    private Long id;
    private Long forkedFrom;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String title;
    private String description;
    private RecipeImageDTO thumbnail;
    private Integer servingSize;

    private List<RecipeTagDTO> tags;
}
