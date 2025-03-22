package com.helps.controller;

import com.helps.domain.model.Mensagem;
import com.helps.domain.service.MensagemService;
import com.helps.dto.MensagemDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chamados/{chamadoId}/mensagens")
public class MensagemController {

    @Autowired
    private MensagemService mensagemService;

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
    public ResponseEntity<Mensagem> enviarMensagem(
            @PathVariable Long chamadoId,
            @RequestBody MensagemDto mensagemDTO) {
        try {
            Mensagem mensagem = mensagemService.enviarMensagem(chamadoId, mensagemDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(mensagem);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}