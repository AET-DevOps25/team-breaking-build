package com.recipefy.version.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.ArrayUtils;

@ToString
@Getter
@Setter
public class ErrorDetailDTO {

    private String key;
    private String message;
    private String errorCode;
    private String[] args;

    public ErrorDetailDTO() {
        this.args = ArrayUtils.EMPTY_STRING_ARRAY;
    }
}
