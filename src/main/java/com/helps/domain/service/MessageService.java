package com.helps.domain.service;

import com.helps.domain.model.Ticket;
import com.helps.domain.model.Message;
import com.helps.domain.model.User;
import com.helps.domain.repository.TicketRepository;
import com.helps.domain.repository.MessageRepository;
import com.helps.dto.MessageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

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

    public List<Message> listMessagesByTicket(Long ticketId) { // previously listarMensagensPorChamado
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        if (!ticketAccessService.canAccessTicket(ticket)) { // previously podeAcessarChamado
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You don't have permission to access the messages of this ticket");
        }

        return messageRepository.findByTicketOrderBySentDateAsc(ticket); // previously findByChamadoOrderByDataEnvioAsc
    }

    @Transactional
    public Message sendMessage(Long ticketId, MessageDto messageDTO) { // previously enviarMensagem
        User sender = userContextService.getCurrentUser();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        ticketAccessService.verifyMessagePermission(ticket); // previously verificarPermissaoEnviarMensagem

        Message message = new Message();
        message.setTicket(ticket); // previously setChamado
        message.setSender(sender); // previously setRemetente
        message.setContent(messageDTO.content()); // previously setConteudo
        message.setSentDate(LocalDateTime.now()); // previously setDataEnvio

        Message savedMessage = messageRepository.save(message);

        webSocketService.sendChatMessage(savedMessage); // previously enviarMensagemChat

        String summarizedContent = summarizeContent(messageDTO.content(), 50); // previously resumirConteudo
        notificationService.notifyMessageReceived(ticketId, sender.getId(), summarizedContent); // previously notificarMensagemRecebida

        return savedMessage;
    }

    private String summarizeContent(String content, int maxLength) { // previously resumirConteudo
        if (content == null) return "";
        if (content.length() <= maxLength) return content;

        return content.substring(0, maxLength - 3) + "...";
    }
}