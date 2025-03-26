package com.helps.domain.service;

import com.helps.domain.model.Chamado;
import com.helps.domain.model.Mensagem;
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

    public void enviarMensagemChat(Mensagem mensagem) {
        Long chamadoId = mensagem.getChamado().getId();
        User remetente = mensagem.getRemetente();

        ChatMessageDto chatMessage = new ChatMessageDto(
                "CHAT",
                chamadoId,
                remetente.getId(),
                remetente.getName() != null ? remetente.getName() : remetente.getUsername(),
                mensagem.getConteudo(),
                mensagem.getDataEnvio()
        );

        messagingTemplate.convertAndSend("/topic/chamado/" + chamadoId, chatMessage);
    }

    public void enviarNotificacao(NotificationDto notification, User user) {
        if (user != null) {
            try {
                // Enviar para o canal específico do usuário
                String destination = "/user/" + user.getUsername() + "/queue/notifications";
                messagingTemplate.convertAndSend(destination, notification);

                // Enviar para o canal baseado no ID do usuário (fallback)
                String idDestination = "/user/" + user.getId() + "/queue/notifications";
                if (!destination.equals(idDestination)) {
                    messagingTemplate.convertAndSend(idDestination, notification);
                }

                // Log para debugar
                System.out.println("Notificação enviada via WebSocket para " + user.getUsername() + " (ID: " + user.getId() + ")");
            } catch (Exception e) {
                System.err.println("Erro ao enviar via WebSocket para " + user.getUsername() + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Usuário é nulo, não é possível enviar notificação");
        }
    }

    public void notificarStatusChamado(Chamado chamado, String evento) {
        ChatMessageDto statusMessage = new ChatMessageDto(
                "STATUS",
                chamado.getId(),
                null,
                "Sistema",
                evento,
                LocalDateTime.now()
        );

        messagingTemplate.convertAndSend("/topic/chamado/" + chamado.getId(), statusMessage);

        // Também notificar usuário e helper sobre mudança de status
        if (chamado.getUsuario() != null) {
            NotificationDto userNotification = new NotificationDto(
                    null,
                    "Status do chamado #" + chamado.getId() + ": " + evento,
                    "STATUS_CHAMADO",
                    false,
                    chamado.getId(),
                    LocalDateTime.now()
            );

            User user = chamado.getUsuario();
            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/notifications",
                    userNotification
            );
        }

        if (chamado.getHelper() != null) {
            NotificationDto helperNotification = new NotificationDto(
                    null,
                    "Status do chamado #" + chamado.getId() + ": " + evento,
                    "STATUS_CHAMADO",
                    false,
                    chamado.getId(),
                    LocalDateTime.now()
            );

            User helper = chamado.getHelper();
            messagingTemplate.convertAndSendToUser(
                    helper.getUsername(),
                    "/queue/notifications",
                    helperNotification
            );
        }
    }

    public void notificarEntradaUsuario(Long chamadoId, Long userId, String username) {
        ChatMessageDto joinMessage = new ChatMessageDto(
                "JOIN",
                chamadoId,
                userId,
                username,
                username + " entrou no chat",
                LocalDateTime.now()
        );

        messagingTemplate.convertAndSend("/topic/chamado/" + chamadoId, joinMessage);
    }

    public void notificarSaidaUsuario(Long chamadoId, Long userId, String username) {
        ChatMessageDto leaveMessage = new ChatMessageDto(
                "LEAVE",
                chamadoId,
                userId,
                username,
                username + " saiu do chat",
                LocalDateTime.now()
        );

        messagingTemplate.convertAndSend("/topic/chamado/" + chamadoId, leaveMessage);
    }

    public void enviarNotificacaoGlobal(NotificationDto notification) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications", notification);
        } catch (Exception e) {
            System.err.println("Erro ao enviar notificação global: " + e.getMessage());
        }
    }
}