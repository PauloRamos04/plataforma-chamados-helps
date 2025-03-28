package com.helps.domain.service;

import com.helps.domain.model.Ticket;
import com.helps.domain.model.User;
import com.helps.domain.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserContextService userContextService;

    @Autowired
    private TicketAccessService ticketAccessService; // previously ChamadoAccessService

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private NotificationService notificationService;

    public List<Ticket> listTickets() { // previously listarChamados
        User currentUser = userContextService.getCurrentUser();

        if (userContextService.hasRole("ADMIN")) {
            return ticketRepository.findAll();
        } else if (userContextService.hasRole("HELPER")) {
            return ticketRepository.findByHelperOrStatus(currentUser, "OPEN"); // previously ABERTO
        } else {
            return ticketRepository.findByUser(currentUser); // previously findByUsuario
        }
    }

    public List<Ticket> listTicketsByStatus(String status) { // previously listarChamadosPorStatus
        return ticketRepository.findByStatus(status);
    }

    @Transactional
    public Ticket openTicket(Ticket ticket) { // previously abrirChamado
        User requester = userContextService.getCurrentUser();

        ticket.setOpeningDate(LocalDateTime.now()); // previously setDataAbertura
        ticket.setStatus("OPEN"); // previously ABERTO
        ticket.setUser(requester); // previously setUsuario

        Ticket savedTicket = ticketRepository.save(ticket);

        // Notify about the new ticket - sending to all helpers and admin, except the creator
        try {
            notificationService.notifyNewTickets(savedTicket); // previously notificarNovosChamados
        } catch (Exception e) {
            // Log the error, but allow the operation to continue
            System.err.println("Error sending notifications for the new ticket: " + e.getMessage());
            e.printStackTrace();
        }

        return savedTicket;
    }

    @Transactional
    public Ticket updateTicket(Long id, Ticket updatedTicket) { // previously atualizarChamado
        return ticketRepository.findById(id)
                .map(ticket -> {
                    if (!ticketAccessService.canAccessTicket(ticket)) { // previously podeAcessarChamado
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "You don't have permission to update this ticket");
                    }

                    ticket.setTitle(updatedTicket.getTitle()); // previously setTitulo
                    ticket.setDescription(updatedTicket.getDescription()); // previously setDescricao

                    if (userContextService.hasRole("ADMIN") && updatedTicket.getStatus() != null) {
                        String previousStatus = ticket.getStatus();
                        ticket.setStatus(updatedTicket.getStatus());

                        if (!previousStatus.equals(updatedTicket.getStatus())) {
                            webSocketService.notifyTicketStatus(ticket, // previously notificarStatusChamado
                                    "Status changed from " + previousStatus + " to " + ticket.getStatus());
                        }
                    }

                    return ticketRepository.save(ticket);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    @Transactional
    public void closeTicket(Long id) { // previously fecharChamado
        ticketRepository.findById(id)
                .map(ticket -> {
                    ticketAccessService.verifyCloseTicketPermission(ticket); // previously verificarPermissaoFecharChamado

                    ticket.setStatus("CLOSED"); // previously FECHADO
                    ticket.setClosingDate(LocalDateTime.now()); // previously setDataFechamento

                    Ticket closedTicket = ticketRepository.save(ticket);

                    webSocketService.notifyTicketStatus(closedTicket, // previously notificarStatusChamado
                            "Ticket finalized by " + userContextService.getCurrentUser().getName());

                    if (ticket.getUser() != null) {
                        notificationService.createNotificationForUser( // previously criarNotificacaoParaUsuario
                                ticket.getUser().getId(),
                                "Your ticket \"" + ticket.getTitle() + "\" has been closed",
                                "TICKET_CLOSED", // previously CHAMADO_FECHADO
                                ticket.getId());
                    }

                    return closedTicket;
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    @Transactional
    public Ticket assignTicket(Long id) { // previously aderirChamado
        User helper = userContextService.getCurrentUser();
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        ticketAccessService.verifyAssignTicketPermission(ticket); // previously verificarPermissaoAderirChamado

        ticket.setHelper(helper);
        ticket.setStatus("IN_PROGRESS"); // previously EM_ATENDIMENTO
        ticket.setStartDate(LocalDateTime.now()); // previously setDataInicio

        Ticket updatedTicket = ticketRepository.save(ticket);

        webSocketService.notifyTicketStatus(updatedTicket, // previously notificarStatusChamado
                helper.getName() + " started attending this ticket");

        if (ticket.getUser() != null) {
            notificationService.createNotificationForUser( // previously criarNotificacaoParaUsuario
                    ticket.getUser().getId(),
                    "Your ticket \"" + ticket.getTitle() + "\" is now being attended by " + helper.getName(),
                    "TICKET_IN_PROGRESS", // previously CHAMADO_EM_ATENDIMENTO
                    ticket.getId());
        }

        return updatedTicket;
    }

    public List<Ticket> listTicketsByHelper() { // previously listarChamadosPorHelper
        User helper = userContextService.getCurrentUser();

        if (!userContextService.hasAnyRole("HELPER", "ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User doesn't have permission to list helper tickets");
        }

        return ticketRepository.findByHelper(helper);
    }

    public List<Ticket> listTicketsByUser() { // previously listarChamadosPorUsuario
        User user = userContextService.getCurrentUser();
        return ticketRepository.findByUser(user); // previously findByUsuario
    }

    public Optional<Ticket> findById(Long id) { // previously buscarPorId
        Optional<Ticket> ticketOpt = ticketRepository.findById(id);

        ticketOpt.ifPresent(ticket -> {
            if (!ticketAccessService.canAccessTicket(ticket)) { // previously podeAcessarChamado
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You don't have permission to access this ticket");
            }
        });

        return ticketOpt;
    }
}