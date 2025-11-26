package com.example.distributetest.idempotency.service;

import com.example.distributetest.idempotency.model.IdempotencyKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class RedisIdempotencyService implements IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyService.class);
    private static final String KEY_PREFIX = "idempotency:";
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getFullKey(key)));
    }

    @Override
    public Optional<IdempotencyKey> get(String key) {
        try {
            String value = redisTemplate.opsForValue().get(getFullKey(key));
            if (value == null) {
                return Optional.empty();
            }
            IdempotencyKey idempotencyKey = objectMapper.readValue(value, IdempotencyKey.class);
            return Optional.of(idempotencyKey);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing idempotency key: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void save(String key, String response, int statusCode, long ttlMinutes) {
        try {
            LocalDateTime now = LocalDateTime.now();
            IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                    .key(key)
                    .response(response)
                    .statusCode(statusCode)
                    .createdAt(now)
                    .expiresAt(now.plusMinutes(ttlMinutes))
                    .build();

            String value = objectMapper.writeValueAsString(idempotencyKey);
            redisTemplate.opsForValue().set(getFullKey(key), value, ttlMinutes, TimeUnit.MINUTES);
            log.debug("Saved idempotency key: {} with TTL: {} minutes", key, ttlMinutes);
        } catch (JsonProcessingException e) {
            log.error("Error serializing idempotency key: {}", key, e);
        }
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(getFullKey(key));
        log.debug("Deleted idempotency key: {}", key);
    }

    private String getFullKey(String key) {
        return KEY_PREFIX + key;
    }
}
