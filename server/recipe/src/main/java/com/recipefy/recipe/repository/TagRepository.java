package com.recipefy.recipe.repository;

import com.recipefy.recipe.model.entity.RecipeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<RecipeTag, Long> {
    Optional<RecipeTag> findByName(String name);
}
