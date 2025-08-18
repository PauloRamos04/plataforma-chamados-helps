package com.helps.controller;

import com.helps.domain.service.MemoryManagementService;
import com.helps.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/memory")
@PreAuthorize("hasRole('ADMIN')")
public class MemoryController {

    @Autowired
    private MemoryManagementService memoryManagementService;

    /**
     * Retorna estatísticas de memória da aplicação
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMemoryStats() {
        MemoryManagementService.MemoryStats stats = memoryManagementService.getMemoryStats();
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", stats.getTimestamp());
        response.put("heapUsedMB", stats.getHeapUsed() / (1024 * 1024));
        response.put("heapMaxMB", stats.getHeapMax() / (1024 * 1024));
        response.put("heapUsagePercentage", Math.round(stats.getHeapUsagePercentage() * 100.0) / 100.0);
        response.put("nonHeapUsedMB", stats.getNonHeapUsed() / (1024 * 1024));
        response.put("nonHeapMaxMB", stats.getNonHeapMax() / (1024 * 1024));
        response.put("nonHeapUsagePercentage", Math.round(stats.getNonHeapUsagePercentage() * 100.0) / 100.0);
        response.put("uptimeSeconds", stats.getUptime() / 1000);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Força limpeza manual de memória
     */
    @PostMapping("/cleanup")
    public ResponseEntity<ApiResponse> forceCleanup() {
        memoryManagementService.forceCleanup();
        return ResponseEntity.ok(new ApiResponse(true, "Limpeza de memória executada com sucesso", null, null));
    }

    /**
     * Retorna informações detalhadas sobre o uso de memória
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalMemoryMB", runtime.totalMemory() / (1024 * 1024));
        response.put("freeMemoryMB", runtime.freeMemory() / (1024 * 1024));
        response.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        response.put("maxMemoryMB", runtime.maxMemory() / (1024 * 1024));
        response.put("availableProcessors", runtime.availableProcessors());
        
        // Informações do sistema
        System.getProperties().forEach((key, value) -> {
            if (key.toString().startsWith("java.") || key.toString().startsWith("os.")) {
                response.put(key.toString(), value);
            }
        });
        
        return ResponseEntity.ok(response);
    }
}
