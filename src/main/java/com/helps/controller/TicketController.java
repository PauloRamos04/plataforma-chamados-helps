package com.helps.controller;

import com.helps.domain.model.Ticket;
import com.helps.domain.service.TicketService;
import com.helps.dto.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tickets")
@Validated
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketListDto>>> listTickets() {
        List<Ticket> tickets = ticketService.listTickets();
        List<TicketListDto> ticketDtos = tickets.stream()
                .map(this::convertToListDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(ticketDtos));
    }

    @GetMapping("/paginated")
    public ResponseEntity<ApiResponse<Page<TicketResponseDto>>> listTicketsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "openingDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        TicketFilterDto filters = new TicketFilterDto(status, category, search, sortBy, sortDirection);
        Page<TicketResponseDto> tickets = ticketService.findAllWithPagination(pageable, filters);

        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponseDto>> getTicketById(@PathVariable Long id) {
        return ticketService.findById(id)
                .map(ticket -> ResponseEntity.ok(ApiResponse.success(ticket)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TicketListDto> openTicket(@RequestBody Ticket ticket) {
        Ticket newTicket = ticketService.openTicket(ticket);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToListDto(newTicket));
    }

    @PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketListDto> abrirChamadoComImagem(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("category") String category,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        TicketDto ticketDto = new TicketDto(title, description, category, image);
        Ticket newTicket = ticketService.openTicketImage(ticketDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToListDto(newTicket));
    }

    @PostMapping(value = "/new", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TicketResponseDto>> createTicket(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("category") String category,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        CreateTicketDto dto = new CreateTicketDto(title, description, category, image);
        TicketResponseDto ticket = ticketService.createTicket(dto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ticket, "Ticket criado com sucesso"));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<TicketResponseDto>> createTicketWithValidation(
            @Valid @RequestBody CreateTicketDto dto) {

        TicketResponseDto ticket = ticketService.createTicket(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ticket, "Ticket criado com sucesso"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketListDto> updateTicket(@PathVariable Long id, @RequestBody Ticket ticket) {
        try {
            Ticket updatedTicket = ticketService.updateTicket(id, ticket);
            return ResponseEntity.ok(convertToListDto(updatedTicket));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<TicketResponseDto>> assignTicket(@PathVariable Long id) {
        TicketResponseDto ticket = ticketService.assignTicket(id);
        return ResponseEntity.ok(ApiResponse.success(ticket, "Ticket atribuído com sucesso"));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<Void>> closeTicket(@PathVariable Long id) {
        ticketService.closeTicket(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Ticket finalizado com sucesso"));
    }

    @RequestMapping(value = "/{id}/aderir", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<TicketResponseDto>> assignTicketLegacy(@PathVariable Long id) {
        try {
            TicketResponseDto ticket = ticketService.assignTicket(id);
            return ResponseEntity.ok(ApiResponse.success(ticket));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(ApiResponse.error(e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erro interno do servidor"));
        }
    }

    @RequestMapping(value = {"/{id}/fechar", "/{id}/finalizar"}, method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<Void>> closeTicketLegacy(@PathVariable Long id) {
        try {
            ticketService.closeTicket(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Ticket não encontrado"));
        }
    }

    private TicketListDto convertToListDto(Ticket ticket) {
        return new TicketListDto(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getCategory(),
                ticket.getOpeningDate(),
                ticket.getStartDate(),
                ticket.getClosingDate(),
                ticket.getImagePath(),
                ticket.getUser() != null ? new SimpleUserDto(
                        ticket.getUser().getId(),
                        ticket.getUser().getUsername(),
                        ticket.getUser().getName()
                ) : null,
                ticket.getHelper() != null ? new SimpleUserDto(
                        ticket.getHelper().getId(),
                        ticket.getHelper().getUsername(),
                        ticket.getHelper().getName()
                ) : null
        );
    }
}