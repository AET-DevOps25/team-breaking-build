package com.recipefy.version.controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipefy.version.model.dto.BranchDTO;
import com.recipefy.version.model.dto.CommitDTO;
import com.recipefy.version.model.dto.RecipeDetailsDTO;
import com.recipefy.version.model.dto.RecipeIngredientDTO;
import com.recipefy.version.model.dto.RecipeStepDTO;
import com.recipefy.version.model.request.CommitToBranchRequest;
import com.recipefy.version.model.request.CopyBranchRequest;
import com.recipefy.version.model.request.CreateBranchRequest;
import com.recipefy.version.model.request.InitRecipeRequest;
import com.recipefy.version.model.response.ChangeResponse;
import com.recipefy.version.model.response.CommitDetailsResponse;
import com.recipefy.version.service.VersionControlService;

@ExtendWith(MockitoExtension.class)
class VersionControllerTest {

    @Mock
    private VersionControlService vcsService;

    @InjectMocks
    private VersionController versionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(versionController).build();
        objectMapper = new ObjectMapper();
        testUserId = UUID.randomUUID();
    }

    @Test
    void initRecipe_ShouldReturnBranchDetails_WhenValidRequest() throws Exception {
        // Given
        Long recipeId = 1L;
        RecipeDetailsDTO recipeDetails = createSampleRecipeDetails();
        InitRecipeRequest request = new InitRecipeRequest(recipeDetails);

        BranchDTO mockBranchDTO = new BranchDTO();
        mockBranchDTO.setId(1L);
        mockBranchDTO.setRecipeId(1L);

        when(vcsService.initRecipe(recipeId, request, testUserId)).thenReturn(mockBranchDTO);

        // When & Then
        mockMvc.perform(post("/vcs/recipes/{recipeId}/init", recipeId)
                        .header("X-User-ID", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.recipeId").value(1));
    }

    @Test
    void initRecipe_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        // Given
        Long recipeId = 1L;
        InitRecipeRequest request = new InitRecipeRequest(null);

        // When & Then
        mockMvc.perform(post("/vcs/recipes/{recipeId}/init", recipeId)
                .header("X-User-ID", testUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBranches_ShouldReturnBranchesList_WhenValidRecipeId() throws Exception {
        // Given
        Long recipeId = 1L;
        List<BranchDTO> branches = Arrays.asList(
                createSampleBranchDTO(1L, "main", recipeId),
                createSampleBranchDTO(2L, "feature", recipeId)
        );

        when(vcsService.getBranchesOfRecipe(recipeId)).thenReturn(branches);

        // When & Then
        mockMvc.perform(get("/vcs/recipes/{recipeId}/branches", recipeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("main"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("feature"));
    }

    @Test
    void createBranch_ShouldReturnBranchDTO_WhenValidRequest() throws Exception {
        // Given
        Long recipeId = 1L;
        CreateBranchRequest request = new CreateBranchRequest("new-branch", 1L);
        BranchDTO branchDTO = createSampleBranchDTO(3L, "new-branch", recipeId);

        when(vcsService.createBranch(recipeId, request, testUserId)).thenReturn(branchDTO);

        // When & Then
        mockMvc.perform(post("/vcs/recipes/{recipeId}/branches", recipeId)
                .header("X-User-ID", testUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("new-branch"))
                .andExpect(jsonPath("$.recipeId").value(recipeId));
    }

    @Test
    void createBranch_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        // Given
        Long recipeId = 1L;
        CreateBranchRequest request = new CreateBranchRequest("", null);

        // When & Then
        mockMvc.perform(post("/vcs/recipes/{recipeId}/branches", recipeId)
                .header("X-User-ID", testUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void commitToBranch_ShouldReturnCommitDTO_WhenValidRequest() throws Exception {
        // Given
        Long recipeId = 1L;
        Long branchId = 1L;
        RecipeDetailsDTO recipeDetails = createSampleRecipeDetails();
        CommitToBranchRequest request = new CommitToBranchRequest("Update recipe", recipeDetails);
        CommitDTO commitDTO = createSampleCommitDTO(1L, testUserId, "Update recipe", null);

        when(vcsService.commitToBranch(branchId, request, testUserId)).thenReturn(commitDTO);

        // When & Then
        mockMvc.perform(post("/vcs/branches/{branchId}/commit", recipeId, branchId)
                .header("X-User-ID", testUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.message").value("Update recipe"));
    }

    @Test
    void commitToBranch_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        // Given
        Long recipeId = 1L;
        Long branchId = 1L;
        CommitToBranchRequest request = new CommitToBranchRequest("", null);

        // When & Then
        mockMvc.perform(post("/vcs/branches/{branchId}/commit", recipeId, branchId)
                .header("X-User-ID", testUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistory_ShouldReturnCommitsList_WhenValidBranchId() throws Exception {
        // Given
        Long recipeId = 1L;
        Long branchId = 1L;
        List<CommitDTO> commits = Arrays.asList(
                createSampleCommitDTO(1L, testUserId, "Initial commit", null),
                createSampleCommitDTO(2L, testUserId, "Update recipe", 1L)
        );

        when(vcsService.getBranchHistory(branchId)).thenReturn(commits);

        // When & Then
        mockMvc.perform(get("/vcs/branches/{branchId}/history", recipeId, branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].message").value("Initial commit"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].message").value("Update recipe"));
    }

    @Test
    void copyRecipe_ShouldReturnBranchDTO_WhenValidRequest() throws Exception {
        // Given
        Long recipeId = 1L;
        Long branchId = 1L;
        CopyBranchRequest request = new CopyBranchRequest(2L);
        BranchDTO branchDTO = createSampleBranchDTO(4L, "main", 2L);

        when(vcsService.copyRecipe(branchId, request)).thenReturn(branchDTO);

        // When & Then
        mockMvc.perform(post("/vcs/branches/{branchId}/copy", recipeId, branchId)
                .header("X-User-ID", testUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.name").value("main"))
                .andExpect(jsonPath("$.recipeId").value(2));
    }

    @Test
    void copyRecipe_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        // Given
        Long recipeId = 1L;
        Long branchId = 1L;
        CopyBranchRequest request = new CopyBranchRequest(null);

        // When & Then
        mockMvc.perform(post("/vcs/branches/{branchId}/copy", recipeId, branchId)
                .header("X-User-ID", testUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCommit_ShouldReturnCommitDetails_WhenValidCommitId() throws Exception {
        // Given
        Long recipeId = 1L;
        Long commitId = 1L;
        CommitDetailsResponse response = createSampleCommitDetailsResponse();

        when(vcsService.getCommit(commitId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/vcs/commits/{commitId}", recipeId, commitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commitMetadata.id").value(1))
                .andExpect(jsonPath("$.commitMetadata.message").value("Initial commit"))
                .andExpect(jsonPath("$.recipeDetails").exists());
    }

    @Test
    void getChanges_ShouldReturnChangeResponse_WhenValidCommitId() throws Exception {
        // Given
        Long recipeId = 1L;
        Long commitId = 1L;
        ChangeResponse response = createSampleChangeResponse();

        when(vcsService.getChanges(commitId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/vcs/commits/{commitId}/changes", recipeId, commitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oldDetails").exists())
                .andExpect(jsonPath("$.currentDetails").exists());
    }

    // Helper methods to create test data
    private RecipeDetailsDTO createSampleRecipeDetails() {
        RecipeIngredientDTO ingredient = new RecipeIngredientDTO();
        ingredient.setName("Flour");
        ingredient.setAmount(2.0f);
        ingredient.setUnit("cups");

        RecipeStepDTO step = new RecipeStepDTO();
        step.setOrder(1);
        step.setDetails("Mix ingredients");

        RecipeDetailsDTO details = new RecipeDetailsDTO();
        details.setServingSize(4);
        details.setRecipeIngredients(Arrays.asList(ingredient));
        details.setRecipeSteps(Arrays.asList(step));
        return details;
    }

    private BranchDTO createSampleBranchDTO(Long id, String name, Long recipeId) {
        return new BranchDTO(id, name, recipeId, 1L, LocalDateTime.now());
    }

    private CommitDTO createSampleCommitDTO(Long id, UUID userId, String message, Long parentId) {
        return new CommitDTO(id, userId, message, parentId, LocalDateTime.now());
    }

    private CommitDetailsResponse createSampleCommitDetailsResponse() {
        CommitDTO commit = createSampleCommitDTO(1L, testUserId, "Initial commit", null);
        RecipeDetailsDTO recipeDetails = createSampleRecipeDetails();
        return new CommitDetailsResponse(commit, recipeDetails);
    }

    private ChangeResponse createSampleChangeResponse() {
        RecipeDetailsDTO oldRecipe = createSampleRecipeDetails();
        RecipeDetailsDTO newRecipe = createSampleRecipeDetails();
        newRecipe.setServingSize(6);
        return new ChangeResponse(oldRecipe, newRecipe, null, true);
    }
}
