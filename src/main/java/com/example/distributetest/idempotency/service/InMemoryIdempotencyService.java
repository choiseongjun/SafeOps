package com.example.distributetest.idempotency.service;

import com.example.distributetest.idempotency.model.IdempotencyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryIdempotencyService implements IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryIdempotencyService.class);
    private final ConcurrentHashMap<String, IdempotencyKey> store = new ConcurrentHashMap<>();

    @Override
    public boolean exists(String key) {
        cleanupExpired();
        IdempotencyKey idempotencyKey = store.get(key);
        if (idempotencyKey != null && idempotencyKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            store.remove(key);
            return false;
        }
        return idempotencyKey != null;
    }

    @Override
    public Optional<IdempotencyKey> get(String key) {
        cleanupExpired();
        IdempotencyKey idempotencyKey = store.get(key);
        if (idempotencyKey != null && idempotencyKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.ofNullable(idempotencyKey);
    }

    @Override
    public void save(String key, String response, int statusCode, long ttlMinutes) {
        LocalDateTime now = LocalDateTime.now();
        IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .key(key)
                .response(response)
                .statusCode(statusCode)
                .createdAt(now)
                .expiresAt(now.plusMinutes(ttlMinutes))
                .build();
        store.put(key, idempotencyKey);
        log.debug("Saved idempotency key: {} with TTL: {} minutes", key, ttlMinutes);
    }

    @Override
    public void delete(String key) {
        store.remove(key);
        log.debug("Deleted idempotency key: {}", key);
    }

    private void cleanupExpired() {
        LocalDateTime now = LocalDateTime.now();
        store.entrySet().removeIf(entry -> entry.getValue().getExpiresAt().isBefore(now));
    }
}
