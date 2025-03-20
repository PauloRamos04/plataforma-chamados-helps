package com.helps.controller;

import com.helps.domain.model.Chamado;
import com.helps.service.ChamadoService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping
    public Chamado abrirChamado(@RequestBody Chamado chamado) {
        return chamadoService.abrirChamado(chamado);
    }
}