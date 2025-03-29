package com.helps.domain.service;

import com.helps.domain.model.Ticket;
import com.helps.domain.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketAccessService {

    @Autowired
    private UserContextService userContextService;

    public boolean canAccessTicket(Ticket ticket) {
        User currentUser = userContextService.getCurrentUser();

        if (userContextService.hasAnyRole("ADMIN", "HELPER")) {
            return true;
        }

        if (ticket.getUser() != null &&
                ticket.getUser().getId().equals(currentUser.getId())) {
            return true;
        }

        if (ticket.getHelper() != null &&
                ticket.getHelper().getId().equals(currentUser.getId())) {
            return true;
        }

        if ("OPEN".equals(ticket.getStatus()) &&
                userContextService.hasRole("HELPER")) {
            return true;
        }

        return false;
    }

    public void verifyAssignTicketPermission(Ticket ticket) {
        if (!userContextService.hasAnyRole("HELPER", "ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User doesn't have permission to assign tickets. Required roles: HELPER or ADMIN");
        }

        if (!"ABERTO".equals(ticket.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ticket is not available for assignment. Current status: " + ticket.getStatus());
        }
    }

    public void verifyCloseTicketPermission(Ticket ticket) { // previously verificarPermissaoFecharChamado
        User currentUser = userContextService.getCurrentUser();

        if (userContextService.hasAnyRole("ADMIN", "HELPER")) {
            return;
        }

        if (ticket.getHelper() == null ||
                !ticket.getHelper().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the assigned helper can close this ticket");
        }

        if (!"IN_PROGRESS".equals(ticket.getStatus())) { // previously EM_ATENDIMENTO
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only tickets in progress can be closed. Current status: " + ticket.getStatus());
        }
    }

    public void verifyMessagePermission(Ticket ticket) {
        User currentUser = userContextService.getCurrentUser();

        if (userContextService.hasAnyRole("ADMIN","HELPER")) {
            return;
        }

        boolean isRequester = ticket.getUser() != null &&
                currentUser.getId().equals(ticket.getUser().getId());

        boolean isHelper = ticket.getHelper() != null &&
                currentUser.getId().equals(ticket.getHelper().getId());

        if (!isRequester && !isHelper) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You don't have permission to send messages in this ticket");
        }

        if (!"IN_PROGRESS".equals(ticket.getStatus())) { // previously EM_ATENDIMENTO
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot send messages to a ticket that is not in progress");
        }
    }
}