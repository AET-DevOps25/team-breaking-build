package com.recipefy.recipe.repository;

import com.recipefy.recipe.model.entity.RecipeMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<RecipeMetadata, Long> {
}
