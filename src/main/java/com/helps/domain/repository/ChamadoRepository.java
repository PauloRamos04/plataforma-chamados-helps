package com.helps.domain.repository;

import com.helps.domain.model.Chamado;
import com.helps.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChamadoRepository extends JpaRepository<Chamado, Long> {
    List<Chamado> findByStatus(String status);
    List<Chamado> findByHelper(User helper);
    List<Chamado> findByUsuario(User usuario);

    @Query("SELECT c FROM Chamado c WHERE c.helper = :helper OR c.status = :status")
    List<Chamado> findByHelperOrStatus(@Param("helper") User helper, @Param("status") String status);

    @Query("SELECT c FROM Chamado c WHERE c.status = 'ABERTO' AND c.categoria = :categoria")
    List<Chamado> findOpenChamadosByCategoria(@Param("categoria") String categoria);

    @Query("SELECT c FROM Chamado c WHERE c.helper = :helper AND c.status = 'EM_ATENDIMENTO'")
    List<Chamado> findActiveByHelper(@Param("helper") User helper);

    @Query("SELECT c FROM Chamado c WHERE (c.helper = :user OR c.usuario = :user) AND c.status != 'FECHADO'")
    List<Chamado> findActiveChamadosByUser(@Param("user") User user);
}