package com.recipefy.version.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends BaseRecipefyException {
    private final HttpStatus httpStatus;

    public BusinessException(String key, HttpStatus httpStatus) {
        super(key);
        this.httpStatus = httpStatus;
    }

    public BusinessException(String key, HttpStatus httpStatus, String... args) {
        super(key, args);
        this.httpStatus = httpStatus;
    }

    public BusinessException(String message, Throwable cause, String key, String[] args, HttpStatus httpStatus) {
        super(message, cause, key, args);
        this.httpStatus = httpStatus;
    }
}
