package com.ledgercore.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    public static final String IDEMPOTENCY_KEY = "IDEMPOTENCY_KEY";
    public static final String IDEMPOTENCY_RESPONSE_BODY = "IDEMPOTENCY_RESPONSE_BODY";
    private static final String KEY_PREFIX = "idempotency:";

    private final RedisTemplate<String, String> redisTemplate;

    public IdempotencyInterceptor(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) {
            return true;
        }

        String redisKey = KEY_PREFIX + key;
        String cachedValue = redisTemplate.opsForValue().get(redisKey);

        if (cachedValue != null && !"PENDING".equals(cachedValue)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(cachedValue);
            return false;
        }

        request.setAttribute(IDEMPOTENCY_KEY, key);
        redisTemplate.opsForValue().setIfAbsent(redisKey, "PENDING", 24, TimeUnit.HOURS);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String key = (String) request.getAttribute(IDEMPOTENCY_KEY);
        if (key == null) {
            return;
        }

        String redisKey = KEY_PREFIX + key;
        int status = response.getStatus();

        if (status >= 200 && status < 300) {
            String body = (String) request.getAttribute(IDEMPOTENCY_RESPONSE_BODY);
            if (body != null) {
                redisTemplate.opsForValue().set(redisKey, body, 24, TimeUnit.HOURS);
            }
        } else {
            redisTemplate.delete(redisKey);
        }
    }
}
