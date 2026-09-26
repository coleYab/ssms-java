package com.example.ssms.auth.dto;

import com.example.ssms.security.AccountStatus;
import com.example.ssms.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public class AuthDtos {

	public record LoginRequest(@NotBlank @Email String email, @NotBlank String password, Boolean rememberMe) {
	}

	public record LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresIn,
			UserSummaryDto user) {
	}

	public record RefreshRequest(@NotBlank String refreshToken) {
	}

	public record ForgotPasswordRequest(@NotBlank @Email String email) {
	}

	public record ResetPasswordRequest(@NotBlank String token, @NotBlank String newPassword) {
	}

	public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
	}

	public record UpdateMeRequest(String phone, String preferredLanguage) {
	}

	public record UserSummaryDto(UUID id, String email, Role role, AccountStatus status, boolean mustChangePassword,
			String phone, String preferredLanguage, Instant createdAt, Instant updatedAt, Integer version) {
	}

	public record SessionDto(UUID id, String ipAddress, String deviceInfo, Instant lastUsedAt, Instant expiresAt) {
	}

	public record MeResponse(UserSummaryDto user, Object profile, java.util.List<String> permissions) {
	}
}
