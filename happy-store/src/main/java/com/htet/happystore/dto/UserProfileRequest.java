package com.htet.happystore.dto;

import lombok.Data;
import com.htet.happystore.entity.User;

@Data
public class UserProfileRequest {
    private String fullName;
    private String phone;
    private String address;
    private User.Country country;
}

