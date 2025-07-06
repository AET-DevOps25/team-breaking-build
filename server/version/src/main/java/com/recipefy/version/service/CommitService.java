package com.recipefy.version.service;

import com.recipefy.version.exception.BusinessException;
import com.recipefy.version.model.postgres.Commit;
import com.recipefy.version.repository.postgres.CommitRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.UUID;

import static com.recipefy.version.constants.ApplicationConstants.INITIAL_COMMIT_MESSAGE;
import static com.recipefy.version.constants.LogMessages.COMMIT_NOT_FOUND_PRE_MESSAGE;

@Service
@RequiredArgsConstructor
public class CommitService {

    private static final Logger logger = LoggerFactory.getLogger(CommitService.class);
    
    private final CommitRepository commitRepository;

    public Commit createInitialCommit(UUID userId) {
        logger.debug("Creating initial commit");
        try {
            Commit commit = createCommit(userId, INITIAL_COMMIT_MESSAGE, null);
            logger.info("Successfully created initial commit with ID: {}", commit.getId());
            return commit;
        } catch (Exception e) {
            logger.error("Failed to create initial commit", e);
            throw e;
        }
    }

    public Commit createCommit(UUID userId, String message, Commit parent) {
        logger.debug("Creating commit with message: {}, parentCommitId: {}", 
                    message, parent != null ? parent.getId() : "null");
        try {
            Commit commit = new Commit();
            commit.setUserId(userId);
            commit.setMessage(message);
            commit.setParent(parent);
            
            Commit savedCommit = commitRepository.save(commit);
            logger.debug("Successfully created commit with ID: {}", savedCommit.getId());
            return savedCommit;
        } catch (Exception e) {
            logger.error("Failed to create commit with message: {}", message, e);
            throw e;
        }
    }

    public Commit getCommitById(Long commitId) {
        logger.debug("Fetching commit by ID: {}", commitId);
        try {
            Commit commit = commitRepository.findById(commitId)
                    .orElseThrow(() -> {
                        logger.warn("Commit not found");
                        return new BusinessException(COMMIT_NOT_FOUND_PRE_MESSAGE + getNotFoundPostfixMessage(commitId), HttpStatus.NOT_FOUND);
                    });
            logger.debug("Successfully retrieved commit");
            return commit;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to fetch commit", e);
            throw e;
        }
    }

    private String getNotFoundPostfixMessage(Long commitId) {
        return " CommitId: " + commitId;
    }
}
