package com.helps.controller;

import com.helps.domain.service.AdminDashboardService;
import com.helps.dto.AdminDashboardDto;
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
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Administração - Dashboard", description = "APIs para visualização de métricas e estatísticas administrativas")
public class AdminDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);

    @Autowired
    private AdminDashboardService adminDashboardService;

    @GetMapping
    @Operation(summary = "Obter dados do dashboard administrativo")
    public ResponseEntity<AdminDashboardDto> getAdminDashboard() {
        logger.debug("Requisição para obter dados do dashboard administrativo");
        return ResponseEntity.ok(adminDashboardService.getAdminDashboard());
    }
}