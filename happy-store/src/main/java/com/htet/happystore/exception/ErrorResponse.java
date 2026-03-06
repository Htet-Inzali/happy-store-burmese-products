package com.htet.happystore.exception;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ErrorResponse {

    private final String message;
    private final int status;
    private final LocalDateTime timestamp;
    private final String path;
    private final List<String> errors;

    public ErrorResponse(int status, String message, String path, List<String> errors) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.message = message;
        this.path = path;
        this.errors = errors;
    }
}