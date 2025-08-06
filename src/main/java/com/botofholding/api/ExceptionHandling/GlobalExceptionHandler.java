package com.botofholding.api.ExceptionHandling;

import com.botofholding.api.Domain.DTO.Response.StandardApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Helper method to format the stack trace as a String
    private String formatStackTrace(Throwable ex) {
        StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    // The return type now uses StandardApiResponse<Object>
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        logger.warn("Resource not found: {}. Request: {}", ex.getMessage(), request.getDescription(false));

        // Construct the standard error response
        StandardApiResponse<Object> errorResponse = new StandardApiResponse<>(
                false, // Success is false
                ex.getMessage(), // The error message
                null // Data is null for errors
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<StandardApiResponse<Object>> handleDuplicateResourceException(DuplicateResourceException ex, WebRequest request) {
        logger.warn("Duplicate resource: {}. Request: {}", ex.getMessage(), request.getDescription(false));

        StandardApiResponse<Object> errorResponse = new StandardApiResponse<>(
                false,
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<StandardApiResponse<Object>> handleUserNotFoundException(UserNotFoundException ex, WebRequest request) {
        logger.warn("User not found: {}. Request: {}", ex.getMessage(), request.getDescription(false));

        StandardApiResponse<Object> errorResponse = new StandardApiResponse<>(
                false,
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<StandardApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        List<String> validationErrors = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toList());

        logger.warn("Validation error. Request: {}. Violations: {}", request.getDescription(false), validationErrors);

        String errorMessage = "Validation failed: " + String.join(", ", validationErrors);
        StandardApiResponse<Object> errorResponse = new StandardApiResponse<>(
                false,
                errorMessage,
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidSearchScopeException.class)
    public ResponseEntity<StandardApiResponse<Object>> handleInvalidSearchScopeException(InvalidSearchScopeException ex, WebRequest request) {
        logger.warn("Invalid search scope: {}. Request: {}", ex.getMessage(), request.getDescription(false));
        StandardApiResponse<Object> errorResponse = new StandardApiResponse<>(
                false,
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OwnerNotFoundException.class)
    public ResponseEntity<StandardApiResponse<Object>> handleOwnerNotFoundException(OwnerNotFoundException ex, WebRequest request) {
        // This is a critical state inconsistency. Log it as an error.
        logger.error("Critical state inconsistency: An owner principal existed in the security context but could not be found in the database. Message: {}. Request: {}", ex.getMessage(), request.getDescription(false));

        // Provide a generic, safe error message to the client.
        String userFacingMessage = "A server error occurred while verifying the request's owner. Please try again later.";
        StandardApiResponse<Object> errorResponse = new StandardApiResponse<>(
                false,
                userFacingMessage,
                null
        );
        // Return an INTERNAL_SERVER_ERROR because this is a server state issue, not a client error.
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<StandardApiResponse<Object>> handleItemNotFoundException(ItemNotFoundException ex, WebRequest request) {
        logger.warn("Item not found: {}. Request: {}", ex.getMessage(), request.getDescription(false));
        StandardApiResponse<Object> errorResponse = new StandardApiResponse<>(
                false,
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<StandardApiResponse<Object>> handleUnsupportedOperationException(UnsupportedOperationException ex, WebRequest request) {
        logger.warn("Unsupported operation: {}. Request: {}", ex.getMessage(), request.getDescription(false));
        StandardApiResponse<Object> errorResponse = new StandardApiResponse<>(
                false,
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardApiResponse<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        logger.warn("Method argument validation failed. Request: {}. Errors: {}", request.getDescription(false), errors);

        String errorMessage = "Validation failed: " + String.join(", ", errors);
        StandardApiResponse<Object> errorResponse = new StandardApiResponse<>(
                false,
                errorMessage,
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataMismatchException.class)
    public ResponseEntity<StandardApiResponse<Object>> handleDataMismatchException(DataMismatchException ex, WebRequest request) {
        logger.warn("Data mismatch exception: {}. Request {}", ex.getMessage(), request.getDescription(false));

        StandardApiResponse<Object> errorResponse = new StandardApiResponse<>(
                false,
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<StandardApiResponse<Object>> handleValidationException(ValidationException ex, WebRequest request) {
        logger.warn("Validation exception: {}. Request: {}", ex.getMessage(), request.getDescription(false));
        StandardApiResponse<Object> errorResponse = new StandardApiResponse<>(
                false,
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AmbiguousResourceException.class)
    public ResponseEntity<StandardApiResponse<Object>> handleAmbiguousResourceException(AmbiguousResourceException ex, WebRequest request) {
        logger.warn("Ambiguous resource: {}. Request: {}", ex.getMessage(), request.getDescription(false));
        StandardApiResponse<Object> errorResponse = new StandardApiResponse<>(
                false,
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }


    @ExceptionHandler(ContainerNotFoundException.class)
    public ResponseEntity<StandardApiResponse<Object>> handleContainerNotFoundException(ContainerNotFoundException ex, WebRequest request) {
        logger.warn("Container not found: {}. Request: {}", ex.getMessage(), request.getDescription(false));
        StandardApiResponse<Object> errorResponse = new StandardApiResponse<>(
                false,
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // A final catch-all for any other unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardApiResponse<Object>> handleAllUncaughtException(Exception ex, WebRequest request) {
        logger.error("An unexpected error occurred. Request: {}. Stack trace: {}",
                request.getDescription(false),
                formatStackTrace(ex));

        StandardApiResponse<Object> errorResponse = new StandardApiResponse<>(
                false,
                "An unexpected internal server error occurred. Please contact support.",
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}