package com.helps.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets") // You can keep the database table as "chamados" if needed with: @Table(name = "chamados")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // previously titulo

    @Column(nullable = false, length = 1000)
    private String description; // previously descricao

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String category; // previously categoria

    @Column(nullable = false)
    private String type; // previously tipo

    @Column(name = "opening_date") // previously data_abertura
    private LocalDateTime openingDate; // previously dataAbertura

    @Column(name = "start_date") // previously data_inicio
    private LocalDateTime startDate; // previously dataInicio

    @Column(name = "closing_date") // previously data_fechamento
    private LocalDateTime closingDate; // previously dataFechamento

    @ManyToOne
    @JoinColumn(name = "user_id") // previously usuario_id
    private User user; // previously usuario

    @ManyToOne
    @JoinColumn(name = "helper_id")
    private User helper;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getOpeningDate() {
        return openingDate;
    }

    public void setOpeningDate(LocalDateTime openingDate) {
        this.openingDate = openingDate;
    }

    public LocalDateTime getClosingDate() {
        return closingDate;
    }

    public void setClosingDate(LocalDateTime closingDate) {
        this.closingDate = closingDate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getHelper() {
        return helper;
    }

    public void setHelper(User helper) {
        this.helper = helper;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
}