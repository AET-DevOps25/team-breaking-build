package com.recipefy.recipe.repository;

import com.recipefy.recipe.model.entity.RecipeMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RecipeRepository extends JpaRepository<RecipeMetadata, Long> {
    Page<RecipeMetadata> findAllByUserId(UUID userId, Pageable pageable);
}
