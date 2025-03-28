package com.helps.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages") // You can keep the database table as "mensagens" if needed with: @Table(name = "mensagens")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false) // previously chamado_id
    private Ticket ticket; // previously chamado

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false) // previously remetente_id
    private User sender; // previously remetente

    @Column(nullable = false, length = 2000)
    private String content; // previously conteudo

    @Column(name = "sent_date", nullable = false) // previously data_envio
    private LocalDateTime sentDate; // previously dataEnvio

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getSentDate() {
        return sentDate;
    }

    public void setSentDate(LocalDateTime sentDate) {
        this.sentDate = sentDate;
    }
}