package com.recipefy.version.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStep {

    private int order;

    private String details;

    private List<RecipeImage> images = new ArrayList<>();;
}
