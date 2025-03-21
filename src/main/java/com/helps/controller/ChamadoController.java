package com.helps.controller;

import com.helps.domain.model.Chamado;
import com.helps.domain.service.ChamadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chamados")
public class ChamadoController {

    @Autowired
    private ChamadoService chamadoService;

    @GetMapping
    public List<Chamado> listarChamados() {
        return chamadoService.listarChamados();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Chamado> buscarChamado(@PathVariable Long id) {
        return chamadoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Chamado> abrirChamado(@RequestBody Chamado chamado) {
        Chamado novoChamado = chamadoService.abrirChamado(chamado);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoChamado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Chamado> atualizarChamado(@PathVariable Long id, @RequestBody Chamado chamado) {
        try {
            Chamado chamadoAtualizado = chamadoService.atualizarChamado(id, chamado);
            return ResponseEntity.ok(chamadoAtualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/fechar")
    public ResponseEntity<Void> fecharChamado(@PathVariable Long id) {
        try {
            chamadoService.fecharChamado(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}