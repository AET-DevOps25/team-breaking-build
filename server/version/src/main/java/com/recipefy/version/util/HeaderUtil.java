package com.recipefy.version.util;

import com.recipefy.version.exception.ValidationException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * Utility class for extracting values from HTTP headers
 */
public class HeaderUtil {

    // Common header names for user ID
    private static final String[] USER_ID_HEADERS = {
        "X-User-ID",
        "X-User-Id", 
        "User-ID",
        "User-Id"
    };

    /**
     * Extract user ID from HTTP headers
     * @return User ID as UUID, or null if not found
     */
    public static UUID extractUserIdFromHeader() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            
            HttpServletRequest request = attributes.getRequest();
            
            // Try to extract user ID from common header names
            for (String headerName : USER_ID_HEADERS) {
                String headerValue = request.getHeader(headerName);
                if (headerValue != null && !headerValue.trim().isEmpty()) {
                    try {
                        return UUID.fromString(headerValue.trim());
                    } catch (IllegalArgumentException e) {
                        // If not a valid UUID, return null
                        return null;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract user ID from HTTP headers - throws exception if not found
     * @return User ID as UUID
     * @throws ValidationException if user ID is not found in headers
     */
    public static UUID extractRequiredUserIdFromHeader() {
        UUID userId = extractUserIdFromHeader();
        if (userId == null) {
            throw new ValidationException("User ID is required but not found in request headers. Expected headers: " + 
                String.join(", ", USER_ID_HEADERS));
        }
        return userId;
    }
}
