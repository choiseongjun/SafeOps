package com.example.distributetest.service;

import com.example.distributetest.dto.AuthResponse;
import com.example.distributetest.dto.LoginRequest;
import com.example.distributetest.dto.SignUpRequest;
import com.example.distributetest.entity.User;
import com.example.distributetest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        try {
            if (userRepository.existsByUsername(request.getUsername())) {
                return AuthResponse.error("Username already exists");
            }

            if (userRepository.existsByEmail(request.getEmail())) {
                return AuthResponse.error("Email already exists");
            }

            String keycloakUserId = keycloakService.createUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword()
            );

            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .keycloakId(keycloakUserId)
                    .enabled(true)
                    .build();

            userRepository.save(user);

            log.info("User registered successfully: {}", request.getUsername());

            Map<String, Object> tokenData = keycloakService.authenticateUser(
                    request.getUsername(),
                    request.getPassword()
            );

            return AuthResponse.success(
                    (String) tokenData.get("access_token"),
                    (String) tokenData.get("refresh_token"),
                    (Long) tokenData.get("expires_in"),
                    user.getUsername(),
                    user.getEmail()
            );

        } catch (Exception e) {
            log.error("Error during sign up", e);
            return AuthResponse.error("Sign up failed: " + e.getMessage());
        }
    }

    public AuthResponse login(LoginRequest request) {
        try {
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.getEnabled()) {
                return AuthResponse.error("Account is disabled");
            }

            Map<String, Object> tokenData = keycloakService.authenticateUser(
                    request.getUsername(),
                    request.getPassword()
            );

            log.info("User logged in successfully: {}", request.getUsername());

            return AuthResponse.success(
                    (String) tokenData.get("access_token"),
                    (String) tokenData.get("refresh_token"),
                    (Long) tokenData.get("expires_in"),
                    user.getUsername(),
                    user.getEmail()
            );

        } catch (Exception e) {
            log.error("Error during login", e);
            return AuthResponse.error("Login failed: " + e.getMessage());
        }
    }
}
