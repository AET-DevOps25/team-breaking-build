package com.recipefy.version.repository.postgres;

import com.recipefy.version.model.postgres.Branch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    @EntityGraph(attributePaths = {"commits"})
    Optional<Branch> findWithCommitsById(Long id);

    List<Branch> findByRecipeId(Long recipeId);

    Optional<Branch> findByRecipeIdAndName(Long recipeId, String branchName);
}
