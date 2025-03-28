package com.helps.domain.service;

import com.helps.domain.model.Ticket;
import com.helps.domain.model.Message;
import com.helps.domain.model.User;
import com.helps.dto.ChatMessageDto;
import com.helps.dto.NotificationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendChatMessage(Message message) { // previously enviarMensagemChat
        Long ticketId = message.getTicket().getId(); // previously chamadoId
        User sender = message.getSender(); // previously remetente

        ChatMessageDto chatMessage = new ChatMessageDto(
                "CHAT",
                ticketId,
                sender.getId(),
                sender.getName() != null ? sender.getName() : sender.getUsername(),
                message.getContent(), // previously getConteudo
                message.getSentDate() // previously getDataEnvio
        );

        messagingTemplate.convertAndSend("/topic/ticket/" + ticketId, chatMessage); // previously /topic/chamado/
    }

    public void sendNotification(NotificationDto notification, User user) { // previously enviarNotificacao
        if (user != null) {
            try {
                // Send to the user-specific channel
                String destination = "/user/" + user.getUsername() + "/queue/notifications";
                messagingTemplate.convertAndSend(destination, notification);

                // Send to user ID-based channel (fallback)
                String idDestination = "/user/" + user.getId() + "/queue/notifications";
                if (!destination.equals(idDestination)) {
                    messagingTemplate.convertAndSend(idDestination, notification);
                }

                // Debug log
                System.out.println("Notification sent via WebSocket to " + user.getUsername() + " (ID: " + user.getId() + ")");
            } catch (Exception e) {
                System.err.println("Error sending via WebSocket to " + user.getUsername() + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("User is null, cannot send notification");
        }
    }

    public void notifyTicketStatus(Ticket ticket, String event) { // previously notificarStatusChamado
        ChatMessageDto statusMessage = new ChatMessageDto(
                "STATUS",
                ticket.getId(),
                null,
                "System",
                event,
                LocalDateTime.now()
        );

        messagingTemplate.convertAndSend("/topic/ticket/" + ticket.getId(), statusMessage); // previously /topic/chamado/

        // Also notify the user and helper about status change
        if (ticket.getUser() != null) {
            NotificationDto userNotification = new NotificationDto(
                    null,
                    "Ticket status #" + ticket.getId() + ": " + event,
                    "TICKET_STATUS", // previously STATUS_CHAMADO
                    false,
                    ticket.getId(),
                    LocalDateTime.now()
            );

            User user = ticket.getUser();
            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/notifications",
                    userNotification
            );
        }

        if (ticket.getHelper() != null) {
            NotificationDto helperNotification = new NotificationDto(
                    null,
                    "Ticket status #" + ticket.getId() + ": " + event,
                    "TICKET_STATUS", // previously STATUS_CHAMADO
                    false,
                    ticket.getId(),
                    LocalDateTime.now()
            );

            User helper = ticket.getHelper();
            messagingTemplate.convertAndSendToUser(
                    helper.getUsername(),
                    "/queue/notifications",
                    helperNotification
            );
        }
    }

    public void notifyUserEntry(Long ticketId, Long userId, String username) { // previously notificarEntradaUsuario
        ChatMessageDto joinMessage = new ChatMessageDto(
                "JOIN",
                ticketId,
                userId,
                username,
                username + " entered the chat",
                LocalDateTime.now()
        );

        messagingTemplate.convertAndSend("/topic/ticket/" + ticketId, joinMessage); // previously /topic/chamado/
    }

    public void notifyUserExit(Long ticketId, Long userId, String username) { // previously notificarSaidaUsuario
        ChatMessageDto leaveMessage = new ChatMessageDto(
                "LEAVE",
                ticketId,
                userId,
                username,
                username + " left the chat",
                LocalDateTime.now()
        );

        messagingTemplate.convertAndSend("/topic/ticket/" + ticketId, leaveMessage); // previously /topic/chamado/
    }

    public void sendGlobalNotification(NotificationDto notification) { // previously enviarNotificacaoGlobal
        try {
            messagingTemplate.convertAndSend("/topic/notifications", notification);
        } catch (Exception e) {
            System.err.println("Error sending global notification: " + e.getMessage());
        }
    }
}