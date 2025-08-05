package com.stockpicks.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private boolean isActive;
    private String role;
    private LocalDateTime createdAt;
    private String token;

    // Constructor without token for listing admins
    public AdminResponse(Long id, String email, String firstName, String lastName, boolean isActive, String role, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.isActive = isActive;
        this.role = role;
        this.createdAt = createdAt;
    }
}