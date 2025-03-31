package com.helps.controller;

import com.helps.domain.service.TicketTypeService;
import com.helps.dto.TicketTypeDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ticket-types")
@Tag(name = "Gerenciamento de Tipos de Ticket", description = "APIs para gerenciar tipos de tickets")
public class TicketTypeController {
    private static final Logger logger = LoggerFactory.getLogger(TicketTypeController.class);

    @Autowired
    private TicketTypeService ticketTypeService;

    @GetMapping
    @Operation(summary = "Listar todos os tipos de ticket")
    public ResponseEntity<List<TicketTypeDto>> getAllTicketTypes() {
        logger.debug("Listando todos os tipos de ticket");
        return ResponseEntity.ok(ticketTypeService.getAllTicketTypes());
    }

    @GetMapping("/active")
    @Operation(summary = "Listar tipos de ticket ativos")
    public ResponseEntity<List<TicketTypeDto>> getActiveTicketTypes() {
        logger.debug("Listando tipos de ticket ativos");
        return ResponseEntity.ok(ticketTypeService.getActiveTicketTypes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar tipo de ticket por ID")
    public ResponseEntity<TicketTypeDto> getTicketTypeById(@PathVariable Long id) {
        logger.debug("Buscando tipo de ticket com ID: {}", id);
        return ResponseEntity.ok(ticketTypeService.getTicketTypeById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Criar novo tipo de ticket")
    public ResponseEntity<TicketTypeDto> createTicketType(@Valid @RequestBody TicketTypeDto ticketTypeDto) {
        logger.info("Criando novo tipo de ticket: {}", ticketTypeDto.name());
        TicketTypeDto createdType = ticketTypeService.createTicketType(ticketTypeDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdType);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Atualizar tipo de ticket existente")
    public ResponseEntity<TicketTypeDto> updateTicketType(
            @PathVariable Long id,
            @Valid @RequestBody TicketTypeDto ticketTypeDto) {
        logger.info("Atualizando tipo de ticket com ID: {}", id);
        TicketTypeDto updatedType = ticketTypeService.updateTicketType(id, ticketTypeDto);
        return ResponseEntity.ok(updatedType);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Excluir tipo de ticket")
    public ResponseEntity<Void> deleteTicketType(@PathVariable Long id) {
        logger.info("Excluindo tipo de ticket com ID: {}", id);
        ticketTypeService.deleteTicketType(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Ativar/Desativar tipo de ticket")
    public ResponseEntity<TicketTypeDto> toggleTicketTypeActive(@PathVariable Long id) {
        logger.info("Alternando estado de ativação do tipo de ticket com ID: {}", id);
        TicketTypeDto updatedType = ticketTypeService.toggleTicketTypeActive(id);
        return ResponseEntity.ok(updatedType);
    }
}