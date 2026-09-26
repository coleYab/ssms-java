package com.example.ssms.enrollment.entity;

import com.example.ssms.common.entity.AuditableEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "enrollments", indexes = {
		@Index(name = "idx_enrollments_student_year", columnList = "student_id,academic_year_id"),
		@Index(name = "idx_enrollments_class_id", columnList = "class_id")})
public class Enrollment extends AuditableEntity {

	@Column(name = "student_id", nullable = false)
	private UUID studentId;

	@Column(name = "class_id", nullable = false)
	private UUID classId;

	@Column(name = "academic_year_id", nullable = false)
	private UUID academicYearId;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate = LocalDate.now();

	@Column(name = "end_date")
	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

	public UUID getStudentId() {
		return studentId;
	}

	public void setStudentId(UUID studentId) {
		this.studentId = studentId;
	}

	public UUID getClassId() {
		return classId;
	}

	public void setClassId(UUID classId) {
		this.classId = classId;
	}

	public UUID getAcademicYearId() {
		return academicYearId;
	}

	public void setAcademicYearId(UUID academicYearId) {
		this.academicYearId = academicYearId;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public EnrollmentStatus getStatus() {
		return status;
	}

	public void setStatus(EnrollmentStatus status) {
		this.status = status;
	}
}
