package com.fragrance.ecommerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 80)
    private String resourceType;

    @Column(name = "resource_id", length = 120)
    private String resourceId;

    @Column(nullable = false, length = 30)
    private String outcome;

    @Column(name = "request_id", length = 120)
    private String requestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(UUID actorUserId, String action, String resourceType, String resourceId,
                    String outcome, String requestId) {
        this.id = UUID.randomUUID();
        this.actorUserId = actorUserId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.outcome = outcome;
        this.requestId = requestId;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }
}
