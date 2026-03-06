package com.htet.happystore.dto;

import com.htet.happystore.entity.User;
import lombok.Data;

public class UserDTO {

    @Data
    public static class ProfileRequest {
        private String fullName;
        private String phone;
        private String address;
        private User.Country country;
    }
}