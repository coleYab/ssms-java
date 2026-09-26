package com.example.ssms.teacher.dto;

import com.example.ssms.common.constant.Gender;
import com.example.ssms.teacher.entity.TeacherStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class TeacherDtos {

    public record CreateTeacherRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank @Email String email,
            @NotBlank String phone,
            LocalDate dateOfBirth,
            Gender gender,
            @NotBlank String department,
            String qualification,
            @NotNull LocalDate hireDate,
            Integer maxWeeklyPeriods
    ) {}

    public record UpdateTeacherRequest(
            String firstName,
            String lastName,
            @Email String email,
            String phone,
            LocalDate dateOfBirth,
            Gender gender,
            String department,
            String qualification,
            Integer maxWeeklyPeriods
    ) {}

    public record ChangeTeacherStatusRequest(
            @NotNull TeacherStatus status,
            String reason,
            Boolean force
    ) {}

    public record ReplaceQualificationsRequest(
            @NotNull List<UUID> courseIds
    ) {}

    public record TeacherResponse(
            UUID id,
            UUID userId,
            String employeeNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            LocalDate dateOfBirth,
            Gender gender,
            String department,
            String qualification,
            LocalDate hireDate,
            int maxWeeklyPeriods,
            TeacherStatus status,
            Instant createdAt,
            Instant updatedAt,
            Integer version
    ) {}
}
