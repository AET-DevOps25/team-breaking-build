package com.recipefy.version.config;

import com.recipefy.version.util.LoggingUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to extract request ID from headers and set it in MDC for logging correlation
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    
    // Common header names for request ID
    private static final String[] REQUEST_ID_HEADERS = {
        "X-Request-ID",
        "X-Correlation-ID", 
        "X-Trace-ID",
        "Request-ID",
        "Correlation-ID"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = extractRequestId(request);
        
        if (requestId != null) {
            LoggingUtil.setRequestId(requestId);
            logger.debug("Request ID extracted from header: {}", requestId);
        } else {
            logger.debug("No request ID found in headers, using generated ID");
            // Fallback to generated ID if not provided in headers
            String generatedId = LoggingUtil.generateRequestId();
            LoggingUtil.setRequestId(generatedId);
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Clear MDC after request completion
        LoggingUtil.clearMDC();
    }

    /**
     * Extract request ID from common header names
     */
    private String extractRequestId(HttpServletRequest request) {
        for (String headerName : REQUEST_ID_HEADERS) {
            String requestId = request.getHeader(headerName);
            if (requestId != null && !requestId.trim().isEmpty()) {
                return requestId.trim();
            }
        }
        return null;
    }
}
