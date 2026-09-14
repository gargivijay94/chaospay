package com.chaospay.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "from_account", nullable = false)
    private UUID fromAccount;

    @Column(name = "to_account", nullable = false)
    private UUID toAccount;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void transitionTo(PaymentStatus next) {
        if (!this.status.canTransitionTo(next)) {
            throw new IllegalStateException("Illegal transition: " + this.status + " -> " + next);
        }
        this.status = next;
        this.updatedAt = Instant.now();
    }
}
