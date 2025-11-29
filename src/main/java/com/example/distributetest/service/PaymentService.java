package com.example.distributetest.service;

import com.example.distributetest.dto.PaymentRequest;
import com.example.distributetest.dto.PaymentResponse;
import com.example.distributetest.dto.RefundRequest;
import com.example.distributetest.dto.RefundResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String PAYMENT_KEY_PREFIX = "payment:";
    private static final String REFUND_KEY_PREFIX = "refund:";
    private static final long PAYMENT_TTL_HOURS = 24;
    private static final long REFUND_TTL_HOURS = 72;

    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("Creating payment in Redis: {}", request);

        // Simulate random failures for circuit breaker testing
        if (Math.random() < 0.3) {
            log.error("Payment processing failed!");
            throw new RuntimeException("Payment gateway error");
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        PaymentResponse response = PaymentResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .amount(request.getAmount())
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .message("Payment processed successfully")
                .build();

        // Save to Redis
        savePaymentToRedis(response);

        log.info("Payment created and saved to Redis: {}", response);
        return response;
    }

    public RefundResponse createRefund(RefundRequest request) {
        log.info("Creating refund in Redis: {}", request);

        // Simulate random failures
        if (Math.random() < 0.2) {
            log.error("Refund processing failed!");
            throw new RuntimeException("Refund service error");
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        RefundResponse response = RefundResponse.builder()
                .refundId(UUID.randomUUID().toString())
                .originalTransactionId(request.getTransactionId())
                .amount(request.getAmount())
                .status("REFUNDED")
                .timestamp(LocalDateTime.now())
                .message("Refund processed successfully")
                .build();

        // Save to Redis
        saveRefundToRedis(response);

        log.info("Refund created and saved to Redis: {}", response);
        return response;
    }

    private void savePaymentToRedis(PaymentResponse payment) {
        try {
            String key = PAYMENT_KEY_PREFIX + payment.getTransactionId();
            String value = objectMapper.writeValueAsString(payment);
            redisTemplate.opsForValue().set(key, value, PAYMENT_TTL_HOURS, TimeUnit.HOURS);
            log.debug("Payment saved to Redis with key: {} (TTL: {} hours)", key, PAYMENT_TTL_HOURS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payment to JSON", e);
            throw new RuntimeException("Failed to save payment to Redis", e);
        }
    }

    private void saveRefundToRedis(RefundResponse refund) {
        try {
            String key = REFUND_KEY_PREFIX + refund.getRefundId();
            String value = objectMapper.writeValueAsString(refund);
            redisTemplate.opsForValue().set(key, value, REFUND_TTL_HOURS, TimeUnit.HOURS);
            log.debug("Refund saved to Redis with key: {} (TTL: {} hours)", key, REFUND_TTL_HOURS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize refund to JSON", e);
            throw new RuntimeException("Failed to save refund to Redis", e);
        }
    }

    public PaymentResponse getPayment(String transactionId) {
        try {
            String key = PAYMENT_KEY_PREFIX + transactionId;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return objectMapper.readValue(value, PaymentResponse.class);
            }
            log.warn("Payment not found in Redis: {}", transactionId);
            return null;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize payment from JSON", e);
            return null;
        }
    }

    public RefundResponse getRefund(String refundId) {
        try {
            String key = REFUND_KEY_PREFIX + refundId;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return objectMapper.readValue(value, RefundResponse.class);
            }
            log.warn("Refund not found in Redis: {}", refundId);
            return null;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize refund from JSON", e);
            return null;
        }
    }
}
