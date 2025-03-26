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
import java.util.ArrayList;
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
        // Obter o usuário atual que criou o chamado
        User usuarioCriador = chamado.getUsuario();
        Long usuarioCriadorId = usuarioCriador != null ? usuarioCriador.getId() : null;

        // Buscar todos os usuários com role HELPER ou ADMIN (com ou sem prefixo ROLE_)
        List<User> usersToNotify = new ArrayList<>();

        try {
            // Buscar helpers
            List<User> helpers = userRepository.findAll().stream()
                    .filter(user -> user.isEnabled() &&
                            (hasRole(user, "HELPER") || hasRole(user, "ROLE_HELPER")))
                    .collect(Collectors.toList());
            usersToNotify.addAll(helpers);

            // Buscar admins
            List<User> admins = userRepository.findAll().stream()
                    .filter(user -> user.isEnabled() &&
                            (hasRole(user, "ADMIN") || hasRole(user, "ROLE_ADMIN")))
                    .collect(Collectors.toList());
            usersToNotify.addAll(admins);

            // Remover duplicados (caso um usuário tenha múltiplas roles)
            usersToNotify = usersToNotify.stream()
                    .distinct()
                    .collect(Collectors.toList());

            // Remover o usuário criador do chamado da lista (se estiver presente)
            if (usuarioCriadorId != null) {
                usersToNotify = usersToNotify.stream()
                        .filter(user -> !user.getId().equals(usuarioCriadorId))
                        .collect(Collectors.toList());
            }

            System.out.println("Total de usuários a notificar: " + usersToNotify.size());

            // Enviar notificações para cada destinatário
            for (User user : usersToNotify) {
                try {
                    criarNotificacaoParaUsuario(
                            user.getId(),
                            "Novo chamado disponível: " + chamado.getTitulo(),
                            "NOVO_CHAMADO",
                            chamado.getId()
                    );
                    System.out.println("Notificação enviada para: " + user.getUsername());
                } catch (Exception e) {
                    System.err.println("Erro ao enviar notificação para " + user.getUsername() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar notificações de novos chamados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));
    }

    public void notificarMensagemRecebida(Long chamadoId, Long remetenteId, String conteudoResumido) {
        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new RuntimeException("Chamado não encontrado"));

        User remetente = userRepository.findById(remetenteId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (chamado.getUsuario() != null && !chamado.getUsuario().getId().equals(remetenteId)) {
            criarNotificacaoParaUsuario(
                    chamado.getUsuario().getId(),
                    "Nova mensagem de " + remetente.getName() + ": " + conteudoResumido,
                    "NOVA_MENSAGEM",
                    chamadoId
            );
        }

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