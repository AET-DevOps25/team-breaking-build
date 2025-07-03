package com.recipefy.version.converter.dto;

import com.recipefy.version.model.dto.BranchDTO;
import com.recipefy.version.model.postgres.BaseEntity;
import com.recipefy.version.model.postgres.Branch;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BranchDTOConverter {

    public static BranchDTO toDTO(Branch branch) {
        BranchDTO branchDTO = new BranchDTO();
        branchDTO.setId(branch.getId());
        branchDTO.setRecipeId(branch.getRecipeId());
        branchDTO.setName(branch.getName());
        branchDTO.setHeadCommitId(Optional.ofNullable(branch.getHeadCommit()).map(BaseEntity::getId).orElse(null));
        branchDTO.setCreatedAt(branch.getCreatedAt());
        return branchDTO;
    }

    public static List<BranchDTO> toDTOList(List<Branch> branches) {
        return branches.stream()
                .map(BranchDTOConverter::toDTO)
                .collect(Collectors.toList());
    }
}
