package com.htet.happystore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Credential is required")
    private String credential; // Email or Phone

    @NotBlank(message = "Password is required")
    private String password;
}