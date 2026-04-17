// ─────────────────────────────────────────────────────────────
// ApiResponse.java — Generic wrapper for all API responses
// ─────────────────────────────────────────────────────────────
package com.redis.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter                                          // FIX #1: Sirf Getter — immutable response
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)       // null fields JSON mein nahi aayenge
public class ApiResponse<T> {

    // ─── Status ────────────────────────────────────────────────────────────────
    private final boolean success;

    // ─── Message ───────────────────────────────────────────────────────────────
    private final String message;

    // ─── FIX #6: Machine-readable error code — client switch-case kar sake ────
    // Success pe null rahega (@JsonInclude handle karega)
    private final String errorCode;

    // ─── Payload ───────────────────────────────────────────────────────────────
    private final T data;

    // ─── FIX #4: Validation errors list — @Valid fail hone pe field-wise detail
    private final List<ValidationError> errors;

    // ─── Meta ──────────────────────────────────────────────────────────────────
    private final Boolean fromCache;             // FIX #7: NON_NULL handle karega

    // ─── FIX #2 & #3: LocalDateTime + @JsonFormat — consistent format ──────────
    @JsonFormat(pattern = "YYYY-MM-DD HH:MM:SS")
    private final LocalDateTime timestamp;

    // ═══════════════════════════════════════════════════════════════════════════
    //  SUCCESS FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Simple success — data ke saath */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /** Cache-aware success — fromCache flag ke saath */
    public static <T> ApiResponse<T> success(String message, T data, boolean fromCache) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .fromCache(fromCache)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /** No data success — delete / update confirmations ke liye */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ERROR FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Simple error — sirf message */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /** FIX #6: Error with errorCode — client programmatically handle kar sake */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /** FIX #4: Validation error — field-wise errors list ke saath */
    public static <T> ApiResponse<T> validationError(
            String message,
            List<ValidationError> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode("VALIDATION_FAILED")
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  NESTED — Validation Error Detail
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * FIX #4: Har field ki error detail alag — GlobalExceptionHandler mein
     *         MethodArgumentNotValidException se build karo
     *
     * JSON output:
     * {
     *   "field": "price",
     *   "rejectedValue": "-5",
     *   "message": "Price must be greater than 0"
     * }
     */
    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationError {
        private final String field;
        private final Object rejectedValue;
        private final String message;
    }
}