package com.stockpicks.backend.service;

import com.stockpicks.backend.dto.auth.RegisterRequest;
import com.stockpicks.backend.dto.user.UserResponse;
import com.stockpicks.backend.entity.User;
import com.stockpicks.backend.entity.UserSubscription;
import com.stockpicks.backend.enums.SubscriptionStatus;
import com.stockpicks.backend.exception.UserAlreadyExistsException;
import com.stockpicks.backend.repository.UserRepository;
import com.stockpicks.backend.repository.UserSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User createUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setIsActive(true);

        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getSubscribedUsers() {
        List<Long> subscribedUserIds = userSubscriptionRepository
                .findAll()
                .stream()
                .filter(sub -> sub.getStatus() == SubscriptionStatus.ACTIVE)
                .map(UserSubscription::getUserId)
                .distinct()
                .collect(Collectors.toList());

        return userRepository.findAllById(subscribedUserIds).stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getNonSubscribedUsers() {
        List<Long> subscribedUserIds = userSubscriptionRepository
                .findAll()
                .stream()
                .filter(sub -> sub.getStatus() == SubscriptionStatus.ACTIVE)
                .map(UserSubscription::getUserId)
                .distinct()
                .collect(Collectors.toList());

        return userRepository.findAll().stream()
                .filter(user -> !subscribedUserIds.contains(user.getId()))
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public long getTotalUsersCount() {
        return userRepository.count();
    }

    public long getActiveSubscribersCount() {
        return getSubscribedUsers().size();
    }

    private UserResponse convertToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getIsActive()
        );
    }
}