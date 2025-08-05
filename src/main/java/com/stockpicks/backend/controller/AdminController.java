package com.stockpicks.backend.controller;

import com.stockpicks.backend.dto.admin.AdminLoginRequest;
import com.stockpicks.backend.dto.admin.AdminRegisterRequest;
import com.stockpicks.backend.dto.admin.AdminResponse;
import com.stockpicks.backend.dto.user.UserResponse;
import com.stockpicks.backend.entity.Admin;
import com.stockpicks.backend.security.JwtUtil;
import com.stockpicks.backend.service.AdminService;
import com.stockpicks.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AdminRegisterRequest request) {
        try {
            Admin admin = adminService.createAdmin(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName()
            );

            AdminResponse response = new AdminResponse(
                admin.getId(),
                admin.getEmail(),
                admin.getFirstName(),
                admin.getLastName(),
                admin.isActive(),
                admin.getRole(),
                admin.getCreatedAt()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating admin: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AdminLoginRequest request) {
        try {
            System.out.println("Admin login attempt for email: " + request.getEmail());
            
            Admin admin = adminService.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Admin not found for email: " + request.getEmail()));

            System.out.println("Found admin: " + admin.getEmail() + ", active: " + admin.isActive());

            if (!adminService.validatePassword(admin, request.getPassword())) {
                System.out.println("Password validation failed for admin: " + request.getEmail());
                return ResponseEntity.badRequest().body("Invalid email or password");
            }

            System.out.println("Password validation successful for admin: " + request.getEmail());

            String token = jwtUtil.generateToken(admin.getEmail(), "ADMIN");

            AdminResponse response = new AdminResponse(
                admin.getId(),
                admin.getEmail(),
                admin.getFirstName(),
                admin.getLastName(),
                admin.isActive(),
                admin.getRole(),
                admin.getCreatedAt(),
                token
            );

            System.out.println("Admin login successful for: " + request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Admin login error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Invalid email or password");
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers(Authentication authentication) {
        // Verify admin authentication
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }

        try {
            List<UserResponse> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/users/subscribers")
    public ResponseEntity<List<UserResponse>> getSubscribedUsers(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }

        try {
            List<UserResponse> subscribers = userService.getSubscribedUsers();
            return ResponseEntity.ok(subscribers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/users/non-subscribers")
    public ResponseEntity<List<UserResponse>> getNonSubscribedUsers(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }

        try {
            List<UserResponse> nonSubscribers = userService.getNonSubscribedUsers();
            return ResponseEntity.ok(nonSubscribers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getAdminStats(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }

        try {
            // This will be implemented with actual stats
            return ResponseEntity.ok("Admin stats endpoint");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching stats");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // For now, we'll check if the authenticated user exists in admin table
        try {
            String email = authentication.getName();
            return adminService.findByEmail(email).isPresent();
        } catch (Exception e) {
            return false;
        }
    }
}