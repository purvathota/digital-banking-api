package com.ledgercore.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgercore.config.JwtTokenProvider;
import com.ledgercore.exception.ErrorResponse;
import com.ledgercore.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Per-user rate limiting filter for transaction endpoints.
 * Intercepts requests to deposit, withdraw, and transfer endpoints.
 * If the user exceeds 5 requests per second, returns HTTP 429.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final List<String> RATE_LIMITED_PATTERNS = List.of(
            "/api/accounts/*/deposit",
            "/api/accounts/*/withdraw",
            "/api/transfers"
    );

    private final RateLimitService rateLimitService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitFilter(RateLimitService rateLimitService,
                           JwtTokenProvider jwtTokenProvider,
                           ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Only apply rate limiting to transaction endpoints
        boolean isRateLimited = RATE_LIMITED_PATTERNS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));

        if (!isRateLimited) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract user ID from JWT
        String token = extractToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            // Let the security filter chain handle authentication
            filterChain.doFilter(request, response);
            return;
        }

        String userId = jwtTokenProvider.extractUserId(token).toString();

        if (!rateLimitService.isAllowed(userId)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ErrorResponse errorResponse = ErrorResponse.of(
                    "RATE_LIMIT_EXCEEDED",
                    "Too many requests. Maximum 5 requests per second allowed. Please try again shortly."
            );

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return; // Do NOT continue the filter chain
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
