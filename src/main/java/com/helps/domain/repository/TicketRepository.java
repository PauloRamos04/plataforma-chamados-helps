package com.helps.domain.repository;

import com.helps.domain.model.Ticket;
import com.helps.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByStatus(String status);
    List<Ticket> findByHelper(User helper);
    List<Ticket> findByUser(User user);
    Page<Ticket> findByUser(User user, Pageable pageable);
    Page<Ticket> findByHelper(User helper, Pageable pageable);

    List<Ticket> findByOpeningDateBetween(LocalDateTime start, LocalDateTime end);

    Long countByHelperId(Long helperId);

    // Método corrigido usando SQL nativo
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (t.closing_date - t.opening_date))/60) " +
            "FROM tickets t " +
            "WHERE t.helper_id = :helperId " +
            "AND t.closing_date IS NOT NULL " +
            "AND t.opening_date IS NOT NULL",
            nativeQuery = true)
    Double getAverageResolutionTimeByHelper(@Param("helperId") Long helperId);

    @Query("SELECT t FROM Ticket t WHERE t.helper = :helper OR t.status = :status")
    List<Ticket> findByHelperOrStatus(@Param("helper") User helper, @Param("status") String status);

    @Query("SELECT t FROM Ticket t WHERE t.status = 'OPEN' AND t.category.name = :category")
    List<Ticket> findOpenTicketsByCategory(@Param("category") String category);

    @Query("SELECT t FROM Ticket t WHERE t.helper = :helper AND t.status = 'IN_PROGRESS'")
    List<Ticket> findActiveByHelper(@Param("helper") User helper);

    @Query("SELECT t FROM Ticket t WHERE (t.helper = :user OR t.user = :user) AND t.status != 'CLOSED'")
    List<Ticket> findActiveTicketsByUser(@Param("user") User user);
}