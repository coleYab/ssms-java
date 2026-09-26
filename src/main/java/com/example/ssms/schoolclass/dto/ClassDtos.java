package com.example.ssms.schoolclass.dto;

import com.example.ssms.schoolclass.entity.SchoolClassStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ClassDtos {

	public record CreateClassRequest(@NotBlank String name, @NotNull UUID academicYearId, @NotNull UUID gradeLevelId,
			@Min(1) @Max(100) Integer capacity, UUID homeroomTeacherId) {
	}

	public record UpdateClassRequest(String name, @Min(1) @Max(100) Integer capacity, SchoolClassStatus status) {
	}

	public record AssignHomeroomTeacherRequest(@NotNull UUID teacherId) {
	}

	public record EnrollStudentsRequest(@NotNull List<UUID> studentIds) {
	}

	public record TransferStudentsRequest(@NotNull UUID toClassId, @NotNull List<UUID> studentIds,
			LocalDate effectiveDate) {
	}

	public record PromoteClassRequest(@NotNull UUID toClassId, @NotNull List<UUID> studentIds,
			List<UUID> repeatStudentIds) {
	}

	public record ClassResponse(UUID id, String name, UUID academicYearId, UUID gradeLevelId, int capacity,
			UUID homeroomTeacherId, SchoolClassStatus status, int enrolledCount, Instant createdAt, Instant updatedAt,
			Integer version) {
	}

	public record BulkEnrollmentResult(List<EnrollmentSuccessItem> succeeded, List<EnrollmentFailedItem> failed) {
	}

	public record EnrollmentSuccessItem(UUID studentId) {
	}

	public record EnrollmentFailedItem(int index, UUID studentId, ErrorDetailItem error) {
	}

	public record ErrorDetailItem(String code, String message) {
	}
}
