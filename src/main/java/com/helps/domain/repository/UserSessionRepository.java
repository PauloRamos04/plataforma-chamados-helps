package com.helps.domain.repository;

import com.helps.domain.model.User;
import com.helps.domain.model.UserSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionIdAndIsActiveTrue(String sessionId);

    List<UserSession> findByUserAndIsActiveTrueOrderByLoginTimeDesc(User user);

    Page<UserSession> findAllByOrderByLoginTimeDesc(Pageable pageable);

    @Query("SELECT us FROM UserSession us WHERE us.isActive = true ORDER BY us.loginTime DESC")
    List<UserSession> findActiveSessions();

    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.isActive = true")
    Long countActiveSessions();

    @Query("SELECT us FROM UserSession us WHERE us.loginTime BETWEEN :startDate AND :endDate ORDER BY us.loginTime DESC")
    Page<UserSession> findByLoginTimeBetween(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate,
                                             Pageable pageable);
}