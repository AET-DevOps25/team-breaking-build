package com.recipefy.version.aspect;

import com.recipefy.version.annotation.LogContext;
import com.recipefy.version.exception.ValidationException;
import com.recipefy.version.util.HeaderUtil;
import com.recipefy.version.util.LoggingUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * AOP Aspect to automatically set logging context based on method parameters and annotations
 */
@Aspect
@Component
@Slf4j
public class LoggingContextAspect {

    // Common header names for user ID
    private static final String[] USER_ID_HEADERS = {
        "X-User-ID",
        "X-User-Id", 
        "User-ID",
        "User-Id",
        "Authorization" // Could contain user info in JWT
    };

    @Before("@annotation(com.recipefy.version.annotation.LogContext)")
    public void setLoggingContext(JoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            LogContext logContext = method.getAnnotation(LogContext.class);
            
            if (logContext == null) {
                return;
            }

            Object[] args = joinPoint.getArgs();
            Parameter[] parameters = method.getParameters();
            
            Map<String, Object> contextValues = new HashMap<>();
            
            // Extract values from method parameters
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                Object arg = args[i];
                
                if (arg == null) continue;
                
                String paramName = parameter.getName();
                
                // Check if this parameter matches any of the context fields
                if (!logContext.recipeId().isEmpty() && logContext.recipeId().equals(paramName)) {
                    contextValues.put("recipeId", arg);
                }
                if (!logContext.userId().isEmpty() && logContext.userId().equals(paramName)) {
                    contextValues.put("userId", arg);
                }
                if (!logContext.branchId().isEmpty() && logContext.branchId().equals(paramName)) {
                    contextValues.put("branchId", arg);
                }
                if (!logContext.commitId().isEmpty() && logContext.commitId().equals(paramName)) {
                    contextValues.put("commitId", arg);
                }
            }
            
            // Extract user ID from headers if enabled
            if (logContext.extractUserIdFromHeader()) {
                try {
                    // Use the method that throws exception if userId is required
                    Long userId = HeaderUtil.extractRequiredUserIdFromHeader();
                    contextValues.put("userId", userId);
                } catch (ValidationException e) {
                    // Log the error and re-throw to ensure the request fails
                    log.error("Required user ID not found in headers: {}", e.getMessage());
                    throw e;
                }
            }
            
            // Extract from path variables if enabled
            if (logContext.extractRecipeIdFromPath()) {
                Object recipeId = extractPathVariable(args, parameters, "recipeId");
                if (recipeId != null) {
                    contextValues.put("recipeId", recipeId);
                }
            }
            
            if (logContext.extractBranchIdFromPath()) {
                Object branchId = extractPathVariable(args, parameters, "branchId");
                if (branchId != null) {
                    contextValues.put("branchId", branchId);
                }
            }
            
            if (logContext.extractCommitIdFromPath()) {
                Object commitId = extractPathVariable(args, parameters, "commitId");
                if (commitId != null) {
                    contextValues.put("commitId", commitId);
                }
            }
            
            // Set the context
            setContextFromValues(contextValues);
            
        } catch (ValidationException e) {
            // Re-throw ValidationException to ensure it's handled by the global exception handler
            throw e;
        } catch (Exception e) {
            log.warn("Failed to set logging context", e);
        }
    }

    @AfterReturning("@annotation(com.recipefy.version.annotation.LogContext)")
    public void clearLoggingContext() {
        // Note: We don't clear MDC here as it's handled by RequestLoggingInterceptor
        // This is just for any additional cleanup if needed
    }

    private Object extractPathVariable(Object[] args, Parameter[] parameters, String pathVarName) {
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
            
            if (pathVariable != null) {
                String value = pathVariable.value();
                if (value.isEmpty()) {
                    value = parameter.getName();
                }
                
                if (pathVarName.equals(value)) {
                    return args[i];
                }
            }
        }
        return null;
    }

    private void setContextFromValues(Map<String, Object> contextValues) {
        Long recipeId = (Long) contextValues.get("recipeId");
        Object userId = contextValues.get("userId");
        Long branchId = (Long) contextValues.get("branchId");
        Long commitId = (Long) contextValues.get("commitId");
        
        // Convert userId to Long if it's a String
        Long userIdLong = null;
        if (userId instanceof Long) {
            userIdLong = (Long) userId;
        } else if (userId instanceof String) {
            try {
                userIdLong = Long.parseLong((String) userId);
            } catch (NumberFormatException e) {
                log.warn("Could not parse userId as Long: {}", userId);
            }
        }
        
        // Set context based on available values
        if (recipeId != null && userIdLong != null) {
            if (branchId != null && commitId != null) {
                LoggingUtil.setCommitContext(commitId, branchId, recipeId, userIdLong);
            } else if (branchId != null) {
                LoggingUtil.setBranchContext(branchId, recipeId, userIdLong);
            } else {
                LoggingUtil.setRecipeContext(recipeId, userIdLong);
            }
        } else {
            // Set individual values if we don't have complete context
            if (recipeId != null) {
                LoggingUtil.setRecipeId(recipeId);
            }
            if (userIdLong != null) {
                LoggingUtil.setUserId(userIdLong);
            }
            if (branchId != null) {
                LoggingUtil.setBranchId(branchId);
            }
            if (commitId != null) {
                LoggingUtil.setCommitId(commitId);
            }
        }
    }
}
