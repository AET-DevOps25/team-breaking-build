package com.recipefy.version.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommitDTO {

    private Long id;

    private Long userId;

    private String message;

    private Long parentId;

    private LocalDateTime createdAt;
}
