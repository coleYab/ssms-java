package com.example.ssms.attendance.entity;

import com.example.ssms.common.entity.AuditableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "attendance_record_histories", indexes = {
    @Index(name = "idx_history_record_id", columnList = "record_id")
})
public class AttendanceRecordHistory extends AuditableEntity {

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 20)
    private AttendanceStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private AttendanceStatus newStatus;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    public UUID getRecordId() {
        return recordId;
    }

    public void setRecordId(UUID recordId) {
        this.recordId = recordId;
    }

    public AttendanceStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(AttendanceStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public AttendanceStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(AttendanceStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(UUID changedBy) {
        this.changedBy = changedBy;
    }
}
