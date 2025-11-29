package com.example.distributetest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {
    private String refundId;
    private String originalTransactionId;
    private Double amount;
    private String status;
    private LocalDateTime timestamp;
    private String message;
}
