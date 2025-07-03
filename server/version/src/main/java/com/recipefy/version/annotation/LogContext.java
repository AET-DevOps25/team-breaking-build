package com.recipefy.version.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to automatically set logging context based on method parameters
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogContext {
    
    /**
     * Parameter name that contains the recipe ID
     */
    String recipeId() default "";
    
    /**
     * Parameter name that contains the user ID
     */
    String userId() default "";
    
    /**
     * Parameter name that contains the branch ID
     */
    String branchId() default "";
    
    /**
     * Parameter name that contains the commit ID
     */
    String commitId() default "";
    
    /**
     * Whether to extract user ID from headers (for all requests)
     */
    boolean extractUserIdFromHeader() default false;
    
    /**
     * Whether to extract recipe ID from path variable
     */
    boolean extractRecipeIdFromPath() default false;
    
    /**
     * Whether to extract branch ID from path variable
     */
    boolean extractBranchIdFromPath() default false;
    
    /**
     * Whether to extract commit ID from path variable
     */
    boolean extractCommitIdFromPath() default false;
}
