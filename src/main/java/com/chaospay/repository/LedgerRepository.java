package com.chaospay.repository;

import com.chaospay.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

    @Query("SELECT COALESCE(SUM(CASE WHEN l.direction = 'DEBIT' THEN -l.amount ELSE l.amount END), 0) " +
           "FROM LedgerEntry l WHERE l.paymentId = :paymentId")
    Long netForPayment(UUID paymentId);

    @Query("SELECT COALESCE(SUM(CASE WHEN l.direction = 'DEBIT' THEN -l.amount ELSE l.amount END), 0) " +
           "FROM LedgerEntry l")
    Long netAcrossAllEntries();
}
