// ─────────────────────────────────────────────────────────────
// GlobalExceptionHandler.java
// ─────────────────────────────────────────────────────────────
package com.redis.exception;

import com.redis.model.dto.response.ApiResponse;          // FIX #1: correct package
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ═══════════════════════════════════════════════════════════════════════════
    //  PRODUCT EXCEPTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductNotFound(
            ProductNotFoundException ex) {
        log.warn("Product not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "PRODUCT_NOT_FOUND"));
    }

    @ExceptionHandler(ProductDuplicateException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductDuplicate(
            ProductDuplicateException ex) {
        log.warn("Duplicate product: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)                 // 409
                .body(ApiResponse.error(ex.getMessage(), "PRODUCT_ALREADY_EXISTS"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  VALIDATION EXCEPTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * FIX #2: getAllErrors() → getFieldErrors() — safe, no ClassCastException
     * FIX #3 & #4: ApiResponse.validationError() factory + ValidationError class use kiya
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        List<ApiResponse.ValidationError> errors = ex.getBindingResult()
                .getFieldErrors()                            // FIX #2: only FieldErrors
                .stream()
                .map(this::buildValidationError)
                .collect(Collectors.toList());

        log.warn("Validation failed — {} error(s): {}", errors.size(), errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.validationError("Validation failed", errors)); // FIX #3
    }

    /**
     * FIX #5: Malformed JSON body — e.g. missing quotes, wrong types
     * Bina is handler ke 500 aata tha
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        "Malformed JSON — please check request body",
                        "INVALID_JSON"
                ));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  HTTP EXCEPTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * FIX #6: Wrong HTTP method — e.g. GET on POST endpoint
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)       // 405
                .body(ApiResponse.error(
                        ex.getMethod() + " method is not supported for this endpoint",
                        "METHOD_NOT_ALLOWED"
                ));
    }

    /**
     * FIX #7: Wrong URL — e.g. /api/productz instead of /api/products
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
            NoResourceFoundException ex) {
        log.warn("Endpoint not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                        "Requested endpoint does not exist",
                        "ENDPOINT_NOT_FOUND"
                ));
    }

    /**
     * IllegalArgumentException — Service layer se aata hai
     * e.g. minPrice > maxPrice
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "INVALID_ARGUMENT"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  FALLBACK
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Koi bhi unexpected exception — actual message client ko mat do
     * Sirf log karo, generic message return karo
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex); // full stack trace log
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "An unexpected error occurred. Please try again later.",
                        "INTERNAL_SERVER_ERROR"
                ));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Private Helper
    // ═══════════════════════════════════════════════════════════════════════════

    private ApiResponse.ValidationError buildValidationError(FieldError err) {
        return ApiResponse.ValidationError.builder()
                .field(err.getField())
                .rejectedValue(err.getRejectedValue())
                .message(err.getDefaultMessage())
                .build();
    }
}