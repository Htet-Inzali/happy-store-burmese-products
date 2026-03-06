package com.htet.happystore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Email(message = "Invalid email format")
    private String email;    // optional — email or phone တစ်ခုတော့ ရှိရမယ်

    private String phone;    // optional

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String address;

    @NotBlank(message = "Country is required")
    @Pattern(regexp = "MYANMAR|VIETNAM", message = "Country must be MYANMAR or VIETNAM")
    private String country;
}