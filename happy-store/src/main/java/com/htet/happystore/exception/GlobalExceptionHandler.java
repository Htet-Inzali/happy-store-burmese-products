package com.htet.happystore.exception;

import com.htet.happystore.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
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

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1️⃣ Validation Errors (@Valid fail)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation Failed", errors, request);

    }

    // 2️⃣ Bad Request (Business Logic Errors)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleBadRequest(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        ex.printStackTrace();
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                List.of(ex.getMessage()), request);
    }

    // 3️⃣ Stock Out / Business State Error
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request) {

        ex.printStackTrace();
        return buildErrorResponse(HttpStatus.CONFLICT, "Business Rule Violated",
                List.of(ex.getMessage()), request);
    }

    // 4️⃣ Access Denied
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        ex.printStackTrace();
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access Denied",
                List.of("ဤလုပ်ဆောင်ချက်ကို ခွင့်ပြုချက်မရှိပါ"), request);
    }

    // 5️⃣ File Size Exceeded
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleMaxSizeException(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {

        ex.printStackTrace();
        return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, "File Too Large",
                List.of("ဖိုင်အရွယ်အစား 5MB ထက် မကျော်ရပါ"), request);
    }

    // 6️⃣ IO Exception
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleIOException(
            IOException ex,
            HttpServletRequest request) {

        ex.printStackTrace();
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "File IO Error",
                List.of("ဖိုင် သိမ်းဆည်းရာတွင် အခက်အခဲရှိပါသည်"), request);
    }

    // 7️⃣ Runtime Exceptions
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {

        ex.printStackTrace(); // Console တွင် အနီရောင်ပြရန်
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                List.of(ex.getMessage() != null ? ex.getMessage() : "Unexpected system error"), request);
    }

    // 8️⃣ Fallback Exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<List<String>>> handleGeneralException(
            Exception ex,
            HttpServletRequest request) {

        ex.printStackTrace();
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error",
                List.of("Something went wrong"), request);
    }

    // 9️⃣ URL Not Found
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleNotFound(
            org.springframework.web.servlet.resource.NoResourceFoundException ex,
            HttpServletRequest request) {

        return buildErrorResponse(HttpStatus.NOT_FOUND, "URL ရှာမတွေ့ပါ",
                List.of("သင်ခေါ်ဆိုသော လမ်းကြောင်း မှားယွင်းနေပါသည်"), request);
    }

    // 🔧 Unified Common Builder (Next.js Friendly)
    private ResponseEntity<ApiResponse<List<String>>> buildErrorResponse(
            HttpStatus status,
            String message,
            List<String> errors,
            HttpServletRequest request) {

        ApiResponse<List<String>> response = ApiResponse.<List<String>>builder()
                .success(false) // Error ဖြစ်၍ false ထားခြင်း
                .message(message)
                .data(errors) // Error အသေးစိတ်များကို data ထဲတွင် ထည့်ပို့ခြင်း
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, status);
    }
}