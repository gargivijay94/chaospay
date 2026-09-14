package com.chaospay.dto;

import com.chaospay.domain.Payment;

import java.util.UUID;

public record PaymentResponse(
    UUID paymentId,
    String status,
    Long amount,
    String currency,
    String failureReason
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
            p.getId(), p.getStatus().name(), p.getAmount(),
            p.getCurrency(), p.getFailureReason()
        );
    }
}
