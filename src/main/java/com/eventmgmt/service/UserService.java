package com.eventmgmt.service;

import com.eventmgmt.model.User;
import com.eventmgmt.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email is already in use.");
        }

        // Encrypt password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Normalize role (sets default to ROLE_ATTENDEE if invalid or empty)
        user.setRole(normalizeRole(user.getRole()));

        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Current user context not found in database."));
    }

    /**
     * Normalizes a role string to a valid role value.
     * If the role is null, empty, or invalid, returns the default role ROLE_ATTENDEE.
     */
    public String normalizeRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return "ROLE_ATTENDEE";
        }
        if (!role.equals("ROLE_ATTENDEE") &&
            !role.equals("ROLE_ORGANIZER") &&
            !role.equals("ROLE_ADMIN")) {
            return "ROLE_ATTENDEE";
        }
        return role;
    }

    /**
     * Validates that the role is one of the allowed values.
     * Throws IllegalArgumentException if the role is invalid.
     */
    public void validateRole(String role) {
        if (!role.equals("ROLE_ATTENDEE") &&
            !role.equals("ROLE_ORGANIZER") &&
            !role.equals("ROLE_ADMIN")) {
            throw new IllegalArgumentException("Invalid user role specified.");
        }
    }
}
