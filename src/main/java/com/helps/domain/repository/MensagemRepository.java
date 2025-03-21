package com.helps.domain.repository;

import com.helps.domain.model.Chamado;
import com.helps.domain.model.Mensagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensagemRepository extends JpaRepository<Mensagem, Long> {
    List<Mensagem> findByChamadoOrderByDataEnvioAsc(Chamado chamado);
}