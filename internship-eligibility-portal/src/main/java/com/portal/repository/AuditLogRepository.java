package com.portal.repository;

import com.portal.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:actorRole IS NULL OR a.actorRole = :actorRole) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:targetType IS NULL OR a.targetType = :targetType) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> filterLogs(
            @Param("actorRole") String actorRole,
            @Param("action") String action,
            @Param("targetType") String targetType,
            Pageable pageable
    );
}
