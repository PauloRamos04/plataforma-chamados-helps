package com.helps.infra.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Profile("!prod")
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(Arrays.asList(
                "users",
                "roles",
                "tickets",
                "notifications",
                "ticket-stats",
                "user-sessions",
                "activity-logs"
        ));
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    @Scheduled(fixedRate = 300000)
    public void evictAllCaches() {
        cacheManager().getCacheNames()
                .forEach(cacheName -> {
                    if (cacheManager().getCache(cacheName) != null) {
                        cacheManager().getCache(cacheName).clear();
                    }
                });
    }
}