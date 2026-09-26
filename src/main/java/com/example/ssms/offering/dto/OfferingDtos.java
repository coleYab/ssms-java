package com.example.ssms.offering.dto;

import com.example.ssms.offering.entity.OfferingStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class OfferingDtos {

    public record CreateOfferingRequest(
            @NotNull UUID courseId,
            @NotNull UUID classId,
            @NotNull UUID termId,
            @NotNull UUID teacherId,
            @NotNull @Min(1) @Max(10) Integer weeklyPeriods
    ) {}

    public record UpdateOfferingRequest(
            @Min(1) @Max(10) Integer weeklyPeriods,
            OfferingStatus status
    ) {}

    public record ReassignTeacherRequest(
            @NotNull UUID teacherId,
            LocalDate effectiveDate
    ) {}

    public record ScheduleSlotDto(
            @NotNull @Min(1) @Max(7) Integer dayOfWeek,
            @NotNull @Min(1) Integer periodNumber
    ) {}

    public record ReplaceScheduleRequest(
            @NotNull List<ScheduleSlotDto> slots
    ) {}

    public record OfferingResponse(
            UUID id,
            UUID courseId,
            UUID classId,
            UUID termId,
            UUID teacherId,
            int weeklyPeriods,
            OfferingStatus status,
            List<ScheduleSlotDto> schedule,
            Instant createdAt,
            Instant updatedAt,
            Integer version
    ) {}
}
