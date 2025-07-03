package com.recipefy.version.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility class for common logging operations and MDC management
 * Request IDs are extracted from HTTP headers by RequestLoggingInterceptor
 */
public class LoggingUtil {

    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";
    public static final String RECIPE_ID = "recipeId";
    public static final String BRANCH_ID = "branchId";
    public static final String COMMIT_ID = "commitId";

    /**
     * Generate a fallback request ID when none is provided in headers
     * This should only be used as a fallback in the RequestLoggingInterceptor
     */
    public static String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Set request ID in MDC
     * This is typically called by RequestLoggingInterceptor with ID from headers
     */
    public static void setRequestId(String requestId) {
        MDC.put(REQUEST_ID, requestId);
    }

    /**
     * Set user ID in MDC
     */
    public static void setUserId(Long userId) {
        if (userId != null) {
            MDC.put(USER_ID, userId.toString());
        }
    }

    /**
     * Set recipe ID in MDC
     */
    public static void setRecipeId(Long recipeId) {
        if (recipeId != null) {
            MDC.put(RECIPE_ID, recipeId.toString());
        }
    }

    /**
     * Set branch ID in MDC
     */
    public static void setBranchId(Long branchId) {
        if (branchId != null) {
            MDC.put(BRANCH_ID, branchId.toString());
        }
    }

    /**
     * Set commit ID in MDC
     */
    public static void setCommitId(Long commitId) {
        if (commitId != null) {
            MDC.put(COMMIT_ID, commitId.toString());
        }
    }

    /**
     * Clear all MDC values
     * This is typically called by RequestLoggingInterceptor after request completion
     */
    public static void clearMDC() {
        MDC.clear();
    }

    /**
     * Set common context for recipe operations
     */
    public static void setRecipeContext(Long recipeId, Long userId) {
        setRecipeId(recipeId);
        setUserId(userId);
    }

    /**
     * Set common context for branch operations
     */
    public static void setBranchContext(Long branchId, Long recipeId, Long userId) {
        setBranchId(branchId);
        setRecipeId(recipeId);
        setUserId(userId);
    }

    /**
     * Set common context for commit operations
     */
    public static void setCommitContext(Long commitId, Long branchId, Long recipeId, Long userId) {
        setCommitId(commitId);
        setBranchId(branchId);
        setRecipeId(recipeId);
        setUserId(userId);
    }
}
