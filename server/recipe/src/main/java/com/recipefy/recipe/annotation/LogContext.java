package com.recipefy.recipe.annotation;

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
     * Whether to extract user ID from headers (for all requests)
     */
    boolean extractUserIdFromHeader() default false;
    
    /**
     * Whether to extract recipe ID from path variable
     */
    boolean extractRecipeIdFromPath() default false;
} 