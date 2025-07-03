package com.recipefy.version.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
