package com.recipefy.version.converter.dto;

import com.recipefy.version.model.dto.CommitDTO;
import com.recipefy.version.model.postgres.BaseEntity;
import com.recipefy.version.model.postgres.Commit;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommitDTOConverter {

    public static CommitDTO toDTO(Commit commit) {
        CommitDTO commitDTO = new CommitDTO();
        commitDTO.setId(commit.getId());
        commitDTO.setMessage(commit.getMessage());
        commitDTO.setUserId(commit.getUserId());
        commitDTO.setParentId(mapParentCommitId(commit));
        commitDTO.setCreatedAt(commit.getCreatedAt());
        return commitDTO;
    }

    public static List<CommitDTO> toDTOList(List<Commit> commits) {
        return commits.stream()
                .map(CommitDTOConverter::toDTO)
                .collect(Collectors.toList());
    }

    private static Long mapParentCommitId(Commit commit) {
        return Optional.ofNullable(commit.getParent())
                .map(BaseEntity::getId)
                .orElse(null);
    }
}
