package com.example.ssms.academic.dto;

import com.example.ssms.academic.entity.AcademicYearStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class AcademicDtos {

    public record CreateAcademicYearRequest(
            @NotBlank String name,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate
    ) {}

    public record UpdateAcademicYearRequest(
            String name,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record CreateTermRequest(
            @NotBlank String name,
            @NotNull Integer sequence,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate
    ) {}

    public record UpdateTermRequest(
            String name,
            Integer sequence,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record TermResponse(
            UUID id,
            UUID academicYearId,
            String name,
            int sequence,
            LocalDate startDate,
            LocalDate endDate,
            Instant createdAt,
            Instant updatedAt,
            Integer version
    ) {}

    public record AcademicYearResponse(
            UUID id,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            AcademicYearStatus status,
            List<TermResponse> terms,
            Instant createdAt,
            Instant updatedAt,
            Integer version
    ) {}
}
