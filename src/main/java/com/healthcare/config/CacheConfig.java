package com.healthcare.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_DOCTORS      = "doctors";
    public static final String CACHE_CHAT_HISTORY = "chat-history";
    public static final String CACHE_AI_CONTEXT   = "ai-context";
    public static final String CACHE_USER_SESSION = "user-session";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                CACHE_DOCTORS,
                CACHE_CHAT_HISTORY,
                CACHE_AI_CONTEXT,
                CACHE_USER_SESSION
        );
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}