package com.recipefy.version.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommitDTO {

    private Long id;

    private UUID userId;

    private String message;

    private Long parentId;

    private LocalDateTime createdAt;
}
