package com.example.ssms.audit.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {@Index(name = "idx_audit_actor_id", columnList = "actor_id"),
		@Index(name = "idx_audit_action", columnList = "action"),
		@Index(name = "idx_audit_resource", columnList = "resource_type,resource_id"),
		@Index(name = "idx_audit_created_at", columnList = "created_at")})
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "actor_id")
	private UUID actorId;

	@Column(name = "actor_role", length = 30)
	private String actorRole;

	@Column(name = "actor_email", length = 254)
	private String actorEmail;

	@Column(name = "action", nullable = false, length = 100)
	private String action;

	@Column(name = "resource_type", length = 100)
	private String resourceType;

	@Column(name = "resource_id")
	private UUID resourceId;

	@Column(name = "outcome", nullable = false, length = 30)
	private String outcome;

	@Column(name = "changes", columnDefinition = "TEXT")
	private String changes;

	@Column(name = "ip", length = 50)
	private String ip;

	@Column(name = "user_agent", length = 500)
	private String userAgent;

	@Column(name = "request_id", length = 100)
	private String requestId;

	@Column(name = "session_id")
	private UUID sessionId;

	@Column(name = "reason", length = 1000)
	private String reason;

	public UUID getId() {
		return id;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public UUID getActorId() {
		return actorId;
	}

	public void setActorId(UUID actorId) {
		this.actorId = actorId;
	}

	public String getActorRole() {
		return actorRole;
	}

	public void setActorRole(String actorRole) {
		this.actorRole = actorRole;
	}

	public String getActorEmail() {
		return actorEmail;
	}

	public void setActorEmail(String actorEmail) {
		this.actorEmail = actorEmail;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public UUID getResourceId() {
		return resourceId;
	}

	public void setResourceId(UUID resourceId) {
		this.resourceId = resourceId;
	}

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}

	public String getChanges() {
		return changes;
	}

	public void setChanges(String changes) {
		this.changes = changes;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public UUID getSessionId() {
		return sessionId;
	}

	public void setSessionId(UUID sessionId) {
		this.sessionId = sessionId;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
