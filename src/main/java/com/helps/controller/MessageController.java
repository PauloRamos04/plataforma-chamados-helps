package com.helps.controller;

import com.helps.domain.model.Message;
import com.helps.domain.service.MessageService;
import com.helps.dto.ChatMessageDto;
import com.helps.dto.MessageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chamados/{ticketId}/mensagens") // Keeping the endpoint URL the same
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public ResponseEntity<List<Message>> listMessages(@PathVariable Long ticketId) {
        try {
            List<Message> messages = messageService.listMessagesByTicket(ticketId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> sendMessage(@PathVariable Long ticketId, @RequestBody Map<String, Object> requestBody) {
        try {
            String content = extractContent(requestBody);

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "error", "Bad Request",
                                "message", "Message content is required"
                        ));
            }

            MessageDto messageDTO = new MessageDto(content);
            Message message = messageService.sendMessage(ticketId, messageDTO);

            notifyNewMessages(ticketId, message);

            return ResponseEntity.status(HttpStatus.CREATED).body(message);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Bad Request",
                            "message", "Error processing message: " + e.getMessage()
                    ));
        }
    }

    @GetMapping("/chat-history")
    public @ResponseBody List<ChatMessageDto> getChatHistory(@PathVariable Long ticketId) {
        List<Message> messages = messageService.listMessagesByTicket(ticketId);

        return messages.stream()
                .map(this::convertToChatMessageDto)
                .collect(Collectors.toList());
    }

    @MessageMapping("/chat.sendMessage/{ticketId}")
    @SendTo("/topic/ticket/{ticketId}")
    public ChatMessageDto sendMessage(
            @DestinationVariable Long ticketId,
            @Payload ChatMessageDto chatMessage) {

        MessageDto messageDto = new MessageDto(chatMessage.content());
        Message message = messageService.sendMessage(ticketId, messageDto);

        return convertToChatMessageDto(message);
    }

    @MessageMapping("/chat.addUser/{ticketId}")
    @SendTo("/topic/ticket/{ticketId}")
    public ChatMessageDto addUser(
            @DestinationVariable Long ticketId,
            @Payload ChatMessageDto chatMessage,
            SimpMessageHeaderAccessor headerAccessor) {

        headerAccessor.getSessionAttributes().put("username", chatMessage.senderName());
        headerAccessor.getSessionAttributes().put("ticketId", ticketId); // previously chamadoId

        return new ChatMessageDto(
                "JOIN",
                ticketId,
                chatMessage.senderId(),
                chatMessage.senderName(),
                chatMessage.senderName() + " joined the chat",
                LocalDateTime.now()
        );
    }

    private String extractContent(Map<String, Object> requestBody) {
        if (requestBody.containsKey("content")) {
            return (String) requestBody.get("content");
        } else if (requestBody.containsKey("conteudo")) {
            return (String) requestBody.get("conteudo");
        } else if (requestBody.containsKey("message")) {
            return (String) requestBody.get("message");
        } else if (requestBody.containsKey("texto")) {
            return (String) requestBody.get("texto");
        }
        return null;
    }

    private void notifyNewMessages(Long ticketId, Message message) {
        ChatMessageDto chatMessage = convertToChatMessageDto(message);
        messagingTemplate.convertAndSend("/topic/ticket/" + ticketId, chatMessage); // previously /topic/chamado/
    }

    private ChatMessageDto convertToChatMessageDto(Message message) {
        return new ChatMessageDto(
                "CHAT",
                message.getTicket().getId(),
                message.getSender().getId(),
                message.getSender().getName() != null ?
                        message.getSender().getName() :
                        message.getSender().getUsername(),
                message.getContent(),
                message.getSentDate()
        );
    }
}