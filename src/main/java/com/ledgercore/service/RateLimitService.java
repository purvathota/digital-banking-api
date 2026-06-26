package com.ledgercore.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Per-user rate limiting using Redis.
 * Implements a fixed-window counter: max 5 requests per second per user
 * on transaction endpoints (deposit, withdraw, transfer).
 * <p>
 * Key pattern: "rate:{userId}:{epochSecond}"
 * On first increment, sets a 2-second TTL to auto-cleanup.
 */
@Service
public class RateLimitService {

    private static final int MAX_REQUESTS_PER_SECOND = 5;
    private static final String KEY_PREFIX = "rate:";

    private final RedisTemplate<String, String> redisTemplate;

    public RateLimitService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Check if the user is allowed to make another request.
     *
     * @param userId the authenticated user's ID
     * @return true if the request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String userId) {
        try {
            long epochSecond = Instant.now().getEpochSecond();
            String key = KEY_PREFIX + userId + ":" + epochSecond;

            Long count = redisTemplate.opsForValue().increment(key);

            if (count != null && count == 1) {
                // First request in this second — set expiry for cleanup
                redisTemplate.expire(key, 2, TimeUnit.SECONDS);
            }

            return count != null && count <= MAX_REQUESTS_PER_SECOND;
        } catch (Exception e) {
            // If Redis is unavailable, allow the request (fail-open)
            // In production, you might want to fail-closed instead
            return true;
        }
    }
}
