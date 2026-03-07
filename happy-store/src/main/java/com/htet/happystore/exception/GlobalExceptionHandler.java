package com.htet.happystore.exception;

import com.htet.happystore.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation Failed", errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleBadRequest(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.error("Bad Request error at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", List.of(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        log.warn("Business logic state conflict at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Business Rule Violated", List.of(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access Denied at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access Denied", List.of("ဤလုပ်ဆောင်ချက်ကို ခွင့်ပြုချက်မရှိပါ"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleMaxSizeException(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.error("File size exceeded at {}", request.getRequestURI());
        return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, "File Too Large", List.of("ဖိုင်အရွယ်အစား 5MB ထက် မကျော်ရပါ"));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleIOException(
            IOException ex, HttpServletRequest request) {
        log.error("IO Exception at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "File IO Error", List.of("ဖိုင် သိမ်းဆည်းရာတွင် အခက်အခဲရှိပါသည်"));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleNotFound(
            org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", request.getRequestURI());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "URL ရှာမတွေ့ပါ", List.of("သင်ခေါ်ဆိုသော လမ်းကြောင်း မှားယွင်းနေပါသည်"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<List<String>>> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}: ", request.getRequestURI(), ex);
        String message = ex.getMessage() != null ? ex.getMessage() : "Unexpected system error";
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", List.of(message));
    }

    // 🔧 Unified Common Builder (Next.js Friendly)
    private ResponseEntity<ApiResponse<List<String>>> buildErrorResponse(
            HttpStatus status, String message, List<String> errors) {

        ApiResponse<List<String>> response = ApiResponse.<List<String>>builder()
                .success(false)
                .message(message)
                .data(errors)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, status);
    }
}