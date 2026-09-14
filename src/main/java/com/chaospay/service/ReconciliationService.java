package com.chaospay.service;

import com.chaospay.domain.Payment;
import com.chaospay.domain.PaymentStatus;
import com.chaospay.repository.LedgerRepository;
import com.chaospay.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final PaymentRepository paymentRepository;
    private final LedgerRepository ledgerRepository;

    private final AtomicLong totalRuns = new AtomicLong();
    private final AtomicLong mismatches = new AtomicLong();

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void reconcile() {
        long run = totalRuns.incrementAndGet();
        List<Payment> successes = paymentRepository.findByStatus(PaymentStatus.SUCCESS);
        int bad = 0;
        for (Payment p : successes) {
            Long net = ledgerRepository.netForPayment(p.getId());
            if (net == null || net != 0L) {
                log.error("LEDGER MISMATCH payment={} net={}", p.getId(), net);
                bad++;
            }
        }
        mismatches.addAndGet(bad);

        Long globalNet = ledgerRepository.netAcrossAllEntries();
        if (globalNet != null && globalNet != 0L) {
            log.error("GLOBAL LEDGER IMBALANCE net={}", globalNet);
        }

        List<Payment> dlq = paymentRepository.findByStatus(PaymentStatus.DLQ);
        log.info("Reconciliation #{}: successes={} mismatches={} dlq={}",
            run, successes.size(), bad, dlq.size());
    }
}
