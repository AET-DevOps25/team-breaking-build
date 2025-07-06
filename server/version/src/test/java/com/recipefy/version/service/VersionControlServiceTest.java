package com.recipefy.version.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipefy.version.model.mongo.RecipeIngredient;
import com.recipefy.version.model.mongo.RecipeStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.recipefy.version.converter.dto.CommitDTOConverter;
import com.recipefy.version.exception.BusinessException;
import com.recipefy.version.model.dto.BranchDTO;
import com.recipefy.version.model.dto.CommitDTO;
import com.recipefy.version.model.dto.RecipeDetailsDTO;
import com.recipefy.version.model.dto.RecipeIngredientDTO;
import com.recipefy.version.model.dto.RecipeStepDTO;
import com.recipefy.version.model.mongo.RecipeDetails;
import com.recipefy.version.model.mongo.RecipeSnapshot;
import com.recipefy.version.model.postgres.Branch;
import com.recipefy.version.model.postgres.Commit;
import com.recipefy.version.model.request.CommitToBranchRequest;
import com.recipefy.version.model.request.CopyBranchRequest;
import com.recipefy.version.model.request.CreateBranchRequest;
import com.recipefy.version.model.request.InitRecipeRequest;
import com.recipefy.version.model.response.ChangeResponse;
import com.recipefy.version.model.response.CommitDetailsResponse;

@ExtendWith(MockitoExtension.class)
class VersionControlServiceTest {

    @Mock
    private BranchService branchService;

    @Mock
    private CommitService commitService;

    @Mock
    private RecipeSnapshotService recipeSnapshotService;

    @InjectMocks
    private VersionControlService versionControlService;

    private RecipeDetailsDTO sampleRecipeDetails;
    private Branch sampleBranch;
    private Commit sampleCommit;
    private RecipeSnapshot sampleRecipeSnapshot;
    private LocalDateTime createdAt;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        createdAt = LocalDateTime.now();
        testUserId = UUID.randomUUID();
        sampleRecipeDetails = createSampleRecipeDetails();
        sampleBranch = createSampleBranch();
        sampleCommit = createSampleCommit();
        sampleRecipeSnapshot = createSampleRecipeSnapshot();
    }

    @Test
    void initRecipe_ShouldInitializeRecipeSuccessfully_WhenValidRequest() {
        // Given
        Long recipeId = 1L;
        InitRecipeRequest request = new InitRecipeRequest(sampleRecipeDetails);

        doNothing().when(branchService).checkIfBranchCreated(recipeId);
        when(commitService.createInitialCommit(testUserId)).thenReturn(sampleCommit);
        when(branchService.createMainBranch(recipeId, sampleCommit)).thenReturn(sampleBranch);
        when(recipeSnapshotService.createRecipeSnapshot(any(RecipeSnapshot.class))).thenReturn(sampleRecipeSnapshot);

        // When
        versionControlService.initRecipe(recipeId, request, testUserId);

        // Then
        verify(branchService).checkIfBranchCreated(recipeId);
        verify(commitService).createInitialCommit(testUserId);
        verify(branchService).createMainBranch(recipeId, sampleCommit);
        verify(recipeSnapshotService).createRecipeSnapshot(any(RecipeSnapshot.class));
    }

    @Test
    void initRecipe_ShouldThrowException_WhenBranchAlreadyExists() {
        // Given
        Long recipeId = 1L;
        InitRecipeRequest request = new InitRecipeRequest(sampleRecipeDetails);

        doThrow(new BusinessException("Branch already exists", HttpStatus.BAD_REQUEST))
                .when(branchService).checkIfBranchCreated(recipeId);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> versionControlService.initRecipe(recipeId, request, testUserId));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void getBranchesOfRecipe_ShouldReturnBranchesList_WhenValidRecipeId() {
        // Given
        Long recipeId = 1L;
        sampleBranch.setHeadCommit(sampleCommit);
        List<Branch> branches = Arrays.asList(sampleBranch);
        List<BranchDTO> expectedBranches = Arrays.asList(createSampleBranchDTO());

        when(branchService.getBranchesOfRecipe(recipeId)).thenReturn(branches);

        // When
        List<BranchDTO> result = versionControlService.getBranchesOfRecipe(recipeId);

        // Then
        assertNotNull(result);
        assertEquals(expectedBranches, result);
        verify(branchService).getBranchesOfRecipe(recipeId);
    }

    @Test
    void createBranch_ShouldCreateBranchSuccessfully_WhenValidRequest() {
        // Given
        Long recipeId = 1L;
        String branchName = "feature-branch";
        Long sourceBranchId = 1L;
        sampleBranch.setHeadCommit(sampleCommit);
        sampleBranch.setName(branchName);
        CreateBranchRequest request = new CreateBranchRequest(branchName, sourceBranchId);

        when(branchService.getBranchById(sourceBranchId)).thenReturn(sampleBranch);
        doNothing().when(branchService).checkUniqueBranchName(recipeId, branchName);
        when(branchService.saveBranch(any(Branch.class))).thenReturn(sampleBranch);
        // When
        BranchDTO result = versionControlService.createBranch(recipeId, request, testUserId);

        // Then
        assertNotNull(result);
        verify(branchService).getBranchById(sourceBranchId);
        verify(branchService).checkUniqueBranchName(recipeId, branchName);
        verify(branchService).saveBranch(any(Branch.class));
    }

    @Test
    void createBranch_ShouldThrowException_WhenSourceBranchNotFound() {
        // Given
        Long recipeId = 1L;
        String branchName = "feature-branch";
        Long sourceBranchId = 999L;
        CreateBranchRequest request = new CreateBranchRequest(branchName, sourceBranchId);

        when(branchService.getBranchById(sourceBranchId))
                .thenThrow(new BusinessException("Branch not found", HttpStatus.NOT_FOUND));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> versionControlService.createBranch(recipeId, request, testUserId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void createBranch_ShouldThrowException_WhenBranchNameNotUnique() {
        // Given
        Long recipeId = 1L;
        String branchName = "existing-branch";
        Long sourceBranchId = 1L;
        CreateBranchRequest request = new CreateBranchRequest(branchName, sourceBranchId);

        when(branchService.getBranchById(sourceBranchId)).thenReturn(sampleBranch);
        doThrow(new BusinessException("Branch name not unique", HttpStatus.BAD_REQUEST))
                .when(branchService).checkUniqueBranchName(recipeId, branchName);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> versionControlService.createBranch(recipeId, request, testUserId));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void commitToBranch_ShouldCreateCommitSuccessfully_WhenValidRequest() {
        // Given
        Long branchId = 1L;
        String message = "Update recipe";
        CommitToBranchRequest request = new CommitToBranchRequest(message, sampleRecipeDetails);
        CommitDTO expectedCommitDTO = createSampleCommitDTO();

        when(branchService.getBranchById(branchId)).thenReturn(sampleBranch);
        when(commitService.createCommit(testUserId, message, sampleBranch.getHeadCommit())).thenReturn(sampleCommit);
        when(branchService.addCommit(sampleBranch, sampleCommit)).thenReturn(sampleBranch);
        when(recipeSnapshotService.createRecipeSnapshot(any(RecipeSnapshot.class))).thenReturn(sampleRecipeSnapshot);

        // When
        CommitDTO result = versionControlService.commitToBranch(branchId, request, testUserId);

        // Then
        assertNotNull(result);
        assertEquals(expectedCommitDTO, result);
        verify(branchService).getBranchById(branchId);
        verify(commitService).createCommit(testUserId, message, sampleBranch.getHeadCommit());
        verify(branchService).addCommit(sampleBranch, sampleCommit);
        verify(recipeSnapshotService).createRecipeSnapshot(any(RecipeSnapshot.class));
    }

    @Test
    void commitToBranch_ShouldThrowException_WhenBranchNotFound() {
        // Given
        Long branchId = 999L;
        String message = "Update recipe";
        CommitToBranchRequest request = new CommitToBranchRequest(message, sampleRecipeDetails);

        when(branchService.getBranchById(branchId))
                .thenThrow(new BusinessException("Branch not found", HttpStatus.NOT_FOUND));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> versionControlService.commitToBranch(branchId, request, testUserId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getBranchHistory_ShouldReturnCommitsList_WhenValidBranchId() {
        // Given
        Long branchId = 1L;
        List<Commit> commits = Arrays.asList(sampleCommit);
        List<CommitDTO> expectedCommits = CommitDTOConverter.toDTOList(commits);

        when(branchService.getHistory(branchId)).thenReturn(commits);

        // When
        List<CommitDTO> result = versionControlService.getBranchHistory(branchId);

        // Then
        assertNotNull(result);
        assertEquals(expectedCommits, result);
        verify(branchService).getHistory(branchId);
    }

    @Test
    void copyRecipe_ShouldCopyBranchSuccessfully_WhenValidRequest() {
        // Given
        Long branchId = 1L;
        Long newRecipeId = 2L;
        CopyBranchRequest request = new CopyBranchRequest(newRecipeId);
        BranchDTO expectedBranchDTO = createSampleBranchDTO();
        sampleBranch.setHeadCommit(sampleCommit);

        when(branchService.copyBranch(branchId, newRecipeId)).thenReturn(sampleBranch);

        // When
        BranchDTO result = versionControlService.copyRecipe(branchId, request);

        // Then
        assertNotNull(result);
        assertEquals(expectedBranchDTO, result);
        verify(branchService).copyBranch(branchId, newRecipeId);
    }

    @Test
    void copyRecipe_ShouldThrowException_WhenSourceBranchNotFound() {
        // Given
        Long branchId = 999L;
        Long newRecipeId = 2L;
        CopyBranchRequest request = new CopyBranchRequest(newRecipeId);

        when(branchService.copyBranch(branchId, newRecipeId))
                .thenThrow(new BusinessException("Branch not found", HttpStatus.NOT_FOUND));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> versionControlService.copyRecipe(branchId, request));
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getCommit_ShouldReturnCommitDetails_WhenValidCommitId() {
        // Given
        Long commitId = 1L;
        CommitDetailsResponse expectedResponse = createSampleCommitDetailsResponse();
        when(commitService.getCommitById(commitId)).thenReturn(sampleCommit);
        when(recipeSnapshotService.getRecipeSnapshot(commitId.toString())).thenReturn(sampleRecipeSnapshot);

        // When
        CommitDetailsResponse result = versionControlService.getCommit(commitId);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(commitService).getCommitById(commitId);
        verify(recipeSnapshotService).getRecipeSnapshot(commitId.toString());
    }

    @Test
    void getCommit_ShouldThrowException_WhenCommitNotFound() {
        // Given
        Long commitId = 999L;

        when(commitService.getCommitById(commitId))
                .thenThrow(new BusinessException("Commit not found", HttpStatus.NOT_FOUND));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> versionControlService.getCommit(commitId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getChanges_ShouldReturnChangeResponse_WhenValidCommitId() {
        // Given
        Long commitId = 1L;
        Commit parentCommit = createSampleCommit();
        parentCommit.setId(2L);
        sampleCommit.setParent(parentCommit);
        ChangeResponse expectedResponse = createSampleChangeResponse();
        expectedResponse.setFirstCommit(false);

        when(commitService.getCommitById(commitId)).thenReturn(sampleCommit);
        when(recipeSnapshotService.getRecipeSnapshot(commitId.toString())).thenReturn(sampleRecipeSnapshot);
        when(recipeSnapshotService.getRecipeSnapshot(parentCommit.getId().toString())).thenReturn(sampleRecipeSnapshot);

        // When
        ChangeResponse result = versionControlService.getChanges(commitId);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(commitService).getCommitById(commitId);
        verify(recipeSnapshotService).getRecipeSnapshot(commitId.toString());
        verify(recipeSnapshotService).getRecipeSnapshot(parentCommit.getId().toString());
    }

    // Helper methods to create test data
    private RecipeDetailsDTO createSampleRecipeDetails() {
        RecipeIngredientDTO ingredient = new RecipeIngredientDTO("Flour", "cups", 2.0f);
        RecipeStepDTO step = new RecipeStepDTO(1, "Mix ingredients", List.of());
        
        RecipeDetailsDTO details = new RecipeDetailsDTO();
        details.setServingSize(4);
        details.setRecipeIngredients(Arrays.asList(ingredient));
        details.setRecipeSteps(Arrays.asList(step));
        details.setImages(List.of());
        return details;
    }

    private Branch createSampleBranch() {
        Branch branch = new Branch();
        branch.setId(1L);
        branch.setName("main");
        branch.setRecipeId(1L);
        branch.setHeadCommit(sampleCommit);
        branch.setCreatedAt(createdAt);
        return branch;
    }

    private Commit createSampleCommit() {
        Commit commit = new Commit();
        commit.setId(1L);
        commit.setUserId(testUserId);
        commit.setMessage("Initial commit");
        commit.setCreatedAt(createdAt);
        return commit;
    }

    private RecipeSnapshot createSampleRecipeSnapshot() {
        RecipeSnapshot snapshot = new RecipeSnapshot();
        snapshot.setId("1");
        snapshot.setRecipeId(1L);

        RecipeIngredient ingredient = new RecipeIngredient("Flour", "cups", 2.0f);
        RecipeStep step = new RecipeStep(1, "Mix ingredients", new ArrayList<>());

        RecipeDetails details = new RecipeDetails();
        details.setServingSize(4);
        details.setRecipeIngredients(Arrays.asList(ingredient));
        details.setRecipeSteps(Arrays.asList(step));
        details.setImages(List.of());

        snapshot.setDetails(details);
        return snapshot;
    }

    private BranchDTO createSampleBranchDTO() {
        return new BranchDTO(1L, "main", 1L, 1L, createdAt);
    }

    private CommitDTO createSampleCommitDTO() {
        return new CommitDTO(1L, testUserId, "Initial commit", null, createdAt);
    }

    private CommitDetailsResponse createSampleCommitDetailsResponse() {
        return new CommitDetailsResponse(createSampleCommitDTO(), sampleRecipeDetails);
    }

    private ChangeResponse createSampleChangeResponse() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode changes = mapper.createArrayNode();
        return new ChangeResponse(sampleRecipeDetails, sampleRecipeDetails, changes, true);
    }
}
