package com.healthcare.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.config.RateLimitConfig;
import com.healthcare.dto.response.ApiResponse;
import io.github.bucket4j.Bucket;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Configuration
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter implements Filter {

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest   = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String ip  = resolveIp(httpRequest);
        String uri = httpRequest.getRequestURI();

        Bucket bucket = selectBucket(uri, ip);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP [{}] on [{}]", ip, uri);
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.getWriter().write(
                    objectMapper.writeValueAsString(
                            ApiResponse.ErrorResponse.of("Too many requests. Please slow down and try again.")));
        }
    }

    private Bucket selectBucket(String uri, String ip) {
        if (uri.startsWith("/api/auth/login") || uri.startsWith("/api/auth/register")) {
            return rateLimitConfig.resolveAuthBucket(ip);
        }
        if (uri.startsWith("/api/ai") || uri.startsWith("/api/chat")) {
            return rateLimitConfig.resolveAiBucket(ip);
        }
        return rateLimitConfig.resolveApiBucket(ip);
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
