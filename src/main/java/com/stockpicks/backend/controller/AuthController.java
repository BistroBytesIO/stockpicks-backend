package com.stockpicks.backend.controller;

import com.stockpicks.backend.dto.ErrorResponse;
import com.stockpicks.backend.dto.auth.AuthResponse;
import com.stockpicks.backend.dto.auth.LoginRequest;
import com.stockpicks.backend.dto.auth.RegisterRequest;
import com.stockpicks.backend.entity.User;
import com.stockpicks.backend.exception.UserAlreadyExistsException;
import com.stockpicks.backend.security.JwtUtil;
import com.stockpicks.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.createUser(request);
            String token = jwtUtil.generateToken(user.getEmail());
            return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getFirstName()));
        } catch (UserAlreadyExistsException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("User already exists"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            User user = userService.findByEmail(request.getEmail());
            String token = jwtUtil.generateToken(user.getEmail());
            return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getFirstName()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid credentials"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Login failed: " + e.getMessage()));
        }
    }
}