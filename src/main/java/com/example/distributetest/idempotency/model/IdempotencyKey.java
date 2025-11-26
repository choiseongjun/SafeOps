package com.example.distributetest.idempotency.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {
    private String key;
    private String response;
    private int statusCode;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
