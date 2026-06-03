package com.healthcare.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    // Per-IP buckets
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> aiBuckets    = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiBuckets   = new ConcurrentHashMap<>();

    /** Auth endpoints: 10 requests per minute */
    public Bucket resolveAuthBucket(String ip) {
        return authBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                      .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build())
                      .build());
    }

    /** AI/analysis endpoints: 20 requests per minute */
    public Bucket resolveAiBucket(String ip) {
        return aiBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                      .addLimit(Bandwidth.builder().capacity(20).refillGreedy(20, Duration.ofMinutes(1)).build())
                      .build());
    }

    /** General API: 100 requests per minute */
    public Bucket resolveApiBucket(String ip) {
        return apiBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                      .addLimit(Bandwidth.builder().capacity(100).refillGreedy(100, Duration.ofMinutes(1)).build())
                      .build());
    }
}
