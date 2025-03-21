package com.helps.domain.repository;

import com.helps.domain.model.Chamado;
import com.helps.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChamadoRepository extends JpaRepository<Chamado, Long> {
    List<Chamado> findByStatus(String status);
    List<Chamado> findByHelper(User helper);
    List<Chamado> findByUsuario(User usuario);
}