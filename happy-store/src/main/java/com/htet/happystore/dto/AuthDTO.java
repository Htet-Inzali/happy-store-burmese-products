package com.htet.happystore.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Getter;

public class AuthDTO {

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Credential is required")
        private String credential; // Email or Phone

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Full name is required")
        private String fullName;

        @Email(message = "Invalid email format")
        private String email;

        private String phone;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        private String address;

        @NotBlank(message = "Country is required")
        @Pattern(regexp = "(?i)MYANMAR|VIETNAM|SINGAPORE", message = "Country must be MYANMAR, VIETNAM or SINGAPORE")
        private String country;
    }

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "Token is required")
        private String token;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String newPassword;
    }

    @Getter
    public static class Response {
        private final String token;
        private final String role;

        public Response(String token, String role) {
            this.token = token;
            this.role = role;
        }
    }
}