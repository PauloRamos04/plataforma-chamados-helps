package com.helps.controller;

import com.helps.domain.model.Mensagem;
import com.helps.domain.service.MensagemService;
import com.helps.dto.ChatMessageDto;
import com.helps.dto.MensagemDto;
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
@RequestMapping("/chamados/{chamadoId}/mensagens")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
public class MensagemController {

    @Autowired
    private MensagemService mensagemService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public ResponseEntity<List<Mensagem>> listarMensagens(@PathVariable Long chamadoId) {
        try {
            List<Mensagem> mensagens = mensagemService.listarMensagensPorChamado(chamadoId);
            return ResponseEntity.ok(mensagens);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> enviarMensagem(@PathVariable Long chamadoId, @RequestBody Map<String, Object> requestBody) {
        try {
            String conteudo = extrairConteudo(requestBody);

            if (conteudo == null || conteudo.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "error", "Bad Request",
                                "message", "O conteúdo da mensagem é obrigatório"
                        ));
            }

            MensagemDto mensagemDTO = new MensagemDto(conteudo);
            Mensagem mensagem = mensagemService.enviarMensagem(chamadoId, mensagemDTO);

            notificarNovasMensagens(chamadoId, mensagem);

            return ResponseEntity.status(HttpStatus.CREATED).body(mensagem);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Bad Request",
                            "message", "Erro ao processar mensagem: " + e.getMessage()
                    ));
        }
    }

    @GetMapping("/chat-history")
    public @ResponseBody List<ChatMessageDto> getChatHistory(@PathVariable Long chamadoId) {
        List<Mensagem> mensagens = mensagemService.listarMensagensPorChamado(chamadoId);

        return mensagens.stream()
                .map(this::convertToChatMessageDto)
                .collect(Collectors.toList());
    }

    @MessageMapping("/chat.sendMessage/{chamadoId}")
    @SendTo("/topic/chamado/{chamadoId}")
    public ChatMessageDto sendMessage(
            @DestinationVariable Long chamadoId,
            @Payload ChatMessageDto chatMessage) {

        MensagemDto mensagemDto = new MensagemDto(chatMessage.content());
        Mensagem mensagem = mensagemService.enviarMensagem(chamadoId, mensagemDto);

        return convertToChatMessageDto(mensagem);
    }

    @MessageMapping("/chat.addUser/{chamadoId}")
    @SendTo("/topic/chamado/{chamadoId}")
    public ChatMessageDto addUser(
            @DestinationVariable Long chamadoId,
            @Payload ChatMessageDto chatMessage,
            SimpMessageHeaderAccessor headerAccessor) {

        headerAccessor.getSessionAttributes().put("username", chatMessage.senderName());
        headerAccessor.getSessionAttributes().put("chamadoId", chamadoId);

        return new ChatMessageDto(
                "JOIN",
                chamadoId,
                chatMessage.senderId(),
                chatMessage.senderName(),
                chatMessage.senderName() + " entrou no chat",
                LocalDateTime.now()
        );
    }

    private String extrairConteudo(Map<String, Object> requestBody) {
        if (requestBody.containsKey("conteudo")) {
            return (String) requestBody.get("conteudo");
        } else if (requestBody.containsKey("content")) {
            return (String) requestBody.get("content");
        } else if (requestBody.containsKey("message")) {
            return (String) requestBody.get("message");
        } else if (requestBody.containsKey("texto")) {
            return (String) requestBody.get("texto");
        }
        return null;
    }

    private void notificarNovasMensagens(Long chamadoId, Mensagem mensagem) {
        ChatMessageDto chatMessage = convertToChatMessageDto(mensagem);
        messagingTemplate.convertAndSend("/topic/chamado/" + chamadoId, chatMessage);
    }

    private ChatMessageDto convertToChatMessageDto(Mensagem mensagem) {
        return new ChatMessageDto(
                "CHAT",
                mensagem.getChamado().getId(),
                mensagem.getRemetente().getId(),
                mensagem.getRemetente().getName() != null ?
                        mensagem.getRemetente().getName() :
                        mensagem.getRemetente().getUsername(),
                mensagem.getConteudo(),
                mensagem.getDataEnvio()
        );
    }
}