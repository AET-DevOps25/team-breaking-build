package com.recipefy.recipe.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchDTO {
    private Long id;
    private String name;
    private Long recipeId;
    private Long headCommitId;
    private LocalDateTime createdAt;
} 
