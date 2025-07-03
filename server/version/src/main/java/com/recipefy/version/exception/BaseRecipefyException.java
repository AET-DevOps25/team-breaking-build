package com.recipefy.version.exception;

import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

@Getter
public class BaseRecipefyException extends RuntimeException {
    private final String key;
    private final String[] args;

    public BaseRecipefyException(String key) {
        this.key = key;
        this.args = ArrayUtils.EMPTY_STRING_ARRAY;
    }

    public BaseRecipefyException(String key, String... args) {
        this.key = key;
        this.args = args;
    }

    public BaseRecipefyException(String message, Throwable cause, String key, String[] args) {
        super(message, cause);
        this.key = key;
        this.args = args;
    }

    @Override
    public String getMessage() {
        return "Business exception occurred " + this.key + " " + StringUtils.join(this.args);
    }
}
