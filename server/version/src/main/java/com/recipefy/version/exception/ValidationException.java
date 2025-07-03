package com.recipefy.version.exception;

import com.recipefy.version.model.dto.ErrorDetailDTO;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ValidationException extends RuntimeException {

    private List<ErrorDetailDTO> errorDetailDTO = new ArrayList<>();

    public ValidationException() {
        super();
    }

    public ValidationException(List<ErrorDetailDTO> errorDetailDTO) {
        this.errorDetailDTO = errorDetailDTO;
    }

    public ValidationException(String msg) {
        super(msg);
    }

    public ValidationException(Throwable e) {
        super(e);
    }

    public ValidationException(String msg, Throwable e) {
        super(msg, e);
    }

}
