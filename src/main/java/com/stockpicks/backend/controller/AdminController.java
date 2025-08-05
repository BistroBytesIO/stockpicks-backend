package com.stockpicks.backend.controller;

import com.stockpicks.backend.dto.admin.AdminLoginRequest;
import com.stockpicks.backend.dto.admin.AdminResponse;
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AdminLoginRequest request) {
        try {
            Admin admin = adminService.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Invalid email or password"));

            if (!adminService.validatePassword(admin, request.getPassword())) {
                return ResponseEntity.badRequest().body("Invalid email or password");
            }

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

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid email or password");
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<?>> getAllUsers(Authentication authentication) {
        // Verify admin authentication
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }

        try {
            List<?> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/users/subscribers")
    public ResponseEntity<List<?>> getSubscribedUsers(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }

        try {
            List<?> subscribers = userService.getSubscribedUsers();
            return ResponseEntity.ok(subscribers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/users/non-subscribers")
    public ResponseEntity<List<?>> getNonSubscribedUsers(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }

        try {
            List<?> nonSubscribers = userService.getNonSubscribedUsers();
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
        // This should check if the user has admin role
        // For now, we'll implement basic check
        return authentication != null && authentication.isAuthenticated();
    }
}