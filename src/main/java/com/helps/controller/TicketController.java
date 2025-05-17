package com.helps.controller;

import com.helps.domain.model.Ticket;
import com.helps.domain.service.TicketService;
import com.helps.dto.TicketDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/chamados")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @GetMapping
    public List<Ticket> listTickets() {
        return ticketService.listTickets();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable Long id) {
        return ticketService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Ticket> openTicket(@RequestBody Ticket ticket) {
        Ticket newTicket = ticketService.openTicket(ticket);
        return ResponseEntity.status(HttpStatus.CREATED).body(newTicket);
    }

    @PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Ticket> abrirChamadoComImagem(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("category") String category,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        TicketDto ticketDto = new TicketDto(title, description, category, image);
        Ticket newTicket = ticketService.openTicketImage(ticketDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newTicket);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ticket> updateTicket(@PathVariable Long id, @RequestBody Ticket ticket) {
        try {
            Ticket updatedTicket = ticketService.updateTicket(id, ticket);
            return ResponseEntity.ok(updatedTicket);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/{id}/aderir", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<Ticket> assignTicket(@PathVariable Long id) {
        try {
            Ticket ticket = ticketService.assignTicket(id);
            return ResponseEntity.ok(ticket);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @RequestMapping(value = {"/{id}/fechar", "/{id}/finalizar"}, method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<Void> closeTicket(@PathVariable Long id) {
        try {
            ticketService.closeTicket(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}