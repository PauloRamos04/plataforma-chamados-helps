package com.helps.domain.repository;

import com.helps.domain.model.Ticket;
import com.helps.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByStatus(String status);
    List<Ticket> findByHelper(User helper);
    List<Ticket> findByUser(User user); // previously findByUsuario

    @Query("SELECT t FROM Ticket t WHERE t.helper.id = :helperId OR t.status = :status")
    List<Ticket> findByHelperOrStatus(@Param("helperId") Long helperId, @Param("status") String status);

    @Query("SELECT t FROM Ticket t WHERE t.status = 'OPEN' AND t.category = :category") // previously categoria
    List<Ticket> findOpenTicketsByCategory(@Param("category") String category);

    @Query("SELECT t FROM Ticket t WHERE t.helper = :helper AND t.status = 'IN_PROGRESS'") // previously EM_ATENDIMENTO
    List<Ticket> findActiveByHelper(@Param("helper") User helper);

    @Query("SELECT t FROM Ticket t WHERE (t.helper = :user OR t.user = :user) AND t.status != 'CLOSED'") // previously FECHADO
    List<Ticket> findActiveTicketsByUser(@Param("user") User user);
}