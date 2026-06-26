package com.ledgercore.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgercore.dto.response.TransactionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Idempotency key service backed by Redis.
 * <p>
 * Prevents duplicate processing of deposit and transfer operations.
 * Uses Redis SET NX (setIfAbsent) for atomic check-and-set:
 * <p>
 * 1. If the key does NOT exist: claim it, execute the action, store the result.
 * 2. If the key DOES exist: return the cached result without re-processing.
 * <p>
 * Keys are stored with a 24-hour TTL.
 * <p>
 * WHY: When a client sends a deposit/transfer request and the network drops
 * before the response arrives, the client cannot know if the operation succeeded.
 * Without idempotency keys, a retry would process the operation twice.
 * With this mechanism, retries are safe — the same result is returned.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String KEY_PREFIX = "idempotency:";
    private static final long TTL_HOURS = 24;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyService(RedisTemplate<String, String> redisTemplate,
                              ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Execute an action with idempotency protection.
     * <p>
     * If the idempotency key is null or blank, the action executes without protection.
     * If the key exists in Redis, the cached result is returned.
     * If the key is new, the action executes, and the result is stored.
     *
     * @param idempotencyKey the client-provided key (nullable)
     * @param action         the action to execute
     * @return the result (either fresh or cached)
     */
    public TransactionResponse executeWithIdempotency(String idempotencyKey,
                                                      Supplier<TransactionResponse> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();
        }

        String redisKey = KEY_PREFIX + idempotencyKey;

        try {
            // Attempt to check if key already exists
            String cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null) {
                log.info("Idempotency key '{}' found in cache — returning cached result",
                        idempotencyKey);
                return objectMapper.readValue(cached, TransactionResponse.class);
            }

            // Execute the action
            TransactionResponse result = action.get();

            // Store the result with 24-hour TTL
            String serialized = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(redisKey, serialized, TTL_HOURS, TimeUnit.HOURS);

            return result;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize/deserialize idempotency result", e);
            // If serialization fails, execute without idempotency protection
            return action.get();
        } catch (Exception e) {
            // If Redis is unavailable, execute without idempotency protection
            log.warn("Redis unavailable for idempotency check — executing without protection", e);
            return action.get();
        }
    }

    /**
     * Check if a cached result exists for the given idempotency key.
     */
    public Optional<TransactionResponse> getCachedResult(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        try {
            String cached = redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
            if (cached != null) {
                return Optional.of(objectMapper.readValue(cached, TransactionResponse.class));
            }
        } catch (Exception e) {
            log.warn("Error checking idempotency cache", e);
        }

        return Optional.empty();
    }

    public TransactionResponse checkAndStore(String key, Supplier<TransactionResponse> action) {
        String prefixedKey = KEY_PREFIX + key;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(prefixedKey, "PENDING", 24, TimeUnit.HOURS);

        if (Boolean.FALSE.equals(success)) {
            // Key already exists, poll for up to 5 seconds
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Polled idempotency wait interrupted", e);
                }
                String cached = redisTemplate.opsForValue().get(prefixedKey);
                if (cached != null && !"PENDING".equals(cached)) {
                    try {
                        return objectMapper.readValue(cached, TransactionResponse.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to deserialize cached response", e);
                    }
                }
            }
            throw new RuntimeException("Timeout waiting for concurrent request to complete");
        } else {
            // Key is new, execute action
            try {
                TransactionResponse result = action.get();
                String serialized = objectMapper.writeValueAsString(result);
                redisTemplate.opsForValue().set(prefixedKey, serialized, 24, TimeUnit.HOURS);
                return result;
            } catch (Throwable t) {
                redisTemplate.delete(prefixedKey);
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                throw new RuntimeException(t);
            }
        }
    }
}
