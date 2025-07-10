package com.ie_project.workflow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Globaler Exception Handler für REST API
 * Global Exception Handler for REST API
 *
 * @author IE Project Team
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Behandlung von Validierungsfehlern
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = Map.of(
                "error", "Validierungsfehler / Validation error",
                "fields", errors,
                "timestamp", LocalDateTime.now()
        );

        System.err.println("=== VALIDIERUNGSFEHLER ===");
        System.err.println("Errors: " + errors);
        System.err.println("==========================");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Behandlung von allgemeinen Fehlern
     * Handle general errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralError(Exception ex) {

        Map<String, Object> response = Map.of(
                "error", "Unerwarteter Fehler / Unexpected error",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now()
        );

        System.err.println("=== UNERWARTETER FEHLER ===");
        ex.printStackTrace();
        System.err.println("===========================");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
