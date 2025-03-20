package com.helps.service;

import com.helps.domain.model.Chamado;
import com.helps.domain.repository.repository.ChamadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChamadoService {

    @Autowired
    private ChamadoRepository chamadoRepository;

    public List<Chamado> listarChamados() {
        return chamadoRepository.findAll();
    }

    public Chamado abrirChamado(Chamado chamado) {
        return chamadoRepository.save(chamado);
    }
}