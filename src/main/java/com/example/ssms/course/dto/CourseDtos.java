package com.example.ssms.course.dto;

import com.example.ssms.course.entity.CourseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.UUID;

public class CourseDtos {

	public record CreateCourseRequest(
			@NotBlank @Pattern(regexp = "^[A-Z0-9-]+$", message = "Course code must be uppercase alphanumeric or hyphens") String courseCode,
			@NotBlank String name, String description, @NotBlank String department, @NotNull UUID gradeLevelId,
			Double creditHours, Boolean isElective) {
	}

	public record UpdateCourseRequest(String courseCode, String name, String description, String department,
			UUID gradeLevelId, Double creditHours, Boolean isElective) {
	}

	public record CourseResponse(UUID id, String courseCode, String name, String description, String department,
			UUID gradeLevelId, Double creditHours, boolean isElective, CourseStatus status, Instant createdAt,
			Instant updatedAt, Integer version) {
	}
}
