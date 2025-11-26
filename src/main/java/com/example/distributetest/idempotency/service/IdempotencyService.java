package com.example.distributetest.idempotency.service;

import com.example.distributetest.idempotency.model.IdempotencyKey;

import java.util.Optional;

public interface IdempotencyService {
    boolean exists(String key);
    Optional<IdempotencyKey> get(String key);
    void save(String key, String response, int statusCode, long ttlMinutes);
    void delete(String key);
}
