package com.portal.service;

import com.portal.entity.AuditLog;
import com.portal.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void logEvent_savesAuditLogRecord() {
        auditService.logEvent(1L, "faculty@demo.com", "FACULTY",
                "VERIFY_STUDENT_VERIFIED", "STUDENT", 5L, "Approved profile", "127.0.0.1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals(1L, saved.getActorUserId());
        assertEquals("faculty@demo.com", saved.getActorEmail());
        assertEquals("VERIFY_STUDENT_VERIFIED", saved.getAction());
        assertEquals("STUDENT", saved.getTargetType());
        assertEquals(5L, saved.getTargetId());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void getAuditLogs_returnsPagedRecords() {
        Pageable pageable = PageRequest.of(0, 10);
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(AuditLog.builder().action("TEST").build())));

        Page<AuditLog> result = auditService.getAuditLogs(null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(auditLogRepository).findAllByOrderByCreatedAtDesc(pageable);
    }
}
