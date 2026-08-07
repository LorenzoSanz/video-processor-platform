package com.lorenzosanz.uploadservice.exception;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.lorenzosanz.uploadservice.dto.error.ErrorResponse;
import com.lorenzosanz.uploadservice.dto.error.ValidationError;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(exception = Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorResponse handleUnexpectedException(Exception ex, HttpServletRequest request) {
    logger.error("Unexpected error occurred", ex);
    return new ErrorResponse(
      HttpStatus.INTERNAL_SERVER_ERROR.value(),
      "An unexpected error occurred",
      LocalDateTime.now(),
      request.getRequestURI()
    );
  }

  @ExceptionHandler(exception = StorageException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorResponse handleStorageException(StorageException ex, HttpServletRequest request) {
    logger.error("Storage operation failed", ex);
    return new ErrorResponse(
      HttpStatus.INTERNAL_SERVER_ERROR.value(), 
      "Storage operation failed.", 
      LocalDateTime.now(), 
      request.getRequestURI()
    );
  }

  @ExceptionHandler(exception = FileValidationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleValidationException(FileValidationException ex, HttpServletRequest request) {
    logger.warn("File validation failed", ex);
    return new ErrorResponse(
      HttpStatus.BAD_REQUEST.value(),
      ex.getMessage(),
      LocalDateTime.now(), 
      request.getRequestURI()
    );
  }

  @ExceptionHandler(exception = MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) { 
    logger.warn("Request validation failed", ex);
    
    List<ObjectError> springErrors = ex.getBindingResult().getAllErrors();
    List<ValidationError> errors = new ArrayList<>();

    for (ObjectError springError : springErrors) {
      if (springError instanceof FieldError) {
        FieldError fieldError = (FieldError) springError;
        ValidationError validationError = new ValidationError(
          fieldError.getField(),
          fieldError.getDefaultMessage()
        );
        errors.add(validationError);
      }
    }

    return new ErrorResponse(
      HttpStatus.BAD_REQUEST.value(),
      "Validation failed",
      LocalDateTime.now(), 
      request.getRequestURI(),
      errors
    );
  }
}
