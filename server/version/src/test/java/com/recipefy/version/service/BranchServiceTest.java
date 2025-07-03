package com.recipefy.version.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import static com.recipefy.version.constants.ApplicationConstants.MAIN_BRANCH_NAME;
import com.recipefy.version.exception.BusinessException;
import com.recipefy.version.model.postgres.Branch;
import com.recipefy.version.model.postgres.Commit;
import com.recipefy.version.repository.postgres.BranchRepository;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private BranchService branchService;

    private Branch sampleBranch;
    private Commit sampleCommit;

    @BeforeEach
    void setUp() {
        sampleCommit = createSampleCommit();
        sampleBranch = createSampleBranch();
    }

    @Test
    void saveBranch_ShouldSaveBranchSuccessfully() {
        // Given
        when(branchRepository.save(any(Branch.class))).thenReturn(sampleBranch);

        // When
        Branch result = branchService.saveBranch(sampleBranch);

        // Then
        assertNotNull(result);
        assertEquals(sampleBranch, result);
        verify(branchRepository).save(sampleBranch);
    }

    @Test
    void createMainBranch_ShouldCreateMainBranchSuccessfully() {
        // Given
        Long recipeId = 1L;
        when(branchRepository.save(any(Branch.class))).thenReturn(sampleBranch);

        // When
        Branch result = branchService.createMainBranch(recipeId, sampleCommit);

        // Then
        assertNotNull(result);
        assertEquals(sampleBranch, result);
        verify(branchRepository).save(any(Branch.class));
    }

    @Test
    void createBranch_ShouldCreateBranchSuccessfully() {
        // Given
        Long recipeId = 1L;
        String branchName = "feature-branch";
        when(branchRepository.save(any(Branch.class))).thenReturn(sampleBranch);

        // When
        Branch result = branchService.createBranch(recipeId, branchName, sampleCommit);

        // Then
        assertNotNull(result);
        assertEquals(sampleBranch, result);
        verify(branchRepository).save(any(Branch.class));
    }

    @Test
    void addCommit_ShouldAddCommitToBranchSuccessfully() {
        // Given
        when(branchRepository.save(any(Branch.class))).thenReturn(sampleBranch);

        // When
        Branch result = branchService.addCommit(sampleBranch, sampleCommit);

        // Then
        assertNotNull(result);
        assertEquals(sampleBranch, result);
        verify(branchRepository).save(sampleBranch);
    }

    @Test
    void getBranchesOfRecipe_ShouldReturnBranchesList_WhenValidRecipeId() {
        // Given
        Long recipeId = 1L;
        List<Branch> expectedBranches = Arrays.asList(sampleBranch);
        when(branchRepository.findByRecipeId(recipeId)).thenReturn(expectedBranches);

        // When
        List<Branch> result = branchService.getBranchesOfRecipe(recipeId);

        // Then
        assertNotNull(result);
        assertEquals(expectedBranches, result);
        assertEquals(1, result.size());
        verify(branchRepository).findByRecipeId(recipeId);
    }

    @Test
    void getBranchesOfRecipe_ShouldReturnEmptyList_WhenNoBranchesExist() {
        // Given
        Long recipeId = 1L;
        when(branchRepository.findByRecipeId(recipeId)).thenReturn(Arrays.asList());

        // When
        List<Branch> result = branchService.getBranchesOfRecipe(recipeId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(branchRepository).findByRecipeId(recipeId);
    }

    @Test
    void getHistory_ShouldReturnSortedCommitsList_WhenValidBranchId() {
        // Given
        Long branchId = 1L;
        Commit commit1 = createSampleCommit();
        commit1.setId(1L);
        commit1.setCreatedAt(LocalDateTime.now().minusHours(1));

        Commit commit2 = createSampleCommit();
        commit2.setId(2L);
        commit2.setCreatedAt(LocalDateTime.now());

        sampleBranch.setCommits(Set.of(commit1, commit2));
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(sampleBranch));

        // When
        List<Commit> result = branchService.getHistory(branchId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        // Should be sorted by createdAt in descending order (newest first)
        assertTrue(result.get(0).getCreatedAt().isAfter(result.get(1).getCreatedAt()));
        verify(branchRepository).findById(branchId);
    }

    @Test
    void getHistory_ShouldThrowException_WhenBranchNotFound() {
        // Given
        Long branchId = 999L;
        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> branchService.getHistory(branchId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verify(branchRepository).findById(branchId);
    }

    @Test
    void copyBranch_ShouldCopyBranchSuccessfully() {
        // Given
        Long branchId = 1L;
        Long newRecipeId = 2L;
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(sampleBranch));
        when(branchRepository.save(any(Branch.class))).thenReturn(sampleBranch);

        // When
        Branch result = branchService.copyBranch(branchId, newRecipeId);

        // Then
        assertNotNull(result);
        assertEquals(sampleBranch, result);
        verify(branchRepository).findById(branchId);
        verify(branchRepository).save(any(Branch.class));
    }

    @Test
    void copyBranch_ShouldThrowException_WhenSourceBranchNotFound() {
        // Given
        Long branchId = 999L;
        Long newRecipeId = 2L;
        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> branchService.copyBranch(branchId, newRecipeId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verify(branchRepository).findById(branchId);
    }

    @Test
    void getBranchesByRecipeId_ShouldReturnBranchesList_WhenValidRecipeId() {
        // Given
        Long recipeId = 1L;
        List<Branch> expectedBranches = Arrays.asList(sampleBranch);
        when(branchRepository.findByRecipeId(recipeId)).thenReturn(expectedBranches);

        // When
        List<Branch> result = branchService.getBranchesByRecipeId(recipeId);

        // Then
        assertNotNull(result);
        assertEquals(expectedBranches, result);
        assertEquals(1, result.size());
        verify(branchRepository).findByRecipeId(recipeId);
    }

    @Test
    void getBranchById_ShouldReturnBranch_WhenValidBranchId() {
        // Given
        Long branchId = 1L;
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(sampleBranch));

        // When
        Branch result = branchService.getBranchById(branchId);

        // Then
        assertNotNull(result);
        assertEquals(sampleBranch, result);
        verify(branchRepository).findById(branchId);
    }

    @Test
    void getBranchById_ShouldThrowException_WhenBranchNotFound() {
        // Given
        Long branchId = 999L;
        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> branchService.getBranchById(branchId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verify(branchRepository).findById(branchId);
    }

    @Test
    void checkIfBranchCreated_ShouldNotThrowException_WhenBranchNotExists() {
        // Given
        Long recipeId = 1L;
        when(branchRepository.findByRecipeIdAndName(recipeId, MAIN_BRANCH_NAME)).thenReturn(Optional.empty());

        // When & Then
        assertDoesNotThrow(() -> branchService.checkIfBranchCreated(recipeId));
        verify(branchRepository).findByRecipeIdAndName(recipeId, MAIN_BRANCH_NAME);
    }

    @Test
    void checkIfBranchCreated_ShouldThrowException_WhenBranchAlreadyExists() {
        // Given
        Long recipeId = 1L;
        when(branchRepository.findByRecipeIdAndName(recipeId, MAIN_BRANCH_NAME)).thenReturn(Optional.of(sampleBranch));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> branchService.checkIfBranchCreated(recipeId));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
        verify(branchRepository).findByRecipeIdAndName(recipeId, MAIN_BRANCH_NAME);
    }

    @Test
    void checkUniqueBranchName_ShouldNotThrowException_WhenBranchNameIsUnique() {
        // Given
        Long recipeId = 1L;
        String branchName = "unique-branch";
        when(branchRepository.findByRecipeIdAndName(recipeId, branchName)).thenReturn(Optional.empty());

        // When & Then
        assertDoesNotThrow(() -> branchService.checkUniqueBranchName(recipeId, branchName));
        verify(branchRepository).findByRecipeIdAndName(recipeId, branchName);
    }

    @Test
    void checkUniqueBranchName_ShouldThrowException_WhenBranchNameNotUnique() {
        // Given
        Long recipeId = 1L;
        String branchName = "existing-branch";
        when(branchRepository.findByRecipeIdAndName(recipeId, branchName)).thenReturn(Optional.of(sampleBranch));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> branchService.checkUniqueBranchName(recipeId, branchName));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
        verify(branchRepository).findByRecipeIdAndName(recipeId, branchName);
    }

    // Helper methods to create test data
    private Branch createSampleBranch() {
        Branch branch = new Branch();
        HashSet<Commit> commits = new HashSet<>();
        commits.add(sampleCommit);
        branch.setId(1L);
        branch.setName("main");
        branch.setRecipeId(1L);
        branch.setHeadCommit(sampleCommit);
        branch.setCommits(commits);
        branch.setCreatedAt(LocalDateTime.now());
        return branch;
    }

    private Commit createSampleCommit() {
        Commit commit = new Commit();
        commit.setId(1L);
        commit.setUserId(1L);
        commit.setMessage("Initial commit");
        commit.setCreatedAt(LocalDateTime.now());
        return commit;
    }
}
