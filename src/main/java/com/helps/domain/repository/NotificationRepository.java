package com.helps.domain.repository;

import com.helps.domain.model.Notification;
import com.helps.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    List<Notification> findByUserAndReadFalseOrderByCreatedAtDesc(User user);
    List<Notification> findByUserAndReadFalse(User user);
    int countByUserAndReadFalse(User user);
}