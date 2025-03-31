package com.helps.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helps.domain.model.AuditLog;
import com.helps.domain.model.User;
import com.helps.domain.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserContextService userContextService;

    @Autowired
    private ObjectMapper objectMapper;

    public void logCreate(String entityType, Long entityId, Object newEntity) {
        try {
            User currentUser = userContextService.getCurrentUser();

            AuditLog log = new AuditLog();
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setAction("CREATE");
            log.setChangedById(currentUser.getId());
            log.setChangedByUsername(currentUser.getUsername());
            log.setPreviousValue(null);
            log.setNewValue(objectMapper.writeValueAsString(newEntity));

            auditLogRepository.save(log);
            logger.debug("Registrado log de criação para entidade {} com ID {}", entityType, entityId);
        } catch (Exception e) {
            logger.error("Erro ao registrar log de auditoria para criação da entidade {} com ID {}: {}",
                    entityType, entityId, e.getMessage());
        }
    }

    public void logUpdate(String entityType, Long entityId, Object oldEntity, Object newEntity) {
        try {
            User currentUser = userContextService.getCurrentUser();

            AuditLog log = new AuditLog();
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setAction("UPDATE");
            log.setChangedById(currentUser.getId());
            log.setChangedByUsername(currentUser.getUsername());
            log.setPreviousValue(objectMapper.writeValueAsString(oldEntity));
            log.setNewValue(objectMapper.writeValueAsString(newEntity));

            auditLogRepository.save(log);
            logger.debug("Registrado log de atualização para entidade {} com ID {}", entityType, entityId);
        } catch (Exception e) {
            logger.error("Erro ao registrar log de auditoria para atualização da entidade {} com ID {}: {}",
                    entityType, entityId, e.getMessage());
        }
    }

    public void logDelete(String entityType, Long entityId, Object oldEntity) {
        try {
            User currentUser = userContextService.getCurrentUser();

            AuditLog log = new AuditLog();
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setAction("DELETE");
            log.setChangedById(currentUser.getId());
            log.setChangedByUsername(currentUser.getUsername());
            log.setPreviousValue(objectMapper.writeValueAsString(oldEntity));
            log.setNewValue(null);

            auditLogRepository.save(log);
            logger.debug("Registrado log de exclusão para entidade {} com ID {}", entityType, entityId);
        } catch (Exception e) {
            logger.error("Erro ao registrar log de auditoria para exclusão da entidade {} com ID {}: {}",
                    entityType, entityId, e.getMessage());
        }
    }

    public Page<AuditLog> getEntityAuditLogs(String entityType, Long entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }

    public Page<AuditLog> getAuditLogsByEntityType(String entityType, Pageable pageable) {
        return auditLogRepository.findByEntityType(entityType, pageable);
    }

    public Page<AuditLog> getAuditLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByChangedById(userId, pageable);
    }

    public Page<AuditLog> getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return auditLogRepository.findByDateRange(start, end, pageable);
    }

    public Page<AuditLog> getAuditLogsByEntityTypeAndDateRange(String entityType, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndDateRange(entityType, startDate,endDate, pageable);
    }
}