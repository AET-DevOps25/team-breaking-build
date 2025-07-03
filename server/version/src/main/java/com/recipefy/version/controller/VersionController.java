package com.recipefy.version.controller;

import com.recipefy.version.annotation.LogContext;
import com.recipefy.version.model.dto.BranchDTO;
import com.recipefy.version.model.dto.CommitDTO;
import com.recipefy.version.model.dto.RecipeDetailsDTO;
import com.recipefy.version.model.request.CopyBranchRequest;
import com.recipefy.version.model.request.CommitToBranchRequest;
import com.recipefy.version.model.request.CreateBranchRequest;
import com.recipefy.version.model.request.InitRecipeRequest;
import com.recipefy.version.model.response.ChangeResponse;
import com.recipefy.version.model.response.CommitDetailsResponse;
import com.recipefy.version.service.VersionControlService;
import com.recipefy.version.util.HeaderUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/vcs")
@RequiredArgsConstructor
public class VersionController {

    private static final Logger logger = LoggerFactory.getLogger(VersionController.class);
    private final VersionControlService vcsService;

    @PostMapping("/recipes/{recipeId}/init")
    @LogContext(extractRecipeIdFromPath = true, extractUserIdFromHeader = true)
    public ResponseEntity<BranchDTO> initRecipe(
            @PathVariable Long recipeId,
            @RequestBody @Valid InitRecipeRequest initRecipeRequest) {
        
        Long userId = HeaderUtil.extractRequiredUserIdFromHeader();
        logger.info("Initializing recipe");
        try {
            BranchDTO branch = vcsService.initRecipe(recipeId, initRecipeRequest, userId);
            logger.info("Successfully initialized recipe");
            return ResponseEntity.ok(branch);
        } catch (Exception e) {
            logger.error("Failed to initialize recipe", e);
            throw e;
        }
    }

    @GetMapping("/recipes/{recipeId}/branches")
    @LogContext(extractRecipeIdFromPath = true)
    public ResponseEntity<List<BranchDTO>> getBranches(@PathVariable Long recipeId) {
        logger.debug("Fetching branches");
        try {
            List<BranchDTO> branches = vcsService.getBranchesOfRecipe(recipeId);
            logger.debug("Found {} branches", branches.size());
            return ResponseEntity.ok(branches);
        } catch (Exception e) {
            logger.error("Failed to fetch branches", e);
            throw e;
        }
    }

    @PostMapping("/recipes/{recipeId}/branches")
    @LogContext(extractRecipeIdFromPath = true, extractUserIdFromHeader = true)
    public ResponseEntity<BranchDTO> createBranch(
            @PathVariable Long recipeId,
            @RequestBody @Valid CreateBranchRequest createBranchRequest) {
        
        Long userId = HeaderUtil.extractRequiredUserIdFromHeader();
        logger.info("Creating branch with name: {}, sourceBranchId: {}", 
                   createBranchRequest.getBranchName(), createBranchRequest.getSourceBranchId());
        try {
            BranchDTO branch = vcsService.createBranch(recipeId, createBranchRequest, userId);
            logger.info("Successfully created branch with ID: {}", branch.getId());
            return ResponseEntity.ok(branch);
        } catch (Exception e) {
            logger.error("Failed to create branch", e);
            throw e;
        }
    }

    @PostMapping("/branches/{branchId}/commit")
    @LogContext(extractBranchIdFromPath = true, extractUserIdFromHeader = true)
    public ResponseEntity<CommitDTO> commitToBranch(
            @PathVariable Long branchId,
            @RequestBody @Valid CommitToBranchRequest commitToBranchRequest) {
        
        Long userId = HeaderUtil.extractRequiredUserIdFromHeader();
        logger.info("Creating commit with message: {}", commitToBranchRequest.getMessage());
        try {
            CommitDTO commit = vcsService.commitToBranch(branchId, commitToBranchRequest, userId);
            logger.info("Successfully created commit with ID: {}", commit.getId());
            return ResponseEntity.ok(commit);
        } catch (Exception e) {
            logger.error("Failed to create commit", e);
            throw e;
        }
    }

    @GetMapping("/branches/{branchId}/history")
    @LogContext(extractBranchIdFromPath = true)
    public ResponseEntity<List<CommitDTO>> getHistory(@PathVariable Long branchId) {
        logger.debug("Fetching branch history");
        try {
            List<CommitDTO> history = vcsService.getBranchHistory(branchId);
            logger.debug("Found {} commits in history", history.size());
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Failed to fetch branch history", e);
            throw e;
        }
    }

    @PostMapping("/branches/{branchId}/copy")
    @LogContext(extractBranchIdFromPath = true, extractUserIdFromHeader = true)
    public ResponseEntity<BranchDTO> copyRecipe(
            @PathVariable Long branchId,
            @RequestBody @Valid CopyBranchRequest copyBranchRequest) {
        try {
            BranchDTO copiedBranch = vcsService.copyRecipe(branchId, copyBranchRequest);
            logger.info("Successfully copied recipe to new branch ID: {}", copiedBranch.getId());
            return ResponseEntity.ok(copiedBranch);
        } catch (Exception e) {
            logger.error("Failed to copy branch", e);
            throw e;
        }
    }

    @GetMapping("/commits/{commitId}")
    @LogContext(extractCommitIdFromPath = true)
    public ResponseEntity<CommitDetailsResponse> getCommit(@PathVariable Long commitId) {
        logger.debug("Fetching commit details");
        try {
            CommitDetailsResponse commitDetails = vcsService.getCommit(commitId);
            logger.debug("Successfully retrieved commit details");
            return ResponseEntity.ok(commitDetails);
        } catch (Exception e) {
            logger.error("Failed to fetch commit details", e);
            throw e;
        }
    }

    @GetMapping("/commits/{commitId}/changes")
    @LogContext(extractCommitIdFromPath = true)
    public ResponseEntity<ChangeResponse> getChanges(@PathVariable Long commitId) {
        logger.debug("Fetching commit changes");
        try {
            ChangeResponse changes = vcsService.getChanges(commitId);
            logger.debug("Successfully retrieved commit changes");
            return ResponseEntity.ok(changes);
        } catch (Exception e) {
            logger.error("Failed to fetch commit changes", e);
            throw e;
        }
    }
}
