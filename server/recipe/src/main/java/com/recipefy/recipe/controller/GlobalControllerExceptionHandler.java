package com.recipefy.recipe.controller;

import com.recipefy.recipe.exception.UnauthorizedException;
import com.recipefy.recipe.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalControllerExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("exception", "MethodArgumentNotValidException");
        errorResponse.put("message", "Validation failed");
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        
        Map<String, String> fieldErrors = new HashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> 
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        errorResponse.put("fieldErrors", fieldErrors);

        log.warn("HTTP_STATUS: {} , Field validation failed. Caused By: {}", HttpStatus.BAD_REQUEST, errorResponse);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException validationException) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("exception", "ValidationException");
        errorResponse.put("message", validationException.getMessage());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());

        log.warn("HTTP_STATUS: {} , ValidationException Caused By: {}", HttpStatus.BAD_REQUEST, errorResponse);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedException(UnauthorizedException unauthorizedException) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("exception", "UnauthorizedException");
        errorResponse.put("message", unauthorizedException.getMessage());
        errorResponse.put("status", HttpStatus.FORBIDDEN.value());

        log.warn("HTTP_STATUS: {} , UnauthorizedException Caused By: {}", HttpStatus.FORBIDDEN, errorResponse);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception exception) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("exception", "GenericException");
        errorResponse.put("message", exception.getMessage());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        log.error("HTTP_STATUS: {} , Generic Exception Occurred. Ex: ", HttpStatus.INTERNAL_SERVER_ERROR, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleHttpRequestMethodNotSupportedException(Exception exception) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("exception", "HttpRequestMethodNotSupportedException");
        errorResponse.put("message", exception.getMessage());
        errorResponse.put("status", HttpStatus.METHOD_NOT_ALLOWED.value());
        
        log.error("HTTP_STATUS: {} , HttpRequestMethodNotSupportedException Exception Occurred. Ex: ", HttpStatus.METHOD_NOT_ALLOWED, exception);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestParameterException(Exception exception) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("exception", "MissingServletRequestParameterException");
        errorResponse.put("message", exception.getMessage());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        
        log.error("HTTP_STATUS: {} , MissingServletRequestParameterException Exception Occurred. Ex: ", HttpStatus.BAD_REQUEST, exception);
        return ResponseEntity.badRequest().body(errorResponse);
    }
} 