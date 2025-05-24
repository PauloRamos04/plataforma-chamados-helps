package com.helps.controller;

import com.helps.domain.service.DashboardService;
import com.helps.dto.ApiResponse;
import com.helps.dto.DashboardStatsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        try {
            DashboardStatsDto stats = dashboardService.getDashboardStats();
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Erro ao carregar estatísticas do dashboard"));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> getSystemHealth() {
        return ResponseEntity.ok(ApiResponse.success("Sistema operacional"));
    }
}