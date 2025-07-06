package com.recipefy.version.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static com.recipefy.version.constants.ApplicationConstants.INITIAL_COMMIT_MESSAGE;
import com.recipefy.version.exception.BusinessException;
import com.recipefy.version.model.postgres.Commit;
import com.recipefy.version.repository.postgres.CommitRepository;

@ExtendWith(MockitoExtension.class)
class CommitServiceTest {

    @Mock
    private CommitRepository commitRepository;

    @InjectMocks
    private CommitService commitService;

    private Commit sampleCommit;
    private Commit parentCommit;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        sampleCommit = createSampleCommit();
        parentCommit = createParentCommit();
    }

    @Test
    void createInitialCommit_ShouldCreateInitialCommitSuccessfully() {
        // Given
        when(commitRepository.save(any(Commit.class))).thenReturn(sampleCommit);

        // When
        Commit result = commitService.createInitialCommit(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(sampleCommit, result);
        verify(commitRepository).save(any(Commit.class));
    }

    @Test
    void createInitialCommit_ShouldSetCorrectInitialMessage() {
        // Given
        when(commitRepository.save(any(Commit.class))).thenAnswer(invocation -> {
            Commit commit = invocation.getArgument(0);
            commit.setId(1L);
            return commit;
        });

        // When
        Commit result = commitService.createInitialCommit(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(INITIAL_COMMIT_MESSAGE, result.getMessage());
        assertEquals(testUserId, result.getUserId());
        assertNull(result.getParent());
        verify(commitRepository).save(any(Commit.class));
    }

    @Test
    void createCommit_ShouldCreateCommitSuccessfully_WhenValidParameters() {
        // Given
        String message = "Update recipe";
        when(commitRepository.save(any(Commit.class))).thenReturn(sampleCommit);

        // When
        Commit result = commitService.createCommit(testUserId, message, parentCommit);

        // Then
        assertNotNull(result);
        assertEquals(sampleCommit, result);
        verify(commitRepository).save(any(Commit.class));
    }

    @Test
    void createCommit_ShouldCreateCommitWithParent_WhenParentProvided() {
        // Given
        String message = "Update recipe";
        when(commitRepository.save(any(Commit.class))).thenAnswer(invocation -> {
            Commit commit = invocation.getArgument(0);
            commit.setId(2L);
            return commit;
        });

        // When
        Commit result = commitService.createCommit(testUserId, message, parentCommit);

        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(message, result.getMessage());
        assertEquals(parentCommit, result.getParent());
        verify(commitRepository).save(any(Commit.class));
    }

    @Test
    void createCommit_ShouldCreateCommitWithoutParent_WhenParentIsNull() {
        // Given
        String message = "Update recipe";
        when(commitRepository.save(any(Commit.class))).thenAnswer(invocation -> {
            Commit commit = invocation.getArgument(0);
            commit.setId(2L);
            return commit;
        });

        // When
        Commit result = commitService.createCommit(testUserId, message, null);

        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(message, result.getMessage());
        assertNull(result.getParent());
        verify(commitRepository).save(any(Commit.class));
    }

    @Test
    void getCommitById_ShouldReturnCommit_WhenValidCommitId() {
        // Given
        Long commitId = 1L;
        when(commitRepository.findById(commitId)).thenReturn(Optional.of(sampleCommit));

        // When
        Commit result = commitService.getCommitById(commitId);

        // Then
        assertNotNull(result);
        assertEquals(sampleCommit, result);
        verify(commitRepository).findById(commitId);
    }

    @Test
    void getCommitById_ShouldThrowException_WhenCommitNotFound() {
        // Given
        Long commitId = 999L;
        when(commitRepository.findById(commitId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> commitService.getCommitById(commitId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verify(commitRepository).findById(commitId);
    }

    @Test
    void getCommitById_ShouldThrowException_WhenCommitIdIsNull() {
        // Given
        when(commitRepository.findById(null)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> commitService.getCommitById(null));
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verify(commitRepository).findById(null);
    }

    @Test
    void createInitialCommit_ShouldSetCorrectUserId() {
        // Given
        UUID userId = UUID.randomUUID();
        when(commitRepository.save(any(Commit.class))).thenAnswer(invocation -> {
            Commit commit = invocation.getArgument(0);
            commit.setId(1L);
            return commit;
        });

        // When
        Commit result = commitService.createInitialCommit(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(commitRepository).save(any(Commit.class));
    }

    @Test
    void createCommit_ShouldHandleEmptyMessage() {
        // Given
        String message = "";
        when(commitRepository.save(any(Commit.class))).thenAnswer(invocation -> {
            Commit commit = invocation.getArgument(0);
            commit.setId(2L);
            return commit;
        });

        // When
        Commit result = commitService.createCommit(testUserId, message, parentCommit);

        // Then
        assertNotNull(result);
        assertEquals(message, result.getMessage());
        verify(commitRepository).save(any(Commit.class));
    }

    @Test
    void createCommit_ShouldHandleNullMessage() {
        // Given
        String message = null;
        when(commitRepository.save(any(Commit.class))).thenAnswer(invocation -> {
            Commit commit = invocation.getArgument(0);
            commit.setId(2L);
            return commit;
        });

        // When
        Commit result = commitService.createCommit(testUserId, message, parentCommit);

        // Then
        assertNotNull(result);
        assertNull(result.getMessage());
        verify(commitRepository).save(any(Commit.class));
    }

    // Helper methods to create test data
    private Commit createSampleCommit() {
        Commit commit = new Commit();
        commit.setId(1L);
        commit.setUserId(testUserId);
        commit.setMessage("Initial commit");
        commit.setCreatedAt(LocalDateTime.now());
        return commit;
    }

    private Commit createParentCommit() {
        Commit commit = new Commit();
        commit.setId(1L);
        commit.setUserId(testUserId);
        commit.setMessage("Parent commit");
        commit.setCreatedAt(LocalDateTime.now().minusHours(1));
        return commit;
    }
}
