package com.helps.domain.repository;

import com.helps.domain.model.User;
import com.helps.domain.model.UserActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

    Page<UserActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<UserActivityLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    @Query("SELECT u FROM UserActivityLog u WHERE u.createdAt BETWEEN :startDate AND :endDate ORDER BY u.createdAt DESC")
    Page<UserActivityLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          Pageable pageable);

    @Query("SELECT u FROM UserActivityLog u WHERE u.activity = :activity ORDER BY u.createdAt DESC")
    Page<UserActivityLog> findByActivityOrderByCreatedAtDesc(@Param("activity") String activity, Pageable pageable);

    List<UserActivityLog> findByActivityAndCreatedAtAfter(String activity, LocalDateTime createdAt);

    List<UserActivityLog> findTop10ByOrderByCreatedAtDesc();

    // Métodos otimizados para reduzir uso de memória
    @Query("SELECT COUNT(u) FROM UserActivityLog u WHERE u.activity = :activity AND u.createdAt >= :createdAt")
    Long countByActivityAndCreatedAtAfter(@Param("activity") String activity, @Param("createdAt") LocalDateTime createdAt);

    @Query("SELECT COUNT(DISTINCT u.user.id) FROM UserActivityLog u WHERE u.activity = :activity AND u.createdAt >= :createdAt")
    Long countDistinctUserByActivityAndCreatedAtAfter(@Param("activity") String activity, @Param("createdAt") LocalDateTime createdAt);

    @Query("SELECT FUNCTION('HOUR', u.createdAt), COUNT(u) FROM UserActivityLog u " +
           "WHERE u.activity = 'LOGIN' AND u.createdAt >= :since " +
           "GROUP BY FUNCTION('HOUR', u.createdAt) ORDER BY FUNCTION('HOUR', u.createdAt)")
    List<Object[]> findLoginsByHourRaw(@Param("since") LocalDateTime since);

    @Query("SELECT u.activity, COUNT(u) FROM UserActivityLog u " +
           "WHERE u.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY u.activity")
    List<Object[]> findActivitiesByTypeRaw(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}