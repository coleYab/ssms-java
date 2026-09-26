package com.example.ssms.offering.entity;

import com.example.ssms.common.entity.AuditableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "course_offerings", indexes = {
		@Index(name = "idx_offerings_course_class_term", columnList = "course_id,class_id,term_id", unique = true),
		@Index(name = "idx_offerings_teacher_id", columnList = "teacher_id"),
		@Index(name = "idx_offerings_term_id", columnList = "term_id"),
		@Index(name = "idx_offerings_class_id", columnList = "class_id")})
public class CourseOffering extends AuditableEntity {

	@Column(name = "course_id", nullable = false)
	private UUID courseId;

	@Column(name = "class_id", nullable = false)
	private UUID classId;

	@Column(name = "term_id", nullable = false)
	private UUID termId;

	@Column(name = "teacher_id", nullable = false)
	private UUID teacherId;

	@Column(name = "weekly_periods", nullable = false)
	private int weeklyPeriods = 3;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OfferingStatus status = OfferingStatus.ACTIVE;

	public UUID getCourseId() {
		return courseId;
	}

	public void setCourseId(UUID courseId) {
		this.courseId = courseId;
	}

	public UUID getClassId() {
		return classId;
	}

	public void setClassId(UUID classId) {
		this.classId = classId;
	}

	public UUID getTermId() {
		return termId;
	}

	public void setTermId(UUID termId) {
		this.termId = termId;
	}

	public UUID getTeacherId() {
		return teacherId;
	}

	public void setTeacherId(UUID teacherId) {
		this.teacherId = teacherId;
	}

	public int getWeeklyPeriods() {
		return weeklyPeriods;
	}

	public void setWeeklyPeriods(int weeklyPeriods) {
		this.weeklyPeriods = weeklyPeriods;
	}

	public OfferingStatus getStatus() {
		return status;
	}

	public void setStatus(OfferingStatus status) {
		this.status = status;
	}
}
