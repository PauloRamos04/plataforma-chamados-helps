package com.helps.domain.repository;

import com.helps.domain.model.Ticket;
import com.helps.domain.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByTicketOrderBySentDateAsc(Ticket ticket);
}