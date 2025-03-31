package com.helps.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private String status;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private TicketType type;

    @Column(name = "opening_date")
    private LocalDateTime openingDate;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "closing_date")
    private LocalDateTime closingDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "helper_id")
    private User helper;

    @Column(name = "image_path")
    private String imagePath;

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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    // Método adicional para compatibilidade com código existente
    public void setCategory(String categoryName) {
        // Esta é uma implementação temporária que pode ser substituída
        // por uma busca real no repositório de categorias
        if (this.category == null) {
            Category tempCategory = new Category();
            tempCategory.setName(categoryName);
            this.category = tempCategory;
        } else {
            this.category.setName(categoryName);
        }
    }

    public TicketType getType() {
        return type;
    }

    public void setType(TicketType type) {
        this.type = type;
    }

    // Método adicional para compatibilidade com código existente
    public void setType(String typeName) {
        // Esta é uma implementação temporária que pode ser substituída
        // por uma busca real no repositório de tipos
        if (this.type == null) {
            TicketType tempType = new TicketType();
            tempType.setName(typeName);
            this.type = tempType;
        } else {
            this.type.setName(typeName);
        }
    }

    public LocalDateTime getOpeningDate() {
        return openingDate;
    }

    public void setOpeningDate(LocalDateTime openingDate) {
        this.openingDate = openingDate;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    // Métodos auxiliares para compatibilidade com código existente
    public String getCategoryName() {
        return category != null ? category.getName() : null;
    }

    public String getTypeName() {
        return type != null ? type.getName() : null;
    }
}