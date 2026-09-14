package com.chaospay.domain;

import java.util.Map;
import java.util.Set;

public enum PaymentStatus {
    INITIATED, PROCESSING, SUCCESS, FAILED, RETRYING, DLQ;

    private static final Map<PaymentStatus, Set<PaymentStatus>> TRANSITIONS = Map.of(
        INITIATED,  Set.of(PROCESSING, FAILED),
        PROCESSING, Set.of(SUCCESS, FAILED, RETRYING),
        RETRYING,   Set.of(PROCESSING, SUCCESS, FAILED, DLQ),
        FAILED,     Set.of(DLQ),
        SUCCESS,    Set.of(),
        DLQ,        Set.of()
    );

    public boolean canTransitionTo(PaymentStatus next) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }
}
