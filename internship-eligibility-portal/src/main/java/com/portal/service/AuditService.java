package com.portal.service;

import com.portal.entity.AuditLog;
import com.portal.repository.AuditLogRepository;
import com.portal.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logEvent(Long actorUserId, String actorEmail, String actorRole, String action,
                         String targetType, Long targetId, String details, String ipAddress) {
        AuditLog auditLog = AuditLog.builder()
                .actorUserId(actorUserId)
                .actorEmail(actorEmail)
                .actorRole(actorRole)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
        log.info("AUDIT EVENT: action={}, actor={}, targetType={}, targetId={}", action, actorEmail, targetType, targetId);
    }

    @Transactional
    public void logCurrentAction(String action, String targetType, Long targetId, String details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        String email = "anonymous";
        String role = "ANONYMOUS";

        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            userId = principal.getId();
            email = principal.getUsername();
            role = principal.getAuthorities().stream().findFirst().map(Object::toString).orElse("USER");
        }

        logEvent(userId, email, role, action, targetType, targetId, details, null);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(String actorRole, String action, String targetType, Pageable pageable) {
        if (actorRole == null && action == null && targetType == null) {
            return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return auditLogRepository.filterLogs(actorRole, action, targetType, pageable);
    }
}
