package com.recipefy.version.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.recipefy.version.model.postgres.Commit;
import com.recipefy.version.repository.postgres.CommitRepository;

@DataJpaTest
@ActiveProfiles("test")
class CommitRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommitRepository commitRepository;

    private Commit sampleCommit;
    private Commit parentCommit;

    @BeforeEach
    void setUp() {
        // Create a parent commit
        parentCommit = new Commit();
        parentCommit.setUserId(1L);
        parentCommit.setMessage("Parent commit");
        parentCommit.setCreatedAt(LocalDateTime.now().minusHours(1));
        parentCommit = entityManager.persistAndFlush(parentCommit);

        // Create a sample commit
        sampleCommit = new Commit();
        sampleCommit.setUserId(1L);
        sampleCommit.setMessage("Sample commit");
        sampleCommit.setParent(parentCommit);
        sampleCommit.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void save_ShouldPersistCommitSuccessfully() {
        // When
        Commit savedCommit = commitRepository.save(sampleCommit);

        // Then
        assertNotNull(savedCommit);
        assertNotNull(savedCommit.getId());
        assertEquals(1L, savedCommit.getUserId());
        assertEquals("Sample commit", savedCommit.getMessage());
        assertEquals(parentCommit, savedCommit.getParent());
    }

    @Test
    void save_ShouldPersistCommitWithoutParent() {
        // Given
        Commit commitWithoutParent = new Commit();
        commitWithoutParent.setUserId(1L);
        commitWithoutParent.setMessage("Commit without parent");
        commitWithoutParent.setCreatedAt(LocalDateTime.now());

        // When
        Commit savedCommit = commitRepository.save(commitWithoutParent);

        // Then
        assertNotNull(savedCommit);
        assertNotNull(savedCommit.getId());
        assertNull(savedCommit.getParent());
    }

    @Test
    void findById_ShouldReturnCommit_WhenCommitExists() {
        // Given
        Commit savedCommit = entityManager.persistAndFlush(sampleCommit);

        // When
        Optional<Commit> foundCommit = commitRepository.findById(savedCommit.getId());

        // Then
        assertTrue(foundCommit.isPresent());
        assertEquals(savedCommit.getId(), foundCommit.get().getId());
        assertEquals("Sample commit", foundCommit.get().getMessage());
        assertEquals(1L, foundCommit.get().getUserId());
    }

    @Test
    void findById_ShouldReturnEmpty_WhenCommitDoesNotExist() {
        // When
        Optional<Commit> foundCommit = commitRepository.findById(999L);

        // Then
        assertFalse(foundCommit.isPresent());
    }

    @Test
    void save_ShouldUpdateExistingCommit() {
        // Given
        Commit savedCommit = entityManager.persistAndFlush(sampleCommit);
        savedCommit.setMessage("Updated commit message");

        // When
        Commit updatedCommit = commitRepository.save(savedCommit);

        // Then
        assertEquals(savedCommit.getId(), updatedCommit.getId());
        assertEquals("Updated commit message", updatedCommit.getMessage());
    }

    @Test
    void save_ShouldHandleMultipleCommits() {
        // Given
        Commit commit1 = createCommit("First commit", null);
        Commit commit2 = createCommit("Second commit", commit1);
        Commit commit3 = createCommit("Third commit", commit2);

        // When
        Commit saved1 = commitRepository.save(commit1);
        Commit saved2 = commitRepository.save(commit2);
        Commit saved3 = commitRepository.save(commit3);

        // Then
        assertNotNull(saved1.getId());
        assertNotNull(saved2.getId());
        assertNotNull(saved3.getId());
        assertNotEquals(saved1.getId(), saved2.getId());
        assertNotEquals(saved2.getId(), saved3.getId());
    }

    @Test
    void save_ShouldPreserveParentChildRelationships() {
        // Given
        Commit parent = createCommit("Parent commit", null);
        parent = entityManager.persistAndFlush(parent);

        Commit child = createCommit("Child commit", parent);

        // When
        Commit savedChild = commitRepository.save(child);

        // Then
        assertNotNull(savedChild.getParent());
        assertEquals(parent.getId(), savedChild.getParent().getId());
        assertEquals("Parent commit", savedChild.getParent().getMessage());
    }

    @Test
    void save_ShouldHandleComplexCommitChain() {
        // Given
        Commit commit1 = createCommit("Initial commit", null);
        commit1 = entityManager.persistAndFlush(commit1);

        Commit commit2 = createCommit("Feature commit", commit1);
        commit2 = entityManager.persistAndFlush(commit2);

        Commit commit3 = createCommit("Bug fix", commit2);
        commit3 = entityManager.persistAndFlush(commit3);

        // When
        Optional<Commit> foundCommit3 = commitRepository.findById(commit3.getId());

        // Then
        assertTrue(foundCommit3.isPresent());
        Commit retrieved = foundCommit3.get();
        assertEquals("Bug fix", retrieved.getMessage());
        assertEquals("Feature commit", retrieved.getParent().getMessage());
        assertEquals("Initial commit", retrieved.getParent().getParent().getMessage());
        assertNull(retrieved.getParent().getParent().getParent());
    }

    @Test
    void save_ShouldSetCreatedAtTimestamp() {
        // Given
        LocalDateTime beforeTest = LocalDateTime.now();
        Commit commit = createCommit("Test commit", null);

        // When
        Commit savedCommit = commitRepository.save(commit);
        LocalDateTime afterTest = LocalDateTime.now();

        // Then
        assertNotNull(savedCommit.getCreatedAt());
        assertTrue(savedCommit.getCreatedAt().isAfter(beforeTest) || savedCommit.getCreatedAt().isEqual(beforeTest));
        assertTrue(savedCommit.getCreatedAt().isBefore(afterTest) || savedCommit.getCreatedAt().isEqual(afterTest));
    }

    @Test
    void save_ShouldHandleNullValues() {
        // Given
        Commit commit = new Commit();
        commit.setUserId(null);
        commit.setMessage(null);
        commit.setParent(null);

        // When
        Commit savedCommit = commitRepository.save(commit);

        // Then
        assertNotNull(savedCommit);
        assertNotNull(savedCommit.getId());
        assertNull(savedCommit.getUserId());
        assertNull(savedCommit.getMessage());
        assertNull(savedCommit.getParent());
    }

    @Test
    void save_ShouldHandleEmptyMessage() {
        // Given
        Commit commit = createCommit("", null);

        // When
        Commit savedCommit = commitRepository.save(commit);

        // Then
        assertNotNull(savedCommit);
        assertEquals("", savedCommit.getMessage());
    }

    @Test
    void save_ShouldHandleSpecialCharactersInMessage() {
        // Given
        String specialMessage = "Commit with special chars: @#$%^&*()_+-=[]{}|;':\",./<>?";
        Commit commit = createCommit(specialMessage, null);

        // When
        Commit savedCommit = commitRepository.save(commit);

        // Then
        assertNotNull(savedCommit);
        assertEquals(specialMessage, savedCommit.getMessage());
    }

    @Test
    void save_ShouldHandleLargeUserId() {
        // Given
        Commit commit = createCommit("Test commit", null);
        commit.setUserId(Long.MAX_VALUE);

        // When
        Commit savedCommit = commitRepository.save(commit);

        // Then
        assertNotNull(savedCommit);
        assertEquals(Long.MAX_VALUE, savedCommit.getUserId());
    }

    @Test
    void save_ShouldHandleNegativeUserId() {
        // Given
        Commit commit = createCommit("Test commit", null);
        commit.setUserId(-1L);

        // When
        Commit savedCommit = commitRepository.save(commit);

        // Then
        assertNotNull(savedCommit);
        assertEquals(-1L, savedCommit.getUserId());
    }

    // Helper methods
    private Commit createCommit(String message, Commit parent) {
        Commit commit = new Commit();
        commit.setUserId(1L);
        commit.setMessage(message);
        commit.setParent(parent);
        commit.setCreatedAt(LocalDateTime.now());
        return commit;
    }
}
