package com.example.distributetest.controller;

import com.example.distributetest.idempotency.annotation.Idempotent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/api/payments")
public class PaymentController {


    @PostMapping
    @Idempotent(ttl = 60)
    public ResponseEntity<PaymentResponse> createPayment(@RequestBody PaymentRequest request) {
        log.info("Processing payment: {}", request);


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

        log.info("Payment created: {}", response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund")
    @Idempotent(ttl = 120)
    public ResponseEntity<RefundResponse> refundPayment(@RequestBody RefundRequest request) {
        log.info("Processing refund: {}", request);

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

        log.info("Refund created: {}", response);
        return ResponseEntity.ok(response);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentRequest {
        private Double amount;
        private String currency;
        private String customerId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class PaymentResponse {
        private String transactionId;
        private Double amount;
        private String status;
        private LocalDateTime timestamp;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundRequest {
        private String transactionId;
        private Double amount;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class RefundResponse {
        private String refundId;
        private String originalTransactionId;
        private Double amount;
        private String status;
        private LocalDateTime timestamp;
        private String message;
    }
}
