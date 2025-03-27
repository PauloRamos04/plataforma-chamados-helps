package com.helps.controller;

import com.helps.domain.model.Chamado;
import com.helps.domain.service.ChamadoService;
import com.helps.dto.ChamadoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Chamado> abrirChamadoComImagem(
            @RequestParam("titulo") String titulo,
            @RequestParam("descricao") String descricao,
            @RequestParam("categoria") String categoria,
            @RequestParam("tipo") String tipo,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        ChamadoDto chamadoDto = new ChamadoDto(titulo, descricao, categoria, tipo, image);
        Chamado novoChamado = chamadoService.abrirChamado(chamadoDto);
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

    @RequestMapping(value = "/{id}/aderir", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<Chamado> aderirChamado(@PathVariable Long id) {
        try {
            Chamado chamado = chamadoService.aderirChamado(id);
            return ResponseEntity.ok(chamado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @RequestMapping(value = {"/{id}/fechar", "/{id}/finalizar"}, method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<Void> fecharChamado(@PathVariable Long id) {
        try {
            chamadoService.fecharChamado(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}