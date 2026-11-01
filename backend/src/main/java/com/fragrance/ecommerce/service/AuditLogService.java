package com.fragrance.ecommerce.service;

import com.fragrance.ecommerce.domain.AuditLog;
import com.fragrance.ecommerce.repository.AuditLogRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(UUID actorUserId, String action, String resourceType, String resourceId,
                       String outcome, String requestId) {
        auditLogRepository.save(new AuditLog(
            actorUserId, action, resourceType, resourceId, outcome, requestId));
        LOGGER.info("Audit event action={} resourceType={} outcome={} requestId={}",
            action, resourceType, outcome, requestId);
    }
}
