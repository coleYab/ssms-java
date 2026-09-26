package com.example.ssms.admin.dto;

import com.example.ssms.security.AccountStatus;
import com.example.ssms.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.UUID;

public class AdminDtos {

	public record CreateAdminRequest(@NotBlank String firstName, @NotBlank String lastName,
			@NotBlank @Email String email,
			@Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone must be in valid format") String phone,
			String department, String jobTitle) {
	}

	public record UpdateAdminRequest(String firstName, String lastName, @Email String email, String phone,
			String department, String jobTitle) {
	}

	public record AdminResponse(UUID id, String firstName, String lastName, String email, String phone,
			String department, String jobTitle, Role role, AccountStatus status, boolean mustChangePassword,
			Instant createdAt, Instant updatedAt, Integer version) {
	}
}
