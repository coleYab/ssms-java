package com.example.ssms.auth.entity;

import com.example.ssms.common.entity.AuditableEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_sessions", indexes = {@Index(name = "idx_sessions_user_id", columnList = "user_id"),
		@Index(name = "idx_sessions_refresh_token_hash", columnList = "refresh_token_hash", unique = true)})
public class UserSession extends AuditableEntity {

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "refresh_token_hash", nullable = false, unique = true)
	private String refreshTokenHash;

	@Column(name = "device_info", length = 500)
	private String deviceInfo;

	@Column(name = "ip_address", length = 50)
	private String ipAddress;

	@Column(name = "last_used_at", nullable = false)
	private Instant lastUsedAt = Instant.now();

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked", nullable = false)
	private boolean revoked = false;

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public String getRefreshTokenHash() {
		return refreshTokenHash;
	}

	public void setRefreshTokenHash(String refreshTokenHash) {
		this.refreshTokenHash = refreshTokenHash;
	}

	public String getDeviceInfo() {
		return deviceInfo;
	}

	public void setDeviceInfo(String deviceInfo) {
		this.deviceInfo = deviceInfo;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public Instant getLastUsedAt() {
		return lastUsedAt;
	}

	public void setLastUsedAt(Instant lastUsedAt) {
		this.lastUsedAt = lastUsedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public boolean isRevoked() {
		return revoked;
	}

	public void setRevoked(boolean revoked) {
		this.revoked = revoked;
	}
}
