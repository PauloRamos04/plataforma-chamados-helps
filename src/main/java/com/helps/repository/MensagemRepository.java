package com.helps.repository;

import com.helps.domain.model.Mensagem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {
}