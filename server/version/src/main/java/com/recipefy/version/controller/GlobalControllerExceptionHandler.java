package com.recipefy.version.controller;

import com.recipefy.version.exception.BaseRecipefyException;
import com.recipefy.version.exception.BusinessException;
import com.recipefy.version.model.dto.ErrorDTO;
import com.recipefy.version.model.dto.ErrorDetailDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalControllerExceptionHandler {
    private static final String ERROR_CODE = ".errorCode";
    private static final Locale EN = Locale.of("en");

    private final MessageSource messageSource;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setException("MethodArgumentNotValidException");

        exception.getBindingResult().getFieldErrors().forEach(i -> {
            ErrorDetailDTO fieldError = new ErrorDetailDTO();
            fieldError.setMessage(exception.getMessage());
            fieldError.setKey(i.getField());
            errorDTO.addError(fieldError);
        });

        log.warn("HTTP_STATUS: {} , Field validation failed. Caused By: {}", HttpStatus.BAD_REQUEST, errorDTO);
        return ResponseEntity.badRequest().body(errorDTO);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorDTO> handleBusinessException(BusinessException businessException) {
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setException("BusinessException");
        errorDTO.addError(getErrorDetailDTO(businessException));
        if (businessException.getHttpStatus().equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
            log.error("HTTP_STATUS: {} , BusinessException Caused By: {}", businessException.getHttpStatus(), errorDTO);
        } else {
            log.info("HTTP_STATUS: {} , BusinessException Caused By: {}", businessException.getHttpStatus(), errorDTO);
        }
        return ResponseEntity.status(businessException.getHttpStatus()).body(errorDTO);
    }

    @ExceptionHandler(BaseRecipefyException.class)
    public ResponseEntity<ErrorDTO> handleProductDetailBusinessException(BaseRecipefyException baseRecipefyException) {
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setException("BaseRecipefyException");
        errorDTO.addError(getErrorDetailDTO(baseRecipefyException));
        log.error("HTTP_STATUS: {} ,BaseRecipefyException Caused By: {}", HttpStatus.BAD_REQUEST, errorDTO);
        return ResponseEntity.badRequest().body(errorDTO);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> handleGenericException(Exception exception) {
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setException("GenericException");
        ErrorDetailDTO errorDetailDTO = new ErrorDetailDTO();
        errorDetailDTO.setMessage(exception.getMessage());
        errorDetailDTO.setKey("vcs.api.generic.error");
        errorDTO.addError(errorDetailDTO);
        log.error("HTTP_STATUS: {} , Generic Exception Occurred. Ex: ", HttpStatus.INTERNAL_SERVER_ERROR, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDTO);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorDTO> handleHttpRequestMethodNotSupportedException(Exception exception) {
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setException("HttpRequestMethodNotSupportedException");
        ErrorDetailDTO errorDetailDTO = new ErrorDetailDTO();
        errorDetailDTO.setMessage(exception.getMessage());
        errorDetailDTO.setKey("vcs.api.methodnotallowed.error");
        errorDTO.addError(errorDetailDTO);
        log.error("HTTP_STATUS: {} , HttpRequestMethodNotSupportedException Exception Occurred. Ex: ", HttpStatus.METHOD_NOT_ALLOWED, exception);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorDTO);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorDTO> handleMissingServletRequestParameterException(Exception exception) {
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setException("MissingServletRequestParameterException");
        ErrorDetailDTO errorDetailDTO = new ErrorDetailDTO();
        errorDetailDTO.setMessage(exception.getMessage());
        errorDetailDTO.setKey("vcs.api.missingservletrequestparameterexception.error");
        errorDTO.addError(errorDetailDTO);
        log.error("HTTP_STATUS: {} , MissingServletRequestParameterException Exception Occurred. Ex: ", HttpStatus.BAD_REQUEST, exception);
        return ResponseEntity.badRequest().body(errorDTO);
    }

    private ErrorDetailDTO getErrorDetailDTO(BaseRecipefyException exception) {
        ErrorDetailDTO errorDetailDTO = new ErrorDetailDTO();
        errorDetailDTO.setKey(exception.getKey());
        errorDetailDTO.setMessage(getMessage(exception.getKey(), exception.getArgs(), exception.getMessage()));
        errorDetailDTO.setErrorCode(getErrorCode(exception.getKey()));
        errorDetailDTO.setArgs(exception.getArgs());
        return errorDetailDTO;
    }

    private String getMessage(String key, Object[] args, String defaultMessage) {
        return Optional.of(getMessage(() -> messageSource.getMessage(key, args, EN), key))
                .filter(StringUtils::isNotEmpty)
                .orElse(defaultMessage);
    }

    private String getMessage(Supplier<String> supplier, String defaultMessage) {
        String message = StringUtils.EMPTY;
        try {
            if (Objects.nonNull(supplier.get())) {
                message = supplier.get();
            } else {
                message = defaultMessage;
            }
        } catch (NoSuchMessageException messageException) {
            message = defaultMessage;
        } catch (Exception exception) {
            log.error("Message Localization error occurred. Ex: ", exception);
        }
        return message;
    }

    private String getErrorCode(String key) {
        return Optional.of(getMessage(() -> messageSource.getMessage(key + ERROR_CODE, new Object[] {}, EN), key))
                .filter(StringUtils::isNotBlank)
                .orElse("");
    }
}
