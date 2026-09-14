package com.chaospay.service;

import com.chaospay.dto.PaymentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public boolean acquireLock(String key) {
        Boolean ok = redis.opsForValue().setIfAbsent("lock:" + key, "1", LOCK_TTL);
        return Boolean.TRUE.equals(ok);
    }

    public void releaseLock(String key) {
        redis.delete("lock:" + key);
    }

    public Optional<PaymentResponse> getCached(String key) {
        String json = redis.opsForValue().get("idem:" + key);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, PaymentResponse.class));
        } catch (Exception e) {
            log.warn("Failed to parse cached response for key {}", key, e);
            return Optional.empty();
        }
    }

    public void cache(String key, PaymentResponse response) {
        try {
            redis.opsForValue().set("idem:" + key,
                objectMapper.writeValueAsString(response), TTL);
        } catch (Exception e) {
            log.warn("Failed to cache response for key {}", key, e);
        }
    }
}
