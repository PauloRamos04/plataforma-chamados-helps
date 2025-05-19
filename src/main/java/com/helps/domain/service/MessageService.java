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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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
    private TicketAccessService ticketAccessService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FileStorageService fileStorageService;

    public List<Message> listMessagesByTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        if (!ticketAccessService.canAccessTicket(ticket)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You don't have permission to access the messages of this ticket");
        }

        return messageRepository.findByTicketOrderBySentDateAsc(ticket);
    }

    @Transactional
    public Message sendMessage(Long ticketId, MessageDto messageDTO) {
        User sender = userContextService.getCurrentUser();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        ticketAccessService.verifyMessagePermission(ticket);

        Message message = new Message();
        message.setTicket(ticket);
        message.setSender(sender);
        message.setContent(messageDTO.content());
        message.setSentDate(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        webSocketService.sendChatMessage(savedMessage);

        String summarizedContent = summarizeContent(messageDTO.content(), 50);
        notificationService.notifyMessageReceived(ticketId, sender.getId(), summarizedContent);

        return savedMessage;
    }

    private String summarizeContent(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;

        return content.substring(0, maxLength - 3) + "...";
    }

    @Transactional
    public Message sendMessageWithImage(Long ticketId, MessageDto messageDTO, MultipartFile image) {
        User sender = userContextService.getCurrentUser();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        ticketAccessService.verifyMessagePermission(ticket);

        Message message = new Message();
        message.setTicket(ticket);
        message.setSender(sender);
        message.setContent(messageDTO.content());
        message.setSentDate(LocalDateTime.now());

        if (image != null && !image.isEmpty()) {
            try {
                String imagePath = fileStorageService.storeFile(image);
                message.setImagePath(imagePath);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Failed to process the image: " + e.getMessage());
            }
        }

        Message savedMessage = messageRepository.save(message);

        webSocketService.sendChatMessage(savedMessage);

        String summarizedContent = summarizeContent(messageDTO.content(), 50);
        notificationService.notifyMessageReceived(ticketId, sender.getId(), summarizedContent);

        return savedMessage;
    }
}