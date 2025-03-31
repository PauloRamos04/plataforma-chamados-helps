package com.helps.controller;

import com.helps.domain.model.Ticket;
import com.helps.domain.service.UserMetricsService;
import com.helps.dto.UserActivitySummaryDto;
import com.helps.dto.UserMetricsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Monitoramento de Usuários", description = "APIs para métricas e atividades de usuários")
public class UserMetricsController {
    private static final Logger logger = LoggerFactory.getLogger(UserMetricsController.class);

    @Autowired
    private UserMetricsService userMetricsService;

    @GetMapping("/{userId}/metrics")
    @PreAuthorize("hasAuthority('ADMIN') or #userId == authentication.principal.id")
    @Operation(summary = "Obter métricas do usuário")
    public ResponseEntity<UserMetricsDto> getUserMetrics(@PathVariable Long userId) {
        logger.debug("Requisição para obter métricas do usuário ID: {}", userId);
        return ResponseEntity.ok(userMetricsService.getUserMetrics(userId));
    }

    @GetMapping("/{userId}/activity")
    @PreAuthorize("hasAuthority('ADMIN') or #userId == authentication.principal.id")
    @Operation(summary = "Obter resumo de atividades do usuário")
    public ResponseEntity<UserActivitySummaryDto> getUserActivitySummary(@PathVariable Long userId) {
        logger.debug("Requisição para obter resumo de atividades do usuário ID: {}", userId);
        return ResponseEntity.ok(userMetricsService.getUserActivitySummary(userId));
    }

    @GetMapping("/{userId}/tickets")
    @PreAuthorize("hasAuthority('ADMIN') or #userId == authentication.principal.id")
    @Operation(summary = "Listar tickets criados pelo usuário")
    public ResponseEntity<Page<Ticket>> getUserTickets(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        logger.debug("Requisição para listar tickets do usuário ID: {}", userId);
        return ResponseEntity.ok(userMetricsService.getUserTickets(userId, pageable));
    }

    @GetMapping("/{userId}/helped-tickets")
    @PreAuthorize("hasAuthority('ADMIN') or #userId == authentication.principal.id")
    @Operation(summary = "Listar tickets atendidos pelo usuário (para helpers)")
    public ResponseEntity<Page<Ticket>> getUserHelperTickets(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        logger.debug("Requisição para listar tickets atendidos pelo usuário ID: {}", userId);
        return ResponseEntity.ok(userMetricsService.getUserHelperTickets(userId, pageable));
    }
}