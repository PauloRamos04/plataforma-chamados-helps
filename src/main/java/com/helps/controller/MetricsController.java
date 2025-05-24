package com.helps.controller;

import com.helps.domain.service.TicketService;
import com.helps.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    @Autowired
    private TicketService ticketService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Object>> getDashboardMetrics() {
        try {
            var tickets = ticketService.listTickets();
            return ResponseEntity.ok(ApiResponse.success(tickets));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success(java.util.Collections.emptyList()));
        }
    }
}