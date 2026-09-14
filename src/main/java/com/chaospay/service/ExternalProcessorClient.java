package com.chaospay.service;

import com.chaospay.exception.ProcessorException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Slf4j
public class ExternalProcessorClient {

    @Retry(name = "processor", fallbackMethod = "fallback")
    @CircuitBreaker(name = "processor")
    public String authorize(UUID paymentId, long amount, String currency) {
        sleep(50 + ThreadLocalRandom.current().nextInt(100));

        if (ThreadLocalRandom.current().nextInt(100) < 20) {
            log.warn("Processor failed for payment {}", paymentId);
            throw new ProcessorException("Processor unavailable for " + paymentId);
        }

        log.info("Processor authorized payment {}", paymentId);
        return "AUTH-" + UUID.randomUUID();
    }

    public String fallback(UUID paymentId, long amount, String currency, Throwable t) {
        log.error("Fallback for payment {}: {}", paymentId, t.getMessage());
        throw new ProcessorException("Processor failed after retries: " + t.getMessage());
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
