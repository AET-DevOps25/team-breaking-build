package com.recipefy.version.service;

import com.recipefy.version.exception.BusinessException;
import com.recipefy.version.model.postgres.Branch;
import com.recipefy.version.model.postgres.Commit;
import com.recipefy.version.repository.postgres.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.recipefy.version.constants.ApplicationConstants.MAIN_BRANCH_NAME;
import static com.recipefy.version.constants.LogMessages.BRANCH_NOT_FOUND_PRE_MESSAGE;
import static com.recipefy.version.constants.LogMessages.BRANCH_NOT_UNIQUE_PRE_MESSAGE;

@Service
@RequiredArgsConstructor
public class BranchService {

    private static final Logger logger = LoggerFactory.getLogger(BranchService.class);
    
    private final BranchRepository branchRepository;

    public Branch saveBranch(Branch branch) {
        logger.debug("Saving branch with name: {}", branch.getName());
        try {
            Branch savedBranch = branchRepository.save(branch);
            logger.debug("Successfully saved branch with ID: {}", savedBranch.getId());
            return savedBranch;
        } catch (Exception e) {
            logger.error("Failed to save branch with name: {}", branch.getName(), e);
            throw e;
        }
    }

    public Branch createMainBranch(Long recipeId, Commit commit) {
        logger.debug("Creating main branch with commitId: {}", commit.getId());
        try {
            Branch mainBranch = createBranch(recipeId, MAIN_BRANCH_NAME, commit);
            logger.info("Successfully created main branch with ID: {}", mainBranch.getId());
            return mainBranch;
        } catch (Exception e) {
            logger.error("Failed to create main branch", e);
            throw e;
        }
    }

    public Branch createBranch(Long recipeId, String branchName, Commit commit) {
        logger.debug("Creating branch with name: {}, commitId: {}", branchName, commit.getId());
        try {
            Branch branch = new Branch();
            branch.setName(branchName);
            branch.setRecipeId(recipeId);
            branch.addCommit(commit);
            branch.setHeadCommit(commit);
            
            Branch savedBranch = branchRepository.save(branch);
            logger.debug("Successfully created branch with ID: {}", savedBranch.getId());
            return savedBranch;
        } catch (Exception e) {
            logger.error("Failed to create branch with name: {}", branchName, e);
            throw e;
        }
    }

    public Branch addCommit(Branch branch, Commit commit) {
        logger.debug("Adding commit with ID: {} to branch with ID: {}", commit.getId(), branch.getId());
        try {
            branch.addCommit(commit);
            Branch updatedBranch = branchRepository.save(branch);
            logger.debug("Successfully added commit with ID: {} to branch with ID: {}", commit.getId(), branch.getId());
            return updatedBranch;
        } catch (Exception e) {
            logger.error("Failed to add commit with ID: {} to branch with ID: {}", commit.getId(), branch.getId(), e);
            throw e;
        }
    }

    public List<Branch> getBranchesOfRecipe(Long recipeId) {
        logger.debug("Fetching branches");
        try {
            List<Branch> branches = branchRepository.findByRecipeId(recipeId);
            logger.debug("Found {} branches", branches.size());
            return branches;
        } catch (Exception e) {
            logger.error("Failed to fetch branches", e);
            throw e;
        }
    }

    public List<Commit> getHistory(Long branchId) {
        logger.debug("Fetching branch history");
        try {
            Branch branch = getBranchById(branchId);
            List<Commit> history = branch.getCommits().stream()
                    .sorted(Comparator.comparing(Commit::getCreatedAt).reversed())
                    .collect(Collectors.toList());
            logger.debug("Found {} commits in history", history.size());
            return history;
        } catch (Exception e) {
            logger.error("Failed to fetch branch history", e);
            throw e;
        }
    }

    public Branch copyBranch(Long branchId, Long recipeId) {
        logger.debug("Copying branch to recipeId: {}", recipeId);
        try {
            Branch existingBranch = getBranchById(branchId);
            logger.debug("Retrieved source branch with ID: {} for copying", branchId);

            Branch newBranch = new Branch();
            newBranch.setName(MAIN_BRANCH_NAME);
            newBranch.setRecipeId(recipeId);
            newBranch.setHeadCommit(existingBranch.getHeadCommit());

            Set<Commit> copiedCommits = new HashSet<>(existingBranch.getCommits());
            newBranch.setCommits(copiedCommits);

            Branch savedBranch = branchRepository.save(newBranch);
            logger.info("Successfully copied branch to new branch with ID: {}", savedBranch.getId());
            return savedBranch;
        } catch (Exception e) {
            logger.error("Failed to copy branch to recipeId: {}", recipeId, e);
            throw e;
        }
    }

    public List<Branch> getBranchesByRecipeId(Long recipeId) {
        logger.debug("Fetching branches by recipeId");
        try {
            List<Branch> branches = branchRepository.findByRecipeId(recipeId);
            logger.debug("Found {} branches", branches.size());
            return branches;
        } catch (Exception e) {
            logger.error("Failed to fetch branches by recipeId", e);
            throw e;
        }
    }

    public Branch getBranchById(Long branchId) {
        logger.debug("Fetching branch by ID: {}", branchId);
        try {
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> {
                        logger.warn("Branch not found with ID: {}", branchId);
                        return new BusinessException(BRANCH_NOT_FOUND_PRE_MESSAGE + getNotFoundPostfixMessage(branchId), HttpStatus.NOT_FOUND);
                    });
            logger.debug("Successfully retrieved branch with ID: {}", branchId);
            return branch;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to fetch branch by ID: {}", branchId, e);
            throw e;
        }
    }

    public void checkIfBranchCreated(Long recipeId) {
        logger.debug("Checking if branch is already created");
        try {
            checkUniqueBranchName(recipeId, MAIN_BRANCH_NAME);
            logger.debug("Branch creation check passed");
        } catch (Exception e) {
            logger.error("Branch creation check failed", e);
            throw e;
        }
    }

    public void checkUniqueBranchName(Long recipeId, String branchName) {
        logger.debug("Checking unique branch name: {}", branchName);
        try {
            if (branchRepository.findByRecipeIdAndName(recipeId, branchName).isPresent()) {
                logger.warn("Branch name already exists: {}", branchName);
                throw new BusinessException(BRANCH_NOT_UNIQUE_PRE_MESSAGE + getNotUniquePostfixMessage(recipeId, branchName), HttpStatus.BAD_REQUEST);
            }
            logger.debug("Branch name uniqueness check passed: {}", branchName);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to check unique branch name: {}", branchName, e);
            throw e;
        }
    }

    private String getNotFoundPostfixMessage(Long branchId) {
        return " BranchId: " + branchId;
    }

    private String getNotUniquePostfixMessage(Long recipeId, String branchName) {
        return " RecipeId: " + recipeId + " BranchName: " + branchName;
    }
}
