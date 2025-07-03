package com.recipefy.version.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBranchRequest {

    @NotNull(message = "Branch name is required")
    @NotBlank(message = "Branch name is required")
    private String branchName;

    @NotNull(message = "Source branch id is required")
    private Long sourceBranchId;
}
