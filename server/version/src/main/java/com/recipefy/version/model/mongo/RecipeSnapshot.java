package com.recipefy.version.model.mongo;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "recipe_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeSnapshot {

    @Id
    private String id; // commitId == snapshot _id

    private Long recipeId;

    private RecipeDetails details;
}
