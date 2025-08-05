package com.stockpicks.backend.service;

import com.stockpicks.backend.entity.Admin;
import com.stockpicks.backend.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Admin createAdmin(String email, String password, String firstName, String lastName) {
        if (adminRepository.existsByEmail(email)) {
            throw new RuntimeException("Admin with this email already exists");
        }

        String hashedPassword = passwordEncoder.encode(password);
        Admin admin = new Admin(email, hashedPassword, firstName, lastName);
        return adminRepository.save(admin);
    }

    public Optional<Admin> findByEmail(String email) {
        return adminRepository.findByEmailAndIsActiveTrue(email);
    }

    public boolean validatePassword(Admin admin, String password) {
        return passwordEncoder.matches(password, admin.getPasswordHash());
    }

    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    public Optional<Admin> findById(Long id) {
        return adminRepository.findById(id);
    }

    public Admin updateAdmin(Long id, String firstName, String lastName, boolean isActive) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        admin.setFirstName(firstName);
        admin.setLastName(lastName);
        admin.setActive(isActive);
        
        return adminRepository.save(admin);
    }

    public void deleteAdmin(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        admin.setActive(false);
        adminRepository.save(admin);
    }

    public boolean existsByEmail(String email) {
        return adminRepository.existsByEmail(email);
    }
}