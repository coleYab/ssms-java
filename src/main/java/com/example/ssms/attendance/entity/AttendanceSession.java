package com.example.ssms.attendance.entity;

import com.example.ssms.common.entity.AuditableEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "attendance_sessions", indexes = {
		@Index(name = "idx_sessions_offering_date_period", columnList = "offering_id,date,period_number", unique = true),
		@Index(name = "idx_sessions_date", columnList = "date")})
public class AttendanceSession extends AuditableEntity {

	@Column(name = "offering_id", nullable = false)
	private UUID offeringId;

	@Column(name = "date", nullable = false)
	private LocalDate date;

	@Column(name = "period_number", nullable = false)
	private int periodNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private AttendanceSessionStatus status = AttendanceSessionStatus.OPEN;

	@Column(name = "taken_by")
	private UUID takenBy;

	@Column(name = "submitted_at")
	private Instant submittedAt;

	public UUID getOfferingId() {
		return offeringId;
	}

	public void setOfferingId(UUID offeringId) {
		this.offeringId = offeringId;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public int getPeriodNumber() {
		return periodNumber;
	}

	public void setPeriodNumber(int periodNumber) {
		this.periodNumber = periodNumber;
	}

	public AttendanceSessionStatus getStatus() {
		return status;
	}

	public void setStatus(AttendanceSessionStatus status) {
		this.status = status;
	}

	public UUID getTakenBy() {
		return takenBy;
	}

	public void setTakenBy(UUID takenBy) {
		this.takenBy = takenBy;
	}

	public Instant getSubmittedAt() {
		return submittedAt;
	}

	public void setSubmittedAt(Instant submittedAt) {
		this.submittedAt = submittedAt;
	}
}
