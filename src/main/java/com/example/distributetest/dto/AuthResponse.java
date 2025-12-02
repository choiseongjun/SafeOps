package com.example.distributetest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private String username;
    private String email;
    private String message;

    public static AuthResponse success(String accessToken, String refreshToken, Long expiresIn, String username, String email) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .username(username)
                .email(email)
                .message("Authentication successful")
                .build();
    }

    public static AuthResponse error(String message) {
        return AuthResponse.builder()
                .message(message)
                .build();
    }
}
