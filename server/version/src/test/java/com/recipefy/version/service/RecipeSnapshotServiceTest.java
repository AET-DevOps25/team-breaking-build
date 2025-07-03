package com.recipefy.version.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.recipefy.version.exception.BusinessException;
import com.recipefy.version.model.mongo.RecipeDetails;
import com.recipefy.version.model.mongo.RecipeSnapshot;
import com.recipefy.version.repository.mongo.RecipeSnapshotRepository;

@ExtendWith(MockitoExtension.class)
class RecipeSnapshotServiceTest {

    @Mock
    private RecipeSnapshotRepository recipeSnapshotRepository;

    @InjectMocks
    private RecipeSnapshotService recipeSnapshotService;

    private RecipeSnapshot sampleRecipeSnapshot;

    @BeforeEach
    void setUp() {
        sampleRecipeSnapshot = createSampleRecipeSnapshot();
    }

    @Test
    void createRecipeSnapshot_ShouldCreateSnapshotSuccessfully() {
        // Given
        when(recipeSnapshotRepository.save(any(RecipeSnapshot.class))).thenReturn(sampleRecipeSnapshot);

        // When
        RecipeSnapshot result = recipeSnapshotService.createRecipeSnapshot(sampleRecipeSnapshot);

        // Then
        assertNotNull(result);
        assertEquals(sampleRecipeSnapshot, result);
        verify(recipeSnapshotRepository).save(sampleRecipeSnapshot);
    }

    @Test
    void createRecipeSnapshot_ShouldReturnSavedSnapshot_WhenValidSnapshot() {
        // Given
        RecipeSnapshot newSnapshot = createSampleRecipeSnapshot();
        newSnapshot.setId("new-snapshot");
        newSnapshot.setRecipeId(2L);
        
        when(recipeSnapshotRepository.save(any(RecipeSnapshot.class))).thenReturn(newSnapshot);

        // When
        RecipeSnapshot result = recipeSnapshotService.createRecipeSnapshot(newSnapshot);

        // Then
        assertNotNull(result);
        assertEquals("new-snapshot", result.getId());
        assertEquals(2L, result.getRecipeId());
        verify(recipeSnapshotRepository).save(newSnapshot);
    }

    @Test
    void createRecipeSnapshot_ShouldHandleNullRecipeDetails() {
        // Given
        RecipeSnapshot snapshotWithNullDetails = createSampleRecipeSnapshot();
        snapshotWithNullDetails.setDetails(null);
        
        when(recipeSnapshotRepository.save(any(RecipeSnapshot.class))).thenReturn(snapshotWithNullDetails);

        // When
        RecipeSnapshot result = recipeSnapshotService.createRecipeSnapshot(snapshotWithNullDetails);

        // Then
        assertNotNull(result);
        assertNull(result.getDetails());
        verify(recipeSnapshotRepository).save(snapshotWithNullDetails);
    }

    @Test
    void getRecipeSnapshot_ShouldReturnSnapshot_WhenValidId() {
        // Given
        String snapshotId = "1";
        when(recipeSnapshotRepository.findById(snapshotId)).thenReturn(Optional.of(sampleRecipeSnapshot));

        // When
        RecipeSnapshot result = recipeSnapshotService.getRecipeSnapshot(snapshotId);

        // Then
        assertNotNull(result);
        assertEquals(sampleRecipeSnapshot, result);
        verify(recipeSnapshotRepository).findById(snapshotId);
    }

    @Test
    void getRecipeSnapshot_ShouldReturnCorrectSnapshot_WhenMultipleSnapshotsExist() {
        // Given
        String snapshotId = "specific-id";
        RecipeSnapshot specificSnapshot = createSampleRecipeSnapshot();
        specificSnapshot.setId(snapshotId);
        specificSnapshot.setRecipeId(999L);
        
        when(recipeSnapshotRepository.findById(snapshotId)).thenReturn(Optional.of(specificSnapshot));

        // When
        RecipeSnapshot result = recipeSnapshotService.getRecipeSnapshot(snapshotId);

        // Then
        assertNotNull(result);
        assertEquals(snapshotId, result.getId());
        assertEquals(999L, result.getRecipeId());
        verify(recipeSnapshotRepository).findById(snapshotId);
    }

    @Test
    void getRecipeSnapshot_ShouldThrowException_WhenSnapshotNotFound() {
        // Given
        String snapshotId = "non-existent";
        when(recipeSnapshotRepository.findById(snapshotId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> recipeSnapshotService.getRecipeSnapshot(snapshotId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verify(recipeSnapshotRepository).findById(snapshotId);
    }

    @Test
    void getRecipeSnapshot_ShouldThrowException_WhenIdIsNull() {
        // Given
        when(recipeSnapshotRepository.findById(null)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> recipeSnapshotService.getRecipeSnapshot(null));
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verify(recipeSnapshotRepository).findById(null);
    }

    @Test
    void getRecipeSnapshot_ShouldThrowException_WhenIdIsEmpty() {
        // Given
        String emptyId = "";
        when(recipeSnapshotRepository.findById(emptyId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> recipeSnapshotService.getRecipeSnapshot(emptyId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verify(recipeSnapshotRepository).findById(emptyId);
    }

    @Test
    void getRecipeSnapshot_ShouldHandleSpecialCharactersInId() {
        // Given
        String specialId = "commit-123@#$%";
        RecipeSnapshot specialSnapshot = createSampleRecipeSnapshot();
        specialSnapshot.setId(specialId);
        
        when(recipeSnapshotRepository.findById(specialId)).thenReturn(Optional.of(specialSnapshot));

        // When
        RecipeSnapshot result = recipeSnapshotService.getRecipeSnapshot(specialId);

        // Then
        assertNotNull(result);
        assertEquals(specialId, result.getId());
        verify(recipeSnapshotRepository).findById(specialId);
    }

    @Test
    void createRecipeSnapshot_ShouldPreserveAllFields() {
        // Given
        RecipeSnapshot complexSnapshot = createComplexRecipeSnapshot();
        when(recipeSnapshotRepository.save(any(RecipeSnapshot.class))).thenReturn(complexSnapshot);

        // When
        RecipeSnapshot result = recipeSnapshotService.createRecipeSnapshot(complexSnapshot);

        // Then
        assertNotNull(result);
        assertEquals("complex-snapshot", result.getId());
        assertEquals(123L, result.getRecipeId());
        assertNotNull(result.getDetails());
        verify(recipeSnapshotRepository).save(complexSnapshot);
    }

    @Test
    void getRecipeSnapshot_ShouldHandleRepositoryException() {
        // Given
        String snapshotId = "1";
        when(recipeSnapshotRepository.findById(snapshotId))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> recipeSnapshotService.getRecipeSnapshot(snapshotId));
        assertEquals("Database connection failed", exception.getMessage());
        verify(recipeSnapshotRepository).findById(snapshotId);
    }

    @Test
    void createRecipeSnapshot_ShouldHandleRepositoryException() {
        // Given
        when(recipeSnapshotRepository.save(any(RecipeSnapshot.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> recipeSnapshotService.createRecipeSnapshot(sampleRecipeSnapshot));
        assertEquals("Database connection failed", exception.getMessage());
        verify(recipeSnapshotRepository).save(sampleRecipeSnapshot);
    }

    // Helper methods to create test data
    private RecipeSnapshot createSampleRecipeSnapshot() {
        RecipeSnapshot snapshot = new RecipeSnapshot();
        snapshot.setId("1");
        snapshot.setRecipeId(1L);
        snapshot.setDetails(new RecipeDetails());
        return snapshot;
    }

    private RecipeSnapshot createComplexRecipeSnapshot() {
        RecipeSnapshot snapshot = new RecipeSnapshot();
        snapshot.setId("complex-snapshot");
        snapshot.setRecipeId(123L);
        
        RecipeDetails details = new RecipeDetails();
        details.setServingSize(6);
        snapshot.setDetails(details);
        
        return snapshot;
    }
}
