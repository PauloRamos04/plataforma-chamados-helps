package com.helps.controller;

import com.helps.domain.model.Mensagem;
import com.helps.domain.service.MensagemService;
import com.helps.dto.MensagemDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chamados/{chamadoId}/mensagens")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
public class MensagemController {

    @Autowired
    private MensagemService mensagemService;

    @GetMapping
    public ResponseEntity<List<Mensagem>> listarMensagens(@PathVariable Long chamadoId) {
        try {
            List<Mensagem> mensagens = mensagemService.listarMensagensPorChamado(chamadoId);
            return ResponseEntity.ok(mensagens);
        } catch (Exception e) {
            System.err.println("Erro ao listar mensagens do chamado " + chamadoId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> enviarMensagem(
            @PathVariable Long chamadoId,
            @RequestBody Map<String, Object> requestBody) {

        try {
            System.out.println("Recebido payload de mensagem para chamado " + chamadoId + ": " + requestBody);

            String conteudo = null;

            if (requestBody.containsKey("conteudo")) {
                conteudo = (String) requestBody.get("conteudo");
            } else if (requestBody.containsKey("content")) {
                conteudo = (String) requestBody.get("content");
            } else if (requestBody.containsKey("message")) {
                conteudo = (String) requestBody.get("message");
            } else if (requestBody.containsKey("texto")) {
                conteudo = (String) requestBody.get("texto");
            }

            if (conteudo == null || conteudo.trim().isEmpty()) {
                System.err.println("Erro: conteúdo da mensagem vazio ou não encontrado");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "error", "Bad Request",
                                "message", "O conteúdo da mensagem é obrigatório",
                                "details", "O campo 'conteudo' deve estar presente e não pode ser vazio"
                        ));
            }

            MensagemDto mensagemDTO = new MensagemDto(conteudo);


            Mensagem mensagem = mensagemService.enviarMensagem(chamadoId, mensagemDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(mensagem);
        } catch (Exception e) {
            System.err.println("Erro ao enviar mensagem para chamado " + chamadoId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Bad Request",
                            "message", "Erro ao processar mensagem: " + e.getMessage()
                    ));
        }
    }
}