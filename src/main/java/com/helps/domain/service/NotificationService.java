package com.helps.domain.service;

import com.helps.domain.model.Chamado;
import com.helps.domain.model.Notification;
import com.helps.domain.model.User;
import com.helps.domain.repository.ChamadoRepository;
import com.helps.domain.repository.NotificationRepository;
import com.helps.domain.repository.UserRepository;
import com.helps.dto.NotificationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private UserContextService userContextService;

    @Transactional
    public NotificationDto criarNotificacaoParaUsuario(Long userId, String message, String type, Long chamadoId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notification.setChamadoId(chamadoId);
        notification.setCreatedAt(LocalDateTime.now());

        notification = notificationRepository.save(notification);
        NotificationDto notificationDto = convertToDto(notification);

        webSocketService.enviarNotificacao(notificationDto, user);

        return notificationDto;
    }

    public void notificarNovosChamados(Chamado chamado) {
        List<User> helpers = userContextService.findUsersWithRole("HELPER");

        for (User helper : helpers) {
            criarNotificacaoParaUsuario(
                    helper.getId(),
                    "Novo chamado disponível: " + chamado.getTitulo(),
                    "NOVO_CHAMADO",
                    chamado.getId()
            );
        }
    }

    public void notificarMensagemRecebida(Long chamadoId, Long remetenteId, String conteudoResumido) {
        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new RuntimeException("Chamado não encontrado"));

        User remetente = userRepository.findById(remetenteId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Notificar o solicitante se a mensagem for do helper
        if (chamado.getUsuario() != null && !chamado.getUsuario().getId().equals(remetenteId)) {
            criarNotificacaoParaUsuario(
                    chamado.getUsuario().getId(),
                    "Nova mensagem de " + remetente.getName() + ": " + conteudoResumido,
                    "NOVA_MENSAGEM",
                    chamadoId
            );
        }

        // Notificar o helper se a mensagem for do solicitante
        if (chamado.getHelper() != null && !chamado.getHelper().getId().equals(remetenteId)) {
            criarNotificacaoParaUsuario(
                    chamado.getHelper().getId(),
                    "Nova mensagem de " + remetente.getName() + ": " + conteudoResumido,
                    "NOVA_MENSAGEM",
                    chamadoId
            );
        }
    }

    @Transactional
    public List<NotificationDto> getUnreadNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationDto markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));

        notification.setRead(true);
        notification = notificationRepository.save(notification);

        return convertToDto(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        List<Notification> notifications = notificationRepository.findByUserAndReadFalse(user);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    private NotificationDto convertToDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getChamadoId(),
                notification.getCreatedAt()
        );
    }
}