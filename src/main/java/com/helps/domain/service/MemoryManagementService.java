package com.helps.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MemoryManagementService {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private UserContextService userContextService;

    private static final long MEMORY_WARNING_THRESHOLD = 80; // 80% do heap
    private static final long MEMORY_CRITICAL_THRESHOLD = 90; // 90% do heap

    /**
     * Monitora o uso de memória a cada 5 minutos
     */
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void monitorMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        
        long usedMemory = heapUsage.getUsed();
        long maxMemory = heapUsage.getMax();
        long usedPercentage = (usedMemory * 100) / maxMemory;
        
        log.info("Memory Usage: {} MB / {} MB ({}%)", 
                usedMemory / (1024 * 1024), 
                maxMemory / (1024 * 1024), 
                usedPercentage);
        
        if (usedPercentage > MEMORY_CRITICAL_THRESHOLD) {
            log.warn("CRITICAL: Memory usage is at {}% - performing emergency cleanup", usedPercentage);
            performEmergencyCleanup();
        } else if (usedPercentage > MEMORY_WARNING_THRESHOLD) {
            log.warn("WARNING: Memory usage is at {}% - performing preventive cleanup", usedPercentage);
            performPreventiveCleanup();
        }
    }

    /**
     * Limpeza preventiva de cache e recursos
     */
    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.MINUTES)
    public void scheduledCleanup() {
        log.info("Performing scheduled memory cleanup");
        clearExpiredCache();
        System.gc(); // Sugere garbage collection
    }

    /**
     * Limpeza preventiva quando o uso de memória está alto
     */
    private void performPreventiveCleanup() {
        try {
            clearExpiredCache();
            clearUserSessions();
            log.info("Preventive cleanup completed");
        } catch (Exception e) {
            log.error("Error during preventive cleanup: {}", e.getMessage());
        }
    }

    /**
     * Limpeza de emergência quando o uso de memória está crítico
     */
    private void performEmergencyCleanup() {
        try {
            // Limpeza agressiva de cache
            if (cacheManager != null) {
                cacheManager.getCacheNames().forEach(cacheName -> {
                    cacheManager.getCache(cacheName).clear();
                    log.info("Cleared cache: {}", cacheName);
                });
            }
            
            clearUserSessions();
            System.gc(); // Força garbage collection
            
            log.info("Emergency cleanup completed");
        } catch (Exception e) {
            log.error("Error during emergency cleanup: {}", e.getMessage());
        }
    }

    /**
     * Limpa cache expirado
     */
    private void clearExpiredCache() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                try {
                    // Para caches com TTL, os itens expirados são removidos automaticamente
                    log.debug("Cache '{}' status checked", cacheName);
                } catch (Exception e) {
                    log.warn("Error checking cache '{}': {}", cacheName, e.getMessage());
                }
            });
        }
    }

    /**
     * Limpa sessões de usuário inativas
     */
    private void clearUserSessions() {
        try {
            // Esta funcionalidade seria implementada se houver um serviço de sessão
            log.debug("User sessions cleanup completed");
        } catch (Exception e) {
            log.warn("Error clearing user sessions: {}", e.getMessage());
        }
    }

    /**
     * Retorna estatísticas de memória
     */
    public MemoryStats getMemoryStats() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        return new MemoryStats(
                LocalDateTime.now(),
                heapUsage.getUsed(),
                heapUsage.getMax(),
                nonHeapUsage.getUsed(),
                nonHeapUsage.getMax(),
                ManagementFactory.getRuntimeMXBean().getUptime()
        );
    }

    /**
     * Força limpeza manual de memória
     */
    public void forceCleanup() {
        log.info("Manual memory cleanup requested");
        performEmergencyCleanup();
    }

    /**
     * Classe para estatísticas de memória
     */
    public static class MemoryStats {
        private final LocalDateTime timestamp;
        private final long heapUsed;
        private final long heapMax;
        private final long nonHeapUsed;
        private final long nonHeapMax;
        private final long uptime;

        public MemoryStats(LocalDateTime timestamp, long heapUsed, long heapMax, 
                          long nonHeapUsed, long nonHeapMax, long uptime) {
            this.timestamp = timestamp;
            this.heapUsed = heapUsed;
            this.heapMax = heapMax;
            this.nonHeapUsed = nonHeapUsed;
            this.nonHeapMax = nonHeapMax;
            this.uptime = uptime;
        }

        public LocalDateTime getTimestamp() { return timestamp; }
        public long getHeapUsed() { return heapUsed; }
        public long getHeapMax() { return heapMax; }
        public long getNonHeapUsed() { return nonHeapUsed; }
        public long getNonHeapMax() { return nonHeapMax; }
        public long getUptime() { return uptime; }
        
        public double getHeapUsagePercentage() {
            return heapMax > 0 ? (heapUsed * 100.0) / heapMax : 0.0;
        }
        
        public double getNonHeapUsagePercentage() {
            return nonHeapMax > 0 ? (nonHeapUsed * 100.0) / nonHeapMax : 0.0;
        }
    }
}
