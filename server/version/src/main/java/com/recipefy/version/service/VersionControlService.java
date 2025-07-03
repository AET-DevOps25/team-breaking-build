package com.recipefy.version.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.recipefy.version.converter.response.ChangeResponseConverter;
import com.recipefy.version.converter.response.CommitDetailsResponseConverter;
import com.recipefy.version.converter.dto.BranchDTOConverter;
import com.recipefy.version.converter.dto.CommitDTOConverter;
import com.recipefy.version.converter.model.RecipeSnapshotConverter;
import com.recipefy.version.model.dto.BranchDTO;
import com.recipefy.version.model.dto.CommitDTO;
import com.recipefy.version.model.mongo.RecipeDetails;
import com.recipefy.version.model.postgres.Branch;
import com.recipefy.version.model.postgres.Commit;
import com.recipefy.version.model.mongo.RecipeSnapshot;
import com.recipefy.version.model.request.CopyBranchRequest;
import com.recipefy.version.model.request.CommitToBranchRequest;
import com.recipefy.version.model.request.CreateBranchRequest;
import com.recipefy.version.model.request.InitRecipeRequest;
import com.recipefy.version.model.response.ChangeResponse;
import com.recipefy.version.model.response.CommitDetailsResponse;
import com.recipefy.version.util.DiffUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VersionControlService {

    private static final Logger logger = LoggerFactory.getLogger(VersionControlService.class);
    
    private final BranchService branchService;
    private final CommitService commitService;
    private final RecipeSnapshotService recipeSnapshotService;

    @Transactional
    public BranchDTO initRecipe(Long recipeId, InitRecipeRequest initRecipeRequest, Long userId) {
        logger.debug("Starting recipe initialization");
        try {
            branchService.checkIfBranchCreated(recipeId);
            logger.debug("Branch creation check passed");

            Commit commit = commitService.createInitialCommit(userId);
            logger.debug("Created initial commit with ID: {}", commit.getId());

            RecipeSnapshot recipeSnapshot = RecipeSnapshotConverter.toDocument(commit.getId(), recipeId, initRecipeRequest.getRecipeDetails());
            logger.debug("Created recipe snapshot for commitId: {}", commit.getId());

            Branch branch = branchService.createMainBranch(recipeId, commit);
            logger.debug("Created main branch");

            recipeSnapshotService.createRecipeSnapshot(recipeSnapshot);
            logger.info("Successfully initialized recipe with commit ID: {}", commit.getId());

            return BranchDTOConverter.toDTO(branch);
        } catch (Exception e) {
            logger.error("Failed to initialize recipe", e);
            throw e;
        }
    }

    @Transactional
    public List<BranchDTO> getBranchesOfRecipe(Long recipeId) {
        logger.debug("Fetching branches");
        try {
            List<Branch> branches = branchService.getBranchesOfRecipe(recipeId);
            List<BranchDTO> branchDTOs = BranchDTOConverter.toDTOList(branches);
            logger.debug("Found {} branches", branchDTOs.size());
            return branchDTOs;
        } catch (Exception e) {
            logger.error("Failed to fetch branches", e);
            throw e;
        }
    }

    @Transactional
    public BranchDTO createBranch(Long recipeId, CreateBranchRequest createBranchRequest, Long userId) {
        String branchName = createBranchRequest.getBranchName();
        Long sourceBranchId = createBranchRequest.getSourceBranchId();
        
        logger.debug("Creating branch with name: {}, sourceBranchId: {}", branchName, sourceBranchId);
        try {
            Branch sourceBranch = branchService.getBranchById(sourceBranchId);
            logger.debug("Retrieved source branch with ID: {}", sourceBranchId);

            branchService.checkUniqueBranchName(recipeId, branchName);
            logger.debug("Branch name uniqueness check passed for: {}", branchName);

            Branch newBranch = Branch.copyFrom(sourceBranch, branchName, recipeId);
            branchService.saveBranch(newBranch);
            
            BranchDTO branchDTO = BranchDTOConverter.toDTO(newBranch);
            logger.info("Successfully created branch with ID: {}", newBranch.getId());
            return branchDTO;
        } catch (Exception e) {
            logger.error("Failed to create branch with name: {}", branchName, e);
            throw e;
        }
    }

    @Transactional
    public CommitDTO commitToBranch(Long branchId, CommitToBranchRequest commitToBranchRequest, Long userId) {
        String message = commitToBranchRequest.getMessage();
        
        logger.debug("Creating commit with message: {}", message);
        try {
            Branch branch = branchService.getBranchById(branchId);
            logger.debug("Retrieved branch with ID: {}", branchId);

            Commit commit = commitService.createCommit(userId, message, branch.getHeadCommit());
            logger.debug("Created commit with ID: {}", commit.getId());

            branchService.addCommit(branch, commit);
            logger.debug("Added commit to branch with ID: {}", branchId);

            RecipeSnapshot recipeSnapshot = RecipeSnapshotConverter.toDocument(commit.getId(), branch.getRecipeId(), commitToBranchRequest.getRecipeDetails());
            recipeSnapshotService.createRecipeSnapshot(recipeSnapshot);
            logger.debug("Created recipe snapshot for commitId: {}", commit.getId());

            CommitDTO commitDTO = CommitDTOConverter.toDTO(commit);
            logger.info("Successfully created commit with ID: {}", commit.getId());
            return commitDTO;
        } catch (Exception e) {
            logger.error("Failed to create commit", e);
            throw e;
        }
    }

    @Transactional
    public List<CommitDTO> getBranchHistory(Long branchId) {
        logger.debug("Fetching branch history");
        try {
            List<CommitDTO> history = CommitDTOConverter.toDTOList(branchService.getHistory(branchId));
            logger.debug("Found {} commits in history", history.size());
            return history;
        } catch (Exception e) {
            logger.error("Failed to fetch branch history", e);
            throw e;
        }
    }

    @Transactional
    public BranchDTO copyRecipe(Long branchId, CopyBranchRequest copyBranchRequest) {
        try {
            Branch branch = branchService.copyBranch(branchId, copyBranchRequest.getRecipeId());
            BranchDTO branchDTO = BranchDTOConverter.toDTO(branch);
            logger.info("Successfully copied branch to new branch with ID: {}", branch.getId());
            return branchDTO;
        } catch (Exception e) {
            logger.error("Failed to copy branch to recipeId: {}", copyBranchRequest.getRecipeId(), e);
            throw e;
        }
    }

    @Transactional
    public CommitDetailsResponse getCommit(Long commitId) {
        logger.debug("Fetching commit details");
        try {
            Commit commit = commitService.getCommitById(commitId);
            logger.debug("Retrieved commit with ID: {}", commitId);

            RecipeSnapshot recipeSnapshot = recipeSnapshotService.getRecipeSnapshot(commitId.toString());
            logger.debug("Retrieved recipe snapshot for commitId: {}", commitId);

            CommitDetailsResponse response = CommitDetailsResponseConverter.apply(commit, recipeSnapshot);
            logger.debug("Successfully created commit details response");
            return response;
        } catch (Exception e) {
            logger.error("Failed to fetch commit details", e);
            throw e;
        }
    }

    @Transactional
    public ChangeResponse getChanges(Long commitId) {
        logger.debug("Fetching commit changes");
        try {
            Commit commit = commitService.getCommitById(commitId);
            logger.debug("Retrieved commit with ID: {}", commitId);

            RecipeDetails newRecipe = getRecipeDetails(commitId);
            logger.debug("Retrieved new recipe details");

            Commit parentCommit = commit.getParent();
            boolean firstCommit = parentCommit == null;
            
            if (firstCommit) {
                logger.debug("This is the first commit");
            } else {
                logger.debug("Parent commit ID: {}", parentCommit.getId());
            }

            RecipeDetails oldRecipe = !firstCommit
                    ? getRecipeDetails(parentCommit.getId())
                    : new RecipeDetails();

            JsonNode changes = DiffUtil.compareRecipes(oldRecipe, newRecipe);
            ChangeResponse response = ChangeResponseConverter.apply(oldRecipe, newRecipe, changes, firstCommit);
            logger.debug("Successfully created change response");
            return response;
        } catch (Exception e) {
            logger.error("Failed to fetch commit changes", e);
            throw e;
        }
    }

    private RecipeDetails getRecipeDetails(Long commitId) {
        logger.trace("Getting recipe details for commitId: {}", commitId);
        return recipeSnapshotService.getRecipeSnapshot(commitId.toString()).getDetails();
    }
}
