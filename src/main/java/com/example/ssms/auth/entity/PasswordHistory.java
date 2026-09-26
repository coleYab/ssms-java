package com.example.ssms.auth.entity;

import com.example.ssms.common.entity.AuditableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "password_histories", indexes = {@Index(name = "idx_pwd_history_user", columnList = "user_id")})
public class PasswordHistory extends AuditableEntity {

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}
}
