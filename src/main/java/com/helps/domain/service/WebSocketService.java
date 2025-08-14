package com.helps.domain.service;

import com.helps.domain.model.Ticket;
import com.helps.domain.model.Message;
import com.helps.domain.model.User;
import com.helps.dto.ChatMessageDto;
import com.helps.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Envia mensagem de chat para todos os usuários conectados ao ticket.
     */
    public void sendChatMessage(Message message) {
        try {
            Long ticketId = message.getTicket().getId();
            User sender = message.getSender();

            ChatMessageDto chatMessage = new ChatMessageDto(
                    "CHAT",
                    ticketId,
                    sender.getId(),
                    sender.getName() != null ? sender.getName() : sender.getUsername(),
                    message.getContent(),
                    message.getSentDate()
            );

            String destination = "/topic/ticket/" + ticketId;
            messagingTemplate.convertAndSend(destination, chatMessage);
            
            log.debug("Chat message sent to {}: user={} content={}",
                    destination, sender.getUsername(), 
                    message.getContent().substring(0, Math.min(50, message.getContent().length())));
                    
        } catch (MessagingException e) {
            log.error("Failed to send chat message for ticket {}: {}", 
                    message.getTicket().getId(), e.getMessage());
        }
    }

    /**
     * Envia notificação para usuário específico.
     */
    public void sendNotification(NotificationDto notification, User user) {
        if (user == null) {
            log.warn("Cannot send notification: user is null");
            return;
        }

        try {
            messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/notifications", notification);
            log.debug("Notification sent to user {}: {}", user.getUsername(), notification.message());
            
        } catch (MessagingException e) {
            log.error("Failed to send notification to user {}: {}", user.getUsername(), e.getMessage());
        }
    }

    /**
     * Notifica mudança de status do ticket.
     */
    public void notifyTicketStatus(Ticket ticket, String event) {
        try {
            ChatMessageDto statusMessage = new ChatMessageDto(
                    "STATUS",
                    ticket.getId(),
                    null,
                    "System",
                    event,
                    LocalDateTime.now()
            );

            messagingTemplate.convertAndSend("/topic/ticket/" + ticket.getId(), statusMessage);

            // Notifica o usuário do ticket
            if (ticket.getUser() != null) {
                NotificationDto userNotification = new NotificationDto(
                        null,
                        "Ticket #" + ticket.getId() + ": " + event,
                        "TICKET_STATUS",
                        false,
                        ticket.getId(),
                        LocalDateTime.now()
                );
                sendNotification(userNotification, ticket.getUser());
            }

            // Notifica o helper
            if (ticket.getHelper() != null && !ticket.getHelper().equals(ticket.getUser())) {
                NotificationDto helperNotification = new NotificationDto(
                        null,
                        "Ticket #" + ticket.getId() + ": " + event,
                        "TICKET_STATUS",
                        false,
                        ticket.getId(),
                        LocalDateTime.now()
                );
                sendNotification(helperNotification, ticket.getHelper());
            }
            
        } catch (MessagingException e) {
            log.error("Failed to notify ticket status for ticket {}: {}", ticket.getId(), e.getMessage());
        }
    }

    /**
     * Notifica entrada de usuário no chat.
     */
    public void notifyUserEntry(Long ticketId, Long userId, String username) {
        try {
            ChatMessageDto joinMessage = new ChatMessageDto(
                    "JOIN",
                    ticketId,
                    userId,
                    username,
                    username + " entrou no chat",
                    LocalDateTime.now()
            );

            messagingTemplate.convertAndSend("/topic/ticket/" + ticketId, joinMessage);
            log.debug("User entry notification sent: user={} ticket={}", username, ticketId);
            
        } catch (MessagingException e) {
            log.error("Failed to notify user entry: user={} ticket={} error={}", username, ticketId, e.getMessage());
        }
    }

    /**
     * Notifica saída de usuário do chat.
     */
    public void notifyUserExit(Long ticketId, Long userId, String username) {
        try {
            ChatMessageDto leaveMessage = new ChatMessageDto(
                    "LEAVE",
                    ticketId,
                    userId,
                    username,
                    username + " saiu do chat",
                    LocalDateTime.now()
            );

            messagingTemplate.convertAndSend("/topic/ticket/" + ticketId, leaveMessage);
            log.debug("User exit notification sent: user={} ticket={}", username, ticketId);
            
        } catch (MessagingException e) {
            log.error("Failed to notify user exit: user={} ticket={} error={}", username, ticketId, e.getMessage());
        }
    }

    /**
     * Envia notificação global para todos os usuários.
     */
    public void sendGlobalNotification(NotificationDto notification) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications", notification);
            log.debug("Global notification sent: {}", notification.message());
            
        } catch (MessagingException e) {
            log.error("Failed to send global notification: {}", e.getMessage());
        }
    }
}