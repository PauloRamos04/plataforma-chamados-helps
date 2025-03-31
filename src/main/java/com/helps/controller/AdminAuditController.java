package com.helps.controller;

import com.helps.domain.model.AuditLog;
import com.helps.domain.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Administração - Auditoria", description = "APIs para visualização de logs de auditoria do sistema")
public class AdminAuditController {
    private static final Logger logger = LoggerFactory.getLogger(AdminAuditController.class);

    @Autowired
    private AuditService auditService;

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Obter logs de auditoria para uma entidade específica")
    public ResponseEntity<Page<AuditLog>> getEntityAuditLogs(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @PageableDefault(size = 20) Pageable pageable) {
        logger.debug("Requisição para obter logs de auditoria da entidade: {} ID: {}", entityType, entityId);
        return ResponseEntity.ok(auditService.getEntityAuditLogs(entityType, entityId, pageable));
    }

    @GetMapping("/entity/{entityType}")
    @Operation(summary = "Obter logs de auditoria para um tipo de entidade")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByEntityType(
            @PathVariable String entityType,
            @PageableDefault(size = 20) Pageable pageable) {
        logger.debug("Requisição para obter logs de auditoria do tipo de entidade: {}", entityType);
        return ResponseEntity.ok(auditService.getAuditLogsByEntityType(entityType, pageable));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Obter logs de auditoria para um usuário específico")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        logger.debug("Requisição para obter logs de auditoria do usuário ID: {}", userId);
        return ResponseEntity.ok(auditService.getAuditLogsByUser(userId, pageable));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Obter logs de auditoria por período")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        logger.debug("Requisição para obter logs de auditoria no período de {} a {}", startDate, endDate);
        return ResponseEntity.ok(auditService.getAuditLogsByDateRange(startDate, endDate, pageable));
    }

    @GetMapping("/entity/date-range")
    @Operation(summary = "Obter logs de auditoria por tipo de entidade e período")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByEntityTypeAndDateRange(
            @RequestParam String entityType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        logger.debug("Requisição para obter logs de auditoria do tipo {} no período de {} a {}",
                entityType, startDate, endDate);
        return ResponseEntity.ok(auditService.getAuditLogsByEntityTypeAndDateRange(
                entityType, startDate, endDate, pageable));
    }
}