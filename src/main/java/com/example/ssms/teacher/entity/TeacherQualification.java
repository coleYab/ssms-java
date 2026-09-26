package com.example.ssms.teacher.entity;

import com.example.ssms.common.entity.AuditableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "teacher_qualifications", indexes = {
		@Index(name = "idx_qualifications_teacher_course", columnList = "teacher_id,course_id", unique = true)})
public class TeacherQualification extends AuditableEntity {

	@Column(name = "teacher_id", nullable = false)
	private UUID teacherId;

	@Column(name = "course_id", nullable = false)
	private UUID courseId;

	public UUID getTeacherId() {
		return teacherId;
	}

	public void setTeacherId(UUID teacherId) {
		this.teacherId = teacherId;
	}

	public UUID getCourseId() {
		return courseId;
	}

	public void setCourseId(UUID courseId) {
		this.courseId = courseId;
	}
}
