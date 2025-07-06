package com.recipefy.recipe.util;

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
    public static void setUserId(UUID userId) {
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
     * Clear all MDC values
     * This is typically called by RequestLoggingInterceptor after request completion
     */
    public static void clearMDC() {
        MDC.clear();
    }

    /**
     * Set common context for recipe operations
     */
    public static void setRecipeContext(Long recipeId, UUID userId) {
        setRecipeId(recipeId);
        setUserId(userId);
    }
} 