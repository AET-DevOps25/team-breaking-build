package com.recipefy.version.service;

import com.recipefy.version.exception.BusinessException;
import com.recipefy.version.model.mongo.RecipeSnapshot;
import com.recipefy.version.repository.mongo.RecipeSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static com.recipefy.version.constants.LogMessages.RECIPE_NOT_FOUND_PRE_MESSAGE;

@Service
@RequiredArgsConstructor
public class RecipeSnapshotService {

    private static final Logger logger = LoggerFactory.getLogger(RecipeSnapshotService.class);
    
    private final RecipeSnapshotRepository recipeSnapshotRepository;

    public RecipeSnapshot createRecipeSnapshot(RecipeSnapshot recipeSnapshot) {
        logger.debug("Creating recipe snapshot with ID: {}", recipeSnapshot.getId());
        try {
            RecipeSnapshot savedSnapshot = recipeSnapshotRepository.save(recipeSnapshot);
            logger.debug("Successfully created recipe snapshot with ID: {}", savedSnapshot.getId());
            return savedSnapshot;
        } catch (Exception e) {
            logger.error("Failed to create recipe snapshot", e);
            throw e;
        }
    }

    public RecipeSnapshot getRecipeSnapshot(String id) {
        logger.debug("Fetching recipe snapshot by ID: {}", id);
        try {
            RecipeSnapshot snapshot = recipeSnapshotRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.warn("Recipe snapshot not found with ID: {}", id);
                        return new BusinessException(RECIPE_NOT_FOUND_PRE_MESSAGE + getNotFoundPostfixMessage(id), HttpStatus.NOT_FOUND);
                    });
            logger.debug("Successfully retrieved recipe snapshot with ID: {}", id);
            return snapshot;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to fetch recipe snapshot by ID: {}", id, e);
            throw e;
        }
    }

    private String getNotFoundPostfixMessage(String id) {
        return " RecipeSnapshotId: " + id;
    }
}
