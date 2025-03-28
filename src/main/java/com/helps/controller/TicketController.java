package com.helps.controller;

import com.helps.domain.model.Ticket;
import com.helps.domain.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chamados") // Keeping the endpoint URL the same
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @GetMapping
    public List<Ticket> listTickets() { // previously listarChamados
        return ticketService.listTickets();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable Long id) { // previously buscarChamado
        return ticketService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Ticket> openTicket(@RequestBody Ticket ticket) { // previously abrirChamado
        Ticket newTicket = ticketService.openTicket(ticket);
        return ResponseEntity.status(HttpStatus.CREATED).body(newTicket);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ticket> updateTicket(@PathVariable Long id, @RequestBody Ticket ticket) { // previously atualizarChamado
        try {
            Ticket updatedTicket = ticketService.updateTicket(id, ticket);
            return ResponseEntity.ok(updatedTicket);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/{id}/aderir", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<Ticket> assignTicket(@PathVariable Long id) { // previously aderirChamado
        try {
            Ticket ticket = ticketService.assignTicket(id);
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @RequestMapping(value = {"/{id}/fechar", "/{id}/finalizar"}, method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<Void> closeTicket(@PathVariable Long id) { // previously fecharChamado
        try {
            ticketService.closeTicket(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}