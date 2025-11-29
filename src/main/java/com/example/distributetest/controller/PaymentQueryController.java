package com.example.distributetest.controller;

import com.example.distributetest.dto.PaymentResponse;
import com.example.distributetest.dto.RefundResponse;
import com.example.distributetest.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentQueryController {

    private final PaymentService paymentService;

    @GetMapping("/{transactionId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String transactionId) {
        log.info("Retrieving payment from Redis: {}", transactionId);
        PaymentResponse payment = paymentService.getPayment(transactionId);

        if (payment != null) {
            return ResponseEntity.ok(payment);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/refund/{refundId}")
    public ResponseEntity<RefundResponse> getRefund(@PathVariable String refundId) {
        log.info("Retrieving refund from Redis: {}", refundId);
        RefundResponse refund = paymentService.getRefund(refundId);

        if (refund != null) {
            return ResponseEntity.ok(refund);
        }
        return ResponseEntity.notFound().build();
    }
}
