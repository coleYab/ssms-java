package com.example.ssms.student.dto;

import com.example.ssms.common.constant.Gender;
import com.example.ssms.guardian.entity.GuardianRelationship;
import com.example.ssms.student.entity.StudentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class StudentDtos {

    public record CreateGuardianSubRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotNull GuardianRelationship relationship,
            @NotBlank String phone,
            @Email String email,
            String address,
            String occupation,
            boolean isPrimary,
            Boolean canPickUp
    ) {}

    public record CreateStudentRequest(
            @NotBlank String firstName,
            String middleName,
            @NotBlank String lastName,
            @NotBlank @Email String email,
            @NotNull @Past LocalDate dateOfBirth,
            @NotNull Gender gender,
            @NotNull LocalDate admissionDate,
            UUID classId,
            String nationality,
            String phone,
            String addressLine1,
            String city,
            String country,
            String bloodType,
            String emergencyNotes,
            List<CreateGuardianSubRequest> guardians
    ) {}

    public record UpdateStudentRequest(
            String firstName,
            String middleName,
            String lastName,
            LocalDate dateOfBirth,
            Gender gender,
            String nationality,
            String phone,
            String addressLine1,
            String city,
            String country,
            String bloodType,
            String emergencyNotes
    ) {}

    public record ChangeStudentStatusRequest(
            @NotNull StudentStatus status,
            LocalDate effectiveDate,
            String reason
    ) {}

    public record CreateGuardianRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotNull GuardianRelationship relationship,
            @NotBlank String phone,
            @Email String email,
            String address,
            String occupation,
            boolean isPrimary,
            Boolean canPickUp
    ) {}

    public record UpdateGuardianRequest(
            String firstName,
            String lastName,
            GuardianRelationship relationship,
            String phone,
            String email,
            String address,
            String occupation,
            Boolean isPrimary,
            Boolean canPickUp
    ) {}

    public record GuardianResponse(
            UUID id,
            UUID studentId,
            String firstName,
            String lastName,
            GuardianRelationship relationship,
            String phone,
            String email,
            String address,
            String occupation,
            boolean isPrimary,
            boolean canPickUp,
            Instant createdAt,
            Instant updatedAt,
            Integer version
    ) {}

    public record StudentResponse(
            UUID id,
            UUID userId,
            String studentNumber,
            String firstName,
            String middleName,
            String lastName,
            String email,
            LocalDate dateOfBirth,
            Gender gender,
            String nationality,
            LocalDate admissionDate,
            StudentStatus status,
            UUID currentClassId,
            String phone,
            String addressLine1,
            String city,
            String country,
            String bloodType,
            String emergencyNotes,
            List<GuardianResponse> guardians,
            Instant createdAt,
            Instant updatedAt,
            Integer version
    ) {}
}
