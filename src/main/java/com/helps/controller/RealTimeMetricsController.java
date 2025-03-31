package com.helps.controller;

import com.helps.domain.service.RealTimeMetricsService;
import com.helps.dto.RealTimeMetricsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Métricas em Tempo Real", description = "APIs para monitoramento de métricas em tempo real")
public class RealTimeMetricsController {
    private static final Logger logger = LoggerFactory.getLogger(RealTimeMetricsController.class);

    @Autowired
    private RealTimeMetricsService realTimeMetricsService;

    @GetMapping("/real-time")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'HELPER')")
    @Operation(summary = "Obter métricas em tempo real")
    public ResponseEntity<RealTimeMetricsDto> getRealTimeMetrics() {
        logger.debug("Requisição para obter métricas em tempo real");
        RealTimeMetricsDto metrics = realTimeMetricsService.getCurrentMetrics();
        return ResponseEntity.ok(metrics);
    }
}