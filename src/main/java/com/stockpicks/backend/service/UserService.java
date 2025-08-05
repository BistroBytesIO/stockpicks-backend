package com.stockpicks.backend.service;

import com.stockpicks.backend.dto.auth.RegisterRequest;
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

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getSubscribedUsers() {
        List<UserSubscription> activeSubscriptions = userSubscriptionRepository
                .findAll()
                .stream()
                .filter(sub -> sub.getStatus() == SubscriptionStatus.ACTIVE)
                .collect(Collectors.toList());

        return activeSubscriptions.stream()
                .map(UserSubscription::getUser)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<User> getNonSubscribedUsers() {
        List<User> subscribedUsers = getSubscribedUsers();
        List<User> allUsers = getAllUsers();

        return allUsers.stream()
                .filter(user -> !subscribedUsers.contains(user))
                .collect(Collectors.toList());
    }

    public long getTotalUsersCount() {
        return userRepository.count();
    }

    public long getActiveSubscribersCount() {
        return getSubscribedUsers().size();
    }
}