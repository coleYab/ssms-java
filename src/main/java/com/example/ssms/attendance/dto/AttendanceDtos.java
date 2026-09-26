package com.example.ssms.attendance.dto;

import com.example.ssms.attendance.entity.AttendanceSessionStatus;
import com.example.ssms.attendance.entity.AttendanceStatus;
import com.example.ssms.attendance.entity.ExcuseReasonCode;
import com.example.ssms.attendance.entity.ExcuseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AttendanceDtos {

    public record OpenSessionRequest(
            @NotNull UUID offeringId,
            @NotNull LocalDate date,
            @NotNull Integer periodNumber
    ) {}

    public record MarkRecordItem(
            @NotNull UUID studentId,
            @NotNull AttendanceStatus status,
            LocalTime arrivalTime,
            String note
    ) {}

    public record BulkMarkRequest(
            @NotNull @Size(min = 1, max = 200) List<MarkRecordItem> records
    ) {}

    public record CorrectRecordRequest(
            @NotNull AttendanceStatus status,
            LocalTime arrivalTime,
            String note,
            String reason
    ) {}

    public record ReopenSessionRequest(
            @NotBlank @Size(min = 10) String reason
    ) {}

    public record CreateExcuseRequest(
            @NotNull UUID recordId,
            @NotNull ExcuseReasonCode reasonCode,
            @NotBlank @Size(min = 10, max = 1000) String description,
            UUID studentId
    ) {}

    public record ReviewExcuseRequest(
            @NotNull String decision, // APPROVED or REJECTED
            String comment
    ) {}

    public record SessionResponse(
            UUID id,
            UUID offeringId,
            LocalDate date,
            int periodNumber,
            AttendanceSessionStatus status,
            UUID takenBy,
            Instant submittedAt,
            List<RecordResponse> records,
            Instant createdAt,
            Instant updatedAt,
            Integer version
    ) {}

    public record RecordResponse(
            UUID id,
            UUID sessionId,
            UUID studentId,
            AttendanceStatus status,
            LocalTime arrivalTime,
            String note,
            Instant createdAt,
            Instant updatedAt,
            Integer version
    ) {}

    public record BulkMarkResponse(
            UUID sessionId,
            AttendanceSessionStatus status,
            Map<String, Long> counts,
            int updated
    ) {}

    public record ExcuseResponse(
            UUID id,
            UUID recordId,
            UUID studentId,
            ExcuseReasonCode reasonCode,
            String description,
            ExcuseStatus status,
            UUID reviewedBy,
            Instant reviewedAt,
            String reviewComment,
            Instant createdAt,
            Instant updatedAt,
            Integer version
    ) {}
}
