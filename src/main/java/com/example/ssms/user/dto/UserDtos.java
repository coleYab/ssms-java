package com.example.ssms.user.dto;

import com.example.ssms.security.AccountStatus;
import com.example.ssms.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public class UserDtos {

	public record UpdateUserRequest(@Email String email, String phone) {
	}

	public record ChangeRoleRequest(@NotNull Role role) {
	}

	public record DeactivateUserRequest(String reason) {
	}

	public record UserDetailsResponse(UUID id, String email, Role role, AccountStatus status, String phone,
			String preferredLanguage, boolean mustChangePassword, Instant lastLoginAt, Instant lockedUntil,
			Instant createdAt, Instant updatedAt, Instant deletedAt, Integer version) {
	}
}
