package com.recipefy.version.repository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.recipefy.version.model.postgres.Branch;
import com.recipefy.version.model.postgres.Commit;
import com.recipefy.version.repository.postgres.BranchRepository;

@DataJpaTest
@ActiveProfiles("test")
class BranchRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BranchRepository branchRepository;

    private Branch sampleBranch;
    private Commit sampleCommit;

    @BeforeEach
    void setUp() {
        // Create a sample commit first
        sampleCommit = new Commit();
        sampleCommit.setUserId(1L);
        sampleCommit.setMessage("Initial commit");
        sampleCommit.setCreatedAt(LocalDateTime.now());
        sampleCommit = entityManager.persistAndFlush(sampleCommit);

        // Create a sample branch
        sampleBranch = new Branch();
        sampleBranch.setName("main");
        sampleBranch.setRecipeId(1L);
        sampleBranch.setHeadCommit(sampleCommit);
        sampleBranch.setCommits(Set.of(sampleCommit));
        sampleBranch.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void save_ShouldPersistBranchSuccessfully() {
        // When
        Branch savedBranch = branchRepository.save(sampleBranch);

        // Then
        assertNotNull(savedBranch);
        assertNotNull(savedBranch.getId());
        assertEquals("main", savedBranch.getName());
        assertEquals(1L, savedBranch.getRecipeId());
        assertEquals(sampleCommit, savedBranch.getHeadCommit());
    }

    @Test
    void findById_ShouldReturnBranch_WhenBranchExists() {
        // Given
        Branch savedBranch = entityManager.persistAndFlush(sampleBranch);

        // When
        Optional<Branch> foundBranch = branchRepository.findById(savedBranch.getId());

        // Then
        assertTrue(foundBranch.isPresent());
        assertEquals(savedBranch.getId(), foundBranch.get().getId());
        assertEquals("main", foundBranch.get().getName());
    }

    @Test
    void findById_ShouldReturnEmpty_WhenBranchDoesNotExist() {
        // When
        Optional<Branch> foundBranch = branchRepository.findById(999L);

        // Then
        assertFalse(foundBranch.isPresent());
    }

    @Test
    void findByRecipeId_ShouldReturnBranches_WhenBranchesExist() {
        // Given
        Branch branch1 = createBranch("main", 1L);
        Branch branch2 = createBranch("feature", 1L);
        Branch branch3 = createBranch("main", 2L); // Different recipe

        entityManager.persistAndFlush(branch1);
        entityManager.persistAndFlush(branch2);
        entityManager.persistAndFlush(branch3);

        // When
        List<Branch> branches = branchRepository.findByRecipeId(1L);

        // Then
        assertEquals(2, branches.size());
        assertTrue(branches.stream().anyMatch(b -> b.getName().equals("main")));
        assertTrue(branches.stream().anyMatch(b -> b.getName().equals("feature")));
    }

    @Test
    void findByRecipeId_ShouldReturnEmptyList_WhenNoBranchesExist() {
        // When
        List<Branch> branches = branchRepository.findByRecipeId(999L);

        // Then
        assertTrue(branches.isEmpty());
    }

    @Test
    void findByRecipeIdAndName_ShouldReturnBranch_WhenBranchExists() {
        // Given
        Branch savedBranch = entityManager.persistAndFlush(sampleBranch);

        // When
        Optional<Branch> foundBranch = branchRepository.findByRecipeIdAndName(1L, "main");

        // Then
        assertTrue(foundBranch.isPresent());
        assertEquals(savedBranch.getId(), foundBranch.get().getId());
        assertEquals("main", foundBranch.get().getName());
        assertEquals(1L, foundBranch.get().getRecipeId());
    }

    @Test
    void findByRecipeIdAndName_ShouldReturnEmpty_WhenBranchDoesNotExist() {
        // When
        Optional<Branch> foundBranch = branchRepository.findByRecipeIdAndName(1L, "non-existent");

        // Then
        assertFalse(foundBranch.isPresent());
    }

    @Test
    void findByRecipeIdAndName_ShouldReturnEmpty_WhenRecipeIdDoesNotExist() {
        // When
        Optional<Branch> foundBranch = branchRepository.findByRecipeIdAndName(999L, "main");

        // Then
        assertFalse(foundBranch.isPresent());
    }

    @Test
    void save_ShouldHandleMultipleBranches() {
        // Given
        Branch branch1 = createBranch("main", 1L);
        Branch branch2 = createBranch("feature", 1L);
        Branch branch3 = createBranch("hotfix", 1L);

        // When
        Branch saved1 = branchRepository.save(branch1);
        Branch saved2 = branchRepository.save(branch2);
        Branch saved3 = branchRepository.save(branch3);

        // Then
        assertNotNull(saved1.getId());
        assertNotNull(saved2.getId());
        assertNotNull(saved3.getId());
        assertNotEquals(saved1.getId(), saved2.getId());
        assertNotEquals(saved2.getId(), saved3.getId());
    }

    @Test
    void findByRecipeId_ShouldReturnBranches() {
        // Given
        Branch branch1 = createBranch("main", 1L);
        branch1.setCreatedAt(LocalDateTime.now().minusHours(2));
        
        Branch branch2 = createBranch("feature", 1L);
        branch2.setCreatedAt(LocalDateTime.now().minusHours(1));
        
        Branch branch3 = createBranch("hotfix", 1L);
        branch3.setCreatedAt(LocalDateTime.now());

        entityManager.persistAndFlush(branch1);
        entityManager.persistAndFlush(branch2);
        entityManager.persistAndFlush(branch3);

        // When
        List<Branch> branches = branchRepository.findByRecipeId(1L);

        // Then
        assertEquals(3, branches.size());
    }

    @Test
    void save_ShouldPreserveCommitRelationships() {
        // Given
        Commit commit1 = createCommit("Initial commit");
        Commit commit2 = createCommit("Second commit");
        
        commit1 = entityManager.persistAndFlush(commit1);
        commit2 = entityManager.persistAndFlush(commit2);
        HashSet<Commit> commits = new HashSet<>();
        commits.add(commit1);
        commits.add(commit2);

        Branch branch = new Branch();
        branch.setName("main");
        branch.setRecipeId(1L);
        branch.setHeadCommit(commit2);
        branch.setCommits(commits);
        branch.setCreatedAt(LocalDateTime.now());

        // When
        Branch savedBranch = branchRepository.save(branch);

        // Then
        assertNotNull(savedBranch.getHeadCommit());
        assertEquals(commit2.getId(), savedBranch.getHeadCommit().getId());
        assertEquals(2, savedBranch.getCommits().size());
    }

    // Helper methods
    private Branch createBranch(String name, Long recipeId) {
        Branch branch = new Branch();
        HashSet<Commit> commits = new HashSet<>();
        commits.add(sampleCommit);
        branch.setName(name);
        branch.setRecipeId(recipeId);
        branch.setHeadCommit(sampleCommit);
        branch.setCommits(commits);
        branch.setCreatedAt(LocalDateTime.now());
        return branch;
    }

    private Commit createCommit(String message) {
        Commit commit = new Commit();
        commit.setUserId(1L);
        commit.setMessage(message);
        commit.setCreatedAt(LocalDateTime.now());
        return commit;
    }
}
