package com.example.ssms.grade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.UUID;

public class GradeDtos {

	public record CreateGradeLevelRequest(@NotBlank String name,
			@NotBlank @Pattern(regexp = "^[A-Z0-9-]+$", message = "Code must be uppercase alphanumeric or hyphens") String code,
			@NotNull Integer sequence, String description) {
	}

	public record UpdateGradeLevelRequest(String name, @Pattern(regexp = "^[A-Z0-9-]+$") String code, Integer sequence,
			String description) {
	}

	public record GradeLevelResponse(UUID id, String name, String code, int sequence, String description,
			Instant createdAt, Instant updatedAt, Integer version) {
	}
}
