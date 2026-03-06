package com.htet.happystore.repository;

import com.htet.happystore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    // findByUsername ဖြုတ်ပြီ — User entity မှာ username field မရှိဘူး
}