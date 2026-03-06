package com.htet.happystore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    public enum Country {
        MYANMAR,
        VIETNAM,
        SINGAPORE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

    @Column(nullable = false)
    private String password;

    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Country country; // MYANMAR or VIETNAM

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // USER or ADMIN

    @OneToMany(mappedBy = "user")
    private List<Order> orders;

    private String profileImageUrl;

    // ==============================
    // Data Integrity Validation
    // ==============================

    @PrePersist
    @PreUpdate
    private void validateContact() {
        if (email == null && phone == null) {
            throw new IllegalArgumentException(
                    "Either email or phone must be provided");
        }
    }
}