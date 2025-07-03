package com.recipefy.version.integration;

import java.util.Arrays;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipefy.version.model.dto.RecipeDetailsDTO;
import com.recipefy.version.model.dto.RecipeIngredientDTO;
import com.recipefy.version.model.dto.RecipeStepDTO;
import com.recipefy.version.model.request.CommitToBranchRequest;
import com.recipefy.version.model.request.CopyBranchRequest;
import com.recipefy.version.model.request.CreateBranchRequest;
import com.recipefy.version.model.request.InitRecipeRequest;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VersionControlIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void overrideMongoProperties(DynamicPropertyRegistry registry) {
        mongoDBContainer.start();
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void clearDB() {
        // Drop all collections
        for (String collectionName : mongoTemplate.getCollectionNames()) {
            mongoTemplate.dropCollection(collectionName);
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void fullRecipeVersionControlFlow_ShouldWorkEndToEnd() throws Exception {
        // Step 1: Initialize a recipe
        Long recipeId = 1L;
        Long userId = 1L;
        RecipeDetailsDTO initialRecipe = createSampleRecipeDetails("Chocolate Cake", 4);
        InitRecipeRequest initRequest = new InitRecipeRequest(initialRecipe);

        mockMvc.perform(post("/vcs/recipes/{recipeId}/init", recipeId)
                .header("X-User-ID", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipeId").value(1))
                .andExpect(jsonPath("$.id").value(1));

        // Step 2: Get branches (should have main branch)
        mockMvc.perform(get("/vcs/recipes/{recipeId}/branches", recipeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("main"))
                .andExpect(jsonPath("$[0].recipeId").value(recipeId));

        // Step 3: Create a feature branch
        CreateBranchRequest createBranchRequest = new CreateBranchRequest("feature-branch", 1L);
        mockMvc.perform(post("/vcs/recipes/{recipeId}/branches", recipeId)
                .header("X-User-ID", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBranchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("feature-branch"))
                .andExpect(jsonPath("$.recipeId").value(recipeId));

        // Step 4: Commit changes to the feature branch
        RecipeDetailsDTO updatedRecipe = createSampleRecipeDetails("Chocolate Cake", 6);
        CommitToBranchRequest commitRequest = new CommitToBranchRequest("Update serving size", updatedRecipe);
        
        mockMvc.perform(post("/vcs/branches/{branchId}/commit", 2L) // feature branch ID
                .header("X-User-ID", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Update serving size"))
                .andExpect(jsonPath("$.userId").value(userId));

        // Step 5: Get branch history
        mockMvc.perform(get("/vcs/branches/{branchId}/history", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].message").value("Update serving size"));

        // Step 6: Get commit details
        mockMvc.perform(get("/vcs/commits/{commitId}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commitMetadata.message").value("Update serving size"))
                .andExpect(jsonPath("$.recipeDetails").exists());

        // Step 7: Get changes between commits
        mockMvc.perform(get("/vcs/commits/{commitId}/changes", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oldDetails").exists())
                .andExpect(jsonPath("$.currentDetails").exists())
                .andExpect(jsonPath("$.changes").exists());

        // Step 8: Copy recipe to new recipe
        CopyBranchRequest copyRequest = new CopyBranchRequest(2L);
        mockMvc.perform(post("/vcs/branches/{branchId}/copy", 2L)
                .header("X-User-ID", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(copyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipeId").value(2L));
    }

    @Test
    void initRecipe_ShouldFail_WhenRecipeAlreadyInitialized() throws Exception {
        // Given
        Long recipeId = 1L;
        Long userId = 1L;
        RecipeDetailsDTO recipe = createSampleRecipeDetails("Test Recipe", 4);
        InitRecipeRequest request = new InitRecipeRequest(recipe);

        // First initialization should succeed
        mockMvc.perform(post("/vcs/recipes/{recipeId}/init", recipeId)
                .header("X-User-ID", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Second initialization should fail
        mockMvc.perform(post("/vcs/recipes/{recipeId}/init", recipeId)
                .header("X-User-ID", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBranch_ShouldFail_WhenBranchNameNotUnique() throws Exception {
        // Given
        Long recipeId = 1L;
        Long userId = 1L;
        
        // Initialize recipe
        RecipeDetailsDTO recipe = createSampleRecipeDetails("Test Recipe", 4);
        InitRecipeRequest initRequest = new InitRecipeRequest(recipe);
        mockMvc.perform(post("/vcs/recipes/{recipeId}/init", recipeId)
                .header("X-User-ID", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest)))
                .andExpect(status().isOk());

        // Create first branch
        CreateBranchRequest createRequest = new CreateBranchRequest("duplicate-branch", 1L);
        mockMvc.perform(post("/vcs/recipes/{recipeId}/branches", recipeId)
                .header("X-User-ID", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        // Create second branch with same name should fail
        mockMvc.perform(post("/vcs/recipes/{recipeId}/branches", recipeId)
                .header("X-User-ID", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void commitToBranch_ShouldFail_WhenBranchNotFound() throws Exception {
        // Given
        Long recipeId = 1L;
        Long nonExistentBranchId = 999L;
        Long userId = 1L;
        RecipeDetailsDTO recipe = createSampleRecipeDetails("Test Recipe", 4);
        CommitToBranchRequest request = new CommitToBranchRequest("Test commit", recipe);

        // When & Then
        mockMvc.perform(post("/vcs/branches/{branchId}/commit", recipeId, nonExistentBranchId)
                .header("X-User-ID", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCommit_ShouldFail_WhenCommitNotFound() throws Exception {
        // Given
        Long recipeId = 1L;
        Long nonExistentCommitId = 999L;

        // When & Then
        mockMvc.perform(get("/vcs/commits/{commitId}", recipeId, nonExistentCommitId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getChanges_ShouldFail_WhenCommitNotFound() throws Exception {
        // Given
        Long recipeId = 1L;
        Long nonExistentCommitId = 999L;

        // When & Then
        mockMvc.perform(get("/vcs/commits/{commitId}/changes", recipeId, nonExistentCommitId))
                .andExpect(status().isNotFound());
    }

    @Test
    void copyRecipe_ShouldFail_WhenSourceBranchNotFound() throws Exception {
        // Given
        Long recipeId = 1L;
        Long nonExistentBranchId = 999L;
        CopyBranchRequest request = new CopyBranchRequest(2L);

        // When & Then
        mockMvc.perform(post("/vcs/branches/{branchId}/copy", recipeId, nonExistentBranchId)
                .header("X-User-ID", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void validation_ShouldFail_WhenRequiredFieldsMissing() throws Exception {
        // Test InitRecipeRequest validation
        InitRecipeRequest invalidInitRequest = new InitRecipeRequest( null);
        mockMvc.perform(post("/vcs/recipes/{recipeId}/init", 1L)
                .header("X-User-ID", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidInitRequest)))
                .andExpect(status().isBadRequest());

        // Test CreateBranchRequest validation
        CreateBranchRequest invalidCreateRequest = new CreateBranchRequest("", null);
        mockMvc.perform(post("/vcs/recipes/{recipeId}/branches", 1L)
                .header("X-User-ID", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCreateRequest)))
                .andExpect(status().isBadRequest());

        // Test CommitToBranchRequest validation
        CommitToBranchRequest invalidCommitRequest = new CommitToBranchRequest("", null);
        mockMvc.perform(post("/vcs/branches/{branchId}/commit", 1L, 1L)
                .header("X-User-ID", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCommitRequest)))
                .andExpect(status().isBadRequest());

        // Test CopyBranchRequest validation
        CopyBranchRequest invalidCopyRequest = new CopyBranchRequest(null);
        mockMvc.perform(post("/vcs/branches/{branchId}/copy", 1L, 1L)
                .header("X-User-ID", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCopyRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void multipleCommits_ShouldMaintainCorrectHistory() throws Exception {
        // Given
        Long recipeId = 1L;
        Long userId = 1L;
        
        // Initialize recipe
        RecipeDetailsDTO initialRecipe = createSampleRecipeDetails("Test Recipe", 4);
        InitRecipeRequest initRequest = new InitRecipeRequest(initialRecipe);
        mockMvc.perform(post("/vcs/recipes/{recipeId}/init", recipeId)
                .header("X-User-ID", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest)))
                .andExpect(status().isOk());

        // Create multiple commits
        for (int i = 1; i <= 3; i++) {
            RecipeDetailsDTO updatedRecipe = createSampleRecipeDetails("Test Recipe", 4 + i);
            CommitToBranchRequest commitRequest = new CommitToBranchRequest("Commit " + i, updatedRecipe);
            
            mockMvc.perform(post("/vcs/branches/{branchId}/commit", 1L)
                    .header("X-User-ID", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commitRequest)))
                    .andExpect(status().isOk());
        }

        // Get history and verify order
        mockMvc.perform(get("/vcs/branches/{branchId}/history", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].message").value("Commit 3"))
                .andExpect(jsonPath("$[1].message").value("Commit 2"))
                .andExpect(jsonPath("$[2].message").value("Commit 1"))
                .andExpect(jsonPath("$[3].message").value("create recipe."));
    }

    // Helper methods to create test data
    private RecipeDetailsDTO createSampleRecipeDetails(String name, int servingSize) {
        RecipeIngredientDTO ingredient = new RecipeIngredientDTO("Flour", "cups", 2.0f);
        RecipeStepDTO step = new RecipeStepDTO(1, "Mix ingredients", null);
        
        RecipeDetailsDTO details = new RecipeDetailsDTO();
        details.setServingSize(servingSize);
        details.setRecipeIngredients(Arrays.asList(ingredient));
        details.setRecipeSteps(Arrays.asList(step));
        return details;
    }
}
