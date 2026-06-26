package com.ledgercore.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyInterceptorTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private IdempotencyInterceptor interceptor;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("No idempotency key header: preHandle returns true, no Redis interaction")
    void preHandle_noHeader_returnsTrue() throws Exception {
        when(request.getHeader("Idempotency-Key")).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("Key not in Redis: preHandle returns true, stores key in request attribute")
    void preHandle_keyNotInRedis_returnsTrue() throws Exception {
        String key = "test-key";
        when(request.getHeader("Idempotency-Key")).thenReturn(key);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:" + key)).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(request).setAttribute(IdempotencyInterceptor.IDEMPOTENCY_KEY, key);
        verify(valueOperations).setIfAbsent(eq("idempotency:" + key), eq("PENDING"), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("Key in Redis with cached response: preHandle writes cached JSON, returns false")
    void preHandle_keyInRedis_returnsFalse() throws Exception {
        String key = "test-key";
        String cachedJson = "{\"id\":\"tx-123\"}";
        when(request.getHeader("Idempotency-Key")).thenReturn(key);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:" + key)).thenReturn(cachedJson);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        assertThat(stringWriter.toString()).isEqualTo(cachedJson);
    }

    @Test
    @DisplayName("afterCompletion with 2xx and response body attribute set: stores the body in Redis")
    void afterCompletion_success_storesBody() throws Exception {
        String key = "test-key";
        String bodyJson = "{\"id\":\"tx-123\"}";
        when(request.getAttribute(IdempotencyInterceptor.IDEMPOTENCY_KEY)).thenReturn(key);
        when(response.getStatus()).thenReturn(200);
        when(request.getAttribute(IdempotencyInterceptor.IDEMPOTENCY_RESPONSE_BODY)).thenReturn(bodyJson);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        interceptor.afterCompletion(request, response, new Object(), null);

        verify(valueOperations).set(eq("idempotency:" + key), eq(bodyJson), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("afterCompletion with 4xx: deletes the Redis key")
    void afterCompletion_failure_deletesKey() throws Exception {
        String key = "test-key";
        when(request.getAttribute(IdempotencyInterceptor.IDEMPOTENCY_KEY)).thenReturn(key);
        when(response.getStatus()).thenReturn(400);

        interceptor.afterCompletion(request, response, new Object(), null);

        verify(redisTemplate).delete("idempotency:" + key);
    }
}
