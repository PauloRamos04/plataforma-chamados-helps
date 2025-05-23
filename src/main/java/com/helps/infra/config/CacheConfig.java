package com.helps.infra.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Profile("!prod")
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(java.util.Arrays.asList(
                "users",
                "roles",
                "tickets",
                "notifications",
                "ticket-stats"
        ));
        return cacheManager;
    }
}