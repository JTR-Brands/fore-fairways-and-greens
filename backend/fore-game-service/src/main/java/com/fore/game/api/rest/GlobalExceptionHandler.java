package com.fore.game.api.rest;

import com.fore.game.application.dto.ErrorResponse;
import com.fore.game.domain.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGameNotFound(
            GameNotFoundException ex, HttpServletRequest request) {
        log.warn("Game not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(ex, request));
    }

    @ExceptionHandler(PlayerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePlayerNotFound(
            PlayerNotFoundException ex, HttpServletRequest request) {
        log.warn("Player not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(ex, request));
    }

    @ExceptionHandler({
            InvalidGameStateException.class,
            InvalidActionException.class,
            NotYourTurnException.class,
            InsufficientFundsException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(
            GameException ex, HttpServletRequest request) {
        log.warn("Bad request: {} - {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(ex, request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .toList();

        ErrorResponse response = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Request validation failed")
                .fieldErrors(fieldErrors)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.builder()
                        .code("ILLEGAL_STATE")
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .code("INTERNAL_ERROR")
                        .message("An unexpected error occurred")
                        .timestamp(Instant.now())
                        .path(request.getRequestURI())
                        .build());
    }

    private ErrorResponse buildErrorResponse(GameException ex, HttpServletRequest request) {
        return ErrorResponse.builder()
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
    }

    private ErrorResponse.FieldError mapFieldError(FieldError fieldError) {
        return ErrorResponse.FieldError.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .rejectedValue(fieldError.getRejectedValue())
                .build();
    }
}
