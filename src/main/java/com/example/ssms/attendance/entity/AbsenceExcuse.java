package com.example.ssms.attendance.entity;

import com.example.ssms.common.entity.AuditableEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "absence_excuses", indexes = {
    @Index(name = "idx_excuses_record_id", columnList = "record_id"),
    @Index(name = "idx_excuses_student_id", columnList = "student_id"),
    @Index(name = "idx_excuses_status", columnList = "status")
})
public class AbsenceExcuse extends AuditableEntity {

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 30)
    private ExcuseReasonCode reasonCode;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExcuseStatus status = ExcuseStatus.PENDING;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    public UUID getRecordId() {
        return recordId;
    }

    public void setRecordId(UUID recordId) {
        this.recordId = recordId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public ExcuseReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(ExcuseReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ExcuseStatus getStatus() {
        return status;
    }

    public void setStatus(ExcuseStatus status) {
        this.status = status;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(UUID reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }
}
