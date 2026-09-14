package com.chaospay.service;

import com.chaospay.domain.*;
import com.chaospay.dto.PaymentRequest;
import com.chaospay.dto.PaymentResponse;
import com.chaospay.exception.InsufficientBalanceException;
import com.chaospay.exception.ProcessorException;
import com.chaospay.repository.AccountRepository;
import com.chaospay.repository.LedgerRepository;
import com.chaospay.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;
    private final IdempotencyService idempotencyService;
    private final ExternalProcessorClient processor;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, String idempotencyKey) {
        Optional<PaymentResponse> cached = idempotencyService.getCached(idempotencyKey);
        if (cached.isPresent()) {
            log.info("Cache hit for idempotency key {}", idempotencyKey);
            return cached.get();
        }

        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            PaymentResponse resp = PaymentResponse.from(existing.get());
            idempotencyService.cache(idempotencyKey, resp);
            return resp;
        }

        if (!idempotencyService.acquireLock(idempotencyKey)) {
            throw new IllegalStateException("Duplicate request in progress");
        }

        try {
            return doProcess(request, idempotencyKey);
        } finally {
            idempotencyService.releaseLock(idempotencyKey);
        }
    }

    private PaymentResponse doProcess(PaymentRequest request, String idempotencyKey) {
        Account from = accountRepository.findById(request.fromAccount())
            .orElseThrow(() -> new IllegalArgumentException("Source account not found"));
        Account to = accountRepository.findById(request.toAccount())
            .orElseThrow(() -> new IllegalArgumentException("Destination account not found"));

        if (from.getBalance() < request.amount()) {
            throw new InsufficientBalanceException("Insufficient balance in " + from.getId());
        }

        Payment payment = Payment.builder()
            .id(UUID.randomUUID())
            .idempotencyKey(idempotencyKey)
            .fromAccount(from.getId())
            .toAccount(to.getId())
            .amount(request.amount())
            .currency(request.currency())
            .status(PaymentStatus.INITIATED)
            .retryCount(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        paymentRepository.save(payment);

        payment.transitionTo(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        try {
            processor.authorize(payment.getId(), payment.getAmount(), payment.getCurrency());

            ledgerRepository.save(LedgerEntry.builder()
                .paymentId(payment.getId())
                .accountId(from.getId())
                .amount(payment.getAmount())
                .direction(LedgerDirection.DEBIT)
                .createdAt(Instant.now())
                .build());

            ledgerRepository.save(LedgerEntry.builder()
                .paymentId(payment.getId())
                .accountId(to.getId())
                .amount(payment.getAmount())
                .direction(LedgerDirection.CREDIT)
                .createdAt(Instant.now())
                .build());

            from.setBalance(from.getBalance() - payment.getAmount());
            to.setBalance(to.getBalance() + payment.getAmount());
            accountRepository.save(from);
            accountRepository.save(to);

            payment.transitionTo(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);

            PaymentResponse resp = PaymentResponse.from(payment);
            idempotencyService.cache(idempotencyKey, resp);
            log.info("Payment {} succeeded", payment.getId());
            return resp;

        } catch (ProcessorException ex) {
            payment.setFailureReason(ex.getMessage());
            payment.transitionTo(PaymentStatus.FAILED);
            payment.transitionTo(PaymentStatus.DLQ);
            paymentRepository.save(payment);

            PaymentResponse resp = PaymentResponse.from(payment);
            idempotencyService.cache(idempotencyKey, resp);
            log.error("Payment {} -> DLQ: {}", payment.getId(), ex.getMessage());
            return resp;
        }
    }
}
