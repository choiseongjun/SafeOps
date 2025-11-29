package com.example.distributetest.controller;

import com.example.distributetest.dto.PaymentRequest;
import com.example.distributetest.dto.PaymentResponse;
import com.example.distributetest.dto.RefundRequest;
import com.example.distributetest.dto.RefundResponse;
import com.example.distributetest.idempotency.annotation.Idempotent;
import com.example.distributetest.service.PaymentService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Idempotent(ttl = 60)
    @CircuitBreaker(name = "payment", fallbackMethod = "paymentFallback")
    public ResponseEntity<PaymentResponse> createPayment(@RequestBody PaymentRequest request) {
        log.info("Processing payment: {}", request);
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<PaymentResponse> paymentFallback(PaymentRequest request, Exception ex) {
        log.warn("Payment fallback triggered for request: {}, reason: {}", request, ex.getMessage());

        PaymentResponse response = PaymentResponse.builder()
                .transactionId("FALLBACK-" + UUID.randomUUID().toString())
                .amount(request.getAmount())
                .status("PENDING")
                .timestamp(LocalDateTime.now())
                .message("Payment is being processed. Please check back later. Error: " + ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @PostMapping("/refund")
    @Idempotent(ttl = 120)
    @CircuitBreaker(name = "payment", fallbackMethod = "refundFallback")
    public ResponseEntity<RefundResponse> refundPayment(@RequestBody RefundRequest request) {
        log.info("Processing refund: {}", request);
        RefundResponse response = paymentService.createRefund(request);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<RefundResponse> refundFallback(RefundRequest request, Exception ex) {
        log.warn("Refund fallback triggered for request: {}, reason: {}", request, ex.getMessage());

        RefundResponse response = RefundResponse.builder()
                .refundId("FALLBACK-" + UUID.randomUUID().toString())
                .originalTransactionId(request.getTransactionId())
                .amount(request.getAmount())
                .status("PENDING")
                .timestamp(LocalDateTime.now())
                .message("Refund is being processed. Please check back later. Error: " + ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
