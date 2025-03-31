package com.helps.controller;

import com.helps.domain.service.ReportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Administração - Relatórios", description = "APIs para exportação de relatórios")
public class ReportExportController {
    private static final Logger logger = LoggerFactory.getLogger(ReportExportController.class);

    @Autowired
    private ReportExportService reportExportService;

    @GetMapping("/tickets")
    @Operation(summary = "Exportar tickets para CSV")
    public ResponseEntity<byte[]> exportTicketsCSV(
            @RequestParam(required = false) String status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        logger.debug("Requisição para exportar tickets para CSV");

        byte[] csvContent = reportExportService.exportTicketsCSV(status, startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tickets_export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent);
    }

    @GetMapping("/user/{userId}/tickets")
    @Operation(summary = "Exportar tickets de um usuário para CSV")
    public ResponseEntity<byte[]> exportUserTicketsCSV(@PathVariable Long userId) {
        logger.debug("Requisição para exportar tickets do usuário ID: {} para CSV", userId);

        byte[] csvContent = reportExportService.exportUserTicketsCSV(userId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=user_tickets_export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent);
    }

    @GetMapping("/helper/{userId}/tickets")
    @Operation(summary = "Exportar tickets atendidos por um helper para CSV")
    public ResponseEntity<byte[]> exportHelperTicketsCSV(@PathVariable Long userId) {
        logger.debug("Requisição para exportar tickets atendidos pelo helper ID: {} para CSV", userId);

        byte[] csvContent = reportExportService.exportHelperTicketsCSV(userId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=helper_tickets_export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent);
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Exportar logs de auditoria para CSV")
    public ResponseEntity<byte[]> exportAuditLogsCSV(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        logger.debug("Requisição para exportar logs de auditoria para CSV");

        byte[] csvContent = reportExportService.exportAuditLogsCSV(entityType, entityId, startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit_logs_export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent);
    }
}