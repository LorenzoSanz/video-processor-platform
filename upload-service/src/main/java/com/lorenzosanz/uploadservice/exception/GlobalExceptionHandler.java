package com.lorenzosanz.uploadservice.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.lorenzosanz.uploadservice.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(exception = MethodArgumentNotValidException.class)
  public ErrorResponse badRequestException(MethodArgumentNotValidException ex) {
    return new ErrorResponse(400, "Invalid Request", LocalDateTime.now());
    ;
  }
}
