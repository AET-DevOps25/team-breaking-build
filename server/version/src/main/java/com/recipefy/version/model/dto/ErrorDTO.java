package com.recipefy.version.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class ErrorDTO {
    private long timestamp = System.currentTimeMillis();
    private String exception;
    private List<ErrorDetailDTO> errors = new ArrayList<>();

    public void addError(ErrorDetailDTO errorDetailDTO) {
        this.errors.add(errorDetailDTO);
    }
}
