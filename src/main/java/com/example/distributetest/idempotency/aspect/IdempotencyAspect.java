package com.example.distributetest.idempotency.aspect;

import com.example.distributetest.idempotency.annotation.Idempotent;
import com.example.distributetest.idempotency.model.IdempotencyKey;
import com.example.distributetest.idempotency.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyAspect {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            log.warn("No HTTP request found in context, proceeding without idempotency check");
            return joinPoint.proceed();
        }

        String idempotencyKey = extractIdempotencyKey(request, idempotent);
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            log.warn("No idempotency key found, proceeding without idempotency check");
            return joinPoint.proceed();
        }

        Optional<IdempotencyKey> existingKey = idempotencyService.get(idempotencyKey);
        if (existingKey.isPresent()) {
            log.info("Idempotency key found: {}, returning cached response", idempotencyKey);
            return buildResponseFromCache(existingKey.get());
        }

        log.info("Processing request with idempotency key: {}", idempotencyKey);
        Object result = joinPoint.proceed();

        if (result instanceof ResponseEntity<?> responseEntity) {
            cacheResponse(idempotencyKey, responseEntity, idempotent.ttl());
        }

        return result;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String extractIdempotencyKey(HttpServletRequest request, Idempotent idempotent) {
        String keyFromHeader = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (keyFromHeader != null && !keyFromHeader.isEmpty()) {
            return keyFromHeader;
        }

        if (!idempotent.keyExpression().isEmpty()) {
            return idempotent.keyExpression();
        }

        return null;
    }

    private void cacheResponse(String key, ResponseEntity<?> response, long ttl) {
        try {
            String responseBody = objectMapper.writeValueAsString(response.getBody());
            int statusCode = response.getStatusCode().value();
            idempotencyService.save(key, responseBody, statusCode, ttl);
            log.debug("Cached response for idempotency key: {}", key);
        } catch (Exception e) {
            log.error("Failed to cache response for key: {}", key, e);
        }
    }

    private Object buildResponseFromCache(IdempotencyKey cachedKey) {
        try {
            Object body = objectMapper.readValue(cachedKey.getResponse(), Object.class);
            return ResponseEntity
                    .status(HttpStatus.valueOf(cachedKey.getStatusCode()))
                    .body(body);
        } catch (Exception e) {
            log.error("Failed to deserialize cached response for key: {}", cachedKey.getKey(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving cached response");
        }
    }
}
