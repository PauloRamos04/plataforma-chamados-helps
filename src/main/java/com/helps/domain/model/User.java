package com.helps.domain.model;

import com.helps.dto.LoginRequest;
import jakarta.persistence.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column
    private String name;

    @Column(nullable = false)
    private Boolean enabled = true;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserActivityLog> activityLogs = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserSession> sessions = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Notification> notifications = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST)
    private Set<Ticket> createdTickets = new HashSet<>();

    @OneToMany(mappedBy = "helper", cascade = CascadeType.PERSIST)
    private Set<Ticket> helperTickets = new HashSet<>();

    @OneToMany(mappedBy = "sender", cascade = CascadeType.PERSIST)
    private Set<Message> sentMessages = new HashSet<>();

    @PreRemove
    private void preRemove() {
        for (Ticket ticket : createdTickets) {
            ticket.setUser(null);
        }
        for (Ticket ticket : helperTickets) {
            ticket.setHelper(null);
        }
        for (Message message : sentMessages) {
            message.setSender(null);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public Set<UserActivityLog> getActivityLogs() {
        return activityLogs;
    }

    public void setActivityLogs(Set<UserActivityLog> activityLogs) {
        this.activityLogs = activityLogs;
    }

    public Set<UserSession> getSessions() {
        return sessions;
    }

    public void setSessions(Set<UserSession> sessions) {
        this.sessions = sessions;
    }

    public Set<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(Set<Notification> notifications) {
        this.notifications = notifications;
    }

    public Set<Ticket> getCreatedTickets() {
        return createdTickets;
    }

    public void setCreatedTickets(Set<Ticket> createdTickets) {
        this.createdTickets = createdTickets;
    }

    public Set<Ticket> getHelperTickets() {
        return helperTickets;
    }

    public void setHelperTickets(Set<Ticket> helperTickets) {
        this.helperTickets = helperTickets;
    }

    public Set<Message> getSentMessages() {
        return sentMessages;
    }

    public void setSentMessages(Set<Message> sentMessages) {
        this.sentMessages = sentMessages;
    }

    public boolean isLoginCorrect(LoginRequest loginRequest, BCryptPasswordEncoder encoder) {
        return encoder.matches(loginRequest.password(), this.password);
    }

    public boolean isEnabled() {
        return this.enabled;
    }
}