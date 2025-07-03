package com.recipefy.version.model.response;

import com.recipefy.version.model.dto.CommitDTO;
import com.recipefy.version.model.dto.RecipeDetailsDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommitDetailsResponse {

    private CommitDTO commitMetadata;

    private RecipeDetailsDTO recipeDetails;
}
