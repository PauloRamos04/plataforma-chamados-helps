package com.helps.domain.repository;

import com.helps.domain.model.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {
    Optional<TicketType> findByName(String name);
    List<TicketType> findByActiveTrue();
    List<TicketType> findByActiveTrueOrderByPriorityLevelDesc();
}