package com.recipefy.version.model.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.recipefy.version.model.dto.RecipeDetailsDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeResponse {

    private RecipeDetailsDTO oldDetails;

    private RecipeDetailsDTO currentDetails;

    private JsonNode changes;

    private boolean firstCommit;
}
