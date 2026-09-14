package com.chaospay;

import com.chaospay.dto.PaymentRequest;
import com.chaospay.dto.PaymentResponse;
import com.chaospay.domain.PaymentStatus;
import com.chaospay.repository.LedgerRepository;
import com.chaospay.repository.PaymentRepository;
import com.chaospay.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/chaospay?options=-c%20TimeZone=UTC",
        "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
})
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    private static final UUID ALICE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOB   = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void shouldProcessPaymentAndCreateBalancedLedgerEntries() {
        String idemKey = "test-" + UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(ALICE, BOB, 100L, "INR");

        PaymentResponse response = paymentService.processPayment(request, idemKey);

        assertThat(response).isNotNull();
        assertThat(response.amount()).isEqualTo(100L);
        assertThat(response.currency()).isEqualTo("INR");

        if (PaymentStatus.SUCCESS.name().equals(response.status())) {
            Long net = ledgerRepository.netForPayment(response.paymentId());
            assertThat(net).isEqualTo(0L);
        }
    }

    @Test
    void shouldReturnSamePaymentForDuplicateIdempotencyKey() {
        String idemKey = "idem-" + UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(ALICE, BOB, 50L, "INR");

        PaymentResponse first = paymentService.processPayment(request, idemKey);
        PaymentResponse second = paymentService.processPayment(request, idemKey);

        assertThat(first.paymentId()).isEqualTo(second.paymentId());

        long count = paymentRepository.findAll().stream()
            .filter(p -> idemKey.equals(p.getIdempotencyKey()))
            .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldKeepGlobalLedgerBalanced() {
        for (int i = 0; i < 3; i++) {
            paymentService.processPayment(
                new PaymentRequest(ALICE, BOB, 10L, "INR"),
                "global-test-" + UUID.randomUUID()
            );
        }

        Long globalNet = ledgerRepository.netAcrossAllEntries();
        assertThat(globalNet).isEqualTo(0L);
    }
}